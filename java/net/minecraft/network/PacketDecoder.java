package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketDecoder<T extends PacketListener> extends ByteToMessageDecoder implements ProtocolSwapHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ProtocolInfo<T> protocolInfo;

   public PacketDecoder(ProtocolInfo<T> pProtocolInfo) {
      this.protocolInfo = pProtocolInfo;
   }

   protected void decode(ChannelHandlerContext pContext, ByteBuf pIn, List<Object> pOut) throws Exception {
      int $$3 = pIn.readableBytes();
      if ($$3 != 0) {
         Packet<? super T> $$4 = this.protocolInfo.codec().decode(pIn);
         PacketType<? extends Packet<? super T>> $$5 = $$4.type();
         JvmProfiler.INSTANCE.onPacketReceived(this.protocolInfo.id(), $$5, pContext.channel().remoteAddress(), $$3);
         if (pIn.readableBytes() > 0) {
            throw new IOException(
               "Packet "
                  + this.protocolInfo.id().id()
                  + "/"
                  + $$5
                  + " ("
                  + $$4.getClass().getSimpleName()
                  + ") was larger than I expected, found "
                  + pIn.readableBytes()
                  + " bytes extra whilst reading packet "
                  + $$5
            );
         } else {
            pOut.add($$4);
            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug(
                  Connection.PACKET_RECEIVED_MARKER,
                  " IN: [{}:{}] {} -> {} bytes",
                  new Object[]{this.protocolInfo.id().id(), $$5, $$4.getClass().getName(), $$3}
               );
            }

            ProtocolSwapHandler.handleInboundTerminalPacket(pContext, $$4);
         }
      }
   }
}
