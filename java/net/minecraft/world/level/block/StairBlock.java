package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StairBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<StairBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_309295_ -> p_309295_.group(BlockState.CODEC.fieldOf("base_state").forGetter(p_309296_ -> p_309296_.baseState), propertiesCodec())
            .apply(p_309295_, StairBlock::new)
   );
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final VoxelShape TOP_AABB = SlabBlock.TOP_AABB;
   protected static final VoxelShape BOTTOM_AABB = SlabBlock.BOTTOM_AABB;
   protected static final VoxelShape OCTET_NNN = Block.box(0.0, 0.0, 0.0, 8.0, 8.0, 8.0);
   protected static final VoxelShape OCTET_NNP = Block.box(0.0, 0.0, 8.0, 8.0, 8.0, 16.0);
   protected static final VoxelShape OCTET_NPN = Block.box(0.0, 8.0, 0.0, 8.0, 16.0, 8.0);
   protected static final VoxelShape OCTET_NPP = Block.box(0.0, 8.0, 8.0, 8.0, 16.0, 16.0);
   protected static final VoxelShape OCTET_PNN = Block.box(8.0, 0.0, 0.0, 16.0, 8.0, 8.0);
   protected static final VoxelShape OCTET_PNP = Block.box(8.0, 0.0, 8.0, 16.0, 8.0, 16.0);
   protected static final VoxelShape OCTET_PPN = Block.box(8.0, 8.0, 0.0, 16.0, 16.0, 8.0);
   protected static final VoxelShape OCTET_PPP = Block.box(8.0, 8.0, 8.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape[] TOP_SHAPES = makeShapes(TOP_AABB, OCTET_NNN, OCTET_PNN, OCTET_NNP, OCTET_PNP);
   protected static final VoxelShape[] BOTTOM_SHAPES = makeShapes(BOTTOM_AABB, OCTET_NPN, OCTET_PPN, OCTET_NPP, OCTET_PPP);
   private static final int[] SHAPE_BY_STATE = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};
   private final Block base;
   protected final BlockState baseState;

   @Override
   public MapCodec<? extends StairBlock> codec() {
      return CODEC;
   }

   private static VoxelShape[] makeShapes(VoxelShape pSlabShape, VoxelShape pNwCorner, VoxelShape pNeCorner, VoxelShape pSwCorner, VoxelShape pSeCorner) {
      return IntStream.range(0, 16)
         .mapToObj(p_56945_ -> makeStairShape(p_56945_, pSlabShape, pNwCorner, pNeCorner, pSwCorner, pSeCorner))
         .toArray(VoxelShape[]::new);
   }

   private static VoxelShape makeStairShape(
      int pBitfield, VoxelShape pSlabShape, VoxelShape pNwCorner, VoxelShape pNeCorner, VoxelShape pSwCorner, VoxelShape pSeCorner
   ) {
      VoxelShape $$6 = pSlabShape;
      if ((pBitfield & 1) != 0) {
         $$6 = Shapes.or(pSlabShape, pNwCorner);
      }

      if ((pBitfield & 2) != 0) {
         $$6 = Shapes.or($$6, pNeCorner);
      }

      if ((pBitfield & 4) != 0) {
         $$6 = Shapes.or($$6, pSwCorner);
      }

      if ((pBitfield & 8) != 0) {
         $$6 = Shapes.or($$6, pSeCorner);
      }

      return $$6;
   }

   public StairBlock(BlockState p_56862_, BlockBehaviour.Properties p_56863_) {
      super(p_56863_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, Half.BOTTOM)
            .setValue(SHAPE, StairsShape.STRAIGHT)
            .setValue(WATERLOGGED, Boolean.valueOf(false))
      );
      this.base = p_56862_.getBlock();
      this.baseState = p_56862_;
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState pState) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return (pState.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_BY_STATE[this.getShapeIndex(pState)]];
   }

   private int getShapeIndex(BlockState pState) {
      return pState.getValue(SHAPE).ordinal() * 4 + pState.getValue(FACING).get2DDataValue();
   }

   @Override
   public float getExplosionResistance() {
      return this.base.getExplosionResistance();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction $$1 = pContext.getClickedFace();
      BlockPos $$2 = pContext.getClickedPos();
      FluidState $$3 = pContext.getLevel().getFluidState($$2);
      BlockState $$4 = this.defaultBlockState()
         .setValue(FACING, pContext.getHorizontalDirection())
         .setValue(HALF, $$1 != Direction.DOWN && ($$1 == Direction.UP || !(pContext.getClickLocation().y - (double)$$2.getY() > 0.5)) ? Half.BOTTOM : Half.TOP)
         .setValue(WATERLOGGED, Boolean.valueOf($$3.getType() == Fluids.WATER));
      return $$4.setValue(SHAPE, getStairsShape($$4, pContext.getLevel(), $$2));
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return pFacing.getAxis().isHorizontal()
         ? pState.setValue(SHAPE, getStairsShape(pState, pLevel, pCurrentPos))
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   private static StairsShape getStairsShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      Direction $$3 = pState.getValue(FACING);
      BlockState $$4 = pLevel.getBlockState(pPos.relative($$3));
      if (isStairs($$4) && pState.getValue(HALF) == $$4.getValue(HALF)) {
         Direction $$5 = $$4.getValue(FACING);
         if ($$5.getAxis() != pState.getValue(FACING).getAxis() && canTakeShape(pState, pLevel, pPos, $$5.getOpposite())) {
            if ($$5 == $$3.getCounterClockWise()) {
               return StairsShape.OUTER_LEFT;
            }

            return StairsShape.OUTER_RIGHT;
         }
      }

      BlockState $$6 = pLevel.getBlockState(pPos.relative($$3.getOpposite()));
      if (isStairs($$6) && pState.getValue(HALF) == $$6.getValue(HALF)) {
         Direction $$7 = $$6.getValue(FACING);
         if ($$7.getAxis() != pState.getValue(FACING).getAxis() && canTakeShape(pState, pLevel, pPos, $$7)) {
            if ($$7 == $$3.getCounterClockWise()) {
               return StairsShape.INNER_LEFT;
            }

            return StairsShape.INNER_RIGHT;
         }
      }

      return StairsShape.STRAIGHT;
   }

   private static boolean canTakeShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pFace) {
      BlockState $$4 = pLevel.getBlockState(pPos.relative(pFace));
      return !isStairs($$4) || $$4.getValue(FACING) != pState.getValue(FACING) || $$4.getValue(HALF) != pState.getValue(HALF);
   }

   public static boolean isStairs(BlockState pState) {
      return pState.getBlock() instanceof StairBlock;
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRot) {
      return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      Direction $$2 = pState.getValue(FACING);
      StairsShape $$3 = pState.getValue(SHAPE);
      switch (pMirror) {
         case LEFT_RIGHT:
            if ($$2.getAxis() == Direction.Axis.Z) {
               switch ($$3) {
                  case INNER_LEFT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  case INNER_RIGHT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                  case OUTER_LEFT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case OUTER_RIGHT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                  default:
                     return pState.rotate(Rotation.CLOCKWISE_180);
               }
            }
            break;
         case FRONT_BACK:
            if ($$2.getAxis() == Direction.Axis.X) {
               switch ($$3) {
                  case INNER_LEFT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                  case INNER_RIGHT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                  case OUTER_LEFT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                  case OUTER_RIGHT:
                     return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                  case STRAIGHT:
                     return pState.rotate(Rotation.CLOCKWISE_180);
               }
            }
      }

      return super.mirror(pState, pMirror);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, HALF, SHAPE, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }
}
