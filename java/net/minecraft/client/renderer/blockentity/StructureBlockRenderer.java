package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureBlockRenderer implements BlockEntityRenderer<StructureBlockEntity> {
   public StructureBlockRenderer(BlockEntityRendererProvider.Context pContext) {
   }

   public void render(
      StructureBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      if (Minecraft.getInstance().player.canUseGameMasterBlocks() || Minecraft.getInstance().player.isSpectator()) {
         BlockPos $$6 = pBlockEntity.getStructurePos();
         Vec3i $$7 = pBlockEntity.getStructureSize();
         if ($$7.getX() >= 1 && $$7.getY() >= 1 && $$7.getZ() >= 1) {
            if (pBlockEntity.getMode() == StructureMode.SAVE || pBlockEntity.getMode() == StructureMode.LOAD) {
               double $$8 = (double)$$6.getX();
               double $$9 = (double)$$6.getZ();
               double $$10 = (double)$$6.getY();
               double $$11 = $$10 + (double)$$7.getY();
               double $$12;
               double $$13;
               switch (pBlockEntity.getMirror()) {
                  case LEFT_RIGHT:
                     $$12 = (double)$$7.getX();
                     $$13 = (double)(-$$7.getZ());
                     break;
                  case FRONT_BACK:
                     $$12 = (double)(-$$7.getX());
                     $$13 = (double)$$7.getZ();
                     break;
                  default:
                     $$12 = (double)$$7.getX();
                     $$13 = (double)$$7.getZ();
               }

               double $$30;
               double $$31;
               double $$32;
               double $$33;
               switch (pBlockEntity.getRotation()) {
                  case CLOCKWISE_90:
                     $$30 = $$13 < 0.0 ? $$8 : $$8 + 1.0;
                     $$31 = $$12 < 0.0 ? $$9 + 1.0 : $$9;
                     $$32 = $$30 - $$13;
                     $$33 = $$31 + $$12;
                     break;
                  case CLOCKWISE_180:
                     $$30 = $$12 < 0.0 ? $$8 : $$8 + 1.0;
                     $$31 = $$13 < 0.0 ? $$9 : $$9 + 1.0;
                     $$32 = $$30 - $$12;
                     $$33 = $$31 - $$13;
                     break;
                  case COUNTERCLOCKWISE_90:
                     $$30 = $$13 < 0.0 ? $$8 + 1.0 : $$8;
                     $$31 = $$12 < 0.0 ? $$9 : $$9 + 1.0;
                     $$32 = $$30 + $$13;
                     $$33 = $$31 - $$12;
                     break;
                  default:
                     $$30 = $$12 < 0.0 ? $$8 + 1.0 : $$8;
                     $$31 = $$13 < 0.0 ? $$9 + 1.0 : $$9;
                     $$32 = $$30 + $$12;
                     $$33 = $$31 + $$13;
               }

               float $$34 = 1.0F;
               float $$35 = 0.9F;
               float $$36 = 0.5F;
               if (pBlockEntity.getMode() == StructureMode.SAVE || pBlockEntity.getShowBoundingBox()) {
                  VertexConsumer $$37 = pBufferSource.getBuffer(RenderType.lines());
                  LevelRenderer.renderLineBox(pPoseStack, $$37, $$30, $$10, $$31, $$32, $$11, $$33, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
               }

               if (pBlockEntity.getMode() == StructureMode.SAVE && pBlockEntity.getShowAir()) {
                  this.renderInvisibleBlocks(pBlockEntity, pBufferSource, pPoseStack);
               }
            }
         }
      }
   }

   private void renderInvisibleBlocks(StructureBlockEntity pBlockEntity, MultiBufferSource pBufferSource, PoseStack pPoseStack) {
      BlockGetter $$3 = pBlockEntity.getLevel();
      VertexConsumer $$4 = pBufferSource.getBuffer(RenderType.lines());
      BlockPos $$5 = pBlockEntity.getBlockPos();
      BlockPos $$6 = StructureUtils.getStructureOrigin(pBlockEntity);

      for (BlockPos $$7 : BlockPos.betweenClosed($$6, $$6.offset(pBlockEntity.getStructureSize()).offset(-1, -1, -1))) {
         BlockState $$8 = $$3.getBlockState($$7);
         boolean $$9 = $$8.isAir();
         boolean $$10 = $$8.is(Blocks.STRUCTURE_VOID);
         boolean $$11 = $$8.is(Blocks.BARRIER);
         boolean $$12 = $$8.is(Blocks.LIGHT);
         boolean $$13 = $$10 || $$11 || $$12;
         if ($$9 || $$13) {
            float $$14 = $$9 ? 0.05F : 0.0F;
            double $$15 = (double)((float)($$7.getX() - $$5.getX()) + 0.45F - $$14);
            double $$16 = (double)((float)($$7.getY() - $$5.getY()) + 0.45F - $$14);
            double $$17 = (double)((float)($$7.getZ() - $$5.getZ()) + 0.45F - $$14);
            double $$18 = (double)((float)($$7.getX() - $$5.getX()) + 0.55F + $$14);
            double $$19 = (double)((float)($$7.getY() - $$5.getY()) + 0.55F + $$14);
            double $$20 = (double)((float)($$7.getZ() - $$5.getZ()) + 0.55F + $$14);
            if ($$9) {
               LevelRenderer.renderLineBox(pPoseStack, $$4, $$15, $$16, $$17, $$18, $$19, $$20, 0.5F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F, 1.0F);
            } else if ($$10) {
               LevelRenderer.renderLineBox(pPoseStack, $$4, $$15, $$16, $$17, $$18, $$19, $$20, 1.0F, 0.75F, 0.75F, 1.0F, 1.0F, 0.75F, 0.75F);
            } else if ($$11) {
               LevelRenderer.renderLineBox(pPoseStack, $$4, $$15, $$16, $$17, $$18, $$19, $$20, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
            } else if ($$12) {
               LevelRenderer.renderLineBox(pPoseStack, $$4, $$15, $$16, $$17, $$18, $$19, $$20, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
            }
         }
      }
   }

   private void renderStructureVoids(StructureBlockEntity pBlockEntity, VertexConsumer pBuffer, PoseStack pPoseStack) {
      BlockGetter $$3 = pBlockEntity.getLevel();
      if ($$3 != null) {
         BlockPos $$4 = pBlockEntity.getBlockPos();
         BlockPos $$5 = StructureUtils.getStructureOrigin(pBlockEntity);
         Vec3i $$6 = pBlockEntity.getStructureSize();
         DiscreteVoxelShape $$7 = new BitSetDiscreteVoxelShape($$6.getX(), $$6.getY(), $$6.getZ());

         for (BlockPos $$8 : BlockPos.betweenClosed($$5, $$5.offset($$6).offset(-1, -1, -1))) {
            if ($$3.getBlockState($$8).is(Blocks.STRUCTURE_VOID)) {
               $$7.fill($$8.getX() - $$5.getX(), $$8.getY() - $$5.getY(), $$8.getZ() - $$5.getZ());
            }
         }

         $$7.forAllFaces((p_342299_, p_345263_, p_344165_, p_344494_) -> {
            float $$8x = 0.48F;
            float $$9 = (float)(p_345263_ + $$5.getX() - $$4.getX()) + 0.5F - 0.48F;
            float $$10 = (float)(p_344165_ + $$5.getY() - $$4.getY()) + 0.5F - 0.48F;
            float $$11 = (float)(p_344494_ + $$5.getZ() - $$4.getZ()) + 0.5F - 0.48F;
            float $$12 = (float)(p_345263_ + $$5.getX() - $$4.getX()) + 0.5F + 0.48F;
            float $$13 = (float)(p_344165_ + $$5.getY() - $$4.getY()) + 0.5F + 0.48F;
            float $$14 = (float)(p_344494_ + $$5.getZ() - $$4.getZ()) + 0.5F + 0.48F;
            LevelRenderer.renderFace(pPoseStack, pBuffer, p_342299_, $$9, $$10, $$11, $$12, $$13, $$14, 0.75F, 0.75F, 1.0F, 0.2F);
         });
      }
   }

   public boolean shouldRenderOffScreen(StructureBlockEntity pBlockEntity) {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 96;
   }
}
