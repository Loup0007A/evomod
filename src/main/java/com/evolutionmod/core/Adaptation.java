package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.NotifyMsg;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.HistoryEntry;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutations « globales » favorisées : quand un mob est tué ou une plante
 * cassée, il y a une petite chance que toute l'espèce s'adapte d'un coup.
 * Le gène favorisé dépend du contexte : biome, température, lumière, eau,
 * altitude, présence de prédateurs, cause de la mort...
 */
public final class Adaptation {

    private Adaptation() {}

    public enum Cause { HUNTED, PLANT_BROKEN, FORCED }

    private record Pressure(GeneType gene, double direction, String reason, double weight) {}

    private static final RandomSource RANDOM = RandomSource.create();

    /** Tire au sort l'adaptation (1 % plantes, 1,5 % mobs par défaut). */
    public static void roll(ServerLevel level, BlockPos pos, String speciesId, Cause cause, Entity victim) {
        double chance = cause == Cause.PLANT_BROKEN ? EvoConfig.plantAdaptationChance() : EvoConfig.mobAdaptationChance();
        if (RANDOM.nextDouble() < chance) trigger(level, pos, speciesId, cause, victim);
    }

    /** Applique une adaptation globale à l'espèce. Renvoie le gène favorisé (ou null). */
    public static GeneType trigger(ServerLevel level, BlockPos pos, String speciesId, Cause cause, Entity victim) {
        EvolutionSavedData data = EvolutionSavedData.get(level.getServer());
        SpeciesRecord record = data.get(speciesId);
        if (record == null) return null;
        Genome template = record.template();

        List<Pressure> pressures = analyse(level, pos, cause, victim, record.kind());
        pressures.removeIf(p -> !template.has(p.gene()));
        Pressure chosen;
        if (pressures.isEmpty()) {
            List<GeneType> genes = new ArrayList<>(template.genes());
            genes.remove(GeneType.HUE);
            if (genes.isEmpty()) return null;
            chosen = new Pressure(genes.get(RANDOM.nextInt(genes.size())), 1, "random", 1);
        } else {
            double total = pressures.stream().mapToDouble(Pressure::weight).sum();
            double r = RANDOM.nextDouble() * total;
            chosen = pressures.get(pressures.size() - 1);
            for (Pressure p : pressures) {
                r -= p.weight();
                if (r <= 0) {
                    chosen = p;
                    break;
                }
            }
        }

        GeneType gene = chosen.gene();
        double step = gene.range() * (0.06 + RANDOM.nextDouble() * 0.06) * chosen.direction();
        template.set(gene, template.get(gene) + step);
        data.addMutations(1);

        // Les individus déjà présents suivent en partie (la mutation se répand)
        if (!record.kind().isPlant()) {
            for (ServerLevel l : level.getServer().getAllLevels()) {
                for (Entity e : l.getAllEntities()) {
                    if (!(e instanceof LivingEntity living)) continue;
                    Genome g = EvolutionCapability.genome(living);
                    if (g == null || !g.species().equals(speciesId)) continue;
                    g.set(gene, g.get(gene) + step * 0.5);
                    Phenotype.apply(living, g, false);
                    EvolutionManager.sync(living);
                }
            }
        } else if (record.kind() == SpeciesProfile.Kind.TREE) {
            TreeMutator.mutateNear(level, pos, speciesId.substring(6), template, 6, 0.6);
        }

        long day = EvolutionManager.currentDay(level.getServer());
        data.addHistory(new HistoryEntry(day, HistoryEntry.Type.ADAPTATION, speciesId,
            gene.name() + "|" + chosen.reason(), step));
        data.setDirty();

        EvoNetwork.sendToAll(level.getServer(), new NotifyMsg(NotifyMsg.Kind.ADAPTATION,
            "evolutionmod.notify.adaptation", List.of("#species:" + speciesId, "#gene:" + gene.name(),
            "#reason:" + chosen.reason(), step > 0 ? "+" : "-")));
        EvolutionMod.LOGGER.info("Adaptation de {} : {} {} ({})", speciesId, gene, String.format("%+.3f", step),
            chosen.reason());
        return gene;
    }

