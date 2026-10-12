package net.minecraft.world.entity.ai.goal.target;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.scores.Team;

public abstract class TargetGoal extends Goal {
   private static final int EMPTY_REACH_CACHE = 0;
   private static final int CAN_REACH_CACHE = 1;
   private static final int CANT_REACH_CACHE = 2;
   protected final Mob mob;
   protected final boolean mustSee;
   private final boolean mustReach;
   private int reachCache;
   private int reachCacheTime;
   private int unseenTicks;
   @Nullable
   protected LivingEntity targetMob;
   protected int unseenMemoryTicks = 60;

   public TargetGoal(Mob pMob, boolean pMustSee) {
      this(pMob, pMustSee, false);
   }

   public TargetGoal(Mob pMob, boolean pMustSee, boolean pMustReach) {
      this.mob = pMob;
      this.mustSee = pMustSee;
      this.mustReach = pMustReach;
   }

   @Override
   public boolean canContinueToUse() {
      LivingEntity $$0 = this.mob.getTarget();
      if ($$0 == null) {
         $$0 = this.targetMob;
      }

      if ($$0 == null) {
         return false;
      } else if (!this.mob.canAttack($$0)) {
         return false;
      } else {
         Team $$1 = this.mob.getTeam();
         Team $$2 = $$0.getTeam();
         if ($$1 != null && $$2 == $$1) {
            return false;
         } else {
            double $$3 = this.getFollowDistance();
            if (this.mob.distanceToSqr($$0) > $$3 * $$3) {
               return false;
            } else {
               if (this.mustSee) {
                  if (this.mob.getSensing().hasLineOfSight($$0)) {
                     this.unseenTicks = 0;
                  } else if (++this.unseenTicks > reducedTickDelay(this.unseenMemoryTicks)) {
                     return false;
                  }
               }

               this.mob.setTarget($$0);
               return true;
            }
         }
      }
   }

   protected double getFollowDistance() {
      return this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
   }

   @Override
   public void start() {
      this.reachCache = 0;
      this.reachCacheTime = 0;
      this.unseenTicks = 0;
   }

   @Override
   public void stop() {
      this.mob.setTarget(null);
      this.targetMob = null;
   }

   protected boolean canAttack(@Nullable LivingEntity pPotentialTarget, TargetingConditions pTargetPredicate) {
      if (pPotentialTarget == null) {
         return false;
      } else if (!pTargetPredicate.test(this.mob, pPotentialTarget)) {
         return false;
      } else if (!this.mob.isWithinRestriction(pPotentialTarget.blockPosition())) {
         return false;
      } else {
         if (this.mustReach) {
            if (--this.reachCacheTime <= 0) {
               this.reachCache = 0;
            }

            if (this.reachCache == 0) {
               this.reachCache = this.canReach(pPotentialTarget) ? 1 : 2;
            }

            if (this.reachCache == 2) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean canReach(LivingEntity pTarget) {
      this.reachCacheTime = reducedTickDelay(10 + this.mob.getRandom().nextInt(5));
      Path $$1 = this.mob.getNavigation().createPath(pTarget, 0);
      if ($$1 == null) {
         return false;
      } else {
         Node $$2 = $$1.getEndNode();
         if ($$2 == null) {
            return false;
         } else {
            int $$3 = $$2.x - pTarget.getBlockX();
            int $$4 = $$2.z - pTarget.getBlockZ();
            return (double)($$3 * $$3 + $$4 * $$4) <= 2.25;
         }
      }
   }

   public TargetGoal setUnseenMemoryTicks(int pUnseenMemoryTicks) {
      this.unseenMemoryTicks = pUnseenMemoryTicks;
      return this;
   }
}
