package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class SetWalkTargetFromBlockMemory {
   public static OneShot<Villager> create(
      MemoryModuleType<GlobalPos> pBlockTargetMemory, float pSpeedModifier, int pCloseEnoughDist, int pTooFarDistance, int pTooLongUnreachableDuration
   ) {
      return BehaviorBuilder.create(
         p_258717_ -> p_258717_.group(
                  p_258717_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
                  p_258717_.absent(MemoryModuleType.WALK_TARGET),
                  p_258717_.present(pBlockTargetMemory)
               )
               .apply(
                  p_258717_,
                  (p_258709_, p_258710_, p_258711_) -> (p_275056_, p_275057_, p_275058_) -> {
                        GlobalPos $$12 = p_258717_.get(p_258711_);
                        Optional<Long> $$13 = p_258717_.tryGet(p_258709_);
                        if ($$12.dimension() == p_275056_.dimension()
                           && (!$$13.isPresent() || p_275056_.getGameTime() - $$13.get() <= (long)pTooLongUnreachableDuration)) {
                           if ($$12.pos().distManhattan(p_275057_.blockPosition()) > pTooFarDistance) {
                              Vec3 $$14 = null;
                              int $$15 = 0;
                              int $$16 = 1000;

                              while ($$14 == null || BlockPos.containing($$14).distManhattan(p_275057_.blockPosition()) > pTooFarDistance) {
                                 $$14 = DefaultRandomPos.getPosTowards(p_275057_, 15, 7, Vec3.atBottomCenterOf($$12.pos()), (float) (Math.PI / 2));
                                 if (++$$15 == 1000) {
                                    p_275057_.releasePoi(pBlockTargetMemory);
                                    p_258711_.erase();
                                    p_258709_.set(p_275058_);
                                    return true;
                                 }
                              }

                              p_258710_.set(new WalkTarget($$14, pSpeedModifier, pCloseEnoughDist));
                           } else if ($$12.pos().distManhattan(p_275057_.blockPosition()) > pCloseEnoughDist) {
                              p_258710_.set(new WalkTarget($$12.pos(), pSpeedModifier, pCloseEnoughDist));
                           }
                        } else {
                           p_275057_.releasePoi(pBlockTargetMemory);
                           p_258711_.erase();
                           p_258709_.set(p_275058_);
                        }

                        return true;
                     }
               )
      );
   }
}
