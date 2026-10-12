package net.minecraft.world.entity.monster;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class Creeper extends Monster implements PowerableMob {
   private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private int oldSwell;
   private int swell;
   private int maxSwell = 30;
   private int explosionRadius = 3;
   private int droppedSkulls;

   public Creeper(EntityType<? extends Creeper> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new SwellGoal(this));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public int getMaxFallDistance() {
      return this.getTarget() == null ? this.getComfortableFallDistance(0.0F) : this.getComfortableFallDistance(this.getHealth() - 1.0F);
   }

   @Override
   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      boolean $$3 = super.causeFallDamage(pFallDistance, pMultiplier, pSource);
      this.swell += (int)(pFallDistance * 1.5F);
      if (this.swell > this.maxSwell - 5) {
         this.swell = this.maxSwell - 5;
      }

      return $$3;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_SWELL_DIR, -1);
      pBuilder.define(DATA_IS_POWERED, false);
      pBuilder.define(DATA_IS_IGNITED, false);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      if (this.entityData.get(DATA_IS_POWERED)) {
         pCompound.putBoolean("powered", true);
      }

      pCompound.putShort("Fuse", (short)this.maxSwell);
      pCompound.putByte("ExplosionRadius", (byte)this.explosionRadius);
      pCompound.putBoolean("ignited", this.isIgnited());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.entityData.set(DATA_IS_POWERED, pCompound.getBoolean("powered"));
      if (pCompound.contains("Fuse", 99)) {
         this.maxSwell = pCompound.getShort("Fuse");
      }

      if (pCompound.contains("ExplosionRadius", 99)) {
         this.explosionRadius = pCompound.getByte("ExplosionRadius");
      }

      if (pCompound.getBoolean("ignited")) {
         this.ignite();
      }
   }

   @Override
   public void tick() {
      if (this.isAlive()) {
         this.oldSwell = this.swell;
         if (this.isIgnited()) {
            this.setSwellDir(1);
         }

         int $$0 = this.getSwellDir();
         if ($$0 > 0 && this.swell == 0) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            this.gameEvent(GameEvent.PRIME_FUSE);
         }

         this.swell += $$0;
         if (this.swell < 0) {
            this.swell = 0;
         }

         if (this.swell >= this.maxSwell) {
            this.swell = this.maxSwell;
            this.explodeCreeper();
         }
      }

      super.tick();
   }

   @Override
   public void setTarget(@Nullable LivingEntity pTarget) {
      if (!(pTarget instanceof Goat)) {
         super.setTarget(pTarget);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.CREEPER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CREEPER_DEATH;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel pLevel, DamageSource pDamageSource, boolean pRecentlyHit) {
      super.dropCustomDeathLoot(pLevel, pDamageSource, pRecentlyHit);
      Entity $$3 = pDamageSource.getEntity();
      if ($$3 != this && $$3 instanceof Creeper $$4 && $$4.canDropMobsSkull()) {
         $$4.increaseDroppedSkulls();
         this.spawnAtLocation(Items.CREEPER_HEAD);
      }
   }

   @Override
   public boolean doHurtTarget(Entity pEntity) {
      return true;
   }

   @Override
   public boolean isPowered() {
      return this.entityData.get(DATA_IS_POWERED);
   }

   public float getSwelling(float pPartialTicks) {
      return Mth.lerp(pPartialTicks, (float)this.oldSwell, (float)this.swell) / (float)(this.maxSwell - 2);
   }

   public int getSwellDir() {
      return this.entityData.get(DATA_SWELL_DIR);
   }

   public void setSwellDir(int pState) {
      this.entityData.set(DATA_SWELL_DIR, pState);
   }

   @Override
   public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
      super.thunderHit(pLevel, pLightning);
      this.entityData.set(DATA_IS_POWERED, true);
   }

   @Override
   protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack $$2 = pPlayer.getItemInHand(pHand);
      if ($$2.is(ItemTags.CREEPER_IGNITERS)) {
         SoundEvent $$3 = $$2.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
         this.level().playSound(pPlayer, this.getX(), this.getY(), this.getZ(), $$3, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
         if (!this.level().isClientSide) {
            this.ignite();
            if (!$$2.isDamageableItem()) {
               $$2.shrink(1);
            } else {
               $$2.hurtAndBreak(1, pPlayer, getSlotForHand(pHand));
            }
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(pPlayer, pHand);
      }
   }

   private void explodeCreeper() {
      if (!this.level().isClientSide) {
         float $$0 = this.isPowered() ? 2.0F : 1.0F;
         this.dead = true;
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius * $$0, Level.ExplosionInteraction.MOB);
         this.spawnLingeringCloud();
         this.triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
         this.discard();
      }
   }

   private void spawnLingeringCloud() {
      Collection<MobEffectInstance> $$0 = this.getActiveEffects();
      if (!$$0.isEmpty()) {
         AreaEffectCloud $$1 = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
         $$1.setRadius(2.5F);
         $$1.setRadiusOnUse(-0.5F);
         $$1.setWaitTime(10);
         $$1.setDuration($$1.getDuration() / 2);
         $$1.setRadiusPerTick(-$$1.getRadius() / (float)$$1.getDuration());

         for (MobEffectInstance $$2 : $$0) {
            $$1.addEffect(new MobEffectInstance($$2));
         }

         this.level().addFreshEntity($$1);
      }
   }

   public boolean isIgnited() {
      return this.entityData.get(DATA_IS_IGNITED);
   }

   public void ignite() {
      this.entityData.set(DATA_IS_IGNITED, true);
   }

   public boolean canDropMobsSkull() {
      return this.isPowered() && this.droppedSkulls < 1;
   }

   public void increaseDroppedSkulls() {
      this.droppedSkulls++;
   }
}
