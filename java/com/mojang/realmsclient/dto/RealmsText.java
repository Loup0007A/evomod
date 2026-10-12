package com.mojang.realmsclient.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsText {
   private static final String TRANSLATION_KEY = "translationKey";
   private static final String ARGS = "args";
   private final String translationKey;
   @Nullable
   private final String[] args;

   private RealmsText(String pTranslationKey, @Nullable String[] pArgs) {
      this.translationKey = pTranslationKey;
      this.args = pArgs;
   }

   public Component createComponent(Component pDefaultValue) {
      return Objects.requireNonNullElse(this.createComponent(), pDefaultValue);
   }

   @Nullable
   public Component createComponent() {
      if (!I18n.exists(this.translationKey)) {
         return null;
      } else {
         return this.args == null ? Component.translatable(this.translationKey) : Component.translatable(this.translationKey, this.args);
      }
   }

   public static RealmsText parse(JsonObject pJson) {
      String $$1 = JsonUtils.getRequiredString("translationKey", pJson);
      JsonElement $$2 = pJson.get("args");
      String[] $$5;
      if ($$2 != null && !$$2.isJsonNull()) {
         JsonArray $$4 = $$2.getAsJsonArray();
         $$5 = new String[$$4.size()];

         for (int $$6 = 0; $$6 < $$4.size(); $$6++) {
            $$5[$$6] = $$4.get($$6).getAsString();
         }
      } else {
         $$5 = null;
      }

      return new RealmsText($$1, $$5);
   }

   @Override
   public String toString() {
      return this.translationKey;
   }
}
