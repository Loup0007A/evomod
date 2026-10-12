package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OptimizeWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = Util.make(new Reference2IntOpenHashMap(), p_308238_ -> {
      p_308238_.put(Level.OVERWORLD, -13408734);
      p_308238_.put(Level.NETHER, -10075085);
      p_308238_.put(Level.END, -8943531);
      p_308238_.defaultReturnValue(-2236963);
   });
   private final BooleanConsumer callback;
   private final WorldUpgrader upgrader;

   @Nullable
   public static OptimizeWorldScreen create(
      Minecraft pMinecraft, BooleanConsumer pCallback, DataFixer pDataFixer, LevelStorageSource.LevelStorageAccess pLevelStorage, boolean pEraseCache
   ) {
      try {
         WorldOpenFlows $$5 = pMinecraft.createWorldOpenFlows();
         PackRepository $$6 = ServerPacksSource.createPackRepository(pLevelStorage);

         OptimizeWorldScreen var10;
         try (WorldStem $$7 = $$5.loadWorldStem(pLevelStorage.getDataTag(), false, $$6)) {
            WorldData $$8 = $$7.worldData();
            RegistryAccess.Frozen $$9 = $$7.registries().compositeAccess();
            pLevelStorage.saveDataTag($$9, $$8);
            var10 = new OptimizeWorldScreen(pCallback, pDataFixer, pLevelStorage, $$8.getLevelSettings(), pEraseCache, $$9);
         }

         return var10;
      } catch (Exception var13) {
         LOGGER.warn("Failed to load datapacks, can't optimize world", var13);
         return null;
      }
   }

   private OptimizeWorldScreen(
      BooleanConsumer pCallback,
      DataFixer pDataFixer,
      LevelStorageSource.LevelStorageAccess pLevelStorage,
      LevelSettings pLevelSettings,
      boolean pEraseCache,
      RegistryAccess pRegistryAccess
   ) {
      super(Component.translatable("optimizeWorld.title", pLevelSettings.levelName()));
      this.callback = pCallback;
      this.upgrader = new WorldUpgrader(pLevelStorage, pDataFixer, pRegistryAccess, pEraseCache, false);
   }

   @Override
   protected void init() {
      super.init();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_101322_ -> {
         this.upgrader.cancel();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
   }

   @Override
   public void tick() {
      if (this.upgrader.isFinished()) {
         this.callback.accept(true);
      }
   }

   @Override
   public void onClose() {
      this.callback.accept(false);
   }

   @Override
   public void removed() {
      this.upgrader.cancel();
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
      int $$4 = this.width / 2 - 150;
      int $$5 = this.width / 2 + 150;
      int $$6 = this.height / 4 + 100;
      int $$7 = $$6 + 10;
      pGuiGraphics.drawCenteredString(this.font, this.upgrader.getStatus(), this.width / 2, $$6 - 9 - 2, 10526880);
      if (this.upgrader.getTotalChunks() > 0) {
         pGuiGraphics.fill($$4 - 1, $$6 - 1, $$5 + 1, $$7 + 1, -16777216);
         pGuiGraphics.drawString(this.font, Component.translatable("optimizeWorld.info.converted", this.upgrader.getConverted()), $$4, 40, 10526880);
         pGuiGraphics.drawString(this.font, Component.translatable("optimizeWorld.info.skipped", this.upgrader.getSkipped()), $$4, 40 + 9 + 3, 10526880);
         pGuiGraphics.drawString(this.font, Component.translatable("optimizeWorld.info.total", this.upgrader.getTotalChunks()), $$4, 40 + (9 + 3) * 2, 10526880);
         int $$8 = 0;

         for (ResourceKey<Level> $$9 : this.upgrader.levels()) {
            int $$10 = Mth.floor(this.upgrader.dimensionProgress($$9) * (float)($$5 - $$4));
            pGuiGraphics.fill($$4 + $$8, $$6, $$4 + $$8 + $$10, $$7, DIMENSION_COLORS.applyAsInt($$9));
            $$8 += $$10;
         }

         int $$11 = this.upgrader.getConverted() + this.upgrader.getSkipped();
         Component $$12 = Component.translatable("optimizeWorld.progress.counter", $$11, this.upgrader.getTotalChunks());
         Component $$13 = Component.translatable("optimizeWorld.progress.percentage", Mth.floor(this.upgrader.getProgress() * 100.0F));
         pGuiGraphics.drawCenteredString(this.font, $$12, this.width / 2, $$6 + 2 * 9 + 2, 10526880);
         pGuiGraphics.drawCenteredString(this.font, $$13, this.width / 2, $$6 + ($$7 - $$6) / 2 - 9 / 2, 10526880);
      }
   }
}
