package net.minecraftforge.network.config;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

public class SimpleConfigurationTask implements ConfigurationTask {
   private final ConfigurationTask.Type type;
   private final Consumer<ConfigurationTaskContext> task;

   public SimpleConfigurationTask(ConfigurationTask.Type type, Consumer<ConfigurationTaskContext> task) {
      this.type = type;
      this.task = task;
   }

   public SimpleConfigurationTask(ConfigurationTask.Type type, Runnable task) {
      this(type, c -> task.run());
   }

   @Override
   public void start(ConfigurationTaskContext ctx) {
      this.task.accept(ctx);
      ctx.finish(this.type());
   }

   @Override
   public void start(Consumer<Packet<?>> send) {
      throw new IllegalStateException("This should never be called");
   }

   @Override
   public ConfigurationTask.Type type() {
      return this.type;
   }
}
