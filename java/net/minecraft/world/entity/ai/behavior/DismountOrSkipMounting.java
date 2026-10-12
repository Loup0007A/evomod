package net.minecraft.world.entity.ai.behavior;

import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DismountOrSkipMounting {
   public static <E extends LivingEntity> BehaviorControl<E> create(int pMaxDistanceFromVehicle, BiPredicate<E, Entity> pShouldStopRiding) {
      return BehaviorBuilder.create(
         p_259780_ -> p_259780_.group(p_259780_.registered(MemoryModuleType.RIDE_TARGET)).apply(p_259780_, p_259326_ -> (p_259287_, p_259246_, p_259462_) -> {
                  Entity $$7 = p_259246_.getVehicle();
                  Entity $$8 = p_259780_.<Entity>tryGet(p_259326_).orElse(null);
                  if ($$7 == null && $$8 == null) {
                     return false;
                  } else {
                     Entity $$9 = $$7 == null ? $$8 : $$7;
                     if (isVehicleValid(p_259246_, $$9, pMaxDistanceFromVehicle) && !pShouldStopRiding.test((E)p_259246_, $$9)) {
                        return false;
                     } else {
                        p_259246_.stopRiding();
                        p_259326_.erase();
                        return true;
                     }
                  }
               })
      );
   }

   private static boolean isVehicleValid(LivingEntity pEntity, Entity pVehicle, int pDistance) {
      return pVehicle.isAlive() && pVehicle.closerThan(pEntity, (double)pDistance) && pVehicle.level() == pEntity.level();
   }
}
