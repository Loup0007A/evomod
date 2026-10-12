package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PresetFlatWorldScreen extends Screen {
   static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
   public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
   private final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen pParent) {
      super(Component.translatable("createWorld.customize.presets.title"));
      this.parent = pParent;
   }

   @Nullable
   private static FlatLayerInfo getLayerInfoFromString(HolderGetter<Block> pBlockGetter, String pLayerInfo, int pCurrentHeight) {
      List<String> $$3 = Splitter.on('*').limit(2).splitToList(pLayerInfo);
      int $$5;
      String $$4;
      if ($$3.size() == 2) {
         $$4 = $$3.get(1);

         try {
            $$5 = Math.max(Integer.parseInt($$3.get(0)), 0);
         } catch (NumberFormatException var11) {
            LOGGER.error("Error while parsing flat world string", var11);
            return null;
         }
      } else {
         $$4 = $$3.get(0);
         $$5 = 1;
      }

      int $$9 = Math.min(pCurrentHeight + $$5, DimensionType.Y_SIZE);
      int $$10 = $$9 - pCurrentHeight;

      Optional<Holder.Reference<Block>> $$11;
      try {
         $$11 = pBlockGetter.get(ResourceKey.create(Registries.BLOCK, ResourceLocation.parse($$4)));
      } catch (Exception var10) {
         LOGGER.error("Error while parsing flat world string", var10);
         return null;
      }

      if ($$11.isEmpty()) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", $$4);
         return null;
      } else {
         return new FlatLayerInfo($$10, $$11.get().value());
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(HolderGetter<Block> pBlockGetter, String pLayerInfo) {
      List<FlatLayerInfo> $$2 = Lists.newArrayList();
      String[] $$3 = pLayerInfo.split(",");
      int $$4 = 0;

      for (String $$5 : $$3) {
         FlatLayerInfo $$6 = getLayerInfoFromString(pBlockGetter, $$5, $$4);
         if ($$6 == null) {
            return Collections.emptyList();
         }

         $$2.add($$6);
         $$4 += $$6.getHeight();
      }

      return $$2;
   }

   public static FlatLevelGeneratorSettings fromString(
      HolderGetter<Block> pBlockGetter,
      HolderGetter<Biome> pBiomeGetter,
      HolderGetter<StructureSet> pStructureSetGetter,
      HolderGetter<PlacedFeature> pPlacedFeatureGetter,
      String pSettings,
      FlatLevelGeneratorSettings pLayerGenerationSettings
   ) {
      Iterator<String> $$6 = Splitter.on(';').split(pSettings).iterator();
      if (!$$6.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(pBiomeGetter, pStructureSetGetter, pPlacedFeatureGetter);
      } else {
         List<FlatLayerInfo> $$7 = getLayersInfoFromString(pBlockGetter, $$6.next());
         if ($$7.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(pBiomeGetter, pStructureSetGetter, pPlacedFeatureGetter);
         } else {
            Holder.Reference<Biome> $$8 = pBiomeGetter.getOrThrow(DEFAULT_BIOME);
            Holder<Biome> $$9 = $$8;
            if ($$6.hasNext()) {
               String $$10 = $$6.next();
               $$9 = Optional.ofNullable(ResourceLocation.tryParse($$10))
                  .map(p_258126_ -> ResourceKey.create(Registries.BIOME, p_258126_))
                  .flatMap(pBiomeGetter::get)
                  .orElseGet(() -> {
                     LOGGER.warn("Invalid biome: {}", $$10);
                     return $$8;
                  });
            }

            return pLayerGenerationSettings.withBiomeAndLayers($$7, pLayerGenerationSettings.structureOverrides(), $$9);
         }
      }
   }

   static String save(FlatLevelGeneratorSettings pLevelGeneratorSettings) {
      StringBuilder $$1 = new StringBuilder();

      for (int $$2 = 0; $$2 < pLevelGeneratorSettings.getLayersInfo().size(); $$2++) {
         if ($$2 > 0) {
            $$1.append(",");
         }

         $$1.append(pLevelGeneratorSettings.getLayersInfo().get($$2));
      }

      $$1.append(";");
      $$1.append(pLevelGeneratorSettings.getBiome().unwrapKey().map(ResourceKey::location).orElseThrow(() -> new IllegalStateException("Biome not registered")));
      return $$1.toString();
   }

   @Override
   protected void init() {
      this.shareText = Component.translatable("createWorld.customize.presets.share");
      this.listText = Component.translatable("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      WorldCreationContext $$0 = this.parent.parent.getUiState().getSettings();
      RegistryAccess $$1 = $$0.worldgenLoadContext();
      FeatureFlagSet $$2 = $$0.dataConfiguration().enabledFeatures();
      HolderGetter<Biome> $$3 = $$1.lookupOrThrow(Registries.BIOME);
      HolderGetter<StructureSet> $$4 = $$1.lookupOrThrow(Registries.STRUCTURE_SET);
      HolderGetter<PlacedFeature> $$5 = $$1.lookupOrThrow(Registries.PLACED_FEATURE);
      HolderGetter<Block> $$6 = $$1.lookupOrThrow(Registries.BLOCK).filterFeatures($$2);
      this.export.setValue(save(this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = this.addRenderableWidget(new PresetFlatWorldScreen.PresetsList($$1, $$2));
      this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets.select"), p_280822_ -> {
         FlatLevelGeneratorSettings $$5x = fromString($$6, $$3, $$4, $$5, this.export.getValue(), this.settings);
         this.parent.setConfig($$5x);
         this.minecraft.setScreen(this.parent);
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_280823_ -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build()
      );
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      return this.list.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
   }

   @Override
   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String $$3 = this.export.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.export.setValue($$3);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
      pGuiGraphics.drawString(this.font, this.shareText, 51, 30, 10526880);
      pGuiGraphics.drawString(this.font, this.listText, 51, 68, 10526880);
      pGuiGraphics.pose().popPose();
      this.export.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }

   public void updateButtonValidity(boolean pValid) {
      this.selectButton.active = pValid || this.export.getValue().length() > 1;
   }

   @OnlyIn(Dist.CLIENT)
   class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList(final RegistryAccess pRegistryAccess, final FeatureFlagSet pFlags) {
         super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height - 117, 80, 24);

         for (Holder<FlatLevelGeneratorPreset> $$2 : pRegistryAccess.registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
            .getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
            Set<Block> $$3 = $$2.value()
               .settings()
               .getLayersInfo()
               .stream()
               .map(p_259579_ -> p_259579_.getBlockState().getBlock())
               .filter(p_259421_ -> !p_259421_.isEnabled(pFlags))
               .collect(Collectors.toSet());
            if (!$$3.isEmpty()) {
               PresetFlatWorldScreen.LOGGER
                  .info(
                     "Discarding flat world preset {} since it contains experimental blocks {}",
                     $$2.unwrapKey().map(p_259357_ -> p_259357_.location().toString()).orElse("<unknown>"),
                     $$3
                  );
            } else {
               this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry($$2));
            }
         }
      }

      public void setSelected(@Nullable PresetFlatWorldScreen.PresetsList.Entry pEntry) {
         super.setSelected(pEntry);
         PresetFlatWorldScreen.this.updateButtonValidity(pEntry != null);
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
         } else {
            if (CommonInputs.selected(pKeyCode) && this.getSelected() != null) {
               this.getSelected().select();
            }

            return false;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private static final ResourceLocation STATS_ICON_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stats_icons.png");
         private final FlatLevelGeneratorPreset preset;
         private final Component name;

         public Entry(final Holder<FlatLevelGeneratorPreset> pPresetHolder) {
            this.preset = pPresetHolder.value();
            this.name = pPresetHolder.unwrapKey()
               .map(p_232760_ -> Component.translatable(p_232760_.location().toLanguageKey("flat_world_preset")))
               .orElse(PresetFlatWorldScreen.UNKNOWN_PRESET);
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            this.blitSlot(pGuiGraphics, pLeft, pTop, this.preset.displayItem().value());
            pGuiGraphics.drawString(PresetFlatWorldScreen.this.font, this.name, pLeft + 18 + 5, pTop + 6, 16777215, false);
         }

         @Override
         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.select();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }

         void select() {
            PresetsList.this.setSelected(this);
            PresetFlatWorldScreen.this.settings = this.preset.settings();
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart(false);
         }

         private void blitSlot(GuiGraphics pGuiGraphics, int pX, int pY, Item pItem) {
            this.blitSlotBg(pGuiGraphics, pX + 1, pY + 1);
            pGuiGraphics.renderFakeItem(new ItemStack(pItem), pX + 2, pY + 2);
         }

         private void blitSlotBg(GuiGraphics pGuiGraphics, int pX, int pY) {
            pGuiGraphics.blitSprite(PresetFlatWorldScreen.SLOT_SPRITE, pX, pY, 0, 18, 18);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }
      }
   }
}
