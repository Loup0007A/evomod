package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class IntegerProperty extends Property<Integer> {
   private final ImmutableSet<Integer> values;
   private final int min;
   private final int max;

   protected IntegerProperty(String pName, int pMin, int pMax) {
      super(pName, Integer.class);
      if (pMin < 0) {
         throw new IllegalArgumentException("Min value of " + pName + " must be 0 or greater");
      } else if (pMax <= pMin) {
         throw new IllegalArgumentException("Max value of " + pName + " must be greater than min (" + pMin + ")");
      } else {
         this.min = pMin;
         this.max = pMax;
         Set<Integer> $$3 = Sets.newHashSet();

         for (int $$4 = pMin; $$4 <= pMax; $$4++) {
            $$3.add($$4);
         }

         this.values = ImmutableSet.copyOf($$3);
      }
   }

   @Override
   public Collection<Integer> getPossibleValues() {
      return this.values;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof IntegerProperty $$1 && super.equals(pOther)) {
            return this.values.equals($$1.values);
         }

         return false;
      }
   }

   @Override
   public int generateHashCode() {
      return 31 * super.generateHashCode() + this.values.hashCode();
   }

   public static IntegerProperty create(String pName, int pMin, int pMax) {
      return new IntegerProperty(pName, pMin, pMax);
   }

   @Override
   public Optional<Integer> getValue(String pValue) {
      try {
         Integer $$1 = Integer.valueOf(pValue);
         return $$1 >= this.min && $$1 <= this.max ? Optional.of($$1) : Optional.empty();
      } catch (NumberFormatException var3) {
         return Optional.empty();
      }
   }

   public String getName(Integer pValue) {
      return pValue.toString();
   }
}
