package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
   private static final int RADIUS = 10;
   private static final Vector4f LIGHT_AND_BLOCKS_COLOR = new Vector4f(1.0F, 1.0F, 0.0F, 0.25F);
   private static final Vector4f LIGHT_ONLY_COLOR = new Vector4f(0.25F, 0.125F, 0.0F, 0.125F);
   private final Minecraft minecraft;
   private final LightLayer lightLayer;
   private Instant lastUpdateTime = Instant.now();
   @Nullable
   private LightSectionDebugRenderer.SectionData data;

   public LightSectionDebugRenderer(Minecraft pMinecraft, LightLayer pLightLayer) {
      this.minecraft = pMinecraft;
      this.lightLayer = pLightLayer;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      Instant $$5 = Instant.now();
      if (this.data == null || Duration.between(this.lastUpdateTime, $$5).compareTo(REFRESH_INTERVAL) > 0) {
         this.lastUpdateTime = $$5;
         this.data = new LightSectionDebugRenderer.SectionData(
            this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
         );
      }

      renderEdges(pPoseStack, this.data.lightAndBlocksShape, this.data.minPos, pBufferSource, pCamX, pCamY, pCamZ, LIGHT_AND_BLOCKS_COLOR);
      renderEdges(pPoseStack, this.data.lightShape, this.data.minPos, pBufferSource, pCamX, pCamY, pCamZ, LIGHT_ONLY_COLOR);
      VertexConsumer $$6 = pBufferSource.getBuffer(RenderType.debugSectionQuads());
      renderFaces(pPoseStack, this.data.lightAndBlocksShape, this.data.minPos, $$6, pCamX, pCamY, pCamZ, LIGHT_AND_BLOCKS_COLOR);
      renderFaces(pPoseStack, this.data.lightShape, this.data.minPos, $$6, pCamX, pCamY, pCamZ, LIGHT_ONLY_COLOR);
   }

   private static void renderFaces(
      PoseStack pPoseStack, DiscreteVoxelShape pShape, SectionPos pPos, VertexConsumer pBuffer, double pCamX, double pCamY, double pCamZ, Vector4f pColor
   ) {
      pShape.forAllFaces((p_282087_, p_283360_, p_282854_, p_282233_) -> {
         int $$11 = p_283360_ + pPos.getX();
         int $$12 = p_282854_ + pPos.getY();
         int $$13 = p_282233_ + pPos.getZ();
         renderFace(pPoseStack, pBuffer, p_282087_, pCamX, pCamY, pCamZ, $$11, $$12, $$13, pColor);
      });
   }

   private static void renderEdges(
      PoseStack pPoseStack,
      DiscreteVoxelShape pShape,
      SectionPos pPos,
      MultiBufferSource pBufferSource,
      double pCamX,
      double pCamY,
      double pCamZ,
      Vector4f pColor
   ) {
      pShape.forAllEdges((p_283441_, p_283631_, p_282083_, p_281900_, p_281481_, p_283547_) -> {
         int $$13 = p_283441_ + pPos.getX();
         int $$14 = p_283631_ + pPos.getY();
         int $$15 = p_282083_ + pPos.getZ();
         int $$16 = p_281900_ + pPos.getX();
         int $$17 = p_281481_ + pPos.getY();
         int $$18 = p_283547_ + pPos.getZ();
         VertexConsumer $$19 = pBufferSource.getBuffer(RenderType.debugLineStrip(1.0));
         renderEdge(pPoseStack, $$19, pCamX, pCamY, pCamZ, $$13, $$14, $$15, $$16, $$17, $$18, pColor);
      }, true);
   }

   private static void renderFace(
      PoseStack pPoseStack,
      VertexConsumer pBuffer,
      Direction pFace,
      double pCamX,
      double pCamY,
      double pCamZ,
      int pBlockX,
      int pBlockY,
      int pBlockZ,
      Vector4f pColor
   ) {
      float $$10 = (float)((double)SectionPos.sectionToBlockCoord(pBlockX) - pCamX);
      float $$11 = (float)((double)SectionPos.sectionToBlockCoord(pBlockY) - pCamY);
      float $$12 = (float)((double)SectionPos.sectionToBlockCoord(pBlockZ) - pCamZ);
      LevelRenderer.renderFace(
         pPoseStack, pBuffer, pFace, $$10, $$11, $$12, $$10 + 16.0F, $$11 + 16.0F, $$12 + 16.0F, pColor.x(), pColor.y(), pColor.z(), pColor.w()
      );
   }

   private static void renderEdge(
      PoseStack pPoseStack,
      VertexConsumer pBuffer,
      double pCamX,
      double pCamY,
      double pCamZ,
      int pX1,
      int pY1,
      int pZ1,
      int pX2,
      int pY2,
      int pZ2,
      Vector4f pColor
   ) {
      float $$12 = (float)((double)SectionPos.sectionToBlockCoord(pX1) - pCamX);
      float $$13 = (float)((double)SectionPos.sectionToBlockCoord(pY1) - pCamY);
      float $$14 = (float)((double)SectionPos.sectionToBlockCoord(pZ1) - pCamZ);
      float $$15 = (float)((double)SectionPos.sectionToBlockCoord(pX2) - pCamX);
      float $$16 = (float)((double)SectionPos.sectionToBlockCoord(pY2) - pCamY);
      float $$17 = (float)((double)SectionPos.sectionToBlockCoord(pZ2) - pCamZ);
      Matrix4f $$18 = pPoseStack.last().pose();
      pBuffer.addVertex($$18, $$12, $$13, $$14).setColor(pColor.x(), pColor.y(), pColor.z(), 1.0F);
      pBuffer.addVertex($$18, $$15, $$16, $$17).setColor(pColor.x(), pColor.y(), pColor.z(), 1.0F);
   }

   @OnlyIn(Dist.CLIENT)
   static final class SectionData {
      final DiscreteVoxelShape lightAndBlocksShape;
      final DiscreteVoxelShape lightShape;
      final SectionPos minPos;

      SectionData(LevelLightEngine pLevelLightEngine, SectionPos pPos, int pRadius, LightLayer pLightLayer) {
         int $$4 = pRadius * 2 + 1;
         this.lightAndBlocksShape = new BitSetDiscreteVoxelShape($$4, $$4, $$4);
         this.lightShape = new BitSetDiscreteVoxelShape($$4, $$4, $$4);

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            for (int $$6 = 0; $$6 < $$4; $$6++) {
               for (int $$7 = 0; $$7 < $$4; $$7++) {
                  SectionPos $$8 = SectionPos.of(pPos.x() + $$7 - pRadius, pPos.y() + $$6 - pRadius, pPos.z() + $$5 - pRadius);
                  LayerLightSectionStorage.SectionType $$9 = pLevelLightEngine.getDebugSectionType(pLightLayer, $$8);
                  if ($$9 == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                     this.lightAndBlocksShape.fill($$7, $$6, $$5);
                     this.lightShape.fill($$7, $$6, $$5);
                  } else if ($$9 == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                     this.lightShape.fill($$7, $$6, $$5);
                  }
               }
            }
         }

         this.minPos = SectionPos.of(pPos.x() - pRadius, pPos.y() - pRadius, pPos.z() - pRadius);
      }
   }
}
