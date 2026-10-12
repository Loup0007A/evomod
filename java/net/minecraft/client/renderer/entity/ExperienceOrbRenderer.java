package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb> {
   private static final ResourceLocation EXPERIENCE_ORB_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
   private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

   public ExperienceOrbRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   protected int getBlockLightLevel(ExperienceOrb pEntity, BlockPos pPos) {
      return Mth.clamp(super.getBlockLightLevel(pEntity, pPos) + 7, 0, 15);
   }

   public void render(ExperienceOrb pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      int $$6 = pEntity.getIcon();
      float $$7 = (float)($$6 % 4 * 16 + 0) / 64.0F;
      float $$8 = (float)($$6 % 4 * 16 + 16) / 64.0F;
      float $$9 = (float)($$6 / 4 * 16 + 0) / 64.0F;
      float $$10 = (float)($$6 / 4 * 16 + 16) / 64.0F;
      float $$11 = 1.0F;
      float $$12 = 0.5F;
      float $$13 = 0.25F;
      float $$14 = 255.0F;
      float $$15 = ((float)pEntity.tickCount + pPartialTicks) / 2.0F;
      int $$16 = (int)((Mth.sin($$15 + 0.0F) + 1.0F) * 0.5F * 255.0F);
      int $$17 = 255;
      int $$18 = (int)((Mth.sin($$15 + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
      pPoseStack.translate(0.0F, 0.1F, 0.0F);
      pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      float $$19 = 0.3F;
      pPoseStack.scale(0.3F, 0.3F, 0.3F);
      VertexConsumer $$20 = pBuffer.getBuffer(RENDER_TYPE);
      PoseStack.Pose $$21 = pPoseStack.last();
      vertex($$20, $$21, -0.5F, -0.25F, $$16, 255, $$18, $$7, $$10, pPackedLight);
      vertex($$20, $$21, 0.5F, -0.25F, $$16, 255, $$18, $$8, $$10, pPackedLight);
      vertex($$20, $$21, 0.5F, 0.75F, $$16, 255, $$18, $$8, $$9, pPackedLight);
      vertex($$20, $$21, -0.5F, 0.75F, $$16, 255, $$18, $$7, $$9, pPackedLight);
      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   private static void vertex(
      VertexConsumer pConsumer, PoseStack.Pose pPose, float pX, float pY, int pRed, int pGreen, int pBlue, float pU, float pV, int pPackedLight
   ) {
      pConsumer.addVertex(pPose, pX, pY, 0.0F)
         .setColor(pRed, pGreen, pBlue, 128)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(pPackedLight)
         .setNormal(pPose, 0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(ExperienceOrb pEntity) {
      return EXPERIENCE_ORB_LOCATION;
   }
}
