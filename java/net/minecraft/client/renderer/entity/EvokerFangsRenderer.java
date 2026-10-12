package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerFangsRenderer extends EntityRenderer<EvokerFangs> {
   private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
   private final EvokerFangsModel<EvokerFangs> model;

   public EvokerFangsRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.model = new EvokerFangsModel<>(pContext.bakeLayer(ModelLayers.EVOKER_FANGS));
   }

   public void render(EvokerFangs pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      float $$6 = pEntity.getAnimationProgress(pPartialTicks);
      if ($$6 != 0.0F) {
         float $$7 = 2.0F;
         if ($$6 > 0.9F) {
            $$7 *= (1.0F - $$6) / 0.1F;
         }

         pPoseStack.pushPose();
         pPoseStack.mulPose(Axis.YP.rotationDegrees(90.0F - pEntity.getYRot()));
         pPoseStack.scale(-$$7, -$$7, $$7);
         float $$8 = 0.03125F;
         pPoseStack.translate(0.0, -0.626, 0.0);
         pPoseStack.scale(0.5F, 0.5F, 0.5F);
         this.model.setupAnim(pEntity, $$6, 0.0F, 0.0F, pEntity.getYRot(), pEntity.getXRot());
         VertexConsumer $$9 = pBuffer.getBuffer(this.model.renderType(TEXTURE_LOCATION));
         this.model.renderToBuffer(pPoseStack, $$9, pPackedLight, OverlayTexture.NO_OVERLAY);
         pPoseStack.popPose();
         super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
      }
   }

   public ResourceLocation getTextureLocation(EvokerFangs pEntity) {
      return TEXTURE_LOCATION;
   }
}
