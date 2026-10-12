package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.nio.charset.StandardCharsets;

public class Utf8String {
   public static String read(ByteBuf pBuffer, int pMaxLength) {
      int $$2 = ByteBufUtil.utf8MaxBytes(pMaxLength);
      int $$3 = VarInt.read(pBuffer);
      if ($$3 > $$2) {
         throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + $$3 + " > " + $$2 + ")");
      } else if ($$3 < 0) {
         throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
      } else {
         int $$4 = pBuffer.readableBytes();
         if ($$3 > $$4) {
            throw new DecoderException("Not enough bytes in buffer, expected " + $$3 + ", but got " + $$4);
         } else {
            String $$5 = pBuffer.toString(pBuffer.readerIndex(), $$3, StandardCharsets.UTF_8);
            pBuffer.readerIndex(pBuffer.readerIndex() + $$3);
            if ($$5.length() > pMaxLength) {
               throw new DecoderException("The received string length is longer than maximum allowed (" + $$5.length() + " > " + pMaxLength + ")");
            } else {
               return $$5;
            }
         }
      }
   }

   public static void write(ByteBuf pBuffer, CharSequence pString, int pMaxLength) {
      if (pString.length() > pMaxLength) {
         throw new EncoderException("String too big (was " + pString.length() + " characters, max " + pMaxLength + ")");
      } else {
         int $$3 = ByteBufUtil.utf8MaxBytes(pString);
         ByteBuf $$4 = pBuffer.alloc().buffer($$3);

         try {
            int $$5 = ByteBufUtil.writeUtf8($$4, pString);
            int $$6 = ByteBufUtil.utf8MaxBytes(pMaxLength);
            if ($$5 > $$6) {
               throw new EncoderException("String too big (was " + $$5 + " bytes encoded, max " + $$6 + ")");
            }

            VarInt.write(pBuffer, $$5);
            pBuffer.writeBytes($$4);
         } finally {
            $$4.release();
         }
      }
   }
}
