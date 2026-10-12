package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class WorldCoordinate {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));
   private final boolean relative;
   private final double value;

   public WorldCoordinate(boolean pRelative, double pValue) {
      this.relative = pRelative;
      this.value = pValue;
   }

   public double get(double pCoord) {
      return this.relative ? this.value + pCoord : this.value;
   }

   public static WorldCoordinate parseDouble(StringReader pReader, boolean pCenterCorrect) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
      } else if (!pReader.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(pReader);
      } else {
         boolean $$2 = isRelative(pReader);
         int $$3 = pReader.getCursor();
         double $$4 = pReader.canRead() && pReader.peek() != ' ' ? pReader.readDouble() : 0.0;
         String $$5 = pReader.getString().substring($$3, pReader.getCursor());
         if ($$2 && $$5.isEmpty()) {
            return new WorldCoordinate(true, 0.0);
         } else {
            if (!$$5.contains(".") && !$$2 && pCenterCorrect) {
               $$4 += 0.5;
            }

            return new WorldCoordinate($$2, $$4);
         }
      }
   }

   public static WorldCoordinate parseInt(StringReader pReader) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
      } else if (!pReader.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(pReader);
      } else {
         boolean $$1 = isRelative(pReader);
         double $$2;
         if (pReader.canRead() && pReader.peek() != ' ') {
            $$2 = $$1 ? pReader.readDouble() : (double)pReader.readInt();
         } else {
            $$2 = 0.0;
         }

         return new WorldCoordinate($$1, $$2);
      }
   }

   public static boolean isRelative(StringReader pReader) {
      boolean $$1;
      if (pReader.peek() == '~') {
         $$1 = true;
         pReader.skip();
      } else {
         $$1 = false;
      }

      return $$1;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof WorldCoordinate $$1)) {
         return false;
      } else {
         return this.relative != $$1.relative ? false : Double.compare($$1.value, this.value) == 0;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.relative ? 1 : 0;
      long $$1 = Double.doubleToLongBits(this.value);
      return 31 * $$0 + (int)($$1 ^ $$1 >>> 32);
   }

   public boolean isRelative() {
      return this.relative;
   }
}
