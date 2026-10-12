package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DoubleHighBlockItem extends BlockItem {
   public DoubleHighBlockItem(Block pBlock, Item.Properties pProperties) {
      super(pBlock, pProperties);
   }

   @Override
   protected boolean placeBlock(BlockPlaceContext pContext, BlockState pState) {
      Level $$2 = pContext.getLevel();
      BlockPos $$3 = pContext.getClickedPos().above();
      BlockState $$4 = $$2.isWaterAt($$3) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
      $$2.setBlock($$3, $$4, 27);
      return super.placeBlock(pContext, pState);
   }
}
