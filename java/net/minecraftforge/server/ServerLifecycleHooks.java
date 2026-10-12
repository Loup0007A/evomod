package net.minecraftforge.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ServerLifecycleHooks {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker SERVERHOOKS = MarkerManager.getMarker("SERVERHOOKS");
   private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");
   private static final AtomicBoolean allowLogins = new AtomicBoolean(false);
   private static volatile CountDownLatch exitLatch = null;
   private static MinecraftServer currentServer;

   private static Path getServerConfigPath(MinecraftServer server) {
      Path serverConfig = server.getWorldPath(SERVERCONFIG);
      if (!Files.isDirectory(serverConfig)) {
         try {
            Files.createDirectories(serverConfig);
         } catch (IOException var3) {
            throw new RuntimeException(var3);
         }
      }

      return serverConfig;
   }

   public static boolean handleServerAboutToStart(MinecraftServer server) {
      currentServer = server;
      LogicalSidedProvider.setServer(() -> server);
      ConfigTracker.INSTANCE.loadConfigs(Type.SERVER, getServerConfigPath(server));
      runModifiers(server);
      return !MinecraftForge.EVENT_BUS.post(new ServerAboutToStartEvent(server));
   }

   public static boolean handleServerStarting(MinecraftServer server) {
      DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            LanguageHook.loadLanguagesOnServer(server);
            if (!(server instanceof GameTestServer)) {
               ForgeGameTestHooks.registerGametests();
            }
         });
      PermissionAPI.initializePermissionAPI();
      return !MinecraftForge.EVENT_BUS.post(new ServerStartingEvent(server));
   }

   public static void expectServerStopped() {
      exitLatch = new CountDownLatch(1);
   }

   public static void handleServerStopped(MinecraftServer server) {
      if (!server.isDedicatedServer()) {
         GameData.revertToFrozen();
      }

      MinecraftForge.EVENT_BUS.post(new ServerStoppedEvent(server));
      currentServer = null;
      LogicalSidedProvider.setServer(null);
      CountDownLatch latch = exitLatch;
      if (latch != null) {
         latch.countDown();
         exitLatch = null;
      }

      ConfigTracker.INSTANCE.unloadConfigs(Type.SERVER, getServerConfigPath(server));
   }

   public static MinecraftServer getCurrentServer() {
      return currentServer;
   }

   private static void runModifiers(MinecraftServer server) {
      RegistryAccess registries = server.registryAccess();
      List<BiomeModifier> biomeModifiers = registries.registryOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS).holders().map(Holder::value).toList();
      List<StructureModifier> structureModifiers = registries.registryOrThrow(ForgeRegistries.Keys.STRUCTURE_MODIFIERS).holders().map(Holder::value).toList();
      registries.registryOrThrow(Registries.BIOME)
         .holders()
         .forEach(biomeHolder -> biomeHolder.value().modifiableBiomeInfo().applyBiomeModifiers(biomeHolder, biomeModifiers));
      registries.registryOrThrow(Registries.LEVEL_STEM).forEach(levelStem -> levelStem.generator().refreshFeaturesPerStep());
      registries.registryOrThrow(Registries.STRUCTURE)
         .holders()
         .forEach(structureHolder -> structureHolder.value().modifiableStructureInfo().applyStructureModifiers(structureHolder, structureModifiers));
   }

   public static void handleServerStarted(MinecraftServer server) {
      MinecraftForge.EVENT_BUS.post(new ServerStartedEvent(server));
      allowLogins.set(true);
   }

   public static void handleServerStopping(MinecraftServer server) {
      allowLogins.set(false);
      MinecraftForge.EVENT_BUS.post(new ServerStoppingEvent(server));
   }

   public static boolean handleServerLogin(ClientIntentionPacket packet, Connection connection) {
      NetworkContext ctx = NetworkContext.get(connection);
      ctx.processIntention(packet.hostName());
      if (!allowLogins.get()) {
         return rejectConnection(connection, ctx.getType(), "Server is still starting! Please wait before reconnecting.");
      } else if (packet.intention() == ClientIntent.STATUS) {
         return true;
      } else if (ctx.getType() == ConnectionType.MODDED && ctx.getNetVersion() != 0) {
         return rejectConnection(
            connection,
            ctx.getType(),
            "This modded server is not impl compatible with your modded client. Please verify your Forge version closely matches the server. Got net version "
               + ctx.getNetVersion()
               + " this server is net version 0"
         );
      } else if (ctx.getType() == ConnectionType.VANILLA && !NetworkRegistry.acceptsVanillaClientConnections()) {
         return rejectConnection(
            connection, ctx.getType(), "This server has mods that require Forge to be installed on the client. Contact your server admin for more details."
         );
      } else {
         NetworkRegistry.onConnectionStart(connection);
         return true;
      }
   }

   private static boolean rejectConnection(Connection connection, ConnectionType type, String message) {
      LOGGER.info(SERVERHOOKS, "[{}] Disconnecting {} connection attempt: {}", connection.getLoggableAddress(true), type, message);
      MutableComponent text = Component.literal(message);
      connection.send(new ClientboundLoginDisconnectPacket(text));
      connection.disconnect(text);
      return false;
   }
}
