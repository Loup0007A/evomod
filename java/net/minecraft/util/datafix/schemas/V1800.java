package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1800 extends NamespacedSchema {
   public V1800(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(pSchema);
      pSchema.register($$1, "minecraft:panda", () -> V100.equipment(pSchema));
      pSchema.register(
         $$1, "minecraft:pillager", p_17738_ -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(pSchema)), V100.equipment(pSchema))
      );
      return $$1;
   }
}
