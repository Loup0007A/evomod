package net.minecraftforge.network;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public record PacketDistributor<T>(BiFunction<PacketDistributor<T>, T, Consumer<Packet<?>>> functor, NetworkDirection<?> direction) {
   public static final PacketDistributor<ServerPlayer> PLAYER = new PacketDistributor<>(PacketDistributor::playerConsumer);
   public static final PacketDistributor<ResourceKey<Level>> DIMENSION = new PacketDistributor<>(PacketDistributor::playerListDimConsumer);
   public static final PacketDistributor<PacketDistributor.TargetPoint> NEAR = new PacketDistributor<>(PacketDistributor::playerListPointConsumer);
   public static final PacketDistributor<Void> ALL = new PacketDistributor<>(PacketDistributor::playerListAll);
   public static final PacketDistributor<Void> SERVER = new PacketDistributor<>(PacketDistributor::clientToServer, NetworkDirection.PLAY_TO_SERVER);
   public static final PacketDistributor<Entity> TRACKING_ENTITY = new PacketDistributor<>(PacketDistributor::trackingEntity);
   public static final PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF = new PacketDistributor<>(PacketDistributor::trackingEntityAndSelf);
   public static final PacketDistributor<LevelChunk> TRACKING_CHUNK = new PacketDistributor<>(PacketDistributor::trackingChunk);
   public static final PacketDistributor<List<Connection>> NMLIST = new PacketDistributor<>(PacketDistributor::networkManagerList);

   public PacketDistributor(BiFunction<PacketDistributor<T>, T, Consumer<Packet<?>>> functor) {
      this(functor, NetworkDirection.PLAY_TO_CLIENT);
   }

   public PacketDistributor.PacketTarget with(T input) {
      return new PacketDistributor.PacketTarget(this.functor.apply(this, input), this.direction());
   }

   public PacketDistributor.PacketTarget noArg() {
      return new PacketDistributor.PacketTarget(this.functor.apply(this, null), this.direction());
   }

   private Consumer<Packet<?>> playerConsumer(ServerPlayer player) {
      return p -> player.connection.getConnection().send(p);
   }

   private Consumer<Packet<?>> playerListDimConsumer(ResourceKey<Level> dimension) {
      return p -> this.getServer().getPlayerList().broadcastAll(p, dimension);
   }

   private Consumer<Packet<?>> playerListAll(Void ignored) {
      return p -> this.getServer().getPlayerList().broadcastAll(p);
   }

   private Consumer<Packet<?>> clientToServer(Void ignored) {
      return p -> Minecraft.getInstance().getConnection().send(p);
   }

   private Consumer<Packet<?>> playerListPointConsumer(PacketDistributor.TargetPoint tp) {
      return p -> this.getServer().getPlayerList().broadcast(tp.excluded, tp.x, tp.y, tp.z, tp.r2, tp.dim, p);
   }

   private Consumer<Packet<?>> trackingEntity(Entity entity) {
      return p -> ((ServerChunkCache)entity.getCommandSenderWorld().getChunkSource()).broadcast(entity, p);
   }

   private Consumer<Packet<?>> trackingEntityAndSelf(Entity entity) {
      return p -> ((ServerChunkCache)entity.getCommandSenderWorld().getChunkSource()).broadcastAndSend(entity, p);
   }

   private Consumer<Packet<?>> trackingChunk(LevelChunk chunk) {
      return p -> ((ServerChunkCache)chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false).forEach(e -> e.connection.send(p));
   }

   private Consumer<Packet<?>> networkManagerList(List<Connection> connections) {
      return p -> connections.forEach(nm -> nm.send(p));
   }

   private MinecraftServer getServer() {
      return ServerLifecycleHooks.getCurrentServer();
   }

   public static record PacketTarget(Consumer<Packet<?>> packetConsumer, NetworkDirection<?> direction) {
      public void send(Packet<?> packet) {
         this.packetConsumer.accept(packet);
      }
   }

   public static record TargetPoint(@Nullable ServerPlayer excluded, double x, double y, double z, double r2, ResourceKey<Level> dim) {
      public TargetPoint(double x, double y, double z, double r2, ResourceKey<Level> dim) {
         this(null, x, y, z, r2, dim);
      }
   }
}
