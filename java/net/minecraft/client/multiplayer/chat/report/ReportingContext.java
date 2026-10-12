package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ReportingContext {
   private static final int LOG_CAPACITY = 1024;
   private final AbuseReportSender sender;
   private final ReportEnvironment environment;
   private final ChatLog chatLog;
   @Nullable
   private Report draftReport;

   public ReportingContext(AbuseReportSender pSender, ReportEnvironment pEnviroment, ChatLog pChatLog) {
      this.sender = pSender;
      this.environment = pEnviroment;
      this.chatLog = pChatLog;
   }

   public static ReportingContext create(ReportEnvironment pEnvironment, UserApiService pUserApiService) {
      ChatLog $$2 = new ChatLog(1024);
      AbuseReportSender $$3 = AbuseReportSender.create(pEnvironment, pUserApiService);
      return new ReportingContext($$3, pEnvironment, $$2);
   }

   public void draftReportHandled(Minecraft pMinecraft, Screen pScreen, Runnable pQuitter, boolean pQuitToTitle) {
      if (this.draftReport != null) {
         Report $$4 = this.draftReport.copy();
         pMinecraft.setScreen(
            new ConfirmScreen(
               p_296240_ -> {
                  this.setReportDraft(null);
                  if (p_296240_) {
                     pMinecraft.setScreen($$4.createScreen(pScreen, this));
                  } else {
                     pQuitter.run();
                  }
               },
               Component.translatable(pQuitToTitle ? "gui.abuseReport.draft.quittotitle.title" : "gui.abuseReport.draft.title"),
               Component.translatable(pQuitToTitle ? "gui.abuseReport.draft.quittotitle.content" : "gui.abuseReport.draft.content"),
               Component.translatable("gui.abuseReport.draft.edit"),
               Component.translatable("gui.abuseReport.draft.discard")
            )
         );
      } else {
         pQuitter.run();
      }
   }

   public AbuseReportSender sender() {
      return this.sender;
   }

   public ChatLog chatLog() {
      return this.chatLog;
   }

   public boolean matches(ReportEnvironment pEnvironment) {
      return Objects.equals(this.environment, pEnvironment);
   }

   public void setReportDraft(@Nullable Report pDraftReport) {
      this.draftReport = pDraftReport;
   }

   public boolean hasDraftReport() {
      return this.draftReport != null;
   }

   public boolean hasDraftReportFor(UUID pUuid) {
      return this.hasDraftReport() && this.draftReport.isReportedPlayer(pUuid);
   }
}
