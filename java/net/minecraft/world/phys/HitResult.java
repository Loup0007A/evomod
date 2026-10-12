package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;

public abstract class HitResult {
   protected final Vec3 location;

   protected HitResult(Vec3 pLocation) {
      this.location = pLocation;
   }

   public double distanceTo(Entity pEntity) {
      double $$1 = this.location.x - pEntity.getX();
      double $$2 = this.location.y - pEntity.getY();
      double $$3 = this.location.z - pEntity.getZ();
      return $$1 * $$1 + $$2 * $$2 + $$3 * $$3;
   }

   public abstract HitResult.Type getType();

   public Vec3 getLocation() {
      return this.location;
   }

   public static enum Type {
      MISS,
      BLOCK,
      ENTITY;
   }
}
