package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal extends Behavior<Villager> {
   private static final int BONEMEALING_DURATION = 80;
   private long nextWorkCycleTime;
   private long lastBonemealingSession;
   private int timeWorkedSoFar;
   private Optional<BlockPos> cropPos = Optional.empty();

   public UseBonemeal() {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      if (pOwner.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= (long)pOwner.tickCount)) {
         if (pOwner.getInventory().countItem(Items.BONE_MEAL) <= 0) {
            return false;
         } else {
            this.cropPos = this.pickNextTarget(pLevel, pOwner);
            return this.cropPos.isPresent();
         }
      } else {
         return false;
      }
   }

   protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
   }

   private Optional<BlockPos> pickNextTarget(ServerLevel pLevel, Villager pVillager) {
      BlockPos.MutableBlockPos $$2 = new BlockPos.MutableBlockPos();
      Optional<BlockPos> $$3 = Optional.empty();
      int $$4 = 0;

      for (int $$5 = -1; $$5 <= 1; $$5++) {
         for (int $$6 = -1; $$6 <= 1; $$6++) {
            for (int $$7 = -1; $$7 <= 1; $$7++) {
               $$2.setWithOffset(pVillager.blockPosition(), $$5, $$6, $$7);
               if (this.validPos($$2, pLevel)) {
                  if (pLevel.random.nextInt(++$$4) == 0) {
                     $$3 = Optional.of($$2.immutable());
                  }
               }
            }
         }
      }

      return $$3;
   }

   private boolean validPos(BlockPos pPos, ServerLevel pLevel) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      Block $$3 = $$2.getBlock();
      return $$3 instanceof CropBlock && !((CropBlock)$$3).isMaxAge($$2);
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      this.setCurrentCropAsTarget(pEntity);
      pEntity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
      this.nextWorkCycleTime = pGameTime;
      this.timeWorkedSoFar = 0;
   }

   private void setCurrentCropAsTarget(Villager pVillager) {
      this.cropPos.ifPresent(p_24484_ -> {
         BlockPosTracker $$2 = new BlockPosTracker(p_24484_);
         pVillager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, $$2);
         pVillager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget($$2, 0.5F, 1));
      });
   }

   protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      pEntity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      this.lastBonemealingSession = (long)pEntity.tickCount;
   }

   protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      BlockPos $$3 = this.cropPos.get();
      if (pGameTime >= this.nextWorkCycleTime && $$3.closerToCenterThan(pOwner.position(), 1.0)) {
         ItemStack $$4 = ItemStack.EMPTY;
         SimpleContainer $$5 = pOwner.getInventory();
         int $$6 = $$5.getContainerSize();

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            ItemStack $$8 = $$5.getItem($$7);
            if ($$8.is(Items.BONE_MEAL)) {
               $$4 = $$8;
               break;
            }
         }

         if (!$$4.isEmpty() && BoneMealItem.growCrop($$4, pLevel, $$3)) {
            pLevel.levelEvent(1505, $$3, 15);
            this.cropPos = this.pickNextTarget(pLevel, pOwner);
            this.setCurrentCropAsTarget(pOwner);
            this.nextWorkCycleTime = pGameTime + 40L;
         }

         this.timeWorkedSoFar++;
      }
   }
}
