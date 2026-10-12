package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldTemplate extends ValueObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String id = "";
   public String name = "";
   public String version = "";
   public String author = "";
   public String link = "";
   @Nullable
   public String image;
   public String trailer = "";
   public String recommendedPlayers = "";
   public WorldTemplate.WorldTemplateType type = WorldTemplate.WorldTemplateType.WORLD_TEMPLATE;

   public static WorldTemplate parse(JsonObject pJson) {
      WorldTemplate $$1 = new WorldTemplate();

      try {
         $$1.id = JsonUtils.getStringOr("id", pJson, "");
         $$1.name = JsonUtils.getStringOr("name", pJson, "");
         $$1.version = JsonUtils.getStringOr("version", pJson, "");
         $$1.author = JsonUtils.getStringOr("author", pJson, "");
         $$1.link = JsonUtils.getStringOr("link", pJson, "");
         $$1.image = JsonUtils.getStringOr("image", pJson, null);
         $$1.trailer = JsonUtils.getStringOr("trailer", pJson, "");
         $$1.recommendedPlayers = JsonUtils.getStringOr("recommendedPlayers", pJson, "");
         $$1.type = WorldTemplate.WorldTemplateType.valueOf(JsonUtils.getStringOr("type", pJson, WorldTemplate.WorldTemplateType.WORLD_TEMPLATE.name()));
      } catch (Exception var3) {
         LOGGER.error("Could not parse WorldTemplate: {}", var3.getMessage());
      }

      return $$1;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum WorldTemplateType {
      WORLD_TEMPLATE,
      MINIGAME,
      ADVENTUREMAP,
      EXPERIENCE,
      INSPIRATION;
   }
}
