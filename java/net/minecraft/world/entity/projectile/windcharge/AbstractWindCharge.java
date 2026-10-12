package net.minecraft.world.entity.projectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractWindCharge extends AbstractHurtingProjectile implements ItemSupplier {
   public static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
      true, false, Optional.empty(), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
   );
   public static final double JUMP_SCALE = 0.25;

   public AbstractWindCharge(EntityType<? extends AbstractWindCharge> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.accelerationPower = 0.0;
   }

   public AbstractWindCharge(EntityType<? extends AbstractWindCharge> pEntityType, Level pLevel, Entity pOwner, double pX, double pY, double pZ) {
      super(pEntityType, pX, pY, pZ, pLevel);
      this.setOwner(pOwner);
      this.accelerationPower = 0.0;
   }

   AbstractWindCharge(EntityType<? extends AbstractWindCharge> pEntityType, double pX, double pY, double pZ, Vec3 pMovement, Level pLevel) {
      super(pEntityType, pX, pY, pZ, pMovement, pLevel);
      this.accelerationPower = 0.0;
   }

   @Override
   protected AABB makeBoundingBox() {
      float $$0 = this.getType().getDimensions().width() / 2.0F;
      float $$1 = this.getType().getDimensions().height();
      float $$2 = 0.15F;
      return new AABB(
         this.position().x - (double)$$0,
         this.position().y - 0.15F,
         this.position().z - (double)$$0,
         this.position().x + (double)$$0,
         this.position().y - 0.15F + (double)$$1,
         this.position().z + (double)$$0
      );
   }

   @Override
   public boolean canCollideWith(Entity pEntity) {
      return pEntity instanceof AbstractWindCharge ? false : super.canCollideWith(pEntity);
   }

   @Override
   protected boolean canHitEntity(Entity pTarget) {
      if (pTarget instanceof AbstractWindCharge) {
         return false;
      } else {
         return pTarget.getType() == EntityType.END_CRYSTAL ? false : super.canHitEntity(pTarget);
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level().isClientSide) {
         LivingEntity $$2 = this.getOwner() instanceof LivingEntity $$1 ? $$1 : null;
         Entity $$3 = pResult.getEntity();
         if ($$2 != null) {
            $$2.setLastHurtMob($$3);
         }

         DamageSource $$4 = this.damageSources().windCharge(this, $$2);
         if ($$3.hurt($$4, 1.0F) && $$3 instanceof LivingEntity $$5) {
            EnchantmentHelper.doPostAttackEffects((ServerLevel)this.level(), $$5, $$4);
         }

         this.explode(this.position());
      }
   }

   @Override
   public void push(double pX, double pY, double pZ) {
   }

   protected abstract void explode(Vec3 pPos);

   @Override
   protected void onHitBlock(BlockHitResult pResult) {
      super.onHitBlock(pResult);
      if (!this.level().isClientSide) {
         Vec3i $$1 = pResult.getDirection().getNormal();
         Vec3 $$2 = Vec3.atLowerCornerOf($$1).multiply(0.25, 0.25, 0.25);
         Vec3 $$3 = pResult.getLocation().add($$2);
         this.explode($$3);
         this.discard();
      }
   }

   @Override
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide) {
         this.discard();
      }
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }

   @Override
   public ItemStack getItem() {
      return ItemStack.EMPTY;
   }

   @Override
   protected float getInertia() {
      return 1.0F;
   }

   @Override
   protected float getLiquidInertia() {
      return this.getInertia();
   }

   @Nullable
   @Override
   protected ParticleOptions getTrailParticle() {
      return null;
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide && this.getBlockY() > this.level().getMaxBuildHeight() + 30) {
         this.explode(this.position());
         this.discard();
      } else {
         super.tick();
      }
   }

   @Override
   public boolean hurt(DamageSource pSource, float pAmount) {
      return false;
   }
}
