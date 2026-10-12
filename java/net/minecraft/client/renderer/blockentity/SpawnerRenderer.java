package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity> {
   private final EntityRenderDispatcher entityRenderer;

   public SpawnerRenderer(BlockEntityRendererProvider.Context pContext) {
      this.entityRenderer = pContext.getEntityRenderer();
   }

   public void render(
      SpawnerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      Level $$6 = pBlockEntity.getLevel();
      if ($$6 != null) {
         BaseSpawner $$7 = pBlockEntity.getSpawner();
         Entity $$8 = $$7.getOrCreateDisplayEntity($$6, pBlockEntity.getBlockPos());
         if ($$8 != null) {
            renderEntityInSpawner(pPartialTick, pPoseStack, pBufferSource, pPackedLight, $$8, this.entityRenderer, $$7.getoSpin(), $$7.getSpin());
         }
      }
   }

   public static void renderEntityInSpawner(
      float pPartialTick,
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      Entity pEntity,
      EntityRenderDispatcher pEntityRenderer,
      double pOSpin,
      double pSpin
   ) {
      pPoseStack.pushPose();
      pPoseStack.translate(0.5F, 0.0F, 0.5F);
      float $$8 = 0.53125F;
      float $$9 = Math.max(pEntity.getBbWidth(), pEntity.getBbHeight());
      if ((double)$$9 > 1.0) {
         $$8 /= $$9;
      }

      pPoseStack.translate(0.0F, 0.4F, 0.0F);
      pPoseStack.mulPose(Axis.YP.rotationDegrees((float)Mth.lerp((double)pPartialTick, pOSpin, pSpin) * 10.0F));
      pPoseStack.translate(0.0F, -0.2F, 0.0F);
      pPoseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
      pPoseStack.scale($$8, $$8, $$8);
      pEntityRenderer.render(pEntity, 0.0, 0.0, 0.0, 0.0F, pPartialTick, pPoseStack, pBuffer, pPackedLight);
      pPoseStack.popPose();
   }
}
