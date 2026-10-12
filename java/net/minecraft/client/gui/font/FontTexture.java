package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontTexture extends AbstractTexture implements Dumpable {
   private static final int SIZE = 256;
   private final GlyphRenderTypes renderTypes;
   private final boolean colored;
   private final FontTexture.Node root;

   public FontTexture(GlyphRenderTypes pRenderTypes, boolean pColored) {
      this.colored = pColored;
      this.root = new FontTexture.Node(0, 0, 256, 256);
      TextureUtil.prepareImage(pColored ? NativeImage.InternalGlFormat.RGBA : NativeImage.InternalGlFormat.RED, this.getId(), 256, 256);
      this.renderTypes = pRenderTypes;
   }

   @Override
   public void load(ResourceManager pManager) {
   }

   @Override
   public void close() {
      this.releaseId();
   }

   @Nullable
   public BakedGlyph add(SheetGlyphInfo pGlyphInfo) {
      if (pGlyphInfo.isColored() != this.colored) {
         return null;
      } else {
         FontTexture.Node $$1 = this.root.insert(pGlyphInfo);
         if ($$1 != null) {
            this.bind();
            pGlyphInfo.upload($$1.x, $$1.y);
            float $$2 = 256.0F;
            float $$3 = 256.0F;
            float $$4 = 0.01F;
            return new BakedGlyph(
               this.renderTypes,
               ((float)$$1.x + 0.01F) / 256.0F,
               ((float)$$1.x - 0.01F + (float)pGlyphInfo.getPixelWidth()) / 256.0F,
               ((float)$$1.y + 0.01F) / 256.0F,
               ((float)$$1.y - 0.01F + (float)pGlyphInfo.getPixelHeight()) / 256.0F,
               pGlyphInfo.getLeft(),
               pGlyphInfo.getRight(),
               pGlyphInfo.getTop(),
               pGlyphInfo.getBottom()
            );
         } else {
            return null;
         }
      }
   }

   @Override
   public void dumpContents(ResourceLocation pResourceLocation, Path pPath) {
      String $$2 = pResourceLocation.toDebugFileName();
      TextureUtil.writeAsPNG(pPath, $$2, this.getId(), 0, 256, 256, p_285145_ -> (p_285145_ & 0xFF000000) == 0 ? -16777216 : p_285145_);
   }

   @OnlyIn(Dist.CLIENT)
   static class Node {
      final int x;
      final int y;
      private final int width;
      private final int height;
      @Nullable
      private FontTexture.Node left;
      @Nullable
      private FontTexture.Node right;
      private boolean occupied;

      Node(int pX, int pY, int pWidth, int pHeight) {
         this.x = pX;
         this.y = pY;
         this.width = pWidth;
         this.height = pHeight;
      }

      @Nullable
      FontTexture.Node insert(SheetGlyphInfo pGlyphInfo) {
         if (this.left != null && this.right != null) {
            FontTexture.Node $$1 = this.left.insert(pGlyphInfo);
            if ($$1 == null) {
               $$1 = this.right.insert(pGlyphInfo);
            }

            return $$1;
         } else if (this.occupied) {
            return null;
         } else {
            int $$2 = pGlyphInfo.getPixelWidth();
            int $$3 = pGlyphInfo.getPixelHeight();
            if ($$2 > this.width || $$3 > this.height) {
               return null;
            } else if ($$2 == this.width && $$3 == this.height) {
               this.occupied = true;
               return this;
            } else {
               int $$4 = this.width - $$2;
               int $$5 = this.height - $$3;
               if ($$4 > $$5) {
                  this.left = new FontTexture.Node(this.x, this.y, $$2, this.height);
                  this.right = new FontTexture.Node(this.x + $$2 + 1, this.y, this.width - $$2 - 1, this.height);
               } else {
                  this.left = new FontTexture.Node(this.x, this.y, this.width, $$3);
                  this.right = new FontTexture.Node(this.x, this.y + $$3 + 1, this.width, this.height - $$3 - 1);
               }

               return this.left.insert(pGlyphInfo);
            }
         }
      }
   }
}
