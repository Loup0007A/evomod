package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarLong {
   private static final int MAX_VARLONG_SIZE = 10;
   private static final int DATA_BITS_MASK = 127;
   private static final int CONTINUATION_BIT_MASK = 128;
   private static final int DATA_BITS_PER_BYTE = 7;

   public static int getByteSize(long pData) {
      for (int $$1 = 1; $$1 < 10; $$1++) {
         if ((pData & -1L << $$1 * 7) == 0L) {
            return $$1;
         }
      }

      return 10;
   }

   public static boolean hasContinuationBit(byte pData) {
      return (pData & 128) == 128;
   }

   public static long read(ByteBuf pBuffer) {
      long $$1 = 0L;
      int $$2 = 0;

      byte $$3;
      do {
         $$3 = pBuffer.readByte();
         $$1 |= (long)($$3 & 127) << $$2++ * 7;
         if ($$2 > 10) {
            throw new RuntimeException("VarLong too big");
         }
      } while (hasContinuationBit($$3));

      return $$1;
   }

   public static ByteBuf write(ByteBuf pBuffer, long pValue) {
      while ((pValue & -128L) != 0L) {
         pBuffer.writeByte((int)(pValue & 127L) | 128);
         pValue >>>= 7;
      }

      pBuffer.writeByte((int)pValue);
      return pBuffer;
   }
}
