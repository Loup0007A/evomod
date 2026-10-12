package net.minecraft.world.entity.animal;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class IronGolem extends AbstractGolem implements NeutralMob {
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(IronGolem.class, EntityDataSerializers.BYTE);
   private static final int IRON_INGOT_HEAL_AMOUNT = 25;
   private int attackAnimationTick;
   private int offerFlowerTick;
   private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
   private int remainingPersistentAngerTime;
   @Nullable
   private UUID persistentAngerTarget;

   public IronGolem(EntityType<? extends IronGolem> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
      this.goalSelector.addGoal(2, new MoveBackToVillageGoal(this, 0.6, false));
      this.goalSelector.addGoal(4, new GolemRandomStrollInVillageGoal(this, 0.6));
      this.goalSelector.addGoal(5, new OfferFlowerGoal(this));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new DefendVillageTargetGoal(this));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
      this.targetSelector
         .addGoal(
            3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, p_28879_ -> p_28879_ instanceof Enemy && !(p_28879_ instanceof Creeper))
         );
      this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_FLAGS_ID, (byte)0);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
         .add(Attributes.ATTACK_DAMAGE, 15.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected int decreaseAirSupply(int pAir) {
      return pAir;
   }

   @Override
   protected void doPush(Entity pEntity) {
      if (pEntity instanceof Enemy && !(pEntity instanceof Creeper) && this.getRandom().nextInt(20) == 0) {
         this.setTarget((LivingEntity)pEntity);
      }

      super.doPush(pEntity);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.attackAnimationTick > 0) {
         this.attackAnimationTick--;
      }

      if (this.offerFlowerTick > 0) {
         this.offerFlowerTick--;
      }

      if (!this.level().isClientSide) {
         this.updatePersistentAnger((ServerLevel)this.level(), true);
      }
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 2.5000003E-7F && this.random.nextInt(5) == 0;
   }

   @Override
   public boolean canAttackType(EntityType<?> pType) {
      if (this.isPlayerCreated() && pType == EntityType.PLAYER) {
         return false;
      } else {
         return pType == EntityType.CREEPER ? false : super.canAttackType(pType);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putBoolean("PlayerCreated", this.isPlayerCreated());
      this.addPersistentAngerSaveData(pCompound);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setPlayerCreated(pCompound.getBoolean("PlayerCreated"));
      this.readPersistentAngerSaveData(this.level(), pCompound);
   }

   @Override
   public void startPersistentAngerTimer() {
      this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
   }

   @Override
   public void setRemainingPersistentAngerTime(int pTime) {
      this.remainingPersistentAngerTime = pTime;
   }

   @Override
   public int getRemainingPersistentAngerTime() {
      return this.remainingPersistentAngerTime;
   }

   @Override
   public void setPersistentAngerTarget(@Nullable UUID pTarget) {
      this.persistentAngerTarget = pTarget;
   }

   @Nullable
   @Override
   public UUID getPersistentAngerTarget() {
      return this.persistentAngerTarget;
   }

   private float getAttackDamage() {
      return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
   }

   @Override
   public boolean doHurtTarget(Entity pEntity) {
      this.attackAnimationTick = 10;
      this.level().broadcastEntityEvent(this, (byte)4);
      float $$1 = this.getAttackDamage();
      float $$2 = (int)$$1 > 0 ? $$1 / 2.0F + (float)this.random.nextInt((int)$$1) : $$1;
      DamageSource $$3 = this.damageSources().mobAttack(this);
      boolean $$4 = pEntity.hurt($$3, $$2);
      if ($$4) {
         double $$6 = pEntity instanceof LivingEntity $$5 ? $$5.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
         double $$7 = Math.max(0.0, 1.0 - $$6);
         pEntity.setDeltaMovement(pEntity.getDeltaMovement().add(0.0, 0.4F * $$7, 0.0));
         if (this.level() instanceof ServerLevel $$8) {
            EnchantmentHelper.doPostAttackEffects($$8, pEntity, $$3);
         }
      }

      this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      return $$4;
   }

   @Override
   public boolean hurt(DamageSource pSource, float pAmount) {
      Crackiness.Level $$2 = this.getCrackiness();
      boolean $$3 = super.hurt(pSource, pAmount);
      if ($$3 && this.getCrackiness() != $$2) {
         this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 1.0F);
      }

      return $$3;
   }

   public Crackiness.Level getCrackiness() {
      return Crackiness.GOLEM.byFraction(this.getHealth() / this.getMaxHealth());
   }

   @Override
   public void handleEntityEvent(byte pId) {
      if (pId == 4) {
         this.attackAnimationTick = 10;
         this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      } else if (pId == 11) {
         this.offerFlowerTick = 400;
      } else if (pId == 34) {
         this.offerFlowerTick = 0;
      } else {
         super.handleEntityEvent(pId);
      }
   }

   public int getAttackAnimationTick() {
      return this.attackAnimationTick;
   }

   public void offerFlower(boolean pOfferingFlower) {
      if (pOfferingFlower) {
         this.offerFlowerTick = 400;
         this.level().broadcastEntityEvent(this, (byte)11);
      } else {
         this.offerFlowerTick = 0;
         this.level().broadcastEntityEvent(this, (byte)34);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   @Override
   protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack $$2 = pPlayer.getItemInHand(pHand);
      if (!$$2.is(Items.IRON_INGOT)) {
         return InteractionResult.PASS;
      } else {
         float $$3 = this.getHealth();
         this.heal(25.0F);
         if (this.getHealth() == $$3) {
            return InteractionResult.PASS;
         } else {
            float $$4 = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
            this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, $$4);
            $$2.consume(1, pPlayer);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
         }
      }
   }

   @Override
   protected void playStepSound(BlockPos pPos, BlockState pBlock) {
      this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
   }

   public int getOfferFlowerTick() {
      return this.offerFlowerTick;
   }

   public boolean isPlayerCreated() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setPlayerCreated(boolean pPlayerCreated) {
      byte $$1 = this.entityData.get(DATA_FLAGS_ID);
      if (pPlayerCreated) {
         this.entityData.set(DATA_FLAGS_ID, (byte)($$1 | 1));
      } else {
         this.entityData.set(DATA_FLAGS_ID, (byte)($$1 & -2));
      }
   }

   @Override
   public void die(DamageSource pCause) {
      super.die(pCause);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader pLevel) {
      BlockPos $$1 = this.blockPosition();
      BlockPos $$2 = $$1.below();
      BlockState $$3 = pLevel.getBlockState($$2);
      if (!$$3.entityCanStandOn(pLevel, $$2, this)) {
         return false;
      } else {
         for (int $$4 = 1; $$4 < 3; $$4++) {
            BlockPos $$5 = $$1.above($$4);
            BlockState $$6 = pLevel.getBlockState($$5);
            if (!NaturalSpawner.isValidEmptySpawnBlock(pLevel, $$5, $$6, $$6.getFluidState(), EntityType.IRON_GOLEM)) {
               return false;
            }
         }

         return NaturalSpawner.isValidEmptySpawnBlock(pLevel, $$1, pLevel.getBlockState($$1), Fluids.EMPTY.defaultFluidState(), EntityType.IRON_GOLEM)
            && pLevel.isUnobstructed(this);
      }
   }

   @Override
   public Vec3 getLeashOffset() {
      return new Vec3(0.0, (double)(0.875F * this.getEyeHeight()), (double)(this.getBbWidth() * 0.4F));
   }
}
