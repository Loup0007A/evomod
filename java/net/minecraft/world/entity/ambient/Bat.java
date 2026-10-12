package net.minecraft.world.entity.ambient;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Bat extends AmbientCreature {
   public static final float FLAP_LENGTH_SECONDS = 0.5F;
   public static final float TICKS_PER_FLAP = 10.0F;
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.BYTE);
   private static final int FLAG_RESTING = 1;
   private static final TargetingConditions BAT_RESTING_TARGETING = TargetingConditions.forNonCombat().range(4.0);
   public final AnimationState flyAnimationState = new AnimationState();
   public final AnimationState restAnimationState = new AnimationState();
   @Nullable
   private BlockPos targetPosition;

   public Bat(EntityType<? extends Bat> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      if (!pLevel.isClientSide) {
         this.setResting(true);
      }
   }

   @Override
   public boolean isFlapping() {
      return !this.isResting() && (float)this.tickCount % 10.0F == 0.0F;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_ID_FLAGS, (byte)0);
   }

   @Override
   protected float getSoundVolume() {
      return 0.1F;
   }

   @Override
   public float getVoicePitch() {
      return super.getVoicePitch() * 0.95F;
   }

   @Nullable
   @Override
   public SoundEvent getAmbientSound() {
      return this.isResting() && this.random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.BAT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BAT_DEATH;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void doPush(Entity pEntity) {
   }

   @Override
   protected void pushEntities() {
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0);
   }

   public boolean isResting() {
      return (this.entityData.get(DATA_ID_FLAGS) & 1) != 0;
   }

   public void setResting(boolean pIsResting) {
      byte $$1 = this.entityData.get(DATA_ID_FLAGS);
      if (pIsResting) {
         this.entityData.set(DATA_ID_FLAGS, (byte)($$1 | 1));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)($$1 & -2));
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isResting()) {
         this.setDeltaMovement(Vec3.ZERO);
         this.setPosRaw(this.getX(), (double)Mth.floor(this.getY()) + 1.0 - (double)this.getBbHeight(), this.getZ());
      } else {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
      }

      this.setupAnimationStates();
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      BlockPos $$0 = this.blockPosition();
      BlockPos $$1 = $$0.above();
      if (this.isResting()) {
         boolean $$2 = this.isSilent();
         if (this.level().getBlockState($$1).isRedstoneConductor(this.level(), $$0)) {
            if (this.random.nextInt(200) == 0) {
               this.yHeadRot = (float)this.random.nextInt(360);
            }

            if (this.level().getNearestPlayer(BAT_RESTING_TARGETING, this) != null) {
               this.setResting(false);
               if (!$$2) {
                  this.level().levelEvent(null, 1025, $$0, 0);
               }
            }
         } else {
            this.setResting(false);
            if (!$$2) {
               this.level().levelEvent(null, 1025, $$0, 0);
            }
         }
      } else {
         if (this.targetPosition != null && (!this.level().isEmptyBlock(this.targetPosition) || this.targetPosition.getY() <= this.level().getMinBuildHeight())
            )
          {
            this.targetPosition = null;
         }

         if (this.targetPosition == null || this.random.nextInt(30) == 0 || this.targetPosition.closerToCenterThan(this.position(), 2.0)) {
            this.targetPosition = BlockPos.containing(
               this.getX() + (double)this.random.nextInt(7) - (double)this.random.nextInt(7),
               this.getY() + (double)this.random.nextInt(6) - 2.0,
               this.getZ() + (double)this.random.nextInt(7) - (double)this.random.nextInt(7)
            );
         }

         double $$3 = (double)this.targetPosition.getX() + 0.5 - this.getX();
         double $$4 = (double)this.targetPosition.getY() + 0.1 - this.getY();
         double $$5 = (double)this.targetPosition.getZ() + 0.5 - this.getZ();
         Vec3 $$6 = this.getDeltaMovement();
         Vec3 $$7 = $$6.add((Math.signum($$3) * 0.5 - $$6.x) * 0.1F, (Math.signum($$4) * 0.7F - $$6.y) * 0.1F, (Math.signum($$5) * 0.5 - $$6.z) * 0.1F);
         this.setDeltaMovement($$7);
         float $$8 = (float)(Mth.atan2($$7.z, $$7.x) * 180.0F / (float)Math.PI) - 90.0F;
         float $$9 = Mth.wrapDegrees($$8 - this.getYRot());
         this.zza = 0.5F;
         this.setYRot(this.getYRot() + $$9);
         if (this.random.nextInt(100) == 0 && this.level().getBlockState($$1).isRedstoneConductor(this.level(), $$1)) {
            this.setResting(true);
         }
      }
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.isInvulnerableTo(pSource)) {
         return false;
      } else {
         if (!this.level().isClientSide && this.isResting()) {
            this.setResting(false);
         }

         return super.hurt(pSource, pAmount);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.entityData.set(DATA_ID_FLAGS, pCompound.getByte("BatFlags"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putByte("BatFlags", this.entityData.get(DATA_ID_FLAGS));
   }

   public static boolean checkBatSpawnRules(EntityType<Bat> pBat, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
      if (pPos.getY() >= pLevel.getSeaLevel()) {
         return false;
      } else {
         int $$5 = pLevel.getMaxLocalRawBrightness(pPos);
         int $$6 = 4;
         if (isHalloween()) {
            $$6 = 7;
         } else if (pRandom.nextBoolean()) {
            return false;
         }

         return $$5 > pRandom.nextInt($$6) ? false : checkMobSpawnRules(pBat, pLevel, pSpawnType, pPos, pRandom);
      }
   }

   private static boolean isHalloween() {
      LocalDate $$0 = LocalDate.now();
      int $$1 = $$0.get(ChronoField.DAY_OF_MONTH);
      int $$2 = $$0.get(ChronoField.MONTH_OF_YEAR);
      return $$2 == 10 && $$1 >= 20 || $$2 == 11 && $$1 <= 3;
   }

   private void setupAnimationStates() {
      if (this.isResting()) {
         this.flyAnimationState.stop();
         this.restAnimationState.startIfStopped(this.tickCount);
      } else {
         this.restAnimationState.stop();
         this.flyAnimationState.startIfStopped(this.tickCount);
      }
   }
}
