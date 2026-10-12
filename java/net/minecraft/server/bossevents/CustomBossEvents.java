package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomBossEvents {
   private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

   @Nullable
   public CustomBossEvent get(ResourceLocation pId) {
      return this.events.get(pId);
   }

   public CustomBossEvent create(ResourceLocation pId, Component pName) {
      CustomBossEvent $$2 = new CustomBossEvent(pId, pName);
      this.events.put(pId, $$2);
      return $$2;
   }

   public void remove(CustomBossEvent pBossbar) {
      this.events.remove(pBossbar.getTextId());
   }

   public Collection<ResourceLocation> getIds() {
      return this.events.keySet();
   }

   public Collection<CustomBossEvent> getEvents() {
      return this.events.values();
   }

   public CompoundTag save(HolderLookup.Provider pLevelRegistry) {
      CompoundTag $$1 = new CompoundTag();

      for (CustomBossEvent $$2 : this.events.values()) {
         $$1.put($$2.getTextId().toString(), $$2.save(pLevelRegistry));
      }

      return $$1;
   }

   public void load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
      for (String $$2 : pTag.getAllKeys()) {
         ResourceLocation $$3 = ResourceLocation.parse($$2);
         this.events.put($$3, CustomBossEvent.load(pTag.getCompound($$2), $$3, pLevelRegistry));
      }
   }

   public void onPlayerConnect(ServerPlayer pPlayer) {
      for (CustomBossEvent $$1 : this.events.values()) {
         $$1.onPlayerConnect(pPlayer);
      }
   }

   public void onPlayerDisconnect(ServerPlayer pPlayer) {
      for (CustomBossEvent $$1 : this.events.values()) {
         $$1.onPlayerDisconnect(pPlayer);
      }
   }
}
