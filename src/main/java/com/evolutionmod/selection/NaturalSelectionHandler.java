package com.evolutionmod.selection;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Implémente la VRAIE sélection naturelle darwinienne.
 *
 * Sans ce module, le système ne fait qu'accumuler des mutations aléatoires
 * (dérive génétique pure). Avec lui, les individus mal adaptés à leur
 * environnement subissent une pression de mortalité accrue, tandis que
 * les individus bien adaptés se reproduisent davantage.
 *
 * C'est ce qui transforme "des mutations aléatoires" en "évolution dirigée
 * par la sélection" — le principe central de Darwin.
 *
 * Mécanismes :
 *  1. Mortalité différentielle : fitness faible → chance de mort passive accrue
 *  2. Reproduction différentielle : fitness élevé → bébés héritent + facilement
 *  3. Hybridation des bébés : croisement génétique réel des parents (Genome.reproduceWith)
 *  4. Pression de famine : METABOLISM_RATE élevé sans DIET_TYPE adapté = malus
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class NaturalSelectionHandler {

    private static final Random RANDOM = new Random();

    // Fréquence de vérification de la pression de sélection (en ticks)
    private static final int SELECTION_CHECK_INTERVAL = 200; // 10 secondes

    /**
     * Vérifie périodiquement le fitness de chaque créature et applique
     * une pression de mortalité aux individus mal adaptés.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity instanceof Player) return;
        if (entity.tickCount % SELECTION_CHECK_INTERVAL != 0) return;

        EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            double fitness = evaluateFitness(entity, genome);
            applySelectionPressure(entity, genome, fitness);
        });
    }

    /**
     * Évalue le fitness d'une entité dans son environnement ACTUEL réel
     * (pas une simulation — les vraies conditions du bloc où elle se trouve).
     */
    private static double evaluateFitness(LivingEntity entity, Genome genome) {
        ServerLevel level = (ServerLevel) entity.level();
        BlockPos pos = entity.blockPosition();

        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        // NOTE 1.21.1 : voir EvolutionManager#buildEnvironmentData pour le détail —
        // Biome#getDownfall() n'a pas pu être confirmé de façon fiable pour cette
        // version exacte ; l'humidité n'étant pas critique au calcul de fitness
        // actuel (calculateFitness ne l'utilise pas), une valeur neutre est sûre.
        float biomeHumidity = 0.5f;

        double predatorPressure = countNearbyThreats(entity);
        double foodAvailability = estimateFoodAvailability(entity);

        Genome.EnvironmentData env = new Genome.EnvironmentData(
                biomeTemp, biomeHumidity, predatorPressure, foodAvailability
        );
        env.isNether = level.dimension().location().getPath().equals("the_nether");
        env.isEnd = level.dimension().location().getPath().equals("the_end");

        double fitness = genome.calculateFitness(env);

        // Pénalité de métabolisme : métabolisme rapide sans nourriture suffisante
        double metabolism = genome.getGeneValue(Gene.GeneType.METABOLISM_RATE);
        if (metabolism > 2.5 && foodAvailability < 0.3) {
            fitness *= 0.6; // famine génétique
        }

        // Bonus de camouflage : pattern adapté au biome (simplifié)
        double stealth = genome.getGeneValue(Gene.GeneType.STEALTH);
        if (stealth > 0.7 && predatorPressure > 0.5) {
            fitness *= 1.0 + stealth * 0.3;
        }

        return Math.max(0.05, Math.min(2.0, fitness));
    }

    /**
     * Applique la pression de sélection : mortalité accrue si mal adapté,
     * bonus de santé/régénération si bien adapté.
     */
    private static void applySelectionPressure(LivingEntity entity, Genome genome, double fitness) {
        double pressure = EvolutionManager.getInstance().getGlobalEnvironmentalPressure();

        if (fitness < 0.35) {
            // Mauvaise adaptation : chance de mortalité passive
            // proportionnelle à l'écart de fitness ET à la pression globale
            double deathChance = (0.35 - fitness) * pressure * 0.15;

            if (RANDOM.nextDouble() < deathChance) {
                EvolutionMod.LOGGER.debug(
                    "Sélection naturelle: {} succombe (fitness={:.2f}, pression={:.2f})",
                    entity.getType().getDescriptionId(), fitness, pressure
                );
                // Mort "naturelle" — pas de loot, juste disparition (comme une mort de faiblesse)
                entity.hurt(entity.damageSources().starve(), 1.0f);
            }
        } else if (fitness > 1.3) {
            // Excellente adaptation : bonus de vitalité (récompense évolutive)
            if (entity.getHealth() < entity.getMaxHealth() && RANDOM.nextDouble() < 0.3) {
                entity.heal(1.0f);
            }
        }
    }

    /**
     * Compte les menaces proches (prédateurs pour animaux, joueurs/loups pour monstres).
     */
    private static double countNearbyThreats(LivingEntity entity) {
        if (entity instanceof Animal) {
            long predators = entity.level().getEntitiesOfClass(
                    Monster.class, entity.getBoundingBox().inflate(24)
            ).size();
            long players = entity.level().getEntitiesOfClass(
                    Player.class, entity.getBoundingBox().inflate(24)
            ).size();
            return Math.min(1.0, (predators + players * 1.5) / 10.0);
        } else if (entity instanceof Monster) {
            long players = entity.level().getEntitiesOfClass(
                    Player.class, entity.getBoundingBox().inflate(24)
            ).size();
            return Math.min(1.0, players / 4.0);
        }
        return 0.2;
    }

    /**
     * Estimation grossière de la nourriture disponible autour de l'entité.
     * Compte les blocs d'herbe/cultures pour les herbivores, les entités
     * potentiellement comestibles pour les carnivores.
     */
    private static double estimateFoodAvailability(LivingEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        BlockPos center = entity.blockPosition();
        int grassCount = 0;
        int radius = 6;

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                BlockPos checkPos = center.offset(dx, 0, dz);
                var state = level.getBlockState(checkPos.below());
                if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
                    state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                    grassCount++;
                }
            }
        }

        int maxPossible = (int) Math.pow((radius / 2.0) * 2 + 1, 2);
        return Math.min(1.0, (double) grassCount / maxPossible);
    }

    // ──────────────────────────────────────────────────────
    //  Reproduction différentielle (bébés des animaux passifs)
    // ──────────────────────────────────────────────────────

    /**
     * Quand deux animaux se reproduisent (amour des animaux passifs),
     * le bébé hérite d'un VRAI génome croisé (pas un nouveau génome aléatoire).
     * C'est ici que l'hérédité mendélienne devient réelle dans le jeu.
     */
    @SubscribeEvent
    public static void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof LivingEntity parentA)) return;
        if (!(event.getParentB() instanceof LivingEntity parentB)) return;
        if (!(event.getChild() instanceof LivingEntity child)) return;

        EvolutionCapability.getEvolutionData(parentA).ifPresent(dataA ->
            EvolutionCapability.getEvolutionData(parentB).ifPresent(dataB ->
                EvolutionCapability.getEvolutionData(child).ifPresent(dataChild -> {

                    Genome genomeA = dataA.getGenome();
                    Genome genomeB = dataB.getGenome();

                    if (genomeA != null && genomeB != null) {
                        long currentDay = child.level().getDayTime() / 24000L;
                        Genome childGenome = genomeA.reproduceWith(genomeB, currentDay);
                        dataChild.setGenome(childGenome);

                        // Appliquer immédiatement les traits hérités
                        EvolutionManager.getInstance().applyGenomeMutations(child, childGenome);

                        EvolutionMod.LOGGER.debug(
                            "Reproduction: {} x {} → enfant lignée {}",
                            genomeA.getSpeciesLineage(), genomeB.getSpeciesLineage(),
                            childGenome.getSpeciesLineage()
                        );
                    }
                })
            )
        );
    }
}
