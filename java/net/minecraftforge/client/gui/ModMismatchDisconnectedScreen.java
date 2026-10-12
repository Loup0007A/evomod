package net.minecraftforge.client.gui;

import com.mojang.blaze3d.vertex.Tesselator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.network.packets.ModVersions;
import org.apache.commons.lang3.tuple.Pair;

public class ModMismatchDisconnectedScreen extends Screen {
   private final Component reason;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Screen parent;
   private int textHeight;
   private final Path modsDir;
   private final Path logFile;
   private final int listHeight;
   private final NetworkContext.NetworkMismatchData data;
   private final boolean hasMismatches;
   private final List<String> allModIds;
   private final Map<String, String> presentModUrls;

   public ModMismatchDisconnectedScreen(Screen parentScreen, Component title, Component reason, NetworkContext.NetworkMismatchData data) {
      super(title);
      this.parent = parentScreen;
      this.reason = reason;
      this.modsDir = FMLPaths.MODSDIR.get();
      this.logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"));
      this.data = data;
      this.hasMismatches = !data.mismatched().isEmpty() || !data.missing().isEmpty();
      this.listHeight = this.hasMismatches ? 140 : 0;
      this.allModIds = data.mods().keySet().stream().distinct().collect(Collectors.toList());
      this.presentModUrls = ModList.get()
         .getMods()
         .stream()
         .filter(info -> this.allModIds.contains(info.getModId()))
         .map(info -> Pair.of(info.getModId(), info.getConfig().getConfigElement("displayURL").orElse("")))
         .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
   }

