package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraftforge.network.config.ConfigurationTaskContext;

public interface ConfigurationTask {
   default void start(ConfigurationTaskContext ctx) {
      this.start(ctx::send);
   }

   void start(Consumer<Packet<?>> pTask);

   ConfigurationTask.Type type();

   public static record Type(String id) {
      @Override
      public String toString() {
         return this.id;
      }
   }
}
