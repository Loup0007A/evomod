package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.NoSuchElementException;
import net.minecraft.util.Mth;

public class SpatialLongSet extends LongLinkedOpenHashSet {
   private final SpatialLongSet.InternalMap map;

   public SpatialLongSet(int pExpectedSize, float pLoadFactor) {
      super(pExpectedSize, pLoadFactor);
      this.map = new SpatialLongSet.InternalMap(pExpectedSize / 64, pLoadFactor);
   }

   public boolean add(long pValue) {
      return this.map.addBit(pValue);
   }

   public boolean rem(long pValue) {
      return this.map.removeBit(pValue);
   }

   public long removeFirstLong() {
      return this.map.removeFirstBit();
   }

   public int size() {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   protected static class InternalMap extends Long2LongLinkedOpenHashMap {
      private static final int X_BITS = Mth.log2(60000000);
      private static final int Z_BITS = Mth.log2(60000000);
      private static final int Y_BITS = 64 - X_BITS - Z_BITS;
      private static final int Y_OFFSET = 0;
      private static final int Z_OFFSET = Y_BITS;
      private static final int X_OFFSET = Y_BITS + Z_BITS;
      private static final long OUTER_MASK = 3L << X_OFFSET | 3L | 3L << Z_OFFSET;
      private int lastPos = -1;
      private long lastOuterKey;
      private final int minSize;

      public InternalMap(int pMinSize, float pLoadFactor) {
         super(pMinSize, pLoadFactor);
         this.minSize = pMinSize;
      }

      static long getOuterKey(long pValue) {
         return pValue & ~OUTER_MASK;
      }

      static int getInnerKey(long pValue) {
         int $$1 = (int)(pValue >>> X_OFFSET & 3L);
         int $$2 = (int)(pValue >>> 0 & 3L);
         int $$3 = (int)(pValue >>> Z_OFFSET & 3L);
         return $$1 << 4 | $$3 << 2 | $$2;
      }

      static long getFullKey(long pValue, int pTrailingZeros) {
         pValue |= (long)(pTrailingZeros >>> 4 & 3) << X_OFFSET;
         pValue |= (long)(pTrailingZeros >>> 2 & 3) << Z_OFFSET;
         return pValue | (long)(pTrailingZeros >>> 0 & 3) << 0;
      }

      public boolean addBit(long pValue) {
         long $$1 = getOuterKey(pValue);
         int $$2 = getInnerKey(pValue);
         long $$3 = 1L << $$2;
         int $$4;
         if ($$1 == 0L) {
            if (this.containsNullKey) {
               return this.replaceBit(this.n, $$3);
            }

            this.containsNullKey = true;
            $$4 = this.n;
         } else {
            if (this.lastPos != -1 && $$1 == this.lastOuterKey) {
               return this.replaceBit(this.lastPos, $$3);
            }

            long[] $$5 = this.key;
            $$4 = (int)HashCommon.mix($$1) & this.mask;

            for (long $$7 = $$5[$$4]; $$7 != 0L; $$7 = $$5[$$4]) {
               if ($$7 == $$1) {
                  this.lastPos = $$4;
                  this.lastOuterKey = $$1;
                  return this.replaceBit($$4, $$3);
               }

               $$4 = $$4 + 1 & this.mask;
            }
         }

         this.key[$$4] = $$1;
         this.value[$$4] = $$3;
         if (this.size == 0) {
            this.first = this.last = $$4;
            this.link[$$4] = -1L;
         } else {
            this.link[this.last] = this.link[this.last] ^ (this.link[this.last] ^ (long)$$4 & 4294967295L) & 4294967295L;
            this.link[$$4] = ((long)this.last & 4294967295L) << 32 | 4294967295L;
            this.last = $$4;
         }

         if (this.size++ >= this.maxFill) {
            this.rehash(HashCommon.arraySize(this.size + 1, this.f));
         }

         return false;
      }

      private boolean replaceBit(int pIndex, long pValue) {
         boolean $$2 = (this.value[pIndex] & pValue) != 0L;
         this.value[pIndex] = this.value[pIndex] | pValue;
         return $$2;
      }

      public boolean removeBit(long pValue) {
         long $$1 = getOuterKey(pValue);
         int $$2 = getInnerKey(pValue);
         long $$3 = 1L << $$2;
         if ($$1 == 0L) {
            return this.containsNullKey ? this.removeFromNullEntry($$3) : false;
         } else if (this.lastPos != -1 && $$1 == this.lastOuterKey) {
            return this.removeFromEntry(this.lastPos, $$3);
         } else {
            long[] $$4 = this.key;
            int $$5 = (int)HashCommon.mix($$1) & this.mask;

            for (long $$6 = $$4[$$5]; $$6 != 0L; $$6 = $$4[$$5]) {
               if ($$1 == $$6) {
                  this.lastPos = $$5;
                  this.lastOuterKey = $$1;
                  return this.removeFromEntry($$5, $$3);
               }

               $$5 = $$5 + 1 & this.mask;
            }

            return false;
         }
      }

      private boolean removeFromNullEntry(long pValue) {
         if ((this.value[this.n] & pValue) == 0L) {
            return false;
         } else {
            this.value[this.n] = this.value[this.n] & ~pValue;
            if (this.value[this.n] != 0L) {
               return true;
            } else {
               this.containsNullKey = false;
               this.size--;
               this.fixPointers(this.n);
               if (this.size < this.maxFill / 4 && this.n > 16) {
                  this.rehash(this.n / 2);
               }

               return true;
            }
         }
      }

      private boolean removeFromEntry(int pIndex, long pValue) {
         if ((this.value[pIndex] & pValue) == 0L) {
            return false;
         } else {
            this.value[pIndex] = this.value[pIndex] & ~pValue;
            if (this.value[pIndex] != 0L) {
               return true;
            } else {
               this.lastPos = -1;
               this.size--;
               this.fixPointers(pIndex);
               this.shiftKeys(pIndex);
               if (this.size < this.maxFill / 4 && this.n > 16) {
                  this.rehash(this.n / 2);
               }

               return true;
            }
         }
      }

      public long removeFirstBit() {
         if (this.size == 0) {
            throw new NoSuchElementException();
         } else {
            int $$0 = this.first;
            long $$1 = this.key[$$0];
            int $$2 = Long.numberOfTrailingZeros(this.value[$$0]);
            this.value[$$0] = this.value[$$0] & ~(1L << $$2);
            if (this.value[$$0] == 0L) {
               this.removeFirstLong();
               this.lastPos = -1;
            }

            return getFullKey($$1, $$2);
         }
      }

      protected void rehash(int pNewSize) {
         if (pNewSize > this.minSize) {
            super.rehash(pNewSize);
         }
      }
   }
}
