package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

@Deprecated
public class SetEntityLookTargetSometimes {
   public static BehaviorControl<LivingEntity> create(float pMaxDist, UniformInt pInterval) {
      return create(pMaxDist, pInterval, p_259715_ -> true);
   }

   public static BehaviorControl<LivingEntity> create(EntityType<?> pEntityType, float pMaxDist, UniformInt pInterval) {
      return create(pMaxDist, pInterval, p_341362_ -> pEntityType.equals(p_341362_.getType()));
   }

   private static BehaviorControl<LivingEntity> create(float pMaxDist, UniformInt pInterval, Predicate<LivingEntity> pCanLookAtTarget) {
      float $$3 = pMaxDist * pMaxDist;
      SetEntityLookTargetSometimes.Ticker $$4 = new SetEntityLookTargetSometimes.Ticker(pInterval);
      return BehaviorBuilder.create(
         p_259288_ -> p_259288_.group(p_259288_.absent(MemoryModuleType.LOOK_TARGET), p_259288_.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES))
               .apply(
                  p_259288_,
                  (p_259350_, p_260134_) -> (p_264952_, p_264953_, p_264954_) -> {
                        Optional<LivingEntity> $$9 = p_259288_.<NearestVisibleLivingEntities>get(p_260134_)
                           .findClosest(pCanLookAtTarget.and(p_326890_ -> p_326890_.distanceToSqr(p_264953_) <= (double)$$3));
                        if ($$9.isEmpty()) {
                           return false;
                        } else if (!$$4.tickDownAndCheck(p_264952_.random)) {
                           return false;
                        } else {
                           p_259350_.set(new EntityTracker($$9.get(), true));
                           return true;
                        }
                     }
               )
      );
   }

   public static final class Ticker {
      private final UniformInt interval;
      private int ticksUntilNextStart;

      public Ticker(UniformInt pInterval) {
         if (pInterval.getMinValue() <= 1) {
            throw new IllegalArgumentException();
         } else {
            this.interval = pInterval;
         }
      }

      public boolean tickDownAndCheck(RandomSource pRandom) {
         if (this.ticksUntilNextStart == 0) {
            this.ticksUntilNextStart = this.interval.sample(pRandom) - 1;
            return false;
         } else {
            return --this.ticksUntilNextStart == 0;
         }
      }
   }
}
