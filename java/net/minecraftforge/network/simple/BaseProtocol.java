package net.minecraftforge.network.simple;

import java.util.function.Consumer;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;

public interface BaseProtocol<FLOW, PROTOCOL extends BaseProtocol<FLOW, PROTOCOL>> {
   FLOW flow(@Nullable PacketFlow var1);

   default PROTOCOL flow(@Nullable PacketFlow flow, Consumer<FLOW> consumer) {
      consumer.accept(this.flow(flow));
      return (PROTOCOL)this;
   }

   default FLOW clientbound() {
      return this.flow(PacketFlow.CLIENTBOUND);
   }

   default PROTOCOL clientbound(Consumer<FLOW> consumer) {
      return this.flow(PacketFlow.CLIENTBOUND, consumer);
   }

   default FLOW serverbound() {
      return this.flow(PacketFlow.SERVERBOUND);
   }

   default PROTOCOL serverbound(Consumer<FLOW> consumer) {
      return this.flow(PacketFlow.SERVERBOUND, consumer);
   }

   default FLOW bidirectional() {
      return this.flow((PacketFlow)null);
   }

   default PROTOCOL bidirectional(Consumer<FLOW> consumer) {
      return this.flow(null, consumer);
   }

   default PROTOCOL flow(Consumer<FLOW> consumer) {
      return this.bidirectional(consumer);
   }
}
