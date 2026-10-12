package net.minecraftforge.network.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.config.ConfigurationTaskContext;
import net.minecraftforge.network.config.SimpleConfigurationTask;
import net.minecraftforge.network.packets.ConfigData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
class SyncConfigTask extends SimpleConfigurationTask {
   static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("forge:sync_configs");
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("FORGE_SYNC_CONFIG");

   SyncConfigTask() {
      super(TYPE, SyncConfigTask::run);
   }

   private static void run(ConfigurationTaskContext ctx) {
      for (ModConfig cfg : (Set)ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER)) {
         try {
            ConfigData pkt = new ConfigData(cfg.getFileName(), Files.readAllBytes(cfg.getFullPath()));
            NetworkInitialization.CONFIG.send(pkt, ctx.getConnection());
         } catch (IOException var4) {
            LOGGER.error(MARKER, "Failed to read config file {} terminating connection", cfg.getFileName(), var4);
            ctx.getConnection().disconnect(Component.literal("Connection closed - Failed to read config on server"));
         }
      }
   }
}
