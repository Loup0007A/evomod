package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class WrittenBookItem extends Item {
   public WrittenBookItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public Component getName(ItemStack pStack) {
      WrittenBookContent $$1 = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$1 != null) {
         String $$2 = $$1.title().raw();
         if (!StringUtil.isBlank($$2)) {
            return Component.literal($$2);
         }
      }

      return super.getName(pStack);
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      WrittenBookContent $$4 = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$4 != null) {
         if (!StringUtil.isBlank($$4.author())) {
            pTooltipComponents.add(Component.translatable("book.byAuthor", $$4.author()).withStyle(ChatFormatting.GRAY));
         }

         pTooltipComponents.add(Component.translatable("book.generation." + $$4.generation()).withStyle(ChatFormatting.GRAY));
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      pPlayer.openItemGui($$3, pHand);
      pPlayer.awardStat(Stats.ITEM_USED.get(this));
      return InteractionResultHolder.sidedSuccess($$3, pLevel.isClientSide());
   }

   public static boolean resolveBookComponents(ItemStack pBookStack, CommandSourceStack pResolvingSource, @Nullable Player pResolvingPlayer) {
      WrittenBookContent $$3 = pBookStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if ($$3 != null && !$$3.resolved()) {
         WrittenBookContent $$4 = $$3.resolve(pResolvingSource, pResolvingPlayer);
         if ($$4 != null) {
            pBookStack.set(DataComponents.WRITTEN_BOOK_CONTENT, $$4);
            return true;
         }

         pBookStack.set(DataComponents.WRITTEN_BOOK_CONTENT, $$3.markResolved());
      }

      return false;
   }
}
