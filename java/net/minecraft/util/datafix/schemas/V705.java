package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V705 extends NamespacedSchema {
   static final Map<String, String> ITEM_TO_ENTITY = ImmutableMap.builder()
      .put("minecraft:armor_stand", "minecraft:armor_stand")
      .put("minecraft:painting", "minecraft:painting")
      .put("minecraft:armadillo_spawn_egg", "minecraft:armadillo")
      .put("minecraft:allay_spawn_egg", "minecraft:allay")
      .put("minecraft:axolotl_spawn_egg", "minecraft:axolotl")
      .put("minecraft:bat_spawn_egg", "minecraft:bat")
      .put("minecraft:bee_spawn_egg", "minecraft:bee")
      .put("minecraft:blaze_spawn_egg", "minecraft:blaze")
      .put("minecraft:bogged_spawn_egg", "minecraft:bogged")
      .put("minecraft:breeze_spawn_egg", "minecraft:breeze")
      .put("minecraft:cat_spawn_egg", "minecraft:cat")
      .put("minecraft:camel_spawn_egg", "minecraft:camel")
      .put("minecraft:cave_spider_spawn_egg", "minecraft:cave_spider")
      .put("minecraft:chicken_spawn_egg", "minecraft:chicken")
      .put("minecraft:cod_spawn_egg", "minecraft:cod")
      .put("minecraft:cow_spawn_egg", "minecraft:cow")
      .put("minecraft:creeper_spawn_egg", "minecraft:creeper")
      .put("minecraft:dolphin_spawn_egg", "minecraft:dolphin")
      .put("minecraft:donkey_spawn_egg", "minecraft:donkey")
      .put("minecraft:drowned_spawn_egg", "minecraft:drowned")
      .put("minecraft:elder_guardian_spawn_egg", "minecraft:elder_guardian")
      .put("minecraft:ender_dragon_spawn_egg", "minecraft:ender_dragon")
      .put("minecraft:enderman_spawn_egg", "minecraft:enderman")
      .put("minecraft:endermite_spawn_egg", "minecraft:endermite")
      .put("minecraft:evoker_spawn_egg", "minecraft:evoker")
      .put("minecraft:fox_spawn_egg", "minecraft:fox")
      .put("minecraft:frog_spawn_egg", "minecraft:frog")
      .put("minecraft:ghast_spawn_egg", "minecraft:ghast")
      .put("minecraft:glow_squid_spawn_egg", "minecraft:glow_squid")
      .put("minecraft:goat_spawn_egg", "minecraft:goat")
      .put("minecraft:guardian_spawn_egg", "minecraft:guardian")
      .put("minecraft:hoglin_spawn_egg", "minecraft:hoglin")
      .put("minecraft:horse_spawn_egg", "minecraft:horse")
      .put("minecraft:husk_spawn_egg", "minecraft:husk")
      .put("minecraft:iron_golem_spawn_egg", "minecraft:iron_golem")
      .put("minecraft:llama_spawn_egg", "minecraft:llama")
      .put("minecraft:magma_cube_spawn_egg", "minecraft:magma_cube")
      .put("minecraft:mooshroom_spawn_egg", "minecraft:mooshroom")
      .put("minecraft:mule_spawn_egg", "minecraft:mule")
      .put("minecraft:ocelot_spawn_egg", "minecraft:ocelot")
      .put("minecraft:panda_spawn_egg", "minecraft:panda")
      .put("minecraft:parrot_spawn_egg", "minecraft:parrot")
      .put("minecraft:phantom_spawn_egg", "minecraft:phantom")
      .put("minecraft:pig_spawn_egg", "minecraft:pig")
      .put("minecraft:piglin_spawn_egg", "minecraft:piglin")
      .put("minecraft:piglin_brute_spawn_egg", "minecraft:piglin_brute")
      .put("minecraft:pillager_spawn_egg", "minecraft:pillager")
      .put("minecraft:polar_bear_spawn_egg", "minecraft:polar_bear")
      .put("minecraft:pufferfish_spawn_egg", "minecraft:pufferfish")
      .put("minecraft:rabbit_spawn_egg", "minecraft:rabbit")
      .put("minecraft:ravager_spawn_egg", "minecraft:ravager")
      .put("minecraft:salmon_spawn_egg", "minecraft:salmon")
      .put("minecraft:sheep_spawn_egg", "minecraft:sheep")
      .put("minecraft:shulker_spawn_egg", "minecraft:shulker")
      .put("minecraft:silverfish_spawn_egg", "minecraft:silverfish")
      .put("minecraft:skeleton_spawn_egg", "minecraft:skeleton")
      .put("minecraft:skeleton_horse_spawn_egg", "minecraft:skeleton_horse")
      .put("minecraft:slime_spawn_egg", "minecraft:slime")
      .put("minecraft:sniffer_spawn_egg", "minecraft:sniffer")
      .put("minecraft:snow_golem_spawn_egg", "minecraft:snow_golem")
      .put("minecraft:spider_spawn_egg", "minecraft:spider")
      .put("minecraft:squid_spawn_egg", "minecraft:squid")
      .put("minecraft:stray_spawn_egg", "minecraft:stray")
      .put("minecraft:strider_spawn_egg", "minecraft:strider")
      .put("minecraft:tadpole_spawn_egg", "minecraft:tadpole")
      .put("minecraft:trader_llama_spawn_egg", "minecraft:trader_llama")
      .put("minecraft:tropical_fish_spawn_egg", "minecraft:tropical_fish")
      .put("minecraft:turtle_spawn_egg", "minecraft:turtle")
      .put("minecraft:vex_spawn_egg", "minecraft:vex")
      .put("minecraft:villager_spawn_egg", "minecraft:villager")
      .put("minecraft:vindicator_spawn_egg", "minecraft:vindicator")
      .put("minecraft:wandering_trader_spawn_egg", "minecraft:wandering_trader")
      .put("minecraft:warden_spawn_egg", "minecraft:warden")
      .put("minecraft:witch_spawn_egg", "minecraft:witch")
      .put("minecraft:wither_spawn_egg", "minecraft:wither")
      .put("minecraft:wither_skeleton_spawn_egg", "minecraft:wither_skeleton")
      .put("minecraft:wolf_spawn_egg", "minecraft:wolf")
      .put("minecraft:zoglin_spawn_egg", "minecraft:zoglin")
      .put("minecraft:zombie_spawn_egg", "minecraft:zombie")
      .put("minecraft:zombie_horse_spawn_egg", "minecraft:zombie_horse")
      .put("minecraft:zombie_villager_spawn_egg", "minecraft:zombie_villager")
      .put("minecraft:zombified_piglin_spawn_egg", "minecraft:zombified_piglin")
      .put("minecraft:item_frame", "minecraft:item_frame")
      .put("minecraft:boat", "minecraft:boat")
      .put("minecraft:oak_boat", "minecraft:boat")
      .put("minecraft:oak_chest_boat", "minecraft:chest_boat")
      .put("minecraft:spruce_boat", "minecraft:boat")
      .put("minecraft:spruce_chest_boat", "minecraft:chest_boat")
      .put("minecraft:birch_boat", "minecraft:boat")
      .put("minecraft:birch_chest_boat", "minecraft:chest_boat")
      .put("minecraft:jungle_boat", "minecraft:boat")
      .put("minecraft:jungle_chest_boat", "minecraft:chest_boat")
      .put("minecraft:acacia_boat", "minecraft:boat")
      .put("minecraft:acacia_chest_boat", "minecraft:chest_boat")
      .put("minecraft:cherry_boat", "minecraft:boat")
      .put("minecraft:cherry_chest_boat", "minecraft:chest_boat")
      .put("minecraft:dark_oak_boat", "minecraft:boat")
      .put("minecraft:dark_oak_chest_boat", "minecraft:chest_boat")
      .put("minecraft:mangrove_boat", "minecraft:boat")
      .put("minecraft:mangrove_chest_boat", "minecraft:chest_boat")
      .put("minecraft:bamboo_raft", "minecraft:boat")
      .put("minecraft:bamboo_chest_raft", "minecraft:chest_boat")
      .put("minecraft:minecart", "minecraft:minecart")
      .put("minecraft:chest_minecart", "minecraft:chest_minecart")
      .put("minecraft:furnace_minecart", "minecraft:furnace_minecart")
      .put("minecraft:tnt_minecart", "minecraft:tnt_minecart")
      .put("minecraft:hopper_minecart", "minecraft:hopper_minecart")
      .build();
   protected static final HookFunction ADD_NAMES = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_18167_, T p_18168_) {
         return V99.addNames(new Dynamic(p_18167_, p_18168_), V704.ITEM_TO_BLOCKENTITY, V705.ITEM_TO_ENTITY);
      }
   };

   public V705(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> V100.equipment(pSchema));
   }

   protected static void registerThrowableProjectile(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = Maps.newHashMap();
      pSchema.register($$1, "minecraft:area_effect_cloud", p_326713_ -> DSL.optionalFields("Particle", References.PARTICLE.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:armor_stand");
      pSchema.register($$1, "minecraft:arrow", p_18158_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:bat");
      registerMob(pSchema, $$1, "minecraft:blaze");
      pSchema.registerSimple($$1, "minecraft:boat");
      registerMob(pSchema, $$1, "minecraft:cave_spider");
      pSchema.register(
         $$1,
         "minecraft:chest_minecart",
         p_18161_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      registerMob(pSchema, $$1, "minecraft:chicken");
      pSchema.register($$1, "minecraft:commandblock_minecart", p_18137_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:cow");
      registerMob(pSchema, $$1, "minecraft:creeper");
      pSchema.register(
         $$1,
         "minecraft:donkey",
         p_18155_ -> DSL.optionalFields(
               "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      pSchema.registerSimple($$1, "minecraft:dragon_fireball");
      registerThrowableProjectile(pSchema, $$1, "minecraft:egg");
      registerMob(pSchema, $$1, "minecraft:elder_guardian");
      pSchema.registerSimple($$1, "minecraft:ender_crystal");
      registerMob(pSchema, $$1, "minecraft:ender_dragon");
      pSchema.register($$1, "minecraft:enderman", p_18146_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(pSchema), V100.equipment(pSchema)));
      registerMob(pSchema, $$1, "minecraft:endermite");
      registerThrowableProjectile(pSchema, $$1, "minecraft:ender_pearl");
      pSchema.registerSimple($$1, "minecraft:eye_of_ender_signal");
      pSchema.register(
         $$1,
         "minecraft:falling_block",
         p_18143_ -> DSL.optionalFields("Block", References.BLOCK_NAME.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
      );
      registerThrowableProjectile(pSchema, $$1, "minecraft:fireball");
      pSchema.register($$1, "minecraft:fireworks_rocket", p_18140_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(pSchema)));
      pSchema.register($$1, "minecraft:furnace_minecart", p_18122_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:ghast");
      registerMob(pSchema, $$1, "minecraft:giant");
      registerMob(pSchema, $$1, "minecraft:guardian");
      pSchema.register(
         $$1,
         "minecraft:hopper_minecart",
         p_18134_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
      );
      pSchema.register(
         $$1,
         "minecraft:horse",
         p_18131_ -> DSL.optionalFields(
               "ArmorItem", References.ITEM_STACK.in(pSchema), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:husk");
      pSchema.register($$1, "minecraft:item", p_18128_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
      pSchema.register($$1, "minecraft:item_frame", p_18125_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
      pSchema.registerSimple($$1, "minecraft:leash_knot");
      registerMob(pSchema, $$1, "minecraft:magma_cube");
      pSchema.register($$1, "minecraft:minecart", p_18107_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:mooshroom");
      pSchema.register(
         $$1,
         "minecraft:mule",
         p_18119_ -> DSL.optionalFields(
               "Items", DSL.list(References.ITEM_STACK.in(pSchema)), "SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:ocelot");
      pSchema.registerSimple($$1, "minecraft:painting");
      pSchema.registerSimple($$1, "minecraft:parrot");
      registerMob(pSchema, $$1, "minecraft:pig");
      registerMob(pSchema, $$1, "minecraft:polar_bear");
      pSchema.register(
         $$1, "minecraft:potion", p_18116_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(pSchema), "inTile", References.BLOCK_NAME.in(pSchema))
      );
      registerMob(pSchema, $$1, "minecraft:rabbit");
      registerMob(pSchema, $$1, "minecraft:sheep");
      registerMob(pSchema, $$1, "minecraft:shulker");
      pSchema.registerSimple($$1, "minecraft:shulker_bullet");
      registerMob(pSchema, $$1, "minecraft:silverfish");
      registerMob(pSchema, $$1, "minecraft:skeleton");
      pSchema.register(
         $$1, "minecraft:skeleton_horse", p_18113_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema))
      );
      registerMob(pSchema, $$1, "minecraft:slime");
      registerThrowableProjectile(pSchema, $$1, "minecraft:small_fireball");
      registerThrowableProjectile(pSchema, $$1, "minecraft:snowball");
      registerMob(pSchema, $$1, "minecraft:snowman");
      pSchema.register(
         $$1,
         "minecraft:spawner_minecart",
         p_18110_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
      );
      pSchema.register($$1, "minecraft:spectral_arrow", p_18164_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
      registerMob(pSchema, $$1, "minecraft:spider");
      registerMob(pSchema, $$1, "minecraft:squid");
      registerMob(pSchema, $$1, "minecraft:stray");
      pSchema.registerSimple($$1, "minecraft:tnt");
      pSchema.register($$1, "minecraft:tnt_minecart", p_18104_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
      pSchema.register(
         $$1,
         "minecraft:villager",
         p_326709_ -> DSL.optionalFields(
               "Inventory",
               DSL.list(References.ITEM_STACK.in(pSchema)),
               "Offers",
               DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))),
               V100.equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "minecraft:villager_golem");
      registerMob(pSchema, $$1, "minecraft:witch");
      registerMob(pSchema, $$1, "minecraft:wither");
      registerMob(pSchema, $$1, "minecraft:wither_skeleton");
      registerThrowableProjectile(pSchema, $$1, "minecraft:wither_skull");
      registerMob(pSchema, $$1, "minecraft:wolf");
      registerThrowableProjectile(pSchema, $$1, "minecraft:xp_bottle");
      pSchema.registerSimple($$1, "minecraft:xp_orb");
      registerMob(pSchema, $$1, "minecraft:zombie");
      pSchema.register($$1, "minecraft:zombie_horse", p_18092_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(pSchema), V100.equipment(pSchema)));
      registerMob(pSchema, $$1, "minecraft:zombie_pigman");
      pSchema.register(
         $$1,
         "minecraft:zombie_villager",
         p_326711_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))), V100.equipment(pSchema))
      );
      pSchema.registerSimple($$1, "minecraft:evocation_fangs");
      registerMob(pSchema, $$1, "minecraft:evocation_illager");
      pSchema.registerSimple($$1, "minecraft:illusion_illager");
      pSchema.register(
         $$1,
         "minecraft:llama",
         p_18081_ -> DSL.optionalFields(
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
      registerMob(pSchema, $$1, "minecraft:vex");
      registerMob(pSchema, $$1, "minecraft:vindication_illager");
      return $$1;
   }

   public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
      super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
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
               ADD_NAMES,
               HookFunction.IDENTITY
            )
      );
   }
}
