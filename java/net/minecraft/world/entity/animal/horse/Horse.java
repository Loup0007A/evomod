package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;

public class Horse extends AbstractHorse implements VariantHolder<Variant> {
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);
   private static final EntityDimensions BABY_DIMENSIONS = EntityType.HORSE
      .getDimensions()
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.HORSE.getHeight() + 0.125F, 0.0F))
      .scale(0.5F);

   public Horse(EntityType<? extends Horse> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void randomizeAttributes(RandomSource pRandom) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)generateMaxHealth(pRandom::nextInt));
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(pRandom::nextDouble));
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(pRandom::nextDouble));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_ID_TYPE_VARIANT, 0);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("Variant", this.getTypeVariant());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setTypeVariant(pCompound.getInt("Variant"));
   }

   private void setTypeVariant(int pTypeVariant) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, pTypeVariant);
   }

   private int getTypeVariant() {
      return this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   private void setVariantAndMarkings(Variant pVariant, Markings pMarking) {
      this.setTypeVariant(pVariant.getId() & 0xFF | pMarking.getId() << 8 & 0xFF00);
   }

   public Variant getVariant() {
      return Variant.byId(this.getTypeVariant() & 0xFF);
   }

   public void setVariant(Variant pVariant) {
      this.setTypeVariant(pVariant.getId() & 0xFF | this.getTypeVariant() & -256);
   }

   public Markings getMarkings() {
      return Markings.byId((this.getTypeVariant() & 0xFF00) >> 8);
   }

   @Override
   public void containerChanged(Container pInvBasic) {
      ItemStack $$1 = this.getBodyArmorItem();
      super.containerChanged(pInvBasic);
      ItemStack $$2 = this.getBodyArmorItem();
      if (this.tickCount > 20 && this.isBodyArmorItem($$2) && $$1 != $$2) {
         this.playSound(SoundEvents.HORSE_ARMOR, 0.5F, 1.0F);
      }
   }

   @Override
   protected void playGallopSound(SoundType pSoundType) {
      super.playGallopSound(pSoundType);
      if (this.random.nextInt(10) == 0) {
         this.playSound(SoundEvents.HORSE_BREATHE, pSoundType.getVolume() * 0.6F, pSoundType.getPitch());
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.HORSE_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.HORSE_DEATH;
   }

   @Nullable
   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.HORSE_EAT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.HORSE_HURT;
   }

   @Override
   protected SoundEvent getAngrySound() {
      return SoundEvents.HORSE_ANGRY;
   }

   @Override
   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      boolean $$2 = !this.isBaby() && this.isTamed() && pPlayer.isSecondaryUseActive();
      if (!this.isVehicle() && !$$2) {
         ItemStack $$3 = pPlayer.getItemInHand(pHand);
         if (!$$3.isEmpty()) {
            if (this.isFood($$3)) {
               return this.fedFood(pPlayer, $$3);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
         }

         return super.mobInteract(pPlayer, pHand);
      } else {
         return super.mobInteract(pPlayer, pHand);
      }
   }

   @Override
   public boolean canMate(Animal pOtherAnimal) {
      if (pOtherAnimal == this) {
         return false;
      } else {
         return !(pOtherAnimal instanceof Donkey) && !(pOtherAnimal instanceof Horse) ? false : this.canParent() && ((AbstractHorse)pOtherAnimal).canParent();
      }
   }

   @Nullable
   @Override
   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      if (pOtherParent instanceof Donkey) {
         Mule $$2 = EntityType.MULE.create(pLevel);
         if ($$2 != null) {
            this.setOffspringAttributes(pOtherParent, $$2);
         }

         return $$2;
      } else {
         Horse $$3 = (Horse)pOtherParent;
         Horse $$4 = EntityType.HORSE.create(pLevel);
         if ($$4 != null) {
            int $$5 = this.random.nextInt(9);
            Variant $$6;
            if ($$5 < 4) {
               $$6 = this.getVariant();
            } else if ($$5 < 8) {
               $$6 = $$3.getVariant();
            } else {
               $$6 = Util.getRandom(Variant.values(), this.random);
            }

            int $$9 = this.random.nextInt(5);
            Markings $$10;
            if ($$9 < 2) {
               $$10 = this.getMarkings();
            } else if ($$9 < 4) {
               $$10 = $$3.getMarkings();
            } else {
               $$10 = Util.getRandom(Markings.values(), this.random);
            }

            $$4.setVariantAndMarkings($$6, $$10);
            this.setOffspringAttributes(pOtherParent, $$4);
         }

         return $$4;
      }
   }

   @Override
   public boolean canUseSlot(EquipmentSlot pSlot) {
      return true;
   }

   @Override
   public boolean isBodyArmorItem(ItemStack pStack) {
      if (pStack.getItem() instanceof AnimalArmorItem $$1 && $$1.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
         return true;
      }

      return false;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pSpawnType, @Nullable SpawnGroupData pSpawnGroupData
   ) {
      RandomSource $$4 = pLevel.getRandom();
      Variant $$5;
      if (pSpawnGroupData instanceof Horse.HorseGroupData) {
         $$5 = ((Horse.HorseGroupData)pSpawnGroupData).variant;
      } else {
         $$5 = Util.getRandom(Variant.values(), $$4);
         pSpawnGroupData = new Horse.HorseGroupData($$5);
      }

      this.setVariantAndMarkings($$5, Util.getRandom(Markings.values(), $$4));
      return super.finalizeSpawn(pLevel, pDifficulty, pSpawnType, pSpawnGroupData);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose pPose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pPose);
   }

   public static class HorseGroupData extends AgeableMob.AgeableMobGroupData {
      public final Variant variant;

      public HorseGroupData(Variant pVariant) {
         super(true);
         this.variant = pVariant;
      }
   }
}
