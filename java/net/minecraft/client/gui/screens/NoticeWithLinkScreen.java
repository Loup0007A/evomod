package net.minecraft.client.gui.screens;

import java.net.URI;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoticeWithLinkScreen extends Screen {
   private static final Component SYMLINK_WORLD_TITLE = Component.translatable("symlink_warning.title.world").withStyle(ChatFormatting.BOLD);
   private static final Component SYMLINK_WORLD_MESSAGE_TEXT = Component.translatable(
      "symlink_warning.message.world", Component.translationArg(CommonLinks.SYMLINK_HELP)
   );
   private static final Component SYMLINK_PACK_TITLE = Component.translatable("symlink_warning.title.pack").withStyle(ChatFormatting.BOLD);
   private static final Component SYMLINK_PACK_MESSAGE_TEXT = Component.translatable(
      "symlink_warning.message.pack", Component.translationArg(CommonLinks.SYMLINK_HELP)
   );
   private final Component message;
   private final URI uri;
   private final Runnable onClose;
   private final GridLayout layout = new GridLayout().rowSpacing(10);

   public NoticeWithLinkScreen(Component pTitle, Component pMessage, URI pUri, Runnable pOnClose) {
      super(pTitle);
      this.message = pMessage;
      this.uri = pUri;
      this.onClose = pOnClose;
   }

   public static Screen createWorldSymlinkWarningScreen(Runnable pOnClose) {
      return new NoticeWithLinkScreen(SYMLINK_WORLD_TITLE, SYMLINK_WORLD_MESSAGE_TEXT, CommonLinks.SYMLINK_HELP, pOnClose);
   }

   public static Screen createPackSymlinkWarningScreen(Runnable pOnClose) {
      return new NoticeWithLinkScreen(SYMLINK_PACK_TITLE, SYMLINK_PACK_MESSAGE_TEXT, CommonLinks.SYMLINK_HELP, pOnClose);
   }

   @Override
   protected void init() {
      super.init();
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper $$0 = this.layout.createRowHelper(1);
      $$0.addChild(new StringWidget(this.title, this.font));
      $$0.addChild(new MultiLineTextWidget(this.message, this.font).setMaxWidth(this.width - 50).setCentered(true));
      int $$1 = 120;
      GridLayout $$2 = new GridLayout().columnSpacing(5);
      GridLayout.RowHelper $$3 = $$2.createRowHelper(3);
      $$3.addChild(Button.builder(CommonComponents.GUI_OPEN_IN_BROWSER, p_340804_ -> Util.getPlatform().openUri(this.uri)).size(120, 20).build());
      $$3.addChild(
         Button.builder(CommonComponents.GUI_COPY_LINK_TO_CLIPBOARD, p_340803_ -> this.minecraft.keyboardHandler.setClipboard(this.uri.toString()))
            .size(120, 20)
            .build()
      );
      $$3.addChild(Button.builder(CommonComponents.GUI_BACK, p_300975_ -> this.onClose()).size(120, 20).build());
      $$0.addChild($$2);
      this.repositionElements();
      this.layout.visitWidgets(this::addRenderableWidget);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.message);
   }

   @Override
   public void onClose() {
      this.onClose.run();
   }
}
