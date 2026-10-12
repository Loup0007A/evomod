package net.minecraft.client.gui.screens.social;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.reporting.ReportPlayerScreen;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
   private static final ResourceLocation DRAFT_REPORT_SPRITE = ResourceLocation.withDefaultNamespace("icon/draft_report");
   private static final Duration TOOLTIP_DELAY = Duration.ofMillis(500L);
   private static final WidgetSprites REPORT_BUTTON_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("social_interactions/report_button"),
      ResourceLocation.withDefaultNamespace("social_interactions/report_button_disabled"),
      ResourceLocation.withDefaultNamespace("social_interactions/report_button_highlighted")
   );
   private static final WidgetSprites MUTE_BUTTON_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("social_interactions/mute_button"),
      ResourceLocation.withDefaultNamespace("social_interactions/mute_button_highlighted")
   );
   private static final WidgetSprites UNMUTE_BUTTON_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("social_interactions/unmute_button"),
      ResourceLocation.withDefaultNamespace("social_interactions/unmute_button_highlighted")
   );
   private final Minecraft minecraft;
   private final List<AbstractWidget> children;
   private final UUID id;
   private final String playerName;
   private final Supplier<PlayerSkin> skinGetter;
   private boolean isRemoved;
   private boolean hasRecentMessages;
   private final boolean reportingEnabled;
   private final boolean hasDraftReport;
   private final boolean chatReportable;
   @Nullable
   private Button hideButton;
   @Nullable
   private Button showButton;
   @Nullable
   private Button reportButton;
   private float tooltipHoverTime;
   private static final Component HIDDEN = Component.translatable("gui.socialInteractions.status_hidden").withStyle(ChatFormatting.ITALIC);
   private static final Component BLOCKED = Component.translatable("gui.socialInteractions.status_blocked").withStyle(ChatFormatting.ITALIC);
   private static final Component OFFLINE = Component.translatable("gui.socialInteractions.status_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component HIDDEN_OFFLINE = Component.translatable("gui.socialInteractions.status_hidden_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component BLOCKED_OFFLINE = Component.translatable("gui.socialInteractions.status_blocked_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component REPORT_DISABLED_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.report.disabled");
   private static final Component HIDE_TEXT_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.hide");
   private static final Component SHOW_TEXT_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.show");
   private static final Component REPORT_PLAYER_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.report");
   private static final int SKIN_SIZE = 24;
   private static final int PADDING = 4;
   public static final int SKIN_SHADE = FastColor.ARGB32.color(190, 0, 0, 0);
   private static final int CHAT_TOGGLE_ICON_SIZE = 20;
   public static final int BG_FILL = FastColor.ARGB32.color(255, 74, 74, 74);
   public static final int BG_FILL_REMOVED = FastColor.ARGB32.color(255, 48, 48, 48);
   public static final int PLAYERNAME_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
   public static final int PLAYER_STATUS_COLOR = FastColor.ARGB32.color(140, 255, 255, 255);

   public PlayerEntry(
      Minecraft pMinecraft,
      SocialInteractionsScreen pSocialInteractionsScreen,
      UUID pId,
      String pPlayerName,
      Supplier<PlayerSkin> pSkinGetter,
      boolean pPlayerReportable
   ) {
      this.minecraft = pMinecraft;
      this.id = pId;
      this.playerName = pPlayerName;
      this.skinGetter = pSkinGetter;
      ReportingContext $$6 = pMinecraft.getReportingContext();
      this.reportingEnabled = $$6.sender().isEnabled();
      this.chatReportable = pPlayerReportable;
      this.hasDraftReport = $$6.hasDraftReportFor(pId);
      Component $$7 = Component.translatable("gui.socialInteractions.narration.hide", pPlayerName);
      Component $$8 = Component.translatable("gui.socialInteractions.narration.show", pPlayerName);
      PlayerSocialManager $$9 = pMinecraft.getPlayerSocialManager();
      boolean $$10 = pMinecraft.getChatStatus().isChatAllowed(pMinecraft.isLocalServer());
      boolean $$11 = !pMinecraft.player.getUUID().equals(pId);
      if ($$11 && $$10 && !$$9.isBlocked(pId)) {
         this.reportButton = new ImageButton(
            0,
            0,
            20,
            20,
            REPORT_BUTTON_SPRITES,
            p_238875_ -> $$6.draftReportHandled(
                  pMinecraft, pSocialInteractionsScreen, () -> pMinecraft.setScreen(new ReportPlayerScreen(pSocialInteractionsScreen, $$6, this)), false
               ),
            Component.translatable("gui.socialInteractions.report")
         ) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.reportButton.active = this.reportingEnabled;
         this.reportButton.setTooltip(this.createReportButtonTooltip());
         this.reportButton.setTooltipDelay(TOOLTIP_DELAY);
         this.hideButton = new ImageButton(0, 0, 20, 20, MUTE_BUTTON_SPRITES, p_100612_ -> {
            $$9.hidePlayer(pId);
            this.onHiddenOrShown(true, Component.translatable("gui.socialInteractions.hidden_in_chat", pPlayerName));
         }, Component.translatable("gui.socialInteractions.hide")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.hideButton.setTooltip(Tooltip.create(HIDE_TEXT_TOOLTIP, $$7));
         this.hideButton.setTooltipDelay(TOOLTIP_DELAY);
         this.showButton = new ImageButton(0, 0, 20, 20, UNMUTE_BUTTON_SPRITES, p_170074_ -> {
            $$9.showPlayer(pId);
            this.onHiddenOrShown(false, Component.translatable("gui.socialInteractions.shown_in_chat", pPlayerName));
         }, Component.translatable("gui.socialInteractions.show")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.showButton.setTooltip(Tooltip.create(SHOW_TEXT_TOOLTIP, $$8));
         this.showButton.setTooltipDelay(TOOLTIP_DELAY);
         this.children = new ArrayList<>();
         this.children.add(this.hideButton);
         this.children.add(this.reportButton);
         this.updateHideAndShowButton($$9.isHidden(this.id));
      } else {
         this.children = ImmutableList.of();
      }
   }

   private Tooltip createReportButtonTooltip() {
      return !this.reportingEnabled
         ? Tooltip.create(REPORT_DISABLED_TOOLTIP)
         : Tooltip.create(REPORT_PLAYER_TOOLTIP, Component.translatable("gui.socialInteractions.narration.report", this.playerName));
   }

   @Override
   public void render(
      GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
   ) {
      int $$10 = pLeft + 4;
      int $$11 = pTop + (pHeight - 24) / 2;
      int $$12 = $$10 + 24 + 4;
      Component $$13 = this.getStatusComponent();
      int $$14;
      if ($$13 == CommonComponents.EMPTY) {
         pGuiGraphics.fill(pLeft, pTop, pLeft + pWidth, pTop + pHeight, BG_FILL);
         $$14 = pTop + (pHeight - 9) / 2;
      } else {
         pGuiGraphics.fill(pLeft, pTop, pLeft + pWidth, pTop + pHeight, BG_FILL_REMOVED);
         $$14 = pTop + (pHeight - (9 + 9)) / 2;
         pGuiGraphics.drawString(this.minecraft.font, $$13, $$12, $$14 + 12, PLAYER_STATUS_COLOR, false);
      }

      PlayerFaceRenderer.draw(pGuiGraphics, this.skinGetter.get(), $$10, $$11, 24);
      pGuiGraphics.drawString(this.minecraft.font, this.playerName, $$12, $$14, PLAYERNAME_COLOR, false);
      if (this.isRemoved) {
         pGuiGraphics.fill($$10, $$11, $$10 + 24, $$11 + 24, SKIN_SHADE);
      }

      if (this.hideButton != null && this.showButton != null && this.reportButton != null) {
         float $$16 = this.tooltipHoverTime;
         this.hideButton.setX(pLeft + (pWidth - this.hideButton.getWidth() - 4) - 20 - 4);
         this.hideButton.setY(pTop + (pHeight - this.hideButton.getHeight()) / 2);
         this.hideButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.showButton.setX(pLeft + (pWidth - this.showButton.getWidth() - 4) - 20 - 4);
         this.showButton.setY(pTop + (pHeight - this.showButton.getHeight()) / 2);
         this.showButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.reportButton.setX(pLeft + (pWidth - this.showButton.getWidth() - 4));
         this.reportButton.setY(pTop + (pHeight - this.showButton.getHeight()) / 2);
         this.reportButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         if ($$16 == this.tooltipHoverTime) {
            this.tooltipHoverTime = 0.0F;
         }
      }

      if (this.hasDraftReport && this.reportButton != null) {
         pGuiGraphics.blitSprite(DRAFT_REPORT_SPRITE, this.reportButton.getX() + 5, this.reportButton.getY() + 1, 15, 15);
      }
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   @Override
   public List<? extends NarratableEntry> narratables() {
      return this.children;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public UUID getPlayerId() {
      return this.id;
   }

   public Supplier<PlayerSkin> getSkinGetter() {
      return this.skinGetter;
   }

   public void setRemoved(boolean pIsRemoved) {
      this.isRemoved = pIsRemoved;
   }

   public boolean isRemoved() {
      return this.isRemoved;
   }

   public void setHasRecentMessages(boolean pHasRecentMessages) {
      this.hasRecentMessages = pHasRecentMessages;
   }

   public boolean hasRecentMessages() {
      return this.hasRecentMessages;
   }

   public boolean isChatReportable() {
      return this.chatReportable;
   }

   private void onHiddenOrShown(boolean pVisible, Component pMessage) {
      this.updateHideAndShowButton(pVisible);
      this.minecraft.gui.getChat().addMessage(pMessage);
      this.minecraft.getNarrator().sayNow(pMessage);
   }

   private void updateHideAndShowButton(boolean pVisible) {
      this.showButton.visible = pVisible;
      this.hideButton.visible = !pVisible;
      this.children.set(0, pVisible ? this.showButton : this.hideButton);
   }

   MutableComponent getEntryNarationMessage(MutableComponent pComponent) {
      Component $$1 = this.getStatusComponent();
      return $$1 == CommonComponents.EMPTY
         ? Component.literal(this.playerName).append(", ").append(pComponent)
         : Component.literal(this.playerName).append(", ").append($$1).append(", ").append(pComponent);
   }

   private Component getStatusComponent() {
      boolean $$0 = this.minecraft.getPlayerSocialManager().isHidden(this.id);
      boolean $$1 = this.minecraft.getPlayerSocialManager().isBlocked(this.id);
      if ($$1 && this.isRemoved) {
         return BLOCKED_OFFLINE;
      } else if ($$0 && this.isRemoved) {
         return HIDDEN_OFFLINE;
      } else if ($$1) {
         return BLOCKED;
      } else if ($$0) {
         return HIDDEN;
      } else {
         return this.isRemoved ? OFFLINE : CommonComponents.EMPTY;
      }
   }
}