    /** Liste des pressions de sélection présentes à cet endroit. */
    private static List<Pressure> analyse(ServerLevel level, BlockPos pos, Cause cause, Entity victim,
                                          SpeciesProfile.Kind kind) {
        List<Pressure> out = new ArrayList<>();
        Holder<Biome> biome = level.getBiome(pos);
        float temp = biome.value().getBaseTemperature();
        boolean dry = !biome.value().hasPrecipitation();
        boolean nether = level.dimension() == Level.NETHER;
        boolean sky = level.canSeeSky(pos.above());
        boolean night = level.isNight();
        boolean water = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)
            || (victim != null && victim.isInWater());
        boolean mountain = biome.is(BiomeTags.IS_MOUNTAIN) || pos.getY() > 110;
        boolean jungle = biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_FOREST);

        if (kind.isPlant()) {
            if (temp > 1.0f || dry) out.add(new Pressure(GeneType.DENSITY, 1, "heat", 2));
            if (temp < 0.3f) out.add(new Pressure(GeneType.DENSITY, 1, "cold", 2));
            if (!sky || night) out.add(new Pressure(GeneType.GLOW, 1, "dark", 3));
            if (nether) out.add(new Pressure(GeneType.POTENCY, 1, "nether", 3));
            if (jungle || biome.value().hasPrecipitation()) out.add(new Pressure(GeneType.YIELD, 1, "rain", 2));
            if (cause == Cause.PLANT_BROKEN) {
                out.add(new Pressure(GeneType.GROWTH, 1, "harvest", 3));
                out.add(new Pressure(GeneType.YIELD, 1, "harvest", 1.5));
            }
            out.add(new Pressure(GeneType.PIGMENT, 1, "flowering", 1.5));
            out.add(new Pressure(GeneType.POTENCY, 1, "flowering", 1));
            return out;
        }

        if (nether) out.add(new Pressure(GeneType.FIRE_RESISTANCE, 1, "nether", 4));
        if (temp > 1.0f) {
            out.add(new Pressure(GeneType.FIRE_RESISTANCE, 1, "heat", 2));
            out.add(new Pressure(GeneType.SUN_RESISTANCE, 1, "heat", 3));
        }
        if (dry) out.add(new Pressure(GeneType.TOXIN_RESISTANCE, 1, "drought", 1));
        if (temp < 0.3f) {
            out.add(new Pressure(GeneType.FROST, 1, "cold", 3));
            out.add(new Pressure(GeneType.ARMOR, 1, "cold", 1.5));
        }
        if (!sky || night) {
            out.add(new Pressure(GeneType.GLOW, 1, "dark", 2));
            out.add(new Pressure(GeneType.CAMOUFLAGE, 1, "dark", 2));
        }
        if (water) {
            out.add(new Pressure(GeneType.AGILITY, 1, "water", 2));
            out.add(new Pressure(GeneType.SPEED, 1, "water", 1));
        }
        if (mountain) {
            out.add(new Pressure(GeneType.AGILITY, 1, "altitude", 2));
            out.add(new Pressure(GeneType.TOUGHNESS, 1, "altitude", 1));
        }
        if (jungle) {
            out.add(new Pressure(GeneType.CAMOUFLAGE, 1, "forest", 1.5));
            out.add(new Pressure(GeneType.VENOM, 1, "forest", 1.5));
        }
        int hostiles = level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(24)).size();
        if (hostiles > 6) {
            out.add(new Pressure(GeneType.ARMOR, 1, "crowd", 1.5));
            out.add(new Pressure(GeneType.VITALITY, 1, "crowd", 1.5));
        }
        if (cause == Cause.HUNTED) {
            // Ceux qui échappent aux chasseurs : plus rapides, plus discrets, plus coriaces
            out.add(new Pressure(GeneType.SPEED, 1, "hunted", 2.5));
            out.add(new Pressure(GeneType.CAMOUFLAGE, 1, "hunted", 2));
            out.add(new Pressure(GeneType.ARMOR, 1, "hunted", 1.5));
            out.add(new Pressure(GeneType.REGENERATION, 1, "hunted", 1));
            if (kind == SpeciesProfile.Kind.HOSTILE) out.add(new Pressure(GeneType.STRENGTH, 1, "hunted", 1.5));
        }
        return out;
    }
}
