package net.minecraft.client.gui.screens.options;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
   private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withColor(-4539718);
   private static final int FOOTER_HEIGHT = 53;
   private LanguageSelectScreen.LanguageSelectionList languageSelectionList;
   final LanguageManager languageManager;

   public LanguageSelectScreen(Screen pLastScreen, Options pOptions, LanguageManager pLanguageManager) {
      super(pLastScreen, pOptions, Component.translatable("options.language.title"));
      this.languageManager = pLanguageManager;
      this.layout.setFooterHeight(53);
   }

   @Override
   protected void addContents() {
      this.languageSelectionList = this.layout.addToContents(new LanguageSelectScreen.LanguageSelectionList(this.minecraft));
   }

   @Override
   protected void addOptions() {
   }

   @Override
   protected void addFooter() {
      LinearLayout $$0 = this.layout.addToFooter(LinearLayout.vertical()).spacing(8);
      $$0.defaultCellSetting().alignHorizontallyCenter();
      $$0.addChild(new StringWidget(WARNING_LABEL, this.font));
      LinearLayout $$1 = $$0.addChild(LinearLayout.horizontal().spacing(8));
      $$1.addChild(
         Button.builder(Component.translatable("options.font"), p_343010_ -> this.minecraft.setScreen(new FontOptionsScreen(this, this.options))).build()
      );
      $$1.addChild(Button.builder(CommonComponents.GUI_DONE, p_343186_ -> this.onDone()).build());
   }

   @Override
   protected void repositionElements() {
      super.repositionElements();
      this.languageSelectionList.updateSize(this.width, this.layout);
   }

   void onDone() {
      LanguageSelectScreen.LanguageSelectionList.Entry $$0 = this.languageSelectionList.getSelected();
      if ($$0 != null && !$$0.code.equals(this.languageManager.getSelected())) {
         this.languageManager.setSelected($$0.code);
         this.options.languageCode = $$0.code;
         this.minecraft.reloadResourcePacks();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   @OnlyIn(Dist.CLIENT)
   class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
      public LanguageSelectionList(final Minecraft pMinecraft) {
         super(pMinecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height - 33 - 53, 33, 18);
         String $$1 = LanguageSelectScreen.this.languageManager.getSelected();
         LanguageSelectScreen.this.languageManager.getLanguages().forEach((p_342614_, p_342581_) -> {
            LanguageSelectScreen.LanguageSelectionList.Entry $$3 = new LanguageSelectScreen.LanguageSelectionList.Entry(p_342614_, p_342581_);
            this.addEntry($$3);
            if ($$1.equals(p_342614_)) {
               this.setSelected($$3);
            }
         });
         if (this.getSelected() != null) {
            this.centerScrollOn(this.getSelected());
         }
      }

      @Override
      public int getRowWidth() {
         return super.getRowWidth() + 50;
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
         final String code;
         private final Component language;
         private long lastClickTime;

         public Entry(final String pCode, final LanguageInfo pLanguageInfo) {
            this.code = pCode;
            this.language = pLanguageInfo.toComponent();
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            pGuiGraphics.drawCenteredString(LanguageSelectScreen.this.font, this.language, LanguageSelectionList.this.width / 2, pTop + pHeight / 2 - 9 / 2, -1);
         }

         @Override
         public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (CommonInputs.selected(pKeyCode)) {
               this.select();
               LanguageSelectScreen.this.onDone();
               return true;
            } else {
               return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
         }

         @Override
         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.select();
            if (Util.getMillis() - this.lastClickTime < 250L) {
               LanguageSelectScreen.this.onDone();
            }

            this.lastClickTime = Util.getMillis();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }

         private void select() {
            LanguageSelectionList.this.setSelected(this);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.language);
         }
      }
   }
}
