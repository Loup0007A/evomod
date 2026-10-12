package net.minecraft.world.level.levelgen;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Column {
   public static Column.Range around(int pFloor, int pCeiling) {
      return new Column.Range(pFloor - 1, pCeiling + 1);
   }

   public static Column.Range inside(int pFloor, int pCeiling) {
      return new Column.Range(pFloor, pCeiling);
   }

   public static Column below(int pCeiling) {
      return new Column.Ray(pCeiling, false);
   }

   public static Column fromHighest(int pCeiling) {
      return new Column.Ray(pCeiling + 1, false);
   }

   public static Column above(int pFloor) {
      return new Column.Ray(pFloor, true);
   }

   public static Column fromLowest(int pFloor) {
      return new Column.Ray(pFloor - 1, true);
   }

   public static Column line() {
      return Column.Line.INSTANCE;
   }

   public static Column create(OptionalInt pFloor, OptionalInt pCeiling) {
      if (pFloor.isPresent() && pCeiling.isPresent()) {
         return inside(pFloor.getAsInt(), pCeiling.getAsInt());
      } else if (pFloor.isPresent()) {
         return above(pFloor.getAsInt());
      } else {
         return pCeiling.isPresent() ? below(pCeiling.getAsInt()) : line();
      }
   }

   public abstract OptionalInt getCeiling();

   public abstract OptionalInt getFloor();

   public abstract OptionalInt getHeight();

   public Column withFloor(OptionalInt pFloor) {
      return create(pFloor, this.getCeiling());
   }

   public Column withCeiling(OptionalInt pCeiling) {
      return create(this.getFloor(), pCeiling);
   }

   public static Optional<Column> scan(
      LevelSimulatedReader pLevel, BlockPos pPos, int pMaxDistance, Predicate<BlockState> pColumnPredicate, Predicate<BlockState> pTipPredicate
   ) {
      BlockPos.MutableBlockPos $$5 = pPos.mutable();
      if (!pLevel.isStateAtPosition(pPos, pColumnPredicate)) {
         return Optional.empty();
      } else {
         int $$6 = pPos.getY();
         OptionalInt $$7 = scanDirection(pLevel, pMaxDistance, pColumnPredicate, pTipPredicate, $$5, $$6, Direction.UP);
         OptionalInt $$8 = scanDirection(pLevel, pMaxDistance, pColumnPredicate, pTipPredicate, $$5, $$6, Direction.DOWN);
         return Optional.of(create($$8, $$7));
      }
   }

   private static OptionalInt scanDirection(
      LevelSimulatedReader pLevel,
      int pMaxDistance,
      Predicate<BlockState> pColumnPredicate,
      Predicate<BlockState> pTipPredicate,
      BlockPos.MutableBlockPos pMutablePos,
      int pStartY,
      Direction pDirection
   ) {
      pMutablePos.setY(pStartY);

      for (int $$7 = 1; $$7 < pMaxDistance && pLevel.isStateAtPosition(pMutablePos, pColumnPredicate); $$7++) {
         pMutablePos.move(pDirection);
      }

      return pLevel.isStateAtPosition(pMutablePos, pTipPredicate) ? OptionalInt.of(pMutablePos.getY()) : OptionalInt.empty();
   }

   public static final class Line extends Column {
      static final Column.Line INSTANCE = new Column.Line();

      private Line() {
      }

      @Override
      public OptionalInt getCeiling() {
         return OptionalInt.empty();
      }

      @Override
      public OptionalInt getFloor() {
         return OptionalInt.empty();
      }

      @Override
      public OptionalInt getHeight() {
         return OptionalInt.empty();
      }

      @Override
      public String toString() {
         return "C(-)";
      }
   }

   public static final class Range extends Column {
      private final int floor;
      private final int ceiling;

      protected Range(int pFloor, int pCeiling) {
         this.floor = pFloor;
         this.ceiling = pCeiling;
         if (this.height() < 0) {
            throw new IllegalArgumentException("Column of negative height: " + this);
         }
      }

      @Override
      public OptionalInt getCeiling() {
         return OptionalInt.of(this.ceiling);
      }

      @Override
      public OptionalInt getFloor() {
         return OptionalInt.of(this.floor);
      }

      @Override
      public OptionalInt getHeight() {
         return OptionalInt.of(this.height());
      }

      public int ceiling() {
         return this.ceiling;
      }

      public int floor() {
         return this.floor;
      }

      public int height() {
         return this.ceiling - this.floor - 1;
      }

      @Override
      public String toString() {
         return "C(" + this.ceiling + "-" + this.floor + ")";
      }
   }

   public static final class Ray extends Column {
      private final int edge;
      private final boolean pointingUp;

      public Ray(int pEdge, boolean pPointingUp) {
         this.edge = pEdge;
         this.pointingUp = pPointingUp;
      }

      @Override
      public OptionalInt getCeiling() {
         return this.pointingUp ? OptionalInt.empty() : OptionalInt.of(this.edge);
      }

      @Override
      public OptionalInt getFloor() {
         return this.pointingUp ? OptionalInt.of(this.edge) : OptionalInt.empty();
      }

      @Override
      public OptionalInt getHeight() {
         return OptionalInt.empty();
      }

      @Override
      public String toString() {
         return this.pointingUp ? "C(" + this.edge + "-)" : "C(-" + this.edge + ")";
      }
   }
}
