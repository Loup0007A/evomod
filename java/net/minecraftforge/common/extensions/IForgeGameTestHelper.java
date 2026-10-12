package net.minecraftforge.common.extensions;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public interface IForgeGameTestHelper {
   private GameTestHelper self() {
      return (GameTestHelper)this;
   }

   default void assertTrue(boolean value, Supplier<String> message) {
      if (!value) {
         throw new GameTestAssertException(message.get());
      }
   }

   default void assertFalse(boolean value, Supplier<String> message) {
      if (value) {
         throw new GameTestAssertException(message.get());
      }
   }

   default ServerPlayer makeMockServerPlayer() {
      ServerLevel level = this.self().getLevel();
      CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
         @Override
         public boolean isSpectator() {
            return false;
         }

         @Override
         public boolean isCreative() {
            return true;
         }
      };
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{connection});
      MinecraftServer server = level.getServer();
      ServerGamePacketListenerImpl listener = new ServerGamePacketListenerImpl(server, connection, player, cookie);
      ProtocolInfo<ServerGamePacketListener> info = GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(server.registryAccess()));
      connection.setupInboundProtocol(info, listener);
      return player;
   }
}
