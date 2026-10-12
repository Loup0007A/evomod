package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;

public class WorkAtPoi extends Behavior<Villager> {
   private static final int CHECK_COOLDOWN = 300;
   private static final double DISTANCE = 1.73;
   private long lastCheck;

   public WorkAtPoi() {
      super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      if (pLevel.getGameTime() - this.lastCheck < 300L) {
         return false;
      } else if (pLevel.random.nextInt(2) != 0) {
         return false;
      } else {
         this.lastCheck = pLevel.getGameTime();
         GlobalPos $$2 = pOwner.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
         return $$2.dimension() == pLevel.dimension() && $$2.pos().closerToCenterThan(pOwner.position(), 1.73);
      }
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      Brain<Villager> $$3 = pEntity.getBrain();
      $$3.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, pGameTime);
      $$3.getMemory(MemoryModuleType.JOB_SITE).ifPresent(p_24821_ -> $$3.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(p_24821_.pos())));
      pEntity.playWorkSound();
      this.useWorkstation(pLevel, pEntity);
      if (pEntity.shouldRestock()) {
         pEntity.restock();
      }
   }

   protected void useWorkstation(ServerLevel pLevel, Villager pVillager) {
   }

   protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      Optional<GlobalPos> $$3 = pEntity.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if ($$3.isEmpty()) {
         return false;
      } else {
         GlobalPos $$4 = $$3.get();
         return $$4.dimension() == pLevel.dimension() && $$4.pos().closerToCenterThan(pEntity.position(), 1.73);
      }
   }
}
