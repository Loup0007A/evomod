package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Map<Integer, Path> pathMap = Maps.newHashMap();
   private final Map<Integer, Float> pathMaxDist = Maps.newHashMap();
   private final Map<Integer, Long> creationMap = Maps.newHashMap();
   private static final long TIMEOUT = 5000L;
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.02F;

   public void addPath(int pEntityId, Path pPath, float pMaxDistanceToWaypoint) {
      this.pathMap.put(pEntityId, pPath);
      this.creationMap.put(pEntityId, Util.getMillis());
      this.pathMaxDist.put(pEntityId, pMaxDistanceToWaypoint);
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      if (!this.pathMap.isEmpty()) {
         long $$5 = Util.getMillis();

         for (Integer $$6 : this.pathMap.keySet()) {
            Path $$7 = this.pathMap.get($$6);
            float $$8 = this.pathMaxDist.get($$6);
            renderPath(pPoseStack, pBufferSource, $$7, $$8, true, true, pCamX, pCamY, pCamZ);
         }

         for (Integer $$9 : this.creationMap.keySet().toArray(new Integer[0])) {
            if ($$5 - this.creationMap.get($$9) > 5000L) {
               this.pathMap.remove($$9);
               this.creationMap.remove($$9);
            }
         }
      }
   }

   public static void renderPath(
      PoseStack pPoseStack, MultiBufferSource pBuffer, Path pPath, float p_270841_, boolean p_270481_, boolean p_270748_, double pX, double pY, double pZ
   ) {
      renderPathLine(pPoseStack, pBuffer.getBuffer(RenderType.debugLineStrip(6.0)), pPath, pX, pY, pZ);
      BlockPos $$9 = pPath.getTarget();
      if (distanceToCamera($$9, pX, pY, pZ) <= 80.0F) {
         DebugRenderer.renderFilledBox(
            pPoseStack,
            pBuffer,
            new AABB(
                  (double)((float)$$9.getX() + 0.25F),
                  (double)((float)$$9.getY() + 0.25F),
                  (double)$$9.getZ() + 0.25,
                  (double)((float)$$9.getX() + 0.75F),
                  (double)((float)$$9.getY() + 0.75F),
                  (double)((float)$$9.getZ() + 0.75F)
               )
               .move(-pX, -pY, -pZ),
            0.0F,
            1.0F,
            0.0F,
            0.5F
         );

         for (int $$10 = 0; $$10 < pPath.getNodeCount(); $$10++) {
            Node $$11 = pPath.getNode($$10);
            if (distanceToCamera($$11.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               float $$12 = $$10 == pPath.getNextNodeIndex() ? 1.0F : 0.0F;
               float $$13 = $$10 == pPath.getNextNodeIndex() ? 0.0F : 1.0F;
               DebugRenderer.renderFilledBox(
                  pPoseStack,
                  pBuffer,
                  new AABB(
                        (double)((float)$$11.x + 0.5F - p_270841_),
                        (double)((float)$$11.y + 0.01F * (float)$$10),
                        (double)((float)$$11.z + 0.5F - p_270841_),
                        (double)((float)$$11.x + 0.5F + p_270841_),
                        (double)((float)$$11.y + 0.25F + 0.01F * (float)$$10),
                        (double)((float)$$11.z + 0.5F + p_270841_)
                     )
                     .move(-pX, -pY, -pZ),
                  $$12,
                  0.0F,
                  $$13,
                  0.5F
               );
            }
         }
      }

      Path.DebugData $$14 = pPath.debugData();
      if (p_270481_ && $$14 != null) {
         for (Node $$15 : $$14.closedSet()) {
            if (distanceToCamera($$15.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  pPoseStack,
                  pBuffer,
                  new AABB(
                        (double)((float)$$15.x + 0.5F - p_270841_ / 2.0F),
                        (double)((float)$$15.y + 0.01F),
                        (double)((float)$$15.z + 0.5F - p_270841_ / 2.0F),
                        (double)((float)$$15.x + 0.5F + p_270841_ / 2.0F),
                        (double)$$15.y + 0.1,
                        (double)((float)$$15.z + 0.5F + p_270841_ / 2.0F)
                     )
                     .move(-pX, -pY, -pZ),
                  1.0F,
                  0.8F,
                  0.8F,
                  0.5F
               );
            }
         }

         for (Node $$16 : $$14.openSet()) {
            if (distanceToCamera($$16.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFilledBox(
                  pPoseStack,
                  pBuffer,
                  new AABB(
                        (double)((float)$$16.x + 0.5F - p_270841_ / 2.0F),
                        (double)((float)$$16.y + 0.01F),
                        (double)((float)$$16.z + 0.5F - p_270841_ / 2.0F),
                        (double)((float)$$16.x + 0.5F + p_270841_ / 2.0F),
                        (double)$$16.y + 0.1,
                        (double)((float)$$16.z + 0.5F + p_270841_ / 2.0F)
                     )
                     .move(-pX, -pY, -pZ),
                  0.8F,
                  1.0F,
                  1.0F,
                  0.5F
               );
            }
         }
      }

      if (p_270748_) {
         for (int $$17 = 0; $$17 < pPath.getNodeCount(); $$17++) {
            Node $$18 = pPath.getNode($$17);
            if (distanceToCamera($$18.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFloatingText(
                  pPoseStack,
                  pBuffer,
                  String.valueOf($$18.type),
                  (double)$$18.x + 0.5,
                  (double)$$18.y + 0.75,
                  (double)$$18.z + 0.5,
                  -1,
                  0.02F,
                  true,
                  0.0F,
                  true
               );
               DebugRenderer.renderFloatingText(
                  pPoseStack,
                  pBuffer,
                  String.format(Locale.ROOT, "%.2f", $$18.costMalus),
                  (double)$$18.x + 0.5,
                  (double)$$18.y + 0.25,
                  (double)$$18.z + 0.5,
                  -1,
                  0.02F,
                  true,
                  0.0F,
                  true
               );
            }
         }
      }
   }

   public static void renderPathLine(PoseStack pPoseStack, VertexConsumer pConsumer, Path pPath, double pX, double pY, double pZ) {
      for (int $$6 = 0; $$6 < pPath.getNodeCount(); $$6++) {
         Node $$7 = pPath.getNode($$6);
         if (!(distanceToCamera($$7.asBlockPos(), pX, pY, pZ) > 80.0F)) {
            float $$8 = (float)$$6 / (float)pPath.getNodeCount() * 0.33F;
            int $$9 = $$6 == 0 ? 0 : Mth.hsvToRgb($$8, 0.9F, 0.9F);
            int $$10 = $$9 >> 16 & 0xFF;
            int $$11 = $$9 >> 8 & 0xFF;
            int $$12 = $$9 & 0xFF;
            pConsumer.addVertex(pPoseStack.last(), (float)((double)$$7.x - pX + 0.5), (float)((double)$$7.y - pY + 0.5), (float)((double)$$7.z - pZ + 0.5))
               .setColor($$10, $$11, $$12, 255);
         }
      }
   }

   private static float distanceToCamera(BlockPos pPos, double pX, double pY, double pZ) {
      return (float)(Math.abs((double)pPos.getX() - pX) + Math.abs((double)pPos.getY() - pY) + Math.abs((double)pPos.getZ() - pZ));
   }
}
