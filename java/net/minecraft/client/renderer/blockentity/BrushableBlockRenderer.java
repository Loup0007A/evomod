package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BrushableBlockRenderer implements BlockEntityRenderer<BrushableBlockEntity> {
   private final ItemRenderer itemRenderer;

   public BrushableBlockRenderer(BlockEntityRendererProvider.Context pContext) {
      this.itemRenderer = pContext.getItemRenderer();
   }

   public void render(
      BrushableBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      if (pBlockEntity.getLevel() != null) {
         int $$6 = pBlockEntity.getBlockState().getValue(BlockStateProperties.DUSTED);
         if ($$6 > 0) {
            Direction $$7 = pBlockEntity.getHitDirection();
            if ($$7 != null) {
               ItemStack $$8 = pBlockEntity.getItem();
               if (!$$8.isEmpty()) {
                  pPoseStack.pushPose();
                  pPoseStack.translate(0.0F, 0.5F, 0.0F);
                  float[] $$9 = this.translations($$7, $$6);
                  pPoseStack.translate($$9[0], $$9[1], $$9[2]);
                  pPoseStack.mulPose(Axis.YP.rotationDegrees(75.0F));
                  boolean $$10 = $$7 == Direction.EAST || $$7 == Direction.WEST;
                  pPoseStack.mulPose(Axis.YP.rotationDegrees((float)(($$10 ? 90 : 0) + 11)));
                  pPoseStack.scale(0.5F, 0.5F, 0.5F);
                  int $$11 = LevelRenderer.getLightColor(pBlockEntity.getLevel(), pBlockEntity.getBlockState(), pBlockEntity.getBlockPos().relative($$7));
                  this.itemRenderer
                     .renderStatic($$8, ItemDisplayContext.FIXED, $$11, OverlayTexture.NO_OVERLAY, pPoseStack, pBufferSource, pBlockEntity.getLevel(), 0);
                  pPoseStack.popPose();
               }
            }
         }
      }
   }

   private float[] translations(Direction pDirection, int pDustedLevel) {
      float[] $$2 = new float[]{0.5F, 0.0F, 0.5F};
      float $$3 = (float)pDustedLevel / 10.0F * 0.75F;
      switch (pDirection) {
         case EAST:
            $$2[0] = 0.73F + $$3;
            break;
         case WEST:
            $$2[0] = 0.25F - $$3;
            break;
         case UP:
            $$2[1] = 0.25F + $$3;
            break;
         case DOWN:
            $$2[1] = -0.23F - $$3;
            break;
         case NORTH:
            $$2[2] = 0.25F - $$3;
            break;
         case SOUTH:
            $$2[2] = 0.73F + $$3;
      }

      return $$2;
   }
}
