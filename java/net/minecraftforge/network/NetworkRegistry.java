package net.minecraftforge.network;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.DataPackRegistriesHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NetworkRegistry {
   static final Logger LOGGER = LogManager.getLogger();
   static final Marker NETREGISTRY = MarkerManager.getMarker("NETREGISTRY");
   private static Map<ResourceLocation, NetworkInstance> instances = Collections.synchronizedMap(new HashMap<>());
   private static Map<ResourceLocation, NetworkInstance> byName = Collections.synchronizedMap(new HashMap<>());
   static boolean lock = false;

   public static boolean acceptsVanillaClientConnections() {
      return listRejectedVanillaMods(n -> n.clientAcceptedVersions).isEmpty() && DataPackRegistriesHooks.getSyncedCustomRegistries().isEmpty();
   }

   public static boolean canConnectToVanillaServer() {
      return listRejectedVanillaMods(n -> n.serverAcceptedVersions).isEmpty();
   }

   @Nullable
   public static NetworkInstance findTarget(ResourceLocation resourceLocation) {
      return byName.get(resourceLocation);
   }

   static Map<ResourceLocation, ServerStatusPing.ChannelData> buildChannelVersionsForListPing() {
      HashMap<ResourceLocation, ServerStatusPing.ChannelData> ret = new HashMap<>();

      for (NetworkInstance channel : instances.values()) {
         ret.put(channel.getChannelName(), channel.pingData);
      }

      return ret;
   }

   static List<String> listRejectedVanillaMods(Function<NetworkInstance, Channel.VersionTest> testFunction) {
      ArrayList<String> results = new ArrayList<>();

      for (NetworkInstance net : instances.values()) {
         boolean test = testFunction.apply(net).accepts(Channel.VersionTest.Status.VANILLA, -1);
         LOGGER.debug(NETREGISTRY, "Channel '{}' : Vanilla acceptance test: {}", net.getChannelName(), test ? "ACCEPTED" : "REJECTED");
         if (!test) {
            results.add(net.getChannelName().toString());
         }
      }

      if (!results.isEmpty()) {
         LOGGER.error(NETREGISTRY, "Channels [{}] rejected vanilla connections", String.join(", ", results));
         return results;
      } else {
         LOGGER.debug(NETREGISTRY, "Accepting channel list from vanilla");
         return Collections.emptyList();
      }
   }

   @Nullable
   public static NetworkContext.NetworkMismatchData validateChannels(Map<ResourceLocation, Integer> incoming, boolean fromClient) {
      String originName = fromClient ? "client" : "server";
      Set<ResourceLocation> missing = new HashSet<>();
      Map<ResourceLocation, NetworkContext.NetworkMismatchData.Version> results = new HashMap<>();

      for (NetworkInstance net : instances.values()) {
         ResourceLocation name = net.getChannelName();
         Channel.VersionTest test = fromClient ? net.clientAcceptedVersions : net.serverAcceptedVersions;
         Channel.VersionTest.Status status = Channel.VersionTest.Status.MISSING;
         int version = 0;
         if (incoming.containsKey(net.getChannelName())) {
            status = Channel.VersionTest.Status.PRESENT;
            version = incoming.get(net.getChannelName());
         }

         boolean accepted = test.accepts(status, version);
         LOGGER.debug(NETREGISTRY, "Channel '{}' : Version test of '{} {}' from {} : {}", name, status, version, originName, accepted ? "ACCEPTED" : "REJECTED");
         if (!accepted) {
            if (status == Channel.VersionTest.Status.MISSING) {
               missing.add(name);
            } else {
               results.put(name, new NetworkContext.NetworkMismatchData.Version(Integer.toString(version), Integer.toString(net.getNetworkProtocolVersion())));
            }
         }
      }

      if (results.isEmpty() && missing.isEmpty()) {
         LOGGER.debug(NETREGISTRY, "Accepting channel list from {}", originName);
         return null;
      } else {
         LOGGER.error(
            NETREGISTRY,
            "Channels [{}] rejected their {} side version number",
            Stream.concat(missing.stream(), results.keySet().stream()).map(Object::toString).collect(Collectors.joining(",")),
            originName
         );
         return new NetworkContext.NetworkMismatchData(results, missing, !fromClient, null);
      }
   }

   public static boolean checkListPingCompatibilityForClient(Map<ResourceLocation, ServerStatusPing.ChannelData> incoming) {
      Set<ResourceLocation> handled = new HashSet<>();
      ArrayList<String> rejected = new ArrayList<>();

      for (NetworkInstance net : instances.values()) {
         Channel.VersionTest.Status status = Channel.VersionTest.Status.MISSING;
         int version = 0;
         if (incoming.containsKey(net.getChannelName())) {
            status = Channel.VersionTest.Status.PRESENT;
            version = incoming.get(net.getChannelName()).version();
         }

         boolean accepted = net.serverAcceptedVersions.accepts(status, version);
         LOGGER.debug(
            NETREGISTRY,
            "Channel '{}' : Version test of '{} {}' during listping : {}",
            net.getChannelName(),
            status,
            version,
            accepted ? "ACCEPTED" : "REJECTED"
         );
         if (!accepted) {
            rejected.add(net.getChannelName().toString());
         }

         handled.add(net.getChannelName());
      }

      ArrayList<String> missingButRequired = new ArrayList<>();
      incoming.forEach((name, data) -> {
         if (data.required() && !handled.contains(name)) {
            missingButRequired.add(name.toString());
         }
      });
      if (!rejected.isEmpty()) {
         LOGGER.error(NETREGISTRY, "Channels [{}] rejected their server side version number during listping", String.join(", ", rejected));
         return false;
      } else if (!missingButRequired.isEmpty()) {
         LOGGER.error(NETREGISTRY, "The server is likely to require channel [{}] to be present, yet we don't have it", String.join(", ", missingButRequired));
         return false;
      } else {
         LOGGER.debug(NETREGISTRY, "Accepting channel list during listping");
         return true;
      }
   }

   public static void lock() {
      lock = true;
   }

   public static void onConnectionStart(Connection connection) {
      ForgeEventFactory.onConnectionStart(connection);
      io.netty.channel.Channel channel = connection.channel();

      for (NetworkInstance inst : instances.values()) {
         if (inst.attributes != null) {
            inst.attributes.forEach((k, v) -> channel.attr(k).compareAndSet(null, v.apply(connection)));
         }

         if (inst.channelHandler != null) {
            inst.channelHandler.accept(connection);
         }
      }
   }

   public static Map<ResourceLocation, Integer> buildChannelVersions() {
      Object2IntOpenHashMap<ResourceLocation> ret = new Object2IntOpenHashMap(instances.size());

      for (NetworkInstance net : instances.values()) {
         ret.put(net.getChannelName(), net.getNetworkProtocolVersion());
      }

      return ret;
   }

   static List<ResourceLocation> buildRegisterList() {
      ArrayList<ResourceLocation> ret = new ArrayList<>(byName.keySet().size());

      for (ResourceLocation name : byName.keySet()) {
         if (!"minecraft".equals(name.getNamespace())) {
            ret.add(name);
         }
      }

      return ret;
   }

   static void register(NetworkInstance instance, ResourceLocation name) {
      checkLock(instance);
      if (byName.containsKey(name)) {
         error("Payload name " + name + " already registered.");
      }

      byName.put(name, instance);
   }

   static void register(NetworkInstance instance) {
      checkLock(instance);
      ResourceLocation name = instance.getChannelName();
      if (instances.containsKey(name)) {
         error("Channel " + name + " already registered.");
      }

      instances.put(name, instance);
   }

   private static void checkLock(NetworkInstance instance) {
      if (lock) {
         error("Attempted to register channel " + instance.getChannelName() + " even though registry phase is over");
      }
   }

   private static void error(String message) {
      LOGGER.error(NETREGISTRY, message);
      throw new IllegalArgumentException(message);
   }
}
