package net.minecraftforge.network;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class EventNetworkChannel extends Channel<FriendlyByteBuf> {
   EventNetworkChannel(NetworkInstance instance) {
      super(instance);
   }

   public <T extends CustomPayloadEvent> EventNetworkChannel addListener(Consumer<T> eventListener) {
      this.instance.addListener(eventListener);
      return this;
   }

   public EventNetworkChannel registerObject(Object object) {
      this.instance.registerObject(object);
      return this;
   }

   public void unregisterObject(Object object) {
      this.instance.unregisterObject(object);
   }

   public void encode(FriendlyByteBuf out, FriendlyByteBuf message) {
      out.writeBytes(message.slice());
   }
}
