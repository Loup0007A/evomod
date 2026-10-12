package net.minecraftforge.network.config;

import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

public class ConfigurationTaskContext {
   private final Connection connection;
   private final Consumer<Packet<?>> send;
   private final Consumer<ConfigurationTask.Type> finish;

   public ConfigurationTaskContext(Connection connection, Consumer<Packet<?>> send, Consumer<ConfigurationTask.Type> finish) {
      this.connection = connection;
      this.send = send;
      this.finish = finish;
   }

   public Connection getConnection() {
      return this.connection;
   }

   public void send(Packet<?> packet) {
      this.send.accept(packet);
   }

   public void finish(ConfigurationTask.Type task) {
      this.finish.accept(task);
   }
}
