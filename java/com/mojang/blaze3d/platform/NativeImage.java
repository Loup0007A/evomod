package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.FastColor;
import net.minecraft.util.PngInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class NativeImage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
      StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   );
   private final NativeImage.Format format;
   private final int width;
   private final int height;
   private final boolean useStbFree;
   private long pixels;
   private final long size;

   public NativeImage(int pWidth, int pHeight, boolean pUseCalloc) {
      this(NativeImage.Format.RGBA, pWidth, pHeight, pUseCalloc);
   }

   public NativeImage(NativeImage.Format pFormat, int pWidth, int pHeight, boolean pUseCalloc) {
      if (pWidth > 0 && pHeight > 0) {
         this.format = pFormat;
         this.width = pWidth;
         this.height = pHeight;
         this.size = (long)pWidth * (long)pHeight * (long)pFormat.components();
         this.useStbFree = false;
         if (pUseCalloc) {
            this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
         } else {
            this.pixels = MemoryUtil.nmemAlloc(this.size);
         }

         if (this.pixels == 0L) {
            throw new IllegalStateException("Unable to allocate texture of size " + pWidth + "x" + pHeight + " (" + pFormat.components() + " channels)");
         }
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + pWidth + "x" + pHeight);
      }
   }

   private NativeImage(NativeImage.Format pFormat, int pWidth, int pHeight, boolean pUseStbFree, long pPixels) {
      if (pWidth > 0 && pHeight > 0) {
         this.format = pFormat;
         this.width = pWidth;
         this.height = pHeight;
         this.useStbFree = pUseStbFree;
         this.pixels = pPixels;
         this.size = (long)pWidth * (long)pHeight * (long)pFormat.components();
      } else {
         throw new IllegalArgumentException("Invalid texture size: " + pWidth + "x" + pHeight);
      }
   }

   @Override
   public String toString() {
      return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
   }

   private boolean isOutsideBounds(int pX, int pY) {
      return pX < 0 || pX >= this.width || pY < 0 || pY >= this.height;
   }

   public static NativeImage read(InputStream pTextureStream) throws IOException {
      return read(NativeImage.Format.RGBA, pTextureStream);
   }

   public static NativeImage read(@Nullable NativeImage.Format pFormat, InputStream pTextureStream) throws IOException {
      ByteBuffer $$2 = null;

      NativeImage var3;
      try {
         $$2 = TextureUtil.readResource(pTextureStream);
         $$2.rewind();
         var3 = read(pFormat, $$2);
      } finally {
         MemoryUtil.memFree($$2);
         IOUtils.closeQuietly(pTextureStream);
      }

      return var3;
   }

   public static NativeImage read(ByteBuffer pTextureData) throws IOException {
      return read(NativeImage.Format.RGBA, pTextureData);
   }

   public static NativeImage read(byte[] pBytes) throws IOException {
      MemoryStack $$1 = MemoryStack.stackPush();

      NativeImage var3;
      try {
         ByteBuffer $$2 = $$1.malloc(pBytes.length);
         $$2.put(pBytes);
         $$2.rewind();
         var3 = read($$2);
      } catch (Throwable var5) {
         if ($$1 != null) {
            try {
               $$1.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if ($$1 != null) {
         $$1.close();
      }

      return var3;
   }

   public static NativeImage read(@Nullable NativeImage.Format pFormat, ByteBuffer pTextureData) throws IOException {
      if (pFormat != null && !pFormat.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to read format " + pFormat);
      } else if (MemoryUtil.memAddress(pTextureData) == 0L) {
         throw new IllegalArgumentException("Invalid buffer");
      } else {
         PngInfo.validateHeader(pTextureData);
         MemoryStack $$2 = MemoryStack.stackPush();

         NativeImage var7;
         try {
            IntBuffer $$3 = $$2.mallocInt(1);
            IntBuffer $$4 = $$2.mallocInt(1);
            IntBuffer $$5 = $$2.mallocInt(1);
            ByteBuffer $$6 = STBImage.stbi_load_from_memory(pTextureData, $$3, $$4, $$5, pFormat == null ? 0 : pFormat.components);
            if ($$6 == null) {
               throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }

            var7 = new NativeImage(
               pFormat == null ? NativeImage.Format.getStbFormat($$5.get(0)) : pFormat, $$3.get(0), $$4.get(0), true, MemoryUtil.memAddress($$6)
            );
         } catch (Throwable var9) {
            if ($$2 != null) {
               try {
                  $$2.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if ($$2 != null) {
            $$2.close();
         }

         return var7;
      }
   }

   private static void setFilter(boolean pLinear, boolean pMipmap) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (pLinear) {
         GlStateManager._texParameter(3553, 10241, pMipmap ? 9987 : 9729);
         GlStateManager._texParameter(3553, 10240, 9729);
      } else {
         GlStateManager._texParameter(3553, 10241, pMipmap ? 9986 : 9728);
         GlStateManager._texParameter(3553, 10240, 9728);
      }
   }

   private void checkAllocated() {
      if (this.pixels == 0L) {
         throw new IllegalStateException("Image is not allocated.");
      }
   }

   @Override
   public void close() {
      if (this.pixels != 0L) {
         if (this.useStbFree) {
            STBImage.nstbi_image_free(this.pixels);
         } else {
            MemoryUtil.nmemFree(this.pixels);
         }
      }

      this.pixels = 0L;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public NativeImage.Format format() {
      return this.format;
   }

   public int getPixelRGBA(int pX, int pY) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         this.checkAllocated();
         long $$2 = ((long)pX + (long)pY * (long)this.width) * 4L;
         return MemoryUtil.memGetInt(this.pixels + $$2);
      }
   }

   public void setPixelRGBA(int pX, int pY, int pAbgrColor) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         this.checkAllocated();
         long $$3 = ((long)pX + (long)pY * (long)this.width) * 4L;
         MemoryUtil.memPutInt(this.pixels + $$3, pAbgrColor);
      }
   }

   public NativeImage mappedCopy(IntUnaryOperator pFunction) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         NativeImage $$1 = new NativeImage(this.width, this.height, false);
         int $$2 = this.width * this.height;
         IntBuffer $$3 = MemoryUtil.memIntBuffer(this.pixels, $$2);
         IntBuffer $$4 = MemoryUtil.memIntBuffer($$1.pixels, $$2);

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            $$4.put($$5, pFunction.applyAsInt($$3.get($$5)));
         }

         return $$1;
      }
   }

   public void applyToAllPixels(IntUnaryOperator pFunction) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         int $$1 = this.width * this.height;
         IntBuffer $$2 = MemoryUtil.memIntBuffer(this.pixels, $$1);

         for (int $$3 = 0; $$3 < $$1; $$3++) {
            $$2.put($$3, pFunction.applyAsInt($$2.get($$3)));
         }
      }
   }

   public int[] getPixelsRGBA() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelsRGBA only works on RGBA images; have %s", this.format));
      } else {
         this.checkAllocated();
         int[] $$0 = new int[this.width * this.height];
         MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get($$0);
         return $$0;
      }
   }

   public void setPixelLuminance(int pX, int pY, byte pLuminance) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminance()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelLuminance only works on image with luminance; have %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         this.checkAllocated();
         long $$3 = ((long)pX + (long)pY * (long)this.width) * (long)this.format.components() + (long)(this.format.luminanceOffset() / 8);
         MemoryUtil.memPutByte(this.pixels + $$3, pLuminance);
      }
   }

   public byte getRedOrLuminance(int pX, int pY) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrRed()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no red or luminance in %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         int $$2 = (pX + pY * this.width) * this.format.components() + this.format.luminanceOrRedOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + (long)$$2);
      }
   }

   public byte getGreenOrLuminance(int pX, int pY) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrGreen()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no green or luminance in %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         int $$2 = (pX + pY * this.width) * this.format.components() + this.format.luminanceOrGreenOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + (long)$$2);
      }
   }

   public byte getBlueOrLuminance(int pX, int pY) {
      RenderSystem.assertOnRenderThread();
      if (!this.format.hasLuminanceOrBlue()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no blue or luminance in %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         int $$2 = (pX + pY * this.width) * this.format.components() + this.format.luminanceOrBlueOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + (long)$$2);
      }
   }

   public byte getLuminanceOrAlpha(int pX, int pY) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
      } else if (this.isOutsideBounds(pX, pY)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height));
      } else {
         int $$2 = (pX + pY * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
         return MemoryUtil.memGetByte(this.pixels + (long)$$2);
      }
   }

   public void blendPixel(int pX, int pY, int pAbgrColor) {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("Can only call blendPixel with RGBA format");
      } else {
         int $$3 = this.getPixelRGBA(pX, pY);
         float $$4 = (float)FastColor.ABGR32.alpha(pAbgrColor) / 255.0F;
         float $$5 = (float)FastColor.ABGR32.blue(pAbgrColor) / 255.0F;
         float $$6 = (float)FastColor.ABGR32.green(pAbgrColor) / 255.0F;
         float $$7 = (float)FastColor.ABGR32.red(pAbgrColor) / 255.0F;
         float $$8 = (float)FastColor.ABGR32.alpha($$3) / 255.0F;
         float $$9 = (float)FastColor.ABGR32.blue($$3) / 255.0F;
         float $$10 = (float)FastColor.ABGR32.green($$3) / 255.0F;
         float $$11 = (float)FastColor.ABGR32.red($$3) / 255.0F;
         float $$13 = 1.0F - $$4;
         float $$14 = $$4 * $$4 + $$8 * $$13;
         float $$15 = $$5 * $$4 + $$9 * $$13;
         float $$16 = $$6 * $$4 + $$10 * $$13;
         float $$17 = $$7 * $$4 + $$11 * $$13;
         if ($$14 > 1.0F) {
            $$14 = 1.0F;
         }

         if ($$15 > 1.0F) {
            $$15 = 1.0F;
         }

         if ($$16 > 1.0F) {
            $$16 = 1.0F;
         }

         if ($$17 > 1.0F) {
            $$17 = 1.0F;
         }

         int $$18 = (int)($$14 * 255.0F);
         int $$19 = (int)($$15 * 255.0F);
         int $$20 = (int)($$16 * 255.0F);
         int $$21 = (int)($$17 * 255.0F);
         this.setPixelRGBA(pX, pY, FastColor.ABGR32.color($$18, $$19, $$20, $$21));
      }
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != NativeImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      } else {
         this.checkAllocated();
         int[] $$0 = new int[this.getWidth() * this.getHeight()];

         for (int $$1 = 0; $$1 < this.getHeight(); $$1++) {
            for (int $$2 = 0; $$2 < this.getWidth(); $$2++) {
               int $$3 = this.getPixelRGBA($$2, $$1);
               $$0[$$2 + $$1 * this.getWidth()] = FastColor.ARGB32.color(
                  FastColor.ABGR32.alpha($$3), FastColor.ABGR32.red($$3), FastColor.ABGR32.green($$3), FastColor.ABGR32.blue($$3)
               );
            }
         }

         return $$0;
      }
   }

   public void upload(int pLevel, int pXOffset, int pYOffset, boolean pMipmap) {
      this.upload(pLevel, pXOffset, pYOffset, 0, 0, this.width, this.height, false, pMipmap);
   }

   public void upload(
      int pLevel, int pXOffset, int pYOffset, int pUnpackSkipPixels, int pUnpackSkipRows, int pWidth, int pHeight, boolean pMipmap, boolean pAutoClose
   ) {
      this.upload(pLevel, pXOffset, pYOffset, pUnpackSkipPixels, pUnpackSkipRows, pWidth, pHeight, false, false, pMipmap, pAutoClose);
   }

   public void upload(
      int pLevel,
      int pXOffset,
      int pYOffset,
      int pUnpackSkipPixels,
      int pUnpackSkipRows,
      int pWidth,
      int pHeight,
      boolean pBlur,
      boolean pClamp,
      boolean pMipmap,
      boolean pAutoClose
   ) {
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(
            () -> this._upload(pLevel, pXOffset, pYOffset, pUnpackSkipPixels, pUnpackSkipRows, pWidth, pHeight, pBlur, pClamp, pMipmap, pAutoClose)
         );
      } else {
         this._upload(pLevel, pXOffset, pYOffset, pUnpackSkipPixels, pUnpackSkipRows, pWidth, pHeight, pBlur, pClamp, pMipmap, pAutoClose);
      }
   }

   private void _upload(
      int pLevel,
      int pXOffset,
      int pYOffset,
      int pUnpackSkipPixels,
      int pUnpackSkipRows,
      int pWidth,
      int pHeight,
      boolean pBlur,
      boolean pClamp,
      boolean pMipmap,
      boolean pAutoClose
   ) {
      try {
         RenderSystem.assertOnRenderThreadOrInit();
         this.checkAllocated();
         setFilter(pBlur, pMipmap);
         if (pWidth == this.getWidth()) {
            GlStateManager._pixelStore(3314, 0);
         } else {
            GlStateManager._pixelStore(3314, this.getWidth());
         }

         GlStateManager._pixelStore(3316, pUnpackSkipPixels);
         GlStateManager._pixelStore(3315, pUnpackSkipRows);
         this.format.setUnpackPixelStoreState();
         GlStateManager._texSubImage2D(3553, pLevel, pXOffset, pYOffset, pWidth, pHeight, this.format.glFormat(), 5121, this.pixels);
         if (pClamp) {
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
         }
      } finally {
         if (pAutoClose) {
            this.close();
         }
      }
   }

   public void downloadTexture(int pLevel, boolean pOpaque) {
      RenderSystem.assertOnRenderThread();
      this.checkAllocated();
      this.format.setPackPixelStoreState();
      GlStateManager._getTexImage(3553, pLevel, this.format.glFormat(), 5121, this.pixels);
      if (pOpaque && this.format.hasAlpha()) {
         for (int $$2 = 0; $$2 < this.getHeight(); $$2++) {
            for (int $$3 = 0; $$3 < this.getWidth(); $$3++) {
               this.setPixelRGBA($$3, $$2, this.getPixelRGBA($$3, $$2) | 255 << this.format.alphaOffset());
            }
         }
      }
   }

   public void downloadDepthBuffer(float pUnused) {
      RenderSystem.assertOnRenderThread();
      if (this.format.components() != 1) {
         throw new IllegalStateException("Depth buffer must be stored in NativeImage with 1 component.");
      } else {
         this.checkAllocated();
         this.format.setPackPixelStoreState();
         GlStateManager._readPixels(0, 0, this.width, this.height, 6402, 5121, this.pixels);
      }
   }

   public void drawPixels() {
      RenderSystem.assertOnRenderThread();
      this.format.setUnpackPixelStoreState();
      GlStateManager._glDrawPixels(this.width, this.height, this.format.glFormat(), 5121, this.pixels);
   }

   public void writeToFile(File pFile) throws IOException {
      this.writeToFile(pFile.toPath());
   }

   public boolean copyFromFont(FT_Face pFace, int pIndex) {
      if (this.format.components() != 1) {
         throw new IllegalArgumentException("Can only write fonts into 1-component images.");
      } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(pFace, pIndex, 4), "Loading glyph")) {
         return false;
      } else {
         FT_GlyphSlot $$2 = Objects.requireNonNull(pFace.glyph(), "Glyph not initialized");
         FT_Bitmap $$3 = $$2.bitmap();
         if ($$3.pixel_mode() != 2) {
            throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
         } else if ($$3.width() == this.getWidth() && $$3.rows() == this.getHeight()) {
            int $$4 = $$3.width() * $$3.rows();
            ByteBuffer $$5 = Objects.requireNonNull($$3.buffer($$4), "Glyph has no bitmap");
            MemoryUtil.memCopy(MemoryUtil.memAddress($$5), this.pixels, (long)$$4);
            return true;
         } else {
            throw new IllegalArgumentException(
               String.format(
                  Locale.ROOT, "Glyph bitmap of size %sx%s does not match image of size: %sx%s", $$3.width(), $$3.rows(), this.getWidth(), this.getHeight()
               )
            );
         }
      }
   }

   public void writeToFile(Path pPath) throws IOException {
      if (!this.format.supportedByStb()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      } else {
         this.checkAllocated();

         try (WritableByteChannel $$1 = Files.newByteChannel(pPath, OPEN_OPTIONS)) {
            if (!this.writeToChannel($$1)) {
               throw new IOException("Could not write image to the PNG file \"" + pPath.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
            }
         }
      }
   }

   public byte[] asByteArray() throws IOException {
      byte[] var3;
      try (
         ByteArrayOutputStream $$0 = new ByteArrayOutputStream();
         WritableByteChannel $$1 = Channels.newChannel($$0);
      ) {
         if (!this.writeToChannel($$1)) {
            throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
         }

         var3 = $$0.toByteArray();
      }

      return var3;
   }

   private boolean writeToChannel(WritableByteChannel pChannel) throws IOException {
      NativeImage.WriteCallback $$1 = new NativeImage.WriteCallback(pChannel);

      boolean var4;
      try {
         int $$2 = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
         if ($$2 < this.getHeight()) {
            LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), $$2);
         }

         if (STBImageWrite.nstbi_write_png_to_func($$1.address(), 0L, this.getWidth(), $$2, this.format.components(), this.pixels, 0) != 0) {
            $$1.throwIfException();
            return true;
         }

         var4 = false;
      } finally {
         $$1.free();
      }

      return var4;
   }

   public void copyFrom(NativeImage pOther) {
      if (pOther.format() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      } else {
         int $$1 = this.format.components();
         this.checkAllocated();
         pOther.checkAllocated();
         if (this.width == pOther.width) {
            MemoryUtil.memCopy(pOther.pixels, this.pixels, Math.min(this.size, pOther.size));
         } else {
            int $$2 = Math.min(this.getWidth(), pOther.getWidth());
            int $$3 = Math.min(this.getHeight(), pOther.getHeight());

            for (int $$4 = 0; $$4 < $$3; $$4++) {
               int $$5 = $$4 * pOther.getWidth() * $$1;
               int $$6 = $$4 * this.getWidth() * $$1;
               MemoryUtil.memCopy(pOther.pixels + (long)$$5, this.pixels + (long)$$6, (long)$$2);
            }
         }
      }
   }

   public void fillRect(int pX, int pY, int pWidth, int pHeight, int pValue) {
      for (int $$5 = pY; $$5 < pY + pHeight; $$5++) {
         for (int $$6 = pX; $$6 < pX + pWidth; $$6++) {
            this.setPixelRGBA($$6, $$5, pValue);
         }
      }
   }

   public void copyRect(int pXFrom, int pYFrom, int pXToDelta, int pYToDelta, int pWidth, int pHeight, boolean pMirrorX, boolean pMirrorY) {
      this.copyRect(this, pXFrom, pYFrom, pXFrom + pXToDelta, pYFrom + pYToDelta, pWidth, pHeight, pMirrorX, pMirrorY);
   }

   public void copyRect(NativeImage pSource, int pXFrom, int pYFrom, int pXTo, int pYTo, int pWidth, int pHeight, boolean pMirrorX, boolean pMirrorY) {
      for (int $$9 = 0; $$9 < pHeight; $$9++) {
         for (int $$10 = 0; $$10 < pWidth; $$10++) {
            int $$11 = pMirrorX ? pWidth - 1 - $$10 : $$10;
            int $$12 = pMirrorY ? pHeight - 1 - $$9 : $$9;
            int $$13 = this.getPixelRGBA(pXFrom + $$10, pYFrom + $$9);
            pSource.setPixelRGBA(pXTo + $$11, pYTo + $$12, $$13);
         }
      }
   }

   public void flipY() {
      this.checkAllocated();
      int $$0 = this.format.components();
      int $$1 = this.getWidth() * $$0;
      long $$2 = MemoryUtil.nmemAlloc((long)$$1);

      try {
         for (int $$3 = 0; $$3 < this.getHeight() / 2; $$3++) {
            int $$4 = $$3 * this.getWidth() * $$0;
            int $$5 = (this.getHeight() - 1 - $$3) * this.getWidth() * $$0;
            MemoryUtil.memCopy(this.pixels + (long)$$4, $$2, (long)$$1);
            MemoryUtil.memCopy(this.pixels + (long)$$5, this.pixels + (long)$$4, (long)$$1);
            MemoryUtil.memCopy($$2, this.pixels + (long)$$5, (long)$$1);
         }
      } finally {
         MemoryUtil.nmemFree($$2);
      }
   }

   public void resizeSubRectTo(int pX, int pY, int pWidth, int pHeight, NativeImage pImage) {
      this.checkAllocated();
      if (pImage.format() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      } else {
         int $$5 = this.format.components();
         STBImageResize.nstbir_resize_uint8(
            this.pixels + (long)((pX + pY * this.getWidth()) * $$5),
            pWidth,
            pHeight,
            this.getWidth() * $$5,
            pImage.pixels,
            pImage.getWidth(),
            pImage.getHeight(),
            0,
            $$5
         );
      }
   }

   public void untrack() {
      DebugMemoryUntracker.untrack(this.pixels);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Format {
      RGBA(4, 6408, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, 6407, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, 33319, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, 6403, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      final int components;
      private final int glFormat;
      private final boolean hasRed;
      private final boolean hasGreen;
      private final boolean hasBlue;
      private final boolean hasLuminance;
      private final boolean hasAlpha;
      private final int redOffset;
      private final int greenOffset;
      private final int blueOffset;
      private final int luminanceOffset;
      private final int alphaOffset;
      private final boolean supportedByStb;

      private Format(
         final int pComponents,
         final int pGlFormat,
         final boolean pHasRed,
         final boolean pHasGreen,
         final boolean pHasBlue,
         final boolean pHasLuminance,
         final boolean pHasAlpha,
         final int pRedOffset,
         final int pGreenOffset,
         final int pBlueOffset,
         final int pLuminanceOffset,
         final int pAlphaOffset,
         final boolean pSupportedByStb
      ) {
         this.components = pComponents;
         this.glFormat = pGlFormat;
         this.hasRed = pHasRed;
         this.hasGreen = pHasGreen;
         this.hasBlue = pHasBlue;
         this.hasLuminance = pHasLuminance;
         this.hasAlpha = pHasAlpha;
         this.redOffset = pRedOffset;
         this.greenOffset = pGreenOffset;
         this.blueOffset = pBlueOffset;
         this.luminanceOffset = pLuminanceOffset;
         this.alphaOffset = pAlphaOffset;
         this.supportedByStb = pSupportedByStb;
      }

      public int components() {
         return this.components;
      }

      public void setPackPixelStoreState() {
         RenderSystem.assertOnRenderThread();
         GlStateManager._pixelStore(3333, this.components());
      }

      public void setUnpackPixelStoreState() {
         RenderSystem.assertOnRenderThreadOrInit();
         GlStateManager._pixelStore(3317, this.components());
      }

      public int glFormat() {
         return this.glFormat;
      }

      public boolean hasRed() {
         return this.hasRed;
      }

      public boolean hasGreen() {
         return this.hasGreen;
      }

      public boolean hasBlue() {
         return this.hasBlue;
      }

      public boolean hasLuminance() {
         return this.hasLuminance;
      }

      public boolean hasAlpha() {
         return this.hasAlpha;
      }

      public int redOffset() {
         return this.redOffset;
      }

      public int greenOffset() {
         return this.greenOffset;
      }

      public int blueOffset() {
         return this.blueOffset;
      }

      public int luminanceOffset() {
         return this.luminanceOffset;
      }

      public int alphaOffset() {
         return this.alphaOffset;
      }

      public boolean hasLuminanceOrRed() {
         return this.hasLuminance || this.hasRed;
      }

      public boolean hasLuminanceOrGreen() {
         return this.hasLuminance || this.hasGreen;
      }

      public boolean hasLuminanceOrBlue() {
         return this.hasLuminance || this.hasBlue;
      }

      public boolean hasLuminanceOrAlpha() {
         return this.hasLuminance || this.hasAlpha;
      }

      public int luminanceOrRedOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.redOffset;
      }

      public int luminanceOrGreenOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
      }

      public int luminanceOrBlueOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
      }

      public int luminanceOrAlphaOffset() {
         return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
      }

      public boolean supportedByStb() {
         return this.supportedByStb;
      }

      static NativeImage.Format getStbFormat(int pChannels) {
         switch (pChannels) {
            case 1:
               return LUMINANCE;
            case 2:
               return LUMINANCE_ALPHA;
            case 3:
               return RGB;
            case 4:
            default:
               return RGBA;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum InternalGlFormat {
      RGBA(6408),
      RGB(6407),
      RG(33319),
      RED(6403);

      private final int glFormat;

      private InternalGlFormat(final int pGlFormat) {
         this.glFormat = pGlFormat;
      }

      public int glFormat() {
         return this.glFormat;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class WriteCallback extends STBIWriteCallback {
      private final WritableByteChannel output;
      @Nullable
      private IOException exception;

      WriteCallback(WritableByteChannel pOutput) {
         this.output = pOutput;
      }

      public void invoke(long pContext, long pData, int pSize) {
         ByteBuffer $$3 = getData(pData, pSize);

         try {
            this.output.write($$3);
         } catch (IOException var8) {
            this.exception = var8;
         }
      }

      public void throwIfException() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
