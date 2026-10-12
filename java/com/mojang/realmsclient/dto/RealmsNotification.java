package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsNotification {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String NOTIFICATION_UUID = "notificationUuid";
   private static final String DISMISSABLE = "dismissable";
   private static final String SEEN = "seen";
   private static final String TYPE = "type";
   private static final String VISIT_URL = "visitUrl";
   private static final String INFO_POPUP = "infoPopup";
   static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
   final UUID uuid;
   final boolean dismissable;
   final boolean seen;
   final String type;

   RealmsNotification(UUID pUuid, boolean pDismissable, boolean pSeen, String pType) {
      this.uuid = pUuid;
      this.dismissable = pDismissable;
      this.seen = pSeen;
      this.type = pType;
   }

   public boolean seen() {
      return this.seen;
   }

   public boolean dismissable() {
      return this.dismissable;
   }

   public UUID uuid() {
      return this.uuid;
   }

   public static List<RealmsNotification> parseList(String pJson) {
      List<RealmsNotification> $$1 = new ArrayList<>();

      try {
         for (JsonElement $$3 : JsonParser.parseString(pJson).getAsJsonObject().get("notifications").getAsJsonArray()) {
            $$1.add(parse($$3.getAsJsonObject()));
         }
      } catch (Exception var5) {
         LOGGER.error("Could not parse list of RealmsNotifications", var5);
      }

      return $$1;
   }

   private static RealmsNotification parse(JsonObject pJson) {
      UUID $$1 = JsonUtils.getUuidOr("notificationUuid", pJson, null);
      if ($$1 == null) {
         throw new IllegalStateException("Missing required property notificationUuid");
      } else {
         boolean $$2 = JsonUtils.getBooleanOr("dismissable", pJson, true);
         boolean $$3 = JsonUtils.getBooleanOr("seen", pJson, false);
         String $$4 = JsonUtils.getRequiredString("type", pJson);
         RealmsNotification $$5 = new RealmsNotification($$1, $$2, $$3, $$4);

         return (RealmsNotification)(switch ($$4) {
            case "visitUrl" -> RealmsNotification.VisitUrl.parse($$5, pJson);
            case "infoPopup" -> RealmsNotification.InfoPopup.parse($$5, pJson);
            default -> $$5;
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class InfoPopup extends RealmsNotification {
      private static final String TITLE = "title";
      private static final String MESSAGE = "message";
      private static final String IMAGE = "image";
      private static final String URL_BUTTON = "urlButton";
      private final RealmsText title;
      private final RealmsText message;
      private final ResourceLocation image;
      @Nullable
      private final RealmsNotification.UrlButton urlButton;

      private InfoPopup(
         RealmsNotification pNotification, RealmsText pTitle, RealmsText pMessage, ResourceLocation pImage, @Nullable RealmsNotification.UrlButton pUrlButton
      ) {
         super(pNotification.uuid, pNotification.dismissable, pNotification.seen, pNotification.type);
         this.title = pTitle;
         this.message = pMessage;
         this.image = pImage;
         this.urlButton = pUrlButton;
      }

      public static RealmsNotification.InfoPopup parse(RealmsNotification pNotification, JsonObject pJson) {
         RealmsText $$2 = JsonUtils.getRequired("title", pJson, RealmsText::parse);
         RealmsText $$3 = JsonUtils.getRequired("message", pJson, RealmsText::parse);
         ResourceLocation $$4 = ResourceLocation.parse(JsonUtils.getRequiredString("image", pJson));
         RealmsNotification.UrlButton $$5 = JsonUtils.getOptional("urlButton", pJson, RealmsNotification.UrlButton::parse);
         return new RealmsNotification.InfoPopup(pNotification, $$2, $$3, $$4, $$5);
      }

      @Nullable
      public PopupScreen buildScreen(Screen pBackgroundScreen, Consumer<UUID> pUuidOutput) {
         Component $$2 = this.title.createComponent();
         if ($$2 == null) {
            RealmsNotification.LOGGER.warn("Realms info popup had title with no available translation: {}", this.title);
            return null;
         } else {
            PopupScreen.Builder $$3 = new PopupScreen.Builder(pBackgroundScreen, $$2)
               .setImage(this.image)
               .setMessage(this.message.createComponent(CommonComponents.EMPTY));
            if (this.urlButton != null) {
               $$3.addButton(this.urlButton.urlText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK), p_310971_ -> {
                  Minecraft $$3x = Minecraft.getInstance();
                  $$3x.setScreen(new ConfirmLinkScreen(p_311599_ -> {
                     if (p_311599_) {
                        Util.getPlatform().openUri(this.urlButton.url);
                        $$3x.setScreen(pBackgroundScreen);
                     } else {
                        $$3x.setScreen(p_310971_);
                     }
                  }, this.urlButton.url, true));
                  pUuidOutput.accept(this.uuid());
               });
            }

            $$3.addButton(CommonComponents.GUI_OK, p_311029_ -> {
               p_311029_.onClose();
               pUuidOutput.accept(this.uuid());
            });
            $$3.onClose(() -> pUuidOutput.accept(this.uuid()));
            return $$3.build();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record UrlButton(String url, RealmsText urlText) {
      private static final String URL = "url";
      private static final String URL_TEXT = "urlText";

      public static RealmsNotification.UrlButton parse(JsonObject pJson) {
         String $$1 = JsonUtils.getRequiredString("url", pJson);
         RealmsText $$2 = JsonUtils.getRequired("urlText", pJson, RealmsText::parse);
         return new RealmsNotification.UrlButton($$1, $$2);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class VisitUrl extends RealmsNotification {
      private static final String URL = "url";
      private static final String BUTTON_TEXT = "buttonText";
      private static final String MESSAGE = "message";
      private final String url;
      private final RealmsText buttonText;
      private final RealmsText message;

      private VisitUrl(RealmsNotification pNotification, String pUrl, RealmsText pButtonText, RealmsText pMessage) {
         super(pNotification.uuid, pNotification.dismissable, pNotification.seen, pNotification.type);
         this.url = pUrl;
         this.buttonText = pButtonText;
         this.message = pMessage;
      }

      public static RealmsNotification.VisitUrl parse(RealmsNotification pNotification, JsonObject pJson) {
         String $$2 = JsonUtils.getRequiredString("url", pJson);
         RealmsText $$3 = JsonUtils.getRequired("buttonText", pJson, RealmsText::parse);
         RealmsText $$4 = JsonUtils.getRequired("message", pJson, RealmsText::parse);
         return new RealmsNotification.VisitUrl(pNotification, $$2, $$3, $$4);
      }

      public Component getMessage() {
         return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
      }

      public Button buildOpenLinkButton(Screen pLastScreen) {
         Component $$1 = this.buttonText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK);
         return Button.builder($$1, ConfirmLinkScreen.confirmLink(pLastScreen, this.url)).build();
      }
   }
}
