package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V3438 extends NamespacedSchema {
   public V3438(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerBlockEntities(pSchema);
      $$1.put("minecraft:brushable_block", $$1.remove("minecraft:suspicious_sand"));
      pSchema.registerSimple($$1, "minecraft:calibrated_sculk_sensor");
      return $$1;
   }
}
