package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
   private static final int MAX_VARINT_SIZE = 5;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(int pData) {
      for (int $$1 = 1; $$1 < 5; $$1++) {
         if ((pData & -1 << $$1 * 7) == 0) {
            return $$1;
         }
      }

      return 5;
   }

   public static boolean hasContinuationBit(byte pData) {
      return (pData & 128) == 128;
   }

   public static int read(ByteBuf pBuffer) {
      int $$1 = 0;
      int $$2 = 0;

      byte $$3;
      do {
         $$3 = pBuffer.readByte();
         $$1 |= ($$3 & 127) << $$2++ * 7;
         if ($$2 > 5) {
            throw new RuntimeException("VarInt too big");
         }
      } while (hasContinuationBit($$3));

      return $$1;
   }

   public static ByteBuf write(ByteBuf pBuffer, int pValue) {
      while ((pValue & -128) != 0) {
         pBuffer.writeByte(pValue & 127 | 128);
         pValue >>>= 7;
      }

      pBuffer.writeByte(pValue);
      return pBuffer;
   }
}
