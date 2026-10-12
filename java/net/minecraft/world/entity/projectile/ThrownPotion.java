package net.minecraft.world.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownPotion extends ThrowableItemProjectile implements ItemSupplier {
   public static final double SPLASH_RANGE = 4.0;
   private static final double SPLASH_RANGE_SQ = 16.0;
   public static final Predicate<LivingEntity> WATER_SENSITIVE_OR_ON_FIRE = p_341491_ -> p_341491_.isSensitiveToWater() || p_341491_.isOnFire();

   public ThrownPotion(EntityType<? extends ThrownPotion> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public ThrownPotion(Level pLevel, LivingEntity pShooter) {
      super(EntityType.POTION, pShooter, pLevel);
   }

   public ThrownPotion(Level pLevel, double pX, double pY, double pZ) {
      super(EntityType.POTION, pX, pY, pZ, pLevel);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SPLASH_POTION;
   }

   @Override
   protected double getDefaultGravity() {
      return 0.05;
   }

   @Override
   protected void onHitBlock(BlockHitResult pResult) {
      super.onHitBlock(pResult);
      if (!this.level().isClientSide) {
         ItemStack $$1 = this.getItem();
         Direction $$2 = pResult.getDirection();
         BlockPos $$3 = pResult.getBlockPos();
         BlockPos $$4 = $$3.relative($$2);
         PotionContents $$5 = $$1.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if ($$5.is(Potions.WATER)) {
            this.dowseFire($$4);
            this.dowseFire($$4.relative($$2.getOpposite()));

            for (Direction $$6 : Direction.Plane.HORIZONTAL) {
               this.dowseFire($$4.relative($$6));
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide) {
         ItemStack $$1 = this.getItem();
         PotionContents $$2 = $$1.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         if ($$2.is(Potions.WATER)) {
            this.applyWater();
         } else if ($$2.hasEffects()) {
            if (this.isLingering()) {
               this.makeAreaOfEffectCloud($$2);
            } else {
               this.applySplash($$2.getAllEffects(), pResult.getType() == HitResult.Type.ENTITY ? ((EntityHitResult)pResult).getEntity() : null);
            }
         }

         int $$3 = $$2.potion().isPresent() && $$2.potion().get().value().hasInstantEffects() ? 2007 : 2002;
         this.level().levelEvent($$3, this.blockPosition(), $$2.getColor());
         this.discard();
      }
   }

   private void applyWater() {
      AABB $$0 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);

      for (LivingEntity $$2 : this.level().getEntitiesOfClass(LivingEntity.class, $$0, WATER_SENSITIVE_OR_ON_FIRE)) {
         double $$3 = this.distanceToSqr($$2);
         if ($$3 < 16.0) {
            if ($$2.isSensitiveToWater()) {
               $$2.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 1.0F);
            }

            if ($$2.isOnFire() && $$2.isAlive()) {
               $$2.extinguishFire();
            }
         }
      }

      for (Axolotl $$5 : this.level().getEntitiesOfClass(Axolotl.class, $$0)) {
         $$5.rehydrate();
      }
   }

   private void applySplash(Iterable<MobEffectInstance> pEffects, @Nullable Entity pEntity) {
      AABB $$2 = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
      List<LivingEntity> $$3 = this.level().getEntitiesOfClass(LivingEntity.class, $$2);
      if (!$$3.isEmpty()) {
         Entity $$4 = this.getEffectSource();

         for (LivingEntity $$5 : $$3) {
            if ($$5.isAffectedByPotions()) {
               double $$6 = this.distanceToSqr($$5);
               if ($$6 < 16.0) {
                  double $$7;
                  if ($$5 == pEntity) {
                     $$7 = 1.0;
                  } else {
                     $$7 = 1.0 - Math.sqrt($$6) / 4.0;
                  }

                  for (MobEffectInstance $$9 : pEffects) {
                     Holder<MobEffect> $$10 = $$9.getEffect();
                     if ($$10.value().isInstantenous()) {
                        $$10.value().applyInstantenousEffect(this, this.getOwner(), $$5, $$9.getAmplifier(), $$7);
                     } else {
                        int $$11 = $$9.mapDuration(p_267930_ -> (int)($$7 * (double)p_267930_ + 0.5));
                        MobEffectInstance $$12 = new MobEffectInstance($$10, $$11, $$9.getAmplifier(), $$9.isAmbient(), $$9.isVisible());
                        if (!$$12.endsWithin(20)) {
                           $$5.addEffect($$12, $$4);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void makeAreaOfEffectCloud(PotionContents pPotionContents) {
      AreaEffectCloud $$1 = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
      if (this.getOwner() instanceof LivingEntity $$2) {
         $$1.setOwner($$2);
      }

      $$1.setRadius(3.0F);
      $$1.setRadiusOnUse(-0.5F);
      $$1.setWaitTime(10);
      $$1.setRadiusPerTick(-$$1.getRadius() / (float)$$1.getDuration());
      $$1.setPotionContents(pPotionContents);
      this.level().addFreshEntity($$1);
   }

   private boolean isLingering() {
      return this.getItem().is(Items.LINGERING_POTION);
   }

   private void dowseFire(BlockPos pPos) {
      BlockState $$1 = this.level().getBlockState(pPos);
      if ($$1.is(BlockTags.FIRE)) {
         this.level().destroyBlock(pPos, false, this);
      } else if (AbstractCandleBlock.isLit($$1)) {
         AbstractCandleBlock.extinguish(null, $$1, this.level(), pPos);
      } else if (CampfireBlock.isLitCampfire($$1)) {
         this.level().levelEvent(null, 1009, pPos, 0);
         CampfireBlock.dowse(this.getOwner(), this.level(), pPos, $$1);
         this.level().setBlockAndUpdate(pPos, $$1.setValue(CampfireBlock.LIT, Boolean.valueOf(false)));
      }
   }

   @Override
   public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity pEntity, DamageSource pDamageSource) {
      double $$2 = pEntity.position().x - this.position().x;
      double $$3 = pEntity.position().z - this.position().z;
      return DoubleDoubleImmutablePair.of($$2, $$3);
   }
}
