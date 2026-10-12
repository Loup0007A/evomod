package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

   public static int generateTextureId() {
      RenderSystem.assertOnRenderThreadOrInit();
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         int[] $$0 = new int[ThreadLocalRandom.current().nextInt(15) + 1];
         GlStateManager._genTextures($$0);
         int $$1 = GlStateManager._genTexture();
         GlStateManager._deleteTextures($$0);
         return $$1;
      } else {
         return GlStateManager._genTexture();
      }
   }

   public static void releaseTextureId(int pTextureId) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._deleteTexture(pTextureId);
   }

   public static void prepareImage(int pTextureId, int pWidth, int pHeight) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, pTextureId, 0, pWidth, pHeight);
   }

   public static void prepareImage(NativeImage.InternalGlFormat pPixelFormat, int pTextureId, int pWidth, int pHeight) {
      prepareImage(pPixelFormat, pTextureId, 0, pWidth, pHeight);
   }

   public static void prepareImage(int pTextureId, int pMipmapLevel, int pWidth, int pHeight) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, pTextureId, pMipmapLevel, pWidth, pHeight);
   }

   public static void prepareImage(NativeImage.InternalGlFormat pPixelFormat, int pTextureId, int pMipmapLevel, int pWidth, int pHeight) {
      RenderSystem.assertOnRenderThreadOrInit();
      bind(pTextureId);
      if (pMipmapLevel >= 0) {
         GlStateManager._texParameter(3553, 33085, pMipmapLevel);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, pMipmapLevel);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      for (int $$5 = 0; $$5 <= pMipmapLevel; $$5++) {
         GlStateManager._texImage2D(3553, $$5, pPixelFormat.glFormat(), pWidth >> $$5, pHeight >> $$5, 0, 6408, 5121, null);
      }
   }

   private static void bind(int pTextureId) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._bindTexture(pTextureId);
   }

   public static ByteBuffer readResource(InputStream pInputStream) throws IOException {
      ReadableByteChannel $$1 = Channels.newChannel(pInputStream);
      return $$1 instanceof SeekableByteChannel $$2 ? readResource($$1, (int)$$2.size() + 1) : readResource($$1, 8192);
   }

   private static ByteBuffer readResource(ReadableByteChannel pChannel, int pSize) throws IOException {
      ByteBuffer $$2 = MemoryUtil.memAlloc(pSize);

      try {
         while (pChannel.read($$2) != -1) {
            if (!$$2.hasRemaining()) {
               $$2 = MemoryUtil.memRealloc($$2, $$2.capacity() * 2);
            }
         }

         return $$2;
      } catch (IOException var4) {
         MemoryUtil.memFree($$2);
         throw var4;
      }
   }

   public static void writeAsPNG(Path pOutputDir, String pTextureName, int pTextureId, int pAmount, int pWidth, int pHeight) {
      writeAsPNG(pOutputDir, pTextureName, pTextureId, pAmount, pWidth, pHeight, null);
   }

   public static void writeAsPNG(
      Path pOutputDir, String pTextureName, int pTextureId, int pAmount, int pWidth, int pHeight, @Nullable IntUnaryOperator pFunction
   ) {
      RenderSystem.assertOnRenderThread();
      bind(pTextureId);

      for (int $$7 = 0; $$7 <= pAmount; $$7++) {
         int $$8 = pWidth >> $$7;
         int $$9 = pHeight >> $$7;

         try (NativeImage $$10 = new NativeImage($$8, $$9, false)) {
            $$10.downloadTexture($$7, false);
            if (pFunction != null) {
               $$10.applyToAllPixels(pFunction);
            }

            Path $$11 = pOutputDir.resolve(pTextureName + "_" + $$7 + ".png");
            $$10.writeToFile($$11);
            LOGGER.debug("Exported png to: {}", $$11.toAbsolutePath());
         } catch (IOException var15) {
            LOGGER.debug("Unable to write: ", var15);
         }
      }
   }

   public static Path getDebugTexturePath(Path pBasePath) {
      return pBasePath.resolve("screenshots").resolve("debug");
   }

   public static Path getDebugTexturePath() {
      return getDebugTexturePath(Path.of("."));
   }
}
