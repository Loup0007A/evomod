package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GameMasterBlockItem extends BlockItem {
   public GameMasterBlockItem(Block pBlock, Item.Properties pProperties) {
      super(pBlock, pProperties);
   }

   @Nullable
   @Override
   protected BlockState getPlacementState(BlockPlaceContext pContext) {
      Player $$1 = pContext.getPlayer();
      return $$1 != null && !$$1.canUseGameMasterBlocks() ? null : super.getPlacementState(pContext);
   }
}
