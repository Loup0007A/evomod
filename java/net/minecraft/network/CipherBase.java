package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class CipherBase {
   private final Cipher cipher;
   private byte[] heapIn = new byte[0];
   private byte[] heapOut = new byte[0];

   protected CipherBase(Cipher pCipher) {
      this.cipher = pCipher;
   }

   private byte[] bufToByte(ByteBuf pBuffer) {
      int $$1 = pBuffer.readableBytes();
      if (this.heapIn.length < $$1) {
         this.heapIn = new byte[$$1];
      }

      pBuffer.readBytes(this.heapIn, 0, $$1);
      return this.heapIn;
   }

   protected ByteBuf decipher(ChannelHandlerContext pCtx, ByteBuf pBuffer) throws ShortBufferException {
      int $$2 = pBuffer.readableBytes();
      byte[] $$3 = this.bufToByte(pBuffer);
      ByteBuf $$4 = pCtx.alloc().heapBuffer(this.cipher.getOutputSize($$2));
      $$4.writerIndex(this.cipher.update($$3, 0, $$2, $$4.array(), $$4.arrayOffset()));
      return $$4;
   }

   protected void encipher(ByteBuf pInput, ByteBuf pOut) throws ShortBufferException {
      int $$2 = pInput.readableBytes();
      byte[] $$3 = this.bufToByte(pInput);
      int $$4 = this.cipher.getOutputSize($$2);
      if (this.heapOut.length < $$4) {
         this.heapOut = new byte[$$4];
      }

      pOut.writeBytes(this.heapOut, 0, this.cipher.update($$3, 0, $$2, this.heapOut));
   }
}
