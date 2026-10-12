package net.minecraft.client.gui.screens.reporting;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportContextBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionLogFiller {
   private final ChatLog log;
   private final ChatReportContextBuilder contextBuilder;
   private final Predicate<LoggedChatMessage.Player> canReport;
   @Nullable
   private SignedMessageLink previousLink = null;
   private int eventId;
   private int missedCount;
   @Nullable
   private PlayerChatMessage lastMessage;

   public ChatSelectionLogFiller(ReportingContext pReportingContext, Predicate<LoggedChatMessage.Player> pCanReport) {
      this.log = pReportingContext.chatLog();
      this.contextBuilder = new ChatReportContextBuilder(pReportingContext.sender().reportLimits().leadingContextMessageCount());
      this.canReport = pCanReport;
      this.eventId = this.log.end();
   }

   public void fillNextPage(int pMaxVisibleEntries, ChatSelectionLogFiller.Output pOutput) {
      int $$2 = 0;

      while ($$2 < pMaxVisibleEntries) {
         LoggedChatEvent $$3 = this.log.lookup(this.eventId);
         if ($$3 == null) {
            break;
         }

         int $$4 = this.eventId--;
         if ($$3 instanceof LoggedChatMessage.Player $$5 && !$$5.message().equals(this.lastMessage)) {
            if (this.acceptMessage(pOutput, $$5)) {
               if (this.missedCount > 0) {
                  pOutput.acceptDivider(Component.translatable("gui.chatSelection.fold", this.missedCount));
                  this.missedCount = 0;
               }

               pOutput.acceptMessage($$4, $$5);
               $$2++;
            } else {
               this.missedCount++;
            }

            this.lastMessage = $$5.message();
         }
      }
   }

   private boolean acceptMessage(ChatSelectionLogFiller.Output pOutput, LoggedChatMessage.Player pPlayer) {
      PlayerChatMessage $$2 = pPlayer.message();
      boolean $$3 = this.contextBuilder.acceptContext($$2);
      if (this.canReport.test(pPlayer)) {
         this.contextBuilder.trackContext($$2);
         if (this.previousLink != null && !this.previousLink.isDescendantOf($$2.link())) {
            pOutput.acceptDivider(Component.translatable("gui.chatSelection.join", pPlayer.profile().getName()).withStyle(ChatFormatting.YELLOW));
         }

         this.previousLink = $$2.link();
         return true;
      } else {
         return $$3;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Output {
      void acceptMessage(int pChatId, LoggedChatMessage.Player pPlayerMessage);

      void acceptDivider(Component pText);
   }
}
