package net.minecraftforge.network.payload;

import io.netty.util.AttributeKey;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.payload.handler.PayloadHandlerProtocol;
import net.minecraftforge.network.simple.handler.SimplePacket;
import org.jetbrains.annotations.Nullable;

public interface PayloadConnection<BASE extends CustomPacketPayload> {
   <NEWBUF extends FriendlyByteBuf, NEWBASE extends CustomPacketPayload> PayloadProtocol<NEWBUF, NEWBASE> protocol(@Nullable NetworkProtocol<NEWBUF> var1);

   <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<NEWBUF, NEWBASE> protocol(
      AttributeKey<CTX> var1, @Nullable NetworkProtocol<NEWBUF> var2
   );

   default <BUF extends FriendlyByteBuf> PayloadConnection<BASE> protocol(
      @Nullable NetworkProtocol<BUF> protocol, Consumer<PayloadProtocol<BUF, BASE>> consumer
   ) {
      PayloadProtocol<BUF, BASE> tmp = this.protocol(protocol);
      consumer.accept(tmp);
      return this;
   }

   default <BUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadConnection<BASE> protocol(
      AttributeKey<CTX> context, @Nullable NetworkProtocol<BUF> protocol, Consumer<PayloadHandlerProtocol<BUF, NEWBASE>> consumer
   ) {
      PayloadHandlerProtocol<BUF, NEWBASE> tmp = this.protocol(context, protocol);
      consumer.accept(tmp);
      return this;
   }

   default PayloadProtocol<FriendlyByteBuf, BASE> configuration() {
      return this.protocol(NetworkProtocol.CONFIGURATION);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE> configuration(
      AttributeKey<CTX> context
   ) {
      return this.protocol(context, NetworkProtocol.CONFIGURATION);
   }

   default PayloadConnection<BASE> configuration(Consumer<PayloadProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.CONFIGURATION, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadConnection<BASE> configuration(
      AttributeKey<CTX> context, Consumer<PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.CONFIGURATION, consumer);
   }

   default PayloadProtocol<FriendlyByteBuf, BASE> login() {
      return this.protocol(NetworkProtocol.LOGIN);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE> login(AttributeKey<CTX> context) {
      return this.protocol(context, NetworkProtocol.LOGIN);
   }

   default PayloadConnection<BASE> login(Consumer<PayloadProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.LOGIN, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadConnection<BASE> login(
      AttributeKey<CTX> context, Consumer<PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.LOGIN, consumer);
   }

   default PayloadProtocol<RegistryFriendlyByteBuf, BASE> play() {
      return this.protocol(NetworkProtocol.PLAY);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<RegistryFriendlyByteBuf, NEWBASE> play(
      AttributeKey<CTX> context
   ) {
      return this.protocol(context, NetworkProtocol.PLAY);
   }

   default PayloadConnection<BASE> play(Consumer<PayloadProtocol<RegistryFriendlyByteBuf, BASE>> consumer) {
      return this.protocol(NetworkProtocol.PLAY, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadConnection<BASE> play(
      AttributeKey<CTX> context, Consumer<PayloadHandlerProtocol<RegistryFriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, NetworkProtocol.PLAY, consumer);
   }

   default PayloadProtocol<FriendlyByteBuf, BASE> any() {
      return this.protocol(null);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE> any(AttributeKey<CTX> context) {
      return this.protocol(context, null);
   }

   default PayloadConnection<BASE> any(Consumer<PayloadProtocol<FriendlyByteBuf, BASE>> consumer) {
      return this.protocol(null, consumer);
   }

   default <CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadConnection<BASE> any(
      AttributeKey<CTX> context, Consumer<PayloadHandlerProtocol<FriendlyByteBuf, NEWBASE>> consumer
   ) {
      return this.protocol(context, null, consumer);
   }
}
