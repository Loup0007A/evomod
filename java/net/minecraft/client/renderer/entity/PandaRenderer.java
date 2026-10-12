package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Panda;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaRenderer extends MobRenderer<Panda, PandaModel<Panda>> {
   private static final Map<Panda.Gene, ResourceLocation> TEXTURES = Util.make(Maps.newEnumMap(Panda.Gene.class), p_340941_ -> {
      p_340941_.put(Panda.Gene.NORMAL, ResourceLocation.withDefaultNamespace("textures/entity/panda/panda.png"));
      p_340941_.put(Panda.Gene.LAZY, ResourceLocation.withDefaultNamespace("textures/entity/panda/lazy_panda.png"));
      p_340941_.put(Panda.Gene.WORRIED, ResourceLocation.withDefaultNamespace("textures/entity/panda/worried_panda.png"));
      p_340941_.put(Panda.Gene.PLAYFUL, ResourceLocation.withDefaultNamespace("textures/entity/panda/playful_panda.png"));
      p_340941_.put(Panda.Gene.BROWN, ResourceLocation.withDefaultNamespace("textures/entity/panda/brown_panda.png"));
      p_340941_.put(Panda.Gene.WEAK, ResourceLocation.withDefaultNamespace("textures/entity/panda/weak_panda.png"));
      p_340941_.put(Panda.Gene.AGGRESSIVE, ResourceLocation.withDefaultNamespace("textures/entity/panda/aggressive_panda.png"));
   });

   public PandaRenderer(EntityRendererProvider.Context p_174334_) {
      super(p_174334_, new PandaModel<>(p_174334_.bakeLayer(ModelLayers.PANDA)), 0.9F);
      this.addLayer(new PandaHoldsItemLayer(this, p_174334_.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(Panda pEntity) {
      return TEXTURES.getOrDefault(pEntity.getVariant(), TEXTURES.get(Panda.Gene.NORMAL));
   }

   protected void setupRotations(Panda pEntity, PoseStack pPoseStack, float pBob, float pYBodyRot, float pPartialTick, float pScale) {
      super.setupRotations(pEntity, pPoseStack, pBob, pYBodyRot, pPartialTick, pScale);
      if (pEntity.rollCounter > 0) {
         int $$6 = pEntity.rollCounter;
         int $$7 = $$6 + 1;
         float $$8 = 7.0F;
         float $$9 = pEntity.isBaby() ? 0.3F : 0.8F;
         if ($$6 < 8) {
            float $$10 = (float)(90 * $$6) / 7.0F;
            float $$11 = (float)(90 * $$7) / 7.0F;
            float $$12 = this.getAngle($$10, $$11, $$7, pPartialTick, 8.0F);
            pPoseStack.translate(0.0F, ($$9 + 0.2F) * ($$12 / 90.0F), 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-$$12));
         } else if ($$6 < 16) {
            float $$13 = ((float)$$6 - 8.0F) / 7.0F;
            float $$14 = 90.0F + 90.0F * $$13;
            float $$15 = 90.0F + 90.0F * ((float)$$7 - 8.0F) / 7.0F;
            float $$16 = this.getAngle($$14, $$15, $$7, pPartialTick, 16.0F);
            pPoseStack.translate(0.0F, $$9 + 0.2F + ($$9 - 0.2F) * ($$16 - 90.0F) / 90.0F, 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-$$16));
         } else if ((float)$$6 < 24.0F) {
            float $$17 = ((float)$$6 - 16.0F) / 7.0F;
            float $$18 = 180.0F + 90.0F * $$17;
            float $$19 = 180.0F + 90.0F * ((float)$$7 - 16.0F) / 7.0F;
            float $$20 = this.getAngle($$18, $$19, $$7, pPartialTick, 24.0F);
            pPoseStack.translate(0.0F, $$9 + $$9 * (270.0F - $$20) / 90.0F, 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-$$20));
         } else if ($$6 < 32) {
            float $$21 = ((float)$$6 - 24.0F) / 7.0F;
            float $$22 = 270.0F + 90.0F * $$21;
            float $$23 = 270.0F + 90.0F * ((float)$$7 - 24.0F) / 7.0F;
            float $$24 = this.getAngle($$22, $$23, $$7, pPartialTick, 32.0F);
            pPoseStack.translate(0.0F, $$9 * ((360.0F - $$24) / 90.0F), 0.0F);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-$$24));
         }
      }

      float $$25 = pEntity.getSitAmount(pPartialTick);
      if ($$25 > 0.0F) {
         pPoseStack.translate(0.0F, 0.8F * $$25, 0.0F);
         pPoseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp($$25, pEntity.getXRot(), pEntity.getXRot() + 90.0F)));
         pPoseStack.translate(0.0F, -1.0F * $$25, 0.0F);
         if (pEntity.isScared()) {
            float $$26 = (float)(Math.cos((double)pEntity.tickCount * 1.25) * Math.PI * 0.05F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees($$26));
            if (pEntity.isBaby()) {
               pPoseStack.translate(0.0F, 0.8F, 0.55F);
            }
         }
      }

      float $$27 = pEntity.getLieOnBackAmount(pPartialTick);
      if ($$27 > 0.0F) {
         float $$28 = pEntity.isBaby() ? 0.5F : 1.3F;
         pPoseStack.translate(0.0F, $$28 * $$27, 0.0F);
         pPoseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp($$27, pEntity.getXRot(), pEntity.getXRot() + 180.0F)));
      }
   }

   private float getAngle(float pCurrentAngle, float pNextAngle, int pNextRollCounter, float pPartialTick, float pRollEndCount) {
      return (float)pNextRollCounter < pRollEndCount ? Mth.lerp(pPartialTick, pCurrentAngle, pNextAngle) : pCurrentAngle;
   }
}
