package net.minecraft.world.phys.shapes;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class DiscreteCubeMerger implements IndexMerger {
   private final CubePointRange result;
   private final int firstDiv;
   private final int secondDiv;

   DiscreteCubeMerger(int pAa, int pBb) {
      this.result = new CubePointRange((int)Shapes.lcm(pAa, pBb));
      int $$2 = IntMath.gcd(pAa, pBb);
      this.firstDiv = pAa / $$2;
      this.secondDiv = pBb / $$2;
   }

   @Override
   public boolean forMergedIndexes(IndexMerger.IndexConsumer pConsumer) {
      int $$1 = this.result.size() - 1;

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         if (!pConsumer.merge($$2 / this.secondDiv, $$2 / this.firstDiv, $$2)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int size() {
      return this.result.size();
   }

   @Override
   public DoubleList getList() {
      return this.result;
   }
}
