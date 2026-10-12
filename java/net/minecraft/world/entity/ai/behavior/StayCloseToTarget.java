package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StayCloseToTarget {
   public static BehaviorControl<LivingEntity> create(
      Function<LivingEntity, Optional<PositionTracker>> pTargetPositionGetter,
      Predicate<LivingEntity> pPredicate,
      int pCloseEnoughDist,
      int pTooClose,
      float pSpeedModifier
   ) {
      return BehaviorBuilder.create(
         p_272460_ -> p_272460_.group(p_272460_.registered(MemoryModuleType.LOOK_TARGET), p_272460_.registered(MemoryModuleType.WALK_TARGET))
               .apply(p_272460_, (p_272466_, p_272467_) -> (p_260054_, p_260069_, p_259517_) -> {
                     Optional<PositionTracker> $$10 = pTargetPositionGetter.apply(p_260069_);
                     if (!$$10.isEmpty() && pPredicate.test(p_260069_)) {
                        PositionTracker $$11 = $$10.get();
                        if (p_260069_.position().closerThan($$11.currentPosition(), (double)pTooClose)) {
                           return false;
                        } else {
                           PositionTracker $$12 = $$10.get();
                           p_272466_.set($$12);
                           p_272467_.set(new WalkTarget($$12, pSpeedModifier, pCloseEnoughDist));
                           return true;
                        }
                     } else {
                        return false;
                     }
                  })
      );
   }
}
