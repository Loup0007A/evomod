package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Deadmau5EarsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public Deadmau5EarsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
      super(pRenderer);
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      AbstractClientPlayer pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      if ("deadmau5".equals(pLivingEntity.getName().getString()) && !pLivingEntity.isInvisible()) {
         VertexConsumer $$10 = pBuffer.getBuffer(RenderType.entitySolid(pLivingEntity.getSkin().texture()));
         int $$11 = LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F);

         for (int $$12 = 0; $$12 < 2; $$12++) {
            float $$13 = Mth.lerp(pPartialTicks, pLivingEntity.yRotO, pLivingEntity.getYRot())
               - Mth.lerp(pPartialTicks, pLivingEntity.yBodyRotO, pLivingEntity.yBodyRot);
            float $$14 = Mth.lerp(pPartialTicks, pLivingEntity.xRotO, pLivingEntity.getXRot());
            pPoseStack.pushPose();
            pPoseStack.mulPose(Axis.YP.rotationDegrees($$13));
            pPoseStack.mulPose(Axis.XP.rotationDegrees($$14));
            pPoseStack.translate(0.375F * (float)($$12 * 2 - 1), 0.0F, 0.0F);
            pPoseStack.translate(0.0F, -0.375F, 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-$$14));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-$$13));
            float $$15 = 1.3333334F;
            pPoseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
            this.getParentModel().renderEars(pPoseStack, $$10, pPackedLight, $$11);
            pPoseStack.popPose();
         }
      }
   }
}
