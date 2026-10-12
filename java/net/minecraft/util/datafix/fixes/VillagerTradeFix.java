package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class VillagerTradeFix extends DataFix {
   public VillagerTradeFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.VILLAGER_TRADE);
      OpticFinder<?> $$1 = $$0.findField("buy");
      OpticFinder<?> $$2 = $$0.findField("buyB");
      OpticFinder<?> $$3 = $$0.findField("sell");
      OpticFinder<Pair<String, String>> $$4 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      Function<Typed<?>, Typed<?>> $$5 = p_17150_ -> this.updateItemStack($$4, p_17150_);
      return this.fixTypeEverywhereTyped("Villager trade fix", $$0, p_145788_ -> p_145788_.updateTyped($$1, $$5).updateTyped($$2, $$5).updateTyped($$3, $$5));
   }

   private Typed<?> updateItemStack(OpticFinder<Pair<String, String>> pId, Typed<?> pTyped) {
      return pTyped.update(
         pId, p_17145_ -> p_17145_.mapSecond(p_145790_ -> Objects.equals(p_145790_, "minecraft:carved_pumpkin") ? "minecraft:pumpkin" : p_145790_)
      );
   }
}
