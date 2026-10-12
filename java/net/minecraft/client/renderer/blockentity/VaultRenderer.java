package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity> {
   private final ItemRenderer itemRenderer;
   private final RandomSource random = RandomSource.create();

   public VaultRenderer(BlockEntityRendererProvider.Context pContext) {
      this.itemRenderer = pContext.getItemRenderer();
   }

   public void render(
      VaultBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      if (VaultBlockEntity.Client.shouldDisplayActiveEffects(pBlockEntity.getSharedData())) {
         Level $$6 = pBlockEntity.getLevel();
         if ($$6 != null) {
            ItemStack $$7 = pBlockEntity.getSharedData().getDisplayItem();
            if (!$$7.isEmpty()) {
               this.random.setSeed((long)ItemEntityRenderer.getSeedForItemStack($$7));
               VaultClientData $$8 = pBlockEntity.getClientData();
               renderItemInside(
                  pPartialTick, $$6, pPoseStack, pBufferSource, pPackedLight, $$7, this.itemRenderer, $$8.previousSpin(), $$8.currentSpin(), this.random
               );
            }
         }
      }
   }

   public static void renderItemInside(
      float pPartialTick,
      Level pLevel,
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      ItemStack pItem,
      ItemRenderer pItemRenderer,
      float pPreviousSpin,
      float pCurrentSpin,
      RandomSource pRandom
   ) {
      pPoseStack.pushPose();
      pPoseStack.translate(0.5F, 0.4F, 0.5F);
      pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(pPartialTick, pPreviousSpin, pCurrentSpin)));
      ItemEntityRenderer.renderMultipleFromCount(pItemRenderer, pPoseStack, pBuffer, pPackedLight, pItem, pRandom, pLevel);
      pPoseStack.popPose();
   }
}
