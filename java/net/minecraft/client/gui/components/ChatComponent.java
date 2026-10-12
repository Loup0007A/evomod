package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private static final int MESSAGE_NOT_FOUND = -1;
   private static final int MESSAGE_INDENT = 4;
   private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
   private static final int BOTTOM_MARGIN = 40;
   private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
   private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   private final Minecraft minecraft;
   private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
   private final List<GuiMessage> allMessages = Lists.newArrayList();
   private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

   public ChatComponent(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
      this.recentChat.addAll(pMinecraft.commandHistory().history());
   }

   public void tick() {
      if (!this.messageDeletionQueue.isEmpty()) {
         this.processMessageDeletionQueue();
      }
   }

   public void render(GuiGraphics pGuiGraphics, int pTickCount, int pMouseX, int pMouseY, boolean pFocused) {
      if (!this.isChatHidden()) {
         int $$5 = this.getLinesPerPage();
         int $$6 = this.trimmedMessages.size();
         if ($$6 > 0) {
            this.minecraft.getProfiler().push("chat");
            float $$7 = (float)this.getScale();
            int $$8 = Mth.ceil((float)this.getWidth() / $$7);
            int $$9 = pGuiGraphics.guiHeight();
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().scale($$7, $$7, 1.0F);
            pGuiGraphics.pose().translate(4.0F, 0.0F, 0.0F);
            int $$10 = Mth.floor((float)($$9 - 40) / $$7);
            int $$11 = this.getMessageEndIndexAt(this.screenToChatX((double)pMouseX), this.screenToChatY((double)pMouseY));
            double $$12 = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
            double $$13 = this.minecraft.options.textBackgroundOpacity().get();
            double $$14 = this.minecraft.options.chatLineSpacing().get();
            int $$15 = this.getLineHeight();
            int $$16 = (int)Math.round(-8.0 * ($$14 + 1.0) + 4.0 * $$14);
            int $$17 = 0;

            for (int $$18 = 0; $$18 + this.chatScrollbarPos < this.trimmedMessages.size() && $$18 < $$5; $$18++) {
               int $$19 = $$18 + this.chatScrollbarPos;
               GuiMessage.Line $$20 = this.trimmedMessages.get($$19);
               if ($$20 != null) {
                  int $$21 = pTickCount - $$20.addedTime();
                  if ($$21 < 200 || pFocused) {
                     double $$22 = pFocused ? 1.0 : getTimeFactor($$21);
                     int $$23 = (int)(255.0 * $$22 * $$12);
                     int $$24 = (int)(255.0 * $$22 * $$13);
                     $$17++;
                     if ($$23 > 3) {
                        int $$25 = 0;
                        int $$26 = $$10 - $$18 * $$15;
                        int $$27 = $$26 + $$16;
                        pGuiGraphics.fill(-4, $$26 - $$15, 0 + $$8 + 4 + 4, $$26, $$24 << 24);
                        GuiMessageTag $$28 = $$20.tag();
                        if ($$28 != null) {
                           int $$29 = $$28.indicatorColor() | $$23 << 24;
                           pGuiGraphics.fill(-4, $$26 - $$15, -2, $$26, $$29);
                           if ($$19 == $$11 && $$28.icon() != null) {
                              int $$30 = this.getTagIconLeft($$20);
                              int $$31 = $$27 + 9;
                              this.drawTagIcon(pGuiGraphics, $$30, $$31, $$28.icon());
                           }
                        }

                        pGuiGraphics.pose().pushPose();
                        pGuiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                        pGuiGraphics.drawString(this.minecraft.font, $$20.content(), 0, $$27, 16777215 + ($$23 << 24));
                        pGuiGraphics.pose().popPose();
                     }
                  }
               }
            }

            long $$32 = this.minecraft.getChatListener().queueSize();
            if ($$32 > 0L) {
               int $$33 = (int)(128.0 * $$12);
               int $$34 = (int)(255.0 * $$13);
               pGuiGraphics.pose().pushPose();
               pGuiGraphics.pose().translate(0.0F, (float)$$10, 0.0F);
               pGuiGraphics.fill(-2, 0, $$8 + 4, 9, $$34 << 24);
               pGuiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
               pGuiGraphics.drawString(this.minecraft.font, Component.translatable("chat.queue", $$32), 0, 1, 16777215 + ($$33 << 24));
               pGuiGraphics.pose().popPose();
            }

            if (pFocused) {
               int $$35 = this.getLineHeight();
               int $$36 = $$6 * $$35;
               int $$37 = $$17 * $$35;
               int $$38 = this.chatScrollbarPos * $$37 / $$6 - $$10;
               int $$39 = $$37 * $$37 / $$36;
               if ($$36 != $$37) {
                  int $$40 = $$38 > 0 ? 170 : 96;
                  int $$41 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int $$42 = $$8 + 4;
                  pGuiGraphics.fill($$42, -$$38, $$42 + 2, -$$38 - $$39, 100, $$41 + ($$40 << 24));
                  pGuiGraphics.fill($$42 + 2, -$$38, $$42 + 1, -$$38 - $$39, 100, 13421772 + ($$40 << 24));
               }
            }

            pGuiGraphics.pose().popPose();
            this.minecraft.getProfiler().pop();
         }
      }
   }

   private void drawTagIcon(GuiGraphics pGuiGraphics, int pLeft, int pBottom, GuiMessageTag.Icon pTagIcon) {
      int $$4 = pBottom - pTagIcon.height - 1;
      pTagIcon.draw(pGuiGraphics, pLeft, $$4);
   }

   private int getTagIconLeft(GuiMessage.Line pLine) {
      return this.minecraft.font.width(pLine.content()) + 4;
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   private static double getTimeFactor(int pCounter) {
      double $$1 = (double)pCounter / 200.0;
      $$1 = 1.0 - $$1;
      $$1 *= 10.0;
      $$1 = Mth.clamp($$1, 0.0, 1.0);
      return $$1 * $$1;
   }

   public void clearMessages(boolean pClearSentMsgHistory) {
      this.minecraft.getChatListener().clearQueue();
      this.messageDeletionQueue.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (pClearSentMsgHistory) {
         this.recentChat.clear();
         this.recentChat.addAll(this.minecraft.commandHistory().history());
      }
   }

   public void addMessage(Component pChatComponent) {
      this.addMessage(pChatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
   }

   public void addMessage(Component pChatComponent, @Nullable MessageSignature pHeaderSignature, @Nullable GuiMessageTag pTag) {
      GuiMessage $$3 = new GuiMessage(this.minecraft.gui.getGuiTicks(), pChatComponent, pHeaderSignature, pTag);
      this.logChatMessage($$3);
      this.addMessageToDisplayQueue($$3);
      this.addMessageToQueue($$3);
   }

   private void logChatMessage(GuiMessage pMessage) {
      String $$1 = pMessage.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String $$2 = Optionull.map(pMessage.tag(), GuiMessageTag::logTag);
      if ($$2 != null) {
         LOGGER.info("[{}] [CHAT] {}", $$2, $$1);
      } else {
         LOGGER.info("[CHAT] {}", $$1);
      }
   }

   private void addMessageToDisplayQueue(GuiMessage pMessage) {
      int $$1 = Mth.floor((double)this.getWidth() / this.getScale());
      GuiMessageTag.Icon $$2 = pMessage.icon();
      if ($$2 != null) {
         $$1 -= $$2.width + 4 + 2;
      }

      List<FormattedCharSequence> $$3 = ComponentRenderUtils.wrapComponents(pMessage.content(), $$1, this.minecraft.font);
      boolean $$4 = this.isChatFocused();

      for (int $$5 = 0; $$5 < $$3.size(); $$5++) {
         FormattedCharSequence $$6 = $$3.get($$5);
         if ($$4 && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean $$7 = $$5 == $$3.size() - 1;
         this.trimmedMessages.add(0, new GuiMessage.Line(pMessage.addedTime(), $$6, pMessage.tag(), $$7));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
      }
   }

   private void addMessageToQueue(GuiMessage pMessage) {
      this.allMessages.add(0, pMessage);

      while (this.allMessages.size() > 100) {
         this.allMessages.remove(this.allMessages.size() - 1);
      }
   }

   private void processMessageDeletionQueue() {
      int $$0 = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue.removeIf(p_250713_ -> $$0 >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
   }

   public void deleteMessage(MessageSignature pMessageSignature) {
      ChatComponent.DelayedMessageDeletion $$1 = this.deleteMessageOrDelay(pMessageSignature);
      if ($$1 != null) {
         this.messageDeletionQueue.add($$1);
      }
   }

   @Nullable
   private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature pMessageSignature) {
      int $$1 = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> $$2 = this.allMessages.listIterator();

      while ($$2.hasNext()) {
         GuiMessage $$3 = $$2.next();
         if (pMessageSignature.equals($$3.signature())) {
            int $$4 = $$3.addedTime() + 60;
            if ($$1 >= $$4) {
               $$2.set(this.createDeletedMarker($$3));
               this.refreshTrimmedMessages();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(pMessageSignature, $$4);
         }
      }

      return null;
   }

   private GuiMessage createDeletedMarker(GuiMessage pMessage) {
      return new GuiMessage(pMessage.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
   }

   public void rescaleChat() {
      this.resetChatScroll();
      this.refreshTrimmedMessages();
   }

   private void refreshTrimmedMessages() {
      this.trimmedMessages.clear();

      for (GuiMessage $$0 : Lists.reverse(this.allMessages)) {
         this.addMessageToDisplayQueue($$0);
      }
   }

   public ArrayListDeque<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String pMessage) {
      if (!pMessage.equals(this.recentChat.peekLast())) {
         if (this.recentChat.size() >= 100) {
            this.recentChat.removeFirst();
         }

         this.recentChat.addLast(pMessage);
      }

      if (pMessage.startsWith("/")) {
         this.minecraft.commandHistory().addCommand(pMessage);
      }
   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int pPosInc) {
      this.chatScrollbarPos += pPosInc;
      int $$1 = this.trimmedMessages.size();
      if (this.chatScrollbarPos > $$1 - this.getLinesPerPage()) {
         this.chatScrollbarPos = $$1 - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }
   }

   public boolean handleChatQueueClicked(double pMouseX, double pMouseY) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
         ChatListener $$2 = this.minecraft.getChatListener();
         if ($$2.queueSize() == 0L) {
            return false;
         } else {
            double $$3 = pMouseX - 2.0;
            double $$4 = (double)this.minecraft.getWindow().getGuiScaledHeight() - pMouseY - 40.0;
            if ($$3 <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && $$4 < 0.0 && $$4 > (double)Mth.floor(-9.0 * this.getScale())) {
               $$2.acceptNextDelayedMessage();
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   public Style getClickedComponentStyleAt(double pMouseX, double pMouseY) {
      double $$2 = this.screenToChatX(pMouseX);
      double $$3 = this.screenToChatY(pMouseY);
      int $$4 = this.getMessageLineIndexAt($$2, $$3);
      if ($$4 >= 0 && $$4 < this.trimmedMessages.size()) {
         GuiMessage.Line $$5 = this.trimmedMessages.get($$4);
         return this.minecraft.font.getSplitter().componentStyleAtWidth($$5.content(), Mth.floor($$2));
      } else {
         return null;
      }
   }

   @Nullable
   public GuiMessageTag getMessageTagAt(double pMouseX, double pMouseY) {
      double $$2 = this.screenToChatX(pMouseX);
      double $$3 = this.screenToChatY(pMouseY);
      int $$4 = this.getMessageEndIndexAt($$2, $$3);
      if ($$4 >= 0 && $$4 < this.trimmedMessages.size()) {
         GuiMessage.Line $$5 = this.trimmedMessages.get($$4);
         GuiMessageTag $$6 = $$5.tag();
         if ($$6 != null && this.hasSelectedMessageTag($$2, $$5, $$6)) {
            return $$6;
         }
      }

      return null;
   }

   private boolean hasSelectedMessageTag(double pX, GuiMessage.Line pLine, GuiMessageTag pTag) {
      if (pX < 0.0) {
         return true;
      } else {
         GuiMessageTag.Icon $$3 = pTag.icon();
         if ($$3 == null) {
            return false;
         } else {
            int $$4 = this.getTagIconLeft(pLine);
            int $$5 = $$4 + $$3.width;
            return pX >= (double)$$4 && pX <= (double)$$5;
         }
      }
   }

   private double screenToChatX(double pX) {
      return pX / this.getScale() - 4.0;
   }

   private double screenToChatY(double pY) {
      double $$1 = (double)this.minecraft.getWindow().getGuiScaledHeight() - pY - 40.0;
      return $$1 / (this.getScale() * (double)this.getLineHeight());
   }

   private int getMessageEndIndexAt(double pMouseX, double pMouseY) {
      int $$2 = this.getMessageLineIndexAt(pMouseX, pMouseY);
      if ($$2 == -1) {
         return -1;
      } else {
         while ($$2 >= 0) {
            if (this.trimmedMessages.get($$2).endOfEntry()) {
               return $$2;
            }

            $$2--;
         }

         return $$2;
      }
   }

   private int getMessageLineIndexAt(double pMouseX, double pMouseY) {
      if (this.isChatFocused() && !this.isChatHidden()) {
         if (!(pMouseX < -4.0) && !(pMouseX > (double)Mth.floor((double)this.getWidth() / this.getScale()))) {
            int $$2 = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
            if (pMouseY >= 0.0 && pMouseY < (double)$$2) {
               int $$3 = Mth.floor(pMouseY + (double)this.chatScrollbarPos);
               if ($$3 >= 0 && $$3 < this.trimmedMessages.size()) {
                  return $$3;
               }
            }

            return -1;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public boolean isChatFocused() {
      return this.minecraft.screen instanceof ChatScreen;
   }

   public int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   public int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
   }

   public double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double pWidth) {
      int $$1 = 320;
      int $$2 = 40;
      return Mth.floor(pWidth * 280.0 + 40.0);
   }

   public static int getHeight(double pHeight) {
      int $$1 = 180;
      int $$2 = 20;
      return Mth.floor(pHeight * 160.0 + 20.0);
   }

   public static double defaultUnfocusedPct() {
      int $$0 = 180;
      int $$1 = 20;
      return 70.0 / (double)(getHeight(1.0) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / this.getLineHeight();
   }

   private int getLineHeight() {
      return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
   }

   public ChatComponent.State storeState() {
      return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
   }

   public void restoreState(ChatComponent.State pState) {
      this.recentChat.clear();
      this.recentChat.addAll(pState.history);
      this.messageDeletionQueue.clear();
      this.messageDeletionQueue.addAll(pState.delayedMessageDeletions);
      this.allMessages.clear();
      this.allMessages.addAll(pState.messages);
      this.refreshTrimmedMessages();
   }

   @OnlyIn(Dist.CLIENT)
   static record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      final List<GuiMessage> messages;
      final List<String> history;
      final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

      public State(List<GuiMessage> pMessages, List<String> pHistory, List<ChatComponent.DelayedMessageDeletion> pDelayedMessageDeletions) {
         this.messages = pMessages;
         this.history = pHistory;
         this.delayedMessageDeletions = pDelayedMessageDeletions;
      }
   }
}
