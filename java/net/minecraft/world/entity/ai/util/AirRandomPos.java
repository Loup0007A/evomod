package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirRandomPos {
   @Nullable
   public static Vec3 getPosTowards(PathfinderMob pMob, int pRadius, int pYRange, int pY, Vec3 pVectorPosition, double pAmplifier) {
      Vec3 $$6 = pVectorPosition.subtract(pMob.getX(), pMob.getY(), pMob.getZ());
      boolean $$7 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(pMob, () -> {
         BlockPos $$7x = AirAndWaterRandomPos.generateRandomPos(pMob, pRadius, pYRange, pY, $$6.x, $$6.z, pAmplifier, $$7);
         return $$7x != null && !GoalUtils.isWater(pMob, $$7x) ? $$7x : null;
      });
   }
}
