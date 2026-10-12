package com.mojang.realmsclient.util;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Date;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUtil {
   private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
   private static final int MINUTES = 60;
   private static final int HOURS = 3600;
   private static final int DAYS = 86400;

   public static Component convertToAgePresentation(long pMillis) {
      if (pMillis < 0L) {
         return RIGHT_NOW;
      } else {
         long $$1 = pMillis / 1000L;
         if ($$1 < 60L) {
            return Component.translatable("mco.time.secondsAgo", $$1);
         } else if ($$1 < 3600L) {
            long $$2 = $$1 / 60L;
            return Component.translatable("mco.time.minutesAgo", $$2);
         } else if ($$1 < 86400L) {
            long $$3 = $$1 / 3600L;
            return Component.translatable("mco.time.hoursAgo", $$3);
         } else {
            long $$4 = $$1 / 86400L;
            return Component.translatable("mco.time.daysAgo", $$4);
         }
      }
   }

   public static Component convertToAgePresentationFromInstant(Date pDate) {
      return convertToAgePresentation(System.currentTimeMillis() - pDate.getTime());
   }

   public static void renderPlayerFace(GuiGraphics pGuiGraphics, int pX, int pY, int pSize, UUID pPlayerUuid) {
      Minecraft $$5 = Minecraft.getInstance();
      ProfileResult $$6 = $$5.getMinecraftSessionService().fetchProfile(pPlayerUuid, false);
      PlayerSkin $$7 = $$6 != null ? $$5.getSkinManager().getInsecureSkin($$6.profile()) : DefaultPlayerSkin.get(pPlayerUuid);
      PlayerFaceRenderer.draw(pGuiGraphics, $$7.texture(), pX, pY, pSize);
   }
}
