package net.minecraft.world.level.block;

import com.google.common.collect.UnmodifiableIterator;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class CrossCollisionBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty NORTH = PipeBlock.NORTH;
   public static final BooleanProperty EAST = PipeBlock.EAST;
   public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
   public static final BooleanProperty WEST = PipeBlock.WEST;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
      .entrySet()
      .stream()
      .filter(p_52346_ -> p_52346_.getKey().getAxis().isHorizontal())
      .collect(Util.toMap());
   protected final VoxelShape[] collisionShapeByIndex;
   protected final VoxelShape[] shapeByIndex;
   private final Object2IntMap<BlockState> stateToIndex = new Object2IntOpenHashMap();

   protected CrossCollisionBlock(
      float pNodeWidth, float pExtensionWidth, float pNodeHeight, float pExtensionHeight, float pCollisionHeight, BlockBehaviour.Properties pProperties
   ) {
      super(pProperties);
      this.collisionShapeByIndex = this.makeShapes(pNodeWidth, pExtensionWidth, pCollisionHeight, 0.0F, pCollisionHeight);
      this.shapeByIndex = this.makeShapes(pNodeWidth, pExtensionWidth, pNodeHeight, 0.0F, pExtensionHeight);
      UnmodifiableIterator var7 = this.stateDefinition.getPossibleStates().iterator();

      while (var7.hasNext()) {
         BlockState $$6 = (BlockState)var7.next();
         this.getAABBIndex($$6);
      }
   }

   @Override
   protected abstract MapCodec<? extends CrossCollisionBlock> codec();

   protected VoxelShape[] makeShapes(float pNodeWidth, float pExtensionWidth, float pNodeHeight, float pExtensionBottom, float pExtensionHeight) {
      float $$5 = 8.0F - pNodeWidth;
      float $$6 = 8.0F + pNodeWidth;
      float $$7 = 8.0F - pExtensionWidth;
      float $$8 = 8.0F + pExtensionWidth;
      VoxelShape $$9 = Block.box((double)$$5, 0.0, (double)$$5, (double)$$6, (double)pNodeHeight, (double)$$6);
      VoxelShape $$10 = Block.box((double)$$7, (double)pExtensionBottom, 0.0, (double)$$8, (double)pExtensionHeight, (double)$$8);
      VoxelShape $$11 = Block.box((double)$$7, (double)pExtensionBottom, (double)$$7, (double)$$8, (double)pExtensionHeight, 16.0);
      VoxelShape $$12 = Block.box(0.0, (double)pExtensionBottom, (double)$$7, (double)$$8, (double)pExtensionHeight, (double)$$8);
      VoxelShape $$13 = Block.box((double)$$7, (double)pExtensionBottom, (double)$$7, 16.0, (double)pExtensionHeight, (double)$$8);
      VoxelShape $$14 = Shapes.or($$10, $$13);
      VoxelShape $$15 = Shapes.or($$11, $$12);
      VoxelShape[] $$16 = new VoxelShape[]{
         Shapes.empty(),
         $$11,
         $$12,
         $$15,
         $$10,
         Shapes.or($$11, $$10),
         Shapes.or($$12, $$10),
         Shapes.or($$15, $$10),
         $$13,
         Shapes.or($$11, $$13),
         Shapes.or($$12, $$13),
         Shapes.or($$15, $$13),
         $$14,
         Shapes.or($$11, $$14),
         Shapes.or($$12, $$14),
         Shapes.or($$15, $$14)
      };

      for (int $$17 = 0; $$17 < 16; $$17++) {
         $$16[$$17] = Shapes.or($$9, $$16[$$17]);
      }

      return $$16;
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return !pState.getValue(WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.shapeByIndex[this.getAABBIndex(pState)];
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.collisionShapeByIndex[this.getAABBIndex(pState)];
   }

   private static int indexFor(Direction pFacing) {
      return 1 << pFacing.get2DDataValue();
   }

   protected int getAABBIndex(BlockState pState) {
      return this.stateToIndex.computeIntIfAbsent(pState, p_52366_ -> {
         int $$1 = 0;
         if (p_52366_.getValue(NORTH)) {
            $$1 |= indexFor(Direction.NORTH);
         }

         if (p_52366_.getValue(EAST)) {
            $$1 |= indexFor(Direction.EAST);
         }

         if (p_52366_.getValue(SOUTH)) {
            $$1 |= indexFor(Direction.SOUTH);
         }

         if (p_52366_.getValue(WEST)) {
            $$1 |= indexFor(Direction.WEST);
         }

         return $$1;
      });
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRot) {
      switch (pRot) {
         case CLOCKWISE_180:
            return pState.setValue(NORTH, pState.getValue(SOUTH))
               .setValue(EAST, pState.getValue(WEST))
               .setValue(SOUTH, pState.getValue(NORTH))
               .setValue(WEST, pState.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return pState.setValue(NORTH, pState.getValue(EAST))
               .setValue(EAST, pState.getValue(SOUTH))
               .setValue(SOUTH, pState.getValue(WEST))
               .setValue(WEST, pState.getValue(NORTH));
         case CLOCKWISE_90:
            return pState.setValue(NORTH, pState.getValue(WEST))
               .setValue(EAST, pState.getValue(NORTH))
               .setValue(SOUTH, pState.getValue(EAST))
               .setValue(WEST, pState.getValue(SOUTH));
         default:
            return pState;
      }
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      switch (pMirror) {
         case LEFT_RIGHT:
            return pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
         case FRONT_BACK:
            return pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
         default:
            return super.mirror(pState, pMirror);
      }
   }
}
