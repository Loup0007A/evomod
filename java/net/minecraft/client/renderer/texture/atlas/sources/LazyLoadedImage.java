package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LazyLoadedImage {
   private final ResourceLocation id;
   private final Resource resource;
   private final AtomicReference<NativeImage> image = new AtomicReference<>();
   private final AtomicInteger referenceCount;

   public LazyLoadedImage(ResourceLocation pId, Resource pResource, int pReferenceCount) {
      this.id = pId;
      this.resource = pResource;
      this.referenceCount = new AtomicInteger(pReferenceCount);
   }

   public NativeImage get() throws IOException {
      NativeImage $$0 = this.image.get();
      if ($$0 == null) {
         synchronized (this) {
            $$0 = this.image.get();
            if ($$0 == null) {
               try (InputStream $$1 = this.resource.open()) {
                  $$0 = NativeImage.read($$1);
                  this.image.set($$0);
               } catch (IOException var9) {
                  throw new IOException("Failed to load image " + this.id, var9);
               }
            }
         }
      }

      return $$0;
   }

   public void release() {
      int $$0 = this.referenceCount.decrementAndGet();
      if ($$0 <= 0) {
         NativeImage $$1 = this.image.getAndSet(null);
         if ($$1 != null) {
            $$1.close();
         }
      }
   }
}
