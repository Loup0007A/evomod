package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S> extends EntityRenderer<T> {
   private final EntityRenderDispatcher entityRenderDispatcher;

   protected DisplayRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.entityRenderDispatcher = pContext.getEntityRenderDispatcher();
   }

   public ResourceLocation getTextureLocation(T pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      Display.RenderState $$6 = pEntity.renderState();
      if ($$6 != null) {
         S $$7 = this.getSubState(pEntity);
         if ($$7 != null) {
            float $$8 = pEntity.calculateInterpolationProgress(pPartialTick);
            this.shadowRadius = $$6.shadowRadius().get($$8);
            this.shadowStrength = $$6.shadowStrength().get($$8);
            int $$9 = $$6.brightnessOverride();
            int $$10 = $$9 != -1 ? $$9 : pPackedLight;
            super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBuffer, $$10);
            pPoseStack.pushPose();
            pPoseStack.mulPose(this.calculateOrientation($$6, pEntity, pPartialTick, new Quaternionf()));
            Transformation $$11 = $$6.transformation().get($$8);
            pPoseStack.mulPose($$11.getMatrix());
            this.renderInner(pEntity, $$7, pPoseStack, pBuffer, $$10, $$8);
            pPoseStack.popPose();
         }
      }
   }

   private Quaternionf calculateOrientation(Display.RenderState pRenderState, T pEntity, float pPartialTick, Quaternionf pQuaternion) {
      Camera $$4 = this.entityRenderDispatcher.camera;

      return switch (pRenderState.billboardConstraints()) {
         case FIXED -> pQuaternion.rotationYXZ(
         (float) (-Math.PI / 180.0) * entityYRot(pEntity, pPartialTick), (float) (Math.PI / 180.0) * entityXRot(pEntity, pPartialTick), 0.0F
      );
         case HORIZONTAL -> pQuaternion.rotationYXZ(
         (float) (-Math.PI / 180.0) * entityYRot(pEntity, pPartialTick), (float) (Math.PI / 180.0) * cameraXRot($$4), 0.0F
      );
         case VERTICAL -> pQuaternion.rotationYXZ(
         (float) (-Math.PI / 180.0) * cameraYrot($$4), (float) (Math.PI / 180.0) * entityXRot(pEntity, pPartialTick), 0.0F
      );
         case CENTER -> pQuaternion.rotationYXZ((float) (-Math.PI / 180.0) * cameraYrot($$4), (float) (Math.PI / 180.0) * cameraXRot($$4), 0.0F);
      };
   }

   private static float cameraYrot(Camera pCamera) {
      return pCamera.getYRot() - 180.0F;
   }

   private static float cameraXRot(Camera pCamera) {
      return -pCamera.getXRot();
   }

   private static <T extends Display> float entityYRot(T pEntity, float pPartialTick) {
      return Mth.rotLerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
   }

   private static <T extends Display> float entityXRot(T pEntity, float pPartialTick) {
      return Mth.lerp(pPartialTick, pEntity.xRotO, pEntity.getXRot());
   }

   @Nullable
   protected abstract S getSubState(T pTextDisplay);

   protected abstract void renderInner(T pTextDisplay, S pRenderState, PoseStack pPoseStack, MultiBufferSource pBuffer, int pLightmapUV, float pPartialTick);

   @OnlyIn(Dist.CLIENT)
   public static class BlockDisplayRenderer extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState> {
      private final BlockRenderDispatcher blockRenderer;

      protected BlockDisplayRenderer(EntityRendererProvider.Context p_270283_) {
         super(p_270283_);
         this.blockRenderer = p_270283_.getBlockRenderDispatcher();
      }

      @Nullable
      protected Display.BlockDisplay.BlockRenderState getSubState(Display.BlockDisplay p_277721_) {
         return p_277721_.blockRenderState();
      }

      public void renderInner(
         Display.BlockDisplay p_277939_,
         Display.BlockDisplay.BlockRenderState p_277885_,
         PoseStack p_277831_,
         MultiBufferSource p_277554_,
         int p_278071_,
         float p_277847_
      ) {
         this.blockRenderer.renderSingleBlock(p_277885_.blockState(), p_277831_, p_277554_, p_278071_, OverlayTexture.NO_OVERLAY);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState> {
      private final ItemRenderer itemRenderer;

      protected ItemDisplayRenderer(EntityRendererProvider.Context p_270110_) {
         super(p_270110_);
         this.itemRenderer = p_270110_.getItemRenderer();
      }

      @Nullable
      protected Display.ItemDisplay.ItemRenderState getSubState(Display.ItemDisplay p_277464_) {
         return p_277464_.itemRenderState();
      }

      public void renderInner(
         Display.ItemDisplay p_277863_,
         Display.ItemDisplay.ItemRenderState p_277481_,
         PoseStack p_277889_,
         MultiBufferSource p_277509_,
         int p_277861_,
         float p_277670_
      ) {
         p_277889_.mulPose(Axis.YP.rotation((float) Math.PI));
         this.itemRenderer
            .renderStatic(
               p_277481_.itemStack(),
               p_277481_.itemTransform(),
               p_277861_,
               OverlayTexture.NO_OVERLAY,
               p_277889_,
               p_277509_,
               p_277863_.level(),
               p_277863_.getId()
            );
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState> {
      private final Font font;

      protected TextDisplayRenderer(EntityRendererProvider.Context pContext) {
         super(pContext);
         this.font = pContext.getFont();
      }

      private Display.TextDisplay.CachedInfo splitLines(Component p_270823_, int p_270893_) {
         List<FormattedCharSequence> $$2 = this.font.split(p_270823_, p_270893_);
         List<Display.TextDisplay.CachedLine> $$3 = new ArrayList<>($$2.size());
         int $$4 = 0;

         for (FormattedCharSequence $$5 : $$2) {
            int $$6 = this.font.width($$5);
            $$4 = Math.max($$4, $$6);
            $$3.add(new Display.TextDisplay.CachedLine($$5, $$6));
         }

         return new Display.TextDisplay.CachedInfo($$3, $$4);
      }

      @Nullable
      protected Display.TextDisplay.TextRenderState getSubState(Display.TextDisplay pTextDisplay) {
         return pTextDisplay.textRenderState();
      }

      public void renderInner(
         Display.TextDisplay pTextDisplay,
         Display.TextDisplay.TextRenderState pRenderState,
         PoseStack pPoseStack,
         MultiBufferSource pBuffer,
         int pLightmapUV,
         float pPartialTick
      ) {
         byte $$6 = pRenderState.flags();
         boolean $$7 = ($$6 & 2) != 0;
         boolean $$8 = ($$6 & 4) != 0;
         boolean $$9 = ($$6 & 1) != 0;
         Display.TextDisplay.Align $$10 = Display.TextDisplay.getAlign($$6);
         byte $$11 = (byte)pRenderState.textOpacity().get(pPartialTick);
         int $$13;
         if ($$8) {
            float $$12 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            $$13 = (int)($$12 * 255.0F) << 24;
         } else {
            $$13 = pRenderState.backgroundColor().get(pPartialTick);
         }

         float $$15 = 0.0F;
         Matrix4f $$16 = pPoseStack.last().pose();
         $$16.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
         $$16.scale(-0.025F, -0.025F, -0.025F);
         Display.TextDisplay.CachedInfo $$17 = pTextDisplay.cacheDisplay(this::splitLines);
         int $$18 = 9 + 1;
         int $$19 = $$17.width();
         int $$20 = $$17.lines().size() * $$18;
         $$16.translate(1.0F - (float)$$19 / 2.0F, (float)(-$$20), 0.0F);
         if ($$13 != 0) {
            VertexConsumer $$21 = pBuffer.getBuffer($$7 ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground());
            $$21.addVertex($$16, -1.0F, -1.0F, 0.0F).setColor($$13).setLight(pLightmapUV);
            $$21.addVertex($$16, -1.0F, (float)$$20, 0.0F).setColor($$13).setLight(pLightmapUV);
            $$21.addVertex($$16, (float)$$19, (float)$$20, 0.0F).setColor($$13).setLight(pLightmapUV);
            $$21.addVertex($$16, (float)$$19, -1.0F, 0.0F).setColor($$13).setLight(pLightmapUV);
         }

         for (Display.TextDisplay.CachedLine $$22 : $$17.lines()) {
            float $$23 = switch ($$10) {
               case LEFT -> 0.0F;
               case RIGHT -> (float)($$19 - $$22.width());
               case CENTER -> (float)$$19 / 2.0F - (float)$$22.width() / 2.0F;
            };
            this.font
               .drawInBatch(
                  $$22.contents(),
                  $$23,
                  $$15,
                  $$11 << 24 | 16777215,
                  $$9,
                  $$16,
                  pBuffer,
                  $$7 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                  0,
                  pLightmapUV
               );
            $$15 += (float)$$18;
         }
      }
   }
}
