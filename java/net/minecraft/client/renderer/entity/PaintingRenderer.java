package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting> {
   public PaintingRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
   }

   public void render(Painting pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - pEntityYaw));
      PaintingVariant $$6 = pEntity.getVariant().value();
      VertexConsumer $$7 = pBuffer.getBuffer(RenderType.entitySolid(this.getTextureLocation(pEntity)));
      PaintingTextureManager $$8 = Minecraft.getInstance().getPaintingTextures();
      this.renderPainting(pPoseStack, $$7, pEntity, $$6.width(), $$6.height(), $$8.get($$6), $$8.getBackSprite());
      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   public ResourceLocation getTextureLocation(Painting pEntity) {
      return Minecraft.getInstance().getPaintingTextures().getBackSprite().atlasLocation();
   }

   private void renderPainting(
      PoseStack pPoseStack,
      VertexConsumer pConsumer,
      Painting pPainting,
      int pWidth,
      int pHeight,
      TextureAtlasSprite pPaintingSprite,
      TextureAtlasSprite pBackSprite
   ) {
      PoseStack.Pose $$7 = pPoseStack.last();
      float $$8 = (float)(-pWidth) / 2.0F;
      float $$9 = (float)(-pHeight) / 2.0F;
      float $$10 = 0.03125F;
      float $$11 = pBackSprite.getU0();
      float $$12 = pBackSprite.getU1();
      float $$13 = pBackSprite.getV0();
      float $$14 = pBackSprite.getV1();
      float $$15 = pBackSprite.getU0();
      float $$16 = pBackSprite.getU1();
      float $$17 = pBackSprite.getV0();
      float $$18 = pBackSprite.getV(0.0625F);
      float $$19 = pBackSprite.getU0();
      float $$20 = pBackSprite.getU(0.0625F);
      float $$21 = pBackSprite.getV0();
      float $$22 = pBackSprite.getV1();
      double $$23 = 1.0 / (double)pWidth;
      double $$24 = 1.0 / (double)pHeight;

      for (int $$25 = 0; $$25 < pWidth; $$25++) {
         for (int $$26 = 0; $$26 < pHeight; $$26++) {
            float $$27 = $$8 + (float)($$25 + 1);
            float $$28 = $$8 + (float)$$25;
            float $$29 = $$9 + (float)($$26 + 1);
            float $$30 = $$9 + (float)$$26;
            int $$31 = pPainting.getBlockX();
            int $$32 = Mth.floor(pPainting.getY() + (double)(($$29 + $$30) / 2.0F));
            int $$33 = pPainting.getBlockZ();
            Direction $$34 = pPainting.getDirection();
            if ($$34 == Direction.NORTH) {
               $$31 = Mth.floor(pPainting.getX() + (double)(($$27 + $$28) / 2.0F));
            }

            if ($$34 == Direction.WEST) {
               $$33 = Mth.floor(pPainting.getZ() - (double)(($$27 + $$28) / 2.0F));
            }

            if ($$34 == Direction.SOUTH) {
               $$31 = Mth.floor(pPainting.getX() - (double)(($$27 + $$28) / 2.0F));
            }

            if ($$34 == Direction.EAST) {
               $$33 = Mth.floor(pPainting.getZ() + (double)(($$27 + $$28) / 2.0F));
            }

            int $$35 = LevelRenderer.getLightColor(pPainting.level(), new BlockPos($$31, $$32, $$33));
            float $$36 = pPaintingSprite.getU((float)($$23 * (double)(pWidth - $$25)));
            float $$37 = pPaintingSprite.getU((float)($$23 * (double)(pWidth - ($$25 + 1))));
            float $$38 = pPaintingSprite.getV((float)($$24 * (double)(pHeight - $$26)));
            float $$39 = pPaintingSprite.getV((float)($$24 * (double)(pHeight - ($$26 + 1))));
            this.vertex($$7, pConsumer, $$27, $$30, $$37, $$38, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$36, $$38, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$36, $$39, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$37, $$39, -0.03125F, 0, 0, -1, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$12, $$13, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$11, $$13, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$11, $$14, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, pConsumer, $$27, $$30, $$12, $$14, 0.03125F, 0, 0, 1, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$15, $$17, -0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$16, $$17, -0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$16, $$18, 0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$15, $$18, 0.03125F, 0, 1, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$30, $$15, $$17, 0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$16, $$17, 0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$16, $$18, -0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$30, $$15, $$18, -0.03125F, 0, -1, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$20, $$21, 0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$30, $$20, $$22, 0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$30, $$19, $$22, -0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$27, $$29, $$19, $$21, -0.03125F, -1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$20, $$21, -0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$20, $$22, -0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$30, $$19, $$22, 0.03125F, 1, 0, 0, $$35);
            this.vertex($$7, pConsumer, $$28, $$29, $$19, $$21, 0.03125F, 1, 0, 0, $$35);
         }
      }
   }

   private void vertex(
      PoseStack.Pose pPose,
      VertexConsumer pConsumer,
      float pX,
      float pY,
      float pU,
      float pV,
      float pZ,
      int pNormalX,
      int pNormalY,
      int pNormalZ,
      int pPackedLight
   ) {
      pConsumer.addVertex(pPose, pX, pY, pZ)
         .setColor(-1)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(pPackedLight)
         .setNormal(pPose, (float)pNormalX, (float)pNormalY, (float)pNormalZ);
   }
}
