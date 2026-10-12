package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DoorBlock extends Block {
   public static final MapCodec<DoorBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_310521_ -> p_310521_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type), propertiesCodec()).apply(p_310521_, DoorBlock::new)
   );
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   protected static final float AABB_DOOR_THICKNESS = 3.0F;
   protected static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
   protected static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape WEST_AABB = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
   private final BlockSetType type;

   @Override
   public MapCodec<? extends DoorBlock> codec() {
      return CODEC;
   }

   public DoorBlock(BlockSetType p_272854_, BlockBehaviour.Properties p_273303_) {
      super(p_273303_.sound(p_272854_.soundType()));
      this.type = p_272854_;
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, Boolean.valueOf(false))
            .setValue(HINGE, DoorHingeSide.LEFT)
            .setValue(POWERED, Boolean.valueOf(false))
            .setValue(HALF, DoubleBlockHalf.LOWER)
      );
   }

   public BlockSetType type() {
      return this.type;
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction $$4 = pState.getValue(FACING);
      boolean $$5 = !pState.getValue(OPEN);
      boolean $$6 = pState.getValue(HINGE) == DoorHingeSide.RIGHT;

      return switch ($$4) {
         case SOUTH -> $$5 ? SOUTH_AABB : ($$6 ? EAST_AABB : WEST_AABB);
         case WEST -> $$5 ? WEST_AABB : ($$6 ? SOUTH_AABB : NORTH_AABB);
         case NORTH -> $$5 ? NORTH_AABB : ($$6 ? WEST_AABB : EAST_AABB);
         default -> $$5 ? EAST_AABB : ($$6 ? NORTH_AABB : SOUTH_AABB);
      };
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      DoubleBlockHalf $$6 = pState.getValue(HALF);
      if (pFacing.getAxis() != Direction.Axis.Y || $$6 == DoubleBlockHalf.LOWER != (pFacing == Direction.UP)) {
         return $$6 == DoubleBlockHalf.LOWER && pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      } else {
         return pFacingState.getBlock() instanceof DoorBlock && pFacingState.getValue(HALF) != $$6
            ? pFacingState.setValue(HALF, $$6)
            : Blocks.AIR.defaultBlockState();
      }
   }

   @Override
   protected void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
      if (pExplosion.canTriggerBlocks() && pState.getValue(HALF) == DoubleBlockHalf.LOWER && this.type.canOpenByWindCharge() && !pState.getValue(POWERED)) {
         this.setOpen(null, pLevel, pState, pPos, !this.isOpen(pState));
      }

      super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
   }

   @Override
   public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
      if (!pLevel.isClientSide && (pPlayer.isCreative() || !pPlayer.hasCorrectToolForDrops(pState))) {
         DoublePlantBlock.preventDropFromBottomPart(pLevel, pPos, pState, pPlayer);
      }

      return super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return switch (pPathComputationType) {
         case LAND, AIR -> pState.getValue(OPEN);
         case WATER -> false;
      };
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockPos $$1 = pContext.getClickedPos();
      Level $$2 = pContext.getLevel();
      if ($$1.getY() < $$2.getMaxBuildHeight() - 1 && $$2.getBlockState($$1.above()).canBeReplaced(pContext)) {
         boolean $$3 = $$2.hasNeighborSignal($$1) || $$2.hasNeighborSignal($$1.above());
         return this.defaultBlockState()
            .setValue(FACING, pContext.getHorizontalDirection())
            .setValue(HINGE, this.getHinge(pContext))
            .setValue(POWERED, Boolean.valueOf($$3))
            .setValue(OPEN, Boolean.valueOf($$3))
            .setValue(HALF, DoubleBlockHalf.LOWER);
      } else {
         return null;
      }
   }

   @Override
   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      pLevel.setBlock(pPos.above(), pState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   private DoorHingeSide getHinge(BlockPlaceContext pContext) {
      BlockGetter $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      Direction $$3 = pContext.getHorizontalDirection();
      BlockPos $$4 = $$2.above();
      Direction $$5 = $$3.getCounterClockWise();
      BlockPos $$6 = $$2.relative($$5);
      BlockState $$7 = $$1.getBlockState($$6);
      BlockPos $$8 = $$4.relative($$5);
      BlockState $$9 = $$1.getBlockState($$8);
      Direction $$10 = $$3.getClockWise();
      BlockPos $$11 = $$2.relative($$10);
      BlockState $$12 = $$1.getBlockState($$11);
      BlockPos $$13 = $$4.relative($$10);
      BlockState $$14 = $$1.getBlockState($$13);
      int $$15 = ($$7.isCollisionShapeFullBlock($$1, $$6) ? -1 : 0)
         + ($$9.isCollisionShapeFullBlock($$1, $$8) ? -1 : 0)
         + ($$12.isCollisionShapeFullBlock($$1, $$11) ? 1 : 0)
         + ($$14.isCollisionShapeFullBlock($$1, $$13) ? 1 : 0);
      boolean $$16 = $$7.getBlock() instanceof DoorBlock && $$7.getValue(HALF) == DoubleBlockHalf.LOWER;
      boolean $$17 = $$12.getBlock() instanceof DoorBlock && $$12.getValue(HALF) == DoubleBlockHalf.LOWER;
      if ((!$$16 || $$17) && $$15 <= 0) {
         if ((!$$17 || $$16) && $$15 >= 0) {
            int $$18 = $$3.getStepX();
            int $$19 = $$3.getStepZ();
            Vec3 $$20 = pContext.getClickLocation();
            double $$21 = $$20.x - (double)$$2.getX();
            double $$22 = $$20.z - (double)$$2.getZ();
            return ($$18 >= 0 || !($$22 < 0.5)) && ($$18 <= 0 || !($$22 > 0.5)) && ($$19 >= 0 || !($$21 > 0.5)) && ($$19 <= 0 || !($$21 < 0.5))
               ? DoorHingeSide.LEFT
               : DoorHingeSide.RIGHT;
         } else {
            return DoorHingeSide.LEFT;
         }
      } else {
         return DoorHingeSide.RIGHT;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (!this.type.canOpenByHand()) {
         return InteractionResult.PASS;
      } else {
         pState = pState.cycle(OPEN);
         pLevel.setBlock(pPos, pState, 10);
         this.playSound(pPlayer, pLevel, pPos, pState.getValue(OPEN));
         pLevel.gameEvent(pPlayer, this.isOpen(pState) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   public boolean isOpen(BlockState pState) {
      return pState.getValue(OPEN);
   }

   public void setOpen(@Nullable Entity pEntity, Level pLevel, BlockState pState, BlockPos pPos, boolean pOpen) {
      if (pState.is(this) && pState.getValue(OPEN) != pOpen) {
         pLevel.setBlock(pPos, pState.setValue(OPEN, Boolean.valueOf(pOpen)), 10);
         this.playSound(pEntity, pLevel, pPos, pOpen);
         pLevel.gameEvent(pEntity, pOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
      }
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean $$6 = pLevel.hasNeighborSignal(pPos)
         || pLevel.hasNeighborSignal(pPos.relative(pState.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
      if (!this.defaultBlockState().is(pBlock) && $$6 != pState.getValue(POWERED)) {
         if ($$6 != pState.getValue(OPEN)) {
            this.playSound(null, pLevel, pPos, $$6);
            pLevel.gameEvent(null, $$6 ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pPos);
         }

         pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf($$6)).setValue(OPEN, Boolean.valueOf($$6)), 2);
      }
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos $$3 = pPos.below();
      BlockState $$4 = pLevel.getBlockState($$3);
      return pState.getValue(HALF) == DoubleBlockHalf.LOWER ? $$4.isFaceSturdy(pLevel, $$3, Direction.UP) : $$4.is(this);
   }

   private void playSound(@Nullable Entity pSource, Level pLevel, BlockPos pPos, boolean pIsOpening) {
      pLevel.playSound(
         pSource, pPos, pIsOpening ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, pLevel.getRandom().nextFloat() * 0.1F + 0.9F
      );
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      return pMirror == Mirror.NONE ? pState : pState.rotate(pMirror.getRotation(pState.getValue(FACING))).cycle(HINGE);
   }

   @Override
   protected long getSeed(BlockState pState, BlockPos pPos) {
      return Mth.getSeed(pPos.getX(), pPos.below(pState.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pPos.getZ());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(HALF, FACING, OPEN, HINGE, POWERED);
   }

   public static boolean isWoodenDoor(Level pLevel, BlockPos pPos) {
      return isWoodenDoor(pLevel.getBlockState(pPos));
   }

   public static boolean isWoodenDoor(BlockState pState) {
      if (pState.getBlock() instanceof DoorBlock $$1 && $$1.type().canOpenByHand()) {
         return true;
      }

      return false;
   }
}
