package net.minecraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MapRenderer implements AutoCloseable {
   private static final int WIDTH = 128;
   private static final int HEIGHT = 128;
   final TextureManager textureManager;
   final MapDecorationTextureManager decorationTextures;
   private final Int2ObjectMap<MapRenderer.MapInstance> maps = new Int2ObjectOpenHashMap();

   public MapRenderer(TextureManager pTextureManager, MapDecorationTextureManager pDecorationTextures) {
      this.textureManager = pTextureManager;
      this.decorationTextures = pDecorationTextures;
   }

   public void update(MapId pMapId, MapItemSavedData pMapData) {
      this.getOrCreateMapInstance(pMapId, pMapData).forceUpload();
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, MapId pMapId, MapItemSavedData pMapData, boolean pActive, int pPackedLight) {
      this.getOrCreateMapInstance(pMapId, pMapData).draw(pPoseStack, pBuffer, pActive, pPackedLight);
   }

   private MapRenderer.MapInstance getOrCreateMapInstance(MapId pMapId, MapItemSavedData pMapData) {
      return (MapRenderer.MapInstance)this.maps.compute(pMapId.id(), (p_182563_, p_182564_) -> {
         if (p_182564_ == null) {
            return new MapRenderer.MapInstance(p_182563_, pMapData);
         } else {
            p_182564_.replaceMapData(pMapData);
            return p_182564_;
         }
      });
   }

   public void resetData() {
      ObjectIterator var1 = this.maps.values().iterator();

      while (var1.hasNext()) {
         MapRenderer.MapInstance $$0 = (MapRenderer.MapInstance)var1.next();
         $$0.close();
      }

      this.maps.clear();
   }

   @Override
   public void close() {
      this.resetData();
   }

   @OnlyIn(Dist.CLIENT)
   class MapInstance implements AutoCloseable {
      private MapItemSavedData data;
      private final DynamicTexture texture;
      private final RenderType renderType;
      private boolean requiresUpload = true;

      MapInstance(final int pId, final MapItemSavedData pData) {
         this.data = pData;
         this.texture = new DynamicTexture(128, 128, true);
         ResourceLocation $$2 = MapRenderer.this.textureManager.register("map/" + pId, this.texture);
         this.renderType = RenderType.text($$2);
      }

      void replaceMapData(MapItemSavedData pData) {
         boolean $$1 = this.data != pData;
         this.data = pData;
         this.requiresUpload |= $$1;
      }

      public void forceUpload() {
         this.requiresUpload = true;
      }

      private void updateTexture() {
         for (int $$0 = 0; $$0 < 128; $$0++) {
            for (int $$1 = 0; $$1 < 128; $$1++) {
               int $$2 = $$1 + $$0 * 128;
               this.texture.getPixels().setPixelRGBA($$1, $$0, MapColor.getColorFromPackedId(this.data.colors[$$2]));
            }
         }

         this.texture.upload();
      }

      void draw(PoseStack pPoseStack, MultiBufferSource pBufferSource, boolean pActive, int pPackedLight) {
         if (this.requiresUpload) {
            this.updateTexture();
            this.requiresUpload = false;
         }

         int $$4 = 0;
         int $$5 = 0;
         float $$6 = 0.0F;
         Matrix4f $$7 = pPoseStack.last().pose();
         VertexConsumer $$8 = pBufferSource.getBuffer(this.renderType);
         $$8.addVertex($$7, 0.0F, 128.0F, -0.01F).setColor(-1).setUv(0.0F, 1.0F).setLight(pPackedLight);
         $$8.addVertex($$7, 128.0F, 128.0F, -0.01F).setColor(-1).setUv(1.0F, 1.0F).setLight(pPackedLight);
         $$8.addVertex($$7, 128.0F, 0.0F, -0.01F).setColor(-1).setUv(1.0F, 0.0F).setLight(pPackedLight);
         $$8.addVertex($$7, 0.0F, 0.0F, -0.01F).setColor(-1).setUv(0.0F, 0.0F).setLight(pPackedLight);
         int $$9 = 0;

         for (MapDecoration $$10 : this.data.getDecorations()) {
            if (!pActive || $$10.renderOnFrame()) {
               pPoseStack.pushPose();
               pPoseStack.translate(0.0F + (float)$$10.x() / 2.0F + 64.0F, 0.0F + (float)$$10.y() / 2.0F + 64.0F, -0.02F);
               pPoseStack.mulPose(Axis.ZP.rotationDegrees((float)($$10.rot() * 360) / 16.0F));
               pPoseStack.scale(4.0F, 4.0F, 3.0F);
               pPoseStack.translate(-0.125F, 0.125F, 0.0F);
               Matrix4f $$11 = pPoseStack.last().pose();
               float $$12 = -0.001F;
               TextureAtlasSprite $$13 = MapRenderer.this.decorationTextures.get($$10);
               float $$14 = $$13.getU0();
               float $$15 = $$13.getV0();
               float $$16 = $$13.getU1();
               float $$17 = $$13.getV1();
               VertexConsumer $$18 = pBufferSource.getBuffer(RenderType.text($$13.atlasLocation()));
               $$18.addVertex($$11, -1.0F, 1.0F, (float)$$9 * -0.001F).setColor(-1).setUv($$14, $$15).setLight(pPackedLight);
               $$18.addVertex($$11, 1.0F, 1.0F, (float)$$9 * -0.001F).setColor(-1).setUv($$16, $$15).setLight(pPackedLight);
               $$18.addVertex($$11, 1.0F, -1.0F, (float)$$9 * -0.001F).setColor(-1).setUv($$16, $$17).setLight(pPackedLight);
               $$18.addVertex($$11, -1.0F, -1.0F, (float)$$9 * -0.001F).setColor(-1).setUv($$14, $$17).setLight(pPackedLight);
               pPoseStack.popPose();
               if ($$10.name().isPresent()) {
                  Font $$19 = Minecraft.getInstance().font;
                  Component $$20 = $$10.name().get();
                  float $$21 = (float)$$19.width($$20);
                  float $$22 = Mth.clamp(25.0F / $$21, 0.0F, 6.0F / 9.0F);
                  pPoseStack.pushPose();
                  pPoseStack.translate(0.0F + (float)$$10.x() / 2.0F + 64.0F - $$21 * $$22 / 2.0F, 0.0F + (float)$$10.y() / 2.0F + 64.0F + 4.0F, -0.025F);
                  pPoseStack.scale($$22, $$22, 1.0F);
                  pPoseStack.translate(0.0F, 0.0F, -0.1F);
                  $$19.drawInBatch(
                     $$20, 0.0F, 0.0F, -1, false, pPoseStack.last().pose(), pBufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, pPackedLight
                  );
                  pPoseStack.popPose();
               }

               $$9++;
            }
         }
      }

      @Override
      public void close() {
         this.texture.close();
      }
   }
}
