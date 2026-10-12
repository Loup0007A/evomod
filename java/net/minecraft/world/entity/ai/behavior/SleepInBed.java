package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SleepInBed extends Behavior<LivingEntity> {
   public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
   private long nextOkStartTime;

   public SleepInBed() {
      super(ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryStatus.REGISTERED));
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      if (pOwner.isPassenger()) {
         return false;
      } else {
         Brain<?> $$2 = pOwner.getBrain();
         GlobalPos $$3 = $$2.getMemory(MemoryModuleType.HOME).get();
         if (pLevel.dimension() != $$3.dimension()) {
            return false;
         } else {
            Optional<Long> $$4 = $$2.getMemory(MemoryModuleType.LAST_WOKEN);
            if ($$4.isPresent()) {
               long $$5 = pLevel.getGameTime() - $$4.get();
               if ($$5 > 0L && $$5 < 100L) {
                  return false;
               }
            }

            BlockState $$6 = pLevel.getBlockState($$3.pos());
            return $$3.pos().closerToCenterThan(pOwner.position(), 2.0) && $$6.is(BlockTags.BEDS) && !$$6.getValue(BedBlock.OCCUPIED);
         }
      }
   }

   @Override
   protected boolean canStillUse(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Optional<GlobalPos> $$3 = pEntity.getBrain().getMemory(MemoryModuleType.HOME);
      if ($$3.isEmpty()) {
         return false;
      } else {
         BlockPos $$4 = $$3.get().pos();
         return pEntity.getBrain().isActive(Activity.REST) && pEntity.getY() > (double)$$4.getY() + 0.4 && $$4.closerToCenterThan(pEntity.position(), 1.14);
      }
   }

   @Override
   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      if (pGameTime > this.nextOkStartTime) {
         Brain<?> $$3 = pEntity.getBrain();
         if ($$3.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
            Set<GlobalPos> $$4 = $$3.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
            Optional<List<LivingEntity>> $$5;
            if ($$3.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)) {
               $$5 = $$3.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
            } else {
               $$5 = Optional.empty();
            }

            InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(pLevel, pEntity, null, null, $$4, $$5);
         }

         pEntity.startSleeping(pEntity.getBrain().getMemory(MemoryModuleType.HOME).get().pos());
      }
   }

   @Override
   protected boolean timedOut(long pGameTime) {
      return false;
   }

   @Override
   protected void stop(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      if (pEntity.isSleeping()) {
         pEntity.stopSleeping();
         this.nextOkStartTime = pGameTime + 40L;
      }
   }
}
