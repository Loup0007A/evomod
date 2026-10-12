package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EmptyItemInVillagerTradeFix extends DataFix {
   public EmptyItemInVillagerTradeFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.VILLAGER_TRADE);
      return this.writeFixAndRead("EmptyItemInVillagerTradeFix", $$0, $$0, p_333025_ -> {
         Dynamic<?> $$1 = p_333025_.get("buyB").orElseEmptyMap();
         String $$2 = NamespacedSchema.ensureNamespaced($$1.get("id").asString("minecraft:air"));
         int $$3 = $$1.get("count").asInt(0);
         return !$$2.equals("minecraft:air") && $$3 != 0 ? p_333025_ : p_333025_.remove("buyB");
      });
   }
}
