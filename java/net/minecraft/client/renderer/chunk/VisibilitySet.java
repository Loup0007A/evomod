package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VisibilitySet {
   private static final int FACINGS = Direction.values().length;
   private final BitSet data = new BitSet(FACINGS * FACINGS);

   public void add(Set<Direction> pFaces) {
      for (Direction $$1 : pFaces) {
         for (Direction $$2 : pFaces) {
            this.set($$1, $$2, true);
         }
      }
   }

   public void set(Direction pFace, Direction pOtherFace, boolean pVisible) {
      this.data.set(pFace.ordinal() + pOtherFace.ordinal() * FACINGS, pVisible);
      this.data.set(pOtherFace.ordinal() + pFace.ordinal() * FACINGS, pVisible);
   }

   public void setAll(boolean pVisible) {
      this.data.set(0, this.data.size(), pVisible);
   }

   public boolean visibilityBetween(Direction pFace, Direction pOtherFace) {
      return this.data.get(pFace.ordinal() + pOtherFace.ordinal() * FACINGS);
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();
      $$0.append(' ');

      for (Direction $$1 : Direction.values()) {
         $$0.append(' ').append($$1.toString().toUpperCase().charAt(0));
      }

      $$0.append('\n');

      for (Direction $$2 : Direction.values()) {
         $$0.append($$2.toString().toUpperCase().charAt(0));

         for (Direction $$3 : Direction.values()) {
            if ($$2 == $$3) {
               $$0.append("  ");
            } else {
               boolean $$4 = this.visibilityBetween($$2, $$3);
               $$0.append(' ').append((char)($$4 ? 'Y' : 'n'));
            }
         }

         $$0.append('\n');
      }

      return $$0.toString();
   }
}
