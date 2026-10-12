package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove extends Behavior<Villager> {
   private long birthTimestamp;

   public VillagerMakeLove() {
      super(
         ImmutableMap.of(
            MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         ),
         350,
         350
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      return this.isBreedingPossible(pOwner);
   }

   protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return pGameTime <= this.birthTimestamp && this.isBreedingPossible(pEntity);
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      AgeableMob $$3 = pEntity.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(pEntity, $$3, 0.5F, 2);
      pLevel.broadcastEntityEvent($$3, (byte)18);
      pLevel.broadcastEntityEvent(pEntity, (byte)18);
      int $$4 = 275 + pEntity.getRandom().nextInt(50);
      this.birthTimestamp = pGameTime + (long)$$4;
   }

   protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      Villager $$3 = (Villager)pOwner.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
      if (!(pOwner.distanceToSqr($$3) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(pOwner, $$3, 0.5F, 2);
         if (pGameTime >= this.birthTimestamp) {
            pOwner.eatAndDigestFood();
            $$3.eatAndDigestFood();
            this.tryToGiveBirth(pLevel, pOwner, $$3);
         } else if (pOwner.getRandom().nextInt(35) == 0) {
            pLevel.broadcastEntityEvent($$3, (byte)12);
            pLevel.broadcastEntityEvent(pOwner, (byte)12);
         }
      }
   }

   private void tryToGiveBirth(ServerLevel pLevel, Villager pParent, Villager pPartner) {
      Optional<BlockPos> $$3 = this.takeVacantBed(pLevel, pParent);
      if ($$3.isEmpty()) {
         pLevel.broadcastEntityEvent(pPartner, (byte)13);
         pLevel.broadcastEntityEvent(pParent, (byte)13);
      } else {
         Optional<Villager> $$4 = this.breed(pLevel, pParent, pPartner);
         if ($$4.isPresent()) {
            this.giveBedToChild(pLevel, $$4.get(), $$3.get());
         } else {
            pLevel.getPoiManager().release($$3.get());
            DebugPackets.sendPoiTicketCountPacket(pLevel, $$3.get());
         }
      }
   }

   protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      pEntity.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
   }

   private boolean isBreedingPossible(Villager pVillager) {
      Brain<Villager> $$1 = pVillager.getBrain();
      Optional<AgeableMob> $$2 = $$1.getMemory(MemoryModuleType.BREED_TARGET).filter(p_341372_ -> p_341372_.getType() == EntityType.VILLAGER);
      return $$2.isEmpty()
         ? false
         : BehaviorUtils.targetIsValid($$1, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER) && pVillager.canBreed() && $$2.get().canBreed();
   }

   private Optional<BlockPos> takeVacantBed(ServerLevel pLevel, Villager pVillager) {
      return pLevel.getPoiManager()
         .take(
            p_217509_ -> p_217509_.is(PoiTypes.HOME), (p_217506_, p_217507_) -> this.canReach(pVillager, p_217507_, p_217506_), pVillager.blockPosition(), 48
         );
   }

   private boolean canReach(Villager pVillager, BlockPos pPos, Holder<PoiType> pPoiType) {
      Path $$3 = pVillager.getNavigation().createPath(pPos, pPoiType.value().validRange());
      return $$3 != null && $$3.canReach();
   }

   private Optional<Villager> breed(ServerLevel pLevel, Villager pParent, Villager pPartner) {
      Villager $$3 = pParent.getBreedOffspring(pLevel, pPartner);
      if ($$3 == null) {
         return Optional.empty();
      } else {
         pParent.setAge(6000);
         pPartner.setAge(6000);
         $$3.setAge(-24000);
         $$3.moveTo(pParent.getX(), pParent.getY(), pParent.getZ(), 0.0F, 0.0F);
         pLevel.addFreshEntityWithPassengers($$3);
         pLevel.broadcastEntityEvent($$3, (byte)12);
         return Optional.of($$3);
      }
   }

   private void giveBedToChild(ServerLevel pLevel, Villager pVillager, BlockPos pPos) {
      GlobalPos $$3 = GlobalPos.of(pLevel.dimension(), pPos);
      pVillager.getBrain().setMemory(MemoryModuleType.HOME, $$3);
   }
}
