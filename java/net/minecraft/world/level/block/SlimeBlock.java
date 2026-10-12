package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SlimeBlock extends HalfTransparentBlock {
   public static final MapCodec<SlimeBlock> CODEC = simpleCodec(SlimeBlock::new);

   @Override
   public MapCodec<SlimeBlock> codec() {
      return CODEC;
   }

   public SlimeBlock(BlockBehaviour.Properties p_56402_) {
      super(p_56402_);
   }

   @Override
   public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
      if (pEntity.isSuppressingBounce()) {
         super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
      } else {
         pEntity.causeFallDamage(pFallDistance, 0.0F, pLevel.damageSources().fall());
      }
   }

   @Override
   public void updateEntityAfterFallOn(BlockGetter pLevel, Entity pEntity) {
      if (pEntity.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(pLevel, pEntity);
      } else {
         this.bounceUp(pEntity);
      }
   }

   private void bounceUp(Entity pEntity) {
      Vec3 $$1 = pEntity.getDeltaMovement();
      if ($$1.y < 0.0) {
         double $$2 = pEntity instanceof LivingEntity ? 1.0 : 0.8;
         pEntity.setDeltaMovement($$1.x, -$$1.y * $$2, $$1.z);
      }
   }

   @Override
   public void stepOn(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity) {
      double $$4 = Math.abs(pEntity.getDeltaMovement().y);
      if ($$4 < 0.1 && !pEntity.isSteppingCarefully()) {
         double $$5 = 0.4 + $$4 * 0.2;
         pEntity.setDeltaMovement(pEntity.getDeltaMovement().multiply($$5, 1.0, $$5));
      }

      super.stepOn(pLevel, pPos, pState, pEntity);
   }
}
