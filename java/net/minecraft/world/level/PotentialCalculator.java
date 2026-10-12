package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;

public class PotentialCalculator {
   private final List<PotentialCalculator.PointCharge> charges = Lists.newArrayList();

   public void addCharge(BlockPos pPos, double pCharge) {
      if (pCharge != 0.0) {
         this.charges.add(new PotentialCalculator.PointCharge(pPos, pCharge));
      }
   }

   public double getPotentialEnergyChange(BlockPos pPos, double pCharge) {
      if (pCharge == 0.0) {
         return 0.0;
      } else {
         double $$2 = 0.0;

         for (PotentialCalculator.PointCharge $$3 : this.charges) {
            $$2 += $$3.getPotentialChange(pPos);
         }

         return $$2 * pCharge;
      }
   }

   static class PointCharge {
      private final BlockPos pos;
      private final double charge;

      public PointCharge(BlockPos pPos, double pCharge) {
         this.pos = pPos;
         this.charge = pCharge;
      }

      public double getPotentialChange(BlockPos pPos) {
         double $$1 = this.pos.distSqr(pPos);
         return $$1 == 0.0 ? Double.POSITIVE_INFINITY : this.charge / Math.sqrt($$1);
      }
   }
}
