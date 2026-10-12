package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
   protected final LevelRenderer levelRenderer;
   protected final Level level;
   protected int sectionGridSizeY;
   protected int sectionGridSizeX;
   protected int sectionGridSizeZ;
   private int viewDistance;
   public SectionRenderDispatcher.RenderSection[] sections;

   public ViewArea(SectionRenderDispatcher pSectionRenderDispatcher, Level pLevel, int pViewDistance, LevelRenderer pLevelRenderer) {
      this.levelRenderer = pLevelRenderer;
      this.level = pLevel;
      this.setViewDistance(pViewDistance);
      this.createSections(pSectionRenderDispatcher);
   }

   protected void createSections(SectionRenderDispatcher pSectionRenderDispatcher) {
      if (!Minecraft.getInstance().isSameThread()) {
         throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
      } else {
         int $$1 = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
         this.sections = new SectionRenderDispatcher.RenderSection[$$1];

         for (int $$2 = 0; $$2 < this.sectionGridSizeX; $$2++) {
            for (int $$3 = 0; $$3 < this.sectionGridSizeY; $$3++) {
               for (int $$4 = 0; $$4 < this.sectionGridSizeZ; $$4++) {
                  int $$5 = this.getSectionIndex($$2, $$3, $$4);
                  this.sections[$$5] = pSectionRenderDispatcher.new RenderSection($$5, $$2 * 16, this.level.getMinBuildHeight() + $$3 * 16, $$4 * 16);
               }
            }
         }
      }
   }

   public void releaseAllBuffers() {
      for (SectionRenderDispatcher.RenderSection $$0 : this.sections) {
         $$0.releaseBuffers();
      }
   }

   private int getSectionIndex(int pX, int pY, int pZ) {
      return (pZ * this.sectionGridSizeY + pY) * this.sectionGridSizeX + pX;
   }

   protected void setViewDistance(int pRenderDistanceChunks) {
      int $$1 = pRenderDistanceChunks * 2 + 1;
      this.sectionGridSizeX = $$1;
      this.sectionGridSizeY = this.level.getSectionsCount();
      this.sectionGridSizeZ = $$1;
      this.viewDistance = pRenderDistanceChunks;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public LevelHeightAccessor getLevelHeightAccessor() {
      return this.level;
   }

   public void repositionCamera(double pViewEntityX, double pViewEntityZ) {
      int $$2 = Mth.ceil(pViewEntityX);
      int $$3 = Mth.ceil(pViewEntityZ);

      for (int $$4 = 0; $$4 < this.sectionGridSizeX; $$4++) {
         int $$5 = this.sectionGridSizeX * 16;
         int $$6 = $$2 - 8 - $$5 / 2;
         int $$7 = $$6 + Math.floorMod($$4 * 16 - $$6, $$5);

         for (int $$8 = 0; $$8 < this.sectionGridSizeZ; $$8++) {
            int $$9 = this.sectionGridSizeZ * 16;
            int $$10 = $$3 - 8 - $$9 / 2;
            int $$11 = $$10 + Math.floorMod($$8 * 16 - $$10, $$9);

            for (int $$12 = 0; $$12 < this.sectionGridSizeY; $$12++) {
               int $$13 = this.level.getMinBuildHeight() + $$12 * 16;
               SectionRenderDispatcher.RenderSection $$14 = this.sections[this.getSectionIndex($$4, $$12, $$8)];
               BlockPos $$15 = $$14.getOrigin();
               if ($$7 != $$15.getX() || $$13 != $$15.getY() || $$11 != $$15.getZ()) {
                  $$14.setOrigin($$7, $$13, $$11);
               }
            }
         }
      }
   }

   public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
      int $$4 = Math.floorMod(pSectionX, this.sectionGridSizeX);
      int $$5 = Math.floorMod(pSectionY - this.level.getMinSection(), this.sectionGridSizeY);
      int $$6 = Math.floorMod(pSectionZ, this.sectionGridSizeZ);
      SectionRenderDispatcher.RenderSection $$7 = this.sections[this.getSectionIndex($$4, $$5, $$6)];
      $$7.setDirty(pReRenderOnMainThread);
   }

   @Nullable
   protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pPos) {
      int $$1 = Mth.floorDiv(pPos.getY() - this.level.getMinBuildHeight(), 16);
      if ($$1 >= 0 && $$1 < this.sectionGridSizeY) {
         int $$2 = Mth.positiveModulo(Mth.floorDiv(pPos.getX(), 16), this.sectionGridSizeX);
         int $$3 = Mth.positiveModulo(Mth.floorDiv(pPos.getZ(), 16), this.sectionGridSizeZ);
         return this.sections[this.getSectionIndex($$2, $$1, $$3)];
      } else {
         return null;
      }
   }
}
