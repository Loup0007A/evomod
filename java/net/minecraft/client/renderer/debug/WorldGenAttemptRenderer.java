package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldGenAttemptRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final List<BlockPos> toRender = Lists.newArrayList();
   private final List<Float> scales = Lists.newArrayList();
   private final List<Float> alphas = Lists.newArrayList();
   private final List<Float> reds = Lists.newArrayList();
   private final List<Float> greens = Lists.newArrayList();
   private final List<Float> blues = Lists.newArrayList();

   public void addPos(BlockPos pPos, float pScale, float pRed, float pGreen, float pBlue, float pAlpha) {
      this.toRender.add(pPos);
      this.scales.add(pScale);
      this.alphas.add(pAlpha);
      this.reds.add(pRed);
      this.greens.add(pGreen);
      this.blues.add(pBlue);
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      VertexConsumer $$5 = pBufferSource.getBuffer(RenderType.debugFilledBox());

      for (int $$6 = 0; $$6 < this.toRender.size(); $$6++) {
         BlockPos $$7 = this.toRender.get($$6);
         Float $$8 = this.scales.get($$6);
         float $$9 = $$8 / 2.0F;
         LevelRenderer.addChainedFilledBoxVertices(
            pPoseStack,
            $$5,
            (double)((float)$$7.getX() + 0.5F - $$9) - pCamX,
            (double)((float)$$7.getY() + 0.5F - $$9) - pCamY,
            (double)((float)$$7.getZ() + 0.5F - $$9) - pCamZ,
            (double)((float)$$7.getX() + 0.5F + $$9) - pCamX,
            (double)((float)$$7.getY() + 0.5F + $$9) - pCamY,
            (double)((float)$$7.getZ() + 0.5F + $$9) - pCamZ,
            this.reds.get($$6),
            this.greens.get($$6),
            this.blues.get($$6),
            this.alphas.get($$6)
         );
      }
   }
}
