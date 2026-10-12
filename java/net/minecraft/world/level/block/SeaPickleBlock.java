package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeaPickleBlock extends BushBlock implements BonemealableBlock, SimpleWaterloggedBlock {
   public static final MapCodec<SeaPickleBlock> CODEC = simpleCodec(SeaPickleBlock::new);
   public static final int MAX_PICKLES = 4;
   public static final IntegerProperty PICKLES = BlockStateProperties.PICKLES;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final VoxelShape ONE_AABB = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
   protected static final VoxelShape TWO_AABB = Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);
   protected static final VoxelShape THREE_AABB = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
   protected static final VoxelShape FOUR_AABB = Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0);

   @Override
   public MapCodec<SeaPickleBlock> codec() {
      return CODEC;
   }

   public SeaPickleBlock(BlockBehaviour.Properties p_56082_) {
      super(p_56082_);
      this.registerDefaultState(this.stateDefinition.any().setValue(PICKLES, Integer.valueOf(1)).setValue(WATERLOGGED, Boolean.valueOf(true)));
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState $$1 = pContext.getLevel().getBlockState(pContext.getClickedPos());
      if ($$1.is(this)) {
         return $$1.setValue(PICKLES, Integer.valueOf(Math.min(4, $$1.getValue(PICKLES) + 1)));
      } else {
         FluidState $$2 = pContext.getLevel().getFluidState(pContext.getClickedPos());
         boolean $$3 = $$2.getType() == Fluids.WATER;
         return super.getStateForPlacement(pContext).setValue(WATERLOGGED, Boolean.valueOf($$3));
      }
   }

   public static boolean isDead(BlockState pState) {
      return !pState.getValue(WATERLOGGED);
   }

   @Override
   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return !pState.getCollisionShape(pLevel, pPos).getFaceShape(Direction.UP).isEmpty() || pState.isFaceSturdy(pLevel, pPos, Direction.UP);
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos $$3 = pPos.below();
      return this.mayPlaceOn(pLevel.getBlockState($$3), pLevel, $$3);
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (!pState.canSurvive(pLevel, pCurrentPos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
         }

         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }

   @Override
   protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return !pUseContext.isSecondaryUseActive() && pUseContext.getItemInHand().is(this.asItem()) && pState.getValue(PICKLES) < 4
         ? true
         : super.canBeReplaced(pState, pUseContext);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      switch (pState.getValue(PICKLES)) {
         case 1:
         default:
            return ONE_AABB;
         case 2:
            return TWO_AABB;
         case 3:
            return THREE_AABB;
         case 4:
            return FOUR_AABB;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(PICKLES, WATERLOGGED);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return true;
   }

   @Override
   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      if (!isDead(pState) && pLevel.getBlockState(pPos.below()).is(BlockTags.CORAL_BLOCKS)) {
         int $$4 = 5;
         int $$5 = 1;
         int $$6 = 2;
         int $$7 = 0;
         int $$8 = pPos.getX() - 2;
         int $$9 = 0;

         for (int $$10 = 0; $$10 < 5; $$10++) {
            for (int $$11 = 0; $$11 < $$5; $$11++) {
               int $$12 = 2 + pPos.getY() - 1;

               for (int $$13 = $$12 - 2; $$13 < $$12; $$13++) {
                  BlockPos $$14 = new BlockPos($$8 + $$10, $$13, pPos.getZ() - $$9 + $$11);
                  if ($$14 != pPos && pRandom.nextInt(6) == 0 && pLevel.getBlockState($$14).is(Blocks.WATER)) {
                     BlockState $$15 = pLevel.getBlockState($$14.below());
                     if ($$15.is(BlockTags.CORAL_BLOCKS)) {
                        pLevel.setBlock($$14, Blocks.SEA_PICKLE.defaultBlockState().setValue(PICKLES, Integer.valueOf(pRandom.nextInt(4) + 1)), 3);
                     }
                  }
               }
            }

            if ($$7 < 2) {
               $$5 += 2;
               $$9++;
            } else {
               $$5 -= 2;
               $$9--;
            }

            $$7++;
         }

         pLevel.setBlock(pPos, pState.setValue(PICKLES, Integer.valueOf(4)), 2);
      }
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }
}
