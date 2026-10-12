package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeRenderer extends MobRenderer<Slime, SlimeModel<Slime>> {
   private static final ResourceLocation SLIME_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png");

   public SlimeRenderer(EntityRendererProvider.Context p_174391_) {
      super(p_174391_, new SlimeModel<>(p_174391_.bakeLayer(ModelLayers.SLIME)), 0.25F);
      this.addLayer(new SlimeOuterLayer<>(this, p_174391_.getModelSet()));
   }

   public void render(Slime pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      this.shadowRadius = 0.25F * (float)pEntity.getSize();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   protected void scale(Slime pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
      float $$3 = 0.999F;
      pPoseStack.scale(0.999F, 0.999F, 0.999F);
      pPoseStack.translate(0.0F, 0.001F, 0.0F);
      float $$4 = (float)pLivingEntity.getSize();
      float $$5 = Mth.lerp(pPartialTickTime, pLivingEntity.oSquish, pLivingEntity.squish) / ($$4 * 0.5F + 1.0F);
      float $$6 = 1.0F / ($$5 + 1.0F);
      pPoseStack.scale($$6 * $$4, 1.0F / $$6 * $$4, $$6 * $$4);
   }

   public ResourceLocation getTextureLocation(Slime pEntity) {
      return SLIME_LOCATION;
   }
}
