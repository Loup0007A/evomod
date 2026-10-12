package net.minecraft.util.datafix;

import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;

public class PackedBitStorage {
   private static final int BIT_TO_LONG_SHIFT = 6;
   private final long[] data;
   private final int bits;
   private final long mask;
   private final int size;

   public PackedBitStorage(int pBits, int pSize) {
      this(pBits, pSize, new long[Mth.roundToward(pSize * pBits, 64) / 64]);
   }

   public PackedBitStorage(int pBits, int pSize, long[] pData) {
      Validate.inclusiveBetween(1L, 32L, (long)pBits);
      this.size = pSize;
      this.bits = pBits;
      this.data = pData;
      this.mask = (1L << pBits) - 1L;
      int $$3 = Mth.roundToward(pSize * pBits, 64) / 64;
      if (pData.length != $$3) {
         throw new IllegalArgumentException("Invalid length given for storage, got: " + pData.length + " but expected: " + $$3);
      }
   }

   public void set(int pIndex, int pValue) {
      Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)pIndex);
      Validate.inclusiveBetween(0L, this.mask, (long)pValue);
      int $$2 = pIndex * this.bits;
      int $$3 = $$2 >> 6;
      int $$4 = (pIndex + 1) * this.bits - 1 >> 6;
      int $$5 = $$2 ^ $$3 << 6;
      this.data[$$3] = this.data[$$3] & ~(this.mask << $$5) | ((long)pValue & this.mask) << $$5;
      if ($$3 != $$4) {
         int $$6 = 64 - $$5;
         int $$7 = this.bits - $$6;
         this.data[$$4] = this.data[$$4] >>> $$7 << $$7 | ((long)pValue & this.mask) >> $$6;
      }
   }

   public int get(int pIndex) {
      Validate.inclusiveBetween(0L, (long)(this.size - 1), (long)pIndex);
      int $$1 = pIndex * this.bits;
      int $$2 = $$1 >> 6;
      int $$3 = (pIndex + 1) * this.bits - 1 >> 6;
      int $$4 = $$1 ^ $$2 << 6;
      if ($$2 == $$3) {
         return (int)(this.data[$$2] >>> $$4 & this.mask);
      } else {
         int $$5 = 64 - $$4;
         return (int)((this.data[$$2] >>> $$4 | this.data[$$3] << $$5) & this.mask);
      }
   }

   public long[] getRaw() {
      return this.data;
   }

   public int getBits() {
      return this.bits;
   }
}
