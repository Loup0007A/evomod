package net.minecraftforge.network;

import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.payload.PayloadConnection;

public class ChannelBuilder {
   private final ResourceLocation name;
   private int networkProtocolVersion = 0;
   private Channel.VersionTest clientAcceptedVersions;
   private Channel.VersionTest serverAcceptedVersions;
   private Map<AttributeKey<?>, Function<Connection, ?>> attributes = new HashMap<>();
   private Consumer<Connection> connectionHandler;
   private boolean registerSelf = true;

   public static ChannelBuilder named(String channelName) {
      return named(ResourceLocation.parse(channelName));
   }

   public static ChannelBuilder named(ResourceLocation channelName) {
      return new ChannelBuilder(channelName);
   }

   private ChannelBuilder(ResourceLocation name) {
      this.name = name;
   }

   public ChannelBuilder networkProtocolVersion(int version) {
      if (version < 0) {
         throw new IllegalArgumentException("Invalid network protocol: " + version);
      } else {
         this.networkProtocolVersion = version;
         return this;
      }
   }

   public ChannelBuilder acceptedVersions(Channel.VersionTest test) {
      return this.clientAcceptedVersions(test).serverAcceptedVersions(test);
   }

   public ChannelBuilder clientAcceptedVersions(Channel.VersionTest test) {
      this.clientAcceptedVersions = test;
      return this;
   }

   public ChannelBuilder serverAcceptedVersions(Channel.VersionTest test) {
      this.serverAcceptedVersions = test;
      return this;
   }

   public ChannelBuilder optional() {
      return this.optionalServer().optionalClient();
   }

   public ChannelBuilder optionalServer() {
      return this.clientAcceptedVersions(Channel.VersionTest.ACCEPT_VANILLA.or(Channel.VersionTest.ACCEPT_MISSING.or(this.getClientAcceptedVersions())));
   }

   public ChannelBuilder optionalClient() {
      return this.serverAcceptedVersions(Channel.VersionTest.ACCEPT_VANILLA.or(Channel.VersionTest.ACCEPT_MISSING.or(this.getServerAcceptedVersions())));
   }

   public <T> ChannelBuilder attribute(AttributeKey<T> key, Supplier<T> factory) {
      return this.attribute(key, con -> factory.get());
   }

   public <T> ChannelBuilder attribute(AttributeKey<T> key, Function<Connection, T> factory) {
      this.attributes.put(key, factory);
      return this;
   }

   public ChannelBuilder connectionHandler(Consumer<Connection> handler) {
      this.connectionHandler = handler;
      return this;
   }

   private NetworkInstance createNetworkInstance() {
      NetworkInstance instance = new NetworkInstance(
         this.name, this.networkProtocolVersion, this.getClientAcceptedVersions(), this.getServerAcceptedVersions(), this.attributes, this.connectionHandler
      );
      NetworkRegistry.register(instance);
      if (this.registerSelf) {
         instance.addChild(this.name);
      }

      return instance;
   }

   private Channel.VersionTest getClientAcceptedVersions() {
      return this.clientAcceptedVersions == null ? Channel.VersionTest.exact(this.networkProtocolVersion) : this.clientAcceptedVersions;
   }

   private Channel.VersionTest getServerAcceptedVersions() {
      return this.serverAcceptedVersions == null ? Channel.VersionTest.exact(this.networkProtocolVersion) : this.serverAcceptedVersions;
   }

   public SimpleChannel simpleChannel() {
      return this.channel(SimpleChannel::new);
   }

   public EventNetworkChannel eventNetworkChannel() {
      return this.channel(EventNetworkChannel::new);
   }

   public PayloadConnection<CustomPacketPayload> payloadChannel() {
      this.registerSelf = false;
      return this.channel(PayloadChannel::builder);
   }

   public <C> C channel(Function<NetworkInstance, C> factory) {
      return factory.apply(this.createNetworkInstance());
   }
}
