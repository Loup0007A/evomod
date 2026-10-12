package net.minecraft.world.entity.animal;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class Turtle extends Animal {
   private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Boolean> HAS_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> LAYING_EGG = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<BlockPos> TRAVEL_POS = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Boolean> GOING_HOME = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> TRAVELLING = SynchedEntityData.defineId(Turtle.class, EntityDataSerializers.BOOLEAN);
   private static final float BABY_SCALE = 0.3F;
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.TURTLE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.TURTLE.getHeight(), -0.25F))
      .scale(0.3F);
   int layEggCounter;
   public static final Predicate<LivingEntity> BABY_ON_LAND_SELECTOR = p_341421_ -> p_341421_.isBaby() && !p_341421_.isInWater();

   public Turtle(EntityType<? extends Turtle> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.setPathfindingMalus(PathType.DOOR_IRON_CLOSED, -1.0F);
      this.setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, -1.0F);
      this.setPathfindingMalus(PathType.DOOR_OPEN, -1.0F);
      this.moveControl = new Turtle.TurtleMoveControl(this);
   }

   public void setHomePos(BlockPos pHomePos) {
      this.entityData.set(HOME_POS, pHomePos);
   }

   BlockPos getHomePos() {
      return this.entityData.get(HOME_POS);
   }

   void setTravelPos(BlockPos pTravelPos) {
      this.entityData.set(TRAVEL_POS, pTravelPos);
   }

   BlockPos getTravelPos() {
      return this.entityData.get(TRAVEL_POS);
   }

   public boolean hasEgg() {
      return this.entityData.get(HAS_EGG);
   }

   void setHasEgg(boolean pHasEgg) {
      this.entityData.set(HAS_EGG, pHasEgg);
   }

   public boolean isLayingEgg() {
      return this.entityData.get(LAYING_EGG);
   }

   void setLayingEgg(boolean pIsLayingEgg) {
      this.layEggCounter = pIsLayingEgg ? 1 : 0;
      this.entityData.set(LAYING_EGG, pIsLayingEgg);
   }

   boolean isGoingHome() {
      return this.entityData.get(GOING_HOME);
   }

   void setGoingHome(boolean pIsGoingHome) {
      this.entityData.set(GOING_HOME, pIsGoingHome);
   }

   boolean isTravelling() {
      return this.entityData.get(TRAVELLING);
   }

   void setTravelling(boolean pIsTravelling) {
      this.entityData.set(TRAVELLING, pIsTravelling);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(HOME_POS, BlockPos.ZERO);
      pBuilder.define(HAS_EGG, false);
      pBuilder.define(TRAVEL_POS, BlockPos.ZERO);
      pBuilder.define(GOING_HOME, false);
      pBuilder.define(TRAVELLING, false);
      pBuilder.define(LAYING_EGG, false);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("HomePosX", this.getHomePos().getX());
      pCompound.putInt("HomePosY", this.getHomePos().getY());
      pCompound.putInt("HomePosZ", this.getHomePos().getZ());
      pCompound.putBoolean("HasEgg", this.hasEgg());
      pCompound.putInt("TravelPosX", this.getTravelPos().getX());
      pCompound.putInt("TravelPosY", this.getTravelPos().getY());
      pCompound.putInt("TravelPosZ", this.getTravelPos().getZ());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      int $$1 = pCompound.getInt("HomePosX");
      int $$2 = pCompound.getInt("HomePosY");
      int $$3 = pCompound.getInt("HomePosZ");
      this.setHomePos(new BlockPos($$1, $$2, $$3));
      super.readAdditionalSaveData(pCompound);
      this.setHasEgg(pCompound.getBoolean("HasEgg"));
      int $$4 = pCompound.getInt("TravelPosX");
      int $$5 = pCompound.getInt("TravelPosY");
      int $$6 = pCompound.getInt("TravelPosZ");
      this.setTravelPos(new BlockPos($$4, $$5, $$6));
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData
   ) {
      this.setHomePos(this.blockPosition());
      this.setTravelPos(BlockPos.ZERO);
      return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
   }

   public static boolean checkTurtleSpawnRules(EntityType<Turtle> pTurtle, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
      return pPos.getY() < pLevel.getSeaLevel() + 4 && TurtleEggBlock.onSand(pLevel, pPos) && isBrightEnoughToSpawn(pLevel, pPos);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new Turtle.TurtlePanicGoal(this, 1.2));
      this.goalSelector.addGoal(1, new Turtle.TurtleBreedGoal(this, 1.0));
      this.goalSelector.addGoal(1, new Turtle.TurtleLayEggGoal(this, 1.0));
      this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, p_326986_ -> p_326986_.is(ItemTags.TURTLE_FOOD), false));
      this.goalSelector.addGoal(3, new Turtle.TurtleGoToWaterGoal(this, 1.0));
      this.goalSelector.addGoal(4, new Turtle.TurtleGoHomeGoal(this, 1.0));
      this.goalSelector.addGoal(7, new Turtle.TurtleTravelGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(9, new Turtle.TurtleRandomStrollGoal(this, 1.0, 100));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 200;
   }

   @Nullable
   @Override
   protected SoundEvent getAmbientSound() {
      return !this.isInWater() && this.onGround() && !this.isBaby() ? SoundEvents.TURTLE_AMBIENT_LAND : super.getAmbientSound();
   }

   @Override
   protected void playSwimSound(float pVolume) {
      super.playSwimSound(pVolume * 1.5F);
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.TURTLE_SWIM;
   }

   @Nullable
   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return this.isBaby() ? SoundEvents.TURTLE_HURT_BABY : SoundEvents.TURTLE_HURT;
   }

   @Nullable
   @Override
   protected SoundEvent getDeathSound() {
      return this.isBaby() ? SoundEvents.TURTLE_DEATH_BABY : SoundEvents.TURTLE_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pPos, BlockState pBlock) {
      SoundEvent $$2 = this.isBaby() ? SoundEvents.TURTLE_SHAMBLE_BABY : SoundEvents.TURTLE_SHAMBLE;
      this.playSound($$2, 0.15F, 1.0F);
   }

   @Override
   public boolean canFallInLove() {
      return super.canFallInLove() && !this.hasEgg();
   }

   @Override
   protected float nextStep() {
      return this.moveDist + 0.15F;
   }

   @Override
   public float getAgeScale() {
      return this.isBaby() ? 0.3F : 1.0F;
   }

   @Override
   protected PathNavigation createNavigation(Level pLevel) {
      return new Turtle.TurtlePathNavigation(this, pLevel);
   }

   @Nullable
   @Override
   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      return EntityType.TURTLE.create(pLevel);
   }

   @Override
   public boolean isFood(ItemStack pStack) {
      return pStack.is(ItemTags.TURTLE_FOOD);
   }

   @Override
   public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
      if (!this.isGoingHome() && pLevel.getFluidState(pPos).is(FluidTags.WATER)) {
         return 10.0F;
      } else {
         return TurtleEggBlock.onSand(pLevel, pPos) ? 10.0F : pLevel.getPathfindingCostFromLightLevels(pPos);
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.isAlive() && this.isLayingEgg() && this.layEggCounter >= 1 && this.layEggCounter % 5 == 0) {
         BlockPos $$0 = this.blockPosition();
         if (TurtleEggBlock.onSand(this.level(), $$0)) {
            this.level().levelEvent(2001, $$0, Block.getId(this.level().getBlockState($$0.below())));
            this.gameEvent(GameEvent.ENTITY_ACTION);
         }
      }
   }

   @Override
   protected void ageBoundaryReached() {
      super.ageBoundaryReached();
      if (!this.isBaby() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         this.spawnAtLocation(Items.TURTLE_SCUTE, 1);
      }
   }

   @Override
   public void travel(Vec3 pTravelVector) {
      if (this.isControlledByLocalInstance() && this.isInWater()) {
         this.moveRelative(0.1F, pTravelVector);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
         if (this.getTarget() == null && (!this.isGoingHome() || !this.getHomePos().closerToCenterThan(this.position(), 20.0))) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
         }
      } else {
         super.travel(pTravelVector);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   @Override
   public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
      this.hurt(this.damageSources().lightningBolt(), Float.MAX_VALUE);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose pPose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pPose);
   }

   static class TurtleBreedGoal extends BreedGoal {
      private final Turtle turtle;

      TurtleBreedGoal(Turtle pTurtle, double pSpeedModifier) {
         super(pTurtle, pSpeedModifier);
         this.turtle = pTurtle;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.turtle.hasEgg();
      }

      @Override
      protected void breed() {
         ServerPlayer $$0 = this.animal.getLoveCause();
         if ($$0 == null && this.partner.getLoveCause() != null) {
            $$0 = this.partner.getLoveCause();
         }

         if ($$0 != null) {
            $$0.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger($$0, this.animal, this.partner, null);
         }

         this.turtle.setHasEgg(true);
         this.animal.setAge(6000);
         this.partner.setAge(6000);
         this.animal.resetLove();
         this.partner.resetLove();
         RandomSource $$1 = this.animal.getRandom();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(this.level, this.animal.getX(), this.animal.getY(), this.animal.getZ(), $$1.nextInt(7) + 1));
         }
      }
   }

   static class TurtleGoHomeGoal extends Goal {
      private final Turtle turtle;
      private final double speedModifier;
      private boolean stuck;
      private int closeToHomeTryTicks;
      private static final int GIVE_UP_TICKS = 600;

      TurtleGoHomeGoal(Turtle pTurtle, double pSpeedModifier) {
         this.turtle = pTurtle;
         this.speedModifier = pSpeedModifier;
      }

      @Override
      public boolean canUse() {
         if (this.turtle.isBaby()) {
            return false;
         } else if (this.turtle.hasEgg()) {
            return true;
         } else {
            return this.turtle.getRandom().nextInt(reducedTickDelay(700)) != 0
               ? false
               : !this.turtle.getHomePos().closerToCenterThan(this.turtle.position(), 64.0);
         }
      }

      @Override
      public void start() {
         this.turtle.setGoingHome(true);
         this.stuck = false;
         this.closeToHomeTryTicks = 0;
      }

      @Override
      public void stop() {
         this.turtle.setGoingHome(false);
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.getHomePos().closerToCenterThan(this.turtle.position(), 7.0)
            && !this.stuck
            && this.closeToHomeTryTicks <= this.adjustedTickDelay(600);
      }

      @Override
      public void tick() {
         BlockPos $$0 = this.turtle.getHomePos();
         boolean $$1 = $$0.closerToCenterThan(this.turtle.position(), 16.0);
         if ($$1) {
            this.closeToHomeTryTicks++;
         }

         if (this.turtle.getNavigation().isDone()) {
            Vec3 $$2 = Vec3.atBottomCenterOf($$0);
            Vec3 $$3 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, $$2, (float) (Math.PI / 10));
            if ($$3 == null) {
               $$3 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, $$2, (float) (Math.PI / 2));
            }

            if ($$3 != null && !$$1 && !this.turtle.level().getBlockState(BlockPos.containing($$3)).is(Blocks.WATER)) {
               $$3 = DefaultRandomPos.getPosTowards(this.turtle, 16, 5, $$2, (float) (Math.PI / 2));
            }

            if ($$3 == null) {
               this.stuck = true;
               return;
            }

            this.turtle.getNavigation().moveTo($$3.x, $$3.y, $$3.z, this.speedModifier);
         }
      }
   }

   static class TurtleGoToWaterGoal extends MoveToBlockGoal {
      private static final int GIVE_UP_TICKS = 1200;
      private final Turtle turtle;

      TurtleGoToWaterGoal(Turtle pTurtle, double pSpeedModifier) {
         super(pTurtle, pTurtle.isBaby() ? 2.0 : pSpeedModifier, 24);
         this.turtle = pTurtle;
         this.verticalSearchStart = -1;
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.isInWater() && this.tryTicks <= 1200 && this.isValidTarget(this.turtle.level(), this.blockPos);
      }

      @Override
      public boolean canUse() {
         if (this.turtle.isBaby() && !this.turtle.isInWater()) {
            return super.canUse();
         } else {
            return !this.turtle.isGoingHome() && !this.turtle.isInWater() && !this.turtle.hasEgg() ? super.canUse() : false;
         }
      }

      @Override
      public boolean shouldRecalculatePath() {
         return this.tryTicks % 160 == 0;
      }

      @Override
      protected boolean isValidTarget(LevelReader pLevel, BlockPos pPos) {
         return pLevel.getBlockState(pPos).is(Blocks.WATER);
      }
   }

   static class TurtleLayEggGoal extends MoveToBlockGoal {
      private final Turtle turtle;

      TurtleLayEggGoal(Turtle pTurtle, double pSpeedModifier) {
         super(pTurtle, pSpeedModifier, 16);
         this.turtle = pTurtle;
      }

      @Override
      public boolean canUse() {
         return this.turtle.hasEgg() && this.turtle.getHomePos().closerToCenterThan(this.turtle.position(), 9.0) ? super.canUse() : false;
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && this.turtle.hasEgg() && this.turtle.getHomePos().closerToCenterThan(this.turtle.position(), 9.0);
      }

      @Override
      public void tick() {
         super.tick();
         BlockPos $$0 = this.turtle.blockPosition();
         if (!this.turtle.isInWater() && this.isReachedTarget()) {
            if (this.turtle.layEggCounter < 1) {
               this.turtle.setLayingEgg(true);
            } else if (this.turtle.layEggCounter > this.adjustedTickDelay(200)) {
               Level $$1 = this.turtle.level();
               $$1.playSound(null, $$0, SoundEvents.TURTLE_LAY_EGG, SoundSource.BLOCKS, 0.3F, 0.9F + $$1.random.nextFloat() * 0.2F);
               BlockPos $$2 = this.blockPos.above();
               BlockState $$3 = Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, Integer.valueOf(this.turtle.random.nextInt(4) + 1));
               $$1.setBlock($$2, $$3, 3);
               $$1.gameEvent(GameEvent.BLOCK_PLACE, $$2, GameEvent.Context.of(this.turtle, $$3));
               this.turtle.setHasEgg(false);
               this.turtle.setLayingEgg(false);
               this.turtle.setInLoveTime(600);
            }

            if (this.turtle.isLayingEgg()) {
               this.turtle.layEggCounter++;
            }
         }
      }

      @Override
      protected boolean isValidTarget(LevelReader pLevel, BlockPos pPos) {
         return !pLevel.isEmptyBlock(pPos.above()) ? false : TurtleEggBlock.isSand(pLevel, pPos);
      }
   }

   static class TurtleMoveControl extends MoveControl {
      private final Turtle turtle;

      TurtleMoveControl(Turtle pTurtle) {
         super(pTurtle);
         this.turtle = pTurtle;
      }

      private void updateSpeed() {
         if (this.turtle.isInWater()) {
            this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0, 0.005, 0.0));
            if (!this.turtle.getHomePos().closerToCenterThan(this.turtle.position(), 16.0)) {
               this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.08F));
            }

            if (this.turtle.isBaby()) {
               this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 3.0F, 0.06F));
            }
         } else if (this.turtle.onGround()) {
            this.turtle.setSpeed(Math.max(this.turtle.getSpeed() / 2.0F, 0.06F));
         }
      }

      @Override
      public void tick() {
         this.updateSpeed();
         if (this.operation == MoveControl.Operation.MOVE_TO && !this.turtle.getNavigation().isDone()) {
            double $$0 = this.wantedX - this.turtle.getX();
            double $$1 = this.wantedY - this.turtle.getY();
            double $$2 = this.wantedZ - this.turtle.getZ();
            double $$3 = Math.sqrt($$0 * $$0 + $$1 * $$1 + $$2 * $$2);
            if ($$3 < 1.0E-5F) {
               this.mob.setSpeed(0.0F);
            } else {
               $$1 /= $$3;
               float $$4 = (float)(Mth.atan2($$2, $$0) * 180.0F / (float)Math.PI) - 90.0F;
               this.turtle.setYRot(this.rotlerp(this.turtle.getYRot(), $$4, 90.0F));
               this.turtle.yBodyRot = this.turtle.getYRot();
               float $$5 = (float)(this.speedModifier * this.turtle.getAttributeValue(Attributes.MOVEMENT_SPEED));
               this.turtle.setSpeed(Mth.lerp(0.125F, this.turtle.getSpeed(), $$5));
               this.turtle.setDeltaMovement(this.turtle.getDeltaMovement().add(0.0, (double)this.turtle.getSpeed() * $$1 * 0.1, 0.0));
            }
         } else {
            this.turtle.setSpeed(0.0F);
         }
      }
   }

   static class TurtlePanicGoal extends PanicGoal {
      TurtlePanicGoal(Turtle pTurtle, double pSpeedModifier) {
         super(pTurtle, pSpeedModifier);
      }

      @Override
      public boolean canUse() {
         if (!this.shouldPanic()) {
            return false;
         } else {
            BlockPos $$0 = this.lookForWater(this.mob.level(), this.mob, 7);
            if ($$0 != null) {
               this.posX = (double)$$0.getX();
               this.posY = (double)$$0.getY();
               this.posZ = (double)$$0.getZ();
               return true;
            } else {
               return this.findRandomPosition();
            }
         }
      }
   }

   static class TurtlePathNavigation extends AmphibiousPathNavigation {
      TurtlePathNavigation(Turtle pTurtle, Level pLevel) {
         super(pTurtle, pLevel);
      }

      @Override
      public boolean isStableDestination(BlockPos pPos) {
         if (this.mob instanceof Turtle $$1 && $$1.isTravelling()) {
            return this.level.getBlockState(pPos).is(Blocks.WATER);
         }

         return !this.level.getBlockState(pPos.below()).isAir();
      }
   }

   static class TurtleRandomStrollGoal extends RandomStrollGoal {
      private final Turtle turtle;

      TurtleRandomStrollGoal(Turtle pTurtle, double pSpeedModifier, int pInterval) {
         super(pTurtle, pSpeedModifier, pInterval);
         this.turtle = pTurtle;
      }

      @Override
      public boolean canUse() {
         return !this.mob.isInWater() && !this.turtle.isGoingHome() && !this.turtle.hasEgg() ? super.canUse() : false;
      }
   }

   static class TurtleTravelGoal extends Goal {
      private final Turtle turtle;
      private final double speedModifier;
      private boolean stuck;

      TurtleTravelGoal(Turtle pTurtle, double pSpeedModifier) {
         this.turtle = pTurtle;
         this.speedModifier = pSpeedModifier;
      }

      @Override
      public boolean canUse() {
         return !this.turtle.isGoingHome() && !this.turtle.hasEgg() && this.turtle.isInWater();
      }

      @Override
      public void start() {
         int $$0 = 512;
         int $$1 = 4;
         RandomSource $$2 = this.turtle.random;
         int $$3 = $$2.nextInt(1025) - 512;
         int $$4 = $$2.nextInt(9) - 4;
         int $$5 = $$2.nextInt(1025) - 512;
         if ((double)$$4 + this.turtle.getY() > (double)(this.turtle.level().getSeaLevel() - 1)) {
            $$4 = 0;
         }

         BlockPos $$6 = BlockPos.containing((double)$$3 + this.turtle.getX(), (double)$$4 + this.turtle.getY(), (double)$$5 + this.turtle.getZ());
         this.turtle.setTravelPos($$6);
         this.turtle.setTravelling(true);
         this.stuck = false;
      }

      @Override
      public void tick() {
         if (this.turtle.getNavigation().isDone()) {
            Vec3 $$0 = Vec3.atBottomCenterOf(this.turtle.getTravelPos());
            Vec3 $$1 = DefaultRandomPos.getPosTowards(this.turtle, 16, 3, $$0, (float) (Math.PI / 10));
            if ($$1 == null) {
               $$1 = DefaultRandomPos.getPosTowards(this.turtle, 8, 7, $$0, (float) (Math.PI / 2));
            }

            if ($$1 != null) {
               int $$2 = Mth.floor($$1.x);
               int $$3 = Mth.floor($$1.z);
               int $$4 = 34;
               if (!this.turtle.level().hasChunksAt($$2 - 34, $$3 - 34, $$2 + 34, $$3 + 34)) {
                  $$1 = null;
               }
            }

            if ($$1 == null) {
               this.stuck = true;
               return;
            }

            this.turtle.getNavigation().moveTo($$1.x, $$1.y, $$1.z, this.speedModifier);
         }
      }

      @Override
      public boolean canContinueToUse() {
         return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.isGoingHome() && !this.turtle.isInLove() && !this.turtle.hasEgg();
      }

      @Override
      public void stop() {
         this.turtle.setTravelling(false);
         super.stop();
      }
   }
}
