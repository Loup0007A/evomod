package net.minecraftforge.network;

import java.util.function.Consumer;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.resources.ResourceLocation;

public class NetworkProtocol<B extends FriendlyByteBuf> {
   public static final NetworkProtocol<RegistryFriendlyByteBuf> PLAY = new NetworkProtocol<>(ConnectionProtocol.PLAY);
   public static final NetworkProtocol<FriendlyByteBuf> LOGIN = new NetworkProtocol<>(ConnectionProtocol.LOGIN);
   public static final NetworkProtocol<FriendlyByteBuf> CONFIGURATION = new NetworkProtocol<>(ConnectionProtocol.CONFIGURATION);
   private final ConnectionProtocol protocol;

   private NetworkProtocol(ConnectionProtocol protocol) {
      this.protocol = protocol;
   }

   @Override
   public String toString() {
      return this.protocol.name();
   }

   public ConnectionProtocol toVanilla() {
      return this.protocol;
   }

   public <T extends PacketListener, MSG> Packet<T> buildPacket(PacketFlow direction, Channel<MSG> channel, MSG packet) {
      return this.buildPacket(direction, channel.getName(packet), buf -> channel.encode(buf, packet));
   }

   public <T extends PacketListener> Packet<T> buildPacket(PacketFlow direction, ResourceLocation name, Consumer<B> encoder) {
      ForgePayload payload = ForgePayload.create(name, encoder);
      switch (this.protocol) {
         case PLAY:
         case CONFIGURATION:
            if (direction == PacketFlow.CLIENTBOUND) {
               return new ClientboundCustomPayloadPacket(payload);
            }

            return new ServerboundCustomPayloadPacket(payload);
         case LOGIN:
            if (direction == PacketFlow.CLIENTBOUND) {
               return new ClientboundCustomQueryPacket(0, payload);
            }

            return new ServerboundCustomQueryAnswerPacket(0, payload);
         default:
            throw new IllegalArgumentException("Invalid protocol, shouldn't be possible as this is a private class..");
      }
   }
}
