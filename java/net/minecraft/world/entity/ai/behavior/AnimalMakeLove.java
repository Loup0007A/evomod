package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;

public class AnimalMakeLove extends Behavior<Animal> {
   private static final int BREED_RANGE = 3;
   private static final int MIN_DURATION = 60;
   private static final int MAX_DURATION = 110;
   private final EntityType<? extends Animal> partnerType;
   private final float speedModifier;
   private final int closeEnoughDistance;
   private static final int DEFAULT_CLOSE_ENOUGH_DISTANCE = 2;
   private long spawnChildAtTime;

   public AnimalMakeLove(EntityType<? extends Animal> pPartnerType) {
      this(pPartnerType, 1.0F, 2);
   }

   public AnimalMakeLove(EntityType<? extends Animal> pPartnerType, float pSpeedModifier, int pCloseEnoughDistance) {
      super(
         ImmutableMap.of(
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREED_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.IS_PANICKING,
            MemoryStatus.VALUE_ABSENT
         ),
         110
      );
      this.partnerType = pPartnerType;
      this.speedModifier = pSpeedModifier;
      this.closeEnoughDistance = pCloseEnoughDistance;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Animal pOwner) {
      return pOwner.isInLove() && this.findValidBreedPartner(pOwner).isPresent();
   }

   protected void start(ServerLevel pLevel, Animal pEntity, long pGameTime) {
      Animal $$3 = this.findValidBreedPartner(pEntity).get();
      pEntity.getBrain().setMemory(MemoryModuleType.BREED_TARGET, $$3);
      $$3.getBrain().setMemory(MemoryModuleType.BREED_TARGET, pEntity);
      BehaviorUtils.lockGazeAndWalkToEachOther(pEntity, $$3, this.speedModifier, this.closeEnoughDistance);
      int $$4 = 60 + pEntity.getRandom().nextInt(50);
      this.spawnChildAtTime = pGameTime + (long)$$4;
   }

   protected boolean canStillUse(ServerLevel pLevel, Animal pEntity, long pGameTime) {
      if (!this.hasBreedTargetOfRightType(pEntity)) {
         return false;
      } else {
         Animal $$3 = this.getBreedTarget(pEntity);
         return $$3.isAlive()
            && pEntity.canMate($$3)
            && BehaviorUtils.entityIsVisible(pEntity.getBrain(), $$3)
            && pGameTime <= this.spawnChildAtTime
            && !pEntity.isPanicking()
            && !$$3.isPanicking();
      }
   }

   protected void tick(ServerLevel pLevel, Animal pOwner, long pGameTime) {
      Animal $$3 = this.getBreedTarget(pOwner);
      BehaviorUtils.lockGazeAndWalkToEachOther(pOwner, $$3, this.speedModifier, this.closeEnoughDistance);
      if (pOwner.closerThan($$3, 3.0)) {
         if (pGameTime >= this.spawnChildAtTime) {
            pOwner.spawnChildFromBreeding(pLevel, $$3);
            pOwner.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            $$3.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
         }
      }
   }

   protected void stop(ServerLevel pLevel, Animal pEntity, long pGameTime) {
      pEntity.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
      pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      pEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      this.spawnChildAtTime = 0L;
   }

   private Animal getBreedTarget(Animal pAnimal) {
      return (Animal)pAnimal.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
   }

   private boolean hasBreedTargetOfRightType(Animal pAnimal) {
      Brain<?> $$1 = pAnimal.getBrain();
      return $$1.hasMemoryValue(MemoryModuleType.BREED_TARGET) && $$1.getMemory(MemoryModuleType.BREED_TARGET).get().getType() == this.partnerType;
   }

   private Optional<? extends Animal> findValidBreedPartner(Animal pAnimal) {
      return pAnimal.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().findClosest(p_341292_ -> {
         if (p_341292_.getType() == this.partnerType && p_341292_ instanceof Animal $$2 && pAnimal.canMate($$2) && !$$2.isPanicking()) {
            return true;
         }

         return false;
      }).map(Animal.class::cast);
   }
}
