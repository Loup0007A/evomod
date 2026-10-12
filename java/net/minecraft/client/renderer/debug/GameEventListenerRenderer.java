package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int LISTENER_RENDER_DIST = 32;
   private static final float BOX_HEIGHT = 1.0F;
   private final List<GameEventListenerRenderer.TrackedGameEvent> trackedGameEvents = Lists.newArrayList();
   private final List<GameEventListenerRenderer.TrackedListener> trackedListeners = Lists.newArrayList();

   public GameEventListenerRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      Level $$5 = this.minecraft.level;
      if ($$5 == null) {
         this.trackedGameEvents.clear();
         this.trackedListeners.clear();
      } else {
         Vec3 $$6 = new Vec3(pCamX, 0.0, pCamZ);
         this.trackedGameEvents.removeIf(GameEventListenerRenderer.TrackedGameEvent::isExpired);
         this.trackedListeners.removeIf(p_234512_ -> p_234512_.isExpired($$5, $$6));
         VertexConsumer $$7 = pBufferSource.getBuffer(RenderType.lines());

         for (GameEventListenerRenderer.TrackedListener $$8 : this.trackedListeners) {
            $$8.getPosition($$5)
               .ifPresent(
                  p_269731_ -> {
                     double $$7x = p_269731_.x() - (double)$$8.getListenerRadius();
                     double $$8x = p_269731_.y() - (double)$$8.getListenerRadius();
                     double $$9 = p_269731_.z() - (double)$$8.getListenerRadius();
                     double $$10 = p_269731_.x() + (double)$$8.getListenerRadius();
                     double $$11 = p_269731_.y() + (double)$$8.getListenerRadius();
                     double $$12x = p_269731_.z() + (double)$$8.getListenerRadius();
                     LevelRenderer.renderVoxelShape(
                        pPoseStack, $$7, Shapes.create(new AABB($$7x, $$8x, $$9, $$10, $$11, $$12x)), -pCamX, -pCamY, -pCamZ, 1.0F, 1.0F, 0.0F, 0.35F, true
                     );
                  }
               );
         }

         VertexConsumer $$9 = pBufferSource.getBuffer(RenderType.debugFilledBox());

         for (GameEventListenerRenderer.TrackedListener $$10 : this.trackedListeners) {
            $$10.getPosition($$5)
               .ifPresent(
                  p_269724_ -> LevelRenderer.addChainedFilledBoxVertices(
                        pPoseStack,
                        $$9,
                        p_269724_.x() - 0.25 - pCamX,
                        p_269724_.y() - pCamY,
                        p_269724_.z() - 0.25 - pCamZ,
                        p_269724_.x() + 0.25 - pCamX,
                        p_269724_.y() - pCamY + 1.0,
                        p_269724_.z() + 0.25 - pCamZ,
                        1.0F,
                        1.0F,
                        0.0F,
                        0.35F
                     )
               );
         }

         for (GameEventListenerRenderer.TrackedListener $$11 : this.trackedListeners) {
            $$11.getPosition($$5)
               .ifPresent(
                  p_274713_ -> {
                     DebugRenderer.renderFloatingText(
                        pPoseStack, pBufferSource, "Listener Origin", p_274713_.x(), p_274713_.y() + 1.8F, p_274713_.z(), -1, 0.025F
                     );
                     DebugRenderer.renderFloatingText(
                        pPoseStack,
                        pBufferSource,
                        BlockPos.containing(p_274713_).toString(),
                        p_274713_.x(),
                        p_274713_.y() + 1.5,
                        p_274713_.z(),
                        -6959665,
                        0.025F
                     );
                  }
               );
         }

         for (GameEventListenerRenderer.TrackedGameEvent $$12 : this.trackedGameEvents) {
            Vec3 $$13 = $$12.position;
            double $$14 = 0.2F;
            double $$15 = $$13.x - 0.2F;
            double $$16 = $$13.y - 0.2F;
            double $$17 = $$13.z - 0.2F;
            double $$18 = $$13.x + 0.2F;
            double $$19 = $$13.y + 0.2F + 0.5;
            double $$20 = $$13.z + 0.2F;
            renderFilledBox(pPoseStack, pBufferSource, new AABB($$15, $$16, $$17, $$18, $$19, $$20), 1.0F, 1.0F, 1.0F, 0.2F);
            DebugRenderer.renderFloatingText(pPoseStack, pBufferSource, $$12.gameEvent.location().toString(), $$13.x, $$13.y + 0.85F, $$13.z, -7564911, 0.0075F);
         }
      }
   }

   private static void renderFilledBox(PoseStack pPoseStack, MultiBufferSource pBuffer, AABB pBoundingBox, float pRed, float pGreen, float pBlue, float pAlpha) {
      Camera $$7 = Minecraft.getInstance().gameRenderer.getMainCamera();
      if ($$7.isInitialized()) {
         Vec3 $$8 = $$7.getPosition().reverse();
         DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pBoundingBox.move($$8), pRed, pGreen, pBlue, pAlpha);
      }
   }

   public void trackGameEvent(ResourceKey<GameEvent> pGameEvent, Vec3 pPosition) {
      this.trackedGameEvents.add(new GameEventListenerRenderer.TrackedGameEvent(Util.getMillis(), pGameEvent, pPosition));
   }

   public void trackListener(PositionSource pListenerSource, int pListenerRange) {
      this.trackedListeners.add(new GameEventListenerRenderer.TrackedListener(pListenerSource, pListenerRange));
   }

   @OnlyIn(Dist.CLIENT)
   static record TrackedGameEvent(long timeStamp, ResourceKey<GameEvent> gameEvent, Vec3 position) {
      public boolean isExpired() {
         return Util.getMillis() - this.timeStamp > 3000L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TrackedListener implements GameEventListener {
      public final PositionSource listenerSource;
      public final int listenerRange;

      public TrackedListener(PositionSource pListenerSource, int pListenerRange) {
         this.listenerSource = pListenerSource;
         this.listenerRange = pListenerRange;
      }

      public boolean isExpired(Level pLevel, Vec3 pPos) {
         return this.listenerSource.getPosition(pLevel).filter(p_234547_ -> p_234547_.distanceToSqr(pPos) <= 1024.0).isPresent();
      }

      public Optional<Vec3> getPosition(Level pLevel) {
         return this.listenerSource.getPosition(pLevel);
      }

      @Override
      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      @Override
      public int getListenerRadius() {
         return this.listenerRange;
      }

      @Override
      public boolean handleGameEvent(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos) {
         return false;
      }
   }
}
