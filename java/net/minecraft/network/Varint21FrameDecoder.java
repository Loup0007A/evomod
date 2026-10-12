package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import javax.annotation.Nullable;

public class Varint21FrameDecoder extends ByteToMessageDecoder {
   private static final int MAX_VARINT21_BYTES = 3;
   private final ByteBuf helperBuf = Unpooled.directBuffer(3);
   @Nullable
   private final BandwidthDebugMonitor monitor;

   public Varint21FrameDecoder(@Nullable BandwidthDebugMonitor pMonitor) {
      this.monitor = pMonitor;
   }

   protected void handlerRemoved0(ChannelHandlerContext pContext) {
      this.helperBuf.release();
   }

   private static boolean copyVarint(ByteBuf pIn, ByteBuf pOut) {
      for (int $$2 = 0; $$2 < 3; $$2++) {
         if (!pIn.isReadable()) {
            return false;
         }

         byte $$3 = pIn.readByte();
         pOut.writeByte($$3);
         if (!VarInt.hasContinuationBit($$3)) {
            return true;
         }
      }

      throw new CorruptedFrameException("length wider than 21-bit");
   }

   protected void decode(ChannelHandlerContext pContext, ByteBuf pIn, List<Object> pOut) {
      pIn.markReaderIndex();
      this.helperBuf.clear();
      if (!copyVarint(pIn, this.helperBuf)) {
         pIn.resetReaderIndex();
      } else {
         int $$3 = VarInt.read(this.helperBuf);
         if (pIn.readableBytes() < $$3) {
            pIn.resetReaderIndex();
         } else {
            if (this.monitor != null) {
               this.monitor.onReceive($$3 + VarInt.getByteSize($$3));
            }

            pOut.add(pIn.readBytes($$3));
         }
      }
   }
}
