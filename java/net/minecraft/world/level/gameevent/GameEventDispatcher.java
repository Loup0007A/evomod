package net.minecraft.world.level.gameevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

public class GameEventDispatcher {
   private final ServerLevel level;

   public GameEventDispatcher(ServerLevel pLevel) {
      this.level = pLevel;
   }

   public void post(Holder<GameEvent> pGameEvent, Vec3 pPos, GameEvent.Context pContext) {
      int $$3 = pGameEvent.value().notificationRadius();
      BlockPos $$4 = BlockPos.containing(pPos);
      int $$5 = SectionPos.blockToSectionCoord($$4.getX() - $$3);
      int $$6 = SectionPos.blockToSectionCoord($$4.getY() - $$3);
      int $$7 = SectionPos.blockToSectionCoord($$4.getZ() - $$3);
      int $$8 = SectionPos.blockToSectionCoord($$4.getX() + $$3);
      int $$9 = SectionPos.blockToSectionCoord($$4.getY() + $$3);
      int $$10 = SectionPos.blockToSectionCoord($$4.getZ() + $$3);
      List<GameEvent.ListenerInfo> $$11 = new ArrayList<>();
      GameEventListenerRegistry.ListenerVisitor $$12 = (p_327435_, p_327436_) -> {
         if (p_327435_.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE) {
            $$11.add(new GameEvent.ListenerInfo(pGameEvent, pPos, pContext, p_327435_, p_327436_));
         } else {
            p_327435_.handleGameEvent(this.level, pGameEvent, pContext, pPos);
         }
      };
      boolean $$13 = false;

      for (int $$14 = $$5; $$14 <= $$8; $$14++) {
         for (int $$15 = $$7; $$15 <= $$10; $$15++) {
            ChunkAccess $$16 = this.level.getChunkSource().getChunkNow($$14, $$15);
            if ($$16 != null) {
               for (int $$17 = $$6; $$17 <= $$9; $$17++) {
                  $$13 |= $$16.getListenerRegistry($$17).visitInRangeListeners(pGameEvent, pPos, pContext, $$12);
               }
            }
         }
      }

      if (!$$11.isEmpty()) {
         this.handleGameEventMessagesInQueue($$11);
      }

      if ($$13) {
         DebugPackets.sendGameEventInfo(this.level, pGameEvent, pPos);
      }
   }

   private void handleGameEventMessagesInQueue(List<GameEvent.ListenerInfo> pListenerInfos) {
      Collections.sort(pListenerInfos);

      for (GameEvent.ListenerInfo $$1 : pListenerInfos) {
         GameEventListener $$2 = $$1.recipient();
         $$2.handleGameEvent(this.level, $$1.gameEvent(), $$1.context(), $$1.source());
      }
   }
}
