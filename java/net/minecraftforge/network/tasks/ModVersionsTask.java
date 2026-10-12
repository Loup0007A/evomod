package net.minecraftforge.network.tasks;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.config.ConfigurationTaskContext;
import net.minecraftforge.network.packets.ModVersions;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ModVersionsTask implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("forge:channel_list");

   @Override
   public void start(ConfigurationTaskContext ctx) {
      NetworkInitialization.CONFIG.send(ModVersions.create(), ctx.getConnection());
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
