package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class ItemLoreFix extends DataFix {
   public ItemLoreFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> $$1 = $$0.findField("tag");
      return this.fixTypeEverywhereTyped(
         "Item Lore componentize",
         $$0,
         p_15962_ -> p_15962_.updateTyped(
               $$1,
               p_145392_ -> p_145392_.update(
                     DSL.remainderFinder(),
                     p_145394_ -> p_145394_.update(
                           "display",
                           p_145396_ -> p_145396_.update(
                                 "Lore",
                                 p_326595_ -> (Dynamic)DataFixUtils.orElse(
                                       p_326595_.asStreamOpt().map(ItemLoreFix::fixLoreList).map(p_326595_::createList).result(), p_326595_
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static <T> Stream<Dynamic<T>> fixLoreList(Stream<Dynamic<T>> p_15970_) {
      return p_15970_.map(ComponentDataFixUtils::wrapLiteralStringAsComponent);
   }
}
