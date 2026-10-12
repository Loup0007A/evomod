package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation TEMPLATE_ICON_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/presets/isles.png");

   public static ResourceLocation worldTemplate(String pKey, @Nullable String pImage) {
      return pImage == null ? TEMPLATE_ICON_LOCATION : getTexture(pKey, pImage);
   }

   private static ResourceLocation getTexture(String pKey, String pImage) {
      RealmsTextureManager.RealmsTexture $$2 = TEXTURES.get(pKey);
      if ($$2 != null && $$2.image().equals(pImage)) {
         return $$2.textureId;
      } else {
         NativeImage $$3 = loadImage(pImage);
         if ($$3 == null) {
            ResourceLocation $$4 = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(pKey, new RealmsTextureManager.RealmsTexture(pImage, $$4));
            return $$4;
         } else {
            ResourceLocation $$5 = ResourceLocation.fromNamespaceAndPath("realms", "dynamic/" + pKey);
            Minecraft.getInstance().getTextureManager().register($$5, new DynamicTexture($$3));
            TEXTURES.put(pKey, new RealmsTextureManager.RealmsTexture(pImage, $$5));
            return $$5;
         }
      }
   }

   @Nullable
   private static NativeImage loadImage(String pBase64Image) {
      byte[] $$1 = Base64.getDecoder().decode(pBase64Image);
      ByteBuffer $$2 = MemoryUtil.memAlloc($$1.length);

      try {
         return NativeImage.read($$2.put($$1).flip());
      } catch (IOException var7) {
         LOGGER.warn("Failed to load world image: {}", pBase64Image, var7);
      } finally {
         MemoryUtil.memFree($$2);
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public static record RealmsTexture(String image, ResourceLocation textureId) {
   }
}
