package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsAddTextBackgroundFix extends DataFix {
   public OptionsAddTextBackgroundFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsAddTextBackgroundFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_16610_ -> p_16610_.update(
               DSL.remainderFinder(),
               p_326633_ -> (Dynamic)DataFixUtils.orElse(
                     p_326633_.get("chatOpacity")
                        .asString()
                        .map(p_145570_ -> p_326633_.set("textBackgroundOpacity", p_326633_.createDouble(this.calculateBackground(p_145570_))))
                        .result(),
                     p_326633_
                  )
            )
      );
   }

   private double calculateBackground(String pOldBackground) {
      try {
         double $$1 = 0.9 * Double.parseDouble(pOldBackground) + 0.1;
         return $$1 / 2.0;
      } catch (NumberFormatException var4) {
         return 0.5;
      }
   }
}
