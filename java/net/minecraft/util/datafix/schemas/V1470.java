package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1470 extends NamespacedSchema {
   public V1470(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> V100.equipment(pSchema));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(pSchema);
      registerMob(pSchema, $$1, "minecraft:turtle");
      registerMob(pSchema, $$1, "minecraft:cod_mob");
      registerMob(pSchema, $$1, "minecraft:tropical_fish");
      registerMob(pSchema, $$1, "minecraft:salmon_mob");
      registerMob(pSchema, $$1, "minecraft:puffer_fish");
      registerMob(pSchema, $$1, "minecraft:phantom");
      registerMob(pSchema, $$1, "minecraft:dolphin");
      registerMob(pSchema, $$1, "minecraft:drowned");
      pSchema.register(
         $$1,
         "minecraft:trident",
         p_309012_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema), "Trident", References.ITEM_STACK.in(pSchema))
      );
      return $$1;
   }
}
