package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class EndGatewayBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndGatewayBlock> CODEC = simpleCodec(EndGatewayBlock::new);

   @Override
   public MapCodec<EndGatewayBlock> codec() {
      return CODEC;
   }

   public EndGatewayBlock(BlockBehaviour.Properties p_52999_) {
      super(p_52999_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new TheEndGatewayBlockEntity(pPos, pState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createTickerHelper(
         pBlockEntityType,
         BlockEntityType.END_GATEWAY,
         pLevel.isClientSide ? TheEndGatewayBlockEntity::beamAnimationTick : TheEndGatewayBlockEntity::portalTick
      );
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockEntity $$4 = pLevel.getBlockEntity(pPos);
      if ($$4 instanceof TheEndGatewayBlockEntity) {
         int $$5 = ((TheEndGatewayBlockEntity)$$4).getParticleAmount();

         for (int $$6 = 0; $$6 < $$5; $$6++) {
            double $$7 = (double)pPos.getX() + pRandom.nextDouble();
            double $$8 = (double)pPos.getY() + pRandom.nextDouble();
            double $$9 = (double)pPos.getZ() + pRandom.nextDouble();
            double $$10 = (pRandom.nextDouble() - 0.5) * 0.5;
            double $$11 = (pRandom.nextDouble() - 0.5) * 0.5;
            double $$12 = (pRandom.nextDouble() - 0.5) * 0.5;
            int $$13 = pRandom.nextInt(2) * 2 - 1;
            if (pRandom.nextBoolean()) {
               $$9 = (double)pPos.getZ() + 0.5 + 0.25 * (double)$$13;
               $$12 = (double)(pRandom.nextFloat() * 2.0F * (float)$$13);
            } else {
               $$7 = (double)pPos.getX() + 0.5 + 0.25 * (double)$$13;
               $$10 = (double)(pRandom.nextFloat() * 2.0F * (float)$$13);
            }

            pLevel.addParticle(ParticleTypes.PORTAL, $$7, $$8, $$9, $$10, $$11, $$12);
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return ItemStack.EMPTY;
   }

   @Override
   protected boolean canBeReplaced(BlockState pState, Fluid pFluid) {
      return false;
   }

   @Override
   protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (pEntity.canUsePortal(false) && !pLevel.isClientSide && pLevel.getBlockEntity(pPos) instanceof TheEndGatewayBlockEntity $$5 && !$$5.isCoolingDown()) {
         pEntity.setAsInsidePortal(this, pPos);
         TheEndGatewayBlockEntity.triggerCooldown(pLevel, pPos, pState, $$5);
      }
   }

   @Nullable
   @Override
   public DimensionTransition getPortalDestination(ServerLevel pLevel, Entity pEntity, BlockPos pPos) {
      if (pLevel.getBlockEntity(pPos) instanceof TheEndGatewayBlockEntity $$4) {
         Vec3 $$6 = $$4.getPortalPosition(pLevel, pPos);
         return $$6 != null
            ? new DimensionTransition(
               pLevel, $$6, calculateExitMovement(pEntity), pEntity.getYRot(), pEntity.getXRot(), DimensionTransition.PLACE_PORTAL_TICKET
            )
            : null;
      } else {
         return null;
      }
   }

   private static Vec3 calculateExitMovement(Entity pEntity) {
      return pEntity instanceof ThrownEnderpearl ? new Vec3(0.0, -1.0, 0.0) : pEntity.getDeltaMovement();
   }
}
