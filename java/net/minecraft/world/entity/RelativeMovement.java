package net.minecraft.world.entity;

import java.util.EnumSet;
import java.util.Set;

public enum RelativeMovement {
   X(0),
   Y(1),
   Z(2),
   Y_ROT(3),
   X_ROT(4);

   public static final Set<RelativeMovement> ALL = Set.of(values());
   public static final Set<RelativeMovement> ROTATION = Set.of(X_ROT, Y_ROT);
   private final int bit;

   private RelativeMovement(final int pBit) {
      this.bit = pBit;
   }

   private int getMask() {
      return 1 << this.bit;
   }

   private boolean isSet(int pPackedMovements) {
      return (pPackedMovements & this.getMask()) == this.getMask();
   }

   public static Set<RelativeMovement> unpack(int pPackedMovements) {
      Set<RelativeMovement> $$1 = EnumSet.noneOf(RelativeMovement.class);

      for (RelativeMovement $$2 : values()) {
         if ($$2.isSet(pPackedMovements)) {
            $$1.add($$2);
         }
      }

      return $$1;
   }

   public static int pack(Set<RelativeMovement> pMovements) {
      int $$1 = 0;

      for (RelativeMovement $$2 : pMovements) {
         $$1 |= $$2.getMask();
      }

      return $$1;
   }
}
