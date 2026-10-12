package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RepeaterBlock extends DiodeBlock {
   public static final MapCodec<RepeaterBlock> CODEC = simpleCodec(RepeaterBlock::new);
   public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
   public static final IntegerProperty DELAY = BlockStateProperties.DELAY;

   @Override
   public MapCodec<RepeaterBlock> codec() {
      return CODEC;
   }

   public RepeaterBlock(BlockBehaviour.Properties p_55801_) {
      super(p_55801_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(DELAY, Integer.valueOf(1))
            .setValue(LOCKED, Boolean.valueOf(false))
            .setValue(POWERED, Boolean.valueOf(false))
      );
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (!pPlayer.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      } else {
         pLevel.setBlock(pPos, pState.cycle(DELAY), 3);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   @Override
   protected int getDelay(BlockState pState) {
      return pState.getValue(DELAY) * 2;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState $$1 = super.getStateForPlacement(pContext);
      return $$1.setValue(LOCKED, Boolean.valueOf(this.isLocked(pContext.getLevel(), pContext.getClickedPos(), $$1)));
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pFacing == Direction.DOWN && !this.canSurviveOn(pLevel, pFacingPos, pFacingState)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return !pLevel.isClientSide() && pFacing.getAxis() != pState.getValue(FACING).getAxis()
            ? pState.setValue(LOCKED, Boolean.valueOf(this.isLocked(pLevel, pCurrentPos, pState)))
            : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }

   @Override
   public boolean isLocked(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return this.getAlternateSignal(pLevel, pPos, pState) > 0;
   }

   @Override
   protected boolean sideInputDiodesOnly() {
      return true;
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(POWERED)) {
         Direction $$4 = pState.getValue(FACING);
         double $$5 = (double)pPos.getX() + 0.5 + (pRandom.nextDouble() - 0.5) * 0.2;
         double $$6 = (double)pPos.getY() + 0.4 + (pRandom.nextDouble() - 0.5) * 0.2;
         double $$7 = (double)pPos.getZ() + 0.5 + (pRandom.nextDouble() - 0.5) * 0.2;
         float $$8 = -5.0F;
         if (pRandom.nextBoolean()) {
            $$8 = (float)(pState.getValue(DELAY) * 2 - 1);
         }

         $$8 /= 16.0F;
         double $$9 = (double)($$8 * (float)$$4.getStepX());
         double $$10 = (double)($$8 * (float)$$4.getStepZ());
         pLevel.addParticle(DustParticleOptions.REDSTONE, $$5 + $$9, $$6, $$7 + $$10, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, DELAY, LOCKED, POWERED);
   }
}
