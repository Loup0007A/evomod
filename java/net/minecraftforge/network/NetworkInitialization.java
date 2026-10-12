package net.minecraftforge.network;

import io.netty.util.AttributeKey;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.packets.Acknowledge;
import net.minecraftforge.network.packets.ChannelVersions;
import net.minecraftforge.network.packets.ConfigData;
import net.minecraftforge.network.packets.LoginWrapper;
import net.minecraftforge.network.packets.MismatchData;
import net.minecraftforge.network.packets.ModVersions;
import net.minecraftforge.network.packets.OpenContainer;
import net.minecraftforge.network.packets.RegistryData;
import net.minecraftforge.network.packets.RegistryList;
import net.minecraftforge.network.packets.SpawnEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class NetworkInitialization {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("FORGE_NETWORK");
   public static final ResourceLocation LOGIN_NAME = ResourceLocation.fromNamespaceAndPath("forge", "login");
   private static final ResourceLocation HANDSHAKE_NAME = ResourceLocation.fromNamespaceAndPath("forge", "handshake");
   public static final AttributeKey<ForgePacketHandler> CONTEXT = AttributeKey.newInstance(HANDSHAKE_NAME.toString());
   public static final SimpleChannel LOGIN = ChannelBuilder.named(LOGIN_NAME)
      .optional()
      .networkProtocolVersion(0)
      .attribute(CONTEXT, ForgePacketHandler::new)
      .simpleChannel()
      .login()
      .serverbound()
      .add(LoginWrapper.class, LoginWrapper.STREAM_CODEC, ctx(ForgePacketHandler::handleLoginWrapper))
      .build();
   public static final SimpleChannel CONFIG = ChannelBuilder.named(HANDSHAKE_NAME)
      .optional()
      .networkProtocolVersion(0)
      .simpleChannel()
      .configuration()
      .serverbound()
      .add(Acknowledge.class, Acknowledge.STREAM_CODEC, ctx(ForgePacketHandler::handleClientAck))
      .bidirectional()
      .add(ModVersions.class, ModVersions.STREAM_CODEC, ctx(ForgePacketHandler::handleModVersions))
      .add(ChannelVersions.class, ChannelVersions.STREAM_CODEC, ctx(ForgePacketHandler::handleChannelVersions))
      .clientbound()
      .add(RegistryList.class, RegistryList.STREAM_CODEC, ctx(ForgePacketHandler::handleRegistryList))
      .add(RegistryData.class, RegistryData.STREAM_CODEC, ctx(ForgePacketHandler::handleRegistryData))
      .add(ConfigData.class, ConfigData.STREAM_CODEC, ctx(ForgePacketHandler::handleConfigSync))
      .add(MismatchData.class, MismatchData.STREAM_CODEC, ctx(ForgePacketHandler::handleModMismatchData))
      .play()
      .clientbound()
      .addMain(SpawnEntity.class, SpawnEntity.STREAM_CODEC, SpawnEntity::handle)
      .addMain(OpenContainer.class, OpenContainer.STREAM_CODEC, OpenContainer::handle)
      .build();
   public static final SimpleChannel PLAY = CONFIG;

   public static void init() {
      for (Channel channel : new Channel[]{LOGIN, CONFIG, PLAY, ChannelListManager.CHANNEL}) {
         LOGGER.debug(MARKER, "Registering Network {} v{}", channel.getName(), channel.getProtocolVersion());
      }
   }

   public static int getVersion() {
      return CONFIG.getProtocolVersion();
   }

   private static <MSG> BiConsumer<MSG, CustomPayloadEvent.Context> ctx(NetworkInitialization.Handler<MSG> handler) {
      return (msg, ctx) -> {
         ForgePacketHandler inst = (ForgePacketHandler)ctx.getConnection().channel().attr(CONTEXT).get();
         handler.handle(inst, msg, ctx);
      };
   }

   private interface Handler<MSG> {
      void handle(ForgePacketHandler var1, MSG var2, CustomPayloadEvent.Context var3);
   }
}
