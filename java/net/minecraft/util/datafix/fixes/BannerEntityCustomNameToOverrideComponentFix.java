package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Map;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class BannerEntityCustomNameToOverrideComponentFix extends DataFix {
   public BannerEntityCustomNameToOverrideComponentFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.BLOCK_ENTITY);
      TaggedChoiceType<?> $$1 = this.getInputSchema().findChoiceType(References.BLOCK_ENTITY);
      OpticFinder<?> $$2 = $$0.findField("components");
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", $$0, p_332332_ -> {
         Object $$3 = ((Pair)p_332332_.get($$1.finder())).getFirst();
         return $$3.equals("minecraft:banner") ? this.fix(p_332332_, $$2) : p_332332_;
      });
   }

   private Typed<?> fix(Typed<?> pData, OpticFinder<?> pFinder) {
      Dynamic<?> $$2 = (Dynamic<?>)pData.getOptional(DSL.remainderFinder()).orElseThrow();
      OptionalDynamic<?> $$3 = $$2.get("CustomName");
      boolean $$4 = $$3.asString()
         .result()
         .flatMap(ComponentDataFixUtils::extractTranslationString)
         .filter(p_334057_ -> p_334057_.equals("block.minecraft.ominous_banner"))
         .isPresent();
      if ($$4) {
         Typed<?> $$5 = pData.getOrCreateTyped(pFinder)
            .update(
               DSL.remainderFinder(),
               p_335676_ -> p_335676_.set("minecraft:item_name", (Dynamic)$$3.result().get())
                     .set("minecraft:hide_additional_tooltip", p_335676_.createMap(Map.of()))
            );
         return pData.set(pFinder, $$5).set(DSL.remainderFinder(), $$2.remove("CustomName"));
      } else {
         return pData;
      }
   }
}
