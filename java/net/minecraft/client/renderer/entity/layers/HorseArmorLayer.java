package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseArmorLayer extends RenderLayer<Horse, HorseModel<Horse>> {
   private final HorseModel<Horse> model;

   public HorseArmorLayer(RenderLayerParent<Horse, HorseModel<Horse>> pRenderer, EntityModelSet pModelSet) {
      super(pRenderer);
      this.model = new HorseModel<>(pModelSet.bakeLayer(ModelLayers.HORSE_ARMOR));
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      Horse pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      ItemStack $$10 = pLivingEntity.getBodyArmorItem();
      if ($$10.getItem() instanceof AnimalArmorItem $$11 && $$11.getBodyType() == AnimalArmorItem.BodyType.EQUESTRIAN) {
         this.getParentModel().copyPropertiesTo(this.model);
         this.model.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTicks);
         this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
         int $$13;
         if ($$10.is(ItemTags.DYEABLE)) {
            $$13 = FastColor.ARGB32.opaque(DyedItemColor.getOrDefault($$10, -6265536));
         } else {
            $$13 = -1;
         }

         VertexConsumer $$15 = pBuffer.getBuffer(RenderType.entityCutoutNoCull($$11.getTexture()));
         this.model.renderToBuffer(pPoseStack, $$15, pPackedLight, OverlayTexture.NO_OVERLAY, $$13);
         return;
      }
   }
}