   @Override
   protected void init() {
      this.message = MultiLineLabel.create(this.font, this.reason, this.width - 50);
      this.textHeight = this.message.getLineCount() * 9;
      int listLeft = Math.max(8, this.width / 2 - 220);
      int listWidth = Math.min(440, this.width - 16);
      int upperButtonHeight = Math.min((this.height + this.listHeight + this.textHeight) / 2 + 10, this.height - 50);
      int lowerButtonHeight = Math.min((this.height + this.listHeight + this.textHeight) / 2 + 35, this.height - 25);
      if (this.hasMismatches) {
         this.addRenderableWidget(
            new ModMismatchDisconnectedScreen.MismatchInfoPanel(this.minecraft, listWidth, this.listHeight, (this.height - this.listHeight) / 2, listLeft)
         );
      }

      int buttonWidth = Math.min(210, this.width / 2 - 20);
      this.addRenderableWidget(
         Button.builder(
               Component.literal(ForgeI18n.parseMessage("fml.button.open.file", this.logFile.getFileName())),
               button -> Util.getPlatform().openFile(this.logFile.toFile())
            )
            .bounds(Math.max(this.width / 4 - buttonWidth / 2, listLeft), upperButtonHeight, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal(ForgeI18n.parseMessage("fml.button.open.mods.folder")), button -> Util.getPlatform().openFile(this.modsDir.toFile()))
            .bounds(Math.min(this.width * 3 / 4 - buttonWidth / 2, listLeft + listWidth - buttonWidth), upperButtonHeight, buttonWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.toMenu"), button -> this.minecraft.setScreen(this.parent))
            .bounds((this.width - buttonWidth) / 2, lowerButtonHeight, buttonWidth, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
      int textYOffset = this.hasMismatches ? 18 : 0;
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, (this.height - this.listHeight - this.textHeight) / 2 - textYOffset - 18, 11184810);
      this.message.renderCentered(guiGraphics, this.width / 2, (this.height - this.listHeight - this.textHeight) / 2 - textYOffset);
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
   }

   class MismatchInfoPanel extends ScrollPanel {
      private final List<Pair<FormattedCharSequence, Pair<FormattedCharSequence, FormattedCharSequence>>> lineTable;
      private final int contentSize;
      private final int nameIndent = 10;
      private final int tableWidth = this.width - this.border * 2 - 6 - 10;
      private final int nameWidth = this.tableWidth * 3 / 5;
      private final int versionWidth = (this.tableWidth - this.nameWidth) / 2;

      public MismatchInfoPanel(Minecraft client, int width, int height, int top, int left) {
         super(client, width, height, top, left);
         List<Pair<MutableComponent, Pair<String, String>>> rawTable = new ArrayList<>();
         if (!ModMismatchDisconnectedScreen.this.data.missing().isEmpty()) {
            rawTable.add(
               Pair.of(
                  Component.literal(
                        ForgeI18n.parseMessage(
                           ModMismatchDisconnectedScreen.this.data.fromServer()
                              ? "fml.modmismatchscreen.missingmods.server"
                              : "fml.modmismatchscreen.missingmods.client"
                        )
                     )
                     .withStyle(ChatFormatting.GRAY),
                  null
               )
            );
            rawTable.add(
               Pair.of(
                  Component.literal(ForgeI18n.parseMessage("fml.modmismatchscreen.table.modname")).withStyle(ChatFormatting.UNDERLINE),
                  Pair.of(
                     "",
                     ForgeI18n.parseMessage(
                        ModMismatchDisconnectedScreen.this.data.fromServer() ? "fml.modmismatchscreen.table.youhave" : "fml.modmismatchscreen.table.youneed"
                     )
                  )
               )
            );
            int i = 0;

            for (ResourceLocation mod : ModMismatchDisconnectedScreen.this.data.missing()) {
               rawTable.add(
                  Pair.of(
                     this.toModNameComponent(mod, ModMismatchDisconnectedScreen.this.data.mods().get(mod.getNamespace()).name(), i),
                     Pair.of("", ModMismatchDisconnectedScreen.this.data.mods().getOrDefault(mod, new ModVersions.Info("", "")).version())
                  )
               );
               if (++i >= 10) {
                  rawTable.add(
                     Pair.of(
                        Component.literal(
                              ForgeI18n.parseMessage("fml.modmismatchscreen.additional", ModMismatchDisconnectedScreen.this.data.missing().size() - i)
                           )
                           .withStyle(ChatFormatting.ITALIC),
                        Pair.of("", "")
                     )
                  );
                  break;
               }
            }

            rawTable.add(Pair.of(Component.literal(" "), null));
         }

         if (!ModMismatchDisconnectedScreen.this.data.mismatched().isEmpty()) {
            rawTable.add(Pair.of(Component.literal(ForgeI18n.parseMessage("fml.modmismatchscreen.mismatchedmods")).withStyle(ChatFormatting.GRAY), null));
            rawTable.add(
               Pair.of(
                  Component.literal(ForgeI18n.parseMessage("fml.modmismatchscreen.table.modname")).withStyle(ChatFormatting.UNDERLINE),
                  Pair.of(
                     ForgeI18n.parseMessage(
                        ModMismatchDisconnectedScreen.this.data.fromServer() ? "fml.modmismatchscreen.table.youhave" : "fml.modmismatchscreen.table.serverhas"
                     ),
                     ForgeI18n.parseMessage(
                        ModMismatchDisconnectedScreen.this.data.fromServer() ? "fml.modmismatchscreen.table.serverhas" : "fml.modmismatchscreen.table.youhave"
                     )
                  )
               )
            );
            int i = 0;

            for (Entry<ResourceLocation, NetworkContext.NetworkMismatchData.Version> modData : ModMismatchDisconnectedScreen.this.data.mismatched().entrySet()) {
               rawTable.add(
                  Pair.of(
                     this.toModNameComponent(modData.getKey(), ModMismatchDisconnectedScreen.this.data.mods().get(modData.getKey().getNamespace()).name(), i),
                     Pair.of(
                        ModMismatchDisconnectedScreen.this.data.mods().getOrDefault(modData.getKey(), new ModVersions.Info("", "")).version(),
                        modData.getValue().received()
                     )
                  )
               );
               if (++i >= 10) {
                  rawTable.add(
                     Pair.of(
                        Component.literal(
                              ForgeI18n.parseMessage("fml.modmismatchscreen.additional", ModMismatchDisconnectedScreen.this.data.mismatched().size() - i)
                           )
                           .withStyle(ChatFormatting.ITALIC),
                        Pair.of("", "")
                     )
                  );
                  break;
               }
            }

            rawTable.add(Pair.of(Component.literal(" "), null));
         }

         this.lineTable = rawTable.stream()
            .flatMap(p -> this.splitLineToWidth((MutableComponent)p.getKey(), (Pair<String, String>)p.getValue()).stream())
            .collect(Collectors.toList());
         this.contentSize = this.lineTable.size();
      }

      private List<Pair<FormattedCharSequence, Pair<FormattedCharSequence, FormattedCharSequence>>> splitLineToWidth(
         MutableComponent name, Pair<String, String> versions
      ) {
         Style style = name.getStyle();
         int versionColumns = versions == null ? 0 : (((String)versions.getLeft()).isEmpty() ? (((String)versions.getRight()).isEmpty() ? 0 : 1) : 2);
         int adaptedNameWidth = this.nameWidth + this.versionWidth * (2 - versionColumns) - 4;
         List<FormattedCharSequence> nameLines = ModMismatchDisconnectedScreen.this.font.split(name, adaptedNameWidth);
         List<FormattedCharSequence> clientVersionLines = ModMismatchDisconnectedScreen.this.font
            .split(Component.literal(versions != null ? (String)versions.getLeft() : "").setStyle(style), this.versionWidth - 4);
         List<FormattedCharSequence> serverVersionLines = ModMismatchDisconnectedScreen.this.font
            .split(Component.literal(versions != null ? (String)versions.getRight() : "").setStyle(style), this.versionWidth - 4);
         List<Pair<FormattedCharSequence, Pair<FormattedCharSequence, FormattedCharSequence>>> splitLines = new ArrayList<>();
         int rowsOccupied = Math.max(nameLines.size(), Math.max(clientVersionLines.size(), serverVersionLines.size()));

         for (int i = 0; i < rowsOccupied; i++) {
            splitLines.add(
               Pair.of(
                  i < nameLines.size() ? nameLines.get(i) : FormattedCharSequence.EMPTY,
                  versions == null
                     ? null
                     : Pair.of(
                        i < clientVersionLines.size() ? clientVersionLines.get(i) : FormattedCharSequence.EMPTY,
                        i < serverVersionLines.size() ? serverVersionLines.get(i) : FormattedCharSequence.EMPTY
                     )
               )
            );
         }

         return splitLines;
      }

      private MutableComponent toModNameComponent(ResourceLocation id, String modName, int color) {
         String modId = id.getNamespace();
         String tooltipId = id.getPath().isEmpty() ? id.getNamespace() : id.toString();
         return Component.literal(modName)
            .withStyle(color % 2 == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW)
            .withStyle(
               s -> s.withHoverEvent(
                     new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.literal(
                           tooltipId
                              + (
                                 !ModMismatchDisconnectedScreen.this.presentModUrls.getOrDefault(modId, "").isEmpty()
                                    ? "\n" + ForgeI18n.parseMessage("fml.modmismatchscreen.homepage")
                                    : ""
                              )
                        )
                     )
                  )
            )
            .withStyle(
               s -> s.withClickEvent(
                     !ModMismatchDisconnectedScreen.this.presentModUrls.getOrDefault(modId, "").isEmpty()
                        ? new ClickEvent(ClickEvent.Action.OPEN_URL, ModMismatchDisconnectedScreen.this.presentModUrls.get(modId))
                        : null
                  )
            );
      }

      @Override
      protected int getContentHeight() {
         int height = this.contentSize * (9 + 3);
         if (height < this.bottom - this.top - 4) {
            height = this.bottom - this.top - 4;
         }

         return height;
      }

      @Override
      protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
         int i = 0;

         for (Pair<FormattedCharSequence, Pair<FormattedCharSequence, FormattedCharSequence>> line : this.lineTable) {
            FormattedCharSequence name = (FormattedCharSequence)line.getLeft();
            Pair<FormattedCharSequence, FormattedCharSequence> versions = (Pair<FormattedCharSequence, FormattedCharSequence>)line.getRight();
            int color = Optional.ofNullable(ModMismatchDisconnectedScreen.this.font.getSplitter().componentStyleAtWidth(name, 0))
               .map(Style::getColor)
               .map(TextColor::getValue)
               .orElse(16777215);
            int nameLeft = this.left + this.border + (versions == null ? 0 : 10);
            guiGraphics.drawString(ModMismatchDisconnectedScreen.this.font, name, nameLeft, relativeY + i * 12, color, false);
            if (versions != null) {
               guiGraphics.drawString(
                  ModMismatchDisconnectedScreen.this.font,
                  (FormattedCharSequence)versions.getLeft(),
                  this.left + this.border + 10 + this.nameWidth,
                  relativeY + i * 12,
                  color,
                  false
               );
               guiGraphics.drawString(
                  ModMismatchDisconnectedScreen.this.font,
                  (FormattedCharSequence)versions.getRight(),
                  this.left + this.border + 10 + this.nameWidth + this.versionWidth,
                  relativeY + i * 12,
                  color,
                  false
               );
            }

            i++;
         }
      }

      @Override
      public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
         super.render(guiGraphics, mouseX, mouseY, partialTicks);
         Style style = this.getComponentStyleAt((double)mouseX, (double)mouseY);
         if (style != null && style.getHoverEvent() != null) {
            guiGraphics.renderComponentHoverEffect(ModMismatchDisconnectedScreen.this.font, style, mouseX, mouseY);
         }
      }

      public Style getComponentStyleAt(double x, double y) {
         if (this.isMouseOver(x, y)) {
            double relativeY = y - (double)this.top + (double)this.scrollDistance - (double)this.border;
            int slotIndex = (int)(relativeY + (double)(this.border / 2)) / 12;
            if (slotIndex < this.contentSize) {
               double relativeX = x - (double)this.left - (double)this.border - (double)(this.lineTable.get(slotIndex).getRight() == null ? 0 : 10);
               if (relativeX >= 0.0) {
                  return ModMismatchDisconnectedScreen.this.font
                     .getSplitter()
                     .componentStyleAtWidth((FormattedCharSequence)this.lineTable.get(slotIndex).getLeft(), (int)relativeX);
               }
            }
         }

         return null;
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         Style style = this.getComponentStyleAt(mouseX, mouseY);
         if (style != null) {
            ModMismatchDisconnectedScreen.this.handleComponentClicked(style);
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
      public void updateNarration(NarrationElementOutput output) {
      }
   }
}
