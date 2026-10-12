package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1906 extends NamespacedSchema {
   public V1906(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerBlockEntities(pSchema);
      registerInventory(pSchema, $$1, "minecraft:barrel");
      registerInventory(pSchema, $$1, "minecraft:smoker");
      registerInventory(pSchema, $$1, "minecraft:blast_furnace");
      pSchema.register($$1, "minecraft:lectern", p_17774_ -> DSL.optionalFields("Book", References.ITEM_STACK.in(pSchema)));
      pSchema.registerSimple($$1, "minecraft:bell");
      return $$1;
   }

   protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema))));
   }
}
