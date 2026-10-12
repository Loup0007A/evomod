package net.minecraft.world.entity.ai.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class HoverRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob pMob, int pRadius, int pYRange, double pX, double pZ, float pAmplifier, int pMaxSwimUp, int pMinSwimUp) {
      boolean $$8 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(
         pMob,
         () -> {
            BlockPos $$9 = RandomPos.generateRandomDirectionWithinRadians(pMob.getRandom(), pRadius, pYRange, 0, pX, pZ, (double)pAmplifier);
            if ($$9 == null) {
               return null;
            } else {
               BlockPos $$10 = LandRandomPos.generateRandomPosTowardDirection(pMob, pRadius, $$8, $$9);
               if ($$10 == null) {
                  return null;
               } else {
                  $$10 = RandomPos.moveUpToAboveSolid(
                     $$10,
                     pMob.getRandom().nextInt(pMaxSwimUp - pMinSwimUp + 1) + pMinSwimUp,
                     pMob.level().getMaxBuildHeight(),
                     p_148486_ -> GoalUtils.isSolid(pMob, p_148486_)
                  );
                  return !GoalUtils.isWater(pMob, $$10) && !GoalUtils.hasMalus(pMob, $$10) ? $$10 : null;
               }
            }
         }
      );
   }
}
