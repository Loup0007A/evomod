package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public enum TrialSpawnerState implements StringRepresentable {
   INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmission.NONE, -1.0, false),
   WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, 200.0, true),
   ACTIVE("active", 8, TrialSpawnerState.ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
   WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
   EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
   COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

   private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
   private static final int TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0F);
   private final String name;
   private final int lightLevel;
   private final double spinningMobSpeed;
   private final TrialSpawnerState.ParticleEmission particleEmission;
   private final boolean isCapableOfSpawning;

   private TrialSpawnerState(
      final String pName,
      final int pLightLevel,
      final TrialSpawnerState.ParticleEmission pParticleEmission,
      final double pSpinningMobSpeed,
      final boolean pIsCapableOfSpawning
   ) {
      this.name = pName;
      this.lightLevel = pLightLevel;
      this.particleEmission = pParticleEmission;
      this.spinningMobSpeed = pSpinningMobSpeed;
      this.isCapableOfSpawning = pIsCapableOfSpawning;
   }

   TrialSpawnerState tickAndGetNext(BlockPos pPos, TrialSpawner pSpawner, ServerLevel pLevel) {
      TrialSpawnerData $$3 = pSpawner.getData();
      TrialSpawnerConfig $$4 = pSpawner.getConfig();

      return switch (this) {
         case INACTIVE -> $$3.getOrCreateDisplayEntity(pSpawner, pLevel, WAITING_FOR_PLAYERS) == null ? this : WAITING_FOR_PLAYERS;
         case WAITING_FOR_PLAYERS -> {
            if (!pSpawner.canSpawnInLevel(pLevel)) {
               $$3.reset();
               yield this;
            } else if (!$$3.hasMobToSpawn(pSpawner, pLevel.random)) {
               yield INACTIVE;
            } else {
               $$3.tryDetectPlayers(pLevel, pPos, pSpawner);
               yield $$3.detectedPlayers.isEmpty() ? this : ACTIVE;
            }
         }
         case ACTIVE -> {
            if (!pSpawner.canSpawnInLevel(pLevel)) {
               $$3.reset();
               yield WAITING_FOR_PLAYERS;
            } else if (!$$3.hasMobToSpawn(pSpawner, pLevel.random)) {
               yield INACTIVE;
            } else {
               int $$5 = $$3.countAdditionalPlayers(pPos);
               $$3.tryDetectPlayers(pLevel, pPos, pSpawner);
               if (pSpawner.isOminous()) {
                  this.spawnOminousOminousItemSpawner(pLevel, pPos, pSpawner);
               }

               if ($$3.hasFinishedSpawningAllMobs($$4, $$5)) {
                  if ($$3.haveAllCurrentMobsDied()) {
                     $$3.cooldownEndsAt = pLevel.getGameTime() + (long)pSpawner.getTargetCooldownLength();
                     $$3.totalMobsSpawned = 0;
                     $$3.nextMobSpawnsAt = 0L;
                     yield WAITING_FOR_REWARD_EJECTION;
                  }
               } else if ($$3.isReadyToSpawnNextMob(pLevel, $$4, $$5)) {
                  pSpawner.spawnMob(pLevel, pPos).ifPresent(p_327378_ -> {
                     $$3.currentMobs.add(p_327378_);
                     $$3.totalMobsSpawned++;
                     $$3.nextMobSpawnsAt = pLevel.getGameTime() + (long)$$4.ticksBetweenSpawn();
                     $$4.spawnPotentialsDefinition().getRandom(pLevel.getRandom()).ifPresent(p_327384_ -> {
                        $$3.nextSpawnData = Optional.of(p_327384_.data());
                        pSpawner.markUpdated();
                     });
                  });
               }

               yield this;
            }
         }
         case WAITING_FOR_REWARD_EJECTION -> {
            if ($$3.isReadyToOpenShutter(pLevel, 40.0F, pSpawner.getTargetCooldownLength())) {
               pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
               yield EJECTING_REWARD;
            } else {
               yield this;
            }
         }
         case EJECTING_REWARD -> {
            if (!$$3.isReadyToEjectItems(pLevel, (float)TIME_BETWEEN_EACH_EJECTION, pSpawner.getTargetCooldownLength())) {
               yield this;
            } else if ($$3.detectedPlayers.isEmpty()) {
               pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
               $$3.ejectingLootTable = Optional.empty();
               yield COOLDOWN;
            } else {
               if ($$3.ejectingLootTable.isEmpty()) {
                  $$3.ejectingLootTable = $$4.lootTablesToEject().getRandomValue(pLevel.getRandom());
               }

               $$3.ejectingLootTable.ifPresent(p_327391_ -> pSpawner.ejectReward(pLevel, pPos, (ResourceKey<LootTable>)p_327391_));
               $$3.detectedPlayers.remove($$3.detectedPlayers.iterator().next());
               yield this;
            }
         }
         case COOLDOWN -> {
            $$3.tryDetectPlayers(pLevel, pPos, pSpawner);
            if (!$$3.detectedPlayers.isEmpty()) {
               $$3.totalMobsSpawned = 0;
               $$3.nextMobSpawnsAt = 0L;
               yield ACTIVE;
            } else if ($$3.isCooldownFinished(pLevel)) {
               pSpawner.removeOminous(pLevel, pPos);
               $$3.reset();
               yield WAITING_FOR_PLAYERS;
            } else {
               yield this;
            }
         }
      };
   }

   private void spawnOminousOminousItemSpawner(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner) {
      TrialSpawnerData $$3 = pSpawner.getData();
      TrialSpawnerConfig $$4 = pSpawner.getConfig();
      ItemStack $$5 = $$3.getDispensingItems(pLevel, $$4, pPos).getRandomValue(pLevel.random).orElse(ItemStack.EMPTY);
      if (!$$5.isEmpty()) {
         if (this.timeToSpawnItemSpawner(pLevel, $$3)) {
            calculatePositionToSpawnSpawner(pLevel, pPos, pSpawner, $$3).ifPresent(p_327373_ -> {
               OminousItemSpawner $$5x = OminousItemSpawner.create(pLevel, $$5);
               $$5x.moveTo(p_327373_);
               pLevel.addFreshEntity($$5x);
               float $$6 = (pLevel.getRandom().nextFloat() - pLevel.getRandom().nextFloat()) * 0.2F + 1.0F;
               pLevel.playSound(null, BlockPos.containing(p_327373_), SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, SoundSource.BLOCKS, 1.0F, $$6);
               $$3.cooldownEndsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenItemSpawners();
            });
         }
      }
   }

   private static Optional<Vec3> calculatePositionToSpawnSpawner(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner, TrialSpawnerData pSpawnerData) {
      List<Player> $$4 = pSpawnerData.detectedPlayers
         .stream()
         .map(pLevel::getPlayerByUUID)
         .filter(Objects::nonNull)
         .filter(
            p_341872_ -> !p_341872_.isCreative()
                  && !p_341872_.isSpectator()
                  && p_341872_.isAlive()
                  && p_341872_.distanceToSqr(pPos.getCenter()) <= (double)Mth.square(pSpawner.getRequiredPlayerRange())
         )
         .toList();
      if ($$4.isEmpty()) {
         return Optional.empty();
      } else {
         Entity $$5 = selectEntityToSpawnItemAbove($$4, pSpawnerData.currentMobs, pSpawner, pPos, pLevel);
         return $$5 == null ? Optional.empty() : calculatePositionAbove($$5, pLevel);
      }
   }

   private static Optional<Vec3> calculatePositionAbove(Entity pEntity, ServerLevel pLevel) {
      Vec3 $$2 = pEntity.position();
      Vec3 $$3 = $$2.relative(Direction.UP, (double)(pEntity.getBbHeight() + 2.0F + (float)pLevel.random.nextInt(4)));
      BlockHitResult $$4 = pLevel.clip(new ClipContext($$2, $$3, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
      Vec3 $$5 = $$4.getBlockPos().getCenter().relative(Direction.DOWN, 1.0);
      BlockPos $$6 = BlockPos.containing($$5);
      return !pLevel.getBlockState($$6).getCollisionShape(pLevel, $$6).isEmpty() ? Optional.empty() : Optional.of($$5);
   }

   @Nullable
   private static Entity selectEntityToSpawnItemAbove(List<Player> pPlayer, Set<UUID> pCurrentMobs, TrialSpawner pSpawner, BlockPos pPos, ServerLevel pLevel) {
      Stream<Entity> $$5 = pCurrentMobs.stream()
         .map(pLevel::getEntity)
         .filter(Objects::nonNull)
         .filter(p_327381_ -> p_327381_.isAlive() && p_327381_.distanceToSqr(pPos.getCenter()) <= (double)Mth.square(pSpawner.getRequiredPlayerRange()));
      List<? extends Entity> $$6 = pLevel.random.nextBoolean() ? $$5.toList() : pPlayer;
      if ($$6.isEmpty()) {
         return null;
      } else {
         return $$6.size() == 1 ? $$6.getFirst() : Util.getRandom($$6, pLevel.random);
      }
   }

   private boolean timeToSpawnItemSpawner(ServerLevel pLevel, TrialSpawnerData pSpawnerData) {
      return pLevel.getGameTime() >= pSpawnerData.cooldownEndsAt;
   }

   public int lightLevel() {
      return this.lightLevel;
   }

   public double spinningMobSpeed() {
      return this.spinningMobSpeed;
   }

   public boolean hasSpinningMob() {
      return this.spinningMobSpeed >= 0.0;
   }

   public boolean isCapableOfSpawning() {
      return this.isCapableOfSpawning;
   }

   public void emitParticles(Level pLevel, BlockPos pPos, boolean pIsOminous) {
      this.particleEmission.emit(pLevel, pLevel.getRandom(), pPos, pIsOminous);
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   static class LightLevel {
      private static final int UNLIT = 0;
      private static final int HALF_LIT = 4;
      private static final int LIT = 8;

      private LightLevel() {
      }
   }

   interface ParticleEmission {
      TrialSpawnerState.ParticleEmission NONE = (p_311158_, p_313095_, p_309870_, p_333658_) -> {
      };
      TrialSpawnerState.ParticleEmission SMALL_FLAMES = (p_327396_, p_327397_, p_327398_, p_327399_) -> {
         if (p_327397_.nextInt(2) == 0) {
            Vec3 $$4 = p_327398_.getCenter().offsetRandom(p_327397_, 0.9F);
            addParticle(p_327399_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, $$4, p_327396_);
         }
      };
      TrialSpawnerState.ParticleEmission FLAMES_AND_SMOKE = (p_327392_, p_327393_, p_327394_, p_327395_) -> {
         Vec3 $$4 = p_327394_.getCenter().offsetRandom(p_327393_, 1.0F);
         addParticle(ParticleTypes.SMOKE, $$4, p_327392_);
         addParticle(p_327395_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, $$4, p_327392_);
      };
      TrialSpawnerState.ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (p_312500_, p_312202_, p_311828_, p_334641_) -> {
         Vec3 $$4 = p_311828_.getCenter().offsetRandom(p_312202_, 0.9F);
         if (p_312202_.nextInt(3) == 0) {
            addParticle(ParticleTypes.SMOKE, $$4, p_312500_);
         }

         if (p_312500_.getGameTime() % 20L == 0L) {
            Vec3 $$5 = p_311828_.getCenter().add(0.0, 0.5, 0.0);
            int $$6 = p_312500_.getRandom().nextInt(4) + 20;

            for (int $$7 = 0; $$7 < $$6; $$7++) {
               addParticle(ParticleTypes.SMOKE, $$5, p_312500_);
            }
         }
      };

      private static void addParticle(SimpleParticleType pParticleType, Vec3 pPos, Level pLevel) {
         pLevel.addParticle(pParticleType, pPos.x(), pPos.y(), pPos.z(), 0.0, 0.0, 0.0);
      }

      void emit(Level pLevel, RandomSource pRandom, BlockPos pPos, boolean pIsOminous);
   }

   static class SpinningMob {
      private static final double NONE = -1.0;
      private static final double SLOW = 200.0;
      private static final double FAST = 1000.0;

      private SpinningMob() {
      }
   }
}
