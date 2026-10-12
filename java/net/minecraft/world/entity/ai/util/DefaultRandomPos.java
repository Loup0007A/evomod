package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class DefaultRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob pMob, int pRadius, int pVerticalDistance) {
      boolean $$3 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(pMob, () -> {
         BlockPos $$4 = RandomPos.generateRandomDirection(pMob.getRandom(), pRadius, pVerticalDistance);
         return generateRandomPosTowardDirection(pMob, pRadius, $$3, $$4);
      });
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob pMob, int pRadius, int pYRange, Vec3 pVectorPosition, double pAmplifier) {
      Vec3 $$5 = pVectorPosition.subtract(pMob.getX(), pMob.getY(), pMob.getZ());
      boolean $$6 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(pMob, () -> {
         BlockPos $$6x = RandomPos.generateRandomDirectionWithinRadians(pMob.getRandom(), pRadius, pYRange, 0, $$5.x, $$5.z, pAmplifier);
         return $$6x == null ? null : generateRandomPosTowardDirection(pMob, pRadius, $$6, $$6x);
      });
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob pMob, int pRadius, int pYRange, Vec3 pVectorPosition) {
      Vec3 $$4 = pMob.position().subtract(pVectorPosition);
      boolean $$5 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(pMob, () -> {
         BlockPos $$5x = RandomPos.generateRandomDirectionWithinRadians(pMob.getRandom(), pRadius, pYRange, 0, $$4.x, $$4.z, (float) (Math.PI / 2));
         return $$5x == null ? null : generateRandomPosTowardDirection(pMob, pRadius, $$5, $$5x);
      });
   }

   @Nullable
   private static BlockPos generateRandomPosTowardDirection(PathfinderMob pMob, int pRadius, boolean pShortCircuit, BlockPos pPos) {
      BlockPos $$4 = RandomPos.generateRandomPosTowardDirection(pMob, pRadius, pMob.getRandom(), pPos);
      return !GoalUtils.isOutsideLimits($$4, pMob)
            && !GoalUtils.isRestricted(pShortCircuit, pMob, $$4)
            && !GoalUtils.isNotStable(pMob.getNavigation(), $$4)
            && !GoalUtils.hasMalus(pMob, $$4)
         ? $$4
         : null;
   }
}
