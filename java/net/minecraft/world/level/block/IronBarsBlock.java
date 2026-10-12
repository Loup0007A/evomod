package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);

   @Override
   public MapCodec<? extends IronBarsBlock> codec() {
      return CODEC;
   }

   public IronBarsBlock(BlockBehaviour.Properties p_54198_) {
      super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, p_54198_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, Boolean.valueOf(false))
            .setValue(EAST, Boolean.valueOf(false))
            .setValue(SOUTH, Boolean.valueOf(false))
            .setValue(WEST, Boolean.valueOf(false))
            .setValue(WATERLOGGED, Boolean.valueOf(false))
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockGetter $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      FluidState $$3 = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockPos $$4 = $$2.north();
      BlockPos $$5 = $$2.south();
      BlockPos $$6 = $$2.west();
      BlockPos $$7 = $$2.east();
      BlockState $$8 = $$1.getBlockState($$4);
      BlockState $$9 = $$1.getBlockState($$5);
      BlockState $$10 = $$1.getBlockState($$6);
      BlockState $$11 = $$1.getBlockState($$7);
      return this.defaultBlockState()
         .setValue(NORTH, Boolean.valueOf(this.attachsTo($$8, $$8.isFaceSturdy($$1, $$4, Direction.SOUTH))))
         .setValue(SOUTH, Boolean.valueOf(this.attachsTo($$9, $$9.isFaceSturdy($$1, $$5, Direction.NORTH))))
         .setValue(WEST, Boolean.valueOf(this.attachsTo($$10, $$10.isFaceSturdy($$1, $$6, Direction.EAST))))
         .setValue(EAST, Boolean.valueOf(this.attachsTo($$11, $$11.isFaceSturdy($$1, $$7, Direction.WEST))))
         .setValue(WATERLOGGED, Boolean.valueOf($$3.getType() == Fluids.WATER));
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return pFacing.getAxis().isHorizontal()
         ? pState.setValue(
            PROPERTY_BY_DIRECTION.get(pFacing),
            Boolean.valueOf(this.attachsTo(pFacingState, pFacingState.isFaceSturdy(pLevel, pFacingPos, pFacing.getOpposite())))
         )
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   protected VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
      return Shapes.empty();
   }

   @Override
   protected boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pSide) {
      if (pAdjacentBlockState.is(this)) {
         if (!pSide.getAxis().isHorizontal()) {
            return true;
         }

         if (pState.getValue(PROPERTY_BY_DIRECTION.get(pSide)) && pAdjacentBlockState.getValue(PROPERTY_BY_DIRECTION.get(pSide.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(pState, pAdjacentBlockState, pSide);
   }

   public final boolean attachsTo(BlockState pState, boolean pSolidSide) {
      return !isExceptionForConnection(pState) && pSolidSide || pState.getBlock() instanceof IronBarsBlock || pState.is(BlockTags.WALLS);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
