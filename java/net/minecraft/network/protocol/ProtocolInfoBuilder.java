package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf> {
   final ConnectionProtocol protocol;
   final PacketFlow flow;
   private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> codecs = new ArrayList<>();
   @Nullable
   private BundlerInfo bundlerInfo;

   public ProtocolInfoBuilder(ConnectionProtocol pProtocol, PacketFlow pFlow) {
      this.protocol = pProtocol;
      this.flow = pFlow;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B> addPacket(PacketType<P> pType, StreamCodec<? super B, P> pSerializer) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(pType, pSerializer));
      return this;
   }

   public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B> withBundlePacket(
      PacketType<P> pType, Function<Iterable<Packet<? super T>>, P> pBundler, D pPacket
   ) {
      StreamCodec<ByteBuf, D> $$3 = StreamCodec.unit(pPacket);
      PacketType<D> $$4 = (PacketType<D>)pPacket.type();
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>($$4, $$3));
      this.bundlerInfo = BundlerInfo.createForPacket(pType, pBundler, pPacket);
      return this;
   }

   StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(Function<ByteBuf, B> pBufferFactory, List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> pCodecs) {
      ProtocolCodecBuilder<ByteBuf, T> $$2 = new ProtocolCodecBuilder<>(this.flow);

      for (ProtocolInfoBuilder.CodecEntry<T, ?, B> $$3 : pCodecs) {
         $$3.addToBuilder($$2, pBufferFactory);
      }

      return $$2.build();
   }

   public ProtocolInfo<T> build(Function<ByteBuf, B> pBufferFactory) {
      return new ProtocolInfoBuilder.Implementation<>(this.protocol, this.flow, this.buildPacketCodec(pBufferFactory, this.codecs), this.bundlerInfo);
   }

   public ProtocolInfo.Unbound<T, B> buildUnbound() {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> $$0 = List.copyOf(this.codecs);
      final BundlerInfo $$1 = this.bundlerInfo;
      return new ProtocolInfo.Unbound<T, B>() {
         @Override
         public ProtocolInfo<T> bind(Function<ByteBuf, B> p_343642_) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol, ProtocolInfoBuilder.this.flow, ProtocolInfoBuilder.this.buildPacketCodec(p_343642_, $$0), $$1
            );
         }

         @Override
         public ConnectionProtocol id() {
            return ProtocolInfoBuilder.this.protocol;
         }

         @Override
         public PacketFlow flow() {
            return ProtocolInfoBuilder.this.flow;
         }

         @Override
         public void listPackets(ProtocolInfo.Unbound.PacketVisitor p_343184_) {
            for (int $$1 = 0; $$1 < $$0.size(); $$1++) {
               ProtocolInfoBuilder.CodecEntry<T, ?, B> $$2 = $$0.get($$1);
               p_343184_.accept($$2.type, $$1);
            }
         }
      };
   }

   private static <L extends PacketListener, B extends ByteBuf> ProtocolInfo.Unbound<L, B> protocol(
      ConnectionProtocol pProtocol, PacketFlow pFlow, Consumer<ProtocolInfoBuilder<L, B>> pSetup
   ) {
      ProtocolInfoBuilder<L, B> $$3 = new ProtocolInfoBuilder<>(pProtocol, pFlow);
      pSetup.accept($$3);
      return $$3.buildUnbound();
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf> ProtocolInfo.Unbound<T, B> serverboundProtocol(
      ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B>> pSetup
   ) {
      return protocol(pProtocol, PacketFlow.SERVERBOUND, pSetup);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf> ProtocolInfo.Unbound<T, B> clientboundProtocol(
      ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B>> pSetup
   ) {
      return protocol(pProtocol, PacketFlow.CLIENTBOUND, pSetup);
   }

   static record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf>(PacketType<P> type, StreamCodec<? super B, P> serializer) {
      public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> pCodecBuilder, Function<ByteBuf, B> pBufferFactory) {
         StreamCodec<ByteBuf, P> $$2 = this.serializer.mapStream(pBufferFactory);
         pCodecBuilder.add(this.type, $$2);
      }
   }

   static record Implementation<L extends PacketListener>(
      ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
   ) implements ProtocolInfo<L> {
   }
}
