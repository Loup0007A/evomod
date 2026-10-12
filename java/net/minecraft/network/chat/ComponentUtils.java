package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;

public class ComponentUtils {
   public static final String DEFAULT_SEPARATOR_TEXT = ", ";
   public static final Component DEFAULT_SEPARATOR = Component.literal(", ").withStyle(ChatFormatting.GRAY);
   public static final Component DEFAULT_NO_STYLE_SEPARATOR = Component.literal(", ");

   public static MutableComponent mergeStyles(MutableComponent pComponent, Style pStyle) {
      if (pStyle.isEmpty()) {
         return pComponent;
      } else {
         Style $$2 = pComponent.getStyle();
         if ($$2.isEmpty()) {
            return pComponent.setStyle(pStyle);
         } else {
            return $$2.equals(pStyle) ? pComponent : pComponent.setStyle($$2.applyTo(pStyle));
         }
      }
   }

   public static Optional<MutableComponent> updateForEntity(
      @Nullable CommandSourceStack pCommandSourceStack, Optional<Component> pOptionalComponent, @Nullable Entity pEntity, int pRecursionDepth
   ) throws CommandSyntaxException {
      return pOptionalComponent.isPresent()
         ? Optional.of(updateForEntity(pCommandSourceStack, pOptionalComponent.get(), pEntity, pRecursionDepth))
         : Optional.empty();
   }

   public static MutableComponent updateForEntity(
      @Nullable CommandSourceStack pCommandSourceStack, Component pComponent, @Nullable Entity pEntity, int pRecursionDepth
   ) throws CommandSyntaxException {
      if (pRecursionDepth > 100) {
         return pComponent.copy();
      } else {
         MutableComponent $$4 = pComponent.getContents().resolve(pCommandSourceStack, pEntity, pRecursionDepth + 1);

         for (Component $$5 : pComponent.getSiblings()) {
            $$4.append(updateForEntity(pCommandSourceStack, $$5, pEntity, pRecursionDepth + 1));
         }

         return $$4.withStyle(resolveStyle(pCommandSourceStack, pComponent.getStyle(), pEntity, pRecursionDepth));
      }
   }

   private static Style resolveStyle(@Nullable CommandSourceStack pCommandSourceStack, Style pStyle, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      HoverEvent $$4 = pStyle.getHoverEvent();
      if ($$4 != null) {
         Component $$5 = $$4.getValue(HoverEvent.Action.SHOW_TEXT);
         if ($$5 != null) {
            HoverEvent $$6 = new HoverEvent(HoverEvent.Action.SHOW_TEXT, updateForEntity(pCommandSourceStack, $$5, pEntity, pRecursionDepth + 1));
            return pStyle.withHoverEvent($$6);
         }
      }

      return pStyle;
   }

   public static Component formatList(Collection<String> pElements) {
      return formatAndSortList(pElements, p_130742_ -> Component.literal(p_130742_).withStyle(ChatFormatting.GREEN));
   }

   public static <T extends Comparable<T>> Component formatAndSortList(Collection<T> pElements, Function<T, Component> pComponentExtractor) {
      if (pElements.isEmpty()) {
         return CommonComponents.EMPTY;
      } else if (pElements.size() == 1) {
         return pComponentExtractor.apply(pElements.iterator().next());
      } else {
         List<T> $$2 = Lists.newArrayList(pElements);
         $$2.sort(Comparable::compareTo);
         return formatList($$2, pComponentExtractor);
      }
   }

   public static <T> Component formatList(Collection<? extends T> pElements, Function<T, Component> pComponentExtractor) {
      return formatList(pElements, DEFAULT_SEPARATOR, pComponentExtractor);
   }

   public static <T> MutableComponent formatList(
      Collection<? extends T> pElements, Optional<? extends Component> pOptionalSeparator, Function<T, Component> pComponentExtractor
   ) {
      return formatList(pElements, (Component)DataFixUtils.orElse(pOptionalSeparator, DEFAULT_SEPARATOR), pComponentExtractor);
   }

   public static Component formatList(Collection<? extends Component> pElements, Component pSeparator) {
      return formatList(pElements, pSeparator, Function.identity());
   }

   public static <T> MutableComponent formatList(Collection<? extends T> pElements, Component pSeparator, Function<T, Component> pComponentExtractor) {
      if (pElements.isEmpty()) {
         return Component.empty();
      } else if (pElements.size() == 1) {
         return pComponentExtractor.apply((T)pElements.iterator().next()).copy();
      } else {
         MutableComponent $$3 = Component.empty();
         boolean $$4 = true;

         for (T $$5 : pElements) {
            if (!$$4) {
               $$3.append(pSeparator);
            }

            $$3.append(pComponentExtractor.apply($$5));
            $$4 = false;
         }

         return $$3;
      }
   }

   public static MutableComponent wrapInSquareBrackets(Component pToWrap) {
      return Component.translatable("chat.square_brackets", pToWrap);
   }

   public static Component fromMessage(Message pMessage) {
      return (Component)(pMessage instanceof Component ? (Component)pMessage : Component.literal(pMessage.getString()));
   }

   public static boolean isTranslationResolvable(@Nullable Component pComponent) {
      if (pComponent != null && pComponent.getContents() instanceof TranslatableContents $$1) {
         String $$2 = $$1.getKey();
         String $$3 = $$1.getFallback();
         return $$3 != null || Language.getInstance().has($$2);
      } else {
         return true;
      }
   }

   public static MutableComponent copyOnClickText(String pText) {
      return wrapInSquareBrackets(
         Component.literal(pText)
            .withStyle(
               p_258207_ -> p_258207_.withColor(ChatFormatting.GREEN)
                     .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, pText))
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                     .withInsertion(pText)
            )
      );
   }
}
