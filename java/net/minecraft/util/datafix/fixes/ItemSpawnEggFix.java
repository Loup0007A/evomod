package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemSpawnEggFix extends DataFix {
   private static final String[] ID_TO_ENTITY = (String[])DataFixUtils.make(new String[256], p_326596_ -> {
      p_326596_[1] = "Item";
      p_326596_[2] = "XPOrb";
      p_326596_[7] = "ThrownEgg";
      p_326596_[8] = "LeashKnot";
      p_326596_[9] = "Painting";
      p_326596_[10] = "Arrow";
      p_326596_[11] = "Snowball";
      p_326596_[12] = "Fireball";
      p_326596_[13] = "SmallFireball";
      p_326596_[14] = "ThrownEnderpearl";
      p_326596_[15] = "EyeOfEnderSignal";
      p_326596_[16] = "ThrownPotion";
      p_326596_[17] = "ThrownExpBottle";
      p_326596_[18] = "ItemFrame";
      p_326596_[19] = "WitherSkull";
      p_326596_[20] = "PrimedTnt";
      p_326596_[21] = "FallingSand";
      p_326596_[22] = "FireworksRocketEntity";
      p_326596_[23] = "TippedArrow";
      p_326596_[24] = "SpectralArrow";
      p_326596_[25] = "ShulkerBullet";
      p_326596_[26] = "DragonFireball";
      p_326596_[30] = "ArmorStand";
      p_326596_[41] = "Boat";
      p_326596_[42] = "MinecartRideable";
      p_326596_[43] = "MinecartChest";
      p_326596_[44] = "MinecartFurnace";
      p_326596_[45] = "MinecartTNT";
      p_326596_[46] = "MinecartHopper";
      p_326596_[47] = "MinecartSpawner";
      p_326596_[40] = "MinecartCommandBlock";
      p_326596_[50] = "Creeper";
      p_326596_[51] = "Skeleton";
      p_326596_[52] = "Spider";
      p_326596_[53] = "Giant";
      p_326596_[54] = "Zombie";
      p_326596_[55] = "Slime";
      p_326596_[56] = "Ghast";
      p_326596_[57] = "PigZombie";
      p_326596_[58] = "Enderman";
      p_326596_[59] = "CaveSpider";
      p_326596_[60] = "Silverfish";
      p_326596_[61] = "Blaze";
      p_326596_[62] = "LavaSlime";
      p_326596_[63] = "EnderDragon";
      p_326596_[64] = "WitherBoss";
      p_326596_[65] = "Bat";
      p_326596_[66] = "Witch";
      p_326596_[67] = "Endermite";
      p_326596_[68] = "Guardian";
      p_326596_[69] = "Shulker";
      p_326596_[90] = "Pig";
      p_326596_[91] = "Sheep";
      p_326596_[92] = "Cow";
      p_326596_[93] = "Chicken";
      p_326596_[94] = "Squid";
      p_326596_[95] = "Wolf";
      p_326596_[96] = "MushroomCow";
      p_326596_[97] = "SnowMan";
      p_326596_[98] = "Ozelot";
      p_326596_[99] = "VillagerGolem";
      p_326596_[100] = "EntityHorse";
      p_326596_[101] = "Rabbit";
      p_326596_[120] = "Villager";
      p_326596_[200] = "EnderCrystal";
   });

   public ItemSpawnEggFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      Type<?> $$1 = $$0.getType(References.ITEM_STACK);
      OpticFinder<Pair<String, String>> $$2 = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
      OpticFinder<String> $$3 = DSL.fieldFinder("id", DSL.string());
      OpticFinder<?> $$4 = $$1.findField("tag");
      OpticFinder<?> $$5 = $$4.type().findField("EntityTag");
      OpticFinder<?> $$6 = DSL.typeFinder($$0.getTypeRaw(References.ENTITY));
      Type<?> $$7 = this.getOutputSchema().getTypeRaw(References.ENTITY);
      return this.fixTypeEverywhereTyped("ItemSpawnEggFix", $$1, p_308990_ -> {
         Optional<Pair<String, String>> $$7x = p_308990_.getOptional($$2);
         if ($$7x.isPresent() && Objects.equals($$7x.get().getSecond(), "minecraft:spawn_egg")) {
            Dynamic<?> $$8 = (Dynamic<?>)p_308990_.get(DSL.remainderFinder());
            short $$9 = $$8.get("Damage").asShort((short)0);
            Optional<? extends Typed<?>> $$10 = p_308990_.getOptionalTyped($$4);
            Optional<? extends Typed<?>> $$11 = $$10.flatMap(p_145417_ -> p_145417_.getOptionalTyped($$5));
            Optional<? extends Typed<?>> $$12 = $$11.flatMap(p_145414_ -> p_145414_.getOptionalTyped($$6));
            Optional<String> $$13 = $$12.flatMap(p_145406_ -> p_145406_.getOptional($$3));
            Typed<?> $$14 = p_308990_;
            String $$15 = ID_TO_ENTITY[$$9 & 255];
            if ($$15 != null && ($$13.isEmpty() || !Objects.equals($$13.get(), $$15))) {
               Typed<?> $$16 = p_308990_.getOrCreateTyped($$4);
               Typed<?> $$17 = $$16.getOrCreateTyped($$5);
               Typed<?> $$18 = $$17.getOrCreateTyped($$6);
               Typed<?> $$20 = Util.writeAndReadTypedOrThrow($$18, $$7, p_308983_ -> p_308983_.set("id", $$8.createString($$15)));
               $$14 = p_308990_.set($$4, $$16.set($$5, $$17.set($$6, $$20)));
            }

            if ($$9 != 0) {
               $$8 = $$8.set("Damage", $$8.createShort((short)0));
               $$14 = $$14.set(DSL.remainderFinder(), $$8);
            }

            return $$14;
         } else {
            return p_308990_;
         }
      });
   }
}
