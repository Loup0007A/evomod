package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreeperRenderer extends MobRenderer<Creeper, CreeperModel<Creeper>> {
   private static final ResourceLocation CREEPER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");

   public CreeperRenderer(EntityRendererProvider.Context p_173958_) {
      super(p_173958_, new CreeperModel<>(p_173958_.bakeLayer(ModelLayers.CREEPER)), 0.5F);
      this.addLayer(new CreeperPowerLayer(this, p_173958_.getModelSet()));
   }

   protected void scale(Creeper pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
      float $$3 = pLivingEntity.getSwelling(pPartialTickTime);
      float $$4 = 1.0F + Mth.sin($$3 * 100.0F) * $$3 * 0.01F;
      $$3 = Mth.clamp($$3, 0.0F, 1.0F);
      $$3 *= $$3;
      $$3 *= $$3;
      float $$5 = (1.0F + $$3 * 0.4F) * $$4;
      float $$6 = (1.0F + $$3 * 0.1F) / $$4;
      pPoseStack.scale($$5, $$6, $$5);
   }

   protected float getWhiteOverlayProgress(Creeper pLivingEntity, float pPartialTicks) {
      float $$2 = pLivingEntity.getSwelling(pPartialTicks);
      return (int)($$2 * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp($$2, 0.5F, 1.0F);
   }

   public ResourceLocation getTextureLocation(Creeper pEntity) {
      return CREEPER_LOCATION;
   }
}
