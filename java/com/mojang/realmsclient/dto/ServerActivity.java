package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerActivity extends ValueObject {
   public String profileUuid;
   public long joinTime;
   public long leaveTime;

   public static ServerActivity parse(JsonObject pJson) {
      ServerActivity $$1 = new ServerActivity();

      try {
         $$1.profileUuid = JsonUtils.getStringOr("profileUuid", pJson, null);
         $$1.joinTime = JsonUtils.getLongOr("joinTime", pJson, Long.MIN_VALUE);
         $$1.leaveTime = JsonUtils.getLongOr("leaveTime", pJson, Long.MIN_VALUE);
      } catch (Exception var3) {
      }

      return $$1;
   }
}
