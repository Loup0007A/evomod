package net.minecraftforge.event.network;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.ForgePayload;
import org.jetbrains.annotations.Nullable;

public class CustomPayloadEvent extends Event {
   private final ResourceLocation channel;
   private final Object payload;
   private final FriendlyByteBuf data;
   private final CustomPayloadEvent.Context source;
   private final int loginIndex;

   public CustomPayloadEvent(ResourceLocation channel, Object payload, CustomPayloadEvent.Context source, int loginIndex) {
      this.channel = channel;
      this.payload = payload;
      this.data = payload instanceof ForgePayload forge ? forge.data() : null;
      this.source = source;
      this.loginIndex = loginIndex;
   }

   public ResourceLocation getChannel() {
      return this.channel;
   }

   @Nullable
   public FriendlyByteBuf getPayload() {
      return this.data;
   }

   public Object getPayloadObject() {
      return this.payload;
   }

   public int getLoginIndex() {
      return this.loginIndex;
   }

   public CustomPayloadEvent.Context getSource() {
      return this.source;
   }

   public static class Context {
      private final Connection connection;
      private final boolean client;
      private boolean packetHandled;

      public Context(Connection connection) {
         this.connection = connection;
         this.client = connection.getReceiving() == PacketFlow.CLIENTBOUND;
      }

      public boolean isClientSide() {
         return this.client;
      }

      public boolean isServerSide() {
         return !this.isClientSide();
      }

      public Connection getConnection() {
         return this.connection;
      }

      public <T> Attribute<T> attr(AttributeKey<T> key) {
         return this.connection.channel().attr(key);
      }

      public void setPacketHandled(boolean packetHandled) {
         this.packetHandled = packetHandled;
      }

      public boolean getPacketHandled() {
         return this.packetHandled;
      }

      public CompletableFuture<Void> enqueueWork(Runnable runnable) {
         BlockableEventLoop<? super TickTask> executor = LogicalSidedProvider.WORKQUEUE.get(this.isClientSide());
         if (!executor.isSameThread()) {
            return executor.submitAsync(runnable);
         } else {
            runnable.run();
            return CompletableFuture.completedFuture(null);
         }
      }

      @Nullable
      public ServerPlayer getSender() {
         return this.connection.getPacketListener() instanceof ServerGamePacketListenerImpl handler ? handler.player : null;
      }
   }
}
