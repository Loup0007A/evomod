package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BellBlock extends BaseEntityBlock {
   public static final MapCodec<BellBlock> CODEC = simpleCodec(BellBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape NORTH_SOUTH_FLOOR_SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape EAST_WEST_FLOOR_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   private static final VoxelShape BELL_TOP_SHAPE = Block.box(5.0, 6.0, 5.0, 11.0, 13.0, 11.0);
   private static final VoxelShape BELL_BOTTOM_SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 6.0, 12.0);
   private static final VoxelShape BELL_SHAPE = Shapes.or(BELL_BOTTOM_SHAPE, BELL_TOP_SHAPE);
   private static final VoxelShape NORTH_SOUTH_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 16.0));
   private static final VoxelShape EAST_WEST_BETWEEN = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_WEST = Shapes.or(BELL_SHAPE, Block.box(0.0, 13.0, 7.0, 13.0, 15.0, 9.0));
   private static final VoxelShape TO_EAST = Shapes.or(BELL_SHAPE, Block.box(3.0, 13.0, 7.0, 16.0, 15.0, 9.0));
   private static final VoxelShape TO_NORTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 13.0));
   private static final VoxelShape TO_SOUTH = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 3.0, 9.0, 15.0, 16.0));
   private static final VoxelShape CEILING_SHAPE = Shapes.or(BELL_SHAPE, Block.box(7.0, 13.0, 7.0, 9.0, 16.0, 9.0));
   public static final int EVENT_BELL_RING = 1;

   @Override
   public MapCodec<BellBlock> codec() {
      return CODEC;
   }

   public BellBlock(BlockBehaviour.Properties p_49696_) {
      super(p_49696_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ATTACHMENT, BellAttachType.FLOOR).setValue(POWERED, Boolean.valueOf(false))
      );
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean $$6 = pLevel.hasNeighborSignal(pPos);
      if ($$6 != pState.getValue(POWERED)) {
         if ($$6) {
            this.attemptToRing(pLevel, pPos, null);
         }

         pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf($$6)), 3);
      }
   }

   @Override
   protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
      Entity $$4 = pProjectile.getOwner();
      Player $$5 = $$4 instanceof Player ? (Player)$$4 : null;
      this.onHit(pLevel, pState, pHit, $$5, true);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      return this.onHit(pLevel, pState, pHitResult, pPlayer, true) ? InteractionResult.sidedSuccess(pLevel.isClientSide) : InteractionResult.PASS;
   }

   public boolean onHit(Level pLevel, BlockState pState, BlockHitResult pResult, @Nullable Player pPlayer, boolean pCanRingBell) {
      Direction $$5 = pResult.getDirection();
      BlockPos $$6 = pResult.getBlockPos();
      boolean $$7 = !pCanRingBell || this.isProperHit(pState, $$5, pResult.getLocation().y - (double)$$6.getY());
      if ($$7) {
         boolean $$8 = this.attemptToRing(pPlayer, pLevel, $$6, $$5);
         if ($$8 && pPlayer != null) {
            pPlayer.awardStat(Stats.BELL_RING);
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isProperHit(BlockState pPos, Direction pDirection, double pDistanceY) {
      if (pDirection.getAxis() != Direction.Axis.Y && !(pDistanceY > 0.8124F)) {
         Direction $$3 = pPos.getValue(FACING);
         BellAttachType $$4 = pPos.getValue(ATTACHMENT);
         switch ($$4) {
            case FLOOR:
               return $$3.getAxis() == pDirection.getAxis();
            case SINGLE_WALL:
            case DOUBLE_WALL:
               return $$3.getAxis() != pDirection.getAxis();
            case CEILING:
               return true;
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public boolean attemptToRing(Level pLevel, BlockPos pPos, @Nullable Direction pDirection) {
      return this.attemptToRing(null, pLevel, pPos, pDirection);
   }

   public boolean attemptToRing(@Nullable Entity pEntity, Level pLevel, BlockPos pPos, @Nullable Direction pDirection) {
      BlockEntity $$4 = pLevel.getBlockEntity(pPos);
      if (!pLevel.isClientSide && $$4 instanceof BellBlockEntity) {
         if (pDirection == null) {
            pDirection = pLevel.getBlockState(pPos).getValue(FACING);
         }

         ((BellBlockEntity)$$4).onHit(pDirection);
         pLevel.playSound(null, pPos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
         pLevel.gameEvent(pEntity, GameEvent.BLOCK_CHANGE, pPos);
         return true;
      } else {
         return false;
      }
   }

   private VoxelShape getVoxelShape(BlockState pState) {
      Direction $$1 = pState.getValue(FACING);
      BellAttachType $$2 = pState.getValue(ATTACHMENT);
      if ($$2 == BellAttachType.FLOOR) {
         return $$1 != Direction.NORTH && $$1 != Direction.SOUTH ? EAST_WEST_FLOOR_SHAPE : NORTH_SOUTH_FLOOR_SHAPE;
      } else if ($$2 == BellAttachType.CEILING) {
         return CEILING_SHAPE;
      } else if ($$2 == BellAttachType.DOUBLE_WALL) {
         return $$1 != Direction.NORTH && $$1 != Direction.SOUTH ? EAST_WEST_BETWEEN : NORTH_SOUTH_BETWEEN;
      } else if ($$1 == Direction.NORTH) {
         return TO_NORTH;
      } else if ($$1 == Direction.SOUTH) {
         return TO_SOUTH;
      } else {
         return $$1 == Direction.EAST ? TO_EAST : TO_WEST;
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.getVoxelShape(pState);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.getVoxelShape(pState);
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction $$1 = pContext.getClickedFace();
      BlockPos $$2 = pContext.getClickedPos();
      Level $$3 = pContext.getLevel();
      Direction.Axis $$4 = $$1.getAxis();
      if ($$4 == Direction.Axis.Y) {
         BlockState $$5 = this.defaultBlockState()
            .setValue(ATTACHMENT, $$1 == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)
            .setValue(FACING, pContext.getHorizontalDirection());
         if ($$5.canSurvive(pContext.getLevel(), $$2)) {
            return $$5;
         }
      } else {
         boolean $$6 = $$4 == Direction.Axis.X
               && $$3.getBlockState($$2.west()).isFaceSturdy($$3, $$2.west(), Direction.EAST)
               && $$3.getBlockState($$2.east()).isFaceSturdy($$3, $$2.east(), Direction.WEST)
            || $$4 == Direction.Axis.Z
               && $$3.getBlockState($$2.north()).isFaceSturdy($$3, $$2.north(), Direction.SOUTH)
               && $$3.getBlockState($$2.south()).isFaceSturdy($$3, $$2.south(), Direction.NORTH);
         BlockState $$7 = this.defaultBlockState()
            .setValue(FACING, $$1.getOpposite())
            .setValue(ATTACHMENT, $$6 ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
         if ($$7.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
            return $$7;
         }

         boolean $$8 = $$3.getBlockState($$2.below()).isFaceSturdy($$3, $$2.below(), Direction.UP);
         $$7 = $$7.setValue(ATTACHMENT, $$8 ? BellAttachType.FLOOR : BellAttachType.CEILING);
         if ($$7.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
            return $$7;
         }
      }

      return null;
   }

   @Override
   protected void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
      if (pExplosion.canTriggerBlocks()) {
         this.attemptToRing(pLevel, pPos, null);
      }

      super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      BellAttachType $$6 = pState.getValue(ATTACHMENT);
      Direction $$7 = getConnectedDirection(pState).getOpposite();
      if ($$7 == pFacing && !pState.canSurvive(pLevel, pCurrentPos) && $$6 != BellAttachType.DOUBLE_WALL) {
         return Blocks.AIR.defaultBlockState();
      } else {
         if (pFacing.getAxis() == pState.getValue(FACING).getAxis()) {
            if ($$6 == BellAttachType.DOUBLE_WALL && !pFacingState.isFaceSturdy(pLevel, pFacingPos, pFacing)) {
               return pState.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL).setValue(FACING, pFacing.getOpposite());
            }

            if ($$6 == BellAttachType.SINGLE_WALL && $$7.getOpposite() == pFacing && pFacingState.isFaceSturdy(pLevel, pFacingPos, pState.getValue(FACING))) {
               return pState.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
            }
         }

         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      Direction $$3 = getConnectedDirection(pState).getOpposite();
      return $$3 == Direction.UP
         ? Block.canSupportCenter(pLevel, pPos.above(), Direction.DOWN)
         : FaceAttachedHorizontalDirectionalBlock.canAttach(pLevel, pPos, $$3);
   }

   private static Direction getConnectedDirection(BlockState pState) {
      switch ((BellAttachType)pState.getValue(ATTACHMENT)) {
         case FLOOR:
            return Direction.UP;
         case CEILING:
            return Direction.DOWN;
         default:
            return pState.getValue(FACING).getOpposite();
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, ATTACHMENT, POWERED);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new BellBlockEntity(pPos, pState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createTickerHelper(pBlockEntityType, BlockEntityType.BELL, pLevel.isClientSide ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   @Override
   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }
}
