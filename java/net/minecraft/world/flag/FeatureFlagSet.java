package net.minecraft.world.flag;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nullable;

public final class FeatureFlagSet {
   private static final FeatureFlagSet EMPTY = new FeatureFlagSet(null, 0L);
   public static final int MAX_CONTAINER_SIZE = 64;
   @Nullable
   private final FeatureFlagUniverse universe;
   private final long mask;

   private FeatureFlagSet(@Nullable FeatureFlagUniverse pUniverse, long pMask) {
      this.universe = pUniverse;
      this.mask = pMask;
   }

   static FeatureFlagSet create(FeatureFlagUniverse pUniverse, Collection<FeatureFlag> pFlags) {
      if (pFlags.isEmpty()) {
         return EMPTY;
      } else {
         long $$2 = computeMask(pUniverse, 0L, pFlags);
         return new FeatureFlagSet(pUniverse, $$2);
      }
   }

   public static FeatureFlagSet of() {
      return EMPTY;
   }

   public static FeatureFlagSet of(FeatureFlag pFlag) {
      return new FeatureFlagSet(pFlag.universe, pFlag.mask);
   }

   public static FeatureFlagSet of(FeatureFlag pFlag, FeatureFlag... pOthers) {
      long $$2 = pOthers.length == 0 ? pFlag.mask : computeMask(pFlag.universe, pFlag.mask, Arrays.asList(pOthers));
      return new FeatureFlagSet(pFlag.universe, $$2);
   }

   private static long computeMask(FeatureFlagUniverse pUniverse, long pMask, Iterable<FeatureFlag> pFlags) {
      for (FeatureFlag $$3 : pFlags) {
         if (pUniverse != $$3.universe) {
            throw new IllegalStateException("Mismatched feature universe, expected '" + pUniverse + "', but got '" + $$3.universe + "'");
         }

         pMask |= $$3.mask;
      }

      return pMask;
   }

   public boolean contains(FeatureFlag pFlag) {
      return this.universe != pFlag.universe ? false : (this.mask & pFlag.mask) != 0L;
   }

   public boolean isEmpty() {
      return this.equals(EMPTY);
   }

   public boolean isSubsetOf(FeatureFlagSet pSet) {
      if (this.universe == null) {
         return true;
      } else {
         return this.universe != pSet.universe ? false : (this.mask & ~pSet.mask) == 0L;
      }
   }

   public boolean intersects(FeatureFlagSet pSet) {
      return this.universe != null && pSet.universe != null && this.universe == pSet.universe ? (this.mask & pSet.mask) != 0L : false;
   }

   public FeatureFlagSet join(FeatureFlagSet pOther) {
      if (this.universe == null) {
         return pOther;
      } else if (pOther.universe == null) {
         return this;
      } else if (this.universe != pOther.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + pOther.universe + "'");
      } else {
         return new FeatureFlagSet(this.universe, this.mask | pOther.mask);
      }
   }

   public FeatureFlagSet subtract(FeatureFlagSet pOther) {
      if (this.universe == null || pOther.universe == null) {
         return this;
      } else if (this.universe != pOther.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + pOther.universe + "'");
      } else {
         long $$1 = this.mask & ~pOther.mask;
         return $$1 == 0L ? EMPTY : new FeatureFlagSet(this.universe, $$1);
      }
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof FeatureFlagSet $$1 && this.universe == $$1.universe && this.mask == $$1.mask) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return (int)HashCommon.mix(this.mask);
   }
}
