package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private final ResourceLocation id;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;

   public CustomBossEvent(ResourceLocation pId, Component pName) {
      super(pName, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.id = pId;
      this.setProgress(0.0F);
   }

   public ResourceLocation getTextId() {
      return this.id;
   }

   @Override
   public void addPlayer(ServerPlayer pPlayer) {
      super.addPlayer(pPlayer);
      this.players.add(pPlayer.getUUID());
   }

   public void addOfflinePlayer(UUID pPlayer) {
      this.players.add(pPlayer);
   }

   @Override
   public void removePlayer(ServerPlayer pPlayer) {
      super.removePlayer(pPlayer);
      this.players.remove(pPlayer.getUUID());
   }

   @Override
   public void removeAllPlayers() {
      super.removeAllPlayers();
      this.players.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMax() {
      return this.max;
   }

   public void setValue(int pValue) {
      this.value = pValue;
      this.setProgress(Mth.clamp((float)pValue / (float)this.max, 0.0F, 1.0F));
   }

   public void setMax(int pMax) {
      this.max = pMax;
      this.setProgress(Mth.clamp((float)this.value / (float)pMax, 0.0F, 1.0F));
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName())
         .withStyle(
            p_326211_ -> p_326211_.withColor(this.getColor().getFormatting())
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getTextId().toString())))
                  .withInsertion(this.getTextId().toString())
         );
   }

   public boolean setPlayers(Collection<ServerPlayer> pServerPlayerList) {
      Set<UUID> $$1 = Sets.newHashSet();
      Set<ServerPlayer> $$2 = Sets.newHashSet();

      for (UUID $$3 : this.players) {
         boolean $$4 = false;

         for (ServerPlayer $$5 : pServerPlayerList) {
            if ($$5.getUUID().equals($$3)) {
               $$4 = true;
               break;
            }
         }

         if (!$$4) {
            $$1.add($$3);
         }
      }

      for (ServerPlayer $$6 : pServerPlayerList) {
         boolean $$7 = false;

         for (UUID $$8 : this.players) {
            if ($$6.getUUID().equals($$8)) {
               $$7 = true;
               break;
            }
         }

         if (!$$7) {
            $$2.add($$6);
         }
      }

      for (UUID $$9 : $$1) {
         for (ServerPlayer $$10 : this.getPlayers()) {
            if ($$10.getUUID().equals($$9)) {
               this.removePlayer($$10);
               break;
            }
         }

         this.players.remove($$9);
      }

      for (ServerPlayer $$11 : $$2) {
         this.addPlayer($$11);
      }

      return !$$1.isEmpty() || !$$2.isEmpty();
   }

   public CompoundTag save(HolderLookup.Provider pLevelRegistry) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putString("Name", Component.Serializer.toJson(this.name, pLevelRegistry));
      $$1.putBoolean("Visible", this.isVisible());
      $$1.putInt("Value", this.value);
      $$1.putInt("Max", this.max);
      $$1.putString("Color", this.getColor().getName());
      $$1.putString("Overlay", this.getOverlay().getName());
      $$1.putBoolean("DarkenScreen", this.shouldDarkenScreen());
      $$1.putBoolean("PlayBossMusic", this.shouldPlayBossMusic());
      $$1.putBoolean("CreateWorldFog", this.shouldCreateWorldFog());
      ListTag $$2 = new ListTag();

      for (UUID $$3 : this.players) {
         $$2.add(NbtUtils.createUUID($$3));
      }

      $$1.put("Players", $$2);
      return $$1;
   }

   public static CustomBossEvent load(CompoundTag pTag, ResourceLocation pId, HolderLookup.Provider pLevelRegistry) {
      CustomBossEvent $$3 = new CustomBossEvent(pId, Component.Serializer.fromJson(pTag.getString("Name"), pLevelRegistry));
      $$3.setVisible(pTag.getBoolean("Visible"));
      $$3.setValue(pTag.getInt("Value"));
      $$3.setMax(pTag.getInt("Max"));
      $$3.setColor(BossEvent.BossBarColor.byName(pTag.getString("Color")));
      $$3.setOverlay(BossEvent.BossBarOverlay.byName(pTag.getString("Overlay")));
      $$3.setDarkenScreen(pTag.getBoolean("DarkenScreen"));
      $$3.setPlayBossMusic(pTag.getBoolean("PlayBossMusic"));
      $$3.setCreateWorldFog(pTag.getBoolean("CreateWorldFog"));

      for (Tag $$5 : pTag.getList("Players", 11)) {
         $$3.addOfflinePlayer(NbtUtils.loadUUID($$5));
      }

      return $$3;
   }

   public void onPlayerConnect(ServerPlayer pPlayer) {
      if (this.players.contains(pPlayer.getUUID())) {
         this.addPlayer(pPlayer);
      }
   }

   public void onPlayerDisconnect(ServerPlayer pPlayer) {
      super.removePlayer(pPlayer);
   }
}
