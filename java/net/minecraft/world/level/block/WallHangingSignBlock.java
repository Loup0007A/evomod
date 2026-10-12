package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallHangingSignBlock extends SignBlock {
   public static final MapCodec<WallHangingSignBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_312769_ -> p_312769_.group(WoodType.CODEC.fieldOf("wood_type").forGetter(SignBlock::type), propertiesCodec())
            .apply(p_312769_, WallHangingSignBlock::new)
   );
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final VoxelShape PLANK_NORTHSOUTH = Block.box(0.0, 14.0, 6.0, 16.0, 16.0, 10.0);
   public static final VoxelShape PLANK_EASTWEST = Block.box(6.0, 14.0, 0.0, 10.0, 16.0, 16.0);
   public static final VoxelShape SHAPE_NORTHSOUTH = Shapes.or(PLANK_NORTHSOUTH, Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0));
   public static final VoxelShape SHAPE_EASTWEST = Shapes.or(PLANK_EASTWEST, Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0));
   private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(
      ImmutableMap.of(Direction.NORTH, SHAPE_NORTHSOUTH, Direction.SOUTH, SHAPE_NORTHSOUTH, Direction.EAST, SHAPE_EASTWEST, Direction.WEST, SHAPE_EASTWEST)
   );

   @Override
   public MapCodec<WallHangingSignBlock> codec() {
      return CODEC;
   }

   public WallHangingSignBlock(WoodType p_252140_, BlockBehaviour.Properties p_251606_) {
      super(p_252140_, p_251606_.sound(p_252140_.hangingSignSoundType()));
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
   ) {
      if (pLevel.getBlockEntity(pPos) instanceof SignBlockEntity $$7 && this.shouldTryToChainAnotherHangingSign(pState, pPlayer, pHitResult, $$7, pStack)) {
         return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }

      return super.useItemOn(pStack, pState, pLevel, pPos, pPlayer, pHand, pHitResult);
   }

   private boolean shouldTryToChainAnotherHangingSign(BlockState pState, Player pPlayer, BlockHitResult pHitResult, SignBlockEntity pSign, ItemStack pStack) {
      return !pSign.canExecuteClickCommands(pSign.isFacingFrontText(pPlayer), pPlayer)
         && pStack.getItem() instanceof HangingSignItem
         && !this.isHittingEditableSide(pHitResult, pState);
   }

   private boolean isHittingEditableSide(BlockHitResult pHitResult, BlockState pState) {
      return pHitResult.getDirection().getAxis() == pState.getValue(FACING).getAxis();
   }

   @Override
   public String getDescriptionId() {
      return this.asItem().getDescriptionId();
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AABBS.get(pState.getValue(FACING));
   }

   @Override
   protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return this.getShape(pState, pLevel, pPos, CollisionContext.empty());
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      switch ((Direction)pState.getValue(FACING)) {
         case EAST:
         case WEST:
            return PLANK_EASTWEST;
         default:
            return PLANK_NORTHSOUTH;
      }
   }

   public boolean canPlace(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      Direction $$3 = pState.getValue(FACING).getClockWise();
      Direction $$4 = pState.getValue(FACING).getCounterClockWise();
      return this.canAttachTo(pLevel, pState, pPos.relative($$3), $$4) || this.canAttachTo(pLevel, pState, pPos.relative($$4), $$3);
   }

   public boolean canAttachTo(LevelReader pLevel, BlockState pState, BlockPos pPos, Direction pDirection) {
      BlockState $$4 = pLevel.getBlockState(pPos);
      return $$4.is(BlockTags.WALL_HANGING_SIGNS)
         ? $$4.getValue(FACING).getAxis().test(pState.getValue(FACING))
         : $$4.isFaceSturdy(pLevel, pPos, pDirection, SupportType.FULL);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState $$1 = this.defaultBlockState();
      FluidState $$2 = pContext.getLevel().getFluidState(pContext.getClickedPos());
      LevelReader $$3 = pContext.getLevel();
      BlockPos $$4 = pContext.getClickedPos();

      for (Direction $$5 : pContext.getNearestLookingDirections()) {
         if ($$5.getAxis().isHorizontal() && !$$5.getAxis().test(pContext.getClickedFace())) {
            Direction $$6 = $$5.getOpposite();
            $$1 = $$1.setValue(FACING, $$6);
            if ($$1.canSurvive($$3, $$4) && this.canPlace($$1, $$3, $$4)) {
               return $$1.setValue(WATERLOGGED, Boolean.valueOf($$2.getType() == Fluids.WATER));
            }
         }
      }

      return null;
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      return pFacing.getAxis() == pState.getValue(FACING).getClockWise().getAxis() && !pState.canSurvive(pLevel, pCurrentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   public float getYRotationDegrees(BlockState pState) {
      return pState.getValue(FACING).toYRot();
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
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
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new HangingSignBlockEntity(pPos, pState);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createTickerHelper(pBlockEntityType, BlockEntityType.HANGING_SIGN, SignBlockEntity::tick);
   }
}
