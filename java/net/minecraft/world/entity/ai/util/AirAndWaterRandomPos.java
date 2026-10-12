package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class AirAndWaterRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob pMob, int pMaxDistance, int pYRange, int pY, double pX, double pZ, double pAmplifier) {
      boolean $$7 = GoalUtils.mobRestricted(pMob, pMaxDistance);
      return RandomPos.generateRandomPos(pMob, () -> generateRandomPos(pMob, pMaxDistance, pYRange, pY, pX, pZ, pAmplifier, $$7));
   }

   @Nullable
   public static BlockPos generateRandomPos(
      PathfinderMob pMob, int pMaxDistance, int pYRange, int pY, double pX, double pZ, double pAmplifier, boolean pShortCircuit
   ) {
      BlockPos $$8 = RandomPos.generateRandomDirectionWithinRadians(pMob.getRandom(), pMaxDistance, pYRange, pY, pX, pZ, pAmplifier);
      if ($$8 == null) {
         return null;
      } else {
         BlockPos $$9 = RandomPos.generateRandomPosTowardDirection(pMob, pMaxDistance, pMob.getRandom(), $$8);
         if (!GoalUtils.isOutsideLimits($$9, pMob) && !GoalUtils.isRestricted(pShortCircuit, pMob, $$9)) {
            $$9 = RandomPos.moveUpOutOfSolid($$9, pMob.level().getMaxBuildHeight(), p_148376_ -> GoalUtils.isSolid(pMob, p_148376_));
            return GoalUtils.hasMalus(pMob, $$9) ? null : $$9;
         } else {
            return null;
         }
      }
   }
}
