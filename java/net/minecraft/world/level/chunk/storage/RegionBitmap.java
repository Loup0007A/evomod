package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;

public class RegionBitmap {
   private final BitSet used = new BitSet();

   public void force(int pSectorOffset, int pSectorCount) {
      this.used.set(pSectorOffset, pSectorOffset + pSectorCount);
   }

   public void free(int pSectorOffset, int pSectorCount) {
      this.used.clear(pSectorOffset, pSectorOffset + pSectorCount);
   }

   public int allocate(int pSectorCount) {
      int $$1 = 0;

      while (true) {
         int $$2 = this.used.nextClearBit($$1);
         int $$3 = this.used.nextSetBit($$2);
         if ($$3 == -1 || $$3 - $$2 >= pSectorCount) {
            this.force($$2, pSectorCount);
            return $$2;
         }

         $$1 = $$3;
      }
   }

   @VisibleForTesting
   public IntSet getUsed() {
      return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
   }
}
