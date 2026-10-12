package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
   private final WolfModel<Wolf> model;
   private static final Map<Crackiness.Level, ResourceLocation> ARMOR_CRACK_LOCATIONS = Map.of(
      Crackiness.Level.LOW,
      ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
      Crackiness.Level.MEDIUM,
      ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
      Crackiness.Level.HIGH,
      ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
   );

   public WolfArmorLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> pRenderer, EntityModelSet pModels) {
      super(pRenderer);
      this.model = new WolfModel<>(pModels.bakeLayer(ModelLayers.WOLF_ARMOR));
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBufferSource,
      int pPackedLight,
      Wolf pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTick,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      if (pLivingEntity.hasArmor()) {
         ItemStack $$10 = pLivingEntity.getBodyArmorItem();
         if ($$10.getItem() instanceof AnimalArmorItem $$11 && $$11.getBodyType() == AnimalArmorItem.BodyType.CANINE) {
            this.getParentModel().copyPropertiesTo(this.model);
            this.model.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTick);
            this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
            VertexConsumer $$13 = pBufferSource.getBuffer(RenderType.entityCutoutNoCull($$11.getTexture()));
            this.model.renderToBuffer(pPoseStack, $$13, pPackedLight, OverlayTexture.NO_OVERLAY);
            this.maybeRenderColoredLayer(pPoseStack, pBufferSource, pPackedLight, $$10, $$11);
            this.maybeRenderCracks(pPoseStack, pBufferSource, pPackedLight, $$10);
            return;
         }
      }
   }

   private void maybeRenderColoredLayer(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ItemStack pArmorStack, AnimalArmorItem pArmorItem) {
      if (pArmorStack.is(ItemTags.DYEABLE)) {
         int $$5 = DyedItemColor.getOrDefault(pArmorStack, 0);
         if (FastColor.ARGB32.alpha($$5) == 0) {
            return;
         }

         ResourceLocation $$6 = pArmorItem.getOverlayTexture();
         if ($$6 == null) {
            return;
         }

         this.model
            .renderToBuffer(
               pPoseStack, pBuffer.getBuffer(RenderType.entityCutoutNoCull($$6)), pPackedLight, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.opaque($$5)
            );
      }
   }

   private void maybeRenderCracks(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ItemStack pArmorStack) {
      Crackiness.Level $$4 = Crackiness.WOLF_ARMOR.byDamage(pArmorStack);
      if ($$4 != Crackiness.Level.NONE) {
         ResourceLocation $$5 = ARMOR_CRACK_LOCATIONS.get($$4);
         VertexConsumer $$6 = pBuffer.getBuffer(RenderType.entityTranslucent($$5));
         this.model.renderToBuffer(pPoseStack, $$6, pPackedLight, OverlayTexture.NO_OVERLAY);
      }
   }
}
