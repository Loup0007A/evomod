package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemHeadLayer extends RenderLayer<SnowGolem, SnowGolemModel<SnowGolem>> {
   private final BlockRenderDispatcher blockRenderer;
   private final ItemRenderer itemRenderer;

   public SnowGolemHeadLayer(
      RenderLayerParent<SnowGolem, SnowGolemModel<SnowGolem>> pRenderer, BlockRenderDispatcher pBlockRenderer, ItemRenderer pItemRenderer
   ) {
      super(pRenderer);
      this.blockRenderer = pBlockRenderer;
      this.itemRenderer = pItemRenderer;
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      SnowGolem pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      if (pLivingEntity.hasPumpkin()) {
         boolean $$10 = Minecraft.getInstance().shouldEntityAppearGlowing(pLivingEntity) && pLivingEntity.isInvisible();
         if (!pLivingEntity.isInvisible() || $$10) {
            pPoseStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(pPoseStack);
            float $$11 = 0.625F;
            pPoseStack.translate(0.0F, -0.34375F, 0.0F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            pPoseStack.scale(0.625F, -0.625F, -0.625F);
            ItemStack $$12 = new ItemStack(Blocks.CARVED_PUMPKIN);
            if ($$10) {
               BlockState $$13 = Blocks.CARVED_PUMPKIN.defaultBlockState();
               BakedModel $$14 = this.blockRenderer.getBlockModel($$13);
               int $$15 = LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F);
               pPoseStack.translate(-0.5F, -0.5F, -0.5F);
               this.blockRenderer
                  .getModelRenderer()
                  .renderModel(
                     pPoseStack.last(), pBuffer.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)), $$13, $$14, 0.0F, 0.0F, 0.0F, pPackedLight, $$15
                  );
            } else {
               this.itemRenderer
                  .renderStatic(
                     pLivingEntity,
                     $$12,
                     ItemDisplayContext.HEAD,
                     false,
                     pPoseStack,
                     pBuffer,
                     pLivingEntity.level(),
                     pPackedLight,
                     LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F),
                     pLivingEntity.getId()
                  );
            }

            pPoseStack.popPose();
         }
      }
   }
}
