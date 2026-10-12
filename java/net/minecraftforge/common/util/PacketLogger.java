package net.minecraftforge.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class PacketLogger {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("PACKETLOG");
   private final Connection connection;
   public boolean enabled = false;

   public PacketLogger(Connection connection) {
      this.connection = connection;
   }

   public void send(Packet<?> packet) {
      this.common(this.connection.getReceiving(), this.connection.getSending(), this.connection.getOutputboundProtocolInfo(), packet);
   }

   public void recv(Packet<?> packet) {
      this.common(this.connection.getReceiving(), this.connection.getReceiving(), this.connection.getInboundProtocolInfo(), packet);
   }

   private void common(PacketFlow side, PacketFlow flow, ProtocolInfo<?> protocol, Packet<?> packet) {
      if (this.enabled) {
         if (packet instanceof ClientboundFinishConfigurationPacket) {
            this.enabled = false;
         }

         String channel = null;
         String hex = null;
         Objects.requireNonNull(packet);
         switch (packet) {
            case ClientboundCustomPayloadPacket custom:
               channel = custom.payload().type().id().toString();
               hex = hex(protocol, packet);
               break;
            case ServerboundCustomPayloadPacket customx:
               channel = customx.payload().type().id().toString();
               hex = hex(protocol, packet);
               break;
            case ClientboundCustomQueryPacket customxx:
               channel = customxx.payload().id().toString() + " id " + customxx.transactionId();
               hex = hex(protocol, packet);
               break;
            case ServerboundCustomQueryAnswerPacket customxxx:
               channel = packet.getClass().getName() + " id " + customxxx.transactionId();
               hex = hex(protocol, packet);
               break;
         }

         if (channel != null && hex != null) {
            LOGGER.info(MARKER, "{} {} {} {}\n{}", side(side), dir(flow), packet.getClass().getName(), channel, hex);
         } else if (channel != null) {
            LOGGER.info(MARKER, "{} {} {} {}", side(side), dir(flow), packet.getClass().getName(), channel);
         } else {
            LOGGER.info(MARKER, "{} {} {}", side(side), dir(flow), packet.getClass().getName());
         }
      }
   }

   private static String side(PacketFlow side) {
      return side == PacketFlow.CLIENTBOUND ? "CLIENT" : "SERVER";
   }

   private static String dir(PacketFlow flow) {
      return flow == PacketFlow.CLIENTBOUND ? "S->C" : "C->S";
   }

   private static <T extends PacketListener> String hex(ProtocolInfo protocol, Packet packet) {
      ByteBuf buf = Unpooled.buffer();
      protocol.codec().encode(buf, packet);
      return HexDumper.dump(buf);
   }
}
