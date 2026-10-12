package net.minecraftforge.common.extensions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface IForgePlayer {
   private Player self() {
      return (Player)this;
   }

   default boolean isCloseEnough(Entity entity, double dist) {
      Vec3 eye = this.self().getEyePosition();
      AABB aabb = entity.getBoundingBox().inflate((double)entity.getPickRadius());
      return aabb.distanceToSqr(eye) < dist * dist;
   }
}
