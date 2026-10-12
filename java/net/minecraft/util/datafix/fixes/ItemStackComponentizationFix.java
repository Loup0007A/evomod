package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.ComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackComponentizationFix extends DataFix {
   private static final int HIDE_ENCHANTMENTS = 1;
   private static final int HIDE_MODIFIERS = 2;
   private static final int HIDE_UNBREAKABLE = 4;
   private static final int HIDE_CAN_DESTROY = 8;
   private static final int HIDE_CAN_PLACE = 16;
   private static final int HIDE_ADDITIONAL = 32;
   private static final int HIDE_DYE = 64;
   private static final int HIDE_UPGRADES = 128;
   private static final Set<String> POTION_HOLDER_IDS = Set.of(
      "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
   );
   private static final Set<String> BUCKETED_MOB_IDS = Set.of(
      "minecraft:pufferfish_bucket",
      "minecraft:salmon_bucket",
      "minecraft:cod_bucket",
      "minecraft:tropical_fish_bucket",
      "minecraft:axolotl_bucket",
      "minecraft:tadpole_bucket"
   );
   private static final List<String> BUCKETED_MOB_TAGS = List.of(
      "NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
   );
   private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
      "attached",
      "bottom",
      "conditional",
      "disarmed",
      "drag",
      "enabled",
      "extended",
      "eye",
      "falling",
      "hanging",
      "has_bottle_0",
      "has_bottle_1",
      "has_bottle_2",
      "has_record",
      "has_book",
      "inverted",
      "in_wall",
      "lit",
      "locked",
      "occupied",
      "open",
      "persistent",
      "powered",
      "short",
      "signal_fire",
      "snowy",
      "triggered",
      "unstable",
      "waterlogged",
      "berries",
      "bloom",
      "shrieking",
      "can_summon",
      "up",
      "down",
      "north",
      "east",
      "south",
      "west",
      "slot_0_occupied",
      "slot_1_occupied",
      "slot_2_occupied",
      "slot_3_occupied",
      "slot_4_occupied",
      "slot_5_occupied",
      "cracked",
      "crafting"
   );
   private static final Splitter PROPERTY_SPLITTER = Splitter.on(',');

   public ItemStackComponentizationFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   private static void fixItemStack(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      int $$2 = pItemStackData.removeTag("HideFlags").asInt(0);
      pItemStackData.moveTagToComponent("Damage", "minecraft:damage", pTag.createInt(0));
      pItemStackData.moveTagToComponent("RepairCost", "minecraft:repair_cost", pTag.createInt(0));
      pItemStackData.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
      pItemStackData.removeTag("BlockStateTag")
         .result()
         .ifPresent(p_333568_ -> pItemStackData.setComponent("minecraft:block_state", fixBlockStateTag((Dynamic<?>)p_333568_)));
      pItemStackData.moveTagToComponent("EntityTag", "minecraft:entity_data");
      pItemStackData.fixSubTag("BlockEntityTag", false, p_330163_ -> {
         String $$2x = NamespacedSchema.ensureNamespaced(p_330163_.get("id").asString(""));
         p_330163_ = fixBlockEntityTag(pItemStackData, p_330163_, $$2x);
         Dynamic<?> $$3 = p_330163_.remove("id");
         return $$3.equals(p_330163_.emptyMap()) ? $$3 : p_330163_;
      });
      pItemStackData.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
      if (pItemStackData.removeTag("Unbreakable").asBoolean(false)) {
         Dynamic<?> $$3 = pTag.emptyMap();
         if (($$2 & 4) != 0) {
            $$3 = $$3.set("show_in_tooltip", pTag.createBoolean(false));
         }

         pItemStackData.setComponent("minecraft:unbreakable", $$3);
      }

      fixEnchantments(pItemStackData, pTag, "Enchantments", "minecraft:enchantments", ($$2 & 1) != 0);
      if (pItemStackData.is("minecraft:enchanted_book")) {
         fixEnchantments(pItemStackData, pTag, "StoredEnchantments", "minecraft:stored_enchantments", ($$2 & 32) != 0);
      }

      pItemStackData.fixSubTag("display", false, p_332023_ -> fixDisplay(pItemStackData, p_332023_, $$2));
      fixAdventureModeChecks(pItemStackData, pTag, $$2);
      fixAttributeModifiers(pItemStackData, pTag, $$2);
      Optional<? extends Dynamic<?>> $$4 = pItemStackData.removeTag("Trim").result();
      if ($$4.isPresent()) {
         Dynamic<?> $$5 = (Dynamic<?>)$$4.get();
         if (($$2 & 128) != 0) {
            $$5 = $$5.set("show_in_tooltip", $$5.createBoolean(false));
         }

         pItemStackData.setComponent("minecraft:trim", $$5);
      }

      if (($$2 & 32) != 0) {
         pItemStackData.setComponent("minecraft:hide_additional_tooltip", pTag.emptyMap());
      }

      if (pItemStackData.is("minecraft:crossbow")) {
         pItemStackData.removeTag("Charged");
         pItemStackData.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", pTag.createList(Stream.empty()));
      }

      if (pItemStackData.is("minecraft:bundle")) {
         pItemStackData.moveTagToComponent("Items", "minecraft:bundle_contents", pTag.createList(Stream.empty()));
      }

      if (pItemStackData.is("minecraft:filled_map")) {
         pItemStackData.moveTagToComponent("map", "minecraft:map_id");
         Map<? extends Dynamic<?>, ? extends Dynamic<?>> $$6 = pItemStackData.removeTag("Decorations")
            .asStream()
            .map(ItemStackComponentizationFix::fixMapDecoration)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (p_328286_, p_333385_) -> p_328286_));
         if (!$$6.isEmpty()) {
            pItemStackData.setComponent("minecraft:map_decorations", pTag.createMap($$6));
         }
      }

      if (pItemStackData.is(POTION_HOLDER_IDS)) {
         fixPotionContents(pItemStackData, pTag);
      }

      if (pItemStackData.is("minecraft:writable_book")) {
         fixWritableBook(pItemStackData, pTag);
      }

      if (pItemStackData.is("minecraft:written_book")) {
         fixWrittenBook(pItemStackData, pTag);
      }

      if (pItemStackData.is("minecraft:suspicious_stew")) {
         pItemStackData.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
      }

      if (pItemStackData.is("minecraft:debug_stick")) {
         pItemStackData.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
      }

      if (pItemStackData.is(BUCKETED_MOB_IDS)) {
         fixBucketedMobData(pItemStackData, pTag);
      }

      if (pItemStackData.is("minecraft:goat_horn")) {
         pItemStackData.moveTagToComponent("instrument", "minecraft:instrument");
      }

      if (pItemStackData.is("minecraft:knowledge_book")) {
         pItemStackData.moveTagToComponent("Recipes", "minecraft:recipes");
      }

      if (pItemStackData.is("minecraft:compass")) {
         fixLodestoneTracker(pItemStackData, pTag);
      }

      if (pItemStackData.is("minecraft:firework_rocket")) {
         fixFireworkRocket(pItemStackData);
      }

      if (pItemStackData.is("minecraft:firework_star")) {
         fixFireworkStar(pItemStackData);
      }

      if (pItemStackData.is("minecraft:player_head")) {
         pItemStackData.removeTag("SkullOwner")
            .result()
            .ifPresent(p_328052_ -> pItemStackData.setComponent("minecraft:profile", fixProfile((Dynamic<?>)p_328052_)));
      }
   }

   private static Dynamic<?> fixBlockStateTag(Dynamic<?> pTag) {
      return (Dynamic<?>)DataFixUtils.orElse(pTag.asMapOpt().result().map(p_331937_ -> p_331937_.collect(Collectors.toMap(Pair::getFirst, p_330078_ -> {
            String $$1 = ((Dynamic)p_330078_.getFirst()).asString("");
            Dynamic<?> $$2 = (Dynamic<?>)p_330078_.getSecond();
            if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains($$1)) {
               Optional<Boolean> $$3 = $$2.asBoolean().result();
               if ($$3.isPresent()) {
                  return $$2.createString(String.valueOf($$3.get()));
               }
            }

            Optional<Number> $$4 = $$2.asNumber().result();
            return $$4.isPresent() ? $$2.createString($$4.get().toString()) : $$2;
         }))).map(pTag::createMap), pTag);
   }

   private static Dynamic<?> fixDisplay(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
      pItemStackData.setComponent("minecraft:custom_name", pTag.get("Name"));
      pItemStackData.setComponent("minecraft:lore", pTag.get("Lore"));
      Optional<Integer> $$3 = pTag.get("color").asNumber().result().map(Number::intValue);
      boolean $$4 = (pHideFlags & 64) != 0;
      if ($$3.isPresent() || $$4) {
         Dynamic<?> $$5 = pTag.emptyMap().set("rgb", pTag.createInt($$3.orElse(10511680)));
         if ($$4) {
            $$5 = $$5.set("show_in_tooltip", pTag.createBoolean(false));
         }

         pItemStackData.setComponent("minecraft:dyed_color", $$5);
      }

      Optional<String> $$6 = pTag.get("LocName").asString().result();
      if ($$6.isPresent()) {
         pItemStackData.setComponent("minecraft:item_name", ComponentDataFixUtils.createTranslatableComponent(pTag.getOps(), $$6.get()));
      }

      if (pItemStackData.is("minecraft:filled_map")) {
         pItemStackData.setComponent("minecraft:map_color", pTag.get("MapColor"));
         pTag = pTag.remove("MapColor");
      }

      return pTag.remove("Name").remove("Lore").remove("color").remove("LocName");
   }

   private static <T> Dynamic<T> fixBlockEntityTag(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<T> pTag, String pEntityId) {
      pItemStackData.setComponent("minecraft:lock", pTag.get("Lock"));
      pTag = pTag.remove("Lock");
      Optional<Dynamic<T>> $$3 = pTag.get("LootTable").result();
      if ($$3.isPresent()) {
         Dynamic<T> $$4 = pTag.emptyMap().set("loot_table", $$3.get());
         long $$5 = pTag.get("LootTableSeed").asLong(0L);
         if ($$5 != 0L) {
            $$4 = $$4.set("seed", pTag.createLong($$5));
         }

         pItemStackData.setComponent("minecraft:container_loot", $$4);
         pTag = pTag.remove("LootTable").remove("LootTableSeed");
      }
      return switch (pEntityId) {
         case "minecraft:skull" -> {
            pItemStackData.setComponent("minecraft:note_block_sound", pTag.get("note_block_sound"));
            yield pTag.remove("note_block_sound");
         }
         case "minecraft:decorated_pot" -> {
            pItemStackData.setComponent("minecraft:pot_decorations", pTag.get("sherds"));
            Optional<Dynamic<T>> $$6 = pTag.get("item").result();
            if ($$6.isPresent()) {
               pItemStackData.setComponent(
                  "minecraft:container", pTag.createList(Stream.of(pTag.emptyMap().set("slot", pTag.createInt(0)).set("item", $$6.get())))
               );
            }

            yield pTag.remove("sherds").remove("item");
         }
         case "minecraft:banner" -> {
            pItemStackData.setComponent("minecraft:banner_patterns", pTag.get("patterns"));
            Optional<Number> $$7 = pTag.get("Base").asNumber().result();
            if ($$7.isPresent()) {
               pItemStackData.setComponent("minecraft:base_color", pTag.createString(BannerPatternFormatFix.fixColor($$7.get().intValue())));
            }

            yield pTag.remove("patterns").remove("Base");
         }
         case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
            List<Dynamic<T>> $$8 = pTag.get("Items")
               .asList(
                  p_334204_ -> p_334204_.emptyMap()
                        .set("slot", p_334204_.createInt(p_334204_.get("Slot").asByte((byte)0) & 255))
                        .set("item", p_334204_.remove("Slot"))
               );
            if (!$$8.isEmpty()) {
               pItemStackData.setComponent("minecraft:container", pTag.createList($$8.stream()));
            }

            yield pTag.remove("Items");
         }
         case "minecraft:beehive" -> {
            pItemStackData.setComponent("minecraft:bees", pTag.get("bees"));
            yield pTag.remove("bees");
         }
         default -> pTag;
      };
   }

   private static void fixEnchantments(
      ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, String pKey, String pComponent, boolean pHideEnchantments
   ) {
      OptionalDynamic<?> $$5 = pItemStackData.removeTag(pKey);
      List<Pair<String, Integer>> $$6 = $$5.asList(Function.identity())
         .stream()
         .flatMap(p_331550_ -> parseEnchantment((Dynamic<?>)p_331550_).stream())
         .toList();
      if (!$$6.isEmpty() || pHideEnchantments) {
         Dynamic<?> $$7 = pTag.emptyMap();
         Dynamic<?> $$8 = pTag.emptyMap();

         for (Pair<String, Integer> $$9 : $$6) {
            $$8 = $$8.set((String)$$9.getFirst(), pTag.createInt((Integer)$$9.getSecond()));
         }

         $$7 = $$7.set("levels", $$8);
         if (pHideEnchantments) {
            $$7 = $$7.set("show_in_tooltip", pTag.createBoolean(false));
         }

         pItemStackData.setComponent(pComponent, $$7);
      }

      if ($$5.result().isPresent() && $$6.isEmpty()) {
         pItemStackData.setComponent("minecraft:enchantment_glint_override", pTag.createBoolean(true));
      }
   }

   private static Optional<Pair<String, Integer>> parseEnchantment(Dynamic<?> pEnchantmentTag) {
      return pEnchantmentTag.get("id")
         .asString()
         .apply2stable((p_335475_, p_333076_) -> Pair.of(p_335475_, Mth.clamp(p_333076_.intValue(), 0, 255)), pEnchantmentTag.get("lvl").asNumber())
         .result();
   }

   private static void fixAdventureModeChecks(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
      fixBlockStatePredicates(pItemStackData, pTag, "CanDestroy", "minecraft:can_break", (pHideFlags & 8) != 0);
      fixBlockStatePredicates(pItemStackData, pTag, "CanPlaceOn", "minecraft:can_place_on", (pHideFlags & 16) != 0);
   }

   private static void fixBlockStatePredicates(
      ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, String pKey, String pComponent, boolean pHide
   ) {
      Optional<? extends Dynamic<?>> $$5 = pItemStackData.removeTag(pKey).result();
      if (!$$5.isEmpty()) {
         Dynamic<?> $$6 = pTag.emptyMap()
            .set(
               "predicates",
               pTag.createList(
                  $$5.get()
                     .asStream()
                     .map(
                        p_336172_ -> (Dynamic)DataFixUtils.orElse(
                              p_336172_.asString().map(p_330317_ -> fixBlockStatePredicate((Dynamic<?>)p_336172_, p_330317_)).result(), p_336172_
                           )
                     )
               )
            );
         if (pHide) {
            $$6 = $$6.set("show_in_tooltip", pTag.createBoolean(false));
         }

         pItemStackData.setComponent(pComponent, $$6);
      }
   }

   private static Dynamic<?> fixBlockStatePredicate(Dynamic<?> pTag, String pBlockId) {
      int $$2 = pBlockId.indexOf(91);
      int $$3 = pBlockId.indexOf(123);
      int $$4 = pBlockId.length();
      if ($$2 != -1) {
         $$4 = $$2;
      }

      if ($$3 != -1) {
         $$4 = Math.min($$4, $$3);
      }

      String $$5 = pBlockId.substring(0, $$4);
      Dynamic<?> $$6 = pTag.emptyMap().set("blocks", pTag.createString($$5.trim()));
      int $$7 = pBlockId.indexOf(93);
      if ($$2 != -1 && $$7 != -1) {
         Dynamic<?> $$8 = pTag.emptyMap();

         for (String $$10 : PROPERTY_SPLITTER.split(pBlockId.substring($$2 + 1, $$7))) {
            int $$11 = $$10.indexOf(61);
            if ($$11 != -1) {
               String $$12 = $$10.substring(0, $$11).trim();
               String $$13 = $$10.substring($$11 + 1).trim();
               $$8 = $$8.set($$12, pTag.createString($$13));
            }
         }

         $$6 = $$6.set("state", $$8);
      }

      int $$14 = pBlockId.indexOf(125);
      if ($$3 != -1 && $$14 != -1) {
         $$6 = $$6.set("nbt", pTag.createString(pBlockId.substring($$3, $$14 + 1)));
      }

      return $$6;
   }

   private static void fixAttributeModifiers(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
      OptionalDynamic<?> $$3 = pItemStackData.removeTag("AttributeModifiers");
      if (!$$3.result().isEmpty()) {
         boolean $$4 = (pHideFlags & 2) != 0;
         List<? extends Dynamic<?>> $$5 = $$3.asList(ItemStackComponentizationFix::fixAttributeModifier);
         Dynamic<?> $$6 = pTag.emptyMap().set("modifiers", pTag.createList($$5.stream()));
         if ($$4) {
            $$6 = $$6.set("show_in_tooltip", pTag.createBoolean(false));
         }

         pItemStackData.setComponent("minecraft:attribute_modifiers", $$6);
      }
   }

   private static Dynamic<?> fixAttributeModifier(Dynamic<?> p_330280_) {
      Dynamic<?> $$1 = p_330280_.emptyMap()
         .set("name", p_330280_.createString(""))
         .set("amount", p_330280_.createDouble(0.0))
         .set("operation", p_330280_.createString("add_value"));
      $$1 = Dynamic.copyField(p_330280_, "AttributeName", $$1, "type");
      $$1 = Dynamic.copyField(p_330280_, "Slot", $$1, "slot");
      $$1 = Dynamic.copyField(p_330280_, "UUID", $$1, "uuid");
      $$1 = Dynamic.copyField(p_330280_, "Name", $$1, "name");
      $$1 = Dynamic.copyField(p_330280_, "Amount", $$1, "amount");
      return Dynamic.copyAndFixField(p_330280_, "Operation", $$1, "operation", p_334772_ -> {
         return p_334772_.createString(switch (p_334772_.asInt(0)) {
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
            default -> "add_value";
         });
      });
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(Dynamic<?> p_329859_) {
      Dynamic<?> $$1 = (Dynamic<?>)DataFixUtils.orElseGet(p_329859_.get("id").result(), () -> p_329859_.createString(""));
      Dynamic<?> $$2 = p_329859_.emptyMap()
         .set("type", p_329859_.createString(fixMapDecorationType(p_329859_.get("type").asInt(0))))
         .set("x", p_329859_.createDouble(p_329859_.get("x").asDouble(0.0)))
         .set("z", p_329859_.createDouble(p_329859_.get("z").asDouble(0.0)))
         .set("rotation", p_329859_.createFloat((float)p_329859_.get("rot").asDouble(0.0)));
      return Pair.of($$1, $$2);
   }

   private static String fixMapDecorationType(int pDecorationType) {
      return switch (pDecorationType) {
         case 1 -> "frame";
         case 2 -> "red_marker";
         case 3 -> "blue_marker";
         case 4 -> "target_x";
         case 5 -> "target_point";
         case 6 -> "player_off_map";
         case 7 -> "player_off_limits";
         case 8 -> "mansion";
         case 9 -> "monument";
         case 10 -> "banner_white";
         case 11 -> "banner_orange";
         case 12 -> "banner_magenta";
         case 13 -> "banner_light_blue";
         case 14 -> "banner_yellow";
         case 15 -> "banner_lime";
         case 16 -> "banner_pink";
         case 17 -> "banner_gray";
         case 18 -> "banner_light_gray";
         case 19 -> "banner_cyan";
         case 20 -> "banner_purple";
         case 21 -> "banner_blue";
         case 22 -> "banner_brown";
         case 23 -> "banner_green";
         case 24 -> "banner_red";
         case 25 -> "banner_black";
         case 26 -> "red_x";
         case 27 -> "village_desert";
         case 28 -> "village_plains";
         case 29 -> "village_savanna";
         case 30 -> "village_snowy";
         case 31 -> "village_taiga";
         case 32 -> "jungle_temple";
         case 33 -> "swamp_hut";
         default -> "player";
      };
   }

   private static void fixPotionContents(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      Dynamic<?> $$2 = pTag.emptyMap();
      Optional<String> $$3 = pItemStackData.removeTag("Potion").asString().result().filter(p_334871_ -> !p_334871_.equals("minecraft:empty"));
      if ($$3.isPresent()) {
         $$2 = $$2.set("potion", pTag.createString($$3.get()));
      }

      $$2 = pItemStackData.moveTagInto("CustomPotionColor", $$2, "custom_color");
      $$2 = pItemStackData.moveTagInto("custom_potion_effects", $$2, "custom_effects");
      if (!$$2.equals(pTag.emptyMap())) {
         pItemStackData.setComponent("minecraft:potion_contents", $$2);
      }
   }

   private static void fixWritableBook(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      Dynamic<?> $$2 = fixBookPages(pItemStackData, pTag);
      if ($$2 != null) {
         pItemStackData.setComponent("minecraft:writable_book_content", pTag.emptyMap().set("pages", $$2));
      }
   }

   private static void fixWrittenBook(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      Dynamic<?> $$2 = fixBookPages(pItemStackData, pTag);
      String $$3 = pItemStackData.removeTag("title").asString("");
      Optional<String> $$4 = pItemStackData.removeTag("filtered_title").asString().result();
      Dynamic<?> $$5 = pTag.emptyMap();
      $$5 = $$5.set("title", createFilteredText(pTag, $$3, $$4));
      $$5 = pItemStackData.moveTagInto("author", $$5, "author");
      $$5 = pItemStackData.moveTagInto("resolved", $$5, "resolved");
      $$5 = pItemStackData.moveTagInto("generation", $$5, "generation");
      if ($$2 != null) {
         $$5 = $$5.set("pages", $$2);
      }

      pItemStackData.setComponent("minecraft:written_book_content", $$5);
   }

   @Nullable
   private static Dynamic<?> fixBookPages(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      List<String> $$2 = pItemStackData.removeTag("pages").asList(p_331615_ -> p_331615_.asString(""));
      Map<String, String> $$3 = pItemStackData.removeTag("filtered_pages").asMap(p_335169_ -> p_335169_.asString("0"), p_329927_ -> p_329927_.asString(""));
      if ($$2.isEmpty()) {
         return null;
      } else {
         List<Dynamic<?>> $$4 = new ArrayList<>($$2.size());

         for (int $$5 = 0; $$5 < $$2.size(); $$5++) {
            String $$6 = $$2.get($$5);
            String $$7 = $$3.get(String.valueOf($$5));
            $$4.add(createFilteredText(pTag, $$6, Optional.ofNullable($$7)));
         }

         return pTag.createList($$4.stream());
      }
   }

   private static Dynamic<?> createFilteredText(Dynamic<?> pTag, String pUnfilteredText, Optional<String> pFilteredText) {
      Dynamic<?> $$3 = pTag.emptyMap().set("raw", pTag.createString(pUnfilteredText));
      if (pFilteredText.isPresent()) {
         $$3 = $$3.set("filtered", pTag.createString(pFilteredText.get()));
      }

      return $$3;
   }

   private static void fixBucketedMobData(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      Dynamic<?> $$2 = pTag.emptyMap();

      for (String $$3 : BUCKETED_MOB_TAGS) {
         $$2 = pItemStackData.moveTagInto($$3, $$2, $$3);
      }

      if (!$$2.equals(pTag.emptyMap())) {
         pItemStackData.setComponent("minecraft:bucket_entity_data", $$2);
      }
   }

   private static void fixLodestoneTracker(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
      Optional<? extends Dynamic<?>> $$2 = pItemStackData.removeTag("LodestonePos").result();
      Optional<? extends Dynamic<?>> $$3 = pItemStackData.removeTag("LodestoneDimension").result();
      if (!$$2.isEmpty() || !$$3.isEmpty()) {
         boolean $$4 = pItemStackData.removeTag("LodestoneTracked").asBoolean(true);
         Dynamic<?> $$5 = pTag.emptyMap();
         if ($$2.isPresent() && $$3.isPresent()) {
            $$5 = $$5.set("target", pTag.emptyMap().set("pos", $$2.get()).set("dimension", $$3.get()));
         }

         if (!$$4) {
            $$5 = $$5.set("tracked", pTag.createBoolean(false));
         }

         pItemStackData.setComponent("minecraft:lodestone_tracker", $$5);
      }
   }

   private static void fixFireworkStar(ItemStackComponentizationFix.ItemStackData pItemStackData) {
      pItemStackData.fixSubTag("Explosion", true, p_334469_ -> {
         pItemStackData.setComponent("minecraft:firework_explosion", fixFireworkExplosion(p_334469_));
         return p_334469_.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
      });
   }

   private static void fixFireworkRocket(ItemStackComponentizationFix.ItemStackData pItemStackData) {
      pItemStackData.fixSubTag(
         "Fireworks",
         true,
         p_335612_ -> {
            Stream<? extends Dynamic<?>> $$2 = p_335612_.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
            int $$3 = p_335612_.get("Flight").asInt(0);
            pItemStackData.setComponent(
               "minecraft:fireworks", p_335612_.emptyMap().set("explosions", p_335612_.createList($$2)).set("flight_duration", p_335612_.createByte((byte)$$3))
            );
            return p_335612_.remove("Explosions").remove("Flight");
         }
      );
   }

   private static Dynamic<?> fixFireworkExplosion(Dynamic<?> p_327955_) {
      p_327955_ = p_327955_.set("shape", p_327955_.createString(switch (p_327955_.get("Type").asInt(0)) {
         case 1 -> "large_ball";
         case 2 -> "star";
         case 3 -> "creeper";
         case 4 -> "burst";
         default -> "small_ball";
      })).remove("Type");
      p_327955_ = p_327955_.renameField("Colors", "colors");
      p_327955_ = p_327955_.renameField("FadeColors", "fade_colors");
      p_327955_ = p_327955_.renameField("Trail", "has_trail");
      return p_327955_.renameField("Flicker", "has_twinkle");
   }

   public static Dynamic<?> fixProfile(Dynamic<?> pTag) {
      Optional<String> $$1 = pTag.asString().result();
      if ($$1.isPresent()) {
         return isValidPlayerName($$1.get()) ? pTag.emptyMap().set("name", pTag.createString($$1.get())) : pTag.emptyMap();
      } else {
         String $$2 = pTag.get("Name").asString("");
         Optional<? extends Dynamic<?>> $$3 = pTag.get("Id").result();
         Dynamic<?> $$4 = fixProfileProperties(pTag.get("Properties"));
         Dynamic<?> $$5 = pTag.emptyMap();
         if (isValidPlayerName($$2)) {
            $$5 = $$5.set("name", pTag.createString($$2));
         }

         if ($$3.isPresent()) {
            $$5 = $$5.set("id", $$3.get());
         }

         if ($$4 != null) {
            $$5 = $$5.set("properties", $$4);
         }

         return $$5;
      }
   }

   private static boolean isValidPlayerName(String pName) {
      return pName.length() > 16 ? false : pName.chars().filter(p_331992_ -> p_331992_ <= 32 || p_331992_ >= 127).findAny().isEmpty();
   }

   @Nullable
   private static Dynamic<?> fixProfileProperties(OptionalDynamic<?> pTag) {
      Map<String, List<Pair<String, Optional<String>>>> $$1 = pTag.asMap(p_330311_ -> p_330311_.asString(""), p_331808_ -> p_331808_.asList(p_329294_ -> {
            String $$1x = p_329294_.get("Value").asString("");
            Optional<String> $$2 = p_329294_.get("Signature").asString().result();
            return Pair.of($$1x, $$2);
         }));
      return $$1.isEmpty() ? null : pTag.createList($$1.entrySet().stream().flatMap(p_334576_ -> p_334576_.getValue().stream().map(p_335577_ -> {
            Dynamic<?> $$3 = pTag.emptyMap().set("name", pTag.createString(p_334576_.getKey())).set("value", pTag.createString((String)p_335577_.getFirst()));
            Optional<String> $$4 = (Optional<String>)p_335577_.getSecond();
            return $$4.isPresent() ? $$3.set("signature", pTag.createString($$4.get())) : $$3;
         })));
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ItemStack componentization",
         this.getInputSchema().getType(References.ITEM_STACK),
         this.getOutputSchema().getType(References.ITEM_STACK),
         p_332668_ -> {
            Optional<? extends Dynamic<?>> $$1 = ItemStackComponentizationFix.ItemStackData.read(p_332668_).map(p_329220_ -> {
               fixItemStack(p_329220_, p_329220_.tag);
               return p_329220_.write();
            });
            return (Dynamic)DataFixUtils.orElse($$1, p_332668_);
         }
      );
   }

   static class ItemStackData {
      private final String item;
      private final int count;
      private Dynamic<?> components;
      private final Dynamic<?> remainder;
      Dynamic<?> tag;

      private ItemStackData(String pItem, int pCount, Dynamic<?> pNbt) {
         this.item = NamespacedSchema.ensureNamespaced(pItem);
         this.count = pCount;
         this.components = pNbt.emptyMap();
         this.tag = pNbt.get("tag").orElseEmptyMap();
         this.remainder = pNbt.remove("tag");
      }

      public static Optional<ItemStackComponentizationFix.ItemStackData> read(Dynamic<?> pTag) {
         return pTag.get("id")
            .asString()
            .apply2stable(
               (p_336038_, p_334919_) -> new ItemStackComponentizationFix.ItemStackData(p_336038_, p_334919_.intValue(), pTag.remove("id").remove("Count")),
               pTag.get("Count").asNumber()
            )
            .result();
      }

      public OptionalDynamic<?> removeTag(String pKey) {
         OptionalDynamic<?> $$1 = this.tag.get(pKey);
         this.tag = this.tag.remove(pKey);
         return $$1;
      }

      public void setComponent(String pComponent, Dynamic<?> pValue) {
         this.components = this.components.set(pComponent, pValue);
      }

      public void setComponent(String pComponent, OptionalDynamic<?> pValue) {
         pValue.result().ifPresent(p_334065_ -> this.components = this.components.set(pComponent, p_334065_));
      }

      public Dynamic<?> moveTagInto(String pOldKey, Dynamic<?> pTag, String pNewKey) {
         Optional<? extends Dynamic<?>> $$3 = this.removeTag(pOldKey).result();
         return $$3.isPresent() ? pTag.set(pNewKey, $$3.get()) : pTag;
      }

      public void moveTagToComponent(String pKey, String pComponent, Dynamic<?> pTag) {
         Optional<? extends Dynamic<?>> $$3 = this.removeTag(pKey).result();
         if ($$3.isPresent() && !$$3.get().equals(pTag)) {
            this.setComponent(pComponent, (Dynamic<?>)$$3.get());
         }
      }

      public void moveTagToComponent(String pKey, String pComponent) {
         this.removeTag(pKey).result().ifPresent(p_331999_ -> this.setComponent(pComponent, (Dynamic<?>)p_331999_));
      }

      public void fixSubTag(String pKey, boolean pSkipIfEmpty, UnaryOperator<Dynamic<?>> pFixer) {
         OptionalDynamic<?> $$3 = this.tag.get(pKey);
         if (!pSkipIfEmpty || !$$3.result().isEmpty()) {
            Dynamic<?> $$4 = $$3.orElseEmptyMap();
            $$4 = pFixer.apply($$4);
            if ($$4.equals($$4.emptyMap())) {
               this.tag = this.tag.remove(pKey);
            } else {
               this.tag = this.tag.set(pKey, $$4);
            }
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> $$0 = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
         if (!this.tag.equals(this.tag.emptyMap())) {
            this.components = this.components.set("minecraft:custom_data", this.tag);
         }

         if (!this.components.equals(this.tag.emptyMap())) {
            $$0 = $$0.set("components", this.components);
         }

         return mergeRemainder($$0, this.remainder);
      }

      private static <T> Dynamic<T> mergeRemainder(Dynamic<T> pTag, Dynamic<?> pRemainder) {
         DynamicOps<T> $$2 = pTag.getOps();
         return $$2.getMap(pTag.getValue())
            .flatMap(p_335224_ -> $$2.mergeToMap(pRemainder.convert($$2).getValue(), p_335224_))
            .map(p_334956_ -> new Dynamic($$2, p_334956_))
            .result()
            .orElse(pTag);
      }

      public boolean is(String pItem) {
         return this.item.equals(pItem);
      }

      public boolean is(Set<String> pItems) {
         return pItems.contains(this.item);
      }

      public boolean hasComponent(String pComponent) {
         return this.components.get(pComponent).result().isPresent();
      }
   }
}
