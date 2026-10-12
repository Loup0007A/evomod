package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigDripleafStemBlock extends HorizontalDirectionalBlock implements BonemealableBlock, SimpleWaterloggedBlock {
   public static final MapCodec<BigDripleafStemBlock> CODEC = simpleCodec(BigDripleafStemBlock::new);
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int STEM_WIDTH = 6;
   protected static final VoxelShape NORTH_SHAPE = Block.box(5.0, 0.0, 9.0, 11.0, 16.0, 15.0);
   protected static final VoxelShape SOUTH_SHAPE = Block.box(5.0, 0.0, 1.0, 11.0, 16.0, 7.0);
   protected static final VoxelShape EAST_SHAPE = Block.box(1.0, 0.0, 5.0, 7.0, 16.0, 11.0);
   protected static final VoxelShape WEST_SHAPE = Block.box(9.0, 0.0, 5.0, 15.0, 16.0, 11.0);

   @Override
   public MapCodec<BigDripleafStemBlock> codec() {
      return CODEC;
   }

   public BigDripleafStemBlock(BlockBehaviour.Properties p_152329_) {
      super(p_152329_);
      this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(FACING, Direction.NORTH));
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      switch ((Direction)pState.getValue(FACING)) {
         case SOUTH:
            return SOUTH_SHAPE;
         case NORTH:
         default:
            return NORTH_SHAPE;
         case WEST:
            return WEST_SHAPE;
         case EAST:
            return EAST_SHAPE;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(WATERLOGGED, FACING);
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockPos $$3 = pPos.below();
      BlockState $$4 = pLevel.getBlockState($$3);
      BlockState $$5 = pLevel.getBlockState(pPos.above());
      return ($$4.is(this) || $$4.is(BlockTags.BIG_DRIPLEAF_PLACEABLE)) && ($$5.is(this) || $$5.is(Blocks.BIG_DRIPLEAF));
   }

   protected static boolean place(LevelAccessor pLevel, BlockPos pPos, FluidState pFluidState, Direction pDirection) {
      BlockState $$4 = Blocks.BIG_DRIPLEAF_STEM
         .defaultBlockState()
         .setValue(WATERLOGGED, Boolean.valueOf(pFluidState.isSourceOfType(Fluids.WATER)))
         .setValue(FACING, pDirection);
      return pLevel.setBlock(pPos, $$4, 3);
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos
   ) {
      if ((pDirection == Direction.DOWN || pDirection == Direction.UP) && !pState.canSurvive(pLevel, pPos)) {
         pLevel.scheduleTick(pPos, this, 1);
      }

      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (!pState.canSurvive(pLevel, pPos)) {
         pLevel.destroyBlock(pPos, true);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      Optional<BlockPos> $$3 = BlockUtil.getTopConnectedBlock(pLevel, pPos, pState.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if ($$3.isEmpty()) {
         return false;
      } else {
         BlockPos $$4 = $$3.get().above();
         BlockState $$5 = pLevel.getBlockState($$4);
         return BigDripleafBlock.canPlaceAt(pLevel, $$4, $$5);
      }
   }

   @Override
   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      Optional<BlockPos> $$4 = BlockUtil.getTopConnectedBlock(pLevel, pPos, pState.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if (!$$4.isEmpty()) {
         BlockPos $$5 = $$4.get();
         BlockPos $$6 = $$5.above();
         Direction $$7 = pState.getValue(FACING);
         place(pLevel, $$5, pLevel.getFluidState($$5), $$7);
         BigDripleafBlock.place(pLevel, $$6, pLevel.getFluidState($$6), $$7);
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return new ItemStack(Blocks.BIG_DRIPLEAF);
   }
}
