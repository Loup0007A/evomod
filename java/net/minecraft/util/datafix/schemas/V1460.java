package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1460 extends NamespacedSchema {
   public V1460(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> V100.equipment(pSchema));
   }

   protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema))));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      pSchema.register($$1, "minecraft:area_effect_cloud", p_326692_ -> DSL.optionalFields("Particle", References.PARTICLE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:armor_stand");
      pSchema.register($$1, "minecraft:arrow", p_17677_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:bat");
      registerMob(pSchema, $$1, "minecraft:blaze");
      pSchema.registerSimple($$1, "minecraft:boat");
      registerMob(pSchema, $$1, "minecraft:cave_spider");
      pSchema.register(
         $$1,
         "minecraft:chest_minecart",
         p_17680_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      registerMob(pSchema, $$1, "minecraft:chicken");
      pSchema.register($$1, "minecraft:commandblock_minecart", p_17654_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:cow");
      registerMob(pSchema, $$1, "minecraft:creeper");
      pSchema.register(
         $$1,
         "minecraft:donkey",
         p_17674_ -> DSL.optionalFields(
               "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      pSchema.registerSimple($$1, "minecraft:dragon_fireball");
      pSchema.registerSimple($$1, "minecraft:egg");
      registerMob(pSchema, $$1, "minecraft:elder_guardian");
      pSchema.registerSimple($$1, "minecraft:ender_crystal");
      registerMob(pSchema, $$1, "minecraft:ender_dragon");
      pSchema.register(
         $$1, "minecraft:enderman", p_17671_ -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(pSchema), V100.equipment(pSchema))
      );
      registerMob(pSchema, $$1, "minecraft:endermite");
      pSchema.registerSimple($$1, "minecraft:ender_pearl");
      pSchema.registerSimple($$1, "minecraft:evocation_fangs");
      registerMob(pSchema, $$1, "minecraft:evocation_illager");
      pSchema.registerSimple($$1, "minecraft:eye_of_ender_signal");
      pSchema.register(
         $$1,
         "minecraft:falling_block",
         p_17668_ -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
      );
      pSchema.registerSimple($$1, "minecraft:fireball");
      pSchema.register($$1, "minecraft:fireworks_rocket", p_17665_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(pSchema)));
      pSchema.register($$1, "minecraft:furnace_minecart", p_17634_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:ghast");
      registerMob(pSchema, $$1, "minecraft:giant");
      registerMob(pSchema, $$1, "minecraft:guardian");
      pSchema.register(
         $$1,
         "minecraft:hopper_minecart",
         p_17651_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      pSchema.register(
         $$1,
         "minecraft:horse",
         p_17648_ -> DSL.optionalFields(
               "ArmorItem", References.ITEM_STACK.in(pSchema), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:husk");
      pSchema.registerSimple($$1, "minecraft:illusion_illager");
      pSchema.register($$1, "minecraft:item", p_17645_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
      pSchema.register($$1, "minecraft:item_frame", p_17642_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
      pSchema.registerSimple($$1, "minecraft:leash_knot");
      pSchema.register(
         $$1,
         "minecraft:llama",
         p_17639_ -> DSL.optionalFields(
               "Items",
               DSL.list(References.ITEM_STACK.in(pSchema)),
               "SaddleItem",
               References.ITEM_STACK.in(pSchema),
               "DecorItem",
               References.ITEM_STACK.in(pSchema),
               V100.equipment(pSchema)
            )
      );
      pSchema.registerSimple($$1, "minecraft:llama_spit");
      registerMob(pSchema, $$1, "minecraft:magma_cube");
      pSchema.register($$1, "minecraft:minecart", p_17683_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:mooshroom");
      pSchema.register(
         $$1,
         "minecraft:mule",
         p_17629_ -> DSL.optionalFields(
               "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:ocelot");
      pSchema.registerSimple($$1, "minecraft:painting");
      pSchema.registerSimple($$1, "minecraft:parrot");
      registerMob(pSchema, $$1, "minecraft:pig");
      registerMob(pSchema, $$1, "minecraft:polar_bear");
      pSchema.register($$1, "minecraft:potion", p_17624_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:rabbit");
      registerMob(pSchema, $$1, "minecraft:sheep");
      registerMob(pSchema, $$1, "minecraft:shulker");
      pSchema.registerSimple($$1, "minecraft:shulker_bullet");
      registerMob(pSchema, $$1, "minecraft:silverfish");
      registerMob(pSchema, $$1, "minecraft:skeleton");
      pSchema.register(
         $$1, "minecraft:skeleton_horse", p_17619_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema))
      );
      registerMob(pSchema, $$1, "minecraft:slime");
      pSchema.registerSimple($$1, "minecraft:small_fireball");
      pSchema.registerSimple($$1, "minecraft:snowball");
      registerMob(pSchema, $$1, "minecraft:snowman");
      pSchema.register(
         $$1,
         "minecraft:spawner_minecart",
         p_17614_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
      );
      pSchema.register($$1, "minecraft:spectral_arrow", p_17609_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:spider");
      registerMob(pSchema, $$1, "minecraft:squid");
      registerMob(pSchema, $$1, "minecraft:stray");
      pSchema.registerSimple($$1, "minecraft:tnt");
      pSchema.register($$1, "minecraft:tnt_minecart", p_17604_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:vex");
      pSchema.register(
         $$1,
         "minecraft:villager",
         p_326686_ -> DSL.optionalFields(
               "Inventory",
               DSL.list(References.ITEM_STACK.in(pSchema)),
               "Offers",
               DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))),
               V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:villager_golem");
      registerMob(pSchema, $$1, "minecraft:vindication_illager");
      registerMob(pSchema, $$1, "minecraft:witch");
      registerMob(pSchema, $$1, "minecraft:wither");
      registerMob(pSchema, $$1, "minecraft:wither_skeleton");
      pSchema.registerSimple($$1, "minecraft:wither_skull");
      registerMob(pSchema, $$1, "minecraft:wolf");
      pSchema.registerSimple($$1, "minecraft:xp_bottle");
      pSchema.registerSimple($$1, "minecraft:xp_orb");
      registerMob(pSchema, $$1, "minecraft:zombie");
      pSchema.register($$1, "minecraft:zombie_horse", p_17592_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)));
      registerMob(pSchema, $$1, "minecraft:zombie_pigman");
      pSchema.register(
         $$1,
         "minecraft:zombie_villager",
         p_326695_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))), V100.equipment(pSchema))
      );
      return $$1;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      registerInventory(pSchema, $$1, "minecraft:furnace");
      registerInventory(pSchema, $$1, "minecraft:chest");
      registerInventory(pSchema, $$1, "minecraft:trapped_chest");
      pSchema.registerSimple($$1, "minecraft:ender_chest");
      pSchema.register($$1, "minecraft:jukebox", p_17586_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(pSchema)));
      registerInventory(pSchema, $$1, "minecraft:dispenser");
      registerInventory(pSchema, $$1, "minecraft:dropper");
      pSchema.registerSimple($$1, "minecraft:sign");
      pSchema.register($$1, "minecraft:mob_spawner", p_17574_ -> References.UNTAGGED_SPAWNER.in(pSchema));
      pSchema.register($$1, "minecraft:piston", p_17559_ -> DSL.optionalFields("blockState", References.BLOCK_STATE.in(pSchema)));
      registerInventory(pSchema, $$1, "minecraft:brewing_stand");
      pSchema.registerSimple($$1, "minecraft:enchanting_table");
      pSchema.registerSimple($$1, "minecraft:end_portal");
      pSchema.registerSimple($$1, "minecraft:beacon");
      pSchema.registerSimple($$1, "minecraft:skull");
      pSchema.registerSimple($$1, "minecraft:daylight_detector");
      registerInventory(pSchema, $$1, "minecraft:hopper");
      pSchema.registerSimple($$1, "minecraft:comparator");
      pSchema.registerSimple($$1, "minecraft:banner");
      pSchema.registerSimple($$1, "minecraft:structure_block");
      pSchema.registerSimple($$1, "minecraft:end_gateway");
      pSchema.registerSimple($$1, "minecraft:command_block");
      registerInventory(pSchema, $$1, "minecraft:shulker_box");
      pSchema.registerSimple($$1, "minecraft:bed");
      return $$1;
   }

   public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
      pSchema.registerType(false, References.LEVEL, DSL::remainder);
      pSchema.registerType(false, References.RECIPE, () -> DSL.constType(namespacedString()));
      pSchema.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields(
               new Pair[]{
                  Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(pSchema))),
                  Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(pSchema))),
                  Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(pSchema))),
                  Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(pSchema)),
                  Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(pSchema)),
                  Pair.of(
                     "recipeBook",
                     DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(pSchema)), "toBeDisplayed", DSL.list(References.RECIPE.in(pSchema)))
                  )
               }
            )
      );
      pSchema.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
               "Level",
               DSL.optionalFields(
                  "Entities",
                  DSL.list(References.ENTITY_TREE.in(pSchema)),
                  "TileEntities",
                  DSL.list(DSL.or(References.BLOCK_ENTITY.in(pSchema), DSL.remainder())),
                  "TileTicks",
                  DSL.list(DSL.fields("i", References.BLOCK_NAME.in(pSchema))),
                  "Sections",
                  DSL.list(DSL.optionalFields("Palette", DSL.list(References.BLOCK_STATE.in(pSchema))))
               )
            )
      );
      pSchema.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(pSchema), DSL.taggedChoiceLazy("id", namespacedString(), pBlockEntityTypes))
      );
      pSchema.registerType(
         true, References.ENTITY_TREE, () -> DSL.optionalFields("Passengers", DSL.list(References.ENTITY_TREE.in(pSchema)), References.ENTITY.in(pSchema))
      );
      pSchema.registerType(true, References.ENTITY, () -> DSL.taggedChoiceLazy("id", namespacedString(), pEntityTypes));
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
               V705.ADD_NAMES,
               HookFunction.IDENTITY
            )
      );
      pSchema.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(pSchema))));
      pSchema.registerType(false, References.OPTIONS, DSL::remainder);
      pSchema.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
               "entities",
               DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(pSchema))),
               "blocks",
               DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(pSchema))),
               "palette",
               DSL.list(References.BLOCK_STATE.in(pSchema))
            )
      );
      pSchema.registerType(false, References.BLOCK_NAME, () -> DSL.constType(namespacedString()));
      pSchema.registerType(false, References.ITEM_NAME, () -> DSL.constType(namespacedString()));
      pSchema.registerType(false, References.BLOCK_STATE, DSL::remainder);
      pSchema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
      Supplier<TypeTemplate> $$3 = () -> DSL.compoundList(References.ITEM_NAME.in(pSchema), DSL.constType(DSL.intType()));
      pSchema.registerType(
         false,
         References.STATS,
         () -> DSL.optionalFields(
               "stats",
               DSL.optionalFields(
                  new Pair[]{
                     Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                     Pair.of("minecraft:crafted", $$3.get()),
                     Pair.of("minecraft:used", $$3.get()),
                     Pair.of("minecraft:broken", $$3.get()),
                     Pair.of("minecraft:picked_up", $$3.get()),
                     Pair.of("minecraft:dropped", $$3.get()),
                     Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                     Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.intType()))),
                     Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
                  }
               )
            )
      );
      pSchema.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
      pSchema.registerType(false, References.SAVED_DATA_FORCED_CHUNKS, DSL::remainder);
      pSchema.registerType(false, References.SAVED_DATA_MAP_DATA, DSL::remainder);
      pSchema.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
      pSchema.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
      pSchema.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
      pSchema.registerType(
         false,
         References.SAVED_DATA_SCOREBOARD,
         () -> DSL.optionalFields(
               "data", DSL.optionalFields("Objectives", DSL.list(References.OBJECTIVE.in(pSchema)), "Teams", DSL.list(References.TEAM.in(pSchema)))
            )
      );
      pSchema.registerType(
         false,
         References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
         () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(pSchema))))
      );
      pSchema.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
      Map<String, Supplier<TypeTemplate>> $$4 = V1451_6.createCriterionTypes(pSchema);
      pSchema.registerType(
         false,
         References.OBJECTIVE,
         () -> DSL.hook(
               DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), $$4)), V1451_6.UNPACK_OBJECTIVE_ID, V1451_6.REPACK_OBJECTIVE_ID
            )
      );
      pSchema.registerType(false, References.TEAM, DSL::remainder);
      pSchema.registerType(
         true,
         References.UNTAGGED_SPAWNER,
         () -> DSL.optionalFields(
               "SpawnPotentials", DSL.list(DSL.fields("Entity", References.ENTITY_TREE.in(pSchema))), "SpawnData", References.ENTITY_TREE.in(pSchema)
            )
      );
      pSchema.registerType(
         false,
         References.ADVANCEMENTS,
         () -> DSL.optionalFields(
               "minecraft:adventure/adventuring_time",
               DSL.optionalFields("criteria", DSL.compoundList(References.BIOME.in(pSchema), DSL.constType(DSL.string()))),
               "minecraft:adventure/kill_a_mob",
               DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string()))),
               "minecraft:adventure/kill_all_mobs",
               DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string()))),
               "minecraft:husbandry/bred_all_animals",
               DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(pSchema), DSL.constType(DSL.string())))
            )
      );
      pSchema.registerType(false, References.BIOME, () -> DSL.constType(namespacedString()));
      pSchema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(namespacedString()));
      pSchema.registerType(false, References.POI_CHUNK, DSL::remainder);
      pSchema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
      pSchema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(pSchema))));
      pSchema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
      pSchema.registerType(
         true,
         References.VILLAGER_TRADE,
         () -> DSL.optionalFields(
               "buy", References.ITEM_STACK.in(pSchema), "buyB", References.ITEM_STACK.in(pSchema), "sell", References.ITEM_STACK.in(pSchema)
            )
      );
      pSchema.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
   }
}
