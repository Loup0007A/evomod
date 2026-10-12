package net.minecraftforge.network;

import io.netty.util.AttributeKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.payload.PayloadConnection;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.network.payload.PayloadProtocol;
import net.minecraftforge.network.payload.handler.PayloadHandlerFlow;
import net.minecraftforge.network.payload.handler.PayloadHandlerProtocol;
import net.minecraftforge.network.simple.handler.SimplePacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

final class PayloadChannel extends Channel<CustomPacketPayload> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("PAYLOAD_CHANNEL");
   private final Map<ResourceLocation, PayloadChannel.Message<?, ?>> payloads;

   @Internal
   private PayloadChannel(PayloadChannel.Builder builder) {
      super(builder.instance);
      builder.instance.addListener(this::onPacketReceived);
      this.payloads = Collections.unmodifiableMap(builder.payloads);
   }

   static PayloadConnection<CustomPacketPayload> builder(NetworkInstance instance) {
      return new PayloadChannel.Builder(instance);
   }

   private PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf> get(ResourceLocation type, boolean send) {
      PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf> msg = (PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf>)this.payloads.get(type);
      if (msg == null) {
         this.error((send ? "Attemped to send" : "Received") + " invalid message " + type + " on channel " + this.getName());
      }

      return msg;
   }

   private void error(String message) {
      LOGGER.error(MARKER, message);
      throw new IllegalArgumentException(message);
   }

   private void onPacketReceived(CustomPayloadEvent event) {
      FriendlyByteBuf data = event.getPayload();
      if (data == null) {
         this.error("Received empty payload on channel " + this.getName() + " login index " + event.getLoginIndex() + " for payload type " + event.getChannel());
      }

      PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf> msg = this.get(event.getChannel(), false);
      this.validate(event.getChannel(), event.getSource().getConnection(), msg.protocol(), msg.direction(), false);
      CustomPacketPayload pkt = msg.codec().decode(data);
      msg.consumer().accept(pkt, event.getSource());
   }

   protected Packet<?> toVanillaPacket(Connection con, CustomPacketPayload message) {
      PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf> msg = this.get(message.type().id(), true);
      this.validate(message.type().id(), con, msg.protocol(), msg.direction(), true);
      return super.toVanillaPacket(con, message);
   }

   ResourceLocation getName(CustomPacketPayload packet) {
      return packet.type().id();
   }

   public void encode(FriendlyByteBuf out, CustomPacketPayload message) {
      PayloadChannel.Message<CustomPacketPayload, FriendlyByteBuf> msg = this.get(message.type().id(), true);
      msg.codec().encode(out, message);
   }

   private static final class Builder implements PayloadConnection<CustomPacketPayload> {
      private final NetworkInstance instance;
      private boolean built = false;
      private final Map<ResourceLocation, PayloadChannel.Message<?, ?>> payloads = new HashMap<>();

      private Builder(NetworkInstance instance) {
         this.instance = instance;
      }

      @Override
      public <NEWBUF extends FriendlyByteBuf, NEWBASE extends CustomPacketPayload> PayloadProtocol<NEWBUF, NEWBASE> protocol(NetworkProtocol<NEWBUF> protocol) {
         return new PayloadChannel.Builder.Protocol<>(new PayloadChannel.Builder.BuilderContext(this).protocol(protocol));
      }

      @Override
      public <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends CustomPacketPayload & SimplePacket<CTX>> PayloadHandlerProtocol<NEWBUF, NEWBASE> protocol(
         AttributeKey<CTX> context, NetworkProtocol<NEWBUF> protocol
      ) {
         return new PayloadChannel.Builder.HandlerProtocol<>(context, new PayloadChannel.Builder.BuilderContext(this).protocol(protocol));
      }

      private void add(PayloadChannel.Message<?, ?> msg) {
         if (this.payloads.put(msg.type().id(), msg) != null) {
            throw new IllegalArgumentException("Packet with type " + msg.type().id() + " already registered");
         } else {
            this.instance.addChild(msg.type().id());
         }
      }

      private void checkBuilt() {
         if (this.built) {
            throw new IllegalStateException("PayloadChannel builder is fully built, can not modify it any more");
         }
      }

      public PayloadChannel build() {
         this.checkBuilt();
         return new PayloadChannel(this);
      }

      private static record BuilderContext<BUF extends FriendlyByteBuf, BASE>(
         PayloadChannel.Builder builder, @Nullable NetworkProtocol<BUF> protocol, @Nullable PacketFlow flow
      ) {
         public BuilderContext(PayloadChannel.Builder builder) {
            this(builder, null, null);
         }

         public <NEWBUF extends FriendlyByteBuf> PayloadChannel.Builder.BuilderContext<NEWBUF, BASE> protocol(NetworkProtocol<NEWBUF> protocol) {
            return new PayloadChannel.Builder.BuilderContext<>(this.builder, protocol, this.flow);
         }

         private PayloadChannel.Builder.BuilderContext<BUF, BASE> flow(@Nullable PacketFlow flow) {
            return new PayloadChannel.Builder.BuilderContext<>(this.builder, this.protocol, flow);
         }
      }

      private static record Flow<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>(PayloadChannel.Builder.BuilderContext<BUF, BASE> ctx)
         implements PayloadChannel.Builder.ProtocolFactory<BUF, BASE>,
         PayloadFlow<BUF, BASE> {
         public <MSG extends BASE> PayloadChannel.Builder.Flow<BUF, BASE> add(
            CustomPacketPayload.Type<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> consumer
         ) {
            this.ctx.builder().add(new PayloadChannel.Message<>(type, this.ctx.protocol(), this.ctx.flow(), codec, consumer));
            return this;
         }

         public PayloadFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
            return new PayloadChannel.Builder.Flow<>(this.ctx.flow(flow));
         }
      }

      private static record HandlerFlow<BUF extends FriendlyByteBuf, CTX, BASE extends CustomPacketPayload & SimplePacket<CTX>>(
         AttributeKey<CTX> key, PayloadChannel.Builder.BuilderContext<BUF, BASE> ctx
      ) implements PayloadChannel.Builder.ProtocolFactory<BUF, BASE>, PayloadHandlerFlow<BUF, BASE> {
         @Override
         public <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> add(CustomPacketPayload.Type<MSG> type, StreamCodec<BUF, MSG> codec) {
            return this.add(type, codec, (msg, ctx) -> {
               CTX inst = (CTX)ctx.getConnection().channel().attr(this.key).get();
               ctx.setPacketHandled(((SimplePacket)msg).handle(inst, ctx));
            });
         }

         @Override
         public <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> addMain(CustomPacketPayload.Type<MSG> type, StreamCodec<BUF, MSG> codec) {
            return this.add(type, codec, (msg, ctx) -> {
               ctx.enqueueWork(() -> {
                  CTX inst = (CTX)ctx.getConnection().channel().attr(this.key).get();
                  ((SimplePacket)msg).handle(inst, ctx);
               });
               ctx.setPacketHandled(true);
            });
         }

         @Override
         public <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> add(
            CustomPacketPayload.Type<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> consumer
         ) {
            this.ctx.builder().add(new PayloadChannel.Message<>(type, this.ctx.protocol(), this.ctx.flow(), codec, consumer));
            return this;
         }

         public PayloadHandlerFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
            return new PayloadChannel.Builder.HandlerFlow<>(this.key, this.ctx.flow(flow));
         }
      }

      private static record HandlerProtocol<BUF extends FriendlyByteBuf, CTX, BASE extends CustomPacketPayload & SimplePacket<CTX>>(
         AttributeKey<CTX> key, PayloadChannel.Builder.BuilderContext<BUF, BASE> ctx
      ) implements PayloadChannel.Builder.ProtocolFactory<BUF, BASE>, PayloadHandlerProtocol<BUF, BASE> {
         public PayloadHandlerFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
            return new PayloadChannel.Builder.HandlerFlow<>(this.key, this.ctx.flow(flow));
         }
      }

      private static record Protocol<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>(PayloadChannel.Builder.BuilderContext<BUF, BASE> ctx)
         implements PayloadChannel.Builder.ProtocolFactory<BUF, BASE>,
         PayloadProtocol<BUF, BASE> {
         public PayloadFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
            return new PayloadChannel.Builder.Flow<>(this.ctx.flow(flow));
         }
      }

      private interface ProtocolFactory<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>
         extends PayloadConnection<BASE>,
         ChannelBuildable<CustomPacketPayload> {
         PayloadChannel.Builder.BuilderContext<BUF, BASE> ctx();

         @Override
         default <NEWBUF extends FriendlyByteBuf, NEWBASE extends CustomPacketPayload> PayloadProtocol<NEWBUF, NEWBASE> protocol(
            NetworkProtocol<NEWBUF> protocol
         ) {
            return this.ctx().builder.protocol(protocol);
         }

         @Override
         default <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX> & CustomPacketPayload> PayloadHandlerProtocol<NEWBUF, NEWBASE> protocol(
            AttributeKey<CTX> context, NetworkProtocol<NEWBUF> protocol
         ) {
            return this.ctx().builder.protocol(context, protocol);
         }

         @Override
         default Channel<CustomPacketPayload> build() {
            return this.ctx().builder.build();
         }
      }
   }

   private static record Message<MSG extends CustomPacketPayload, BUF extends FriendlyByteBuf>(
      CustomPacketPayload.Type<MSG> type,
      @Nullable NetworkProtocol<BUF> protocol,
      @Nullable PacketFlow direction,
      StreamCodec<BUF, MSG> codec,
      BiConsumer<MSG, CustomPayloadEvent.Context> consumer
   ) {
   }
}
