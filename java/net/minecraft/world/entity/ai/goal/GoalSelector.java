package net.minecraft.world.entity.ai.goal;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.util.profiling.ProfilerFiller;

public class GoalSelector {
   private static final WrappedGoal NO_GOAL = new WrappedGoal(Integer.MAX_VALUE, new Goal() {
      @Override
      public boolean canUse() {
         return false;
      }
   }) {
      @Override
      public boolean isRunning() {
         return false;
      }
   };
   private final Map<Goal.Flag, WrappedGoal> lockedFlags = new EnumMap<>(Goal.Flag.class);
   private final Set<WrappedGoal> availableGoals = new ObjectLinkedOpenHashSet();
   private final Supplier<ProfilerFiller> profiler;
   private final EnumSet<Goal.Flag> disabledFlags = EnumSet.noneOf(Goal.Flag.class);

   public GoalSelector(Supplier<ProfilerFiller> pProfiler) {
      this.profiler = pProfiler;
   }

   public void addGoal(int pPriority, Goal pGoal) {
      this.availableGoals.add(new WrappedGoal(pPriority, pGoal));
   }

   @VisibleForTesting
   public void removeAllGoals(Predicate<Goal> pFilter) {
      this.availableGoals.removeIf(p_262564_ -> pFilter.test(p_262564_.getGoal()));
   }

   public void removeGoal(Goal pGoal) {
      for (WrappedGoal $$1 : this.availableGoals) {
         if ($$1.getGoal() == pGoal && $$1.isRunning()) {
            $$1.stop();
         }
      }

      this.availableGoals.removeIf(p_25378_ -> p_25378_.getGoal() == pGoal);
   }

   private static boolean goalContainsAnyFlags(WrappedGoal pGoal, EnumSet<Goal.Flag> pFlag) {
      for (Goal.Flag $$2 : pGoal.getFlags()) {
         if (pFlag.contains($$2)) {
            return true;
         }
      }

      return false;
   }

   private static boolean goalCanBeReplacedForAllFlags(WrappedGoal pGoal, Map<Goal.Flag, WrappedGoal> pFlag) {
      for (Goal.Flag $$2 : pGoal.getFlags()) {
         if (!pFlag.getOrDefault($$2, NO_GOAL).canBeReplacedBy(pGoal)) {
            return false;
         }
      }

      return true;
   }

   public void tick() {
      ProfilerFiller $$0 = this.profiler.get();
      $$0.push("goalCleanup");

      for (WrappedGoal $$1 : this.availableGoals) {
         if ($$1.isRunning() && (goalContainsAnyFlags($$1, this.disabledFlags) || !$$1.canContinueToUse())) {
            $$1.stop();
         }
      }

      this.lockedFlags.entrySet().removeIf(p_326929_ -> !p_326929_.getValue().isRunning());
      $$0.pop();
      $$0.push("goalUpdate");

      for (WrappedGoal $$2 : this.availableGoals) {
         if (!$$2.isRunning() && !goalContainsAnyFlags($$2, this.disabledFlags) && goalCanBeReplacedForAllFlags($$2, this.lockedFlags) && $$2.canUse()) {
            for (Goal.Flag $$3 : $$2.getFlags()) {
               WrappedGoal $$4 = this.lockedFlags.getOrDefault($$3, NO_GOAL);
               $$4.stop();
               this.lockedFlags.put($$3, $$2);
            }

            $$2.start();
         }
      }

      $$0.pop();
      this.tickRunningGoals(true);
   }

   public void tickRunningGoals(boolean pTickAllRunning) {
      ProfilerFiller $$1 = this.profiler.get();
      $$1.push("goalTick");

      for (WrappedGoal $$2 : this.availableGoals) {
         if ($$2.isRunning() && (pTickAllRunning || $$2.requiresUpdateEveryTick())) {
            $$2.tick();
         }
      }

      $$1.pop();
   }

   public Set<WrappedGoal> getAvailableGoals() {
      return this.availableGoals;
   }

   public void disableControlFlag(Goal.Flag pFlag) {
      this.disabledFlags.add(pFlag);
   }

   public void enableControlFlag(Goal.Flag pFlag) {
      this.disabledFlags.remove(pFlag);
   }

   public void setControlFlag(Goal.Flag pFlag, boolean pEnabled) {
      if (pEnabled) {
         this.enableControlFlag(pFlag);
      } else {
         this.disableControlFlag(pFlag);
      }
   }
}
