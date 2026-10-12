package net.minecraftforge.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.gui.widget.ModListWidget;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.VersionChecker.CheckResult;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;

public class ModListScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int PADDING = 6;
   private static final int BUTTON_MARGIN = 1;
   private static final int NUM_BUTTONS = ModListScreen.SortType.values().length;
   private final Screen parentScreen;
   private ModListWidget modList;
   private ModListScreen.InfoPanel modInfo;
   private ModListWidget.ModEntry selected = null;
   private int listWidth;
   private List<IModInfo> mods;
   private final List<IModInfo> unsortedMods;
   private Button configButton;
   private Button openModsFolderButton;
   private Button doneButton;
   private String lastFilterText = "";
   private EditBox search;
   private boolean sorted = false;
   private ModListScreen.SortType sortType = ModListScreen.SortType.NORMAL;

   private static String stripControlCodes(String value) {
      return StringUtil.stripColor(value);
   }

   public ModListScreen(Screen parentScreen) {
      super(Component.translatable("fml.menu.mods.title"));
      this.parentScreen = parentScreen;
      this.mods = ModList.get().getMods();
      this.unsortedMods = List.copyOf(this.mods);
   }

   @Override
   public void init() {
      for (IModInfo mod : this.mods) {
         this.listWidth = Math.max(this.listWidth, this.getFontRenderer().width(mod.getDisplayName()) + 10);
         this.listWidth = Math.max(this.listWidth, this.getFontRenderer().width(MavenVersionStringHelper.artifactVersionToString(mod.getVersion())) + 5);
      }

      this.listWidth = Math.max(Math.min(this.listWidth, this.width / 3), 100);
      this.listWidth = this.listWidth + (this.listWidth % NUM_BUTTONS != 0 ? NUM_BUTTONS - this.listWidth % NUM_BUTTONS : 0);
      int modInfoWidth = this.width - this.listWidth - 18;
      int doneButtonWidth = Math.min(modInfoWidth, 200);
      int y = this.height - 20 - 6;
      int fullButtonHeight = 32;
      this.doneButton = Button.builder(Component.translatable("gui.done"), b -> this.onClose())
         .bounds((this.listWidth + 6 + this.width - doneButtonWidth) / 2, y, doneButtonWidth, 20)
         .build();
      this.openModsFolderButton = Button.builder(
            Component.translatable("fml.menu.mods.openmodsfolder"), b -> Util.getPlatform().openFile(FMLPaths.MODSDIR.get().toFile())
         )
         .bounds(6, y, this.listWidth, 20)
         .build();
      y -= 26;
      this.configButton = Button.builder(Component.translatable("fml.menu.mods.config"), b -> this.displayModConfig()).bounds(6, y, this.listWidth, 20).build();
      y -= 20;
      this.search = new EditBox(this.getFontRenderer(), 7, y, this.listWidth - 2, 14, Component.translatable("fml.menu.mods.search"));
      this.modList = new ModListWidget(this, this.listWidth, fullButtonHeight, this.search.getY() - 9 * 4 - 6);
      this.modList.setX(6);
      this.modInfo = new ModListScreen.InfoPanel(this.minecraft, modInfoWidth, this.height - 6 - fullButtonHeight, 6);
      this.addRenderableWidget(this.modList);
      this.addRenderableWidget(this.modInfo);
      this.addRenderableWidget(this.search);
      this.addRenderableWidget(this.doneButton);
      this.addRenderableWidget(this.configButton);
      this.addRenderableWidget(this.openModsFolderButton);
      this.search.setFocused(false);
      this.search.setCanLoseFocus(true);
      this.configButton.active = false;
      int width = this.listWidth / NUM_BUTTONS;
      int x = 6;
      this.addRenderableWidget(
         ModListScreen.SortType.NORMAL.button = Button.builder(
               ModListScreen.SortType.NORMAL.getButtonText(), b -> this.resortMods(ModListScreen.SortType.NORMAL)
            )
            .bounds(x, 6, width - 1, 20)
            .build()
      );
      x += width + 1;
      this.addRenderableWidget(
         ModListScreen.SortType.A_TO_Z.button = Button.builder(
               ModListScreen.SortType.A_TO_Z.getButtonText(), b -> this.resortMods(ModListScreen.SortType.A_TO_Z)
            )
            .bounds(x, 6, width - 1, 20)
            .build()
      );
      x += width + 1;
      this.addRenderableWidget(
         ModListScreen.SortType.Z_TO_A.button = Button.builder(
               ModListScreen.SortType.Z_TO_A.getButtonText(), b -> this.resortMods(ModListScreen.SortType.Z_TO_A)
            )
            .bounds(x, 6, width - 1, 20)
            .build()
      );
      this.resortMods(ModListScreen.SortType.NORMAL);
      this.updateCache();
   }

   private void displayModConfig() {
      if (this.selected != null) {
         try {
            ConfigScreenHandler.getScreenFactoryFor(this.selected.getInfo())
               .map(f -> f.apply(this.minecraft, this))
               .ifPresent(newScreen -> this.minecraft.setScreen(newScreen));
         } catch (Exception var2) {
            LOGGER.error("There was a critical issue trying to build the config GUI for {}", this.selected.getInfo().getModId(), var2);
         }
      }
   }

   @Override
   public void tick() {
      this.modList.setSelected(this.selected);
      if (!this.search.getValue().equals(this.lastFilterText)) {
         this.reloadMods();
         this.sorted = false;
      }

      if (!this.sorted) {
         this.reloadMods();
         this.mods.sort(this.sortType);
         this.modList.refreshList();
         if (this.selected != null) {
            this.selected = this.modList.children().stream().filter(e -> e.getInfo() == this.selected.getInfo()).findFirst().orElse(null);
            this.updateCache();
         }

         this.sorted = true;
      }
   }

   public <T extends ObjectSelectionList.Entry<T>> void buildModList(Consumer<T> modListViewConsumer, Function<IModInfo, T> newEntry) {
      for (IModInfo mod : this.mods) {
         modListViewConsumer.accept(newEntry.apply(mod));
      }
   }

   private void reloadMods() {
      this.mods = this.unsortedMods
         .stream()
         .filter(mi -> StringUtils.toLowerCase(stripControlCodes(mi.getDisplayName())).contains(StringUtils.toLowerCase(this.search.getValue())))
         .collect(Collectors.toList());
      this.lastFilterText = this.search.getValue();
   }

   private void resortMods(ModListScreen.SortType newSort) {
      this.sortType = newSort;

      for (ModListScreen.SortType sort : ModListScreen.SortType.values()) {
         if (sort.button != null) {
            sort.button.active = this.sortType != sort;
         }
      }

      this.sorted = false;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.modList.render(guiGraphics, mouseX, mouseY, partialTick);
      if (this.modInfo != null) {
         this.modInfo.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      Component text = Component.translatable("fml.menu.mods.search");
      int x = this.modList.getX() + this.modList.getWidth() / 2 - this.getFontRenderer().width(text) / 2;
      this.search.render(guiGraphics, mouseX, mouseY, partialTick);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawString(this.getFontRenderer(), text.getVisualOrderText(), x, this.search.getY() - 9, 16777215, false);
   }

   public Minecraft getMinecraftInstance() {
      return this.minecraft;
   }

   public Font getFontRenderer() {
      return this.font;
   }

   public void setSelected(ModListWidget.ModEntry entry) {
      this.selected = entry == this.selected ? null : entry;
      this.updateCache();
   }

   private void updateCache() {
      if (this.selected == null) {
         this.configButton.active = false;
         this.modInfo.clearInfo();
      } else {
         IModInfo selectedMod = this.selected.getInfo();
         this.configButton.active = ConfigScreenHandler.getScreenFactoryFor(selectedMod).isPresent();
         List<String> lines = new ArrayList<>();
         CheckResult vercheck = VersionChecker.getResult(selectedMod);
         Pair<ResourceLocation, Size2i> logoData = selectedMod.getLogoFile().map(logoFile -> {
            TextureManager tm = this.minecraft.getTextureManager();

            try {
               NativeImage logo = null;
               IModFileInfo modfile = ModList.get().getModFileById(selectedMod.getModId());
               if (modfile != null) {
                  Path path = modfile.getFile().findResource(new String[]{logoFile});
                  if (Files.exists(path)) {
                     logo = NativeImage.read(Files.newInputStream(path));
                  }
               }

               if (logo != null) {
                  return Pair.of(tm.register("modlogo", new DynamicTexture(logo) {
                     @Override
                     public void upload() {
                        this.bind();
                        NativeImage td = this.getPixels();
                        this.getPixels().upload(0, 0, 0, 0, 0, td.getWidth(), td.getHeight(), selectedMod.getLogoBlur(), false, false, false);
                     }
                  }), new Size2i(logo.getWidth(), logo.getHeight()));
               }
            } catch (IOException var7) {
            }

            return Pair.of(null, new Size2i(0, 0));
         }).orElse(Pair.of(null, new Size2i(0, 0)));
         lines.add(selectedMod.getDisplayName());
         lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.version", MavenVersionStringHelper.artifactVersionToString(selectedMod.getVersion())));
         lines.add(
            ForgeI18n.parseMessage(
               "fml.menu.mods.info.idstate",
               selectedMod.getModId(),
               ModList.get()
                  .getModContainerById(selectedMod.getModId())
                  .<ModLoadingStage>map(ModContainer::getCurrentState)
                  .map(Object::toString)
                  .orElse("NONE")
            )
         );
         selectedMod.getConfig().getConfigElement("credits").ifPresent(credits -> lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.credits", credits)));
         selectedMod.getConfig().getConfigElement("authors").ifPresent(authors -> lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.authors", authors)));
         selectedMod.getConfig()
            .getConfigElement("displayURL")
            .ifPresent(displayURL -> lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.displayurl", displayURL)));
         if (selectedMod.getOwningFile() != null && selectedMod.getOwningFile().getMods().size() != 1) {
            lines.add(
               ForgeI18n.parseMessage(
                  "fml.menu.mods.info.childmods",
                  selectedMod.getOwningFile().getMods().stream().<CharSequence>map(IModInfo::getDisplayName).collect(Collectors.joining(","))
               )
            );
         } else {
            lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.nochildmods"));
         }

         if (vercheck.status().isOutdated()) {
            lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.updateavailable", vercheck.url() == null ? "" : vercheck.url()));
         }

         lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.license", ((ModFileInfo)selectedMod.getOwningFile()).getLicense()));
         lines.add(null);
         lines.add(selectedMod.getDescription());
         if (vercheck.status().isOutdated() && !vercheck.changes().isEmpty()) {
            lines.add(null);
            lines.add(ForgeI18n.parseMessage("fml.menu.mods.info.changelogheader"));

            for (Entry<ComparableVersion, String> entry : vercheck.changes().entrySet()) {
               lines.add("  " + entry.getKey() + ":");
               lines.add(entry.getValue());
               lines.add(null);
            }
         }

         this.modInfo.setInfo(lines, (ResourceLocation)logoData.getLeft(), (Size2i)logoData.getRight());
      }
   }

   @Override
   public void resize(Minecraft mc, int width, int height) {
      String s = this.search.getValue();
      ModListScreen.SortType sort = this.sortType;
      ModListWidget.ModEntry selected = this.selected;
      this.init(mc, width, height);
      this.search.setValue(s);
      this.selected = selected;
      if (!this.search.getValue().isEmpty()) {
         this.reloadMods();
      }

      if (sort != ModListScreen.SortType.NORMAL) {
         this.resortMods(sort);
      }

      this.updateCache();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parentScreen);
   }

   class InfoPanel extends ScrollPanel {
      private ResourceLocation logoPath;
      private Size2i logoDims = new Size2i(0, 0);
      private List<FormattedCharSequence> lines = Collections.emptyList();

      InfoPanel(Minecraft mcIn, int widthIn, int heightIn, int topIn) {
         super(mcIn, widthIn, heightIn, topIn, ModListScreen.this.modList.getRight() + 6);
      }

      void setInfo(List<String> lines, ResourceLocation logoPath, Size2i logoDims) {
         this.logoPath = logoPath;
         this.logoDims = logoDims;
         this.lines = this.resizeContent(lines);
      }

      void clearInfo() {
         this.logoPath = null;
         this.logoDims = new Size2i(0, 0);
         this.lines = Collections.emptyList();
      }

      private List<FormattedCharSequence> resizeContent(List<String> lines) {
         List<FormattedCharSequence> ret = new ArrayList<>();

         for (String line : lines) {
            if (line == null) {
               ret.add(null);
            } else {
               Component chat = ForgeHooks.newChatWithLinks(line, false);
               int maxTextLength = this.width - 12;
               if (maxTextLength >= 0) {
                  ret.addAll(Language.getInstance().getVisualOrder(ModListScreen.this.font.getSplitter().splitLines(chat, maxTextLength, Style.EMPTY)));
               }
            }
         }

         return ret;
      }

      @Override
      public int getContentHeight() {
         int height = 50;
         height += this.lines.size() * 9;
         if (height < this.bottom - this.top - 8) {
            height = this.bottom - this.top - 8;
         }

         return height;
      }

      @Override
      protected int getScrollAmount() {
         return 9 * 3;
      }

      @Override
      protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
         if (this.logoPath != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int headerHeight = 50;
            guiGraphics.blitInscribed(
               this.logoPath, this.left + 6, relativeY, this.width - 12, headerHeight, this.logoDims.width(), this.logoDims.height(), false, true
            );
            relativeY += headerHeight + 6;
         }

         for (FormattedCharSequence line : this.lines) {
            if (line != null) {
               RenderSystem.enableBlend();
               guiGraphics.drawString(ModListScreen.this.font, line, this.left + 6, relativeY, 16777215);
               RenderSystem.disableBlend();
            }

            relativeY += 9;
         }

         Style component = this.findTextLine(mouseX, mouseY);
         if (component != null) {
            guiGraphics.renderComponentHoverEffect(ModListScreen.this.font, component, mouseX, mouseY);
         }
      }

      private Style findTextLine(int mouseX, int mouseY) {
         if (!this.isMouseOver((double)mouseX, (double)mouseY)) {
            return null;
         } else {
            double offset = (double)((float)(mouseY - this.top - 6 - this.border) + this.scrollDistance);
            if (this.logoPath != null) {
               offset -= 50.0;
            }

            if (offset <= 0.0) {
               return null;
            } else {
               int lineIdx = (int)(offset / 9.0);
               if (lineIdx < this.lines.size() && lineIdx >= 0) {
                  FormattedCharSequence line = this.lines.get(lineIdx);
                  return line != null ? ModListScreen.this.font.getSplitter().componentStyleAtWidth(line, mouseX - this.left - this.border) : null;
               } else {
                  return null;
               }
            }
         }
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         Style component = this.findTextLine((int)mouseX, (int)mouseY);
         if (component != null) {
            ModListScreen.this.handleComponentClicked(component);
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }

      @Override
      public NarratableEntry.NarrationPriority narrationPriority() {
         return NarratableEntry.NarrationPriority.NONE;
      }

      @Override
      public void updateNarration(NarrationElementOutput p_169152_) {
      }
   }

   private static enum SortType implements Comparator<IModInfo> {
      NORMAL,
      A_TO_Z {
         @Override
         protected int compare(String name1, String name2) {
            return name1.compareTo(name2);
         }
      },
      Z_TO_A {
         @Override
         protected int compare(String name1, String name2) {
            return name2.compareTo(name1);
         }
      };

      Button button;

      protected int compare(String name1, String name2) {
         return 0;
      }

      public int compare(IModInfo o1, IModInfo o2) {
         String name1 = StringUtils.toLowerCase(ModListScreen.stripControlCodes(o1.getDisplayName()));
         String name2 = StringUtils.toLowerCase(ModListScreen.stripControlCodes(o2.getDisplayName()));
         return this.compare(name1, name2);
      }

      Component getButtonText() {
         return Component.translatable("fml.menu.mods." + StringUtils.toLowerCase(this.name()));
      }
   }
}
