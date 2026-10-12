package net.minecraftforge.client.gui;

import com.google.common.base.Strings;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadingErrorScreen extends ErrorScreen {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Path modsDir;
   private final Path logFile;
   private final List<ModLoadingException> modLoadErrors;
   private final List<ModLoadingWarning> modLoadWarnings;
   private final Path dumpedLocation;
   private LoadingErrorScreen.LoadingEntryList entryList;
   private Component errorHeader;
   private Component warningHeader;

   public LoadingErrorScreen(LoadingFailedException loadingException, List<ModLoadingWarning> warnings, File dumpedLocation) {
      super(Component.literal("Loading Error"), null);
      this.modLoadWarnings = warnings;
      this.modLoadErrors = loadingException == null ? Collections.emptyList() : loadingException.getErrors();
      this.modsDir = FMLPaths.MODSDIR.get();
      this.logFile = FMLPaths.GAMEDIR.get().resolve(Paths.get("logs", "latest.log"));
      this.dumpedLocation = dumpedLocation != null ? dumpedLocation.toPath() : null;
   }

   @Override
   public void init() {
      super.init();
      this.clearWidgets();
      this.errorHeader = Component.literal(
         ChatFormatting.RED + ForgeI18n.parseMessage("fml.loadingerrorscreen.errorheader", this.modLoadErrors.size()) + ChatFormatting.RESET
      );
      this.warningHeader = Component.literal(
         ChatFormatting.YELLOW + ForgeI18n.parseMessage("fml.loadingerrorscreen.warningheader", this.modLoadErrors.size()) + ChatFormatting.RESET
      );
      int yOffset = 46;
      this.addRenderableWidget(
         new ExtendedButton(
            50,
            this.height - yOffset,
            this.width / 2 - 55,
            20,
            Component.literal(ForgeI18n.parseMessage("fml.button.open.mods.folder")),
            b -> Util.getPlatform().openFile(this.modsDir.toFile())
         )
      );
      this.addRenderableWidget(
         new ExtendedButton(
            this.width / 2 + 5,
            this.height - yOffset,
            this.width / 2 - 55,
            20,
            Component.literal(ForgeI18n.parseMessage("fml.button.open.file", this.logFile.getFileName())),
            b -> Util.getPlatform().openFile(this.logFile.toFile())
         )
      );
      if (this.modLoadErrors.isEmpty()) {
         this.addRenderableWidget(
            new ExtendedButton(
               this.width / 4,
               this.height - 24,
               this.width / 2,
               20,
               Component.literal(ForgeI18n.parseMessage("fml.button.continue.launch")),
               b -> this.minecraft.setScreen(null)
            )
         );
      } else {
         this.addRenderableWidget(
            new ExtendedButton(
               this.width / 4,
               this.height - 24,
               this.width / 2,
               20,
               Component.literal(ForgeI18n.parseMessage("fml.button.open.file", this.dumpedLocation.getFileName())),
               b -> Util.getPlatform().openFile(this.dumpedLocation.toFile())
            )
         );
      }

      this.entryList = new LoadingErrorScreen.LoadingEntryList(this, this.modLoadErrors, this.modLoadWarnings);
      this.addWidget(this.entryList);
      this.setFocused(this.entryList);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      this.entryList.render(guiGraphics, mouseX, mouseY, partialTick);
      this.drawMultiLineCenteredString(guiGraphics, this.font, this.modLoadErrors.isEmpty() ? this.warningHeader : this.errorHeader, this.width / 2, 10);
      this.renderables.forEach(button -> button.render(guiGraphics, mouseX, mouseY, partialTick));
   }

   private void drawMultiLineCenteredString(GuiGraphics guiGraphics, Font fr, Component str, int x, int y) {
      for (FormattedCharSequence s : fr.split(str, this.width)) {
         guiGraphics.drawString(fr, s, (float)((double)x - (double)fr.width(s) / 2.0), (float)y, 16777215, true);
         y += 9;
      }
   }

   public static class LoadingEntryList extends ObjectSelectionList<LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry> {
      LoadingEntryList(LoadingErrorScreen parent, List<ModLoadingException> errors, List<ModLoadingWarning> warnings) {
         super(
            Objects.requireNonNull(parent.minecraft),
            parent.width,
            parent.height,
            35,
            Math.max(
                     errors.stream()
                        .mapToInt(error -> parent.font.split(Component.literal(error.getMessage() != null ? error.getMessage() : ""), parent.width - 20).size())
                        .max()
                        .orElse(0),
                     warnings.stream()
                        .mapToInt(
                           warning -> parent.font
                                 .split(Component.literal(warning.formatToString() != null ? warning.formatToString() : ""), parent.width - 20)
                                 .size()
                        )
                        .max()
                        .orElse(0)
                  )
                  * 9
               + 8
         );
         boolean both = !errors.isEmpty() && !warnings.isEmpty();
         if (both) {
            this.addEntry(new LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry(parent.errorHeader, true));
         }

         errors.forEach(e -> this.addEntry(new LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry(Component.literal(e.formatToString()))));
         if (both) {
            int maxChars = (this.width - 10) / parent.minecraft.font.width("-");
            this.addEntry(new LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry(Component.literal("\n" + Strings.repeat("-", maxChars) + "\n")));
            this.addEntry(new LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry(parent.warningHeader, true));
         }

         warnings.forEach(w -> this.addEntry(new LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry(Component.literal(w.formatToString()))));
      }

      @Override
      protected int getScrollbarPosition() {
         return this.getRight() - 6;
      }

      @Override
      public int getRowWidth() {
         return this.width;
      }

      public class LoadingMessageEntry extends ObjectSelectionList.Entry<LoadingErrorScreen.LoadingEntryList.LoadingMessageEntry> {
         private final Component message;
         private final boolean center;

         LoadingMessageEntry(final Component message) {
            this(message, false);
         }

         LoadingMessageEntry(final Component message, final boolean center) {
            this.message = Objects.requireNonNull(message);
            this.center = center;
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.message);
         }

         @Override
         public void render(
            GuiGraphics guiGraphics,
            int entryIdx,
            int top,
            int left,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean p_194999_5_,
            float partialTick
         ) {
            Font font = Minecraft.getInstance().font;
            List<FormattedCharSequence> strings = font.split(this.message, LoadingEntryList.this.width - 20);
            int y = top + 2;

            for (FormattedCharSequence string : strings) {
               if (this.center) {
                  guiGraphics.drawString(
                     font, string, (float)left + (float)LoadingEntryList.this.width / 2.0F - (float)font.width(string) / 2.0F, (float)y, 16777215, false
                  );
               } else {
                  guiGraphics.drawString(font, string, left + 5, y, 16777215, false);
               }

               y += 9;
            }
         }
      }
   }
}
