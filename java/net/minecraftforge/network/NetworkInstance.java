package net.minecraftforge.network;

import io.netty.util.AttributeKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventListener;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class NetworkInstance {
   private final IEventBus networkEventBus;
   private final ResourceLocation channelName;
   private final int networkProtocolVersion;
   final Channel.VersionTest clientAcceptedVersions;
   final Channel.VersionTest serverAcceptedVersions;
   final Map<AttributeKey<?>, Function<Connection, ?>> attributes;
   final Consumer<Connection> channelHandler;
   final ServerStatusPing.ChannelData pingData;
   private final Set<ResourceLocation> ids = new HashSet<>();

   NetworkInstance(
      ResourceLocation channelName,
      int networkProtocolVersion,
      Channel.VersionTest clientAcceptedVersions,
      Channel.VersionTest serverAcceptedVersions,
      Map<AttributeKey<?>, Function<Connection, ?>> attributes,
      Consumer<Connection> channelHandler
   ) {
      this.channelName = channelName;
      this.networkProtocolVersion = networkProtocolVersion;
      this.clientAcceptedVersions = clientAcceptedVersions;
      this.serverAcceptedVersions = serverAcceptedVersions;
      this.attributes = attributes;
      this.channelHandler = channelHandler;
      this.networkEventBus = BusBuilder.builder().setExceptionHandler(this::handleError).useModLauncher().build();
      this.pingData = new ServerStatusPing.ChannelData(
         channelName, networkProtocolVersion, this.clientAcceptedVersions.accepts(Channel.VersionTest.Status.MISSING, -1)
      );
   }

   private void handleError(IEventBus iEventBus, Event event, IEventListener[] iEventListeners, int i, Throwable throwable) {
   }

   public <T extends CustomPayloadEvent> void addListener(Consumer<T> eventListener) {
      this.networkEventBus.addListener(eventListener);
   }

   public void registerObject(Object object) {
      this.networkEventBus.register(object);
   }

   public void unregisterObject(Object object) {
      this.networkEventBus.unregister(object);
   }

   public boolean dispatch(CustomPayloadEvent event) {
      this.networkEventBus.post(event);
      return event.getSource().getPacketHandled();
   }

   public NetworkInstance addChild(ResourceLocation name) {
      NetworkRegistry.register(this, name);
      this.ids.add(name);
      return this;
   }

   ResourceLocation getChannelName() {
      return this.channelName;
   }

   int getNetworkProtocolVersion() {
      return this.networkProtocolVersion;
   }

   void registrationChange(ResourceLocation name, boolean registered) {
   }

   boolean isRemotePresent(Connection con) {
      Set<ResourceLocation> channels = NetworkContext.get(con).getRemoteChannels();
      return channels.containsAll(this.ids);
   }
}
