package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBlock extends Block {
   protected final Direction growthDirection;
   protected final boolean scheduleFluidTicks;
   protected final VoxelShape shape;

   protected GrowingPlantBlock(BlockBehaviour.Properties pProperties, Direction pGrowthDirection, VoxelShape pShape, boolean pScheduleFluidTicks) {
      super(pProperties);
      this.growthDirection = pGrowthDirection;
      this.shape = pShape;
      this.scheduleFluidTicks = pScheduleFluidTicks;
   }

   @Override
   protected abstract MapCodec<? extends GrowingPlantBlock> codec();

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState $$1 = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(this.growthDirection));
      return !$$1.is(this.getHeadBlock()) && !$$1.is(this.getBodyBlock())
         ? this.getStateForPlacement(pContext.getLevel())
         : this.getBodyBlock().defaultBlockState();
   }

   public BlockState getStateForPlacement(LevelAccessor pLevel) {
      return this.defaultBlockState();
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos $$3 = pPos.relative(this.growthDirection.getOpposite());
      BlockState $$4 = pLevel.getBlockState($$3);
      return !this.canAttachTo($$4) ? false : $$4.is(this.getHeadBlock()) || $$4.is(this.getBodyBlock()) || $$4.isFaceSturdy(pLevel, $$3, this.growthDirection);
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!pState.canSurvive(pLevel, pPos)) {
         pLevel.destroyBlock(pPos, true);
      }
   }

   protected boolean canAttachTo(BlockState pState) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.shape;
   }

   protected abstract GrowingPlantHeadBlock getHeadBlock();

   protected abstract Block getBodyBlock();
}
