package com.evolutionmod.events;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Gère l'expression phénotypique des gènes en temps réel.
 *
 * Traduit les valeurs génomiques en comportements et effets concrets :
 * - venin lors des attaques
 * - bioluminescence
 * - résistances élémentaires
 * - comportements spéciaux
 * - effets visuels (NBT de couleur pour le renderer)
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class EntityEvolutionHandler {

    private static final Random RANDOM = new Random();

    /**
     * À la naissance/spawn : appliquer le génome initial et synchroniser les effets.
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (event.getLevel().isClientSide()) return;

        EvolutionCapability.getEvolutionData(livingEntity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome != null) {
                applyInitialTraits(livingEntity, genome);
            }
        });
    }

    /**
     * Lors d'une attaque : appliquer les effets génétiques offensifs.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof LivingEntity attacker) {
            EvolutionCapability.getEvolutionData(attacker).ifPresent(evoData -> {
                Genome genome = evoData.getGenome();
                if (genome == null) return;

                LivingEntity target = event.getEntity();

                // VENIN : si le gène de venin est élevé
                double venomLevel = genome.getGeneValue(Gene.GeneType.VENOMOUS);
                if (venomLevel > 2.0) {
                    int poisonAmplifier = (int) Math.min(4, venomLevel / 2.0);
                    int duration = (int) (venomLevel * 20 * 3); // secondes * ticks
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, poisonAmplifier));
                }

                // FAIBLESSE APPLIQUÉE (entité très forte affaiblit la cible)
                double strength = genome.getGeneValue(Gene.GeneType.STRENGTH);
                if (strength > 7.0 && RANDOM.nextDouble() < 0.3) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                }

                // GÈLE (résistance au froid extrême = peut congeler)
                double coldRes = genome.getGeneValue(Gene.GeneType.COLD_RESISTANCE);
                if (coldRes > 0.9 && RANDOM.nextDouble() < 0.2) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
            });
        }
    }

    /**
     * Lors d'une mort : possibilité de loot génétique spécial.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity entity = event.getEntity();
        EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            // Entités très mutées = drop plus de loot rare
            int totalMutations = genome.getTotalMutations();
            if (totalMutations > 20) {
                // Spawner du loot génétique spécial (ex: cristal génomique)
                spawnGeneticLoot(entity, genome);
            }
        });
    }

    /**
     * Quand un creeper explose, son rayon de souffle réel reflète le gène
     * EXPLOSION_POWER de son génome au lieu du rayon fixe vanilla (3.0).
     * Sans ce hook, EXPLOSION_POWER serait un gène stocké et affiché par le
     * Scanner/Cristal mais sans aucun effet observable en jeu — exactement
     * le défaut qu'avait FRUIT_EFFECT avant le branchement de MutantFruitItem.
     */
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide()) return;

        var explosion = event.getExplosion();
        // NOTE 1.21.1 : Explosion#getExploder() a été retiré. getIndirectSourceEntity()
        // retourne directement un LivingEntity — c'est l'entité "responsable" de
        // l'explosion (le creeper lui-même), par opposition à une éventuelle source
        // directe différente (ex: un projectile qu'il aurait lancé).
        if (!(explosion.getIndirectSourceEntity() instanceof Creeper creeper)) return;

        EvolutionCapability.getEvolutionData(creeper).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            double explosionPower = genome.getGeneValue(Gene.GeneType.EXPLOSION_POWER);
            if (explosionPower <= 1.0) return; // valeur de base, comportement vanilla inchangé

            // Le rayon interne d'Explosion n'est pas modifiable après construction en 1.21.1
            // sans mixin ; on simule le surplus de souffle par des dégâts de zone
            // additionnels proportionnels au surplus de puissance génétique, ce qui reste
            // observable et cohérent sans dépendance à un outil de transformation de bytecode.
            double bonusRadius = explosionPower - 1.0;
            creeper.level().getEntitiesOfClass(
                LivingEntity.class,
                creeper.getBoundingBox().inflate(3.0 + bonusRadius)
            ).forEach(victim -> {
                if (victim == creeper) return;
                double dist = victim.distanceTo(creeper);
                if (dist < 3.0 + bonusRadius) {
                    float bonusDamage = (float) (bonusRadius * (1.0 - dist / (3.0 + bonusRadius)) * 3.0);
                    if (bonusDamage > 0.5f) {
                        victim.hurt(victim.damageSources().explosion(explosion), bonusDamage);
                    }
                }
            });
        });
    }

    /**
     * Chaque tick de vie : appliquer les effets passifs génétiques.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.tickCount % 40 != 0) return; // Toutes les 2 secondes

        EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            // RÉGÉNÉRATION GÉNÉTIQUE
            double regenRate = genome.getGeneValue(Gene.GeneType.REGENERATION);
            if (regenRate > 0.5 && entity.getHealth() < entity.getMaxHealth()) {
                int regenAmplifier = (int) Math.min(3, regenRate);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, regenAmplifier, false, false));
            }

            // BIOLUMINESCENCE -> léger halo lumineux PONCTUEL, jamais permanent.
            // Un glow permanent casserait la furtivité de base du jeu en survie pour
            // toute créature mutée (visible à travers les murs en permanence) — au
            // lieu de ça, on émet juste des particules ambiantes de temps en temps
            // (voir GeneticEntityRenderer côté client) et on laisse l'apparence
            // réelle (texture/couleur) porter l'information visuelle, pas un glow.
            // Jamais appliqué au joueur de toute façon (pas de RenderLayer dédié).

            // RÉSISTANCE AU FEU -> ne brûle pas
            double fireRes = genome.getGeneValue(Gene.GeneType.FIRE_RESISTANCE);
            if (fireRes > 0.8 && entity.isOnFire()) {
                entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
            }

            // RÉSISTANCE AU POISON
            double poisonRes = genome.getGeneValue(Gene.GeneType.POISON_RESISTANCE);
            if (poisonRes > 0.7 && entity.hasEffect(MobEffects.POISON)) {
                entity.removeEffect(MobEffects.POISON);
            }

            // VISION NOCTURNE (yeux mutés)
            double eyeType = genome.getGeneValue(Gene.GeneType.EYE_TYPE);
            if (eyeType > 2.5 && entity instanceof Monster monster) {
                // Les yeux IR voient dans le noir parfaitement
                // (pas d'effet joueur, mais influence le targetting)
            }

            // ESCALADE GÉNÉTIQUE (stocker dans NBT pour le mouvement)
            double climbing = genome.getGeneValue(Gene.GeneType.CLIMBING);
            if (climbing > 0.7 && entity.horizontalCollision) {
                entity.setOnGround(false);
                // Note: Pour un vrai climbing il faudrait un mixin
                // mais on peut simuler via addEffect de vitesse verticale
            }
        });
    }

    /**
     * Résistances aux dégâts basées sur les gènes.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            float damage = event.getAmount();
            DamageSource source = event.getSource();

            // Résistance au feu
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                double fireRes = genome.getGeneValue(Gene.GeneType.FIRE_RESISTANCE);
                event.setAmount(damage * (float)(1.0 - fireRes * 0.9));
            }

            // Résistance générale (cuirasse biologique)
            double defense = genome.getGeneValue(Gene.GeneType.DEFENSE);
            if (defense > 0.2) {
                event.setAmount(damage * (float)(1.0 - defense * 0.6));
            }

            // Taille du corps = plus de PV mais plus gros = plus facile à toucher
            // (jamais appliqué au joueur : avec les bornes serrées 0.85-1.25 c'est
            // de toute façon inatteignable, mais on exclut explicitement pour ne
            // jamais surprendre un joueur avec une esquive aléatoire inexpliquée)
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                double bodySize = genome.getGeneValue(Gene.GeneType.BODY_SIZE);
                if (bodySize < 0.5) {
                    // Très petite entité = difficile à toucher, 20% de chance d'esquiver
                    if (RANDOM.nextDouble() < 0.2) {
                        event.setAmount(0);
                    }
                }
            }
        });
    }

    // =================== HELPERS ===================

    private static void applyInitialTraits(LivingEntity entity, Genome genome) {
        boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;

        // Marquer la couleur dans les tags NBT pour le renderer client.
        // Inutile et invisible pour le joueur (pas de RenderLayer custom sur le
        // modèle joueur dans ce mod), donc on ne le fait que pour les mobs/plantes.
        if (!isPlayer) {
            CompoundTagHelper.setGeneticData(entity, genome);
        }

        // Taille : pour le joueur, on borne beaucoup plus strictement (0.85–1.25x)
        // qu'un mob (0.1–5x) — un changement de taille drastique et soudain en
        // survie serait injouable (hitbox/caméra cassées) plutôt qu'amusant.
        double bodySize = genome.getGeneValue(Gene.GeneType.BODY_SIZE);
        var scaleAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scaleAttr != null) {
            double clamped = isPlayer
                ? Math.max(0.85, Math.min(1.25, bodySize))
                : Math.max(0.1, Math.min(5.0, bodySize));
            scaleAttr.setBaseValue(clamped);
        }

        // Santé initiale
        double maxHp = genome.getGeneValue(Gene.GeneType.HEALTH_MAX);
        if (maxHp > 1) {
            var hpAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                double clampedHp = isPlayer
                    ? Math.max(10.0, Math.min(40.0, maxHp))  // 5 à 20 cœurs, jamais moins/plus
                    : Math.max(1.0, Math.min(500.0, maxHp));
                hpAttr.setBaseValue(clampedHp);
                // Ne JAMAIS forcer setHealth() sur un joueur déjà vivant : ça le soignerait
                // ou le blesserait instantanément à chaque connexion, ce qui est punitif
                // et n'a aucun rapport avec le gameplay de survie attendu.
                if (!isPlayer) {
                    entity.setHealth((float) hpAttr.getValue());
                }
            }
        }

        // Vol par gravité réduite : jamais pour le joueur (briserait toute la
        // survie — chute, nage, combat aérien — pour un effet cosmétique mineur).
        if (!isPlayer) {
            double flight = genome.getGeneValue(Gene.GeneType.FLIGHT);
            if (flight > 1.5) {
                var gravAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
                if (gravAttr != null) {
                    gravAttr.setBaseValue(0.02);
                }
            }
        }
    }

    /**
     * Utilise le vrai item GeneticCrystalItem (ModItems.GENETIC_CRYSTAL) plutôt
     * qu'un placeholder vanilla — le mod possède désormais son propre cristal
     * génomique avec interactions de séquençage/injection, donc le loot de mort
     * doit produire ce même item plutôt qu'une AMETHYST_SHARD générique.
     */
    private static void spawnGeneticLoot(LivingEntity entity, Genome genome) {
        net.minecraft.world.item.ItemStack crystal = new net.minecraft.world.item.ItemStack(
            com.evolutionmod.items.ModItems.GENETIC_CRYSTAL.get()
        );

        com.evolutionmod.util.ItemNbt.update(crystal, itemNbt -> {
            itemNbt.put("StoredGenome", genome.toNBT());
            itemNbt.putString("SourceEntity", entity.getType().getDescriptionId());
            itemNbt.putInt("SequencedMutations", genome.getTotalMutations());
        });

        crystal.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal(
                "§d§lCristal Génomique §7[" + genome.getTotalMutations() + " mutations]"
            ));

        entity.spawnAtLocation(crystal);
    }

    /**
     * Utilitaire pour stocker les données génétiques dans les NBT de l'entité.
     */
    public static class CompoundTagHelper {
        public static void setGeneticData(LivingEntity entity, Genome genome) {
            try {
                net.minecraft.nbt.CompoundTag entityData = entity.getPersistentData();
                entityData.put("EvolutionGenome", genome.toNBT());

                // Stocker la couleur pour le renderer
                double hue = genome.getGeneValue(Gene.GeneType.COLOR_HUE);
                double sat = genome.getGeneValue(Gene.GeneType.COLOR_SATURATION);
                double bright = genome.getGeneValue(Gene.GeneType.COLOR_BRIGHTNESS);
                int rgb = hsvToRgb((float)(hue/360.0f), (float)sat, (float)Math.max(0.3, bright));

                entityData.putInt("GeneticColorRGB", rgb);
                entityData.putFloat("GeneticScale", (float)genome.getGeneValue(Gene.GeneType.BODY_SIZE));
                entityData.putBoolean("GeneticGlow", genome.getGeneValue(Gene.GeneType.BIOLUMINESCENCE) > 4.0);

            } catch (Exception e) {
                EvolutionMod.LOGGER.warn("Erreur setGeneticData: {}", e.getMessage());
            }
        }

        private static int hsvToRgb(float h, float s, float v) {
            int rgb = java.awt.Color.HSBtoRGB(h, s, v);
            return rgb & 0xFFFFFF;
        }
    }
}
