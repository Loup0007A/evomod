package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity> {
   private final ShulkerModel<?> model;

   public ShulkerBoxRenderer(BlockEntityRendererProvider.Context pContext) {
      this.model = new ShulkerModel(pContext.bakeLayer(ModelLayers.SHULKER));
   }

   public void render(
      ShulkerBoxBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      Direction $$6 = Direction.UP;
      if (pBlockEntity.hasLevel()) {
         BlockState $$7 = pBlockEntity.getLevel().getBlockState(pBlockEntity.getBlockPos());
         if ($$7.getBlock() instanceof ShulkerBoxBlock) {
            $$6 = $$7.getValue(ShulkerBoxBlock.FACING);
         }
      }

      DyeColor $$8 = pBlockEntity.getColor();
      Material $$9;
      if ($$8 == null) {
         $$9 = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
      } else {
         $$9 = Sheets.SHULKER_TEXTURE_LOCATION.get($$8.getId());
      }

      pPoseStack.pushPose();
      pPoseStack.translate(0.5F, 0.5F, 0.5F);
      float $$11 = 0.9995F;
      pPoseStack.scale(0.9995F, 0.9995F, 0.9995F);
      pPoseStack.mulPose($$6.getRotation());
      pPoseStack.scale(1.0F, -1.0F, -1.0F);
      pPoseStack.translate(0.0F, -1.0F, 0.0F);
      ModelPart $$12 = this.model.getLid();
      $$12.setPos(0.0F, 24.0F - pBlockEntity.getProgress(pPartialTick) * 0.5F * 16.0F, 0.0F);
      $$12.yRot = 270.0F * pBlockEntity.getProgress(pPartialTick) * (float) (Math.PI / 180.0);
      VertexConsumer $$13 = $$9.buffer(pBufferSource, RenderType::entityCutoutNoCull);
      this.model.renderToBuffer(pPoseStack, $$13, pPackedLight, pPackedOverlay);
      pPoseStack.popPose();
   }
}
