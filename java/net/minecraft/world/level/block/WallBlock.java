package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<WallBlock> CODEC = simpleCodec(WallBlock::new);
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST_WALL = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH_WALL = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH_WALL = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST_WALL = BlockStateProperties.WEST_WALL;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Map<BlockState, VoxelShape> shapeByIndex;
   private final Map<BlockState, VoxelShape> collisionShapeByIndex;
   private static final int WALL_WIDTH = 3;
   private static final int WALL_HEIGHT = 14;
   private static final int POST_WIDTH = 4;
   private static final int POST_COVER_WIDTH = 1;
   private static final int WALL_COVER_START = 7;
   private static final int WALL_COVER_END = 9;
   private static final VoxelShape POST_TEST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape NORTH_TEST = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 9.0);
   private static final VoxelShape SOUTH_TEST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 16.0);
   private static final VoxelShape WEST_TEST = Block.box(0.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape EAST_TEST = Block.box(7.0, 0.0, 7.0, 16.0, 16.0, 9.0);

   @Override
   public MapCodec<WallBlock> codec() {
      return CODEC;
   }

   public WallBlock(BlockBehaviour.Properties p_57964_) {
      super(p_57964_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(UP, Boolean.valueOf(true))
            .setValue(NORTH_WALL, WallSide.NONE)
            .setValue(EAST_WALL, WallSide.NONE)
            .setValue(SOUTH_WALL, WallSide.NONE)
            .setValue(WEST_WALL, WallSide.NONE)
            .setValue(WATERLOGGED, Boolean.valueOf(false))
      );
      this.shapeByIndex = this.makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
      this.collisionShapeByIndex = this.makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
   }

   private static VoxelShape applyWallShape(VoxelShape pBaseShape, WallSide pHeight, VoxelShape pLowShape, VoxelShape pTallShape) {
      if (pHeight == WallSide.TALL) {
         return Shapes.or(pBaseShape, pTallShape);
      } else {
         return pHeight == WallSide.LOW ? Shapes.or(pBaseShape, pLowShape) : pBaseShape;
      }
   }

   private Map<BlockState, VoxelShape> makeShapes(
      float pWidth, float pDepth, float pWallPostHeight, float pWallMinY, float pWallLowHeight, float pWallTallHeight
   ) {
      float $$6 = 8.0F - pWidth;
      float $$7 = 8.0F + pWidth;
      float $$8 = 8.0F - pDepth;
      float $$9 = 8.0F + pDepth;
      VoxelShape $$10 = Block.box((double)$$6, 0.0, (double)$$6, (double)$$7, (double)pWallPostHeight, (double)$$7);
      VoxelShape $$11 = Block.box((double)$$8, (double)pWallMinY, 0.0, (double)$$9, (double)pWallLowHeight, (double)$$9);
      VoxelShape $$12 = Block.box((double)$$8, (double)pWallMinY, (double)$$8, (double)$$9, (double)pWallLowHeight, 16.0);
      VoxelShape $$13 = Block.box(0.0, (double)pWallMinY, (double)$$8, (double)$$9, (double)pWallLowHeight, (double)$$9);
      VoxelShape $$14 = Block.box((double)$$8, (double)pWallMinY, (double)$$8, 16.0, (double)pWallLowHeight, (double)$$9);
      VoxelShape $$15 = Block.box((double)$$8, (double)pWallMinY, 0.0, (double)$$9, (double)pWallTallHeight, (double)$$9);
      VoxelShape $$16 = Block.box((double)$$8, (double)pWallMinY, (double)$$8, (double)$$9, (double)pWallTallHeight, 16.0);
      VoxelShape $$17 = Block.box(0.0, (double)pWallMinY, (double)$$8, (double)$$9, (double)pWallTallHeight, (double)$$9);
      VoxelShape $$18 = Block.box((double)$$8, (double)pWallMinY, (double)$$8, 16.0, (double)pWallTallHeight, (double)$$9);
      Builder<BlockState, VoxelShape> $$19 = ImmutableMap.builder();

      for (Boolean $$20 : UP.getPossibleValues()) {
         for (WallSide $$21 : EAST_WALL.getPossibleValues()) {
            for (WallSide $$22 : NORTH_WALL.getPossibleValues()) {
               for (WallSide $$23 : WEST_WALL.getPossibleValues()) {
                  for (WallSide $$24 : SOUTH_WALL.getPossibleValues()) {
                     VoxelShape $$25 = Shapes.empty();
                     $$25 = applyWallShape($$25, $$21, $$14, $$18);
                     $$25 = applyWallShape($$25, $$23, $$13, $$17);
                     $$25 = applyWallShape($$25, $$22, $$11, $$15);
                     $$25 = applyWallShape($$25, $$24, $$12, $$16);
                     if ($$20) {
                        $$25 = Shapes.or($$25, $$10);
                     }

                     BlockState $$26 = this.defaultBlockState()
                        .setValue(UP, $$20)
                        .setValue(EAST_WALL, $$21)
                        .setValue(WEST_WALL, $$23)
                        .setValue(NORTH_WALL, $$22)
                        .setValue(SOUTH_WALL, $$24);
                     $$19.put($$26.setValue(WATERLOGGED, Boolean.valueOf(false)), $$25);
                     $$19.put($$26.setValue(WATERLOGGED, Boolean.valueOf(true)), $$25);
                  }
               }
            }
         }
      }

      return $$19.build();
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.shapeByIndex.get(pState);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.collisionShapeByIndex.get(pState);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   private boolean connectsTo(BlockState pState, boolean pSideSolid, Direction pDirection) {
      Block $$3 = pState.getBlock();
      boolean $$4 = $$3 instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(pState, pDirection);
      return pState.is(BlockTags.WALLS) || !isExceptionForConnection(pState) && pSideSolid || $$3 instanceof IronBarsBlock || $$4;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      LevelReader $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      FluidState $$3 = pContext.getLevel().getFluidState(pContext.getClickedPos());
      BlockPos $$4 = $$2.north();
      BlockPos $$5 = $$2.east();
      BlockPos $$6 = $$2.south();
      BlockPos $$7 = $$2.west();
      BlockPos $$8 = $$2.above();
      BlockState $$9 = $$1.getBlockState($$4);
      BlockState $$10 = $$1.getBlockState($$5);
      BlockState $$11 = $$1.getBlockState($$6);
      BlockState $$12 = $$1.getBlockState($$7);
      BlockState $$13 = $$1.getBlockState($$8);
      boolean $$14 = this.connectsTo($$9, $$9.isFaceSturdy($$1, $$4, Direction.SOUTH), Direction.SOUTH);
      boolean $$15 = this.connectsTo($$10, $$10.isFaceSturdy($$1, $$5, Direction.WEST), Direction.WEST);
      boolean $$16 = this.connectsTo($$11, $$11.isFaceSturdy($$1, $$6, Direction.NORTH), Direction.NORTH);
      boolean $$17 = this.connectsTo($$12, $$12.isFaceSturdy($$1, $$7, Direction.EAST), Direction.EAST);
      BlockState $$18 = this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf($$3.getType() == Fluids.WATER));
      return this.updateShape($$1, $$18, $$8, $$13, $$14, $$15, $$16, $$17);
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      if (pFacing == Direction.DOWN) {
         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      } else {
         return pFacing == Direction.UP
            ? this.topUpdate(pLevel, pState, pFacingPos, pFacingState)
            : this.sideUpdate(pLevel, pCurrentPos, pState, pFacingPos, pFacingState, pFacing);
      }
   }

   private static boolean isConnected(BlockState pState, Property<WallSide> pHeightProperty) {
      return pState.getValue(pHeightProperty) != WallSide.NONE;
   }

   private static boolean isCovered(VoxelShape pFirstShape, VoxelShape pSecondShape) {
      return !Shapes.joinIsNotEmpty(pSecondShape, pFirstShape, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(LevelReader pLevel, BlockState pState, BlockPos pPos, BlockState pSecondState) {
      boolean $$4 = isConnected(pState, NORTH_WALL);
      boolean $$5 = isConnected(pState, EAST_WALL);
      boolean $$6 = isConnected(pState, SOUTH_WALL);
      boolean $$7 = isConnected(pState, WEST_WALL);
      return this.updateShape(pLevel, pState, pPos, pSecondState, $$4, $$5, $$6, $$7);
   }

   private BlockState sideUpdate(LevelReader pLevel, BlockPos pFirstPos, BlockState pFirstState, BlockPos pSecondPos, BlockState pSecondState, Direction pDir) {
      Direction $$6 = pDir.getOpposite();
      boolean $$7 = pDir == Direction.NORTH
         ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, $$6), $$6)
         : isConnected(pFirstState, NORTH_WALL);
      boolean $$8 = pDir == Direction.EAST
         ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, $$6), $$6)
         : isConnected(pFirstState, EAST_WALL);
      boolean $$9 = pDir == Direction.SOUTH
         ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, $$6), $$6)
         : isConnected(pFirstState, SOUTH_WALL);
      boolean $$10 = pDir == Direction.WEST
         ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, $$6), $$6)
         : isConnected(pFirstState, WEST_WALL);
      BlockPos $$11 = pFirstPos.above();
      BlockState $$12 = pLevel.getBlockState($$11);
      return this.updateShape(pLevel, pFirstState, $$11, $$12, $$7, $$8, $$9, $$10);
   }

   private BlockState updateShape(
      LevelReader pLevel,
      BlockState pState,
      BlockPos pPos,
      BlockState pNeighbour,
      boolean pNorthConnection,
      boolean pEastConnection,
      boolean pSouthConnection,
      boolean pWestConnection
   ) {
      VoxelShape $$8 = pNeighbour.getCollisionShape(pLevel, pPos).getFaceShape(Direction.DOWN);
      BlockState $$9 = this.updateSides(pState, pNorthConnection, pEastConnection, pSouthConnection, pWestConnection, $$8);
      return $$9.setValue(UP, Boolean.valueOf(this.shouldRaisePost($$9, pNeighbour, $$8)));
   }

   private boolean shouldRaisePost(BlockState pState, BlockState pNeighbour, VoxelShape pShape) {
      boolean $$3 = pNeighbour.getBlock() instanceof WallBlock && pNeighbour.getValue(UP);
      if ($$3) {
         return true;
      } else {
         WallSide $$4 = pState.getValue(NORTH_WALL);
         WallSide $$5 = pState.getValue(SOUTH_WALL);
         WallSide $$6 = pState.getValue(EAST_WALL);
         WallSide $$7 = pState.getValue(WEST_WALL);
         boolean $$8 = $$5 == WallSide.NONE;
         boolean $$9 = $$7 == WallSide.NONE;
         boolean $$10 = $$6 == WallSide.NONE;
         boolean $$11 = $$4 == WallSide.NONE;
         boolean $$12 = $$11 && $$8 && $$9 && $$10 || $$11 != $$8 || $$9 != $$10;
         if ($$12) {
            return true;
         } else {
            boolean $$13 = $$4 == WallSide.TALL && $$5 == WallSide.TALL || $$6 == WallSide.TALL && $$7 == WallSide.TALL;
            return $$13 ? false : pNeighbour.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(pShape, POST_TEST);
         }
      }
   }

   private BlockState updateSides(
      BlockState pState, boolean pNorthConnection, boolean pEastConnection, boolean pSouthConnection, boolean pWestConnection, VoxelShape pWallShape
   ) {
      return pState.setValue(NORTH_WALL, this.makeWallState(pNorthConnection, pWallShape, NORTH_TEST))
         .setValue(EAST_WALL, this.makeWallState(pEastConnection, pWallShape, EAST_TEST))
         .setValue(SOUTH_WALL, this.makeWallState(pSouthConnection, pWallShape, SOUTH_TEST))
         .setValue(WEST_WALL, this.makeWallState(pWestConnection, pWallShape, WEST_TEST));
   }

   private WallSide makeWallState(boolean pAllowConnection, VoxelShape pShape, VoxelShape pNeighbourShape) {
      if (pAllowConnection) {
         return isCovered(pShape, pNeighbourShape) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return !pState.getValue(WATERLOGGED);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(UP, NORTH_WALL, EAST_WALL, WEST_WALL, SOUTH_WALL, WATERLOGGED);
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      switch (pRotation) {
         case CLOCKWISE_180:
            return pState.setValue(NORTH_WALL, pState.getValue(SOUTH_WALL))
               .setValue(EAST_WALL, pState.getValue(WEST_WALL))
               .setValue(SOUTH_WALL, pState.getValue(NORTH_WALL))
               .setValue(WEST_WALL, pState.getValue(EAST_WALL));
         case COUNTERCLOCKWISE_90:
            return pState.setValue(NORTH_WALL, pState.getValue(EAST_WALL))
               .setValue(EAST_WALL, pState.getValue(SOUTH_WALL))
               .setValue(SOUTH_WALL, pState.getValue(WEST_WALL))
               .setValue(WEST_WALL, pState.getValue(NORTH_WALL));
         case CLOCKWISE_90:
            return pState.setValue(NORTH_WALL, pState.getValue(WEST_WALL))
               .setValue(EAST_WALL, pState.getValue(NORTH_WALL))
               .setValue(SOUTH_WALL, pState.getValue(EAST_WALL))
               .setValue(WEST_WALL, pState.getValue(SOUTH_WALL));
         default:
            return pState;
      }
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      switch (pMirror) {
         case LEFT_RIGHT:
            return pState.setValue(NORTH_WALL, pState.getValue(SOUTH_WALL)).setValue(SOUTH_WALL, pState.getValue(NORTH_WALL));
         case FRONT_BACK:
            return pState.setValue(EAST_WALL, pState.getValue(WEST_WALL)).setValue(WEST_WALL, pState.getValue(EAST_WALL));
         default:
            return super.mirror(pState, pMirror);
      }
   }
}
