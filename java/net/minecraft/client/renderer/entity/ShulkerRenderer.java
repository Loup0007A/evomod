package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.layers.ShulkerHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerModel<Shulker>> {
   private static final ResourceLocation DEFAULT_TEXTURE_LOCATION = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION
      .texture()
      .withPath(p_340944_ -> "textures/" + p_340944_ + ".png");
   private static final ResourceLocation[] TEXTURE_LOCATION = Sheets.SHULKER_TEXTURE_LOCATION
      .stream()
      .map(p_340943_ -> p_340943_.texture().withPath(p_340942_ -> "textures/" + p_340942_ + ".png"))
      .toArray(ResourceLocation[]::new);

   public ShulkerRenderer(EntityRendererProvider.Context p_174370_) {
      super(p_174370_, new ShulkerModel<>(p_174370_.bakeLayer(ModelLayers.SHULKER)), 0.0F);
      this.addLayer(new ShulkerHeadLayer(this));
   }

   public Vec3 getRenderOffset(Shulker pEntity, float pPartialTicks) {
      return pEntity.getRenderPosition(pPartialTicks).orElse(super.getRenderOffset(pEntity, pPartialTicks)).scale((double)pEntity.getScale());
   }

   public boolean shouldRender(Shulker pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
      return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ)
         ? true
         : pLivingEntity.getRenderPosition(0.0F)
            .filter(
               p_174374_ -> {
                  EntityType<?> $$3 = pLivingEntity.getType();
                  float $$4 = $$3.getHeight() / 2.0F;
                  float $$5 = $$3.getWidth() / 2.0F;
                  Vec3 $$6 = Vec3.atBottomCenterOf(pLivingEntity.blockPosition());
                  return pCamera.isVisible(
                     new AABB(p_174374_.x, p_174374_.y + (double)$$4, p_174374_.z, $$6.x, $$6.y + (double)$$4, $$6.z)
                        .inflate((double)$$5, (double)$$4, (double)$$5)
                  );
               }
            )
            .isPresent();
   }

   public ResourceLocation getTextureLocation(Shulker pEntity) {
      return getTextureLocation(pEntity.getColor());
   }

   public static ResourceLocation getTextureLocation(@Nullable DyeColor pColor) {
      return pColor == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[pColor.getId()];
   }

   protected void setupRotations(Shulker pEntity, PoseStack pPoseStack, float pBob, float pYBodyRot, float pPartialTick, float pScale) {
      super.setupRotations(pEntity, pPoseStack, pBob, pYBodyRot + 180.0F, pPartialTick, pScale);
      pPoseStack.rotateAround(pEntity.getAttachFace().getOpposite().getRotation(), 0.0F, 0.5F, 0.0F);
   }
}
