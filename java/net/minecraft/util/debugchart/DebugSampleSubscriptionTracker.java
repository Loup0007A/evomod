package net.minecraft.util.debugchart;

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import net.minecraft.Util;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class DebugSampleSubscriptionTracker {
   public static final int STOP_SENDING_AFTER_TICKS = 200;
   public static final int STOP_SENDING_AFTER_MS = 10000;
   private final PlayerList playerList;
   private final EnumMap<RemoteDebugSampleType, Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt>> subscriptions;
   private final Queue<DebugSampleSubscriptionTracker.SubscriptionRequest> subscriptionRequestQueue = new LinkedList<>();

   public DebugSampleSubscriptionTracker(PlayerList pPlayerList) {
      this.playerList = pPlayerList;
      this.subscriptions = new EnumMap<>(RemoteDebugSampleType.class);

      for (RemoteDebugSampleType $$1 : RemoteDebugSampleType.values()) {
         this.subscriptions.put($$1, Maps.newHashMap());
      }
   }

   public boolean shouldLogSamples(RemoteDebugSampleType pSampleType) {
      return !this.subscriptions.get(pSampleType).isEmpty();
   }

   public void broadcast(ClientboundDebugSamplePacket pPacket) {
      for (ServerPlayer $$2 : this.subscriptions.get(pPacket.debugSampleType()).keySet()) {
         $$2.connection.send(pPacket);
      }
   }

   public void subscribe(ServerPlayer pPlayer, RemoteDebugSampleType pSampleType) {
      if (this.playerList.isOp(pPlayer.getGameProfile())) {
         this.subscriptionRequestQueue.add(new DebugSampleSubscriptionTracker.SubscriptionRequest(pPlayer, pSampleType));
      }
   }

   public void tick(int pTick) {
      long $$1 = Util.getMillis();
      this.handleSubscriptions($$1, pTick);
      this.handleUnsubscriptions($$1, pTick);
   }

   private void handleSubscriptions(long pMillis, int pTick) {
      for (DebugSampleSubscriptionTracker.SubscriptionRequest $$2 : this.subscriptionRequestQueue) {
         this.subscriptions.get($$2.sampleType()).put($$2.player(), new DebugSampleSubscriptionTracker.SubscriptionStartedAt(pMillis, pTick));
      }
   }

   private void handleUnsubscriptions(long pMillis, int pTick) {
      for (Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt> $$2 : this.subscriptions.values()) {
         $$2.entrySet().removeIf(p_336353_ -> {
            boolean $$3 = !this.playerList.isOp(p_336353_.getKey().getGameProfile());
            DebugSampleSubscriptionTracker.SubscriptionStartedAt $$4 = p_336353_.getValue();
            return $$3 || pTick > $$4.tick() + 200 && pMillis > $$4.millis() + 10000L;
         });
      }
   }

   static record SubscriptionRequest(ServerPlayer player, RemoteDebugSampleType sampleType) {
   }

   static record SubscriptionStartedAt(long millis, int tick) {
   }
}
