package net.minecraft.world.level.block.piston;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class PistonMath {
   public static AABB getMovementArea(AABB pBounds, Direction pDir, double pDelta) {
      double $$3 = pDelta * (double)pDir.getAxisDirection().getStep();
      double $$4 = Math.min($$3, 0.0);
      double $$5 = Math.max($$3, 0.0);
      switch (pDir) {
         case WEST:
            return new AABB(pBounds.minX + $$4, pBounds.minY, pBounds.minZ, pBounds.minX + $$5, pBounds.maxY, pBounds.maxZ);
         case EAST:
            return new AABB(pBounds.maxX + $$4, pBounds.minY, pBounds.minZ, pBounds.maxX + $$5, pBounds.maxY, pBounds.maxZ);
         case DOWN:
            return new AABB(pBounds.minX, pBounds.minY + $$4, pBounds.minZ, pBounds.maxX, pBounds.minY + $$5, pBounds.maxZ);
         case UP:
         default:
            return new AABB(pBounds.minX, pBounds.maxY + $$4, pBounds.minZ, pBounds.maxX, pBounds.maxY + $$5, pBounds.maxZ);
         case NORTH:
            return new AABB(pBounds.minX, pBounds.minY, pBounds.minZ + $$4, pBounds.maxX, pBounds.maxY, pBounds.minZ + $$5);
         case SOUTH:
            return new AABB(pBounds.minX, pBounds.minY, pBounds.maxZ + $$4, pBounds.maxX, pBounds.maxY, pBounds.maxZ + $$5);
      }
   }
}
