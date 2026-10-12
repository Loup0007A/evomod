package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;

public class MutableComponent implements Component {
   private final ComponentContents contents;
   private final List<Component> siblings;
   private Style style;
   private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
   @Nullable
   private Language decomposedWith;

   MutableComponent(ComponentContents pContents, List<Component> pSiblings, Style pStyle) {
      this.contents = pContents;
      this.siblings = pSiblings;
      this.style = pStyle;
   }

   public static MutableComponent create(ComponentContents pContents) {
      return new MutableComponent(pContents, Lists.newArrayList(), Style.EMPTY);
   }

   @Override
   public ComponentContents getContents() {
      return this.contents;
   }

   @Override
   public List<Component> getSiblings() {
      return this.siblings;
   }

   public MutableComponent setStyle(Style pStyle) {
      this.style = pStyle;
      return this;
   }

   @Override
   public Style getStyle() {
      return this.style;
   }

   public MutableComponent append(String pString) {
      return pString.isEmpty() ? this : this.append(Component.literal(pString));
   }

   public MutableComponent append(Component pSibling) {
      this.siblings.add(pSibling);
      return this;
   }

   public MutableComponent withStyle(UnaryOperator<Style> pModifyFunc) {
      this.setStyle(pModifyFunc.apply(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(Style pStyle) {
      this.setStyle(pStyle.applyTo(this.getStyle()));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting... pFormats) {
      this.setStyle(this.getStyle().applyFormats(pFormats));
      return this;
   }

   public MutableComponent withStyle(ChatFormatting pFormat) {
      this.setStyle(this.getStyle().applyFormat(pFormat));
      return this;
   }

   public MutableComponent withColor(int pColor) {
      this.setStyle(this.getStyle().withColor(pColor));
      return this;
   }

   @Override
   public FormattedCharSequence getVisualOrderText() {
      Language $$0 = Language.getInstance();
      if (this.decomposedWith != $$0) {
         this.visualOrderText = $$0.getVisualOrder(this);
         this.decomposedWith = $$0;
      }

      return this.visualOrderText;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof MutableComponent $$1)
            ? false
            : this.contents.equals($$1.contents) && this.style.equals($$1.style) && this.siblings.equals($$1.siblings);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.contents, this.style, this.siblings);
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder(this.contents.toString());
      boolean $$1 = !this.style.isEmpty();
      boolean $$2 = !this.siblings.isEmpty();
      if ($$1 || $$2) {
         $$0.append('[');
         if ($$1) {
            $$0.append("style=");
            $$0.append(this.style);
         }

         if ($$1 && $$2) {
            $$0.append(", ");
         }

         if ($$2) {
            $$0.append("siblings=");
            $$0.append(this.siblings);
         }

         $$0.append(']');
      }

      return $$0.toString();
   }
}
