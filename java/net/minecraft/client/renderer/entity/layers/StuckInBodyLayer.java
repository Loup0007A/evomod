package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StuckInBodyLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
   public StuckInBodyLayer(LivingEntityRenderer<T, M> pRenderer) {
      super(pRenderer);
   }

   protected abstract int numStuck(T pEntity);

   protected abstract void renderStuckItem(
      PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Entity pEntity, float pX, float pY, float pZ, float pPartialTick
   );

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      T pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      int $$10 = this.numStuck(pLivingEntity);
      RandomSource $$11 = RandomSource.create((long)pLivingEntity.getId());
      if ($$10 > 0) {
         for (int $$12 = 0; $$12 < $$10; $$12++) {
            pPoseStack.pushPose();
            ModelPart $$13 = this.getParentModel().getRandomModelPart($$11);
            ModelPart.Cube $$14 = $$13.getRandomCube($$11);
            $$13.translateAndRotate(pPoseStack);
            float $$15 = $$11.nextFloat();
            float $$16 = $$11.nextFloat();
            float $$17 = $$11.nextFloat();
            float $$18 = Mth.lerp($$15, $$14.minX, $$14.maxX) / 16.0F;
            float $$19 = Mth.lerp($$16, $$14.minY, $$14.maxY) / 16.0F;
            float $$20 = Mth.lerp($$17, $$14.minZ, $$14.maxZ) / 16.0F;
            pPoseStack.translate($$18, $$19, $$20);
            $$15 = -1.0F * ($$15 * 2.0F - 1.0F);
            $$16 = -1.0F * ($$16 * 2.0F - 1.0F);
            $$17 = -1.0F * ($$17 * 2.0F - 1.0F);
            this.renderStuckItem(pPoseStack, pBuffer, pPackedLight, pLivingEntity, $$15, $$16, $$17, pPartialTicks);
            pPoseStack.popPose();
         }
      }
   }
}
