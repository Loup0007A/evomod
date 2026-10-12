package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsServer extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_VALUE = -1;
   public long id;
   public String remoteSubscriptionId;
   public String name;
   public String motd;
   public RealmsServer.State state;
   public String owner;
   public UUID ownerUUID = Util.NIL_UUID;
   public List<PlayerInfo> players;
   public Map<Integer, RealmsWorldOptions> slots;
   public boolean expired;
   public boolean expiredTrial;
   public int daysLeft;
   public RealmsServer.WorldType worldType;
   public int activeSlot;
   @Nullable
   public String minigameName;
   public int minigameId;
   public String minigameImage;
   public long parentRealmId = -1L;
   @Nullable
   public String parentWorldName;
   public String activeVersion = "";
   public RealmsServer.Compatibility compatibility = RealmsServer.Compatibility.UNVERIFIABLE;

   public String getDescription() {
      return this.motd;
   }

   public String getName() {
      return this.name;
   }

   @Nullable
   public String getMinigameName() {
      return this.minigameName;
   }

   public void setName(String pName) {
      this.name = pName;
   }

   public void setDescription(String pMotd) {
      this.motd = pMotd;
   }

   public static RealmsServer parse(JsonObject pJson) {
      RealmsServer $$1 = new RealmsServer();

      try {
         $$1.id = JsonUtils.getLongOr("id", pJson, -1L);
         $$1.remoteSubscriptionId = JsonUtils.getStringOr("remoteSubscriptionId", pJson, null);
         $$1.name = JsonUtils.getStringOr("name", pJson, null);
         $$1.motd = JsonUtils.getStringOr("motd", pJson, null);
         $$1.state = getState(JsonUtils.getStringOr("state", pJson, RealmsServer.State.CLOSED.name()));
         $$1.owner = JsonUtils.getStringOr("owner", pJson, null);
         if (pJson.get("players") != null && pJson.get("players").isJsonArray()) {
            $$1.players = parseInvited(pJson.get("players").getAsJsonArray());
            sortInvited($$1);
         } else {
            $$1.players = Lists.newArrayList();
         }

         $$1.daysLeft = JsonUtils.getIntOr("daysLeft", pJson, 0);
         $$1.expired = JsonUtils.getBooleanOr("expired", pJson, false);
         $$1.expiredTrial = JsonUtils.getBooleanOr("expiredTrial", pJson, false);
         $$1.worldType = getWorldType(JsonUtils.getStringOr("worldType", pJson, RealmsServer.WorldType.NORMAL.name()));
         $$1.ownerUUID = JsonUtils.getUuidOr("ownerUUID", pJson, Util.NIL_UUID);
         if (pJson.get("slots") != null && pJson.get("slots").isJsonArray()) {
            $$1.slots = parseSlots(pJson.get("slots").getAsJsonArray());
         } else {
            $$1.slots = createEmptySlots();
         }

         $$1.minigameName = JsonUtils.getStringOr("minigameName", pJson, null);
         $$1.activeSlot = JsonUtils.getIntOr("activeSlot", pJson, -1);
         $$1.minigameId = JsonUtils.getIntOr("minigameId", pJson, -1);
         $$1.minigameImage = JsonUtils.getStringOr("minigameImage", pJson, null);
         $$1.parentRealmId = JsonUtils.getLongOr("parentWorldId", pJson, -1L);
         $$1.parentWorldName = JsonUtils.getStringOr("parentWorldName", pJson, null);
         $$1.activeVersion = JsonUtils.getStringOr("activeVersion", pJson, "");
         $$1.compatibility = getCompatibility(JsonUtils.getStringOr("compatibility", pJson, RealmsServer.Compatibility.UNVERIFIABLE.name()));
      } catch (Exception var3) {
         LOGGER.error("Could not parse McoServer: {}", var3.getMessage());
      }

      return $$1;
   }

   private static void sortInvited(RealmsServer pRealmsServer) {
      pRealmsServer.players
         .sort(
            (p_87502_, p_87503_) -> ComparisonChain.start()
                  .compareFalseFirst(p_87503_.getAccepted(), p_87502_.getAccepted())
                  .compare(p_87502_.getName().toLowerCase(Locale.ROOT), p_87503_.getName().toLowerCase(Locale.ROOT))
                  .result()
         );
   }

   private static List<PlayerInfo> parseInvited(JsonArray pJsonArray) {
      List<PlayerInfo> $$1 = Lists.newArrayList();

      for (JsonElement $$2 : pJsonArray) {
         try {
            JsonObject $$3 = $$2.getAsJsonObject();
            PlayerInfo $$4 = new PlayerInfo();
            $$4.setName(JsonUtils.getStringOr("name", $$3, null));
            $$4.setUuid(JsonUtils.getUuidOr("uuid", $$3, Util.NIL_UUID));
            $$4.setOperator(JsonUtils.getBooleanOr("operator", $$3, false));
            $$4.setAccepted(JsonUtils.getBooleanOr("accepted", $$3, false));
            $$4.setOnline(JsonUtils.getBooleanOr("online", $$3, false));
            $$1.add($$4);
         } catch (Exception var6) {
         }
      }

      return $$1;
   }

   private static Map<Integer, RealmsWorldOptions> parseSlots(JsonArray pJsonArray) {
      Map<Integer, RealmsWorldOptions> $$1 = Maps.newHashMap();

      for (JsonElement $$2 : pJsonArray) {
         try {
            JsonObject $$3 = $$2.getAsJsonObject();
            JsonParser $$4 = new JsonParser();
            JsonElement $$5 = $$4.parse($$3.get("options").getAsString());
            RealmsWorldOptions $$6;
            if ($$5 == null) {
               $$6 = RealmsWorldOptions.createDefaults();
            } else {
               $$6 = RealmsWorldOptions.parse($$5.getAsJsonObject());
            }

            int $$8 = JsonUtils.getIntOr("slotId", $$3, -1);
            $$1.put($$8, $$6);
         } catch (Exception var9) {
         }
      }

      for (int $$9 = 1; $$9 <= 3; $$9++) {
         if (!$$1.containsKey($$9)) {
            $$1.put($$9, RealmsWorldOptions.createEmptyDefaults());
         }
      }

      return $$1;
   }

   private static Map<Integer, RealmsWorldOptions> createEmptySlots() {
      Map<Integer, RealmsWorldOptions> $$0 = Maps.newHashMap();
      $$0.put(1, RealmsWorldOptions.createEmptyDefaults());
      $$0.put(2, RealmsWorldOptions.createEmptyDefaults());
      $$0.put(3, RealmsWorldOptions.createEmptyDefaults());
      return $$0;
   }

   public static RealmsServer parse(String pJson) {
      try {
         return parse(new JsonParser().parse(pJson).getAsJsonObject());
      } catch (Exception var2) {
         LOGGER.error("Could not parse McoServer: {}", var2.getMessage());
         return new RealmsServer();
      }
   }

   private static RealmsServer.State getState(String pName) {
      try {
         return RealmsServer.State.valueOf(pName);
      } catch (Exception var2) {
         return RealmsServer.State.CLOSED;
      }
   }

   private static RealmsServer.WorldType getWorldType(String pName) {
      try {
         return RealmsServer.WorldType.valueOf(pName);
      } catch (Exception var2) {
         return RealmsServer.WorldType.NORMAL;
      }
   }

   public static RealmsServer.Compatibility getCompatibility(@Nullable String pId) {
      try {
         return RealmsServer.Compatibility.valueOf(pId);
      } catch (Exception var2) {
         return RealmsServer.Compatibility.UNVERIFIABLE;
      }
   }

   public boolean isCompatible() {
      return this.compatibility.isCompatible();
   }

   public boolean needsUpgrade() {
      return this.compatibility.needsUpgrade();
   }

   public boolean needsDowngrade() {
      return this.compatibility.needsDowngrade();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id, this.name, this.motd, this.state, this.owner, this.expired);
   }

   @Override
   public boolean equals(Object pOther) {
      if (pOther == null) {
         return false;
      } else if (pOther == this) {
         return true;
      } else if (pOther.getClass() != this.getClass()) {
         return false;
      } else {
         RealmsServer $$1 = (RealmsServer)pOther;
         return new EqualsBuilder()
            .append(this.id, $$1.id)
            .append(this.name, $$1.name)
            .append(this.motd, $$1.motd)
            .append(this.state, $$1.state)
            .append(this.owner, $$1.owner)
            .append(this.expired, $$1.expired)
            .append(this.worldType, this.worldType)
            .isEquals();
      }
   }

   public RealmsServer clone() {
      RealmsServer $$0 = new RealmsServer();
      $$0.id = this.id;
      $$0.remoteSubscriptionId = this.remoteSubscriptionId;
      $$0.name = this.name;
      $$0.motd = this.motd;
      $$0.state = this.state;
      $$0.owner = this.owner;
      $$0.players = this.players;
      $$0.slots = this.cloneSlots(this.slots);
      $$0.expired = this.expired;
      $$0.expiredTrial = this.expiredTrial;
      $$0.daysLeft = this.daysLeft;
      $$0.worldType = this.worldType;
      $$0.ownerUUID = this.ownerUUID;
      $$0.minigameName = this.minigameName;
      $$0.activeSlot = this.activeSlot;
      $$0.minigameId = this.minigameId;
      $$0.minigameImage = this.minigameImage;
      $$0.parentWorldName = this.parentWorldName;
      $$0.parentRealmId = this.parentRealmId;
      $$0.activeVersion = this.activeVersion;
      $$0.compatibility = this.compatibility;
      return $$0;
   }

   public Map<Integer, RealmsWorldOptions> cloneSlots(Map<Integer, RealmsWorldOptions> pSlots) {
      Map<Integer, RealmsWorldOptions> $$1 = Maps.newHashMap();

      for (Entry<Integer, RealmsWorldOptions> $$2 : pSlots.entrySet()) {
         $$1.put($$2.getKey(), $$2.getValue().clone());
      }

      return $$1;
   }

   public boolean isSnapshotRealm() {
      return this.parentRealmId != -1L;
   }

   public boolean isMinigameActive() {
      return this.worldType == RealmsServer.WorldType.MINIGAME;
   }

   public String getWorldName(int pSlot) {
      return this.name + " (" + this.slots.get(pSlot).getSlotName(pSlot) + ")";
   }

   public ServerData toServerData(String pIp) {
      return new ServerData(this.name, pIp, ServerData.Type.REALM);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Compatibility {
      UNVERIFIABLE,
      INCOMPATIBLE,
      RELEASE_TYPE_INCOMPATIBLE,
      NEEDS_DOWNGRADE,
      NEEDS_UPGRADE,
      COMPATIBLE;

      public boolean isCompatible() {
         return this == COMPATIBLE;
      }

      public boolean needsUpgrade() {
         return this == NEEDS_UPGRADE;
      }

      public boolean needsDowngrade() {
         return this == NEEDS_DOWNGRADE;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class McoServerComparator implements Comparator<RealmsServer> {
      private final String refOwner;

      public McoServerComparator(String pRefOwner) {
         this.refOwner = pRefOwner;
      }

      public int compare(RealmsServer pFirst, RealmsServer pSecond) {
         return ComparisonChain.start()
            .compareTrueFirst(pFirst.isSnapshotRealm(), pSecond.isSnapshotRealm())
            .compareTrueFirst(pFirst.state == RealmsServer.State.UNINITIALIZED, pSecond.state == RealmsServer.State.UNINITIALIZED)
            .compareTrueFirst(pFirst.expiredTrial, pSecond.expiredTrial)
            .compareTrueFirst(pFirst.owner.equals(this.refOwner), pSecond.owner.equals(this.refOwner))
            .compareFalseFirst(pFirst.expired, pSecond.expired)
            .compareTrueFirst(pFirst.state == RealmsServer.State.OPEN, pSecond.state == RealmsServer.State.OPEN)
            .compare(pFirst.id, pSecond.id)
            .result();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum State {
      CLOSED,
      OPEN,
      UNINITIALIZED;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum WorldType {
      NORMAL,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
