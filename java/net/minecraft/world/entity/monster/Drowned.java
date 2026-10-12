package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class Drowned extends Zombie implements RangedAttackMob {
   public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
   boolean searchingForLand;
   protected final WaterBoundPathNavigation waterNavigation;
   protected final GroundPathNavigation groundNavigation;

   public Drowned(EntityType<? extends Drowned> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.moveControl = new Drowned.DrownedMoveControl(this);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.waterNavigation = new WaterBoundPathNavigation(this, pLevel);
      this.groundNavigation = new GroundPathNavigation(this, pLevel);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes().add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(1, new Drowned.DrownedGoToWaterGoal(this, 1.0));
      this.goalSelector.addGoal(2, new Drowned.DrownedTridentAttackGoal(this, 1.0, 40, 10.0F));
      this.goalSelector.addGoal(2, new Drowned.DrownedAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new Drowned.DrownedGoToBeachGoal(this, 1.0));
      this.goalSelector.addGoal(6, new Drowned.DrownedSwimUpGoal(this, 1.0, this.level().getSeaLevel()));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Drowned.class).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::okTarget));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Axolotl.class, true, false));
      this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData
   ) {
      pSpawnGroupData = super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
      if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && pLevel.getRandom().nextFloat() < 0.03F) {
         this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
         this.setGuaranteedDrop(EquipmentSlot.OFFHAND);
      }

      return pSpawnGroupData;
   }

   public static boolean checkDrownedSpawnRules(
      EntityType<Drowned> pDrowned, ServerLevelAccessor pServerLevel, MobSpawnType pMobSpawnType, BlockPos pPos, RandomSource pRandom
   ) {
      if (!pServerLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && !MobSpawnType.isSpawner(pMobSpawnType)) {
         return false;
      } else {
         Holder<Biome> $$5 = pServerLevel.getBiome(pPos);
         boolean $$6 = pServerLevel.getDifficulty() != Difficulty.PEACEFUL
            && (MobSpawnType.ignoresLightRequirements(pMobSpawnType) || isDarkEnoughToSpawn(pServerLevel, pPos, pRandom))
            && (MobSpawnType.isSpawner(pMobSpawnType) || pServerLevel.getFluidState(pPos).is(FluidTags.WATER));
         if ($$6 && MobSpawnType.isSpawner(pMobSpawnType)) {
            return true;
         } else {
            return $$5.is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
               ? pRandom.nextInt(15) == 0 && $$6
               : pRandom.nextInt(40) == 0 && isDeepEnoughToSpawn(pServerLevel, pPos) && $$6;
         }
      }
   }

   private static boolean isDeepEnoughToSpawn(LevelAccessor pLevel, BlockPos pPos) {
      return pPos.getY() < pLevel.getSeaLevel() - 5;
   }

   @Override
   protected boolean supportsBreakDoorGoal() {
      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? SoundEvents.DROWNED_AMBIENT_WATER : SoundEvents.DROWNED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return this.isInWater() ? SoundEvents.DROWNED_HURT_WATER : SoundEvents.DROWNED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isInWater() ? SoundEvents.DROWNED_DEATH_WATER : SoundEvents.DROWNED_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.DROWNED_STEP;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.DROWNED_SWIM;
   }

   @Override
   protected ItemStack getSkull() {
      return ItemStack.EMPTY;
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource pRandom, DifficultyInstance pDifficulty) {
      if ((double)pRandom.nextFloat() > 0.9) {
         int $$2 = pRandom.nextInt(16);
         if ($$2 < 10) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
         } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
         }
      }
   }

   @Override
   protected boolean canReplaceCurrentItem(ItemStack pCandidate, ItemStack pExisting) {
      if (pExisting.is(Items.NAUTILUS_SHELL)) {
         return false;
      } else if (pExisting.is(Items.TRIDENT)) {
         return pCandidate.is(Items.TRIDENT) ? pCandidate.getDamageValue() < pExisting.getDamageValue() : false;
      } else {
         return pCandidate.is(Items.TRIDENT) ? true : super.canReplaceCurrentItem(pCandidate, pExisting);
      }
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader pLevel) {
      return pLevel.isUnobstructed(this);
   }

   public boolean okTarget(@Nullable LivingEntity p_32396_) {
      return p_32396_ != null ? !this.level().isDay() || p_32396_.isInWater() : false;
   }

   @Override
   public boolean isPushedByFluid() {
      return !this.isSwimming();
   }

   boolean wantsToSwim() {
      if (this.searchingForLand) {
         return true;
      } else {
         LivingEntity $$0 = this.getTarget();
         return $$0 != null && $$0.isInWater();
      }
   }

   @Override
   public void travel(Vec3 pTravelVector) {
      if (this.isControlledByLocalInstance() && this.isInWater() && this.wantsToSwim()) {
         this.moveRelative(0.01F, pTravelVector);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
      } else {
         super.travel(pTravelVector);
      }
   }

   @Override
   public void updateSwimming() {
      if (!this.level().isClientSide) {
         if (this.isEffectiveAi() && this.isInWater() && this.wantsToSwim()) {
            this.navigation = this.waterNavigation;
            this.setSwimming(true);
         } else {
            this.navigation = this.groundNavigation;
            this.setSwimming(false);
         }
      }
   }

   @Override
   public boolean isVisuallySwimming() {
      return this.isSwimming();
   }

   protected boolean closeToNextPos() {
      Path $$0 = this.getNavigation().getPath();
      if ($$0 != null) {
         BlockPos $$1 = $$0.getTarget();
         if ($$1 != null) {
            double $$2 = this.distanceToSqr((double)$$1.getX(), (double)$$1.getY(), (double)$$1.getZ());
            if ($$2 < 4.0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
      ThrownTrident $$2 = new ThrownTrident(this.level(), this, new ItemStack(Items.TRIDENT));
      double $$3 = pTarget.getX() - this.getX();
      double $$4 = pTarget.getY(0.3333333333333333) - $$2.getY();
      double $$5 = pTarget.getZ() - this.getZ();
      double $$6 = Math.sqrt($$3 * $$3 + $$5 * $$5);
      $$2.shoot($$3, $$4 + $$6 * 0.2F, $$5, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
      this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
      this.level().addFreshEntity($$2);
   }

   public void setSearchingForLand(boolean pSearchingForLand) {
      this.searchingForLand = pSearchingForLand;
   }

   static class DrownedAttackGoal extends ZombieAttackGoal {
      private final Drowned drowned;

      public DrownedAttackGoal(Drowned pDrowned, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
         super(pDrowned, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
         this.drowned = pDrowned;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.okTarget(this.drowned.getTarget());
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && this.drowned.okTarget(this.drowned.getTarget());
      }
   }

   static class DrownedGoToBeachGoal extends MoveToBlockGoal {
      private final Drowned drowned;

      public DrownedGoToBeachGoal(Drowned pDrowned, double pSpeedModifier) {
         super(pDrowned, pSpeedModifier, 8, 2);
         this.drowned = pDrowned;
      }

      @Override
      public boolean canUse() {
         return super.canUse()
            && !this.drowned.level().isDay()
            && this.drowned.isInWater()
            && this.drowned.getY() >= (double)(this.drowned.level().getSeaLevel() - 3);
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse();
      }

      @Override
      protected boolean isValidTarget(LevelReader pLevel, BlockPos pPos) {
         BlockPos $$2 = pPos.above();
         return pLevel.isEmptyBlock($$2) && pLevel.isEmptyBlock($$2.above()) ? pLevel.getBlockState(pPos).entityCanStandOn(pLevel, pPos, this.drowned) : false;
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(false);
         this.drowned.navigation = this.drowned.groundNavigation;
         super.start();
      }

      @Override
      public void stop() {
         super.stop();
      }
   }

   static class DrownedGoToWaterGoal extends Goal {
      private final PathfinderMob mob;
      private double wantedX;
      private double wantedY;
      private double wantedZ;
      private final double speedModifier;
      private final Level level;

      public DrownedGoToWaterGoal(PathfinderMob pMob, double pSpeedModifier) {
         this.mob = pMob;
         this.speedModifier = pSpeedModifier;
         this.level = pMob.level();
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (!this.level.isDay()) {
            return false;
         } else if (this.mob.isInWater()) {
            return false;
         } else {
            Vec3 $$0 = this.getWaterPos();
            if ($$0 == null) {
               return false;
            } else {
               this.wantedX = $$0.x;
               this.wantedY = $$0.y;
               this.wantedZ = $$0.z;
               return true;
            }
         }
      }

      @Override
      public boolean canContinueToUse() {
         return !this.mob.getNavigation().isDone();
      }

      @Override
      public void start() {
         this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
      }

      @Nullable
      private Vec3 getWaterPos() {
         RandomSource $$0 = this.mob.getRandom();
         BlockPos $$1 = this.mob.blockPosition();

         for (int $$2 = 0; $$2 < 10; $$2++) {
            BlockPos $$3 = $$1.offset($$0.nextInt(20) - 10, 2 - $$0.nextInt(8), $$0.nextInt(20) - 10);
            if (this.level.getBlockState($$3).is(Blocks.WATER)) {
               return Vec3.atBottomCenterOf($$3);
            }
         }

         return null;
      }
   }

   static class DrownedMoveControl extends MoveControl {
      private final Drowned drowned;

      public DrownedMoveControl(Drowned pDrowned) {
         super(pDrowned);
         this.drowned = pDrowned;
      }

      @Override
      public void tick() {
         LivingEntity $$0 = this.drowned.getTarget();
         if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
            if ($$0 != null && $$0.getY() > this.drowned.getY() || this.drowned.searchingForLand) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, 0.002, 0.0));
            }

            if (this.operation != MoveControl.Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
               this.drowned.setSpeed(0.0F);
               return;
            }

            double $$1 = this.wantedX - this.drowned.getX();
            double $$2 = this.wantedY - this.drowned.getY();
            double $$3 = this.wantedZ - this.drowned.getZ();
            double $$4 = Math.sqrt($$1 * $$1 + $$2 * $$2 + $$3 * $$3);
            $$2 /= $$4;
            float $$5 = (float)(Mth.atan2($$3, $$1) * 180.0F / (float)Math.PI) - 90.0F;
            this.drowned.setYRot(this.rotlerp(this.drowned.getYRot(), $$5, 90.0F));
            this.drowned.yBodyRot = this.drowned.getYRot();
            float $$6 = (float)(this.speedModifier * this.drowned.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float $$7 = Mth.lerp(0.125F, this.drowned.getSpeed(), $$6);
            this.drowned.setSpeed($$7);
            this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add((double)$$7 * $$1 * 0.005, (double)$$7 * $$2 * 0.1, (double)$$7 * $$3 * 0.005));
         } else {
            if (!this.drowned.onGround()) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, -0.008, 0.0));
            }

            super.tick();
         }
      }
   }

   static class DrownedSwimUpGoal extends Goal {
      private final Drowned drowned;
      private final double speedModifier;
      private final int seaLevel;
      private boolean stuck;

      public DrownedSwimUpGoal(Drowned pDrowned, double pSpeedModifier, int pSeaLevel) {
         this.drowned = pDrowned;
         this.speedModifier = pSpeedModifier;
         this.seaLevel = pSeaLevel;
      }

      @Override
      public boolean canUse() {
         return !this.drowned.level().isDay() && this.drowned.isInWater() && this.drowned.getY() < (double)(this.seaLevel - 2);
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse() && !this.stuck;
      }

      @Override
      public void tick() {
         if (this.drowned.getY() < (double)(this.seaLevel - 1) && (this.drowned.getNavigation().isDone() || this.drowned.closeToNextPos())) {
            Vec3 $$0 = DefaultRandomPos.getPosTowards(
               this.drowned, 4, 8, new Vec3(this.drowned.getX(), (double)(this.seaLevel - 1), this.drowned.getZ()), (float) (Math.PI / 2)
            );
            if ($$0 == null) {
               this.stuck = true;
               return;
            }

            this.drowned.getNavigation().moveTo($$0.x, $$0.y, $$0.z, this.speedModifier);
         }
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(true);
         this.stuck = false;
      }

      @Override
      public void stop() {
         this.drowned.setSearchingForLand(false);
      }
   }

   static class DrownedTridentAttackGoal extends RangedAttackGoal {
      private final Drowned drowned;

      public DrownedTridentAttackGoal(RangedAttackMob pRangedAttackMob, double pSpeedModifier, int pAttackInterval, float pAttackRadius) {
         super(pRangedAttackMob, pSpeedModifier, pAttackInterval, pAttackRadius);
         this.drowned = (Drowned)pRangedAttackMob;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.getMainHandItem().is(Items.TRIDENT);
      }

      @Override
      public void start() {
         super.start();
         this.drowned.setAggressive(true);
         this.drowned.startUsingItem(InteractionHand.MAIN_HAND);
      }

      @Override
      public void stop() {
         super.stop();
         this.drowned.stopUsingItem();
         this.drowned.setAggressive(false);
      }
   }
}
