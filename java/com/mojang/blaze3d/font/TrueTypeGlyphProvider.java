package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@OnlyIn(Dist.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
   @Nullable
   private ByteBuffer fontMemory;
   @Nullable
   private FT_Face face;
   final float oversample;
   private final IntSet skip = new IntArraySet();

   public TrueTypeGlyphProvider(ByteBuffer pFontMemory, FT_Face pFace, float pSize, float pOversample, float pShiftX, float pShiftY, String pSkip) {
      this.fontMemory = pFontMemory;
      this.face = pFace;
      this.oversample = pOversample;
      pSkip.codePoints().forEach(this.skip::add);
      int $$7 = Math.round(pSize * pOversample);
      FreeType.FT_Set_Pixel_Sizes(pFace, $$7, $$7);
      float $$8 = pShiftX * pOversample;
      float $$9 = -pShiftY * pOversample;
      MemoryStack $$10 = MemoryStack.stackPush();

      try {
         FT_Vector $$11 = FreeTypeUtil.setVector(FT_Vector.malloc($$10), $$8, $$9);
         FreeType.FT_Set_Transform(pFace, null, $$11);
      } catch (Throwable var15) {
         if ($$10 != null) {
            try {
               $$10.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if ($$10 != null) {
         $$10.close();
      }
   }

   @Nullable
   @Override
   public GlyphInfo getGlyph(int pCharacter) {
      FT_Face $$1 = this.validateFontOpen();
      if (this.skip.contains(pCharacter)) {
         return null;
      } else {
         int $$2 = FreeType.FT_Get_Char_Index($$1, (long)pCharacter);
         if ($$2 == 0) {
            return null;
         } else {
            FreeTypeUtil.assertError(FreeType.FT_Load_Glyph($$1, $$2, 4194312), "Loading glyph");
            FT_GlyphSlot $$3 = Objects.requireNonNull($$1.glyph(), "Glyph not initialized");
            float $$4 = FreeTypeUtil.x($$3.advance());
            FT_Bitmap $$5 = $$3.bitmap();
            int $$6 = $$3.bitmap_left();
            int $$7 = $$3.bitmap_top();
            int $$8 = $$5.width();
            int $$9 = $$5.rows();
            return (GlyphInfo)($$8 > 0 && $$9 > 0 ? new TrueTypeGlyphProvider.Glyph((float)$$6, (float)$$7, $$8, $$9, $$4, $$2) : () -> $$4 / this.oversample);
         }
      }
   }

   FT_Face validateFontOpen() {
      if (this.fontMemory != null && this.face != null) {
         return this.face;
      } else {
         throw new IllegalStateException("Provider already closed");
      }
   }

   @Override
   public void close() {
      if (this.face != null) {
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
         }

         this.face = null;
      }

      MemoryUtil.memFree(this.fontMemory);
      this.fontMemory = null;
   }

   @Override
   public IntSet getSupportedGlyphs() {
      FT_Face $$0 = this.validateFontOpen();
      IntSet $$1 = new IntOpenHashSet();
      MemoryStack $$2 = MemoryStack.stackPush();

      try {
         IntBuffer $$3 = $$2.mallocInt(1);

         for (long $$4 = FreeType.FT_Get_First_Char($$0, $$3); $$3.get(0) != 0; $$4 = FreeType.FT_Get_Next_Char($$0, $$4, $$3)) {
            $$1.add((int)$$4);
         }
      } catch (Throwable var8) {
         if ($$2 != null) {
            try {
               $$2.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if ($$2 != null) {
         $$2.close();
      }

      $$1.removeAll(this.skip);
      return $$1;
   }

   @OnlyIn(Dist.CLIENT)
   class Glyph implements GlyphInfo {
      final int width;
      final int height;
      final float bearingX;
      final float bearingY;
      private final float advance;
      final int index;

      Glyph(final float pBearingX, final float pBearingY, final int pWidth, final int pHeight, final float pAdvance, final int pIndex) {
         this.width = pWidth;
         this.height = pHeight;
         this.advance = pAdvance / TrueTypeGlyphProvider.this.oversample;
         this.bearingX = pBearingX / TrueTypeGlyphProvider.this.oversample;
         this.bearingY = pBearingY / TrueTypeGlyphProvider.this.oversample;
         this.index = pIndex;
      }

      @Override
      public float getAdvance() {
         return this.advance;
      }

      @Override
      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> pGlyphProvider) {
         return pGlyphProvider.apply(new SheetGlyphInfo() {
            @Override
            public int getPixelWidth() {
               return Glyph.this.width;
            }

            @Override
            public int getPixelHeight() {
               return Glyph.this.height;
            }

            @Override
            public float getOversample() {
               return TrueTypeGlyphProvider.this.oversample;
            }

            @Override
            public float getBearingLeft() {
               return Glyph.this.bearingX;
            }

            @Override
            public float getBearingTop() {
               return Glyph.this.bearingY;
            }

            @Override
            public void upload(int p_231126_, int p_231127_) {
               FT_Face $$2 = TrueTypeGlyphProvider.this.validateFontOpen();
               NativeImage $$3 = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false);
               if ($$3.copyFromFont($$2, Glyph.this.index)) {
                  $$3.upload(0, p_231126_, p_231127_, 0, 0, Glyph.this.width, Glyph.this.height, false, true);
               } else {
                  $$3.close();
               }
            }

            @Override
            public boolean isColored() {
               return false;
            }
         });
      }
   }
}
