package com.evolutionmod.events;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.items.MutantMeatItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tous les événements de jeu « communs » liés à l'expression des gènes et
 * à la sélection naturelle.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public final class CommonEvents {

    private CommonEvents() {}

    // ───────────────────────── Horloge ─────────────────────────

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 != 0) return; // une vérification par seconde suffit
        EvolutionManager.tick(level.getServer());
        PlantEvents.processPending(level.getServer());
    }

    // ───────────────────────── Naissance / synchro ─────────────────────────

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living) {
            EvolutionManager.onEntityJoin(living);
            if (living instanceof ServerPlayer sp) EvolutionManager.syncTo(sp, sp);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer sp) {
            EvolutionManager.syncTo(sp, living);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Genome g = EvolutionCapability.genome(sp);
            if (g != null) com.evolutionmod.core.Phenotype.apply(sp, g, false);
            EvolutionManager.syncTo(sp, sp);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) EvolutionManager.syncTo(sp, sp);
    }

    // ───────────────────────── Effets passifs ─────────────────────────

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.isAlive()) return;
        if ((entity.tickCount + entity.getId()) % 40 != 0) return; // toutes les 2 s, étalé
        Genome g = EvolutionCapability.genome(entity);
        if (g == null) return;

        double regen = g.get(GeneType.REGENERATION);
        if (regen > 0.1 && entity.getHealth() < entity.getMaxHealth()) {
            boolean player = entity instanceof Player;
            entity.heal((float) (regen * (player ? 0.5 : 1.5)));
        }

        double toxin = g.get(GeneType.TOXIN_RESISTANCE);
        if (toxin > 0.1 && entity.hasEffect(MobEffects.POISON) && entity.getRandom().nextDouble() < toxin * 0.6) {
            entity.removeEffect(MobEffects.POISON);
        }

        double sun = g.get(GeneType.SUN_RESISTANCE);
        if (sun > 0.3 && entity.isOnFire() && entity.level().isDay()
            && entity.getType().is(EntityTypeTags.UNDEAD) && entity.getRandom().nextDouble() < sun) {
            entity.clearFire();
        }

        double fire = g.get(GeneType.FIRE_RESISTANCE);
        if (fire > 0.85 && entity.isOnFire() && !(entity instanceof Player)) {
            entity.clearFire();
        }

        double camo = g.get(GeneType.CAMOUFLAGE);
        if (camo > 0.35 && !(entity instanceof Player) && !entity.hasEffect(MobEffects.INVISIBILITY)
            && entity.getRandom().nextDouble() < camo * 0.08) {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (40 + camo * 80), 0, false, false));
        }
    }

    // ───────────────────────── Combat ─────────────────────────

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // Défense de la cible
        Genome tg = EvolutionCapability.genome(target);
        if (tg != null && source.is(DamageTypeTags.IS_FIRE)) {
            amount *= (float) (1.0 - tg.get(GeneType.FIRE_RESISTANCE) * 0.85);
        }
        event.setAmount(amount);

        // Armes biologiques de l'attaquant (attaque directe uniquement)
        if (source.getDirectEntity() instanceof LivingEntity attacker && attacker != target) {
            Genome ag = EvolutionCapability.genome(attacker);
            if (ag != null) {
                double venom = ag.get(GeneType.VENOM);
                if (venom > 0.12) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON,
                        (int) (40 + venom * 160), venom > 0.7 ? 1 : 0), attacker);
                }
                double frost = ag.get(GeneType.FROST);
                if (frost > 0.15) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        (int) (30 + frost * 100), frost > 0.7 ? 1 : 0), attacker);
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40,
                        target.getTicksFrozen() + (int) (frost * 60)));
                }
            }
        }

        // Sélection : un mob qui blesse un joueur a « réussi »
        if (target instanceof Player && amount > 0 && source.getEntity() instanceof LivingEntity mob
            && !(mob instanceof Player)) {
            EvolutionManager.selectFor(mob, 0.02);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) return;
        Genome g = EvolutionCapability.genome(entity);
        if (g == null) return;

        boolean byPlayer = event.getSource().getEntity() instanceof Player;
        EvolutionManager.recordDeath(entity, byPlayer);
        if (byPlayer) EvolutionManager.selectAgainst(entity, 0.012);
        if (byPlayer && entity.level() instanceof ServerLevel sl) {
            com.evolutionmod.core.Adaptation.roll(sl, entity.blockPosition(), g.species(),
                com.evolutionmod.core.Adaptation.Cause.HUNTED, entity);
        }

        // Victoire d'un mob sur un joueur : très forte sélection positive
        // (géré dans PlayerDeath ci-dessous).

        if (!EvoConfig.geneticDrops() || !byPlayer) return;
        double div = 0;
        var server = entity.getServer();
        if (server != null) {
            var record = com.evolutionmod.world.EvolutionSavedData.get(server).get(g.species());
            if (record != null) div = g.divergenceFrom(record.origin());
        }
        // Cristal génomique : plus la créature est mutée, plus il est probable
        double crystalChance = 0.015 + Math.min(0.25, div * 0.8 + g.totalMutations() * 0.01);
        if (entity.getRandom().nextDouble() < crystalChance) {
            ItemStack crystal = GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()), g);
            entity.spawnAtLocation(crystal);
        }
        // Viande mutante pour les animaux ayant divergé
        if (entity instanceof Animal && div > 0.05 && entity.getRandom().nextDouble() < 0.5 + div) {
            entity.spawnAtLocation(MutantMeatItem.fromGenome(g));
        }
    }

    @SubscribeEvent
    public static void onPlayerKilled(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getSource().getEntity() instanceof LivingEntity killer && !(killer instanceof Player)) {
            EvolutionManager.selectFor(killer, 0.08);
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getChild() == null || event.getParentA() == null || event.getParentB() == null) return;
        if (event.getParentA().level().isClientSide()) return;
        EvolutionManager.onBreed(event.getParentA(), event.getParentB(), event.getChild());
    }
}
