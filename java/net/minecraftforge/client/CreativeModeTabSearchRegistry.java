package net.minecraftforge.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.CreativeModeTabRegistry;
import org.jetbrains.annotations.Nullable;

public class CreativeModeTabSearchRegistry {
   private static final Map<CreativeModeTab, SessionSearchTrees.Key> NAME_SEARCH_KEYS = new IdentityHashMap<>();
   private static final Map<CreativeModeTab, SessionSearchTrees.Key> TAG_SEARCH_KEYS = new IdentityHashMap<>();

   public static Map<CreativeModeTab, SessionSearchTrees.Key> getNameSearchKeys() {
      Map<CreativeModeTab, SessionSearchTrees.Key> nameSearchKeys = new IdentityHashMap<>();
      nameSearchKeys.put(CreativeModeTabs.searchTab(), getNameSearchKey(CreativeModeTabs.searchTab()));

      for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
         SessionSearchTrees.Key nameSearchKey = getNameSearchKey(tab);
         if (nameSearchKey != null) {
            nameSearchKeys.put(tab, nameSearchKey);
         }
      }

      return nameSearchKeys;
   }

   public static Map<CreativeModeTab, SessionSearchTrees.Key> getTagSearchKeys() {
      Map<CreativeModeTab, SessionSearchTrees.Key> tagSearchKeys = new IdentityHashMap<>();
      tagSearchKeys.put(CreativeModeTabs.searchTab(), getTagSearchKey(CreativeModeTabs.searchTab()));

      for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
         SessionSearchTrees.Key tagSearchKey = getTagSearchKey(tab);
         if (tagSearchKey != null) {
            tagSearchKeys.put(tab, tagSearchKey);
         }
      }

      return tagSearchKeys;
   }

   @Nullable
   public static SessionSearchTrees.Key getNameSearchKey(CreativeModeTab tab) {
      if (tab == CreativeModeTabs.searchTab()) {
         return SessionSearchTrees.CREATIVE_NAMES;
      } else {
         return !tab.hasSearchBar() ? null : NAME_SEARCH_KEYS.computeIfAbsent(tab, k -> new SessionSearchTrees.Key());
      }
   }

   @Nullable
   public static SessionSearchTrees.Key getTagSearchKey(CreativeModeTab tab) {
      if (tab == CreativeModeTabs.searchTab()) {
         return SessionSearchTrees.CREATIVE_TAGS;
      } else {
         return !tab.hasSearchBar() ? null : TAG_SEARCH_KEYS.computeIfAbsent(tab, k -> new SessionSearchTrees.Key());
      }
   }
}
