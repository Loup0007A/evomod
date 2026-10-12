package net.minecraftforge.network;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LogMessageAdapter;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.packets.Acknowledge;
import net.minecraftforge.network.packets.ChannelVersions;
import net.minecraftforge.network.packets.ConfigData;
import net.minecraftforge.network.packets.LoginWrapper;
import net.minecraftforge.network.packets.MismatchData;
import net.minecraftforge.network.packets.ModVersions;
import net.minecraftforge.network.packets.RegistryData;
import net.minecraftforge.network.packets.RegistryList;
import net.minecraftforge.network.tasks.ChannelVersionsTask;
import net.minecraftforge.network.tasks.ModVersionsTask;
import net.minecraftforge.registries.DataPackRegistriesHooks;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class ForgePacketHandler {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("FORGE_PACKET_HANDLER");
   private Set<ResourceLocation> registriesToReceive;
   private Map<ResourceLocation, ForgeRegistry.Snapshot> registrySnapshots = new HashMap<>();
   private int nextAckId = 0;
   private Int2ObjectMap<BiConsumer<Acknowledge, CustomPayloadEvent.Context>> pendingAcknowledgments = new Int2ObjectOpenHashMap();

   ForgePacketHandler(Connection connection) {
   }

   public int expectAck(BiConsumer<Acknowledge, CustomPayloadEvent.Context> consumer) {
      int id = this.nextAckId++;
      this.pendingAcknowledgments.put(id, consumer);
      return id;
   }

   void handleLoginWrapper(LoginWrapper msg, CustomPayloadEvent.Context ctx) {
      ForgeHooks.onCustomPayload(new CustomPayloadEvent(msg.name(), ForgePayload.create(msg.name(), msg.data()), ctx, -1));
   }

   void handleClientAck(Acknowledge msg, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      BiConsumer<Acknowledge, CustomPayloadEvent.Context> consumer = (BiConsumer<Acknowledge, CustomPayloadEvent.Context>)this.pendingAcknowledgments
         .remove(msg.token());
      if (consumer != null) {
         LOGGER.debug(MARKER, "Received acknowledgement {} from client", msg.token());
         consumer.accept(msg, ctx);
      } else {
         LOGGER.error(MARKER, "Received unknown acknowledgement {} from client", msg.token());
         ctx.getConnection().disconnect(Component.literal("Illegal Acknowledge packet received, unknown token: " + Integer.toHexString(msg.token())));
      }
   }

   void handleModVersions(ModVersions list, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      LOGGER.debug(
         MARKER,
         "Received {} connection with modlist [{}]",
         ctx.isClientSide() ? "server" : "client",
         list.mods().keySet().stream().sorted().collect(Collectors.joining(", "))
      );
      NetworkContext nctx = NetworkContext.get(ctx.getConnection());
      nctx.modList.clear();
      nctx.modList.putAll(list.mods());
      if (ctx.isClientSide()) {
         NetworkInitialization.CONFIG.send(ModVersions.create(), ctx.getConnection());
         LOGGER.debug(MARKER, "Accepted server connection");
         nctx.setConnectionType(ConnectionType.MODDED);
      } else {
         nctx.finishTask(ModVersionsTask.TYPE);
      }
   }

   void handleChannelVersions(ChannelVersions list, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      LOGGER.debug(
         MARKER,
         "Received {} connection with channels [{}]",
         ctx.isClientSide() ? "server" : "client",
         list.channels().keySet().stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "))
      );
      NetworkContext nctx = NetworkContext.get(ctx.getConnection());
      nctx.channelList.clear();
      nctx.channelList.putAll(list.channels());
      NetworkContext.NetworkMismatchData invalid = NetworkRegistry.validateChannels(list.channels(), ctx.isServerSide());
      if (invalid != null) {
         LOGGER.error(MARKER, "Terminating connection with {}, mismatched channel list", ctx.isClientSide() ? "server" : "client");
         nctx.mismatchData = new NetworkContext.NetworkMismatchData(invalid.mismatched(), invalid.missing(), invalid.fromServer(), nctx.getModList());
         if (ctx.isServerSide()) {
            NetworkInitialization.CONFIG.reply(new MismatchData(nctx.mismatchData), ctx);
         }

         ctx.getConnection().disconnect(Component.literal("Connection closed - mismatched mod channel list"));
      } else {
         if (ctx.isClientSide()) {
            NetworkInitialization.CONFIG.send(new ChannelVersions(), ctx.getConnection());
         } else {
            nctx.finishTask(ChannelVersionsTask.TYPE);
         }
      }
   }

   void handleModMismatchData(MismatchData data, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      NetworkContext nctx = NetworkContext.get(ctx.getConnection());
      nctx.mismatchData = new NetworkContext.NetworkMismatchData(data.mismatched(), data.missing(), true, nctx.getModList());
      LOGGER.error(
         MARKER,
         "Channels [{}] rejected their client side version number",
         Stream.concat(data.missing().stream(), data.mismatched().keySet().stream()).map(Object::toString).collect(Collectors.joining(", "))
      );
      LOGGER.error(MARKER, "Terminating connection with server, mismatched mod list");
      ctx.getConnection().disconnect(Component.literal("Connection closed - mismatched mod channel list"));
   }

   void handleRegistryList(RegistryList list, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      List<String> missing = new ArrayList<>();
      Set<ResourceKey<? extends Registry<?>>> clientDataPackRegistries = DataPackRegistriesHooks.getSyncedCustomRegistries();

      for (ResourceKey<? extends Registry<?>> key : list.datapacks()) {
         if (!clientDataPackRegistries.contains(key)) {
            LOGGER.error(MARKER, "Missing required datapack registry: {}", key.location());
            missing.add(key.location().toString());
         }
      }

      if (!missing.isEmpty()) {
         ctx.getConnection().disconnect(Component.translatable("fml.menu.multiplayer.missingdatapackregistries", String.join(", ", missing)));
      } else {
         NetworkInitialization.CONFIG.reply(new Acknowledge(list.token()), ctx);
         this.registriesToReceive = new HashSet<>(list.normal());
         this.registrySnapshots.clear();
         LOGGER.debug(
            MARKER,
            "Expecting {} registries: {}",
            this.registriesToReceive.size(),
            this.registriesToReceive.stream().map(Object::toString).collect(Collectors.joining(", "))
         );
      }
   }

   void handleRegistryData(RegistryData msg, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      LOGGER.debug(MARKER, "Received registry data packet for {} token {}", msg.name(), msg.token());
      if (!this.registriesToReceive.remove(msg.name())) {
         LOGGER.error(MARKER, "Received unexpected registry data packet for {}", msg.name());
         ctx.getConnection().disconnect(Component.literal("Illegal Registry Data packet received, unexpected regitry: " + msg.name()));
      } else {
         this.registrySnapshots.put(msg.name(), msg.data());
         boolean continueHandshake = true;
         if (this.registriesToReceive.isEmpty()) {
            continueHandshake = this.handleRegistryLoading(ctx);
         }

         if (!continueHandshake) {
            LOGGER.error(MARKER, "Connection closed, not continuing handshake");
         } else {
            NetworkInitialization.CONFIG.reply(new Acknowledge(msg.token()), ctx);
         }
      }
   }

   private boolean handleRegistryLoading(CustomPayloadEvent.Context ctx) {
      NetworkContext nctx = NetworkContext.get(ctx.getConnection());
      CountDownLatch block = new CountDownLatch(1);
      AtomicReference<NetworkContext.NetworkMismatchData> errorData = new AtomicReference<>();
      ctx.enqueueWork(
         () -> {
            LOGGER.debug(MARKER, "Injecting registry snapshot from server.");
            Multimap<ResourceLocation, ResourceLocation> missingData = GameData.injectSnapshot(this.registrySnapshots, false, false);
            LOGGER.debug(MARKER, "Snapshot injected.");
            if (!missingData.isEmpty()) {
               LOGGER.error(
                  MARKER,
                  "Missing registry data for connection:\n{}",
                  LogMessageAdapter.adapt(sb -> missingData.forEach((k, v) -> sb.append("\t").append(k).append(": ").append(v).append('\n')))
               );
               List<String> missingMods = missingData.values().stream().map(ResourceLocation::getNamespace).distinct().toList();
               HashMap<ResourceLocation, NetworkContext.NetworkMismatchData.Version> mismatched = new HashMap<>();
               HashSet<ResourceLocation> missing = new HashSet<>();

               for (String id : missingMods) {
                  ResourceLocation key = ResourceLocation.fromNamespaceAndPath(id, "");
                  ModContainer container = (ModContainer)ModList.get().getModContainerById(id).orElse(null);
                  if (container != null) {
                     mismatched.put(key, new NetworkContext.NetworkMismatchData.Version(container.getModInfo().getVersion().toString(), ""));
                  } else {
                     missing.add(key);
                  }
               }

               errorData.set(new NetworkContext.NetworkMismatchData(mismatched, missing, true, nctx.getModList()));
            }

            block.countDown();
         }
      );
      LOGGER.debug(MARKER, "Waiting for registries to load.");

      try {
         block.await();
      } catch (InterruptedException var6) {
         Thread.interrupted();
      }

      NetworkContext.NetworkMismatchData mismatchData = errorData.get();
      if (mismatchData != null) {
         LOGGER.error(MARKER, "Failed to load registry, closing connection.");
         nctx.mismatchData = mismatchData;
         ctx.getConnection().disconnect(Component.literal("Failed to synchronize registry data from server, closing connection"));
         return false;
      } else {
         LOGGER.debug(MARKER, "Registry load complete, continuing handshake.");
         return true;
      }
   }

   void handleConfigSync(ConfigData msg, CustomPayloadEvent.Context ctx) {
      ctx.setPacketHandled(true);
      LOGGER.debug(MARKER, "Received config sync from server for {}", msg.name());
      if (!ctx.getConnection().isMemoryConnection()) {
         ModConfig cfg = (ModConfig)ConfigTracker.INSTANCE.fileMap().get(msg.name());
         if (cfg != null) {
            cfg.acceptSyncedConfig(msg.data());
         }
      }
   }
}
