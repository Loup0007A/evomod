package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ScaffoldingBlockItem extends BlockItem {
   public ScaffoldingBlockItem(Block pBlock, Item.Properties pProperties) {
      super(pBlock, pProperties);
   }

   @Nullable
   @Override
   public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
      BlockPos $$1 = pContext.getClickedPos();
      Level $$2 = pContext.getLevel();
      BlockState $$3 = $$2.getBlockState($$1);
      Block $$4 = this.getBlock();
      if (!$$3.is($$4)) {
         return ScaffoldingBlock.getDistance($$2, $$1) == 7 ? null : pContext;
      } else {
         Direction $$5;
         if (pContext.isSecondaryUseActive()) {
            $$5 = pContext.isInside() ? pContext.getClickedFace().getOpposite() : pContext.getClickedFace();
         } else {
            $$5 = pContext.getClickedFace() == Direction.UP ? pContext.getHorizontalDirection() : Direction.UP;
         }

         int $$7 = 0;
         BlockPos.MutableBlockPos $$8 = $$1.mutable().move($$5);

         while ($$7 < 7) {
            if (!$$2.isClientSide && !$$2.isInWorldBounds($$8)) {
               Player $$9 = pContext.getPlayer();
               int $$10 = $$2.getMaxBuildHeight();
               if ($$9 instanceof ServerPlayer && $$8.getY() >= $$10) {
                  ((ServerPlayer)$$9).sendSystemMessage(Component.translatable("build.tooHigh", $$10 - 1).withStyle(ChatFormatting.RED), true);
               }
               break;
            }

            $$3 = $$2.getBlockState($$8);
            if (!$$3.is(this.getBlock())) {
               if ($$3.canBeReplaced(pContext)) {
                  return BlockPlaceContext.at(pContext, $$8, $$5);
               }
               break;
            }

            $$8.move($$5);
            if ($$5.getAxis().isHorizontal()) {
               $$7++;
            }
         }

         return null;
      }
   }

   @Override
   protected boolean mustSurvive() {
      return false;
   }
}
