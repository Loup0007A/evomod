package net.minecraftforge.network;

import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.simple.SimpleBuildable;
import net.minecraftforge.network.simple.SimpleConnection;
import net.minecraftforge.network.simple.SimpleFlow;
import net.minecraftforge.network.simple.SimpleProtocol;
import net.minecraftforge.network.simple.handler.SimpleHandlerFlow;
import net.minecraftforge.network.simple.handler.SimpleHandlerProtocol;
import net.minecraftforge.network.simple.handler.SimplePacket;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class SimpleChannel extends Channel<Object> implements SimpleConnection<Object> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER = MarkerManager.getMarker("SIMPLE_CHANNEL");
   private boolean built = false;
   protected int lastIndex = 0;
   protected Int2ObjectMap<SimpleChannel.Message<?, ?>> byId = new Int2ObjectArrayMap();
   protected Object2ObjectMap<Class<?>, SimpleChannel.Message<?, ?>> byType = new Object2ObjectArrayMap();

   @Internal
   SimpleChannel(NetworkInstance instance) {
      super(instance);
      instance.addListener(this::networkEventListener);
   }

   @Override
   public <NEWBUF extends FriendlyByteBuf, NEWBASE> SimpleProtocol<NEWBUF, NEWBASE> protocol(NetworkProtocol<NEWBUF> protocol) {
      return new SimpleChannel.Protocol<>(new SimpleChannel.SimpleContext(this).protocol(protocol));
   }

   @Override
   public <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<NEWBUF, NEWBASE> protocol(
      AttributeKey<CTX> context, NetworkProtocol<NEWBUF> protocol
   ) {
      return new SimpleChannel.HandlerProtocol<>(context, new SimpleChannel.SimpleContext(this).protocol(protocol));
   }

   public <M> SimpleChannel.MessageBuilder<M, FriendlyByteBuf> messageBuilder(Class<M> type) {
      return this.messageBuilder(type, this.nextIndex());
   }

   public <M> SimpleChannel.MessageBuilder<M, FriendlyByteBuf> messageBuilder(Class<M> type, int discriminator) {
      return this.messageBuilder(type, discriminator, (NetworkProtocol<FriendlyByteBuf>)null);
   }

   public <M, B extends FriendlyByteBuf> SimpleChannel.MessageBuilder<M, B> messageBuilder(Class<M> type, NetworkProtocol<B> protocol) {
      return this.messageBuilder(type, this.nextIndex(), protocol);
   }

   public <M, B extends FriendlyByteBuf> SimpleChannel.MessageBuilder<M, B> messageBuilder(Class<M> type, NetworkDirection<B> direction) {
      return this.messageBuilder(type, this.nextIndex(), direction.protocol()).direction(direction.direction());
   }

   public <M, B extends FriendlyByteBuf> SimpleChannel.MessageBuilder<M, B> messageBuilder(Class<M> type, int discriminator, NetworkProtocol<B> protocol) {
      this.checkBuilt();
      return new SimpleChannel.MessageBuilder<>(this, type, discriminator, protocol);
   }

   public <M, B extends FriendlyByteBuf> SimpleChannel.MessageBuilder<M, B> messageBuilder(Class<M> type, int discriminator, NetworkDirection<B> direction) {
      return this.messageBuilder(type, discriminator, direction.protocol()).direction(direction.direction());
   }

   public SimpleChannel build() {
      this.checkBuilt();
      this.built = true;
      return this;
   }

   protected void checkBuilt() {
      if (this.built) {
         throw new IllegalStateException("SimpleChannel builder is fully built, can not modify it any more");
      }
   }

   private SimpleChannel.Message<Object, FriendlyByteBuf> get(int id) {
      SimpleChannel.Message<Object, FriendlyByteBuf> msg = (SimpleChannel.Message<Object, FriendlyByteBuf>)this.byId.get(id);
      if (msg == null) {
         this.error("Received invalid discriminator " + id + " on channel " + this.getName());
      }

      return msg;
   }

   private SimpleChannel.Message<Object, FriendlyByteBuf> get(Object type) {
      SimpleChannel.Message<Object, FriendlyByteBuf> msg = (SimpleChannel.Message<Object, FriendlyByteBuf>)this.byType.get(type.getClass());
      if (msg == null) {
         this.error("Attemped to send invalid message " + type + " on channel " + this.getName());
      }

      return msg;
   }

   protected int nextIndex() {
      while (this.byId.containsKey(this.lastIndex)) {
         this.lastIndex++;
         if (this.lastIndex < 0) {
            throw new IllegalStateException("Ran out of discriminator, only 2147483647 are allowed");
         }
      }

      return this.lastIndex;
   }

   private void networkEventListener(CustomPayloadEvent event) {
      FriendlyByteBuf data = event.getPayload();
      if (data != null && data.isReadable()) {
         SimpleChannel.Message<Object, FriendlyByteBuf> msg = this.get(data.readVarInt());
         super.validate(msg.type.getName(), event.getSource().getConnection(), msg.protocol(), msg.direction(), false);
         Object pkt = msg.decoder().apply(data);
         msg.consumer().accept(pkt, event.getSource());
      } else {
         LOGGER.error(MARKER, "Received empty payload on channel {} login index {}", this.getName(), event.getLoginIndex());
      }
   }

   @Override
   protected Packet<?> toVanillaPacket(Connection con, Object message) {
      SimpleChannel.Message<Object, FriendlyByteBuf> msg = this.get(message);
      super.validate(message.getClass().getName(), con, msg.protocol(), msg.direction(), true);
      return super.toVanillaPacket(con, message);
   }

   @Override
   public void encode(FriendlyByteBuf out, Object message) {
      SimpleChannel.Message<Object, FriendlyByteBuf> msg = this.get(message);
      out.writeVarInt(msg.index());
      if (msg.encoder() != null) {
         msg.encoder().accept(message, out);
      }
   }

   private void error(String message) {
      LOGGER.error(MARKER, message);
      throw new IllegalArgumentException(message);
   }

   private static record Flow<BUF extends FriendlyByteBuf, BASE>(SimpleChannel.SimpleContext<BUF, BASE> ctx)
      implements SimpleChannel.ProtocolFactory<BUF, BASE>,
      SimpleFlow<BUF, BASE> {
      public <MSG extends BASE> SimpleChannel.Flow<BUF, BASE> add(
         Class<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> handler
      ) {
         this.ctx.channel.messageBuilder(type, this.ctx.protocol).codec(codec).consumer(handler).direction(this.ctx.flow()).add();
         return this;
      }

      public SimpleFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
         return new SimpleChannel.Flow<>(this.ctx.flow(flow));
      }
   }

   private static record HandlerFlow<BUF extends FriendlyByteBuf, CTX, BASE extends SimplePacket<CTX>>(
      AttributeKey<CTX> key, SimpleChannel.SimpleContext<BUF, BASE> ctx
   ) implements SimpleChannel.ProtocolFactory<BUF, BASE>, SimpleHandlerFlow<BUF, BASE> {
      @Override
      public <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> add(Class<MSG> type, StreamCodec<BUF, MSG> codec) {
         return this.add(type, codec, (msg, ctx) -> {
            CTX inst = (CTX)ctx.getConnection().channel().attr(this.key).get();
            ctx.setPacketHandled(msg.handle(inst, ctx));
         });
      }

      @Override
      public <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> addMain(Class<MSG> type, StreamCodec<BUF, MSG> codec) {
         return this.add(type, codec, (msg, ctx) -> {
            ctx.enqueueWork(() -> {
               CTX inst = (CTX)ctx.getConnection().channel().attr(this.key).get();
               msg.handle(inst, ctx);
            });
            ctx.setPacketHandled(true);
         });
      }

      @Override
      public <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> add(
         Class<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> handler
      ) {
         this.ctx.channel.messageBuilder(type, this.ctx.protocol).codec(codec).consumer(handler).direction(this.ctx.flow()).add();
         return this;
      }

      public SimpleHandlerFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
         return new SimpleChannel.HandlerFlow<>(this.key, this.ctx.flow(flow));
      }
   }

   private static record HandlerProtocol<BUF extends FriendlyByteBuf, CTX, BASE extends SimplePacket<CTX>>(
      AttributeKey<CTX> key, SimpleChannel.SimpleContext<BUF, BASE> ctx
   ) implements SimpleChannel.ProtocolFactory<BUF, BASE>, SimpleHandlerProtocol<BUF, BASE> {
      public SimpleHandlerFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
         return new SimpleChannel.HandlerFlow<>(this.key, this.ctx.flow(flow));
      }
   }

   protected static record Message<MSG, BUF extends FriendlyByteBuf>(
      int index,
      Class<MSG> type,
      NetworkProtocol<BUF> protocol,
      PacketFlow direction,
      BiConsumer<MSG, BUF> encoder,
      Function<BUF, MSG> decoder,
      BiConsumer<MSG, CustomPayloadEvent.Context> consumer
   ) {
   }

   public static class MessageBuilder<MSG, BUF extends FriendlyByteBuf> {
      private final SimpleChannel channel;
      private final Class<MSG> type;
      private final int id;
      private final NetworkProtocol<BUF> protocol;
      private BiConsumer<MSG, BUF> encoder;
      private Function<BUF, MSG> decoder;
      private BiConsumer<MSG, CustomPayloadEvent.Context> consumer;
      private PacketFlow direction;

      private MessageBuilder(SimpleChannel channel, Class<MSG> type, int id, @Nullable NetworkProtocol<BUF> protocol) {
         this.channel = channel;
         this.type = type;
         this.id = id;
         this.protocol = protocol;
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> direction(PacketFlow direction) {
         this.direction = direction;
         return this;
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> encoder(BiConsumer<MSG, BUF> encoder) {
         this.encoder = encoder;
         return this;
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> decoder(Function<BUF, MSG> decoder) {
         this.decoder = decoder;
         return this;
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> codec(StreamCodec<BUF, MSG> codec) {
         return this.encoder((msg, buf) -> codec.encode(buf, msg)).decoder(codec::decode);
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> consumer(BiConsumer<MSG, CustomPayloadEvent.Context> consumer) {
         this.consumer = consumer;
         return this;
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> consumerNetworkThread(BiConsumer<MSG, CustomPayloadEvent.Context> consumer) {
         return this.consumer(consumer);
      }

      public <C> SimpleChannel.MessageBuilder<MSG, BUF> consumerNetworkThread(AttributeKey<C> key, TriConsumer<C, MSG, CustomPayloadEvent.Context> consumer) {
         return this.consumerNetworkThread((msg, ctx) -> {
            C inst = (C)ctx.getConnection().channel().attr(key).get();
            consumer.accept(inst, msg, ctx);
         });
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> consumerNetworkThread(
         SimpleChannel.MessageBuilder.ToBooleanBiFunction<MSG, CustomPayloadEvent.Context> handler
      ) {
         this.consumer = (msg, ctx) -> ctx.setPacketHandled(handler.applyAsBool(msg, ctx));
         return this;
      }

      public <C> SimpleChannel.MessageBuilder<MSG, BUF> consumerNetworkThread(
         AttributeKey<C> key, SimpleChannel.MessageBuilder.ToBooleanTriFunction<C, MSG, CustomPayloadEvent.Context> handler
      ) {
         return this.consumerNetworkThread((msg, ctx) -> {
            C inst = (C)ctx.getConnection().channel().attr(key).get();
            return handler.applyAsBool(inst, msg, ctx);
         });
      }

      public SimpleChannel.MessageBuilder<MSG, BUF> consumerMainThread(BiConsumer<MSG, CustomPayloadEvent.Context> consumer) {
         this.consumer = (msg, context) -> {
            context.enqueueWork(() -> consumer.accept(msg, context));
            context.setPacketHandled(true);
         };
         return this;
      }

      public <C> SimpleChannel.MessageBuilder<MSG, BUF> consumerMainThread(AttributeKey<C> key, TriConsumer<C, MSG, CustomPayloadEvent.Context> consumer) {
         this.consumer = (msg, ctx) -> {
            ctx.enqueueWork(() -> {
               C inst = (C)ctx.getConnection().channel().attr(key).get();
               consumer.accept(inst, msg, ctx);
            });
            ctx.setPacketHandled(true);
         };
         return this;
      }

      public SimpleChannel add() {
         this.channel.checkBuilt();
         if (this.id < 0) {
            throw new IllegalStateException("Failed to register SimpleChannel message, Invalid ID " + this.id + ": " + this.type.getName());
         } else {
            SimpleChannel.Message<MSG, BUF> msg = new SimpleChannel.Message<>(
               this.id, this.type, this.protocol, this.direction, this.encoder, this.decoder, this.consumer
            );
            if (this.channel.byId.containsKey(msg.index())) {
               throw new IllegalStateException("Failed to register SimpleChannel message, ID " + msg.index() + " already claimed: " + msg.type().getName());
            } else if (this.channel.byType.containsKey(msg.type())) {
               throw new IllegalStateException("Failed to register SimpleChannel message, Class " + msg.type().getName() + " already registered");
            } else {
               this.channel.byId.put(msg.index(), msg);
               this.channel.byType.put(msg.type(), msg);
               return this.channel;
            }
         }
      }

      @FunctionalInterface
      public interface ToBooleanBiFunction<T, U> {
         boolean applyAsBool(T var1, U var2);
      }

      @FunctionalInterface
      public interface ToBooleanTriFunction<T, U, V> {
         boolean applyAsBool(T var1, U var2, V var3);
      }
   }

   private static record Protocol<BUF extends FriendlyByteBuf, BASE>(SimpleChannel.SimpleContext<BUF, BASE> ctx)
      implements SimpleChannel.ProtocolFactory<BUF, BASE>,
      SimpleProtocol<BUF, BASE> {
      public SimpleFlow<BUF, BASE> flow(@Nullable PacketFlow flow) {
         return new SimpleChannel.Flow<>(this.ctx.flow(flow));
      }
   }

   private interface ProtocolFactory<BUF extends FriendlyByteBuf, BASE> extends SimpleConnection<BASE>, SimpleBuildable {
      SimpleChannel.SimpleContext<BUF, BASE> ctx();

      @Override
      default <NEWBUF extends FriendlyByteBuf, NEWBASE> SimpleProtocol<NEWBUF, NEWBASE> protocol(NetworkProtocol<NEWBUF> protocol) {
         return this.ctx().channel.protocol(protocol);
      }

      @Override
      default <NEWBUF extends FriendlyByteBuf, CTX, NEWBASE extends SimplePacket<CTX>> SimpleHandlerProtocol<NEWBUF, NEWBASE> protocol(
         AttributeKey<CTX> context, NetworkProtocol<NEWBUF> protocol
      ) {
         return this.ctx().channel.protocol(context, protocol);
      }

      @Override
      default SimpleChannel build() {
         return this.ctx().channel.build();
      }
   }

   private static record SimpleContext<BUF extends FriendlyByteBuf, BASE>(
      SimpleChannel channel, @Nullable NetworkProtocol<BUF> protocol, @Nullable PacketFlow flow
   ) {
      public SimpleContext(SimpleChannel channel) {
         this(channel, null, null);
      }

      public <NEWBUF extends FriendlyByteBuf> SimpleChannel.SimpleContext<NEWBUF, BASE> protocol(NetworkProtocol<NEWBUF> protocol) {
         return new SimpleChannel.SimpleContext<>(this.channel, protocol, this.flow);
      }

      private SimpleChannel.SimpleContext<BUF, BASE> flow(@Nullable PacketFlow flow) {
         return new SimpleChannel.SimpleContext<>(this.channel, this.protocol, flow);
      }
   }
}
