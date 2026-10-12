package net.minecraftforge.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class VillagerTradingManager {
   private static final Map<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> VANILLA_TRADES = new HashMap<>();
   private static final Int2ObjectMap<VillagerTrades.ItemListing[]> WANDERER_TRADES = new Int2ObjectOpenHashMap();

   static void loadTrades(ServerAboutToStartEvent e) {
      postWandererEvent();
      postVillagerEvents();
   }

   private static void postWandererEvent() {
      List<VillagerTrades.ItemListing> generic = NonNullList.create();
      List<VillagerTrades.ItemListing> rare = NonNullList.create();
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(1)).forEach(generic::add);
      Arrays.stream((VillagerTrades.ItemListing[])WANDERER_TRADES.get(2)).forEach(rare::add);
      MinecraftForge.EVENT_BUS.post(new WandererTradesEvent(generic, rare));
      VillagerTrades.WANDERING_TRADER_TRADES.put(1, generic.toArray(new VillagerTrades.ItemListing[0]));
      VillagerTrades.WANDERING_TRADER_TRADES.put(2, rare.toArray(new VillagerTrades.ItemListing[0]));
   }

   private static void postVillagerEvents() {
      for (VillagerProfession prof : ForgeRegistries.VILLAGER_PROFESSIONS) {
         Int2ObjectMap<VillagerTrades.ItemListing[]> trades = VANILLA_TRADES.getOrDefault(prof, new Int2ObjectOpenHashMap());
         Int2ObjectMap<List<VillagerTrades.ItemListing>> mutableTrades = new Int2ObjectOpenHashMap();

         for (int i = 1; i < 6; i++) {
            mutableTrades.put(i, NonNullList.create());
         }

         ObjectIterator var7 = trades.int2ObjectEntrySet().iterator();

         while (var7.hasNext()) {
            Entry<VillagerTrades.ItemListing[]> entry = (Entry<VillagerTrades.ItemListing[]>)var7.next();
            Arrays.stream((VillagerTrades.ItemListing[])entry.getValue()).forEach(((List)mutableTrades.get(entry.getIntKey()))::add);
         }

         MinecraftForge.EVENT_BUS.post(new VillagerTradesEvent(mutableTrades, prof));
         Int2ObjectMap<VillagerTrades.ItemListing[]> newTrades = new Int2ObjectOpenHashMap();
         ObjectIterator var9 = mutableTrades.int2ObjectEntrySet().iterator();

         while (var9.hasNext()) {
            Entry<List<VillagerTrades.ItemListing>> entry = (Entry<List<VillagerTrades.ItemListing>>)var9.next();
            newTrades.put(entry.getIntKey(), ((List)entry.getValue()).toArray(new VillagerTrades.ItemListing[0]));
         }

         VillagerTrades.TRADES.put(prof, newTrades);
      }
   }

   static {
      VillagerTrades.TRADES
         .forEach(
            (key, value) -> {
               Int2ObjectMap<VillagerTrades.ItemListing[]> copy = new Int2ObjectOpenHashMap();
               ObjectIterator var3 = value.int2ObjectEntrySet().iterator();

               while (var3.hasNext()) {
                  Entry<VillagerTrades.ItemListing[]> ent = (Entry<VillagerTrades.ItemListing[]>)var3.next();
                  copy.put(
                     ent.getIntKey(),
                     (VillagerTrades.ItemListing[])Arrays.copyOf(
                        (VillagerTrades.ItemListing[])ent.getValue(), ((VillagerTrades.ItemListing[])ent.getValue()).length
                     )
                  );
               }

               VANILLA_TRADES.put(key, copy);
            }
         );
      VillagerTrades.WANDERING_TRADER_TRADES
         .int2ObjectEntrySet()
         .forEach(
            e -> WANDERER_TRADES.put(
                  e.getIntKey(),
                  (VillagerTrades.ItemListing[])Arrays.copyOf((VillagerTrades.ItemListing[])e.getValue(), ((VillagerTrades.ItemListing[])e.getValue()).length)
               )
         );
   }
}
