package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class EuclideanGameEventListenerRegistry implements GameEventListenerRegistry {
   private final List<GameEventListener> listeners = Lists.newArrayList();
   private final Set<GameEventListener> listenersToRemove = Sets.newHashSet();
   private final List<GameEventListener> listenersToAdd = Lists.newArrayList();
   private boolean processing;
   private final ServerLevel level;
   private final int sectionY;
   private final EuclideanGameEventListenerRegistry.OnEmptyAction onEmptyAction;

   public EuclideanGameEventListenerRegistry(ServerLevel pLevel, int pSectionY, EuclideanGameEventListenerRegistry.OnEmptyAction pOnEmptyAction) {
      this.level = pLevel;
      this.sectionY = pSectionY;
      this.onEmptyAction = pOnEmptyAction;
   }

   @Override
   public boolean isEmpty() {
      return this.listeners.isEmpty();
   }

   @Override
   public void register(GameEventListener pListener) {
      if (this.processing) {
         this.listenersToAdd.add(pListener);
      } else {
         this.listeners.add(pListener);
      }

      DebugPackets.sendGameEventListenerInfo(this.level, pListener);
   }

   @Override
   public void unregister(GameEventListener pListener) {
      if (this.processing) {
         this.listenersToRemove.add(pListener);
      } else {
         this.listeners.remove(pListener);
      }

      if (this.listeners.isEmpty()) {
         this.onEmptyAction.apply(this.sectionY);
      }
   }

   @Override
   public boolean visitInRangeListeners(Holder<GameEvent> pGameEvent, Vec3 pPos, GameEvent.Context pContext, GameEventListenerRegistry.ListenerVisitor pVisitor) {
      this.processing = true;
      boolean $$4 = false;

      try {
         Iterator<GameEventListener> $$5 = this.listeners.iterator();

         while ($$5.hasNext()) {
            GameEventListener $$6 = $$5.next();
            if (this.listenersToRemove.remove($$6)) {
               $$5.remove();
            } else {
               Optional<Vec3> $$7 = getPostableListenerPosition(this.level, pPos, $$6);
               if ($$7.isPresent()) {
                  pVisitor.visit($$6, $$7.get());
                  $$4 = true;
               }
            }
         }
      } finally {
         this.processing = false;
      }

      if (!this.listenersToAdd.isEmpty()) {
         this.listeners.addAll(this.listenersToAdd);
         this.listenersToAdd.clear();
      }

      if (!this.listenersToRemove.isEmpty()) {
         this.listeners.removeAll(this.listenersToRemove);
         this.listenersToRemove.clear();
      }

      return $$4;
   }

   private static Optional<Vec3> getPostableListenerPosition(ServerLevel pLevel, Vec3 pPos, GameEventListener pListener) {
      Optional<Vec3> $$3 = pListener.getListenerSource().getPosition(pLevel);
      if ($$3.isEmpty()) {
         return Optional.empty();
      } else {
         double $$4 = BlockPos.containing($$3.get()).distSqr(BlockPos.containing(pPos));
         int $$5 = pListener.getListenerRadius() * pListener.getListenerRadius();
         return $$4 > (double)$$5 ? Optional.empty() : $$3;
      }
   }

   @FunctionalInterface
   public interface OnEmptyAction {
      void apply(int pSectionY);
   }
}
