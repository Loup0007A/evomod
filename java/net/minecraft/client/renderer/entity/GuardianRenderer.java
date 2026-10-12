package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianModel> {
   private static final ResourceLocation GUARDIAN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian.png");
   private static final ResourceLocation GUARDIAN_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/guardian_beam.png");
   private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

   public GuardianRenderer(EntityRendererProvider.Context p_174159_) {
      this(p_174159_, 0.5F, ModelLayers.GUARDIAN);
   }

   protected GuardianRenderer(EntityRendererProvider.Context pContext, float pShadowRadius, ModelLayerLocation pLayer) {
      super(pContext, new GuardianModel(pContext.bakeLayer(pLayer)), pShadowRadius);
   }

   public boolean shouldRender(Guardian pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
      if (super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ)) {
         return true;
      } else {
         if (pLivingEntity.hasActiveAttackTarget()) {
            LivingEntity $$5 = pLivingEntity.getActiveAttackTarget();
            if ($$5 != null) {
               Vec3 $$6 = this.getPosition($$5, (double)$$5.getBbHeight() * 0.5, 1.0F);
               Vec3 $$7 = this.getPosition(pLivingEntity, (double)pLivingEntity.getEyeHeight(), 1.0F);
               return pCamera.isVisible(new AABB($$7.x, $$7.y, $$7.z, $$6.x, $$6.y, $$6.z));
            }
         }

         return false;
      }
   }

   private Vec3 getPosition(LivingEntity pLivingEntity, double pYOffset, float pPartialTick) {
      double $$3 = Mth.lerp((double)pPartialTick, pLivingEntity.xOld, pLivingEntity.getX());
      double $$4 = Mth.lerp((double)pPartialTick, pLivingEntity.yOld, pLivingEntity.getY()) + pYOffset;
      double $$5 = Mth.lerp((double)pPartialTick, pLivingEntity.zOld, pLivingEntity.getZ());
      return new Vec3($$3, $$4, $$5);
   }

   public void render(Guardian pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
      LivingEntity $$6 = pEntity.getActiveAttackTarget();
      if ($$6 != null) {
         float $$7 = pEntity.getAttackAnimationScale(pPartialTicks);
         float $$8 = pEntity.getClientSideAttackTime() + pPartialTicks;
         float $$9 = $$8 * 0.5F % 1.0F;
         float $$10 = pEntity.getEyeHeight();
         pPoseStack.pushPose();
         pPoseStack.translate(0.0F, $$10, 0.0F);
         Vec3 $$11 = this.getPosition($$6, (double)$$6.getBbHeight() * 0.5, pPartialTicks);
         Vec3 $$12 = this.getPosition(pEntity, (double)$$10, pPartialTicks);
         Vec3 $$13 = $$11.subtract($$12);
         float $$14 = (float)($$13.length() + 1.0);
         $$13 = $$13.normalize();
         float $$15 = (float)Math.acos($$13.y);
         float $$16 = (float)Math.atan2($$13.z, $$13.x);
         pPoseStack.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2) - $$16) * (180.0F / (float)Math.PI)));
         pPoseStack.mulPose(Axis.XP.rotationDegrees($$15 * (180.0F / (float)Math.PI)));
         int $$17 = 1;
         float $$18 = $$8 * 0.05F * -1.5F;
         float $$19 = $$7 * $$7;
         int $$20 = 64 + (int)($$19 * 191.0F);
         int $$21 = 32 + (int)($$19 * 191.0F);
         int $$22 = 128 - (int)($$19 * 64.0F);
         float $$23 = 0.2F;
         float $$24 = 0.282F;
         float $$25 = Mth.cos($$18 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float $$26 = Mth.sin($$18 + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
         float $$27 = Mth.cos($$18 + (float) (Math.PI / 4)) * 0.282F;
         float $$28 = Mth.sin($$18 + (float) (Math.PI / 4)) * 0.282F;
         float $$29 = Mth.cos($$18 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float $$30 = Mth.sin($$18 + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
         float $$31 = Mth.cos($$18 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float $$32 = Mth.sin($$18 + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
         float $$33 = Mth.cos($$18 + (float) Math.PI) * 0.2F;
         float $$34 = Mth.sin($$18 + (float) Math.PI) * 0.2F;
         float $$35 = Mth.cos($$18 + 0.0F) * 0.2F;
         float $$36 = Mth.sin($$18 + 0.0F) * 0.2F;
         float $$37 = Mth.cos($$18 + (float) (Math.PI / 2)) * 0.2F;
         float $$38 = Mth.sin($$18 + (float) (Math.PI / 2)) * 0.2F;
         float $$39 = Mth.cos($$18 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float $$40 = Mth.sin($$18 + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
         float $$42 = 0.0F;
         float $$43 = 0.4999F;
         float $$44 = -1.0F + $$9;
         float $$45 = $$14 * 2.5F + $$44;
         VertexConsumer $$46 = pBuffer.getBuffer(BEAM_RENDER_TYPE);
         PoseStack.Pose $$47 = pPoseStack.last();
         vertex($$46, $$47, $$33, $$14, $$34, $$20, $$21, $$22, 0.4999F, $$45);
         vertex($$46, $$47, $$33, 0.0F, $$34, $$20, $$21, $$22, 0.4999F, $$44);
         vertex($$46, $$47, $$35, 0.0F, $$36, $$20, $$21, $$22, 0.0F, $$44);
         vertex($$46, $$47, $$35, $$14, $$36, $$20, $$21, $$22, 0.0F, $$45);
         vertex($$46, $$47, $$37, $$14, $$38, $$20, $$21, $$22, 0.4999F, $$45);
         vertex($$46, $$47, $$37, 0.0F, $$38, $$20, $$21, $$22, 0.4999F, $$44);
         vertex($$46, $$47, $$39, 0.0F, $$40, $$20, $$21, $$22, 0.0F, $$44);
         vertex($$46, $$47, $$39, $$14, $$40, $$20, $$21, $$22, 0.0F, $$45);
         float $$48 = 0.0F;
         if (pEntity.tickCount % 2 == 0) {
            $$48 = 0.5F;
         }

         vertex($$46, $$47, $$25, $$14, $$26, $$20, $$21, $$22, 0.5F, $$48 + 0.5F);
         vertex($$46, $$47, $$27, $$14, $$28, $$20, $$21, $$22, 1.0F, $$48 + 0.5F);
         vertex($$46, $$47, $$31, $$14, $$32, $$20, $$21, $$22, 1.0F, $$48);
         vertex($$46, $$47, $$29, $$14, $$30, $$20, $$21, $$22, 0.5F, $$48);
         pPoseStack.popPose();
      }
   }

   private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, float pX, float pY, float pZ, int pRed, int pGreen, int pBlue, float pU, float pV) {
      pConsumer.addVertex(pPose, pX, pY, pZ)
         .setColor(pRed, pGreen, pBlue, 255)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pPose, 0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(Guardian pEntity) {
      return GUARDIAN_LOCATION;
   }
}
