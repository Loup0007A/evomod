package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
   static final ResourceLocation CHECKMARK_SPRITE = ResourceLocation.withDefaultNamespace("icon/checkmark");
   private static final Component TITLE = Component.translatable("gui.chatSelection.title");
   private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
   @Nullable
   private final Screen lastScreen;
   private final ReportingContext reportingContext;
   private Button confirmSelectedButton;
   private MultiLineLabel contextInfoLabel;
   @Nullable
   private ChatSelectionScreen.ChatSelectionList chatSelectionList;
   final ChatReport.Builder report;
   private final Consumer<ChatReport.Builder> onSelected;
   private ChatSelectionLogFiller chatLogFiller;

   public ChatSelectionScreen(
      @Nullable Screen pLastScreen, ReportingContext pReportingContext, ChatReport.Builder pReport, Consumer<ChatReport.Builder> pOnSelected
   ) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.reportingContext = pReportingContext;
      this.report = pReport.copy();
      this.onSelected = pOnSelected;
   }

   @Override
   protected void init() {
      this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
      this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
      this.chatSelectionList = this.addRenderableWidget(
         new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9)
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, p_239860_ -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build()
      );
      this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_296214_ -> {
         this.onSelected.accept(this.report);
         this.onClose();
      }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
      this.updateConfirmSelectedButton();
      this.extendLog();
      this.chatSelectionList.setScrollAmount((double)this.chatSelectionList.getMaxScroll());
   }

   private boolean canReport(LoggedChatMessage p_242240_) {
      return p_242240_.canReport(this.report.reportedProfileId());
   }

   private void extendLog() {
      int $$0 = this.chatSelectionList.getMaxVisibleEntries();
      this.chatLogFiller.fillNextPage($$0, this.chatSelectionList);
   }

   void onReachedScrollTop() {
      this.extendLog();
   }

   void updateConfirmSelectedButton() {
      this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 16777215);
      AbuseReportLimits $$4 = this.reportingContext.sender().reportLimits();
      int $$5 = this.report.reportedMessages().size();
      int $$6 = $$4.maxReportedMessageCount();
      Component $$7 = Component.translatable("gui.chatSelection.selected", $$5, $$6);
      pGuiGraphics.drawCenteredString(this.font, $$7, this.width / 2, 16 + 9 * 3 / 2, -1);
      this.contextInfoLabel.renderCentered(pGuiGraphics, this.width / 2, this.chatSelectionList.getFooterTop());
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
   }

   @OnlyIn(Dist.CLIENT)
   public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
      @Nullable
      private ChatSelectionScreen.ChatSelectionList.Heading previousHeading;

      public ChatSelectionList(final Minecraft pMinecraft, final int pHeight) {
         super(pMinecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - pHeight - 80, 40, 16);
      }

      @Override
      public void setScrollAmount(double pScroll) {
         double $$1 = this.getScrollAmount();
         super.setScrollAmount(pScroll);
         if ((float)this.getMaxScroll() > 1.0E-5F && pScroll <= 1.0E-5F && !Mth.equal(pScroll, $$1)) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public void acceptMessage(int pChatId, LoggedChatMessage.Player pPlayerMessage) {
         boolean $$2 = pPlayerMessage.canReport(ChatSelectionScreen.this.report.reportedProfileId());
         ChatTrustLevel $$3 = pPlayerMessage.trustLevel();
         GuiMessageTag $$4 = $$3.createTag(pPlayerMessage.message());
         ChatSelectionScreen.ChatSelectionList.Entry $$5 = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
            pChatId, pPlayerMessage.toContentComponent(), pPlayerMessage.toNarrationComponent(), $$4, $$2, true
         );
         this.addEntryToTop($$5);
         this.updateHeading(pPlayerMessage, $$2);
      }

      private void updateHeading(LoggedChatMessage.Player pLoggedPlayerChatMessage, boolean pCanReport) {
         ChatSelectionScreen.ChatSelectionList.Entry $$2 = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
            pLoggedPlayerChatMessage.profile(), pLoggedPlayerChatMessage.toHeadingComponent(), pCanReport
         );
         this.addEntryToTop($$2);
         ChatSelectionScreen.ChatSelectionList.Heading $$3 = new ChatSelectionScreen.ChatSelectionList.Heading(pLoggedPlayerChatMessage.profileId(), $$2);
         if (this.previousHeading != null && this.previousHeading.canCombine($$3)) {
            this.removeEntryFromTop(this.previousHeading.entry());
         }

         this.previousHeading = $$3;
      }

      @Override
      public void acceptDivider(Component pText) {
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(pText));
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.previousHeading = null;
      }

      @Override
      public int getRowWidth() {
         return Math.min(350, this.width - 50);
      }

      public int getMaxVisibleEntries() {
         return Mth.positiveCeilDiv(this.height, this.itemHeight);
      }

      @Override
      protected void renderItem(
         GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight
      ) {
         ChatSelectionScreen.ChatSelectionList.Entry $$9 = this.getEntry(pIndex);
         if (this.shouldHighlightEntry($$9)) {
            boolean $$10 = this.getSelected() == $$9;
            int $$11 = this.isFocused() && $$10 ? -1 : -8355712;
            this.renderSelection(pGuiGraphics, pTop, pWidth, pHeight, $$11, -16777216);
         }

         $$9.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, this.getHovered() == $$9, pPartialTick);
      }

      private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry pEntry) {
         if (pEntry.canSelect()) {
            boolean $$1 = this.getSelected() == pEntry;
            boolean $$2 = this.getSelected() == null;
            boolean $$3 = this.getHovered() == pEntry;
            return $$1 || $$2 && $$3 && pEntry.canReport();
         } else {
            return false;
         }
      }

      @Nullable
      protected ChatSelectionScreen.ChatSelectionList.Entry nextEntry(ScreenDirection pDirection) {
         return this.nextEntry(pDirection, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
      }

      public void setSelected(@Nullable ChatSelectionScreen.ChatSelectionList.Entry pSelected) {
         super.setSelected(pSelected);
         ChatSelectionScreen.ChatSelectionList.Entry $$1 = this.nextEntry(ScreenDirection.UP);
         if ($$1 == null) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         ChatSelectionScreen.ChatSelectionList.Entry $$3 = this.getSelected();
         return $$3 != null && $$3.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      public int getFooterTop() {
         return this.getBottom() + 9;
      }

      @OnlyIn(Dist.CLIENT)
      public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int COLOR = -6250336;
         private final Component text;

         public DividerEntry(final Component pText) {
            this.text = pText;
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            int $$10 = pTop + pHeight / 2;
            int $$11 = pLeft + pWidth - 8;
            int $$12 = ChatSelectionScreen.this.font.width(this.text);
            int $$13 = (pLeft + $$11 - $$12) / 2;
            int $$14 = $$10 - 9 / 2;
            pGuiGraphics.drawString(ChatSelectionScreen.this.font, this.text, $$13, $$14, -6250336);
         }

         @Override
         public Component getNarration() {
            return this.text;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public abstract class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
         @Override
         public Component getNarration() {
            return CommonComponents.EMPTY;
         }

         public boolean isSelected() {
            return false;
         }

         public boolean canSelect() {
            return false;
         }

         public boolean canReport() {
            return this.canSelect();
         }
      }

      @OnlyIn(Dist.CLIENT)
      static record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
         public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading pOther) {
            return pOther.sender.equals(this.sender);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int CHECKMARK_WIDTH = 9;
         private static final int CHECKMARK_HEIGHT = 8;
         private static final int INDENT_AMOUNT = 11;
         private static final int TAG_MARGIN_LEFT = 4;
         private final int chatId;
         private final FormattedText text;
         private final Component narration;
         @Nullable
         private final List<FormattedCharSequence> hoverText;
         @Nullable
         private final GuiMessageTag.Icon tagIcon;
         @Nullable
         private final List<FormattedCharSequence> tagHoverText;
         private final boolean canReport;
         private final boolean playerMessage;

         public MessageEntry(
            final int pChatId,
            final Component pText,
            final Component pNarration,
            @Nullable final GuiMessageTag pTagIcon,
            final boolean pCanReport,
            final boolean pPlayerMessage
         ) {
            this.chatId = pChatId;
            this.tagIcon = Optionull.map(pTagIcon, GuiMessageTag::icon);
            this.tagHoverText = pTagIcon != null && pTagIcon.text() != null
               ? ChatSelectionScreen.this.font.split(pTagIcon.text(), ChatSelectionList.this.getRowWidth())
               : null;
            this.canReport = pCanReport;
            this.playerMessage = pPlayerMessage;
            FormattedText $$7 = ChatSelectionScreen.this.font
               .substrByWidth(pText, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
            if (pText != $$7) {
               this.text = FormattedText.composite($$7, CommonComponents.ELLIPSIS);
               this.hoverText = ChatSelectionScreen.this.font.split(pText, ChatSelectionList.this.getRowWidth());
            } else {
               this.text = pText;
               this.hoverText = null;
            }

            this.narration = pNarration;
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            if (this.isSelected() && this.canReport) {
               this.renderSelectedCheckmark(pGuiGraphics, pTop, pLeft, pHeight);
            }

            int $$10 = pLeft + this.getTextIndent();
            int $$11 = pTop + 1 + (pHeight - 9) / 2;
            pGuiGraphics.drawString(
               ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), $$10, $$11, this.canReport ? -1 : -1593835521
            );
            if (this.hoverText != null && pHovering) {
               ChatSelectionScreen.this.setTooltipForNextRenderPass(this.hoverText);
            }

            int $$12 = ChatSelectionScreen.this.font.width(this.text);
            this.renderTag(pGuiGraphics, $$10 + $$12 + 4, pTop, pHeight, pMouseX, pMouseY);
         }

         private void renderTag(GuiGraphics pGuiGraphics, int pX, int pY, int pHeight, int pMouseX, int pMouseY) {
            if (this.tagIcon != null) {
               int $$6 = pY + (pHeight - this.tagIcon.height) / 2;
               this.tagIcon.draw(pGuiGraphics, pX, $$6);
               if (this.tagHoverText != null && pMouseX >= pX && pMouseX <= pX + this.tagIcon.width && pMouseY >= $$6 && pMouseY <= $$6 + this.tagIcon.height) {
                  ChatSelectionScreen.this.setTooltipForNextRenderPass(this.tagHoverText);
               }
            }
         }

         private void renderSelectedCheckmark(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pHeight) {
            int $$5 = pTop + (pHeight - 8) / 2;
            RenderSystem.enableBlend();
            pGuiGraphics.blitSprite(ChatSelectionScreen.CHECKMARK_SPRITE, pLeft, $$5, 9, 8);
            RenderSystem.disableBlend();
         }

         private int getMaximumTextWidth() {
            int $$0 = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
            return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - $$0;
         }

         private int getTextIndent() {
            return this.playerMessage ? 11 : 0;
         }

         @Override
         public Component getNarration() {
            return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
         }

         @Override
         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            ChatSelectionList.this.setSelected(null);
            return this.toggleReport();
         }

         @Override
         public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            return CommonInputs.selected(pKeyCode) ? this.toggleReport() : false;
         }

         @Override
         public boolean isSelected() {
            return ChatSelectionScreen.this.report.isReported(this.chatId);
         }

         @Override
         public boolean canSelect() {
            return true;
         }

         @Override
         public boolean canReport() {
            return this.canReport;
         }

         private boolean toggleReport() {
            if (this.canReport) {
               ChatSelectionScreen.this.report.toggleReported(this.chatId);
               ChatSelectionScreen.this.updateConfirmSelectedButton();
               return true;
            } else {
               return false;
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int FACE_SIZE = 12;
         private static final int PADDING = 4;
         private final Component heading;
         private final Supplier<PlayerSkin> skin;
         private final boolean canReport;

         public MessageHeadingEntry(final GameProfile pProfile, final Component pHeading, final boolean pCanReport) {
            this.heading = pHeading;
            this.canReport = pCanReport;
            this.skin = ChatSelectionList.this.minecraft.getSkinManager().lookupInsecure(pProfile);
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            int $$10 = pLeft - 12 + 4;
            int $$11 = pTop + (pHeight - 12) / 2;
            PlayerFaceRenderer.draw(pGuiGraphics, this.skin.get(), $$10, $$11, 12);
            int $$12 = pTop + 1 + (pHeight - 9) / 2;
            pGuiGraphics.drawString(ChatSelectionScreen.this.font, this.heading, $$10 + 12 + 4, $$12, this.canReport ? -1 : -1593835521);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         @Override
         public void render(
            GuiGraphics p_282007_,
            int p_240110_,
            int p_240111_,
            int p_240112_,
            int p_240113_,
            int p_240114_,
            int p_240115_,
            int p_240116_,
            boolean p_240117_,
            float p_240118_
         ) {
         }
      }
   }
}
