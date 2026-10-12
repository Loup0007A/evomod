package net.minecraft.world.inventory;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

public class SlotRanges {
   private static final List<SlotRange> SLOTS = Util.make(new ArrayList<>(), p_329672_ -> {
      addSingleSlot(p_329672_, "contents", 0);
      addSlotRange(p_329672_, "container.", 0, 54);
      addSlotRange(p_329672_, "hotbar.", 0, 9);
      addSlotRange(p_329672_, "inventory.", 9, 27);
      addSlotRange(p_329672_, "enderchest.", 200, 27);
      addSlotRange(p_329672_, "villager.", 300, 8);
      addSlotRange(p_329672_, "horse.", 500, 15);
      int $$1 = EquipmentSlot.MAINHAND.getIndex(98);
      int $$2 = EquipmentSlot.OFFHAND.getIndex(98);
      addSingleSlot(p_329672_, "weapon", $$1);
      addSingleSlot(p_329672_, "weapon.mainhand", $$1);
      addSingleSlot(p_329672_, "weapon.offhand", $$2);
      addSlots(p_329672_, "weapon.*", $$1, $$2);
      $$1 = EquipmentSlot.HEAD.getIndex(100);
      $$2 = EquipmentSlot.CHEST.getIndex(100);
      int $$5 = EquipmentSlot.LEGS.getIndex(100);
      int $$6 = EquipmentSlot.FEET.getIndex(100);
      int $$7 = EquipmentSlot.BODY.getIndex(105);
      addSingleSlot(p_329672_, "armor.head", $$1);
      addSingleSlot(p_329672_, "armor.chest", $$2);
      addSingleSlot(p_329672_, "armor.legs", $$5);
      addSingleSlot(p_329672_, "armor.feet", $$6);
      addSingleSlot(p_329672_, "armor.body", $$7);
      addSlots(p_329672_, "armor.*", $$1, $$2, $$5, $$6, $$7);
      addSingleSlot(p_329672_, "horse.saddle", 400);
      addSingleSlot(p_329672_, "horse.chest", 499);
      addSingleSlot(p_329672_, "player.cursor", 499);
      addSlotRange(p_329672_, "player.crafting.", 500, 4);
   });
   public static final Codec<SlotRange> CODEC = StringRepresentable.fromValues(() -> SLOTS.toArray(new SlotRange[0]));
   private static final Function<String, SlotRange> NAME_LOOKUP = StringRepresentable.createNameLookup(SLOTS.toArray(new SlotRange[0]), p_331861_ -> p_331861_);

   private static SlotRange create(String pName, int pValue) {
      return SlotRange.of(pName, IntLists.singleton(pValue));
   }

   private static SlotRange create(String pName, IntList pValues) {
      return SlotRange.of(pName, IntLists.unmodifiable(pValues));
   }

   private static SlotRange create(String pName, int... pValues) {
      return SlotRange.of(pName, IntList.of(pValues));
   }

   private static void addSingleSlot(List<SlotRange> pList, String pName, int pValue) {
      pList.add(create(pName, pValue));
   }

   private static void addSlotRange(List<SlotRange> pList, String pPrefix, int pStartValue, int pSize) {
      IntList $$4 = new IntArrayList(pSize);

      for (int $$5 = 0; $$5 < pSize; $$5++) {
         int $$6 = pStartValue + $$5;
         pList.add(create(pPrefix + $$5, $$6));
         $$4.add($$6);
      }

      pList.add(create(pPrefix + "*", $$4));
   }

   private static void addSlots(List<SlotRange> pList, String pName, int... pValues) {
      pList.add(create(pName, pValues));
   }

   @Nullable
   public static SlotRange nameToIds(String pName) {
      return NAME_LOOKUP.apply(pName);
   }

   public static Stream<String> allNames() {
      return SLOTS.stream().map(StringRepresentable::getSerializedName);
   }

   public static Stream<String> singleSlotNames() {
      return SLOTS.stream().filter(p_336128_ -> p_336128_.size() == 1).map(StringRepresentable::getSerializedName);
   }
}
