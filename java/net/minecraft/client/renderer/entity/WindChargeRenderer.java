package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WindChargeRenderer extends EntityRenderer<AbstractWindCharge> {
   private static final float MIN_CAMERA_DISTANCE_SQUARED = Mth.square(3.5F);
   private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");
   private final WindChargeModel model;

   public WindChargeRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.model = new WindChargeModel(pContext.bakeLayer(ModelLayers.WIND_CHARGE));
   }

   public void render(AbstractWindCharge pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight) {
      if (pEntity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(pEntity) < (double)MIN_CAMERA_DISTANCE_SQUARED)) {
         float $$6 = (float)pEntity.tickCount + pPartialTick;
         VertexConsumer $$7 = pBufferSource.getBuffer(RenderType.breezeWind(TEXTURE_LOCATION, this.xOffset($$6) % 1.0F, 0.0F));
         this.model.setupAnim(pEntity, 0.0F, 0.0F, $$6, 0.0F, 0.0F);
         this.model.renderToBuffer(pPoseStack, $$7, pPackedLight, OverlayTexture.NO_OVERLAY);
         super.render(pEntity, pEntityYaw, pPartialTick, pPoseStack, pBufferSource, pPackedLight);
      }
   }

   protected float xOffset(float pTickCount) {
      return pTickCount * 0.03F;
   }

   public ResourceLocation getTextureLocation(AbstractWindCharge pEntity) {
      return TEXTURE_LOCATION;
   }
}
