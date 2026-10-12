package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressionDecoder extends ByteToMessageDecoder {
   public static final int MAXIMUM_COMPRESSED_LENGTH = 2097152;
   public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;
   private final Inflater inflater;
   private int threshold;
   private boolean validateDecompressed;

   public CompressionDecoder(int pThreshold, boolean pValidateDecompressed) {
      this.threshold = pThreshold;
      this.validateDecompressed = pValidateDecompressed;
      this.inflater = new Inflater();
   }

   protected void decode(ChannelHandlerContext pContext, ByteBuf pIn, List<Object> pOut) throws Exception {
      if (pIn.readableBytes() != 0) {
         int $$3 = VarInt.read(pIn);
         if ($$3 == 0) {
            pOut.add(pIn.readBytes(pIn.readableBytes()));
         } else {
            if (this.validateDecompressed) {
               if ($$3 < this.threshold) {
                  throw new DecoderException("Badly compressed packet - size of " + $$3 + " is below server threshold of " + this.threshold);
               }

               if ($$3 > 8388608) {
                  throw new DecoderException("Badly compressed packet - size of " + $$3 + " is larger than protocol maximum of 8388608");
               }
            }

            this.setupInflaterInput(pIn);
            ByteBuf $$4 = this.inflate(pContext, $$3);
            this.inflater.reset();
            pOut.add($$4);
         }
      }
   }

   private void setupInflaterInput(ByteBuf pBuffer) {
      ByteBuffer $$1;
      if (pBuffer.nioBufferCount() > 0) {
         $$1 = pBuffer.nioBuffer();
         pBuffer.skipBytes(pBuffer.readableBytes());
      } else {
         $$1 = ByteBuffer.allocateDirect(pBuffer.readableBytes());
         pBuffer.readBytes($$1);
         $$1.flip();
      }

      this.inflater.setInput($$1);
   }

   private ByteBuf inflate(ChannelHandlerContext pContext, int pSize) throws DataFormatException {
      ByteBuf $$2 = pContext.alloc().directBuffer(pSize);

      try {
         ByteBuffer $$3 = $$2.internalNioBuffer(0, pSize);
         int $$4 = $$3.position();
         this.inflater.inflate($$3);
         int $$5 = $$3.position() - $$4;
         if ($$5 != pSize) {
            throw new DecoderException("Badly compressed packet - actual length of uncompressed payload " + $$5 + " is does not match declared size " + pSize);
         } else {
            $$2.writerIndex($$2.writerIndex() + $$5);
            return $$2;
         }
      } catch (Exception var7) {
         $$2.release();
         throw var7;
      }
   }

   public void setThreshold(int pThreshold, boolean pValidateDecompressed) {
      this.threshold = pThreshold;
      this.validateDecompressed = pValidateDecompressed;
   }
}
