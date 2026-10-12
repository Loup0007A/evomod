package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WorkAtComposter extends WorkAtPoi {
   private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

   @Override
   protected void useWorkstation(ServerLevel pLevel, Villager pVillager) {
      Optional<GlobalPos> $$2 = pVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
      if (!$$2.isEmpty()) {
         GlobalPos $$3 = $$2.get();
         BlockState $$4 = pLevel.getBlockState($$3.pos());
         if ($$4.is(Blocks.COMPOSTER)) {
            this.makeBread(pVillager);
            this.compostItems(pLevel, pVillager, $$3, $$4);
         }
      }
   }

   private void compostItems(ServerLevel pLevel, Villager pVillager, GlobalPos pGlobal, BlockState pState) {
      BlockPos $$4 = pGlobal.pos();
      if (pState.getValue(ComposterBlock.LEVEL) == 8) {
         pState = ComposterBlock.extractProduce(pVillager, pState, pLevel, $$4);
      }

      int $$5 = 20;
      int $$6 = 10;
      int[] $$7 = new int[COMPOSTABLE_ITEMS.size()];
      SimpleContainer $$8 = pVillager.getInventory();
      int $$9 = $$8.getContainerSize();
      BlockState $$10 = pState;

      for (int $$11 = $$9 - 1; $$11 >= 0 && $$5 > 0; $$11--) {
         ItemStack $$12 = $$8.getItem($$11);
         int $$13 = COMPOSTABLE_ITEMS.indexOf($$12.getItem());
         if ($$13 != -1) {
            int $$14 = $$12.getCount();
            int $$15 = $$7[$$13] + $$14;
            $$7[$$13] = $$15;
            int $$16 = Math.min(Math.min($$15 - 10, $$5), $$14);
            if ($$16 > 0) {
               $$5 -= $$16;

               for (int $$17 = 0; $$17 < $$16; $$17++) {
                  $$10 = ComposterBlock.insertItem(pVillager, $$10, pLevel, $$12, $$4);
                  if ($$10.getValue(ComposterBlock.LEVEL) == 7) {
                     this.spawnComposterFillEffects(pLevel, pState, $$4, $$10);
                     return;
                  }
               }
            }
         }
      }

      this.spawnComposterFillEffects(pLevel, pState, $$4, $$10);
   }

   private void spawnComposterFillEffects(ServerLevel pLevel, BlockState pPreState, BlockPos pPos, BlockState pPostState) {
      pLevel.levelEvent(1500, pPos, pPostState != pPreState ? 1 : 0);
   }

   private void makeBread(Villager pVillager) {
      SimpleContainer $$1 = pVillager.getInventory();
      if ($$1.countItem(Items.BREAD) <= 36) {
         int $$2 = $$1.countItem(Items.WHEAT);
         int $$3 = 3;
         int $$4 = 3;
         int $$5 = Math.min(3, $$2 / 3);
         if ($$5 != 0) {
            int $$6 = $$5 * 3;
            $$1.removeItemType(Items.WHEAT, $$6);
            ItemStack $$7 = $$1.addItem(new ItemStack(Items.BREAD, $$5));
            if (!$$7.isEmpty()) {
               pVillager.spawnAtLocation($$7, 0.5F);
            }
         }
      }
   }
}
