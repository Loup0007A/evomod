package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScaffoldingBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);
   private static final int TICK_DELAY = 1;
   private static final VoxelShape STABLE_SHAPE;
   private static final VoxelShape UNSTABLE_SHAPE;
   private static final VoxelShape UNSTABLE_SHAPE_BOTTOM = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
   private static final VoxelShape BELOW_BLOCK = Shapes.block().move(0.0, -1.0, 0.0);
   public static final int STABILITY_MAX_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.STABILITY_DISTANCE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

   @Override
   public MapCodec<ScaffoldingBlock> codec() {
      return CODEC;
   }

   public ScaffoldingBlock(BlockBehaviour.Properties p_56021_) {
      super(p_56021_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(DISTANCE, Integer.valueOf(7))
            .setValue(WATERLOGGED, Boolean.valueOf(false))
            .setValue(BOTTOM, Boolean.valueOf(false))
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(DISTANCE, WATERLOGGED, BOTTOM);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (!pContext.isHoldingItem(pState.getBlock().asItem())) {
         return pState.getValue(BOTTOM) ? UNSTABLE_SHAPE : STABLE_SHAPE;
      } else {
         return Shapes.block();
      }
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Shapes.block();
   }

   @Override
   protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return pUseContext.getItemInHand().is(this.asItem());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockPos $$1 = pContext.getClickedPos();
      Level $$2 = pContext.getLevel();
      int $$3 = getDistance($$2, $$1);
      return this.defaultBlockState()
         .setValue(WATERLOGGED, Boolean.valueOf($$2.getFluidState($$1).getType() == Fluids.WATER))
         .setValue(DISTANCE, Integer.valueOf($$3))
         .setValue(BOTTOM, Boolean.valueOf(this.isBottom($$2, $$1, $$3)));
   }

   @Override
   protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         pLevel.scheduleTick(pPos, this, 1);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      if (!pLevel.isClientSide()) {
         pLevel.scheduleTick(pCurrentPos, this, 1);
      }

      return pState;
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      int $$4 = getDistance(pLevel, pPos);
      BlockState $$5 = pState.setValue(DISTANCE, Integer.valueOf($$4)).setValue(BOTTOM, Boolean.valueOf(this.isBottom(pLevel, pPos, $$4)));
      if ($$5.getValue(DISTANCE) == 7) {
         if (pState.getValue(DISTANCE) == 7) {
            FallingBlockEntity.fall(pLevel, pPos, $$5);
         } else {
            pLevel.destroyBlock(pPos, true);
         }
      } else if (pState != $$5) {
         pLevel.setBlock(pPos, $$5, 3);
      }
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return getDistance(pLevel, pPos) < 7;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (pContext.isAbove(Shapes.block(), pPos, true) && !pContext.isDescending()) {
         return STABLE_SHAPE;
      } else {
         return pState.getValue(DISTANCE) != 0 && pState.getValue(BOTTOM) && pContext.isAbove(BELOW_BLOCK, pPos, true) ? UNSTABLE_SHAPE_BOTTOM : Shapes.empty();
      }
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   private boolean isBottom(BlockGetter pLevel, BlockPos pPos, int pDistance) {
      return pDistance > 0 && !pLevel.getBlockState(pPos.below()).is(this);
   }

   public static int getDistance(BlockGetter pLevel, BlockPos pPos) {
      BlockPos.MutableBlockPos $$2 = pPos.mutable().move(Direction.DOWN);
      BlockState $$3 = pLevel.getBlockState($$2);
      int $$4 = 7;
      if ($$3.is(Blocks.SCAFFOLDING)) {
         $$4 = $$3.getValue(DISTANCE);
      } else if ($$3.isFaceSturdy(pLevel, $$2, Direction.UP)) {
         return 0;
      }

      for (Direction $$5 : Direction.Plane.HORIZONTAL) {
         BlockState $$6 = pLevel.getBlockState($$2.setWithOffset(pPos, $$5));
         if ($$6.is(Blocks.SCAFFOLDING)) {
            $$4 = Math.min($$4, $$6.getValue(DISTANCE) + 1);
            if ($$4 == 1) {
               break;
            }
         }
      }

      return $$4;
   }

   static {
      VoxelShape $$0 = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
      VoxelShape $$1 = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 2.0);
      VoxelShape $$2 = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 2.0);
      VoxelShape $$3 = Block.box(0.0, 0.0, 14.0, 2.0, 16.0, 16.0);
      VoxelShape $$4 = Block.box(14.0, 0.0, 14.0, 16.0, 16.0, 16.0);
      STABLE_SHAPE = Shapes.or($$0, $$1, $$2, $$3, $$4);
      VoxelShape $$5 = Block.box(0.0, 0.0, 0.0, 2.0, 2.0, 16.0);
      VoxelShape $$6 = Block.box(14.0, 0.0, 0.0, 16.0, 2.0, 16.0);
      VoxelShape $$7 = Block.box(0.0, 0.0, 14.0, 16.0, 2.0, 16.0);
      VoxelShape $$8 = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 2.0);
      UNSTABLE_SHAPE = Shapes.or(ScaffoldingBlock.UNSTABLE_SHAPE_BOTTOM, STABLE_SHAPE, $$6, $$5, $$8, $$7);
   }
}
