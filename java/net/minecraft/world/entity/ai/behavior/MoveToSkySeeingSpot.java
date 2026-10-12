package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot {
   public static OneShot<LivingEntity> create(float pSpeedModifier) {
      return BehaviorBuilder.create(
         p_258543_ -> p_258543_.group(p_258543_.absent(MemoryModuleType.WALK_TARGET)).apply(p_258543_, p_258545_ -> (p_341348_, p_341349_, p_341350_) -> {
                  if (p_341348_.canSeeSky(p_341349_.blockPosition())) {
                     return false;
                  } else {
                     Optional<Vec3> $$5 = Optional.ofNullable(getOutdoorPosition(p_341348_, p_341349_));
                     $$5.ifPresent(p_258548_ -> p_258545_.set(new WalkTarget(p_258548_, pSpeedModifier, 0)));
                     return true;
                  }
               })
      );
   }

   @Nullable
   private static Vec3 getOutdoorPosition(ServerLevel pLevel, LivingEntity pEntity) {
      RandomSource $$2 = pEntity.getRandom();
      BlockPos $$3 = pEntity.blockPosition();

      for (int $$4 = 0; $$4 < 10; $$4++) {
         BlockPos $$5 = $$3.offset($$2.nextInt(20) - 10, $$2.nextInt(6) - 3, $$2.nextInt(20) - 10);
         if (hasNoBlocksAbove(pLevel, pEntity, $$5)) {
            return Vec3.atBottomCenterOf($$5);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(ServerLevel pLevel, LivingEntity pEntity, BlockPos pPos) {
      return pLevel.canSeeSky(pPos) && (double)pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pPos).getY() <= pEntity.getY();
   }
}
