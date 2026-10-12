package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CELL_BORDER = FastColor.ARGB32.color(255, 0, 155, 155);
   private static final int YELLOW = FastColor.ARGB32.color(255, 255, 255, 0);

   public ChunkBorderRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      Entity $$5 = this.minecraft.gameRenderer.getMainCamera().getEntity();
      float $$6 = (float)((double)this.minecraft.level.getMinBuildHeight() - pCamY);
      float $$7 = (float)((double)this.minecraft.level.getMaxBuildHeight() - pCamY);
      ChunkPos $$8 = $$5.chunkPosition();
      float $$9 = (float)((double)$$8.getMinBlockX() - pCamX);
      float $$10 = (float)((double)$$8.getMinBlockZ() - pCamZ);
      VertexConsumer $$11 = pBufferSource.getBuffer(RenderType.debugLineStrip(1.0));
      Matrix4f $$12 = pPoseStack.last().pose();

      for (int $$13 = -16; $$13 <= 32; $$13 += 16) {
         for (int $$14 = -16; $$14 <= 32; $$14 += 16) {
            $$11.addVertex($$12, $$9 + (float)$$13, $$6, $$10 + (float)$$14).setColor(1.0F, 0.0F, 0.0F, 0.0F);
            $$11.addVertex($$12, $$9 + (float)$$13, $$6, $$10 + (float)$$14).setColor(1.0F, 0.0F, 0.0F, 0.5F);
            $$11.addVertex($$12, $$9 + (float)$$13, $$7, $$10 + (float)$$14).setColor(1.0F, 0.0F, 0.0F, 0.5F);
            $$11.addVertex($$12, $$9 + (float)$$13, $$7, $$10 + (float)$$14).setColor(1.0F, 0.0F, 0.0F, 0.0F);
         }
      }

      for (int $$15 = 2; $$15 < 16; $$15 += 2) {
         int $$16 = $$15 % 4 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9 + (float)$$15, $$6, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + (float)$$15, $$6, $$10).setColor($$16);
         $$11.addVertex($$12, $$9 + (float)$$15, $$7, $$10).setColor($$16);
         $$11.addVertex($$12, $$9 + (float)$$15, $$7, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + (float)$$15, $$6, $$10 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + (float)$$15, $$6, $$10 + 16.0F).setColor($$16);
         $$11.addVertex($$12, $$9 + (float)$$15, $$7, $$10 + 16.0F).setColor($$16);
         $$11.addVertex($$12, $$9 + (float)$$15, $$7, $$10 + 16.0F).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      for (int $$17 = 2; $$17 < 16; $$17 += 2) {
         int $$18 = $$17 % 4 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9, $$6, $$10 + (float)$$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$6, $$10 + (float)$$17).setColor($$18);
         $$11.addVertex($$12, $$9, $$7, $$10 + (float)$$17).setColor($$18);
         $$11.addVertex($$12, $$9, $$7, $$10 + (float)$$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$6, $$10 + (float)$$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$6, $$10 + (float)$$17).setColor($$18);
         $$11.addVertex($$12, $$9 + 16.0F, $$7, $$10 + (float)$$17).setColor($$18);
         $$11.addVertex($$12, $$9 + 16.0F, $$7, $$10 + (float)$$17).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      for (int $$19 = this.minecraft.level.getMinBuildHeight(); $$19 <= this.minecraft.level.getMaxBuildHeight(); $$19 += 2) {
         float $$20 = (float)((double)$$19 - pCamY);
         int $$21 = $$19 % 8 == 0 ? CELL_BORDER : YELLOW;
         $$11.addVertex($$12, $$9, $$20, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10 + 16.0F).setColor($$21);
         $$11.addVertex($$12, $$9 + 16.0F, $$20, $$10 + 16.0F).setColor($$21);
         $$11.addVertex($$12, $$9 + 16.0F, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor($$21);
         $$11.addVertex($$12, $$9, $$20, $$10).setColor(1.0F, 1.0F, 0.0F, 0.0F);
      }

      $$11 = pBufferSource.getBuffer(RenderType.debugLineStrip(2.0));

      for (int $$22 = 0; $$22 <= 16; $$22 += 16) {
         for (int $$23 = 0; $$23 <= 16; $$23 += 16) {
            $$11.addVertex($$12, $$9 + (float)$$22, $$6, $$10 + (float)$$23).setColor(0.25F, 0.25F, 1.0F, 0.0F);
            $$11.addVertex($$12, $$9 + (float)$$22, $$6, $$10 + (float)$$23).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            $$11.addVertex($$12, $$9 + (float)$$22, $$7, $$10 + (float)$$23).setColor(0.25F, 0.25F, 1.0F, 1.0F);
            $$11.addVertex($$12, $$9 + (float)$$22, $$7, $$10 + (float)$$23).setColor(0.25F, 0.25F, 1.0F, 0.0F);
         }
      }

      for (int $$24 = this.minecraft.level.getMinBuildHeight(); $$24 <= this.minecraft.level.getMaxBuildHeight(); $$24 += 16) {
         float $$25 = (float)((double)$$24 - pCamY);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 0.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$25, $$10 + 16.0F).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9 + 16.0F, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 1.0F);
         $$11.addVertex($$12, $$9, $$25, $$10).setColor(0.25F, 0.25F, 1.0F, 0.0F);
      }
   }
}
