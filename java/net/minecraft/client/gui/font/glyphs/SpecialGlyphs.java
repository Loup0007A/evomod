package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum SpecialGlyphs implements GlyphInfo {
   WHITE(() -> generate(5, 8, (p_232613_, p_232614_) -> -1)),
   MISSING(() -> {
      int $$0 = 5;
      int $$1 = 8;
      return generate(5, 8, (p_232606_, p_232607_) -> {
         boolean $$2 = p_232606_ == 0 || p_232606_ + 1 == 5 || p_232607_ == 0 || p_232607_ + 1 == 8;
         return $$2 ? -1 : 0;
      });
   });

   final NativeImage image;

   private static NativeImage generate(int pWidth, int pHeight, SpecialGlyphs.PixelProvider pPixelProvider) {
      NativeImage $$3 = new NativeImage(NativeImage.Format.RGBA, pWidth, pHeight, false);

      for (int $$4 = 0; $$4 < pHeight; $$4++) {
         for (int $$5 = 0; $$5 < pWidth; $$5++) {
            $$3.setPixelRGBA($$5, $$4, pPixelProvider.getColor($$5, $$4));
         }
      }

      $$3.untrack();
      return $$3;
   }

   private SpecialGlyphs(final Supplier<NativeImage> pImage) {
      this.image = pImage.get();
   }

   @Override
   public float getAdvance() {
      return (float)(this.image.getWidth() + 1);
   }

   @Override
   public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> pGlyphProvider) {
      return pGlyphProvider.apply(new SheetGlyphInfo() {
         @Override
         public int getPixelWidth() {
            return SpecialGlyphs.this.image.getWidth();
         }

         @Override
         public int getPixelHeight() {
            return SpecialGlyphs.this.image.getHeight();
         }

         @Override
         public float getOversample() {
            return 1.0F;
         }

         @Override
         public void upload(int p_232629_, int p_232630_) {
            SpecialGlyphs.this.image.upload(0, p_232629_, p_232630_, false);
         }

         @Override
         public boolean isColored() {
            return true;
         }
      });
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface PixelProvider {
      int getColor(int pX, int pY);
   }
}
