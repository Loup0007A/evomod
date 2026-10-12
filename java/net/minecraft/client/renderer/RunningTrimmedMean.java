package net.minecraft.client.renderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RunningTrimmedMean {
   private final long[] values;
   private int count;
   private int cursor;

   public RunningTrimmedMean(int pSize) {
      this.values = new long[pSize];
   }

   public long registerValueAndGetMean(long pValue) {
      if (this.count < this.values.length) {
         this.count++;
      }

      this.values[this.cursor] = pValue;
      this.cursor = (this.cursor + 1) % this.values.length;
      long $$1 = Long.MAX_VALUE;
      long $$2 = Long.MIN_VALUE;
      long $$3 = 0L;

      for (int $$4 = 0; $$4 < this.count; $$4++) {
         long $$5 = this.values[$$4];
         $$3 += $$5;
         $$1 = Math.min($$1, $$5);
         $$2 = Math.max($$2, $$5);
      }

      if (this.count > 2) {
         $$3 -= $$1 + $$2;
         return $$3 / (long)(this.count - 2);
      } else {
         return $$3 > 0L ? (long)this.count / $$3 : 0L;
      }
   }
}
