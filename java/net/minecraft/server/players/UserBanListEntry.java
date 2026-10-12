package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public class UserBanListEntry extends BanListEntry<GameProfile> {
   public UserBanListEntry(@Nullable GameProfile pUser) {
      this(pUser, null, null, null, null);
   }

   public UserBanListEntry(@Nullable GameProfile pProfile, @Nullable Date pCreated, @Nullable String pSource, @Nullable Date pExpires, @Nullable String pReason) {
      super(pProfile, pCreated, pSource, pExpires, pReason);
   }

   public UserBanListEntry(JsonObject pEntryData) {
      super(createGameProfile(pEntryData), pEntryData);
   }

   @Override
   protected void serialize(JsonObject pData) {
      if (this.getUser() != null) {
         pData.addProperty("uuid", this.getUser().getId().toString());
         pData.addProperty("name", this.getUser().getName());
         super.serialize(pData);
      }
   }

   @Override
   public Component getDisplayName() {
      GameProfile $$0 = this.getUser();
      return $$0 != null ? Component.literal($$0.getName()) : Component.translatable("commands.banlist.entry.unknown");
   }

   @Nullable
   private static GameProfile createGameProfile(JsonObject pJson) {
      if (pJson.has("uuid") && pJson.has("name")) {
         String $$1 = pJson.get("uuid").getAsString();

         UUID $$2;
         try {
            $$2 = UUID.fromString($$1);
         } catch (Throwable var4) {
            return null;
         }

         return new GameProfile($$2, pJson.get("name").getAsString());
      } else {
         return null;
      }
   }
}
