package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SimpleTexture extends AbstractTexture {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final ResourceLocation location;

   public SimpleTexture(ResourceLocation pLocation) {
      this.location = pLocation;
   }

   @Override
   public void load(ResourceManager pResourceManager) throws IOException {
      SimpleTexture.TextureImage $$1 = this.getTextureImage(pResourceManager);
      $$1.throwIfError();
      TextureMetadataSection $$2 = $$1.getTextureMetadata();
      boolean $$3;
      boolean $$4;
      if ($$2 != null) {
         $$3 = $$2.isBlur();
         $$4 = $$2.isClamp();
      } else {
         $$3 = false;
         $$4 = false;
      }

      NativeImage $$7 = $$1.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> this.doLoad($$7, $$3, $$4));
      } else {
         this.doLoad($$7, $$3, $$4);
      }
   }

   private void doLoad(NativeImage pImage, boolean pBlur, boolean pClamp) {
      TextureUtil.prepareImage(this.getId(), 0, pImage.getWidth(), pImage.getHeight());
      pImage.upload(0, 0, 0, 0, 0, pImage.getWidth(), pImage.getHeight(), pBlur, pClamp, false, true);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager pResourceManager) {
      return SimpleTexture.TextureImage.load(pResourceManager, this.location);
   }

   @OnlyIn(Dist.CLIENT)
   protected static class TextureImage implements Closeable {
      @Nullable
      private final TextureMetadataSection metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureImage(IOException pException) {
         this.exception = pException;
         this.metadata = null;
         this.image = null;
      }

      public TextureImage(@Nullable TextureMetadataSection pMetadata, NativeImage pImage) {
         this.exception = null;
         this.metadata = pMetadata;
         this.image = pImage;
      }

      public static SimpleTexture.TextureImage load(ResourceManager pResourceManager, ResourceLocation pLocation) {
         try {
            Resource $$2 = pResourceManager.getResourceOrThrow(pLocation);

            NativeImage $$4;
            try (InputStream $$3 = $$2.open()) {
               $$4 = NativeImage.read($$3);
            }

            TextureMetadataSection $$6 = null;

            try {
               $$6 = $$2.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse(null);
            } catch (RuntimeException var8) {
               SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", pLocation, var8);
            }

            return new SimpleTexture.TextureImage($$6, $$4);
         } catch (IOException var10) {
            return new SimpleTexture.TextureImage(var10);
         }
      }

      @Nullable
      public TextureMetadataSection getTextureMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      @Override
      public void close() {
         if (this.image != null) {
            this.image.close();
         }
      }

      public void throwIfError() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
