package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class TrialSpawner {
   public static final String NORMAL_CONFIG_TAG_NAME = "normal_config";
   public static final String OMINOUS_CONFIG_TAG_NAME = "ominous_config";
   public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
   private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
   private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
   private static final int MAX_MOB_TRACKING_DISTANCE = 47;
   private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
   private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
   private final TrialSpawnerConfig normalConfig;
   private final TrialSpawnerConfig ominousConfig;
   private final TrialSpawnerData data;
   private final int requiredPlayerRange;
   private final int targetCooldownLength;
   private final TrialSpawner.StateAccessor stateAccessor;
   private PlayerDetector playerDetector;
   private final PlayerDetector.EntitySelector entitySelector;
   private boolean overridePeacefulAndMobSpawnRule;
   private boolean isOminous;

   public Codec<TrialSpawner> codec() {
      return RecordCodecBuilder.create(
         p_327363_ -> p_327363_.group(
                  TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", TrialSpawnerConfig.DEFAULT).forGetter(TrialSpawner::getNormalConfig),
                  TrialSpawnerConfig.CODEC
                     .optionalFieldOf("ominous_config", TrialSpawnerConfig.DEFAULT)
                     .forGetter(TrialSpawner::getOminousConfigForSerialization),
                  TrialSpawnerData.MAP_CODEC.forGetter(TrialSpawner::getData),
                  Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner::getTargetCooldownLength),
                  Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner::getRequiredPlayerRange)
               )
               .apply(
                  p_327363_,
                  (p_327358_, p_327359_, p_327360_, p_327361_, p_327362_) -> new TrialSpawner(
                        p_327358_, p_327359_, p_327360_, p_327361_, p_327362_, this.stateAccessor, this.playerDetector, this.entitySelector
                     )
               )
      );
   }

   public TrialSpawner(TrialSpawner.StateAccessor pStateAccessor, PlayerDetector pPlayerDetector, PlayerDetector.EntitySelector pEntitySelector) {
      this(TrialSpawnerConfig.DEFAULT, TrialSpawnerConfig.DEFAULT, new TrialSpawnerData(), 36000, 14, pStateAccessor, pPlayerDetector, pEntitySelector);
   }

   public TrialSpawner(
      TrialSpawnerConfig pNormalConfig,
      TrialSpawnerConfig pOminousConfig,
      TrialSpawnerData pData,
      int pTargetCooldownLength,
      int pRequiredPlayerRange,
      TrialSpawner.StateAccessor pStateAccessor,
      PlayerDetector pPlayerDetector,
      PlayerDetector.EntitySelector pEntitySelector
   ) {
      this.normalConfig = pNormalConfig;
      this.ominousConfig = pOminousConfig;
      this.data = pData;
      this.targetCooldownLength = pTargetCooldownLength;
      this.requiredPlayerRange = pRequiredPlayerRange;
      this.stateAccessor = pStateAccessor;
      this.playerDetector = pPlayerDetector;
      this.entitySelector = pEntitySelector;
   }

   public TrialSpawnerConfig getConfig() {
      return this.isOminous ? this.ominousConfig : this.normalConfig;
   }

   @VisibleForTesting
   public TrialSpawnerConfig getNormalConfig() {
      return this.normalConfig;
   }

   @VisibleForTesting
   public TrialSpawnerConfig getOminousConfig() {
      return this.ominousConfig;
   }

   private TrialSpawnerConfig getOminousConfigForSerialization() {
      return !this.ominousConfig.equals(this.normalConfig) ? this.ominousConfig : TrialSpawnerConfig.DEFAULT;
   }

   public void applyOminous(ServerLevel pLevel, BlockPos pPos) {
      pLevel.setBlock(pPos, pLevel.getBlockState(pPos).setValue(TrialSpawnerBlock.OMINOUS, Boolean.valueOf(true)), 3);
      pLevel.levelEvent(3020, pPos, 1);
      this.isOminous = true;
      this.data.resetAfterBecomingOminous(this, pLevel);
   }

   public void removeOminous(ServerLevel pLevel, BlockPos pPos) {
      pLevel.setBlock(pPos, pLevel.getBlockState(pPos).setValue(TrialSpawnerBlock.OMINOUS, Boolean.valueOf(false)), 3);
      this.isOminous = false;
   }

   public boolean isOminous() {
      return this.isOminous;
   }

   public TrialSpawnerData getData() {
      return this.data;
   }

   public int getTargetCooldownLength() {
      return this.targetCooldownLength;
   }

   public int getRequiredPlayerRange() {
      return this.requiredPlayerRange;
   }

   public TrialSpawnerState getState() {
      return this.stateAccessor.getState();
   }

   public void setState(Level pLevel, TrialSpawnerState pState) {
      this.stateAccessor.setState(pLevel, pState);
   }

   public void markUpdated() {
      this.stateAccessor.markUpdated();
   }

   public PlayerDetector getPlayerDetector() {
      return this.playerDetector;
   }

   public PlayerDetector.EntitySelector getEntitySelector() {
      return this.entitySelector;
   }

   public boolean canSpawnInLevel(Level pLevel) {
      if (this.overridePeacefulAndMobSpawnRule) {
         return true;
      } else {
         return pLevel.getDifficulty() == Difficulty.PEACEFUL ? false : pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
      }
   }

   public Optional<UUID> spawnMob(ServerLevel pLevel, BlockPos pPos) {
      RandomSource $$2 = pLevel.getRandom();
      SpawnData $$3 = this.data.getOrCreateNextSpawnData(this, pLevel.getRandom());
      CompoundTag $$4 = $$3.entityToSpawn();
      ListTag $$5 = $$4.getList("Pos", 6);
      Optional<EntityType<?>> $$6 = EntityType.by($$4);
      if ($$6.isEmpty()) {
         return Optional.empty();
      } else {
         int $$7 = $$5.size();
         double $$8 = $$7 >= 1 ? $$5.getDouble(0) : (double)pPos.getX() + ($$2.nextDouble() - $$2.nextDouble()) * (double)this.getConfig().spawnRange() + 0.5;
         double $$9 = $$7 >= 2 ? $$5.getDouble(1) : (double)(pPos.getY() + $$2.nextInt(3) - 1);
         double $$10 = $$7 >= 3 ? $$5.getDouble(2) : (double)pPos.getZ() + ($$2.nextDouble() - $$2.nextDouble()) * (double)this.getConfig().spawnRange() + 0.5;
         if (!pLevel.noCollision($$6.get().getSpawnAABB($$8, $$9, $$10))) {
            return Optional.empty();
         } else {
            Vec3 $$11 = new Vec3($$8, $$9, $$10);
            if (!inLineOfSight(pLevel, pPos.getCenter(), $$11)) {
               return Optional.empty();
            } else {
               BlockPos $$12 = BlockPos.containing($$11);
               if (!SpawnPlacements.checkSpawnRules($$6.get(), pLevel, MobSpawnType.TRIAL_SPAWNER, $$12, pLevel.getRandom())) {
                  return Optional.empty();
               } else {
                  if ($$3.getCustomSpawnRules().isPresent()) {
                     SpawnData.CustomSpawnRules $$13 = $$3.getCustomSpawnRules().get();
                     if (!$$13.isValidPosition($$12, pLevel)) {
                        return Optional.empty();
                     }
                  }

                  Entity $$14 = EntityType.loadEntityRecursive($$4, pLevel, p_312166_ -> {
                     p_312166_.moveTo($$8, $$9, $$10, $$2.nextFloat() * 360.0F, 0.0F);
                     return p_312166_;
                  });
                  if ($$14 == null) {
                     return Optional.empty();
                  } else {
                     if ($$14 instanceof Mob $$15) {
                        if (!$$15.checkSpawnObstruction(pLevel)) {
                           return Optional.empty();
                        }

                        boolean $$16 = $$3.getEntityToSpawn().size() == 1 && $$3.getEntityToSpawn().contains("id", 8);
                        if ($$16) {
                           $$15.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt($$15.blockPosition()), MobSpawnType.TRIAL_SPAWNER, null);
                        }

                        $$15.setPersistenceRequired();
                        $$3.getEquipment().ifPresent($$15::equip);
                     }

                     if (!pLevel.tryAddFreshEntityWithPassengers($$14)) {
                        return Optional.empty();
                     } else {
                        TrialSpawner.FlameParticle $$17 = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
                        pLevel.levelEvent(3011, pPos, $$17.encode());
                        pLevel.levelEvent(3012, $$12, $$17.encode());
                        pLevel.gameEvent($$14, GameEvent.ENTITY_PLACE, $$12);
                        return Optional.of($$14.getUUID());
                     }
                  }
               }
            }
         }
      }
   }

   public void ejectReward(ServerLevel pLevel, BlockPos pPos, ResourceKey<LootTable> pLootTable) {
      LootTable $$3 = pLevel.getServer().reloadableRegistries().getLootTable(pLootTable);
      LootParams $$4 = new LootParams.Builder(pLevel).create(LootContextParamSets.EMPTY);
      ObjectArrayList<ItemStack> $$5 = $$3.getRandomItems($$4);
      if (!$$5.isEmpty()) {
         ObjectListIterator var7 = $$5.iterator();

         while (var7.hasNext()) {
            ItemStack $$6 = (ItemStack)var7.next();
            DefaultDispenseItemBehavior.spawnItem(pLevel, $$6, 2, Direction.UP, Vec3.atBottomCenterOf(pPos).relative(Direction.UP, 1.2));
         }

         pLevel.levelEvent(3014, pPos, 0);
      }
   }

   public void tickClient(Level pLevel, BlockPos pPos, boolean pIsOminous) {
      TrialSpawnerState $$3 = this.getState();
      $$3.emitParticles(pLevel, pPos, pIsOminous);
      if ($$3.hasSpinningMob()) {
         double $$4 = (double)Math.max(0L, this.data.nextMobSpawnsAt - pLevel.getGameTime());
         this.data.oSpin = this.data.spin;
         this.data.spin = (this.data.spin + $$3.spinningMobSpeed() / ($$4 + 200.0)) % 360.0;
      }

      if ($$3.isCapableOfSpawning()) {
         RandomSource $$5 = pLevel.getRandom();
         if ($$5.nextFloat() <= 0.02F) {
            SoundEvent $$6 = pIsOminous ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            pLevel.playLocalSound(pPos, $$6, SoundSource.BLOCKS, $$5.nextFloat() * 0.25F + 0.75F, $$5.nextFloat() + 0.5F, false);
         }
      }
   }

   public void tickServer(ServerLevel pLevel, BlockPos pPos, boolean pIsOminous) {
      this.isOminous = pIsOminous;
      TrialSpawnerState $$3 = this.getState();
      if (this.data.currentMobs.removeIf(p_309715_ -> shouldMobBeUntracked(pLevel, pPos, p_309715_))) {
         this.data.nextMobSpawnsAt = pLevel.getGameTime() + (long)this.getConfig().ticksBetweenSpawn();
      }

      TrialSpawnerState $$4 = $$3.tickAndGetNext(pPos, this, pLevel);
      if ($$4 != $$3) {
         this.setState(pLevel, $$4);
      }
   }

   private static boolean shouldMobBeUntracked(ServerLevel pLevel, BlockPos pPos, UUID pUuid) {
      Entity $$3 = pLevel.getEntity(pUuid);
      return $$3 == null
         || !$$3.isAlive()
         || !$$3.level().dimension().equals(pLevel.dimension())
         || $$3.blockPosition().distSqr(pPos) > (double)MAX_MOB_TRACKING_DISTANCE_SQR;
   }

   private static boolean inLineOfSight(Level pLevel, Vec3 pSpawnerPos, Vec3 pMobPos) {
      BlockHitResult $$3 = pLevel.clip(new ClipContext(pMobPos, pSpawnerPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
      return $$3.getBlockPos().equals(BlockPos.containing(pSpawnerPos)) || $$3.getType() == HitResult.Type.MISS;
   }

   public static void addSpawnParticles(Level pLevel, BlockPos pPos, RandomSource pRandom, SimpleParticleType pParticleType) {
      for (int $$4 = 0; $$4 < 20; $$4++) {
         double $$5 = (double)pPos.getX() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         double $$6 = (double)pPos.getY() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         double $$7 = (double)pPos.getZ() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         pLevel.addParticle(ParticleTypes.SMOKE, $$5, $$6, $$7, 0.0, 0.0, 0.0);
         pLevel.addParticle(pParticleType, $$5, $$6, $$7, 0.0, 0.0, 0.0);
      }
   }

   public static void addBecomeOminousParticles(Level pLevel, BlockPos pPos, RandomSource pRandom) {
      for (int $$3 = 0; $$3 < 20; $$3++) {
         double $$4 = (double)pPos.getX() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         double $$5 = (double)pPos.getY() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         double $$6 = (double)pPos.getZ() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
         double $$7 = pRandom.nextGaussian() * 0.02;
         double $$8 = pRandom.nextGaussian() * 0.02;
         double $$9 = pRandom.nextGaussian() * 0.02;
         pLevel.addParticle(ParticleTypes.TRIAL_OMEN, $$4, $$5, $$6, $$7, $$8, $$9);
         pLevel.addParticle(ParticleTypes.SOUL_FIRE_FLAME, $$4, $$5, $$6, $$7, $$8, $$9);
      }
   }

   public static void addDetectPlayerParticles(Level pLevel, BlockPos pPos, RandomSource pRandom, int pType, ParticleOptions pParticle) {
      for (int $$5 = 0; $$5 < 30 + Math.min(pType, 10) * 5; $$5++) {
         double $$6 = (double)(2.0F * pRandom.nextFloat() - 1.0F) * 0.65;
         double $$7 = (double)(2.0F * pRandom.nextFloat() - 1.0F) * 0.65;
         double $$8 = (double)pPos.getX() + 0.5 + $$6;
         double $$9 = (double)pPos.getY() + 0.1 + (double)pRandom.nextFloat() * 0.8;
         double $$10 = (double)pPos.getZ() + 0.5 + $$7;
         pLevel.addParticle(pParticle, $$8, $$9, $$10, 0.0, 0.0, 0.0);
      }
   }

   public static void addEjectItemParticles(Level pLevel, BlockPos pPos, RandomSource pRandom) {
      for (int $$3 = 0; $$3 < 20; $$3++) {
         double $$4 = (double)pPos.getX() + 0.4 + pRandom.nextDouble() * 0.2;
         double $$5 = (double)pPos.getY() + 0.4 + pRandom.nextDouble() * 0.2;
         double $$6 = (double)pPos.getZ() + 0.4 + pRandom.nextDouble() * 0.2;
         double $$7 = pRandom.nextGaussian() * 0.02;
         double $$8 = pRandom.nextGaussian() * 0.02;
         double $$9 = pRandom.nextGaussian() * 0.02;
         pLevel.addParticle(ParticleTypes.SMALL_FLAME, $$4, $$5, $$6, $$7, $$8, $$9 * 0.25);
         pLevel.addParticle(ParticleTypes.SMOKE, $$4, $$5, $$6, $$7, $$8, $$9);
      }
   }

   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void setPlayerDetector(PlayerDetector pPlayerDetector) {
      this.playerDetector = pPlayerDetector;
   }

   @Deprecated(
      forRemoval = true
   )
   @VisibleForTesting
   public void overridePeacefulAndMobSpawnRule() {
      this.overridePeacefulAndMobSpawnRule = true;
   }

   public static enum FlameParticle {
      NORMAL(ParticleTypes.FLAME),
      OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

      public final SimpleParticleType particleType;

      private FlameParticle(final SimpleParticleType pParticleType) {
         this.particleType = pParticleType;
      }

      public static TrialSpawner.FlameParticle decode(int pId) {
         TrialSpawner.FlameParticle[] $$1 = values();
         return pId <= $$1.length && pId >= 0 ? $$1[pId] : NORMAL;
      }

      public int encode() {
         return this.ordinal();
      }
   }

   public interface StateAccessor {
      void setState(Level pLevel, TrialSpawnerState pState);

      TrialSpawnerState getState();

      void markUpdated();
   }
}
