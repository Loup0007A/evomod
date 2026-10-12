package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.NotifyMsg;
import com.evolutionmod.network.SyncGenomeMsg;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.HistoryEntry;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cœur de la simulation évolutive (côté serveur uniquement).
 *
 * <h2>Modèle</h2>
 * <ul>
 *   <li>Chaque espèce possède un <b>gabarit</b> (génome de population) qui dérive
 *       un peu chaque jour et qui est tiré par la <b>sélection</b> : les individus
 *       qui blessent un joueur, survivent longtemps ou se reproduisent attirent
 *       le gabarit vers eux ; ceux que les joueurs tuent l'en éloignent.</li>
 *   <li>Chaque nouvel individu naît du gabarit (avec une variation personnelle),
 *       puis accumule ses propres mutations au fil des jours.</li>
 *   <li>Quand le gabarit s'éloigne assez de l'ancêtre vanilla, une nouvelle
 *       lignée (spéciation) est nommée et annoncée.</li>
 * </ul>
 */
public final class EvolutionManager {

    private EvolutionManager() {}

    private static final RandomSource RANDOM = RandomSource.create();

    // ───────────────────────── Temps ─────────────────────────

    public static long currentDay(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L;
    }

    /** Pression environnementale : monte doucement de 0.15 vers 1.0 au fil des cycles. */
    public static double pressure(long cycles) {
        return 0.15 + 0.85 * (1.0 - Math.exp(-cycles / 90.0));
    }

    public static double pressure(MinecraftServer server) {
        return pressure(EvolutionSavedData.get(server).cycles());
    }

    // ───────────────────────── Naissance ─────────────────────────

    /**
     * Appelé quand une entité vivante arrive dans un monde serveur : création du
     * génome si nécessaire, puis expression des gènes.
     */
    public static void onEntityJoin(LivingEntity entity) {
        MinecraftServer server = entity.getServer();
        if (server == null) return;

        EvolutionCapability.get(entity).ifPresent(data -> {
            if (data.isLegacy()) {
                Phenotype.repairLegacyAttributes(entity);
                data.clearLegacy();
            }

            if (!SpeciesProfile.isEvolvable(entity)) {
                if (data.genome() != null) {
                    data.setGenome(null);
                    Phenotype.clear(entity);
                }
                return;
            }

            boolean fresh = false;
            if (data.genome() == null) {
                EvolutionSavedData saved = EvolutionSavedData.get(server);
                long day = currentDay(server);
                SpeciesProfile profile = SpeciesProfile.forEntity(entity);
                boolean newSpecies = saved.get(profile.id()) == null;
                SpeciesRecord record = saved.getOrCreate(profile, day);
                if (newSpecies && !(entity instanceof Player)) {
                    saved.addHistory(new HistoryEntry(day, HistoryEntry.Type.NEW_SPECIES, profile.id(), "", 0));
                }

                Genome genome;
                if (entity instanceof Player) {
                    genome = Genome.fromProfile(profile); // le joueur part toujours de l'humain vanilla
                } else {
                    genome = record.template().spawnIndividual(RANDOM, 0.035);
                    record.addBirth();
                }
                data.setGenome(genome);
                data.setBornDay(day);
                data.setLastCycle(saved.cycles());
                saved.setDirty();
                fresh = true;
            }
            Phenotype.apply(entity, data.genome(), fresh);
        });
    }

    // ───────────────────────── Cycle quotidien ─────────────────────────

    /** Vérifie à chaque tick de l'overworld si un ou plusieurs jours sont passés. */
    public static void tick(MinecraftServer server) {
        EvolutionSavedData data = EvolutionSavedData.get(server);
        long day = currentDay(server);
        long last = data.lastProcessedDay();

        if (last < 0 || day < last) {
            // Premier lancement, ou temps remonté avec /time set : on se recale sans évoluer.
            data.setLastProcessedDay(day);
            return;
        }
        if (day == last) return;

        long missed = day - last;
        int cycles = (int) Math.min(missed, EvoConfig.maxCatchUpCycles());
        for (int i = 0; i < cycles; i++) {
            runCycle(server, day - cycles + 1 + i);
        }
        data.setLastProcessedDay(day);
    }

