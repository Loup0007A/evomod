package net.minecraftforge.network.simple;

import io.netty.util.AttributeKey;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.simple.handler.SimpleHandlerProtocol;
import net.minecraftforge.network.simple.handler.SimplePacket;

public interface SimpleConnection<BASE> {
   <NEWBUF extends FriendlyByteBuf, NEWBASE> SimpleProtocol<NEWBUF, NEWBASE> protocol(NetworkProtocol<NEWBUF> var1);

   <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<NEWBUF, NEWBASE> protocol(
      AttributeKey<CTX> var1, NetworkProtocol<NEWBUF> var2
   );

   default <BUF extends FriendlyByteBuf> SimpleConnection<BASE> protocol(NetworkProtocol<BUF> protocol, Consumer<SimpleProtocol<BUF, BASE>> consumer) {
      SimpleProtocol<BUF, BASE> tmp = this.protocol(protocol);
      consumer.accept(tmp);
      return this;
   }

   default <BUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX>> SimpleConnection<BASE> protocol(
      AttributeKey<CTX> context, NetworkProtocol<BUF> protocol, Consumer<SimpleHandlerProtocol<BUF, NEWBASE>> consumer
   ) {
      SimpleHandlerProtocol<BUF, NEWBASE> tmp = this.protocol(context, protocol);
      consumer.accept(tmp);
      return this;
   }

   default SimpleProtocol<FriendlyByteBuf, BASE> configuration() {
      return this.protocol(NetworkProtocol.CONFIGURATION);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE> configuration(AttributeKey<CTX> context) {
      return this.protocol(context, NetworkProtocol.CONFIGURATION);
   }

   default SimpleConnection<BASE> configuration(Consumer<SimpleProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.CONFIGURATION, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleConnection<BASE> configuration(
      AttributeKey<CTX> context, Consumer<SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.CONFIGURATION, consumer);
   }

   default SimpleProtocol<FriendlyByteBuf, BASE> login() {
      return this.protocol(NetworkProtocol.LOGIN);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE> login(AttributeKey<CTX> context) {
      return this.protocol(context, NetworkProtocol.LOGIN);
   }

   default SimpleConnection<BASE> login(Consumer<SimpleProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.LOGIN, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleConnection<BASE> login(
      AttributeKey<CTX> context, Consumer<SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.LOGIN, consumer);
   }

   default SimpleProtocol<RegistryFriendlyByteBuf, BASE> play() {
      return this.protocol(NetworkProtocol.PLAY);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<RegistryFriendlyByteBuf, NEWBASE> play(AttributeKey<CTX> context) {
      return this.protocol(context, NetworkProtocol.PLAY);
   }

   default SimpleConnection<BASE> play(Consumer<SimpleProtocol<RegistryFriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.PLAY, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleConnection<BASE> play(
      AttributeKey<CTX> context, Consumer<SimpleHandlerProtocol<RegistryFriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.PLAY, consumer);
   }

   default SimpleProtocol<FriendlyByteBuf, BASE> any() {
      return this.protocol(null);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE> any(AttributeKey<CTX> context) {
      return this.protocol(context, null);
   }

   default SimpleConnection<BASE> any(Consumer<SimpleProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(null, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX>> SimpleConnection<BASE> any(
      AttributeKey<CTX> context, Consumer<SimpleHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, null, consumer);
   }
}
