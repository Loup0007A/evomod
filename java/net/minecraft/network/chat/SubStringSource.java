package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
   private final String plainText;
   private final List<Style> charStyles;
   private final Int2IntFunction reverseCharModifier;

   private SubStringSource(String pPlainText, List<Style> pCharStyles, Int2IntFunction pReverseCharModifier) {
      this.plainText = pPlainText;
      this.charStyles = ImmutableList.copyOf(pCharStyles);
      this.reverseCharModifier = pReverseCharModifier;
   }

   public String getPlainText() {
      return this.plainText;
   }

   public List<FormattedCharSequence> substring(int pFromIndex, int pToIndex, boolean pReversed) {
      if (pToIndex == 0) {
         return ImmutableList.of();
      } else {
         List<FormattedCharSequence> $$3 = Lists.newArrayList();
         Style $$4 = this.charStyles.get(pFromIndex);
         int $$5 = pFromIndex;

         for (int $$6 = 1; $$6 < pToIndex; $$6++) {
            int $$7 = pFromIndex + $$6;
            Style $$8 = this.charStyles.get($$7);
            if (!$$8.equals($$4)) {
               String $$9 = this.plainText.substring($$5, $$7);
               $$3.add(pReversed ? FormattedCharSequence.backward($$9, $$4, this.reverseCharModifier) : FormattedCharSequence.forward($$9, $$4));
               $$4 = $$8;
               $$5 = $$7;
            }
         }

         if ($$5 < pFromIndex + pToIndex) {
            String $$10 = this.plainText.substring($$5, pFromIndex + pToIndex);
            $$3.add(pReversed ? FormattedCharSequence.backward($$10, $$4, this.reverseCharModifier) : FormattedCharSequence.forward($$10, $$4));
         }

         return pReversed ? Lists.reverse($$3) : $$3;
      }
   }

   public static SubStringSource create(FormattedText pFormattedText) {
      return create(pFormattedText, p_178527_ -> p_178527_, p_178529_ -> p_178529_);
   }

   public static SubStringSource create(FormattedText pFormattedText, Int2IntFunction pReverseCharModifier, UnaryOperator<String> pTextTransformer) {
      StringBuilder $$3 = new StringBuilder();
      List<Style> $$4 = Lists.newArrayList();
      pFormattedText.visit((p_131249_, p_131250_) -> {
         StringDecomposer.iterateFormatted(p_131250_, p_131249_, (p_178533_, p_178534_, p_178535_) -> {
            $$3.appendCodePoint(p_178535_);
            int $$5 = Character.charCount(p_178535_);

            for (int $$6 = 0; $$6 < $$5; $$6++) {
               $$4.add(p_178534_);
            }

            return true;
         });
         return Optional.empty();
      }, Style.EMPTY);
      return new SubStringSource(pTextTransformer.apply($$3.toString()), $$4, pReverseCharModifier);
   }
}
