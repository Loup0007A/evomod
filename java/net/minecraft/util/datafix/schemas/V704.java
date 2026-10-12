package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V704 extends Schema {
   protected static final Map<String, String> ITEM_TO_BLOCKENTITY = (Map<String, String>)DataFixUtils.make(() -> {
      Map<String, String> $$0 = Maps.newHashMap();
      $$0.put("minecraft:furnace", "minecraft:furnace");
      $$0.put("minecraft:lit_furnace", "minecraft:furnace");
      $$0.put("minecraft:chest", "minecraft:chest");
      $$0.put("minecraft:trapped_chest", "minecraft:chest");
      $$0.put("minecraft:ender_chest", "minecraft:ender_chest");
      $$0.put("minecraft:jukebox", "minecraft:jukebox");
      $$0.put("minecraft:dispenser", "minecraft:dispenser");
      $$0.put("minecraft:dropper", "minecraft:dropper");
      $$0.put("minecraft:sign", "minecraft:sign");
      $$0.put("minecraft:mob_spawner", "minecraft:mob_spawner");
      $$0.put("minecraft:spawner", "minecraft:mob_spawner");
      $$0.put("minecraft:noteblock", "minecraft:noteblock");
      $$0.put("minecraft:brewing_stand", "minecraft:brewing_stand");
      $$0.put("minecraft:enhanting_table", "minecraft:enchanting_table");
      $$0.put("minecraft:command_block", "minecraft:command_block");
      $$0.put("minecraft:beacon", "minecraft:beacon");
      $$0.put("minecraft:skull", "minecraft:skull");
      $$0.put("minecraft:daylight_detector", "minecraft:daylight_detector");
      $$0.put("minecraft:hopper", "minecraft:hopper");
      $$0.put("minecraft:banner", "minecraft:banner");
      $$0.put("minecraft:flower_pot", "minecraft:flower_pot");
      $$0.put("minecraft:repeating_command_block", "minecraft:command_block");
      $$0.put("minecraft:chain_command_block", "minecraft:command_block");
      $$0.put("minecraft:shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:white_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:orange_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:magenta_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:light_blue_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:yellow_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:lime_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:pink_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:gray_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:silver_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:cyan_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:purple_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:blue_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:brown_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:green_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:red_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:black_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:bed", "minecraft:bed");
      $$0.put("minecraft:light_gray_shulker_box", "minecraft:shulker_box");
      $$0.put("minecraft:banner", "minecraft:banner");
      $$0.put("minecraft:white_banner", "minecraft:banner");
      $$0.put("minecraft:orange_banner", "minecraft:banner");
      $$0.put("minecraft:magenta_banner", "minecraft:banner");
      $$0.put("minecraft:light_blue_banner", "minecraft:banner");
      $$0.put("minecraft:yellow_banner", "minecraft:banner");
      $$0.put("minecraft:lime_banner", "minecraft:banner");
      $$0.put("minecraft:pink_banner", "minecraft:banner");
      $$0.put("minecraft:gray_banner", "minecraft:banner");
      $$0.put("minecraft:silver_banner", "minecraft:banner");
      $$0.put("minecraft:light_gray_banner", "minecraft:banner");
      $$0.put("minecraft:cyan_banner", "minecraft:banner");
      $$0.put("minecraft:purple_banner", "minecraft:banner");
      $$0.put("minecraft:blue_banner", "minecraft:banner");
      $$0.put("minecraft:brown_banner", "minecraft:banner");
      $$0.put("minecraft:green_banner", "minecraft:banner");
      $$0.put("minecraft:red_banner", "minecraft:banner");
      $$0.put("minecraft:black_banner", "minecraft:banner");
      $$0.put("minecraft:standing_sign", "minecraft:sign");
      $$0.put("minecraft:wall_sign", "minecraft:sign");
      $$0.put("minecraft:piston_head", "minecraft:piston");
      $$0.put("minecraft:daylight_detector_inverted", "minecraft:daylight_detector");
      $$0.put("minecraft:unpowered_comparator", "minecraft:comparator");
      $$0.put("minecraft:powered_comparator", "minecraft:comparator");
      $$0.put("minecraft:wall_banner", "minecraft:banner");
      $$0.put("minecraft:standing_banner", "minecraft:banner");
      $$0.put("minecraft:structure_block", "minecraft:structure_block");
      $$0.put("minecraft:end_portal", "minecraft:end_portal");
      $$0.put("minecraft:end_gateway", "minecraft:end_gateway");
      $$0.put("minecraft:sign", "minecraft:sign");
      $$0.put("minecraft:shield", "minecraft:banner");
      $$0.put("minecraft:white_bed", "minecraft:bed");
      $$0.put("minecraft:orange_bed", "minecraft:bed");
      $$0.put("minecraft:magenta_bed", "minecraft:bed");
      $$0.put("minecraft:light_blue_bed", "minecraft:bed");
      $$0.put("minecraft:yellow_bed", "minecraft:bed");
      $$0.put("minecraft:lime_bed", "minecraft:bed");
      $$0.put("minecraft:pink_bed", "minecraft:bed");
      $$0.put("minecraft:gray_bed", "minecraft:bed");
      $$0.put("minecraft:silver_bed", "minecraft:bed");
      $$0.put("minecraft:light_gray_bed", "minecraft:bed");
      $$0.put("minecraft:cyan_bed", "minecraft:bed");
      $$0.put("minecraft:purple_bed", "minecraft:bed");
      $$0.put("minecraft:blue_bed", "minecraft:bed");
      $$0.put("minecraft:brown_bed", "minecraft:bed");
      $$0.put("minecraft:green_bed", "minecraft:bed");
      $$0.put("minecraft:red_bed", "minecraft:bed");
      $$0.put("minecraft:black_bed", "minecraft:bed");
      $$0.put("minecraft:oak_sign", "minecraft:sign");
      $$0.put("minecraft:spruce_sign", "minecraft:sign");
      $$0.put("minecraft:birch_sign", "minecraft:sign");
      $$0.put("minecraft:jungle_sign", "minecraft:sign");
      $$0.put("minecraft:acacia_sign", "minecraft:sign");
      $$0.put("minecraft:dark_oak_sign", "minecraft:sign");
      $$0.put("minecraft:crimson_sign", "minecraft:sign");
      $$0.put("minecraft:warped_sign", "minecraft:sign");
      $$0.put("minecraft:skeleton_skull", "minecraft:skull");
      $$0.put("minecraft:wither_skeleton_skull", "minecraft:skull");
      $$0.put("minecraft:zombie_head", "minecraft:skull");
      $$0.put("minecraft:player_head", "minecraft:skull");
      $$0.put("minecraft:creeper_head", "minecraft:skull");
      $$0.put("minecraft:dragon_head", "minecraft:skull");
      $$0.put("minecraft:barrel", "minecraft:barrel");
      $$0.put("minecraft:conduit", "minecraft:conduit");
      $$0.put("minecraft:smoker", "minecraft:smoker");
      $$0.put("minecraft:blast_furnace", "minecraft:blast_furnace");
      $$0.put("minecraft:lectern", "minecraft:lectern");
      $$0.put("minecraft:bell", "minecraft:bell");
      $$0.put("minecraft:jigsaw", "minecraft:jigsaw");
      $$0.put("minecraft:campfire", "minecraft:campfire");
      $$0.put("minecraft:bee_nest", "minecraft:beehive");
      $$0.put("minecraft:beehive", "minecraft:beehive");
      $$0.put("minecraft:sculk_sensor", "minecraft:sculk_sensor");
      $$0.put("minecraft:decorated_pot", "minecraft:decorated_pot");
      $$0.put("minecraft:crafter", "minecraft:crafter");
      return ImmutableMap.copyOf($$0);
   });
   protected static final HookFunction ADD_NAMES = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_18070_, T p_18071_) {
         return V99.addNames(new Dynamic(p_18070_, p_18071_), V704.ITEM_TO_BLOCKENTITY, V99.ITEM_TO_ENTITY);
      }
   };

   public V704(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema))));
   }

   public Type<?> getChoiceType(TypeReference pType, String pChoiceName) {
      return Objects.equals(pType.typeName(), References.BLOCK_ENTITY.typeName())
         ? super.getChoiceType(pType, NamespacedSchema.ensureNamespaced(pChoiceName))
         : super.getChoiceType(pType, pChoiceName);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      registerInventory(pSchema, $$1, "minecraft:furnace");
      registerInventory(pSchema, $$1, "minecraft:chest");
      pSchema.registerSimple($$1, "minecraft:ender_chest");
      pSchema.register($$1, "minecraft:jukebox", p_18058_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(pSchema)));
      registerInventory(pSchema, $$1, "minecraft:dispenser");
      registerInventory(pSchema, $$1, "minecraft:dropper");
      pSchema.registerSimple($$1, "minecraft:sign");
      pSchema.register($$1, "minecraft:mob_spawner", p_18055_ -> References.UNTAGGED_SPAWNER.in(pSchema));
      pSchema.registerSimple($$1, "minecraft:noteblock");
      pSchema.registerSimple($$1, "minecraft:piston");
      registerInventory(pSchema, $$1, "minecraft:brewing_stand");
      pSchema.registerSimple($$1, "minecraft:enchanting_table");
      pSchema.registerSimple($$1, "minecraft:end_portal");
      pSchema.registerSimple($$1, "minecraft:beacon");
      pSchema.registerSimple($$1, "minecraft:skull");
      pSchema.registerSimple($$1, "minecraft:daylight_detector");
      registerInventory(pSchema, $$1, "minecraft:hopper");
      pSchema.registerSimple($$1, "minecraft:comparator");
      pSchema.register(
         $$1, "minecraft:flower_pot", p_18042_ -> DSL.optionalFields("Item", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(pSchema)))
      );
      pSchema.registerSimple($$1, "minecraft:banner");
      pSchema.registerSimple($$1, "minecraft:structure_block");
      pSchema.registerSimple($$1, "minecraft:end_gateway");
      pSchema.registerSimple($$1, "minecraft:command_block");
      return $$1;
   }

   public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
      super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
      pSchema.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields(
               "components", References.DATA_COMPONENTS.in(pSchema), DSL.taggedChoiceLazy("id", NamespacedSchema.namespacedString(), pBlockEntityTypes)
            )
      );
      pSchema.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(
               DSL.optionalFields(
                  "id",
                  References.ITEM_NAME.in(pSchema),
                  "tag",
                  DSL.optionalFields(
                     new Pair[]{
                        Pair.of("EntityTag", References.ENTITY_TREE.in(pSchema)),
                        Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(pSchema)),
                        Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(pSchema))),
                        Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(pSchema))),
                        Pair.of("Items", DSL.list(References.ITEM_STACK.in(pSchema))),
                        Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(pSchema)))
                     }
                  )
               ),
               ADD_NAMES,
               HookFunction.IDENTITY
            )
      );
   }
}
