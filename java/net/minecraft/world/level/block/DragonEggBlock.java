package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DragonEggBlock extends FallingBlock {
   public static final MapCodec<DragonEggBlock> CODEC = simpleCodec(DragonEggBlock::new);
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

   @Override
   public MapCodec<DragonEggBlock> codec() {
      return CODEC;
   }

   public DragonEggBlock(BlockBehaviour.Properties p_52911_) {
      super(p_52911_);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      this.teleport(pState, pLevel, pPos);
      return InteractionResult.sidedSuccess(pLevel.isClientSide);
   }

   @Override
   protected void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      this.teleport(pState, pLevel, pPos);
   }

   private void teleport(BlockState pState, Level pLevel, BlockPos pPos) {
      WorldBorder $$3 = pLevel.getWorldBorder();

      for (int $$4 = 0; $$4 < 1000; $$4++) {
         BlockPos $$5 = pPos.offset(
            pLevel.random.nextInt(16) - pLevel.random.nextInt(16),
            pLevel.random.nextInt(8) - pLevel.random.nextInt(8),
            pLevel.random.nextInt(16) - pLevel.random.nextInt(16)
         );
         if (pLevel.getBlockState($$5).isAir() && $$3.isWithinBounds($$5)) {
            if (pLevel.isClientSide) {
               for (int $$6 = 0; $$6 < 128; $$6++) {
                  double $$7 = pLevel.random.nextDouble();
                  float $$8 = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                  float $$9 = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                  float $$10 = (pLevel.random.nextFloat() - 0.5F) * 0.2F;
                  double $$11 = Mth.lerp($$7, (double)$$5.getX(), (double)pPos.getX()) + (pLevel.random.nextDouble() - 0.5) + 0.5;
                  double $$12 = Mth.lerp($$7, (double)$$5.getY(), (double)pPos.getY()) + pLevel.random.nextDouble() - 0.5;
                  double $$13 = Mth.lerp($$7, (double)$$5.getZ(), (double)pPos.getZ()) + (pLevel.random.nextDouble() - 0.5) + 0.5;
                  pLevel.addParticle(ParticleTypes.PORTAL, $$11, $$12, $$13, (double)$$8, (double)$$9, (double)$$10);
               }
            } else {
               pLevel.setBlock($$5, pState, 2);
               pLevel.removeBlock(pPos, false);
            }

            return;
         }
      }
   }

   @Override
   protected int getDelayAfterPlace() {
      return 5;
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }
}
