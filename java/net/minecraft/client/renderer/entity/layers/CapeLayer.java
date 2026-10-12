package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CapeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public CapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
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
      if (!pLivingEntity.isInvisible() && pLivingEntity.isModelPartShown(PlayerModelPart.CAPE)) {
         PlayerSkin $$10 = pLivingEntity.getSkin();
         if ($$10.capeTexture() != null) {
            ItemStack $$11 = pLivingEntity.getItemBySlot(EquipmentSlot.CHEST);
            if (!$$11.is(Items.ELYTRA)) {
               pPoseStack.pushPose();
               pPoseStack.translate(0.0F, 0.0F, 0.125F);
               double $$12 = Mth.lerp((double)pPartialTicks, pLivingEntity.xCloakO, pLivingEntity.xCloak)
                  - Mth.lerp((double)pPartialTicks, pLivingEntity.xo, pLivingEntity.getX());
               double $$13 = Mth.lerp((double)pPartialTicks, pLivingEntity.yCloakO, pLivingEntity.yCloak)
                  - Mth.lerp((double)pPartialTicks, pLivingEntity.yo, pLivingEntity.getY());
               double $$14 = Mth.lerp((double)pPartialTicks, pLivingEntity.zCloakO, pLivingEntity.zCloak)
                  - Mth.lerp((double)pPartialTicks, pLivingEntity.zo, pLivingEntity.getZ());
               float $$15 = Mth.rotLerp(pPartialTicks, pLivingEntity.yBodyRotO, pLivingEntity.yBodyRot);
               double $$16 = (double)Mth.sin($$15 * (float) (Math.PI / 180.0));
               double $$17 = (double)(-Mth.cos($$15 * (float) (Math.PI / 180.0)));
               float $$18 = (float)$$13 * 10.0F;
               $$18 = Mth.clamp($$18, -6.0F, 32.0F);
               float $$19 = (float)($$12 * $$16 + $$14 * $$17) * 100.0F;
               $$19 = Mth.clamp($$19, 0.0F, 150.0F);
               float $$20 = (float)($$12 * $$17 - $$14 * $$16) * 100.0F;
               $$20 = Mth.clamp($$20, -20.0F, 20.0F);
               if ($$19 < 0.0F) {
                  $$19 = 0.0F;
               }

               float $$21 = Mth.lerp(pPartialTicks, pLivingEntity.oBob, pLivingEntity.bob);
               $$18 += Mth.sin(Mth.lerp(pPartialTicks, pLivingEntity.walkDistO, pLivingEntity.walkDist) * 6.0F) * 32.0F * $$21;
               if (pLivingEntity.isCrouching()) {
                  $$18 += 25.0F;
               }

               pPoseStack.mulPose(Axis.XP.rotationDegrees(6.0F + $$19 / 2.0F + $$18));
               pPoseStack.mulPose(Axis.ZP.rotationDegrees($$20 / 2.0F));
               pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - $$20 / 2.0F));
               VertexConsumer $$22 = pBuffer.getBuffer(RenderType.entitySolid($$10.capeTexture()));
               this.getParentModel().renderCloak(pPoseStack, $$22, pPackedLight, OverlayTexture.NO_OVERLAY);
               pPoseStack.popPose();
            }
         }
      }
   }
}
