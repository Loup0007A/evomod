package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;
   private final Int2ObjectMap<GoalSelectorDebugRenderer.EntityGoalInfo> goalSelectors = new Int2ObjectOpenHashMap();

   @Override
   public void clear() {
      this.goalSelectors.clear();
   }

   public void addGoalSelector(int pMobId, BlockPos pEntityPos, List<GoalDebugPayload.DebugGoal> pGoals) {
      this.goalSelectors.put(pMobId, new GoalSelectorDebugRenderer.EntityGoalInfo(pEntityPos, pGoals));
   }

   public void removeGoalSelector(int pMobId) {
      this.goalSelectors.remove(pMobId);
   }

   public GoalSelectorDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      Camera $$5 = this.minecraft.gameRenderer.getMainCamera();
      BlockPos $$6 = BlockPos.containing($$5.getPosition().x, 0.0, $$5.getPosition().z);
      ObjectIterator var11 = this.goalSelectors.values().iterator();

      while (var11.hasNext()) {
         GoalSelectorDebugRenderer.EntityGoalInfo $$7 = (GoalSelectorDebugRenderer.EntityGoalInfo)var11.next();
         BlockPos $$8 = $$7.entityPos;
         if ($$6.closerThan($$8, 160.0)) {
            for (int $$9 = 0; $$9 < $$7.goals.size(); $$9++) {
               GoalDebugPayload.DebugGoal $$10 = $$7.goals.get($$9);
               double $$11 = (double)$$8.getX() + 0.5;
               double $$12 = (double)$$8.getY() + 2.0 + (double)$$9 * 0.25;
               double $$13 = (double)$$8.getZ() + 0.5;
               int $$14 = $$10.isRunning() ? -16711936 : -3355444;
               DebugRenderer.renderFloatingText(pPoseStack, pBufferSource, $$10.name(), $$11, $$12, $$13, $$14);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record EntityGoalInfo(BlockPos entityPos, List<GoalDebugPayload.DebugGoal> goals) {
   }
}
