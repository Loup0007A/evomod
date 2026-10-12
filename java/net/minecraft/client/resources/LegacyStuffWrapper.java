package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyStuffWrapper {
   @Deprecated
   public static int[] getPixels(ResourceManager pManager, ResourceLocation pLocation) throws IOException {
      int[] var4;
      try (
         InputStream $$2 = pManager.open(pLocation);
         NativeImage $$3 = NativeImage.read($$2);
      ) {
         var4 = $$3.makePixelArray();
      }

      return var4;
   }
}
