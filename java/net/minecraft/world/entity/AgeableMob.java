package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class AgeableMob extends PathfinderMob {
   private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(AgeableMob.class, EntityDataSerializers.BOOLEAN);
   public static final int BABY_START_AGE = -24000;
   private static final int FORCED_AGE_PARTICLE_TICKS = 40;
   protected int age;
   protected int forcedAge;
   protected int forcedAgeTimer;

   protected AgeableMob(EntityType<? extends AgeableMob> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData
   ) {
      if (pSpawnGroupData == null) {
         pSpawnGroupData = new AgeableMob.AgeableMobGroupData(true);
      }

      AgeableMob.AgeableMobGroupData $$4 = (AgeableMob.AgeableMobGroupData)pSpawnGroupData;
      if ($$4.isShouldSpawnBaby() && $$4.getGroupSize() > 0 && pLevel.getRandom().nextFloat() <= $$4.getBabySpawnChance()) {
         this.setAge(-24000);
      }

      $$4.increaseGroupSizeByOne();
      return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
   }

   @Nullable
   public abstract AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent);

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_BABY_ID, false);
   }

   public boolean canBreed() {
      return false;
   }

   public int getAge() {
      if (this.level().isClientSide) {
         return this.entityData.get(DATA_BABY_ID) ? -1 : 1;
      } else {
         return this.age;
      }
   }

   public void ageUp(int pAmount, boolean pForced) {
      int $$2 = this.getAge();
      $$2 += pAmount * 20;
      if ($$2 > 0) {
         $$2 = 0;
      }

      int $$4 = $$2 - $$2;
      this.setAge($$2);
      if (pForced) {
         this.forcedAge += $$4;
         if (this.forcedAgeTimer == 0) {
            this.forcedAgeTimer = 40;
         }
      }

      if (this.getAge() == 0) {
         this.setAge(this.forcedAge);
      }
   }

   public void ageUp(int pAmount) {
      this.ageUp(pAmount, false);
   }

   public void setAge(int pAge) {
      int $$1 = this.getAge();
      this.age = pAge;
      if ($$1 < 0 && pAge >= 0 || $$1 >= 0 && pAge < 0) {
         this.entityData.set(DATA_BABY_ID, pAge < 0);
         this.ageBoundaryReached();
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("Age", this.getAge());
      pCompound.putInt("ForcedAge", this.forcedAge);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setAge(pCompound.getInt("Age"));
      this.forcedAge = pCompound.getInt("ForcedAge");
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (DATA_BABY_ID.equals(pKey)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(pKey);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level().isClientSide) {
         if (this.forcedAgeTimer > 0) {
            if (this.forcedAgeTimer % 4 == 0) {
               this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
            }

            this.forcedAgeTimer--;
         }
      } else if (this.isAlive()) {
         int $$0 = this.getAge();
         if ($$0 < 0) {
            this.setAge(++$$0);
         } else if ($$0 > 0) {
            this.setAge(--$$0);
         }
      }
   }

   protected void ageBoundaryReached() {
      if (!this.isBaby() && this.isPassenger() && this.getVehicle() instanceof Boat $$0 && !$$0.hasEnoughSpaceFor(this)) {
         this.stopRiding();
      }
   }

   @Override
   public boolean isBaby() {
      return this.getAge() < 0;
   }

   @Override
   public void setBaby(boolean pBaby) {
      this.setAge(pBaby ? -24000 : 0);
   }

   public static int getSpeedUpSecondsWhenFeeding(int pTicksUntilAdult) {
      return (int)((float)(pTicksUntilAdult / 20) * 0.1F);
   }

   public static class AgeableMobGroupData implements SpawnGroupData {
      private int groupSize;
      private final boolean shouldSpawnBaby;
      private final float babySpawnChance;

      private AgeableMobGroupData(boolean pShouldSpawnBaby, float pBabySpawnChance) {
         this.shouldSpawnBaby = pShouldSpawnBaby;
         this.babySpawnChance = pBabySpawnChance;
      }

      public AgeableMobGroupData(boolean pShouldSpawnBaby) {
         this(pShouldSpawnBaby, 0.05F);
      }

      public AgeableMobGroupData(float pBabySpawnChance) {
         this(true, pBabySpawnChance);
      }

      public int getGroupSize() {
         return this.groupSize;
      }

      public void increaseGroupSizeByOne() {
         this.groupSize++;
      }

      public boolean isShouldSpawnBaby() {
         return this.shouldSpawnBaby;
      }

      public float getBabySpawnChance() {
         return this.babySpawnChance;
      }
   }
}
