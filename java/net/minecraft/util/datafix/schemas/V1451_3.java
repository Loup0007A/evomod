package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_3 extends NamespacedSchema {
   public V1451_3(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(pSchema);
      pSchema.registerSimple($$1, "minecraft:egg");
      pSchema.registerSimple($$1, "minecraft:ender_pearl");
      pSchema.registerSimple($$1, "minecraft:fireball");
      pSchema.register($$1, "minecraft:potion", p_17450_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema)));
      pSchema.registerSimple($$1, "minecraft:small_fireball");
      pSchema.registerSimple($$1, "minecraft:snowball");
      pSchema.registerSimple($$1, "minecraft:wither_skull");
      pSchema.registerSimple($$1, "minecraft:xp_bottle");
      pSchema.register($$1, "minecraft:arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
      pSchema.register($$1, "minecraft:enderman", () -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(pSchema), V100.equipment(pSchema)));
      pSchema.register(
         $$1,
         "minecraft:falling_block",
         () -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
      );
      pSchema.register($$1, "minecraft:spectral_arrow", () -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
      pSchema.register(
         $$1,
         "minecraft:chest_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      pSchema.register($$1, "minecraft:commandblock_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      pSchema.register($$1, "minecraft:furnace_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      pSchema.register(
         $$1,
         "minecraft:hopper_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      pSchema.register($$1, "minecraft:minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      pSchema.register(
         $$1,
         "minecraft:spawner_minecart",
         () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
      );
      pSchema.register($$1, "minecraft:tnt_minecart", () -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      return $$1;
   }
}
