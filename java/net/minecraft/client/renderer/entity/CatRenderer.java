package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatRenderer extends MobRenderer<Cat, CatModel<Cat>> {
   public CatRenderer(EntityRendererProvider.Context p_173943_) {
      super(p_173943_, new CatModel<>(p_173943_.bakeLayer(ModelLayers.CAT)), 0.4F);
      this.addLayer(new CatCollarLayer(this, p_173943_.getModelSet()));
   }

   public ResourceLocation getTextureLocation(Cat pEntity) {
      return pEntity.getTextureId();
   }

   protected void scale(Cat pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
      super.scale(pLivingEntity, pPoseStack, pPartialTickTime);
      pPoseStack.scale(0.8F, 0.8F, 0.8F);
   }

   protected void setupRotations(Cat pEntity, PoseStack pPoseStack, float pBob, float pYBodyRot, float pPartialTick, float pScale) {
      super.setupRotations(pEntity, pPoseStack, pBob, pYBodyRot, pPartialTick, pScale);
      float $$6 = pEntity.getLieDownAmount(pPartialTick);
      if ($$6 > 0.0F) {
         pPoseStack.translate(0.4F * $$6, 0.15F * $$6, 0.1F * $$6);
         pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp($$6, 0.0F, 90.0F)));
         BlockPos $$7 = pEntity.blockPosition();

         for (Player $$9 : pEntity.level().getEntitiesOfClass(Player.class, new AABB($$7).inflate(2.0, 2.0, 2.0))) {
            if ($$9.isSleeping()) {
               pPoseStack.translate(0.15F * $$6, 0.0F, 0.0F);
               break;
            }
         }
      }
   }
}
