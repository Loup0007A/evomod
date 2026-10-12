package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V100 extends Schema {
   public V100(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   protected static TypeTemplate equipment(Schema pSchema) {
      return DSL.optionalFields(
         "ArmorItems",
         DSL.list(References.ITEM_STACK.in(pSchema)),
         "HandItems",
         DSL.list(References.ITEM_STACK.in(pSchema)),
         "body_armor_item",
         References.ITEM_STACK.in(pSchema)
      );
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> equipment(pSchema));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> $$1 = super.registerEntities(pSchema);
      registerMob(pSchema, $$1, "ArmorStand");
      registerMob(pSchema, $$1, "Creeper");
      registerMob(pSchema, $$1, "Skeleton");
      registerMob(pSchema, $$1, "Spider");
      registerMob(pSchema, $$1, "Giant");
      registerMob(pSchema, $$1, "Zombie");
      registerMob(pSchema, $$1, "Slime");
      registerMob(pSchema, $$1, "Ghast");
      registerMob(pSchema, $$1, "PigZombie");
      pSchema.register($$1, "Enderman", p_17348_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(pSchema), equipment(pSchema)));
      registerMob(pSchema, $$1, "CaveSpider");
      registerMob(pSchema, $$1, "Silverfish");
      registerMob(pSchema, $$1, "Blaze");
      registerMob(pSchema, $$1, "LavaSlime");
      registerMob(pSchema, $$1, "EnderDragon");
      registerMob(pSchema, $$1, "WitherBoss");
      registerMob(pSchema, $$1, "Bat");
      registerMob(pSchema, $$1, "Witch");
      registerMob(pSchema, $$1, "Endermite");
      registerMob(pSchema, $$1, "Guardian");
      registerMob(pSchema, $$1, "Pig");
      registerMob(pSchema, $$1, "Sheep");
      registerMob(pSchema, $$1, "Cow");
      registerMob(pSchema, $$1, "Chicken");
      registerMob(pSchema, $$1, "Squid");
      registerMob(pSchema, $$1, "Wolf");
      registerMob(pSchema, $$1, "MushroomCow");
      registerMob(pSchema, $$1, "SnowMan");
      registerMob(pSchema, $$1, "Ozelot");
      registerMob(pSchema, $$1, "VillagerGolem");
      pSchema.register(
         $$1,
         "EntityHorse",
         p_17343_ -> DSL.optionalFields(
               "Items",
               DSL.list(References.ITEM_STACK.in(pSchema)),
               "ArmorItem",
               References.ITEM_STACK.in(pSchema),
               "SaddleItem",
               References.ITEM_STACK.in(pSchema),
               equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "Rabbit");
      pSchema.register(
         $$1,
         "Villager",
         p_326678_ -> DSL.optionalFields(
               "Inventory",
               DSL.list(References.ITEM_STACK.in(pSchema)),
               "Offers",
               DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))),
               equipment(pSchema)
            )
      );
      registerMob(pSchema, $$1, "Shulker");
      pSchema.register($$1, "AreaEffectCloud", p_326680_ -> DSL.optionalFields("Particle", References.PARTICLE.in(pSchema)));
      pSchema.registerSimple($$1, "ShulkerBullet");
      return $$1;
   }

   public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
      super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
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
      pSchema.registerType(false, References.BLOCK_STATE, DSL::remainder);
      pSchema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
   }
}
