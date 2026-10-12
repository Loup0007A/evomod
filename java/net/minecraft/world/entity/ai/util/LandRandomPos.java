package net.minecraft.world.entity.ai.util;

import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class LandRandomPos {
   @Nullable
   public static Vec3 getPos(PathfinderMob pMob, int pRadius, int pVerticalRange) {
      return getPos(pMob, pRadius, pVerticalRange, pMob::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 getPos(PathfinderMob pMob, int pRadius, int pYRange, ToDoubleFunction<BlockPos> pToDoubleFunction) {
      boolean $$4 = GoalUtils.mobRestricted(pMob, pRadius);
      return RandomPos.generateRandomPos(() -> {
         BlockPos $$4x = RandomPos.generateRandomDirection(pMob.getRandom(), pRadius, pYRange);
         BlockPos $$5 = generateRandomPosTowardDirection(pMob, pRadius, $$4, $$4x);
         return $$5 == null ? null : movePosUpOutOfSolid(pMob, $$5);
      }, pToDoubleFunction);
   }

   @Nullable
   public static Vec3 getPosTowards(PathfinderMob pMob, int pRadius, int pYRange, Vec3 pVectorPosition) {
      Vec3 $$4 = pVectorPosition.subtract(pMob.getX(), pMob.getY(), pMob.getZ());
      boolean $$5 = GoalUtils.mobRestricted(pMob, pRadius);
      return getPosInDirection(pMob, pRadius, pYRange, $$4, $$5);
   }

   @Nullable
   public static Vec3 getPosAway(PathfinderMob pMob, int pRadius, int pYRange, Vec3 pVectorPosition) {
      Vec3 $$4 = pMob.position().subtract(pVectorPosition);
      boolean $$5 = GoalUtils.mobRestricted(pMob, pRadius);
      return getPosInDirection(pMob, pRadius, pYRange, $$4, $$5);
   }

   @Nullable
   private static Vec3 getPosInDirection(PathfinderMob pMob, int pRadius, int pYRange, Vec3 pVectorPosition, boolean pShortCircuit) {
      return RandomPos.generateRandomPos(
         pMob,
         () -> {
            BlockPos $$5 = RandomPos.generateRandomDirectionWithinRadians(
               pMob.getRandom(), pRadius, pYRange, 0, pVectorPosition.x, pVectorPosition.z, (float) (Math.PI / 2)
            );
            if ($$5 == null) {
               return null;
            } else {
               BlockPos $$6 = generateRandomPosTowardDirection(pMob, pRadius, pShortCircuit, $$5);
               return $$6 == null ? null : movePosUpOutOfSolid(pMob, $$6);
            }
         }
      );
   }

   @Nullable
   public static BlockPos movePosUpOutOfSolid(PathfinderMob pMob, BlockPos pPos) {
      pPos = RandomPos.moveUpOutOfSolid(pPos, pMob.level().getMaxBuildHeight(), p_148534_ -> GoalUtils.isSolid(pMob, p_148534_));
      return !GoalUtils.isWater(pMob, pPos) && !GoalUtils.hasMalus(pMob, pPos) ? pPos : null;
   }

   @Nullable
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob pMob, int pRadius, boolean pShortCircuit, BlockPos pPos) {
      BlockPos $$4 = RandomPos.generateRandomPosTowardDirection(pMob, pRadius, pMob.getRandom(), pPos);
      return !GoalUtils.isOutsideLimits($$4, pMob) && !GoalUtils.isRestricted(pShortCircuit, pMob, $$4) && !GoalUtils.isNotStable(pMob.getNavigation(), $$4)
         ? $$4
         : null;
   }
}
