package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.TextRenderingUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonLinks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectWorldTemplateScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
   private static final Component SELECT_BUTTON_NAME = Component.translatable("mco.template.button.select");
   private static final Component TRAILER_BUTTON_NAME = Component.translatable("mco.template.button.trailer");
   private static final Component PUBLISHER_BUTTON_NAME = Component.translatable("mco.template.button.publisher");
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_SPACING = 10;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   final Consumer<WorldTemplate> callback;
   RealmsSelectWorldTemplateScreen.WorldTemplateList worldTemplateList;
   private final RealmsServer.WorldType worldType;
   private Button selectButton;
   private Button trailerButton;
   private Button publisherButton;
   @Nullable
   WorldTemplate selectedTemplate = null;
   @Nullable
   String currentLink;
   @Nullable
   private Component[] warning;
   @Nullable
   List<TextRenderingUtils.Line> noTemplatesMessage;

   public RealmsSelectWorldTemplateScreen(Component pTitle, Consumer<WorldTemplate> pCallback, RealmsServer.WorldType pWorldType) {
      this(pTitle, pCallback, pWorldType, null);
   }

   public RealmsSelectWorldTemplateScreen(
      Component pTitle, Consumer<WorldTemplate> pCallback, RealmsServer.WorldType pWorldType, @Nullable WorldTemplatePaginatedList pWorldTemplatePaginatedList
   ) {
      super(pTitle);
      this.callback = pCallback;
      this.worldType = pWorldType;
      if (pWorldTemplatePaginatedList == null) {
         this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList(this);
         this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
      } else {
         this.worldTemplateList = new RealmsSelectWorldTemplateScreen.WorldTemplateList(this, Lists.newArrayList(pWorldTemplatePaginatedList.templates));
         this.fetchTemplatesAsync(pWorldTemplatePaginatedList);
      }
   }

   public void setWarning(Component... pWarning) {
      this.warning = pWarning;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(this.title, this.font);
      this.worldTemplateList = this.layout.addToContents(new RealmsSelectWorldTemplateScreen.WorldTemplateList(this, this.worldTemplateList.getTemplates()));
      LinearLayout $$0 = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
      $$0.defaultCellSetting().alignHorizontallyCenter();
      this.trailerButton = $$0.addChild(Button.builder(TRAILER_BUTTON_NAME, p_89701_ -> this.onTrailer()).width(100).build());
      this.selectButton = $$0.addChild(Button.builder(SELECT_BUTTON_NAME, p_89696_ -> this.selectTemplate()).width(100).build());
      $$0.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_89691_ -> this.onClose()).width(100).build());
      this.publisherButton = $$0.addChild(Button.builder(PUBLISHER_BUTTON_NAME, p_89679_ -> this.onPublish()).width(100).build());
      this.updateButtonStates();
      this.layout.visitWidgets(p_325159_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_325159_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.worldTemplateList.setSize(this.width, this.height - this.layout.getFooterHeight() - this.getHeaderHeight());
      this.layout.arrangeElements();
   }

   @Override
   public Component getNarrationMessage() {
      List<Component> $$0 = Lists.newArrayListWithCapacity(2);
      $$0.add(this.title);
      if (this.warning != null) {
         $$0.addAll(Arrays.asList(this.warning));
      }

      return CommonComponents.joinLines($$0);
   }

   void updateButtonStates() {
      this.publisherButton.visible = this.selectedTemplate != null && !this.selectedTemplate.link.isEmpty();
      this.trailerButton.visible = this.selectedTemplate != null && !this.selectedTemplate.trailer.isEmpty();
      this.selectButton.active = this.selectedTemplate != null;
   }

   @Override
   public void onClose() {
      this.callback.accept(null);
   }

   private void selectTemplate() {
      if (this.selectedTemplate != null) {
         this.callback.accept(this.selectedTemplate);
      }
   }

   private void onTrailer() {
      if (this.selectedTemplate != null && !this.selectedTemplate.trailer.isBlank()) {
         ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.trailer);
      }
   }

   private void onPublish() {
      if (this.selectedTemplate != null && !this.selectedTemplate.link.isBlank()) {
         ConfirmLinkScreen.confirmLinkNow(this, this.selectedTemplate.link);
      }
   }

   private void fetchTemplatesAsync(final WorldTemplatePaginatedList pOutput) {
      (new Thread("realms-template-fetcher") {
            @Override
            public void run() {
               WorldTemplatePaginatedList $$0 = pOutput;
               RealmsClient $$1 = RealmsClient.create();

               while ($$0 != null) {
                  Either<WorldTemplatePaginatedList, Exception> $$2 = RealmsSelectWorldTemplateScreen.this.fetchTemplates($$0, $$1);
                  $$0 = RealmsSelectWorldTemplateScreen.this.minecraft
                     .submit(
                        () -> {
                           if ($$2.right().isPresent()) {
                              RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates", (Throwable)$$2.right().get());
                              if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                 RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(I18n.get("mco.template.select.failure"));
                              }

                              return null;
                           } else {
                              WorldTemplatePaginatedList $$1x = (WorldTemplatePaginatedList)$$2.left().get();

                              for (WorldTemplate $$2x : $$1x.templates) {
                                 RealmsSelectWorldTemplateScreen.this.worldTemplateList.addEntry($$2x);
                              }

                              if ($$1x.templates.isEmpty()) {
                                 if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                    String $$3 = I18n.get("mco.template.select.none", "%link");
                                    TextRenderingUtils.LineSegment $$4 = TextRenderingUtils.LineSegment.link(
                                       I18n.get("mco.template.select.none.linkTitle"), CommonLinks.REALMS_CONTENT_CREATION.toString()
                                    );
                                    RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose($$3, $$4);
                                 }

                                 return null;
                              } else {
                                 return $$1x;
                              }
                           }
                        }
                     )
                     .join();
               }
            }
         })
         .start();
   }

   Either<WorldTemplatePaginatedList, Exception> fetchTemplates(WorldTemplatePaginatedList pTemplates, RealmsClient pRealmsClient) {
      try {
         return Either.left(pRealmsClient.fetchWorldTemplates(pTemplates.page + 1, pTemplates.size, this.worldType));
      } catch (RealmsServiceException var4) {
         return Either.right(var4);
      }
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.currentLink = null;
      if (this.noTemplatesMessage != null) {
         this.renderMultilineMessage(pGuiGraphics, pMouseX, pMouseY, this.noTemplatesMessage);
      }

      if (this.warning != null) {
         for (int $$4 = 0; $$4 < this.warning.length; $$4++) {
            Component $$5 = this.warning[$$4];
            pGuiGraphics.drawCenteredString(this.font, $$5, this.width / 2, row(-1 + $$4), -6250336);
         }
      }
   }

   private void renderMultilineMessage(GuiGraphics pGuiGraphics, int pX, int pY, List<TextRenderingUtils.Line> pLines) {
      for (int $$4 = 0; $$4 < pLines.size(); $$4++) {
         TextRenderingUtils.Line $$5 = pLines.get($$4);
         int $$6 = row(4 + $$4);
         int $$7 = $$5.segments.stream().mapToInt(p_280748_ -> this.font.width(p_280748_.renderedText())).sum();
         int $$8 = this.width / 2 - $$7 / 2;

         for (TextRenderingUtils.LineSegment $$9 : $$5.segments) {
            int $$10 = $$9.isLink() ? 3368635 : -1;
            int $$11 = pGuiGraphics.drawString(this.font, $$9.renderedText(), $$8, $$6, $$10);
            if ($$9.isLink() && pX > $$8 && pX < $$11 && pY > $$6 - 3 && pY < $$6 + 8) {
               this.setTooltipForNextRenderPass(Component.literal($$9.getLinkUrl()));
               this.currentLink = $$9.getLinkUrl();
            }

            $$8 = $$11;
         }
      }
   }

   int getHeaderHeight() {
      return this.warning != null ? row(1) : 33;
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsSelectWorldTemplateScreen.Entry> {
      private static final WidgetSprites WEBSITE_LINK_SPRITES = new WidgetSprites(
         ResourceLocation.withDefaultNamespace("icon/link"), ResourceLocation.withDefaultNamespace("icon/link_highlighted")
      );
      private static final WidgetSprites TRAILER_LINK_SPRITES = new WidgetSprites(
         ResourceLocation.withDefaultNamespace("icon/video_link"), ResourceLocation.withDefaultNamespace("icon/video_link_highlighted")
      );
      private static final Component PUBLISHER_LINK_TOOLTIP = Component.translatable("mco.template.info.tooltip");
      private static final Component TRAILER_LINK_TOOLTIP = Component.translatable("mco.template.trailer.tooltip");
      public final WorldTemplate template;
      private long lastClickTime;
      @Nullable
      private ImageButton websiteButton;
      @Nullable
      private ImageButton trailerButton;

      public Entry(final WorldTemplate pTemplate) {
         this.template = pTemplate;
         if (!pTemplate.link.isBlank()) {
            this.websiteButton = new ImageButton(
               15, 15, WEBSITE_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, pTemplate.link), PUBLISHER_LINK_TOOLTIP
            );
            this.websiteButton.setTooltip(Tooltip.create(PUBLISHER_LINK_TOOLTIP));
         }

         if (!pTemplate.trailer.isBlank()) {
            this.trailerButton = new ImageButton(
               15, 15, TRAILER_LINK_SPRITES, ConfirmLinkScreen.confirmLink(RealmsSelectWorldTemplateScreen.this, pTemplate.trailer), TRAILER_LINK_TOOLTIP
            );
            this.trailerButton.setTooltip(Tooltip.create(TRAILER_LINK_TOOLTIP));
         }
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.template;
         RealmsSelectWorldTemplateScreen.this.updateButtonStates();
         if (Util.getMillis() - this.lastClickTime < 250L && this.isFocused()) {
            RealmsSelectWorldTemplateScreen.this.callback.accept(this.template);
         }

         this.lastClickTime = Util.getMillis();
         if (this.websiteButton != null) {
            this.websiteButton.mouseClicked(pMouseX, pMouseY, pButton);
         }

         if (this.trailerButton != null) {
            this.trailerButton.mouseClicked(pMouseX, pMouseY, pButton);
         }

         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         pGuiGraphics.blit(RealmsTextureManager.worldTemplate(this.template.id, this.template.image), pLeft + 1, pTop + 1 + 1, 0.0F, 0.0F, 38, 38, 38, 38);
         pGuiGraphics.blitSprite(RealmsSelectWorldTemplateScreen.SLOT_FRAME_SPRITE, pLeft, pTop + 1, 40, 40);
         int $$10 = 5;
         int $$11 = RealmsSelectWorldTemplateScreen.this.font.width(this.template.version);
         if (this.websiteButton != null) {
            this.websiteButton.setPosition(pLeft + pWidth - $$11 - this.websiteButton.getWidth() - 10, pTop);
            this.websiteButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         }

         if (this.trailerButton != null) {
            this.trailerButton.setPosition(pLeft + pWidth - $$11 - this.trailerButton.getWidth() * 2 - 15, pTop);
            this.trailerButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         }

         int $$12 = pLeft + 45 + 20;
         int $$13 = pTop + 5;
         pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.name, $$12, $$13, -1, false);
         pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.version, pLeft + pWidth - $$11 - 5, $$13, 7105644, false);
         pGuiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.author, $$12, $$13 + 9 + 5, -6250336, false);
         if (!this.template.recommendedPlayers.isBlank()) {
            pGuiGraphics.drawString(
               RealmsSelectWorldTemplateScreen.this.font, this.template.recommendedPlayers, $$12, pTop + pHeight - 9 / 2 - 5, 5000268, false
            );
         }
      }

      @Override
      public Component getNarration() {
         Component $$0 = CommonComponents.joinLines(
            Component.literal(this.template.name),
            Component.translatable("mco.template.select.narrate.authors", this.template.author),
            Component.literal(this.template.recommendedPlayers),
            Component.translatable("mco.template.select.narrate.version", this.template.version)
         );
         return Component.translatable("narrator.select", $$0);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class WorldTemplateList extends RealmsObjectSelectionList<RealmsSelectWorldTemplateScreen.Entry> {
      public WorldTemplateList(final RealmsSelectWorldTemplateScreen param1) {
         this(var1, Collections.emptyList());
      }

      public WorldTemplateList(final Iterable<WorldTemplate> param1, final Iterable pTemplates) {
         super(var1.width, var1.height - 33 - var1.getHeaderHeight(), var1.getHeaderHeight(), 46);
         this.this$0 = var1;
         pTemplates.forEach(this::addEntry);
      }

      public void addEntry(WorldTemplate p_89805_) {
         this.addEntry(this.this$0.new Entry(p_89805_));
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.this$0.currentLink != null) {
            ConfirmLinkScreen.confirmLinkNow(this.this$0, this.this$0.currentLink);
            return true;
         } else {
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }
      }

      public void setSelected(@Nullable RealmsSelectWorldTemplateScreen.Entry pSelected) {
         super.setSelected(pSelected);
         this.this$0.selectedTemplate = pSelected == null ? null : pSelected.template;
         this.this$0.updateButtonStates();
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 46;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }

      public boolean isEmpty() {
         return this.getItemCount() == 0;
      }

      public List<WorldTemplate> getTemplates() {
         return this.children().stream().map(p_89814_ -> p_89814_.template).collect(Collectors.toList());
      }
   }
}
