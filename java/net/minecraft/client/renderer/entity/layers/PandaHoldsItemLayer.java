package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaHoldsItemLayer extends RenderLayer<Panda, PandaModel<Panda>> {
   private final ItemInHandRenderer itemInHandRenderer;

   public PandaHoldsItemLayer(RenderLayerParent<Panda, PandaModel<Panda>> pRenderer, ItemInHandRenderer pItemInHandRenderer) {
      super(pRenderer);
      this.itemInHandRenderer = pItemInHandRenderer;
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      Panda pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      ItemStack $$10 = pLivingEntity.getItemBySlot(EquipmentSlot.MAINHAND);
      if (pLivingEntity.isSitting() && !pLivingEntity.isScared()) {
         float $$11 = -0.6F;
         float $$12 = 1.4F;
         if (pLivingEntity.isEating()) {
            $$11 -= 0.2F * Mth.sin(pAgeInTicks * 0.6F) + 0.2F;
            $$12 -= 0.09F * Mth.sin(pAgeInTicks * 0.6F);
         }

         pPoseStack.pushPose();
         pPoseStack.translate(0.1F, $$12, $$11);
         this.itemInHandRenderer.renderItem(pLivingEntity, $$10, ItemDisplayContext.GROUND, false, pPoseStack, pBuffer, pPackedLight);
         pPoseStack.popPose();
      }
   }
}
