package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {
   public SignItem(Item.Properties pProperties, Block pStandingBlock, Block pWallBlock) {
      super(pStandingBlock, pWallBlock, pProperties, Direction.DOWN);
   }

   public SignItem(Item.Properties pProperties, Block pStandingBlock, Block pWallBlock, Direction pAttachmentDirection) {
      super(pStandingBlock, pWallBlock, pProperties, pAttachmentDirection);
   }

   @Override
   protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, @Nullable Player pPlayer, ItemStack pStack, BlockState pState) {
      boolean $$5 = super.updateCustomBlockEntityTag(pPos, pLevel, pPlayer, pStack, pState);
      if (!pLevel.isClientSide
         && !$$5
         && pPlayer != null
         && pLevel.getBlockEntity(pPos) instanceof SignBlockEntity $$6
         && pLevel.getBlockState(pPos).getBlock() instanceof SignBlock $$7) {
         $$7.openTextEdit(pPlayer, $$6, true);
      }

      return $$5;
   }
}
