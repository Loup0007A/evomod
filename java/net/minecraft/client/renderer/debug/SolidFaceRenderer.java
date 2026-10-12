package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      Matrix4f $$5 = pPoseStack.last().pose();
      BlockGetter $$6 = this.minecraft.player.level();
      BlockPos $$7 = BlockPos.containing(pCamX, pCamY, pCamZ);

      for (BlockPos $$8 : BlockPos.betweenClosed($$7.offset(-6, -6, -6), $$7.offset(6, 6, 6))) {
         BlockState $$9 = $$6.getBlockState($$8);
         if (!$$9.is(Blocks.AIR)) {
            VoxelShape $$10 = $$9.getShape($$6, $$8);

            for (AABB $$11 : $$10.toAabbs()) {
               AABB $$12 = $$11.move($$8).inflate(0.002);
               float $$13 = (float)($$12.minX - pCamX);
               float $$14 = (float)($$12.minY - pCamY);
               float $$15 = (float)($$12.minZ - pCamZ);
               float $$16 = (float)($$12.maxX - pCamX);
               float $$17 = (float)($$12.maxY - pCamY);
               float $$18 = (float)($$12.maxZ - pCamZ);
               int $$19 = -2130771968;
               if ($$9.isFaceSturdy($$6, $$8, Direction.WEST)) {
                  VertexConsumer $$20 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$20.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$20.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.SOUTH)) {
                  VertexConsumer $$21 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$21.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
                  $$21.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.EAST)) {
                  VertexConsumer $$22 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$22.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
                  $$22.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.NORTH)) {
                  VertexConsumer $$23 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$23.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$23.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.DOWN)) {
                  VertexConsumer $$24 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$24.addVertex($$5, $$13, $$14, $$15).setColor(-2130771968);
                  $$24.addVertex($$5, $$16, $$14, $$15).setColor(-2130771968);
                  $$24.addVertex($$5, $$13, $$14, $$18).setColor(-2130771968);
                  $$24.addVertex($$5, $$16, $$14, $$18).setColor(-2130771968);
               }

               if ($$9.isFaceSturdy($$6, $$8, Direction.UP)) {
                  VertexConsumer $$25 = pBufferSource.getBuffer(RenderType.debugFilledBox());
                  $$25.addVertex($$5, $$13, $$17, $$15).setColor(-2130771968);
                  $$25.addVertex($$5, $$13, $$17, $$18).setColor(-2130771968);
                  $$25.addVertex($$5, $$16, $$17, $$15).setColor(-2130771968);
                  $$25.addVertex($$5, $$16, $$17, $$18).setColor(-2130771968);
               }
            }
         }
      }
   }
}
