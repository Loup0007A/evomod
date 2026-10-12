package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3689 extends NamespacedSchema {
   public V3689(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(pSchema);
      pSchema.register($$1, "minecraft:breeze", () -> V100.equipment(pSchema));
      pSchema.registerSimple($$1, "minecraft:wind_charge");
      pSchema.registerSimple($$1, "minecraft:breeze_wind_charge");
      return $$1;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerBlockEntities(pSchema);
      pSchema.register(
         $$1,
         "minecraft:trial_spawner",
         () -> DSL.optionalFields(
               "spawn_potentials",
               DSL.list(DSL.fields("data", DSL.fields("entity", References.ENTITY_TREE.in(pSchema)))),
               "spawn_data",
               DSL.fields("entity", References.ENTITY_TREE.in(pSchema))
            )
      );
      return $$1;
   }
}
