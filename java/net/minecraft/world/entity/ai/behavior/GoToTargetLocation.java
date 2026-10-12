package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
   private static BlockPos getNearbyPos(Mob pMob, BlockPos pPos) {
      RandomSource $$2 = pMob.level().random;
      return pPos.offset(getRandomOffset($$2), 0, getRandomOffset($$2));
   }

   private static int getRandomOffset(RandomSource pRandom) {
      return pRandom.nextInt(3) - 1;
   }

   public static <E extends Mob> OneShot<E> create(MemoryModuleType<BlockPos> pLocationMemory, int pCloseEnoughDist, float pSpeedModifier) {
      return BehaviorBuilder.create(
         p_259997_ -> p_259997_.group(
                  p_259997_.present(pLocationMemory),
                  p_259997_.absent(MemoryModuleType.ATTACK_TARGET),
                  p_259997_.absent(MemoryModuleType.WALK_TARGET),
                  p_259997_.registered(MemoryModuleType.LOOK_TARGET)
               )
               .apply(p_259997_, (p_259831_, p_259115_, p_259521_, p_259223_) -> (p_341305_, p_341306_, p_341307_) -> {
                     BlockPos $$7 = p_259997_.get(p_259831_);
                     boolean $$8 = $$7.closerThan(p_341306_.blockPosition(), (double)pCloseEnoughDist);
                     if (!$$8) {
                        BehaviorUtils.setWalkAndLookTargetMemories(p_341306_, getNearbyPos(p_341306_, $$7), pSpeedModifier, pCloseEnoughDist);
                     }

                     return true;
                  })
      );
   }
}
