package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CopperBulbBlock extends Block {
   public static final MapCodec<CopperBulbBlock> CODEC = simpleCodec(CopperBulbBlock::new);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   @Override
   protected MapCodec<? extends CopperBulbBlock> codec() {
      return CODEC;
   }

   public CopperBulbBlock(BlockBehaviour.Properties p_311115_) {
      super(p_311115_);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)).setValue(POWERED, Boolean.valueOf(false)));
   }

   @Override
   protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
      if (pOldState.getBlock() != pState.getBlock() && pLevel instanceof ServerLevel $$5) {
         this.checkAndFlip(pState, $$5, pPos);
      }
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
      if (pLevel instanceof ServerLevel $$6) {
         this.checkAndFlip(pState, $$6, pPos);
      }
   }

   public void checkAndFlip(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
      boolean $$3 = pLevel.hasNeighborSignal(pPos);
      if ($$3 != pState.getValue(POWERED)) {
         BlockState $$4 = pState;
         if (!pState.getValue(POWERED)) {
            $$4 = pState.cycle(LIT);
            pLevel.playSound(null, pPos, $$4.getValue(LIT) ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS);
         }

         pLevel.setBlock(pPos, $$4.setValue(POWERED, Boolean.valueOf($$3)), 3);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LIT, POWERED);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return pLevel.getBlockState(pPos).getValue(LIT) ? 15 : 0;
   }
}
