package net.minecraft.world.entity.monster.piglin;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.common.ToolActions;

public class StopHoldingItemIfNoLongerAdmiring {
   public static BehaviorControl<Piglin> create() {
      return BehaviorBuilder.create(
         p_259197_ -> p_259197_.group(p_259197_.absent(MemoryModuleType.ADMIRING_ITEM)).apply(p_259197_, p_259512_ -> (p_341467_, p_341468_, p_341469_) -> {
                  if (!p_341468_.getOffhandItem().isEmpty() && !p_341468_.getOffhandItem().canPerformAction(ToolActions.SHIELD_BLOCK)) {
                     PiglinAi.stopHoldingOffHandItem(p_341468_, true);
                     return true;
                  } else {
                     return false;
                  }
               })
      );
   }
}
