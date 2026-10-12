package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class NonOverlappingMerger extends AbstractDoubleList implements IndexMerger {
   private final DoubleList lower;
   private final DoubleList upper;
   private final boolean swap;

   protected NonOverlappingMerger(DoubleList pLower, DoubleList pUpper, boolean pSwap) {
      this.lower = pLower;
      this.upper = pUpper;
      this.swap = pSwap;
   }

   @Override
   public int size() {
      return this.lower.size() + this.upper.size();
   }

   @Override
   public boolean forMergedIndexes(IndexMerger.IndexConsumer pConsumer) {
      return this.swap
         ? this.forNonSwappedIndexes((p_83020_, p_83021_, p_83022_) -> pConsumer.merge(p_83021_, p_83020_, p_83022_))
         : this.forNonSwappedIndexes(pConsumer);
   }

   private boolean forNonSwappedIndexes(IndexMerger.IndexConsumer pConsumer) {
      int $$1 = this.lower.size();

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         if (!pConsumer.merge($$2, -1, $$2)) {
            return false;
         }
      }

      int $$3 = this.upper.size() - 1;

      for (int $$4 = 0; $$4 < $$3; $$4++) {
         if (!pConsumer.merge($$1 - 1, $$4, $$1 + $$4)) {
            return false;
         }
      }

      return true;
   }

   public double getDouble(int pIndex) {
      return pIndex < this.lower.size() ? this.lower.getDouble(pIndex) : this.upper.getDouble(pIndex - this.lower.size());
   }

   @Override
   public DoubleList getList() {
      return this;
   }
}
