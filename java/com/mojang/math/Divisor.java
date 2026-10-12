package com.mojang.math;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

public class Divisor implements IntIterator {
   private final int denominator;
   private final int quotient;
   private final int mod;
   private int returnedParts;
   private int remainder;

   public Divisor(int pNumerator, int pDenominator) {
      this.denominator = pDenominator;
      if (pDenominator > 0) {
         this.quotient = pNumerator / pDenominator;
         this.mod = pNumerator % pDenominator;
      } else {
         this.quotient = 0;
         this.mod = 0;
      }
   }

   public boolean hasNext() {
      return this.returnedParts < this.denominator;
   }

   public int nextInt() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         int $$0 = this.quotient;
         this.remainder = this.remainder + this.mod;
         if (this.remainder >= this.denominator) {
            this.remainder = this.remainder - this.denominator;
            $$0++;
         }

         this.returnedParts++;
         return $$0;
      }
   }

   @VisibleForTesting
   public static Iterable<Integer> asIterable(int pNumerator, int pDenominator) {
      return () -> new Divisor(pNumerator, pDenominator);
   }
}
