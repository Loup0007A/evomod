package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
   public static final int MAX_VARINT21_BYTES = 3;

   protected void encode(ChannelHandlerContext pContext, ByteBuf pEncoder, ByteBuf pDecoder) {
      int $$3 = pEncoder.readableBytes();
      int $$4 = VarInt.getByteSize($$3);
      if ($$4 > 3) {
         throw new EncoderException("Packet too large: size " + $$3 + " is over 8");
      } else {
         pDecoder.ensureWritable($$4 + $$3);
         VarInt.write(pDecoder, $$3);
         pDecoder.writeBytes(pEncoder, pEncoder.readerIndex(), $$3);
      }
   }
}
