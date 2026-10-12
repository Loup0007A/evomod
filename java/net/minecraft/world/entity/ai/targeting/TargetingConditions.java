package net.minecraft.world.entity.ai.targeting;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class TargetingConditions {
   public static final TargetingConditions DEFAULT = forCombat();
   private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0;
   private final boolean isCombat;
   private double range = -1.0;
   private boolean checkLineOfSight = true;
   private boolean testInvisible = true;
   @Nullable
   private Predicate<LivingEntity> selector;

   private TargetingConditions(boolean pIsCombat) {
      this.isCombat = pIsCombat;
   }

   public static TargetingConditions forCombat() {
      return new TargetingConditions(true);
   }

   public static TargetingConditions forNonCombat() {
      return new TargetingConditions(false);
   }

   public TargetingConditions copy() {
      TargetingConditions $$0 = this.isCombat ? forCombat() : forNonCombat();
      $$0.range = this.range;
      $$0.checkLineOfSight = this.checkLineOfSight;
      $$0.testInvisible = this.testInvisible;
      $$0.selector = this.selector;
      return $$0;
   }

   public TargetingConditions range(double pDistance) {
      this.range = pDistance;
      return this;
   }

   public TargetingConditions ignoreLineOfSight() {
      this.checkLineOfSight = false;
      return this;
   }

   public TargetingConditions ignoreInvisibilityTesting() {
      this.testInvisible = false;
      return this;
   }

   public TargetingConditions selector(@Nullable Predicate<LivingEntity> pCustomPredicate) {
      this.selector = pCustomPredicate;
      return this;
   }

   public boolean test(@Nullable LivingEntity pAttacker, LivingEntity pTarget) {
      if (pAttacker == pTarget) {
         return false;
      } else if (!pTarget.canBeSeenByAnyone()) {
         return false;
      } else if (this.selector != null && !this.selector.test(pTarget)) {
         return false;
      } else {
         if (pAttacker == null) {
            if (this.isCombat && (!pTarget.canBeSeenAsEnemy() || pTarget.level().getDifficulty() == Difficulty.PEACEFUL)) {
               return false;
            }
         } else {
            if (this.isCombat && (!pAttacker.canAttack(pTarget) || !pAttacker.canAttackType(pTarget.getType()) || pAttacker.isAlliedTo(pTarget))) {
               return false;
            }

            if (this.range > 0.0) {
               double $$2 = this.testInvisible ? pTarget.getVisibilityPercent(pAttacker) : 1.0;
               double $$3 = Math.max(this.range * $$2, 2.0);
               double $$4 = pAttacker.distanceToSqr(pTarget.getX(), pTarget.getY(), pTarget.getZ());
               if ($$4 > $$3 * $$3) {
                  return false;
               }
            }

            if (this.checkLineOfSight && pAttacker instanceof Mob $$5 && !$$5.getSensing().hasLineOfSight(pTarget)) {
               return false;
            }
         }

         return true;
      }
   }
}
