package net.minecraftforge.network.tasks;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraftforge.network.ChannelListManager;
import net.minecraftforge.network.config.ConfigurationTaskContext;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
class RegisterChannelsTask implements ConfigurationTask {
   private static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("forge:send_vanilla_channel_list");

   @Override
   public void start(ConfigurationTaskContext ctx) {
      ChannelListManager.addChannels(ctx.getConnection());
      ctx.finish(this.type());
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
