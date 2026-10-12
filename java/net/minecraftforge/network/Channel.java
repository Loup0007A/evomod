package net.minecraftforge.network;

import java.util.Objects;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.packets.LoginWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public abstract class Channel<MSG> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("CHANNEL");
   protected final NetworkInstance instance;

   protected Channel(NetworkInstance instance) {
      this.instance = instance;
   }

   public ResourceLocation getName() {
      return this.instance.getChannelName();
   }

   ResourceLocation getName(MSG packet) {
      return this.getName();
   }

   public int getProtocolVersion() {
      return this.instance.getNetworkProtocolVersion();
   }

   public boolean isRemotePresent(Connection connection) {
      return this.instance.isRemotePresent(connection);
   }

   public abstract void encode(FriendlyByteBuf var1, MSG var2);

   protected Packet<?> toVanillaPacket(Connection connection, MSG message) {
      ConnectionProtocol protocol = connection.getProtocol();

      NetworkProtocol<? extends FriendlyByteBuf> handler = switch (protocol) {
         case LOGIN -> NetworkProtocol.LOGIN;
         case CONFIGURATION -> NetworkProtocol.CONFIGURATION;
         case PLAY -> NetworkProtocol.PLAY;
         default -> throw new IllegalStateException("Unsupported protocol " + protocol.name() + " in Forge Networking Channel");
      };
      return protocol == ConnectionProtocol.LOGIN && this != NetworkInitialization.LOGIN && connection.getSending() == PacketFlow.SERVERBOUND
         ? NetworkInitialization.LOGIN.toVanillaPacket(connection, new LoginWrapper(this, message))
         : handler.buildPacket(connection.getSending(), this, message);
   }

   public void send(MSG msg, Connection connection) {
      connection.send(this.toVanillaPacket(connection, msg));
   }

   public void send(MSG msg, PacketDistributor.PacketTarget target) {
      target.send(target.direction().buildPacket(this, msg));
   }

   public void reply(MSG msg, CustomPayloadEvent.Context context) {
      this.send(msg, context.getConnection());
   }

   protected void validate(Object id, Connection con, NetworkProtocol<?> protocol, PacketFlow direction, boolean sending) {
      PacketFlow actualD = sending ? con.getSending() : con.getReceiving();
      ConnectionProtocol actualP = con.getProtocol();
      ConnectionProtocol expectedP = protocol == null ? actualP : protocol.toVanilla();
      PacketFlow expectedD = direction == null ? actualD : direction;
      if (expectedP != actualP || expectedD != actualD) {
         String error = "Illegal packet "
            + (sending ? "sent" : "received")
            + ", terminating connection. "
            + id
            + " expected "
            + expectedD
            + " "
            + expectedP
            + " but was "
            + actualD
            + " "
            + actualP;
         LOGGER.error(MARKER, error);
         con.disconnect(Component.literal(error));
         throw new IllegalStateException(error);
      }
   }

   @FunctionalInterface
   public interface VersionTest {
      Channel.VersionTest ACCEPT_MISSING = (status, version) -> status == Channel.VersionTest.Status.MISSING;
      Channel.VersionTest ACCEPT_VANILLA = (status, version) -> status == Channel.VersionTest.Status.VANILLA;

      static Channel.VersionTest exact(int version) {
         return (status, remoteVersion) -> status == Channel.VersionTest.Status.PRESENT && version == remoteVersion;
      }

      boolean accepts(Channel.VersionTest.Status var1, int var2);

      default Channel.VersionTest negate() {
         return (status, version) -> !this.accepts(status, version);
      }

      default Channel.VersionTest and(Channel.VersionTest other) {
         Objects.requireNonNull(other);
         return (status, version) -> this.accepts(status, version) && other.accepts(status, version);
      }

      default Channel.VersionTest or(Channel.VersionTest other) {
         Objects.requireNonNull(other);
         return (status, version) -> this.accepts(status, version) || other.accepts(status, version);
      }

      public static enum Status {
         VANILLA,
         MISSING,
         PRESENT;
      }
   }
}
