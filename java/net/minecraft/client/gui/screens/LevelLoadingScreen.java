package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LevelLoadingScreen extends Screen {
   private static final long NARRATION_DELAY_MS = 2000L;
   private final StoringChunkProgressListener progressListener;
   private long lastNarration = -1L;
   private boolean done;
   private static final Object2IntMap<ChunkStatus> COLORS = Util.make(new Object2IntOpenHashMap(), p_280803_ -> {
      p_280803_.defaultReturnValue(0);
      p_280803_.put(ChunkStatus.EMPTY, 5526612);
      p_280803_.put(ChunkStatus.STRUCTURE_STARTS, 10066329);
      p_280803_.put(ChunkStatus.STRUCTURE_REFERENCES, 6250897);
      p_280803_.put(ChunkStatus.BIOMES, 8434258);
      p_280803_.put(ChunkStatus.NOISE, 13750737);
      p_280803_.put(ChunkStatus.SURFACE, 7497737);
      p_280803_.put(ChunkStatus.CARVERS, 3159410);
      p_280803_.put(ChunkStatus.FEATURES, 2213376);
      p_280803_.put(ChunkStatus.INITIALIZE_LIGHT, 13421772);
      p_280803_.put(ChunkStatus.LIGHT, 16769184);
      p_280803_.put(ChunkStatus.SPAWN, 15884384);
      p_280803_.put(ChunkStatus.FULL, 16777215);
   });

   public LevelLoadingScreen(StoringChunkProgressListener pProgressListener) {
      super(GameNarrator.NO_TITLE);
      this.progressListener = pProgressListener;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean shouldNarrateNavigation() {
      return false;
   }

   @Override
   public void removed() {
      this.done = true;
      this.triggerImmediateNarration(true);
   }

   @Override
   protected void updateNarratedWidget(NarrationElementOutput pNarrationElementOutput) {
      if (this.done) {
         pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narrator.loading.done"));
      } else {
         pNarrationElementOutput.add(NarratedElementType.TITLE, this.getFormattedProgress());
      }
   }

   private Component getFormattedProgress() {
      return Component.translatable("loading.progress", Mth.clamp(this.progressListener.getProgress(), 0, 100));
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      long $$4 = Util.getMillis();
      if ($$4 - this.lastNarration > 2000L) {
         this.lastNarration = $$4;
         this.triggerImmediateNarration(true);
      }

      int $$5 = this.width / 2;
      int $$6 = this.height / 2;
      renderChunks(pGuiGraphics, this.progressListener, $$5, $$6, 2, 0);
      int $$7 = this.progressListener.getDiameter() + 9 + 2;
      pGuiGraphics.drawCenteredString(this.font, this.getFormattedProgress(), $$5, $$6 - $$7, 16777215);
   }

   public static void renderChunks(GuiGraphics pGuiGraphics, StoringChunkProgressListener pProgressListener, int pX, int pY, int p_96154_, int p_96155_) {
      int $$6 = p_96154_ + p_96155_;
      int $$7 = pProgressListener.getFullDiameter();
      int $$8 = $$7 * $$6 - p_96155_;
      int $$9 = pProgressListener.getDiameter();
      int $$10 = $$9 * $$6 - p_96155_;
      int $$11 = pX - $$10 / 2;
      int $$12 = pY - $$10 / 2;
      int $$13 = $$8 / 2 + 1;
      int $$14 = -16772609;
      pGuiGraphics.drawManaged(() -> {
         if (p_96155_ != 0) {
            pGuiGraphics.fill(pX - $$13, pY - $$13, pX - $$13 + 1, pY + $$13, -16772609);
            pGuiGraphics.fill(pX + $$13 - 1, pY - $$13, pX + $$13, pY + $$13, -16772609);
            pGuiGraphics.fill(pX - $$13, pY - $$13, pX + $$13, pY - $$13 + 1, -16772609);
            pGuiGraphics.fill(pX - $$13, pY + $$13 - 1, pX + $$13, pY + $$13, -16772609);
         }

         for (int $$11x = 0; $$11x < $$9; $$11x++) {
            for (int $$12x = 0; $$12x < $$9; $$12x++) {
               ChunkStatus $$13x = pProgressListener.getStatus($$11x, $$12x);
               int $$14x = $$11 + $$11x * $$6;
               int $$15 = $$12 + $$12x * $$6;
               pGuiGraphics.fill($$14x, $$15, $$14x + p_96154_, $$15 + p_96154_, COLORS.getInt($$13x) | 0xFF000000);
            }
         }
      });
   }
}
