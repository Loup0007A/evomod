package net.minecraftforge.network;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.network.packets.ModVersions;
import org.jetbrains.annotations.ApiStatus.Internal;

public class NetworkContext {
   private static final String MARKER = "FORGE";
   public static final int NET_VERSION = 0;
   private static final AttributeKey<NetworkContext> CHANNEL_KEY = AttributeKey.valueOf("forge:netcontext");
   private final Connection connection;
   private ConnectionType type = ConnectionType.VANILLA;
   private int netVersion = 0;
   @Internal
   Set<ResourceLocation> remoteChannels = new HashSet<>();
   private Set<ResourceLocation> remoteChannelsView = Collections.unmodifiableSet(this.remoteChannels);
   @Internal
   Set<ResourceLocation> sentChannels = new HashSet<>();
   @Internal
   Map<String, ModVersions.Info> modList = new HashMap<>();
   private Map<String, ModVersions.Info> modListView = Collections.unmodifiableMap(this.modList);
   @Internal
   Map<ResourceLocation, Integer> channelList = new Object2IntOpenHashMap();
   @Internal
   NetworkContext.NetworkMismatchData mismatchData = null;

   public static NetworkContext get(Connection connection) {
      Attribute<NetworkContext> attr = connection.channel().attr(CHANNEL_KEY);
      NetworkContext ret = (NetworkContext)attr.get();
      if (ret == null) {
         synchronized (connection.channel()) {
            ret = (NetworkContext)attr.get();
            if (ret == null) {
               ret = new NetworkContext(connection);
               attr.set(ret);
            }
         }
      }

      return ret;
   }

   private NetworkContext(Connection connection) {
      this.connection = connection;
   }

   public ConnectionType getType() {
      return this.type;
   }

   public int getNetVersion() {
      return this.netVersion;
   }

   public Set<ResourceLocation> getRemoteChannels() {
      return this.remoteChannelsView;
   }

   public Map<String, ModVersions.Info> getModList() {
      return this.modListView;
   }

   @Internal
   public static String enhanceHostName(String hostName) {
      return String.join("\u0000", hostName, "FORGE");
   }

   @Internal
   public void processIntention(String hostName) {
      this.type = ConnectionType.VANILLA;
      this.netVersion = 0;
      int idx = hostName.indexOf(0);
      if (idx != -1) {
         for (String pt : hostName.split("\u0000")) {
            if (pt.startsWith("FORGE")) {
               this.type = ConnectionType.MODDED;
               if (pt.length() > "FORGE".length()) {
                  this.netVersion = Integer.valueOf(pt.substring("FORGE".length()));
               }
            }
         }
      }
   }

   @Internal
   public void finishTask(ConfigurationTask.Type task) {
      if (this.connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl cfg) {
         cfg.finishCurrentTask(task);
      } else {
         throw new IllegalStateException(
            "Attempted to finish task " + task + " when packet listener was " + this.connection.getPacketListener().getClass().getName()
         );
      }
   }

   @Internal
   public NetworkContext.NetworkMismatchData getMismatchs() {
      return this.mismatchData;
   }

   @Internal
   void setConnectionType(ConnectionType type) {
      this.type = type;
   }

   @Internal
   public static record NetworkMismatchData(
      Map<ResourceLocation, NetworkContext.NetworkMismatchData.Version> mismatched,
      Set<ResourceLocation> missing,
      boolean fromServer,
      Map<String, ModVersions.Info> mods
   ) {
      public static record Version(String received, String had) {
      }
   }
}
