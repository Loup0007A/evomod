package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer {
   private static final int JUMP_TARGET_LINE_COLOR = FastColor.ARGB32.color(255, 255, 100, 255);
   private static final int TARGET_LINE_COLOR = FastColor.ARGB32.color(255, 100, 255, 255);
   private static final int INNER_CIRCLE_COLOR = FastColor.ARGB32.color(255, 0, 255, 0);
   private static final int MIDDLE_CIRCLE_COLOR = FastColor.ARGB32.color(255, 255, 165, 0);
   private static final int OUTER_CIRCLE_COLOR = FastColor.ARGB32.color(255, 255, 0, 0);
   private static final int CIRCLE_VERTICES = 20;
   private static final float SEGMENT_SIZE_RADIANS = (float) (Math.PI / 10);
   private final Minecraft minecraft;
   private final Map<Integer, BreezeDebugPayload.BreezeInfo> perEntity = new HashMap<>();

   public BreezeDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBuffer, double pXOffset, double pYOffset, double pZOffset) {
      LocalPlayer $$5 = this.minecraft.player;
      $$5.level()
         .getEntities(EntityType.BREEZE, $$5.getBoundingBox().inflate(100.0), p_312249_ -> true)
         .forEach(
            p_340927_ -> {
               Optional<BreezeDebugPayload.BreezeInfo> $$7 = Optional.ofNullable(this.perEntity.get(p_340927_.getId()));
               $$7.map(BreezeDebugPayload.BreezeInfo::attackTarget)
                  .map(p_340919_ -> $$5.level().getEntity(p_340919_))
                  .map(p_340920_ -> p_340920_.getPosition(this.minecraft.getTimer().getGameTimeDeltaPartialTick(true)))
                  .ifPresent(
                     p_310972_ -> {
                        drawLine(pPoseStack, pBuffer, pXOffset, pYOffset, pZOffset, p_340927_.position(), p_310972_, TARGET_LINE_COLOR);
                        Vec3 $$7x = p_310972_.add(0.0, 0.01F, 0.0);
                        drawCircle(
                           pPoseStack.last().pose(),
                           pXOffset,
                           pYOffset,
                           pZOffset,
                           pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                           $$7x,
                           4.0F,
                           INNER_CIRCLE_COLOR
                        );
                        drawCircle(
                           pPoseStack.last().pose(),
                           pXOffset,
                           pYOffset,
                           pZOffset,
                           pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                           $$7x,
                           8.0F,
                           MIDDLE_CIRCLE_COLOR
                        );
                        drawCircle(
                           pPoseStack.last().pose(),
                           pXOffset,
                           pYOffset,
                           pZOffset,
                           pBuffer.getBuffer(RenderType.debugLineStrip(2.0)),
                           $$7x,
                           20.0F,
                           OUTER_CIRCLE_COLOR
                        );
                     }
                  );
               $$7.map(BreezeDebugPayload.BreezeInfo::jumpTarget)
                  .ifPresent(
                     p_340934_ -> {
                        drawLine(pPoseStack, pBuffer, pXOffset, pYOffset, pZOffset, p_340927_.position(), p_340934_.getCenter(), JUMP_TARGET_LINE_COLOR);
                        DebugRenderer.renderFilledBox(
                           pPoseStack,
                           pBuffer,
                           AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_340934_)).move(-pXOffset, -pYOffset, -pZOffset),
                           1.0F,
                           0.0F,
                           0.0F,
                           1.0F
                        );
                     }
                  );
            }
         );
   }

   private static void drawLine(
      PoseStack pPoseStack, MultiBufferSource pBuffer, double pXOffset, double pYOffset, double pZOffset, Vec3 pFromPos, Vec3 pToPos, int pColor
   ) {
      VertexConsumer $$8 = pBuffer.getBuffer(RenderType.debugLineStrip(2.0));
      $$8.addVertex(pPoseStack.last(), (float)(pFromPos.x - pXOffset), (float)(pFromPos.y - pYOffset), (float)(pFromPos.z - pZOffset)).setColor(pColor);
      $$8.addVertex(pPoseStack.last(), (float)(pToPos.x - pXOffset), (float)(pToPos.y - pYOffset), (float)(pToPos.z - pZOffset)).setColor(pColor);
   }

   private static void drawCircle(
      Matrix4f pPose, double pXOffset, double pYOffset, double pZOffset, VertexConsumer pConsumer, Vec3 pPos, float pRadius, int pColor
   ) {
      for (int $$8 = 0; $$8 < 20; $$8++) {
         drawCircleVertex($$8, pPose, pXOffset, pYOffset, pZOffset, pConsumer, pPos, pRadius, pColor);
      }

      drawCircleVertex(0, pPose, pXOffset, pYOffset, pZOffset, pConsumer, pPos, pRadius, pColor);
   }

   private static void drawCircleVertex(
      int pIndex, Matrix4f pPose, double pXOffset, double pYOffset, double pZOffset, VertexConsumer pConsumer, Vec3 pCircleCenter, float pRadius, int pColor
   ) {
      float $$9 = (float)pIndex * (float) (Math.PI / 10);
      Vec3 $$10 = pCircleCenter.add((double)pRadius * Math.cos((double)$$9), 0.0, (double)pRadius * Math.sin((double)$$9));
      pConsumer.addVertex(pPose, (float)($$10.x - pXOffset), (float)($$10.y - pYOffset), (float)($$10.z - pZOffset)).setColor(pColor);
   }

   public void clear() {
      this.perEntity.clear();
   }

   public void add(BreezeDebugPayload.BreezeInfo pBreeze) {
      this.perEntity.put(pBreeze.id(), pBreeze);
   }
}
