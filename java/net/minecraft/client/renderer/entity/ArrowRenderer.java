package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T> {
   public ArrowRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
      pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot())));
      int $$6 = 0;
      float $$7 = 0.0F;
      float $$8 = 0.5F;
      float $$9 = 0.0F;
      float $$10 = 0.15625F;
      float $$11 = 0.0F;
      float $$12 = 0.15625F;
      float $$13 = 0.15625F;
      float $$14 = 0.3125F;
      float $$15 = 0.05625F;
      float $$16 = (float)pEntity.shakeTime - pPartialTicks;
      if ($$16 > 0.0F) {
         float $$17 = -Mth.sin($$16 * 3.0F) * $$16;
         pPoseStack.mulPose(Axis.ZP.rotationDegrees($$17));
      }

      pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
      pPoseStack.scale(0.05625F, 0.05625F, 0.05625F);
      pPoseStack.translate(-4.0F, 0.0F, 0.0F);
      VertexConsumer $$18 = pBuffer.getBuffer(RenderType.entityCutout(this.getTextureLocation(pEntity)));
      PoseStack.Pose $$19 = pPoseStack.last();
      this.vertex($$19, $$18, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, pPackedLight);
      this.vertex($$19, $$18, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, pPackedLight);

      for (int $$20 = 0; $$20 < 4; $$20++) {
         pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         this.vertex($$19, $$18, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, pPackedLight);
         this.vertex($$19, $$18, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, pPackedLight);
         this.vertex($$19, $$18, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, pPackedLight);
         this.vertex($$19, $$18, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, pPackedLight);
      }

      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   public void vertex(
      PoseStack.Pose pPose, VertexConsumer pConsumer, int pX, int pY, int pZ, float pU, float pV, int pNormalX, int pNormalY, int pNormalZ, int pPackedLight
   ) {
      pConsumer.addVertex(pPose, (float)pX, (float)pY, (float)pZ)
         .setColor(-1)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(pPackedLight)
         .setNormal(pPose, (float)pNormalX, (float)pNormalZ, (float)pNormalY);
   }
}
