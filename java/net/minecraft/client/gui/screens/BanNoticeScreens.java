package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class BanNoticeScreens {
   private static final Component TEMPORARY_BAN_TITLE = Component.translatable("gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
   private static final Component PERMANENT_BAN_TITLE = Component.translatable("gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);
   public static final Component NAME_BAN_TITLE = Component.translatable("gui.banned.name.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_TITLE = Component.translatable("gui.banned.skin.title").withStyle(ChatFormatting.BOLD);
   private static final Component SKIN_BAN_DESCRIPTION = Component.translatable(
      "gui.banned.skin.description", Component.translationArg(CommonLinks.SUSPENSION_HELP)
   );

   public static ConfirmLinkScreen create(BooleanConsumer pCallback, BanDetails pBanDetails) {
      return new ConfirmLinkScreen(
         pCallback, getBannedTitle(pBanDetails), getBannedScreenText(pBanDetails), CommonLinks.SUSPENSION_HELP, CommonComponents.GUI_ACKNOWLEDGE, true
      );
   }

   public static ConfirmLinkScreen createSkinBan(Runnable pCallback) {
      URI $$1 = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(p_340785_ -> {
         if (p_340785_) {
            Util.getPlatform().openUri($$1);
         }

         pCallback.run();
      }, SKIN_BAN_TITLE, SKIN_BAN_DESCRIPTION, $$1, CommonComponents.GUI_ACKNOWLEDGE, true);
   }

   public static ConfirmLinkScreen createNameBan(String pUsername, Runnable pCallback) {
      URI $$2 = CommonLinks.SUSPENSION_HELP;
      return new ConfirmLinkScreen(
         p_340782_ -> {
            if (p_340782_) {
               Util.getPlatform().openUri($$2);
            }

            pCallback.run();
         },
         NAME_BAN_TITLE,
         Component.translatable(
            "gui.banned.name.description", Component.literal(pUsername).withStyle(ChatFormatting.YELLOW), Component.translationArg(CommonLinks.SUSPENSION_HELP)
         ),
         $$2,
         CommonComponents.GUI_ACKNOWLEDGE,
         true
      );
   }

   private static Component getBannedTitle(BanDetails pBanDetails) {
      return isTemporaryBan(pBanDetails) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
   }

   private static Component getBannedScreenText(BanDetails pBanDetails) {
      return Component.translatable(
         "gui.banned.description", getBanReasonText(pBanDetails), getBanStatusText(pBanDetails), Component.translationArg(CommonLinks.SUSPENSION_HELP)
      );
   }

   private static Component getBanReasonText(BanDetails pBanDetails) {
      String $$1 = pBanDetails.reason();
      String $$2 = pBanDetails.reasonMessage();
      if (StringUtils.isNumeric($$1)) {
         int $$3 = Integer.parseInt($$1);
         BanReason $$4 = BanReason.byId($$3);
         Component $$5;
         if ($$4 != null) {
            $$5 = ComponentUtils.mergeStyles($$4.title().copy(), Style.EMPTY.withBold(true));
         } else if ($$2 != null) {
            $$5 = Component.translatable("gui.banned.description.reason_id_message", $$3, $$2).withStyle(ChatFormatting.BOLD);
         } else {
            $$5 = Component.translatable("gui.banned.description.reason_id", $$3).withStyle(ChatFormatting.BOLD);
         }

         return Component.translatable("gui.banned.description.reason", $$5);
      } else {
         return Component.translatable("gui.banned.description.unknownreason");
      }
   }

   private static Component getBanStatusText(BanDetails pBanDetails) {
      if (isTemporaryBan(pBanDetails)) {
         Component $$1 = getBanDurationText(pBanDetails);
         return Component.translatable(
            "gui.banned.description.temporary", Component.translatable("gui.banned.description.temporary.duration", $$1).withStyle(ChatFormatting.BOLD)
         );
      } else {
         return Component.translatable("gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
      }
   }

   private static Component getBanDurationText(BanDetails pBanDetails) {
      Duration $$1 = Duration.between(Instant.now(), pBanDetails.expires());
      long $$2 = $$1.toHours();
      if ($$2 > 72L) {
         return CommonComponents.days($$1.toDays());
      } else {
         return $$2 < 1L ? CommonComponents.minutes($$1.toMinutes()) : CommonComponents.hours($$1.toHours());
      }
   }

   private static boolean isTemporaryBan(BanDetails pBanDetails) {
      return pBanDetails.expires() != null;
   }
}
