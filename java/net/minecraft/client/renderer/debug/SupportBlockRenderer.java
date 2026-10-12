package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      double $$5 = (double)Util.getNanos();
      if ($$5 - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = $$5;
         Entity $$6 = this.minecraft.gameRenderer.getMainCamera().getEntity();
         this.surroundEntities = ImmutableList.copyOf($$6.level().getEntities($$6, $$6.getBoundingBox().inflate(16.0)));
      }

      Player $$7 = this.minecraft.player;
      if ($$7 != null && $$7.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ, $$7, () -> 0.0, 1.0F, 0.0F, 0.0F);
      }

      for (Entity $$8 : this.surroundEntities) {
         if ($$8 != $$7) {
            this.drawHighlights(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ, $$8, () -> this.getBias($$8), 0.0F, 1.0F, 0.0F);
         }
      }
   }

   private void drawHighlights(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      double pCamX,
      double pCamY,
      double pCamZ,
      Entity pEntity,
      DoubleSupplier pBiasGetter,
      float pRed,
      float pGreen,
      float pBlue
   ) {
      pEntity.mainSupportingBlockPos.ifPresent(p_286428_ -> {
         double $$11 = pBiasGetter.getAsDouble();
         BlockPos $$12 = pEntity.getOnPos();
         this.highlightPosition($$12, pPoseStack, pCamX, pCamY, pCamZ, pBuffer, 0.02 + $$11, pRed, pGreen, pBlue);
         BlockPos $$13 = pEntity.getOnPosLegacy();
         if (!$$13.equals($$12)) {
            this.highlightPosition($$13, pPoseStack, pCamX, pCamY, pCamZ, pBuffer, 0.04 + $$11, 0.0F, 1.0F, 1.0F);
         }
      });
   }

   private double getBias(Entity pEntity) {
      return 0.02 * (double)(String.valueOf((double)pEntity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
   }

   private void highlightPosition(
      BlockPos pPos,
      PoseStack pPoseStack,
      double pCamX,
      double pCamY,
      double pCamZ,
      MultiBufferSource pBuffer,
      double pBias,
      float pRed,
      float pGreen,
      float pBlue
   ) {
      double $$10 = (double)pPos.getX() - pCamX - 2.0 * pBias;
      double $$11 = (double)pPos.getY() - pCamY - 2.0 * pBias;
      double $$12 = (double)pPos.getZ() - pCamZ - 2.0 * pBias;
      double $$13 = $$10 + 1.0 + 4.0 * pBias;
      double $$14 = $$11 + 1.0 + 4.0 * pBias;
      double $$15 = $$12 + 1.0 + 4.0 * pBias;
      LevelRenderer.renderLineBox(pPoseStack, pBuffer.getBuffer(RenderType.lines()), $$10, $$11, $$12, $$13, $$14, $$15, pRed, pGreen, pBlue, 0.4F);
      LevelRenderer.renderVoxelShape(
         pPoseStack,
         pBuffer.getBuffer(RenderType.lines()),
         this.minecraft
            .level
            .getBlockState(pPos)
            .getCollisionShape(this.minecraft.level, pPos, CollisionContext.empty())
            .move((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ()),
         -pCamX,
         -pCamY,
         -pCamZ,
         pRed,
         pGreen,
         pBlue,
         1.0F,
         false
      );
   }
}
