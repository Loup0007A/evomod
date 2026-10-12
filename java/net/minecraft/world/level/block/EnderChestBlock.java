package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnderChestBlock extends AbstractChestBlock<EnderChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final MapCodec<EnderChestBlock> CODEC = simpleCodec(EnderChestBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private static final Component CONTAINER_TITLE = Component.translatable("container.enderchest");

   @Override
   public MapCodec<EnderChestBlock> codec() {
      return CODEC;
   }

   public EnderChestBlock(BlockBehaviour.Properties p_53121_) {
      super(p_53121_, () -> BlockEntityType.ENDER_CHEST);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState pState, Level pLevel, BlockPos pPos, boolean pOverride) {
      return DoubleBlockCombiner.Combiner::acceptNone;
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      FluidState $$1 = pContext.getLevel().getFluidState(pContext.getClickedPos());
      return this.defaultBlockState()
         .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
         .setValue(WATERLOGGED, Boolean.valueOf($$1.getType() == Fluids.WATER));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      PlayerEnderChestContainer $$5 = pPlayer.getEnderChestInventory();
      BlockEntity $$6 = pLevel.getBlockEntity(pPos);
      if ($$5 != null && $$6 instanceof EnderChestBlockEntity) {
         BlockPos $$7 = pPos.above();
         if (pLevel.getBlockState($$7).isRedstoneConductor(pLevel, $$7)) {
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
         } else if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            EnderChestBlockEntity $$8 = (EnderChestBlockEntity)$$6;
            $$5.setActiveChest($$8);
            pPlayer.openMenu(new SimpleMenuProvider((p_53124_, p_53125_, p_53126_) -> ChestMenu.threeRows(p_53124_, p_53125_, $$5), CONTAINER_TITLE));
            pPlayer.awardStat(Stats.OPEN_ENDERCHEST);
            PiglinAi.angerNearbyPiglins(pPlayer, true);
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new EnderChestBlockEntity(pPos, pState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return pLevel.isClientSide ? createTickerHelper(pBlockEntityType, BlockEntityType.ENDER_CHEST, EnderChestBlockEntity::lidAnimateTick) : null;
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      for (int $$4 = 0; $$4 < 3; $$4++) {
         int $$5 = pRandom.nextInt(2) * 2 - 1;
         int $$6 = pRandom.nextInt(2) * 2 - 1;
         double $$7 = (double)pPos.getX() + 0.5 + 0.25 * (double)$$5;
         double $$8 = (double)((float)pPos.getY() + pRandom.nextFloat());
         double $$9 = (double)pPos.getZ() + 0.5 + 0.25 * (double)$$6;
         double $$10 = (double)(pRandom.nextFloat() * (float)$$5);
         double $$11 = ((double)pRandom.nextFloat() - 0.5) * 0.125;
         double $$12 = (double)(pRandom.nextFloat() * (float)$$6);
         pLevel.addParticle(ParticleTypes.PORTAL, $$7, $$8, $$9, $$10, $$11, $$12);
      }
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRot) {
      return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, WATERLOGGED);
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockEntity $$4 = pLevel.getBlockEntity(pPos);
      if ($$4 instanceof EnderChestBlockEntity) {
         ((EnderChestBlockEntity)$$4).recheckOpen();
      }
   }
}
