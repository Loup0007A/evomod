package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus {
   public static BehaviorControl<LivingEntity> create() {
      return BehaviorBuilder.create(p_259870_ -> p_259870_.point((p_288835_, p_288836_, p_288837_) -> {
            if (p_288835_.random.nextInt(20) != 0) {
               return false;
            } else {
               Brain<?> $$3 = p_288836_.getBrain();
               Raid $$4 = p_288835_.getRaidAt(p_288836_.blockPosition());
               if ($$4 == null || $$4.isStopped() || $$4.isLoss()) {
                  $$3.setDefaultActivity(Activity.IDLE);
                  $$3.updateActivityFromSchedule(p_288835_.getDayTime(), p_288835_.getGameTime());
               }

               return true;
            }
         }));
   }
}
