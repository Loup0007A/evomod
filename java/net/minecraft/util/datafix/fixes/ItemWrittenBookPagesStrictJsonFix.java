package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class ItemWrittenBookPagesStrictJsonFix extends DataFix {
   public ItemWrittenBookPagesStrictJsonFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public Dynamic<?> fixTag(Dynamic<?> p_16172_) {
      return p_16172_.update(
         "pages",
         p_326606_ -> (Dynamic)DataFixUtils.orElse(
               p_326606_.asStreamOpt().map(p_145441_ -> p_145441_.map(ComponentDataFixUtils::rewriteFromLenient)).map(p_16172_::createList).result(),
               p_16172_.emptyList()
            )
      );
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> $$1 = $$0.findField("tag");
      return this.fixTypeEverywhereTyped(
         "ItemWrittenBookPagesStrictJsonFix", $$0, p_16168_ -> p_16168_.updateTyped($$1, p_145439_ -> p_145439_.update(DSL.remainderFinder(), this::fixTag))
      );
   }
}
