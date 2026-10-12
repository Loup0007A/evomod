package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import org.apache.commons.lang3.mutable.MutableLong;

public class StrollToPoiList {
   public static BehaviorControl<Villager> create(
      MemoryModuleType<List<GlobalPos>> pPoiListMemory,
      float pSpeedModifier,
      int pCloseEnoughDist,
      int pMaxDistFromPoi,
      MemoryModuleType<GlobalPos> pMustBeCloseToMemory
   ) {
      MutableLong $$5 = new MutableLong(0L);
      return BehaviorBuilder.create(
         p_259612_ -> p_259612_.group(
                  p_259612_.registered(MemoryModuleType.WALK_TARGET), p_259612_.present(pPoiListMemory), p_259612_.present(pMustBeCloseToMemory)
               )
               .apply(
                  p_259612_,
                  (p_259574_, p_259801_, p_259116_) -> (p_259940_, p_259222_, p_260161_) -> {
                        List<GlobalPos> $$11 = p_259612_.get(p_259801_);
                        GlobalPos $$12 = p_259612_.get(p_259116_);
                        if ($$11.isEmpty()) {
                           return false;
                        } else {
                           GlobalPos $$13 = $$11.get(p_259940_.getRandom().nextInt($$11.size()));
                           if ($$13 != null
                              && p_259940_.dimension() == $$13.dimension()
                              && $$12.pos().closerToCenterThan(p_259222_.position(), (double)pMaxDistFromPoi)) {
                              if (p_260161_ > $$5.getValue()) {
                                 p_259574_.set(new WalkTarget($$13.pos(), pSpeedModifier, pCloseEnoughDist));
                                 $$5.setValue(p_260161_ + 100L);
                              }

                              return true;
                           } else {
                              return false;
                           }
                        }
                     }
               )
      );
   }
}
