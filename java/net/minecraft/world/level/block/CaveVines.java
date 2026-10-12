package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
   VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   BooleanProperty BERRIES = BlockStateProperties.BERRIES;

   static InteractionResult use(@Nullable Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos) {
      if (pState.getValue(BERRIES)) {
         Block.popResource(pLevel, pPos, new ItemStack(Items.GLOW_BERRIES, 1));
         float $$4 = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
         pLevel.playSound(null, pPos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, $$4);
         BlockState $$5 = pState.setValue(BERRIES, Boolean.valueOf(false));
         pLevel.setBlock(pPos, $$5, 2);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, $$5));
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(BlockState pState) {
      return pState.hasProperty(BERRIES) && pState.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(int pBerries) {
      return p_181216_ -> p_181216_.getValue(BlockStateProperties.BERRIES) ? pBerries : 0;
   }
}
