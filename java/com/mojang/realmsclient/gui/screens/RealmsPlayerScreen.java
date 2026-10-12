package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPlayerScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.players.title");
   static final Component QUESTION_TITLE = Component.translatable("mco.question");
   private static final int PADDING = 8;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final RealmsConfigureWorldScreen lastScreen;
   final RealmsServer serverData;
   @Nullable
   private RealmsPlayerScreen.InvitedObjectSelectionList invitedList;
   boolean stateChanged;

   public RealmsPlayerScreen(RealmsConfigureWorldScreen pLastScreen, RealmsServer pServerData) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.serverData = pServerData;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.invitedList = this.layout.addToContents(new RealmsPlayerScreen.InvitedObjectSelectionList());
      this.repopulateInvitedList();
      LinearLayout $$0 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      $$0.addChild(
         Button.builder(
               Component.translatable("mco.configure.world.buttons.invite"),
               p_280732_ -> this.minecraft.setScreen(new RealmsInviteScreen(this.lastScreen, this, this.serverData))
            )
            .build()
      );
      $$0.addChild(Button.builder(CommonComponents.GUI_BACK, p_325135_ -> this.onClose()).build());
      this.layout.visitWidgets(p_325137_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_325137_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.invitedList != null) {
         this.invitedList.updateSize(this.width, this.layout);
      }
   }

   void repopulateInvitedList() {
      if (this.invitedList != null) {
         this.invitedList.children().clear();

         for (PlayerInfo $$0 : this.serverData.players) {
            this.invitedList.children().add(new RealmsPlayerScreen.Entry($$0));
         }
      }
   }

   @Override
   public void onClose() {
      this.backButtonClicked();
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.minecraft.setScreen(this.lastScreen.getNewScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ContainerObjectSelectionList.Entry<RealmsPlayerScreen.Entry> {
      private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
      private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
      private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
      private static final ResourceLocation MAKE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/make_operator");
      private static final ResourceLocation REMOVE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_operator");
      private static final ResourceLocation REMOVE_PLAYER_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_player");
      private static final int ICON_WIDTH = 8;
      private static final int ICON_HEIGHT = 7;
      private final PlayerInfo playerInfo;
      private final Button removeButton;
      private final Button makeOpButton;
      private final Button removeOpButton;

      public Entry(final PlayerInfo pPlayerInfo) {
         this.playerInfo = pPlayerInfo;
         int $$1 = RealmsPlayerScreen.this.serverData.players.indexOf(this.playerInfo);
         this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, p_325150_ -> this.op($$1), false)
            .sprite(MAKE_OP_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(NORMAL_USER_TEXT))
            .narration(
               p_325144_ -> CommonComponents.joinForNarration(
                     Component.translatable("mco.invited.player.narration", pPlayerInfo.getName()),
                     p_325144_.get(),
                     Component.translatable("narration.cycle_button.usage.focused", OP_TEXT)
                  )
            )
            .build();
         this.removeOpButton = SpriteIconButton.builder(OP_TEXT, p_325146_ -> this.deop($$1), false)
            .sprite(REMOVE_OP_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(OP_TEXT))
            .narration(
               p_325142_ -> CommonComponents.joinForNarration(
                     Component.translatable("mco.invited.player.narration", pPlayerInfo.getName()),
                     p_325142_.get(),
                     Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT)
                  )
            )
            .build();
         this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, p_325152_ -> this.uninvite($$1), false)
            .sprite(REMOVE_PLAYER_SPRITE, 8, 7)
            .width(16 + RealmsPlayerScreen.this.font.width(REMOVE_TEXT))
            .narration(
               p_325148_ -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", pPlayerInfo.getName()), p_325148_.get())
            )
            .build();
         this.updateOpButtons();
      }

      private void op(int pIndex) {
         RealmsClient $$1 = RealmsClient.create();
         UUID $$2 = RealmsPlayerScreen.this.serverData.players.get(pIndex).getUuid();

         try {
            this.updateOps($$1.op(RealmsPlayerScreen.this.serverData.id, $$2));
         } catch (RealmsServiceException var5) {
            RealmsPlayerScreen.LOGGER.error("Couldn't op the user", var5);
         }

         this.updateOpButtons();
      }

      private void deop(int pIndex) {
         RealmsClient $$1 = RealmsClient.create();
         UUID $$2 = RealmsPlayerScreen.this.serverData.players.get(pIndex).getUuid();

         try {
            this.updateOps($$1.deop(RealmsPlayerScreen.this.serverData.id, $$2));
         } catch (RealmsServiceException var5) {
            RealmsPlayerScreen.LOGGER.error("Couldn't deop the user", var5);
         }

         this.updateOpButtons();
      }

      private void uninvite(int pIndex) {
         if (pIndex >= 0 && pIndex < RealmsPlayerScreen.this.serverData.players.size()) {
            PlayerInfo $$1 = RealmsPlayerScreen.this.serverData.players.get(pIndex);
            RealmsConfirmScreen $$2 = new RealmsConfirmScreen(p_325140_ -> {
               if (p_325140_) {
                  RealmsClient $$3 = RealmsClient.create();

                  try {
                     $$3.uninvite(RealmsPlayerScreen.this.serverData.id, $$1.getUuid());
                  } catch (RealmsServiceException var6) {
                     RealmsPlayerScreen.LOGGER.error("Couldn't uninvite user", var6);
                  }

                  RealmsPlayerScreen.this.serverData.players.remove(pIndex);
                  RealmsPlayerScreen.this.repopulateInvitedList();
               }

               RealmsPlayerScreen.this.stateChanged = true;
               RealmsPlayerScreen.this.minecraft.setScreen(RealmsPlayerScreen.this);
            }, RealmsPlayerScreen.QUESTION_TITLE, Component.translatable("mco.configure.world.uninvite.player", $$1.getName()));
            RealmsPlayerScreen.this.minecraft.setScreen($$2);
         }
      }

      private void updateOps(Ops pOps) {
         for (PlayerInfo $$1 : RealmsPlayerScreen.this.serverData.players) {
            $$1.setOperator(pOps.ops.contains($$1.getName()));
         }
      }

      private void updateOpButtons() {
         this.makeOpButton.visible = !this.playerInfo.isOperator();
         this.removeOpButton.visible = !this.makeOpButton.visible;
      }

      private Button activeOpButton() {
         return this.makeOpButton.visible ? this.makeOpButton : this.removeOpButton;
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.activeOpButton(), this.removeButton);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.activeOpButton(), this.removeButton);
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         int $$10;
         if (!this.playerInfo.getAccepted()) {
            $$10 = -6250336;
         } else if (this.playerInfo.getOnline()) {
            $$10 = 8388479;
         } else {
            $$10 = -1;
         }

         int $$13 = pTop + pHeight / 2 - 16;
         RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft, $$13, 32, this.playerInfo.getUuid());
         int $$14 = pTop + pHeight / 2 - 9 / 2;
         pGuiGraphics.drawString(RealmsPlayerScreen.this.font, this.playerInfo.getName(), pLeft + 8 + 32, $$14, $$10, false);
         int $$15 = pTop + pHeight / 2 - 10;
         int $$16 = pLeft + pWidth - this.removeButton.getWidth();
         this.removeButton.setPosition($$16, $$15);
         this.removeButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         int $$17 = $$16 - this.activeOpButton().getWidth() - 8;
         this.makeOpButton.setPosition($$17, $$15);
         this.makeOpButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.removeOpButton.setPosition($$17, $$15);
         this.removeOpButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class InvitedObjectSelectionList extends ContainerObjectSelectionList<RealmsPlayerScreen.Entry> {
      private static final int ITEM_HEIGHT = 36;

      public InvitedObjectSelectionList() {
         super(
            Minecraft.getInstance(),
            RealmsPlayerScreen.this.width,
            RealmsPlayerScreen.this.layout.getContentHeight(),
            RealmsPlayerScreen.this.layout.getHeaderHeight(),
            36
         );
         this.setRenderHeader(true, (int)(9.0F * 1.5F));
      }

      @Override
      protected void renderHeader(GuiGraphics p_329500_, int p_331955_, int p_330781_) {
         String $$3 = RealmsPlayerScreen.this.serverData.players != null ? Integer.toString(RealmsPlayerScreen.this.serverData.players.size()) : "0";
         Component $$4 = Component.translatable("mco.configure.world.invited.number", $$3).withStyle(ChatFormatting.UNDERLINE);
         p_329500_.drawString(
            RealmsPlayerScreen.this.font, $$4, p_331955_ + this.getRowWidth() / 2 - RealmsPlayerScreen.this.font.width($$4) / 2, p_330781_, -1, false
         );
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * this.itemHeight + this.headerHeight;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }
}