    /** Exécute un cycle d'évolution complet. */
    public static void runCycle(MinecraftServer server, long day) {
        EvolutionSavedData data = EvolutionSavedData.get(server);
        data.addCycle();
        double pressure = pressure(data.cycles());
        double rate = EvoConfig.mutationRate();
        long mutationsBefore = data.totalMutations();

        // 1. Les individus de chaque monde chargé
        Map<String, Integer> population = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
                evolveIndividual(data, living, day, pressure, rate, population);
            }
        }

        // 2. Les espèces (gabarits de population)
        if (EvoConfig.plantEvolution()) {
            for (String p : SpeciesProfile.TREE_SPECIES) data.getOrCreate(SpeciesProfile.forPlant(p), day);
            for (String p : SpeciesProfile.CROP_SPECIES) data.getOrCreate(SpeciesProfile.forPlant(p), day);
        }
        for (SpeciesRecord record : data.species().values()) {
            if (record.kind() == SpeciesProfile.Kind.PLAYER) {
                record.setPopulation(population.getOrDefault(record.id(), 0));
                continue;
            }
            if (record.kind().isPlant() && !EvoConfig.plantEvolution()) continue;

            double drift = record.kind().isPlant() ? 0.30 : 0.20;
            List<GeneType> changed = record.template().mutate(RANDOM,
                drift * rate * (0.5 + pressure), 0.03);
            data.addMutations(changed.size());
            if (!record.kind().isPlant()) record.setPopulation(population.getOrDefault(record.id(), 0));
            record.recordDivergenceSample();

            checkSpeciation(server, data, record, day);
        }
        data.recordPressure(pressure);
        data.setDirty();

        long newMutations = data.totalMutations() - mutationsBefore;
        EvolutionMod.LOGGER.info("Cycle d'évolution #{} (jour {}) : {} mutations, pression {}",
            data.cycles(), day, newMutations, String.format("%.2f", pressure));

        if (EvoConfig.dailyReport()) {
            NotifyMsg msg = new NotifyMsg(NotifyMsg.Kind.DAILY, "evolutionmod.notify.daily",
                List.of(String.valueOf(day), String.valueOf(newMutations),
                    String.format("%.0f", pressure * 100)));
            EvoNetwork.sendToAll(server, msg);
        }
    }

    private static void evolveIndividual(EvolutionSavedData data, LivingEntity entity, long day,
                                         double pressure, double rate, Map<String, Integer> population) {
        EvolutionCapability.get(entity).ifPresent(cap -> {
            Genome genome = cap.genome();
            if (genome == null || cap.lastCycle() == data.cycles()) return;
            cap.setLastCycle(data.cycles());

            boolean player = entity instanceof Player;
            population.merge(genome.species(), 1, Integer::sum);

            double chance = (player ? 0.05 : 0.07) * rate * (0.5 + pressure);
            List<GeneType> changed = genome.mutate(RANDOM, chance, 0.05);
            data.addMutations(changed.size());

            SpeciesRecord record = data.get(genome.species());
            // Survivre un jour entier est un petit succès évolutif
            if (record != null && !player && cap.bornDay() >= 0 && day - cap.bornDay() >= 1) {
                record.template().pullTowards(genome, 0.006 * EvoConfig.selectionStrength());
                record.addSuccess();
            }

            if (!changed.isEmpty()) {
                Phenotype.apply(entity, genome, false);
                sync(entity);
                if (player && entity instanceof ServerPlayer sp) {
                    GeneType g = changed.get(0);
                    EvoNetwork.sendToPlayer(sp, new NotifyMsg(NotifyMsg.Kind.PLAYER,
                        "evolutionmod.notify.player_mutation", List.of("#gene:" + g.name())));
                }
            }
        });
    }

    /** Nomme une nouvelle lignée si l'espèce a assez divergé. */
    private static boolean checkSpeciation(MinecraftServer server, EvolutionSavedData data,
                                           SpeciesRecord record, long day) {
        if (record.targetLevel() <= record.lineages().size()) return false;
        String name = record.speciate(data.seed());
        data.addSpeciation();
        data.addHistory(new HistoryEntry(day, HistoryEntry.Type.SPECIATION, record.id(), name, record.divergence()));
        if (EvoConfig.announceSpeciation()) {
            EvoNetwork.sendToAll(server, new NotifyMsg(NotifyMsg.Kind.SPECIATION,
                "evolutionmod.notify.speciation", List.of("#species:" + record.id(), name)));
            Component chat = Component.translatable("evolutionmod.chat.speciation",
                    speciesComponent(record.id()), Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.GOLD);
            server.getPlayerList().broadcastSystemMessage(chat, false);
        }
        return true;
    }

    public static Component speciesComponent(String id) {
        if (id.startsWith("plant:")) {
            return Component.translatable("evolutionmod.species." + id.substring(6));
        }
        return net.minecraft.world.entity.EntityType.byString(id)
            .<Component>map(t -> t.getDescription())
            .orElse(Component.literal(id));
    }

    // ───────────────────────── Sélection ─────────────────────────

    /** Un individu a réussi (blessé un joueur, s'est reproduit...) : le gabarit se rapproche de lui. */
    public static void selectFor(LivingEntity entity, double weight) {
        adjustTemplate(entity, weight, true);
    }

    /** Un individu a échoué (tué par un joueur) : le gabarit s'en éloigne légèrement. */
    public static void selectAgainst(LivingEntity entity, double weight) {
        adjustTemplate(entity, -weight, false);
    }

    private static void adjustTemplate(LivingEntity entity, double weight, boolean success) {
        MinecraftServer server = entity.getServer();
        if (server == null || entity instanceof Player) return;
        Genome genome = EvolutionCapability.genome(entity);
        if (genome == null) return;
        EvolutionSavedData data = EvolutionSavedData.get(server);
        SpeciesRecord record = data.get(genome.species());
        if (record == null) return;
        record.template().pullTowards(genome, weight * EvoConfig.selectionStrength());
        if (success) record.addSuccess();
        data.setDirty();
    }

    public static void recordDeath(LivingEntity entity, boolean byPlayer) {
        MinecraftServer server = entity.getServer();
        if (server == null || entity instanceof Player) return;
        Genome genome = EvolutionCapability.genome(entity);
        if (genome == null) return;
        EvolutionSavedData data = EvolutionSavedData.get(server);
        SpeciesRecord record = data.get(genome.species());
        if (record != null) {
            record.addDeath(byPlayer);
            data.setDirty();
        }
    }

    /** Reproduction : l'enfant reçoit un vrai croisement génétique de ses parents. */
    public static void onBreed(LivingEntity parentA, LivingEntity parentB, LivingEntity child) {
        Genome a = EvolutionCapability.genome(parentA);
        Genome b = EvolutionCapability.genome(parentB);
        if (a == null || b == null || !a.species().equals(b.species())) return;
        MinecraftServer server = parentA.getServer();
        if (server == null) return;

        Genome kid = Genome.crossover(a, b, RANDOM);
        long day = currentDay(server);
        EvolutionCapability.get(child).ifPresent(cap -> {
            cap.setGenome(kid);
            cap.setBornDay(day);
            cap.setLastCycle(EvolutionSavedData.get(server).cycles());
        });

        EvolutionSavedData data = EvolutionSavedData.get(server);
        SpeciesRecord record = data.get(a.species());
        if (record != null) {
            record.addBirth();
            // Les reproducteurs transmettent leurs gènes : sélection (souvent artificielle !)
            record.template().pullTowards(a, 0.02 * EvoConfig.selectionStrength());
            record.template().pullTowards(b, 0.02 * EvoConfig.selectionStrength());
            record.addSuccess();
            data.setDirty();
        }
    }

    // ───────────────────────── Synchronisation ─────────────────────────

    public static void sync(LivingEntity entity) {
        Genome genome = EvolutionCapability.genome(entity);
        EvoNetwork.sendToTrackingAndSelf(entity,
            new SyncGenomeMsg(entity.getId(), genome == null ? new CompoundTag() : genome.save()));
    }

    public static void syncTo(ServerPlayer player, LivingEntity entity) {
        Genome genome = EvolutionCapability.genome(entity);
        if (genome == null) return;
        EvoNetwork.sendToPlayer(player, new SyncGenomeMsg(entity.getId(), genome.save()));
    }

    // ───────────────────────── Données pour les écrans ─────────────────────────

    /** Instantané complet du monde pour le Journal d'évolution. */
    public static CompoundTag buildJournal(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        EvolutionSavedData data = EvolutionSavedData.get(server);
        CompoundTag tag = new CompoundTag();
        tag.putLong("Day", currentDay(server));
        tag.putLong("Cycles", data.cycles());
        tag.putLong("Mutations", data.totalMutations());
        tag.putInt("Speciations", data.speciations());
        tag.putDouble("Pressure", pressure(data.cycles()));
        int[] p = new int[data.pressureHistory().size()];
        for (int i = 0; i < p.length; i++) p[i] = Float.floatToIntBits(data.pressureHistory().get(i));
        tag.putIntArray("PressureHistory", p);

        ListTag species = new ListTag();
        for (SpeciesRecord r : data.species().values()) {
            if (r.kind() == SpeciesProfile.Kind.PLAYER) continue;
            species.add(r.toClientTag());
        }
        tag.put("Species", species);

        ListTag history = new ListTag();
        for (HistoryEntry e : data.history()) history.add(e.save());
        tag.put("History", history);

        tag.putBoolean("PlayerEvolution", EvoConfig.playerEvolution());
        Genome mine = EvolutionCapability.genome(player);
        if (mine != null) tag.put("Player", mine.save());
        return tag;
    }

    /** Résumé de l'espèce d'un génome pour l'écran de scan. */
    public static CompoundTag speciesSummary(MinecraftServer server, Genome genome) {
        SpeciesRecord record = EvolutionSavedData.get(server).get(genome.species());
        return record == null ? new CompoundTag() : record.toClientTag();
    }
}
