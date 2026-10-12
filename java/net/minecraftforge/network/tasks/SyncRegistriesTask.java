package net.minecraftforge.network.tasks;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ForgePacketHandler;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.config.ConfigurationTaskContext;
import net.minecraftforge.network.packets.Acknowledge;
import net.minecraftforge.network.packets.RegistryData;
import net.minecraftforge.network.packets.RegistryList;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SyncRegistriesTask implements ConfigurationTask {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("SYNC_REGISTRIES_TASK");
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("forge:sync_registries");
   private ConfigurationTaskContext taskCtx;
   private Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot;
   private ForgePacketHandler handler;
   private int expectedToken;

   @Override
   public void start(ConfigurationTaskContext ctx) {
      this.taskCtx = ctx;
      Connection con = ctx.getConnection();
      this.handler = (ForgePacketHandler)con.channel().attr(NetworkInitialization.CONTEXT).get();
      if (!ctx.getConnection().isMemoryConnection()) {
         this.snapshot = RegistryManager.ACTIVE.takeSnapshot(false);
      }

      this.expectedToken = this.handler.expectAck(this::sendRegistries);
      NetworkInitialization.CONFIG.send(new RegistryList(this.expectedToken), con);
   }

   private void sendRegistries(Acknowledge msg, CustomPayloadEvent.Context ctx) {
      if (msg.token() != this.expectedToken) {
         LOGGER.error(MARKER, "Received unknown acknowledgement received {} exptected {}", msg.token(), this.expectedToken);
         ctx.getConnection().disconnect(Component.literal("Illegal Acknowledge packet received, unknown token: " + msg.token()));
      } else if (this.snapshot != null && !this.snapshot.isEmpty()) {
         ResourceLocation name = this.snapshot.keySet().iterator().next();
         ForgeRegistry.Snapshot data = this.snapshot.remove(name);
         this.expectedToken = this.handler.expectAck(this::sendRegistries);
         NetworkInitialization.CONFIG.reply(new RegistryData(this.expectedToken, name, data), ctx);
      } else {
         this.taskCtx.finish(this.type());
      }
   }

   @Override
   public void start(Consumer<Packet<?>> send) {
      throw new IllegalStateException("This should never be called");
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
