package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public record DyedItemColor(int rgb, boolean showInTooltip) implements TooltipProvider {
   private static final Codec<DyedItemColor> FULL_CODEC = RecordCodecBuilder.create(
      p_332588_ -> p_332588_.group(
               Codec.INT.fieldOf("rgb").forGetter(DyedItemColor::rgb),
               Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(DyedItemColor::showInTooltip)
            )
            .apply(p_332588_, DyedItemColor::new)
   );
   public static final Codec<DyedItemColor> CODEC = Codec.withAlternative(FULL_CODEC, Codec.INT, p_330172_ -> new DyedItemColor(p_330172_, true));
   public static final StreamCodec<ByteBuf, DyedItemColor> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, DyedItemColor::rgb, ByteBufCodecs.BOOL, DyedItemColor::showInTooltip, DyedItemColor::new
   );
   public static final int LEATHER_COLOR = -6265536;

   public static int getOrDefault(ItemStack pStack, int pDefaultValue) {
      DyedItemColor $$2 = pStack.get(DataComponents.DYED_COLOR);
      return $$2 != null ? FastColor.ARGB32.opaque($$2.rgb()) : pDefaultValue;
   }

   public static ItemStack applyDyes(ItemStack pStack, List<DyeItem> pDyes) {
      if (!pStack.is(ItemTags.DYEABLE)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$2 = pStack.copyWithCount(1);
         int $$3 = 0;
         int $$4 = 0;
         int $$5 = 0;
         int $$6 = 0;
         int $$7 = 0;
         DyedItemColor $$8 = $$2.get(DataComponents.DYED_COLOR);
         if ($$8 != null) {
            int $$9 = FastColor.ARGB32.red($$8.rgb());
            int $$10 = FastColor.ARGB32.green($$8.rgb());
            int $$11 = FastColor.ARGB32.blue($$8.rgb());
            $$6 += Math.max($$9, Math.max($$10, $$11));
            $$3 += $$9;
            $$4 += $$10;
            $$5 += $$11;
            $$7++;
         }

         for (DyeItem $$12 : pDyes) {
            int $$13 = $$12.getDyeColor().getTextureDiffuseColor();
            int $$14 = FastColor.ARGB32.red($$13);
            int $$15 = FastColor.ARGB32.green($$13);
            int $$16 = FastColor.ARGB32.blue($$13);
            $$6 += Math.max($$14, Math.max($$15, $$16));
            $$3 += $$14;
            $$4 += $$15;
            $$5 += $$16;
            $$7++;
         }

         int $$17 = $$3 / $$7;
         int $$18 = $$4 / $$7;
         int $$19 = $$5 / $$7;
         float $$20 = (float)$$6 / (float)$$7;
         float $$21 = (float)Math.max($$17, Math.max($$18, $$19));
         $$17 = (int)((float)$$17 * $$20 / $$21);
         $$18 = (int)((float)$$18 * $$20 / $$21);
         $$19 = (int)((float)$$19 * $$20 / $$21);
         int $$22 = FastColor.ARGB32.color(0, $$17, $$18, $$19);
         boolean $$23 = $$8 == null || $$8.showInTooltip();
         $$2.set(DataComponents.DYED_COLOR, new DyedItemColor($$22, $$23));
         return $$2;
      }
   }

   @Override
   public void addToTooltip(Item.TooltipContext pContext, Consumer<Component> pTooltipAdder, TooltipFlag pTooltipFlag) {
      if (this.showInTooltip) {
         if (pTooltipFlag.isAdvanced()) {
            pTooltipAdder.accept(Component.translatable("item.color", String.format(Locale.ROOT, "#%06X", this.rgb)).withStyle(ChatFormatting.GRAY));
         } else {
            pTooltipAdder.accept(Component.translatable("item.dyed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
         }
      }
   }

   public DyedItemColor withTooltip(boolean pShowInTooltip) {
      return new DyedItemColor(this.rgb, pShowInTooltip);
   }
}
