package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.stream.Collectors;

public class OptionsKeyTranslationFix extends DataFix {
   public OptionsKeyTranslationFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsKeyTranslationFix",
         this.getInputSchema().getType(References.OPTIONS),
         p_16648_ -> p_16648_.update(
               DSL.remainderFinder(),
               p_326636_ -> p_326636_.getMapValues().map(p_145588_ -> p_326636_.createMap(p_145588_.entrySet().stream().map(p_145585_ -> {
                        if (((Dynamic)p_145585_.getKey()).asString("").startsWith("key_")) {
                           String $$2 = ((Dynamic)p_145585_.getValue()).asString("");
                           if (!$$2.startsWith("key.mouse") && !$$2.startsWith("scancode.")) {
                              return Pair.of((Dynamic)p_145585_.getKey(), p_326636_.createString("key.keyboard." + $$2.substring("key.".length())));
                           }
                        }

                        return Pair.of((Dynamic)p_145585_.getKey(), (Dynamic)p_145585_.getValue());
                     }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))).result().orElse(p_326636_)
            )
      );
   }
}
