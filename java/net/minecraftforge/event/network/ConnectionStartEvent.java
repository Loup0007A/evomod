package net.minecraftforge.event.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ConnectionStartEvent extends Event {
   private final Connection connection;

   @Internal
   public ConnectionStartEvent(Connection connection) {
      this.connection = connection;
   }

   public Connection getConnection() {
      return this.connection;
   }

   public boolean isClient() {
      return this.connection.getSending() == PacketFlow.SERVERBOUND;
   }
}
