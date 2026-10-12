package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock extends PipeBlock {
   public static final MapCodec<ChorusPlantBlock> CODEC = simpleCodec(ChorusPlantBlock::new);

   @Override
   public MapCodec<ChorusPlantBlock> codec() {
      return CODEC;
   }

   public ChorusPlantBlock(BlockBehaviour.Properties p_51707_) {
      super(0.3125F, p_51707_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, Boolean.valueOf(false))
            .setValue(EAST, Boolean.valueOf(false))
            .setValue(SOUTH, Boolean.valueOf(false))
            .setValue(WEST, Boolean.valueOf(false))
            .setValue(UP, Boolean.valueOf(false))
            .setValue(DOWN, Boolean.valueOf(false))
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return getStateWithConnections(pContext.getLevel(), pContext.getClickedPos(), this.defaultBlockState());
   }

   public static BlockState getStateWithConnections(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      BlockState $$3 = pLevel.getBlockState(pPos.below());
      BlockState $$4 = pLevel.getBlockState(pPos.above());
      BlockState $$5 = pLevel.getBlockState(pPos.north());
      BlockState $$6 = pLevel.getBlockState(pPos.east());
      BlockState $$7 = pLevel.getBlockState(pPos.south());
      BlockState $$8 = pLevel.getBlockState(pPos.west());
      Block $$9 = pState.getBlock();
      return pState.trySetValue(DOWN, Boolean.valueOf($$3.is($$9) || $$3.is(Blocks.CHORUS_FLOWER) || $$3.is(Blocks.END_STONE)))
         .trySetValue(UP, Boolean.valueOf($$4.is($$9) || $$4.is(Blocks.CHORUS_FLOWER)))
         .trySetValue(NORTH, Boolean.valueOf($$5.is($$9) || $$5.is(Blocks.CHORUS_FLOWER)))
         .trySetValue(EAST, Boolean.valueOf($$6.is($$9) || $$6.is(Blocks.CHORUS_FLOWER)))
         .trySetValue(SOUTH, Boolean.valueOf($$7.is($$9) || $$7.is(Blocks.CHORUS_FLOWER)))
         .trySetValue(WEST, Boolean.valueOf($$8.is($$9) || $$8.is(Blocks.CHORUS_FLOWER)));
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (!pState.canSurvive(pLevel, pCurrentPos)) {
         pLevel.scheduleTick(pCurrentPos, this, 1);
         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      } else {
         boolean $$6 = pFacingState.is(this) || pFacingState.is(Blocks.CHORUS_FLOWER) || pFacing == Direction.DOWN && pFacingState.is(Blocks.END_STONE);
         return pState.setValue(PROPERTY_BY_DIRECTION.get(pFacing), Boolean.valueOf($$6));
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!pState.canSurvive(pLevel, pPos)) {
         pLevel.destroyBlock(pPos, true);
      }
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockState $$3 = pLevel.getBlockState(pPos.below());
      boolean $$4 = !pLevel.getBlockState(pPos.above()).isAir() && !$$3.isAir();

      for (Direction $$5 : Direction.Plane.HORIZONTAL) {
         BlockPos $$6 = pPos.relative($$5);
         BlockState $$7 = pLevel.getBlockState($$6);
         if ($$7.is(this)) {
            if ($$4) {
               return false;
            }

            BlockState $$8 = pLevel.getBlockState($$6.below());
            if ($$8.is(this) || $$8.is(Blocks.END_STONE)) {
               return true;
            }
         }
      }

      return $$3.is(this) || $$3.is(Blocks.END_STONE);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }
}
