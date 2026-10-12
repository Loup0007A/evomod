package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LocalCoordinates implements Coordinates {
   public static final char PREFIX_LOCAL_COORDINATE = '^';
   private final double left;
   private final double up;
   private final double forwards;

   public LocalCoordinates(double pLeft, double pUp, double pForwards) {
      this.left = pLeft;
      this.up = pUp;
      this.forwards = pForwards;
   }

   @Override
   public Vec3 getPosition(CommandSourceStack pSource) {
      Vec2 $$1 = pSource.getRotation();
      Vec3 $$2 = pSource.getAnchor().apply(pSource);
      float $$3 = Mth.cos(($$1.y + 90.0F) * (float) (Math.PI / 180.0));
      float $$4 = Mth.sin(($$1.y + 90.0F) * (float) (Math.PI / 180.0));
      float $$5 = Mth.cos(-$$1.x * (float) (Math.PI / 180.0));
      float $$6 = Mth.sin(-$$1.x * (float) (Math.PI / 180.0));
      float $$7 = Mth.cos((-$$1.x + 90.0F) * (float) (Math.PI / 180.0));
      float $$8 = Mth.sin((-$$1.x + 90.0F) * (float) (Math.PI / 180.0));
      Vec3 $$9 = new Vec3((double)($$3 * $$5), (double)$$6, (double)($$4 * $$5));
      Vec3 $$10 = new Vec3((double)($$3 * $$7), (double)$$8, (double)($$4 * $$7));
      Vec3 $$11 = $$9.cross($$10).scale(-1.0);
      double $$12 = $$9.x * this.forwards + $$10.x * this.up + $$11.x * this.left;
      double $$13 = $$9.y * this.forwards + $$10.y * this.up + $$11.y * this.left;
      double $$14 = $$9.z * this.forwards + $$10.z * this.up + $$11.z * this.left;
      return new Vec3($$2.x + $$12, $$2.y + $$13, $$2.z + $$14);
   }

   @Override
   public Vec2 getRotation(CommandSourceStack pSource) {
      return Vec2.ZERO;
   }

   @Override
   public boolean isXRelative() {
      return true;
   }

   @Override
   public boolean isYRelative() {
      return true;
   }

   @Override
   public boolean isZRelative() {
      return true;
   }

   public static LocalCoordinates parse(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();
      double $$2 = readDouble(pReader, $$1);
      if (pReader.canRead() && pReader.peek() == ' ') {
         pReader.skip();
         double $$3 = readDouble(pReader, $$1);
         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            double $$4 = readDouble(pReader, $$1);
            return new LocalCoordinates($$2, $$3, $$4);
         } else {
            pReader.setCursor($$1);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
         }
      } else {
         pReader.setCursor($$1);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
      }
   }

   private static double readDouble(StringReader pReader, int pStart) throws CommandSyntaxException {
      if (!pReader.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(pReader);
      } else if (pReader.peek() != '^') {
         pReader.setCursor(pStart);
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
      } else {
         pReader.skip();
         return pReader.canRead() && pReader.peek() != ' ' ? pReader.readDouble() : 0.0;
      }
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof LocalCoordinates $$1) ? false : this.left == $$1.left && this.up == $$1.up && this.forwards == $$1.forwards;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.left, this.up, this.forwards);
   }
}
