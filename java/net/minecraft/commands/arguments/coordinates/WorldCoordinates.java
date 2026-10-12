package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WorldCoordinates implements Coordinates {
   private final WorldCoordinate x;
   private final WorldCoordinate y;
   private final WorldCoordinate z;

   public WorldCoordinates(WorldCoordinate pX, WorldCoordinate pY, WorldCoordinate pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   @Override
   public Vec3 getPosition(CommandSourceStack pSource) {
      Vec3 $$1 = pSource.getPosition();
      return new Vec3(this.x.get($$1.x), this.y.get($$1.y), this.z.get($$1.z));
   }

   @Override
   public Vec2 getRotation(CommandSourceStack pSource) {
      Vec2 $$1 = pSource.getRotation();
      return new Vec2((float)this.x.get((double)$$1.x), (float)this.y.get((double)$$1.y));
   }

   @Override
   public boolean isXRelative() {
      return this.x.isRelative();
   }

   @Override
   public boolean isYRelative() {
      return this.y.isRelative();
   }

   @Override
   public boolean isZRelative() {
      return this.z.isRelative();
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof WorldCoordinates $$1)) {
         return false;
      } else if (!this.x.equals($$1.x)) {
         return false;
      } else {
         return !this.y.equals($$1.y) ? false : this.z.equals($$1.z);
      }
   }

   public static WorldCoordinates parseInt(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();
      WorldCoordinate $$2 = WorldCoordinate.parseInt(pReader);
      if (pReader.canRead() && pReader.peek() == ' ') {
         pReader.skip();
         WorldCoordinate $$3 = WorldCoordinate.parseInt(pReader);
         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            WorldCoordinate $$4 = WorldCoordinate.parseInt(pReader);
            return new WorldCoordinates($$2, $$3, $$4);
         } else {
            pReader.setCursor($$1);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
         }
      } else {
         pReader.setCursor($$1);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
      }
   }

   public static WorldCoordinates parseDouble(StringReader pReader, boolean pCenterCorrect) throws CommandSyntaxException {
      int $$2 = pReader.getCursor();
      WorldCoordinate $$3 = WorldCoordinate.parseDouble(pReader, pCenterCorrect);
      if (pReader.canRead() && pReader.peek() == ' ') {
         pReader.skip();
         WorldCoordinate $$4 = WorldCoordinate.parseDouble(pReader, false);
         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            WorldCoordinate $$5 = WorldCoordinate.parseDouble(pReader, pCenterCorrect);
            return new WorldCoordinates($$3, $$4, $$5);
         } else {
            pReader.setCursor($$2);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
         }
      } else {
         pReader.setCursor($$2);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
      }
   }

   public static WorldCoordinates absolute(double pX, double pY, double pZ) {
      return new WorldCoordinates(new WorldCoordinate(false, pX), new WorldCoordinate(false, pY), new WorldCoordinate(false, pZ));
   }

   public static WorldCoordinates absolute(Vec2 pVector) {
      return new WorldCoordinates(new WorldCoordinate(false, (double)pVector.x), new WorldCoordinate(false, (double)pVector.y), new WorldCoordinate(true, 0.0));
   }

   public static WorldCoordinates current() {
      return new WorldCoordinates(new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0), new WorldCoordinate(true, 0.0));
   }

   @Override
   public int hashCode() {
      int $$0 = this.x.hashCode();
      $$0 = 31 * $$0 + this.y.hashCode();
      return 31 * $$0 + this.z.hashCode();
   }
}
