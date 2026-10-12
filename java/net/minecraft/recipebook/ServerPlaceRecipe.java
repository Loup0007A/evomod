package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<I extends RecipeInput, R extends Recipe<I>> implements PlaceRecipe<Integer> {
   private static final int ITEM_NOT_FOUND = -1;
   protected final StackedContents stackedContents = new StackedContents();
   protected Inventory inventory;
   protected RecipeBookMenu<I, R> menu;

   public ServerPlaceRecipe(RecipeBookMenu<I, R> pMenu) {
      this.menu = pMenu;
   }

   public void recipeClicked(ServerPlayer pPlayer, @Nullable RecipeHolder<R> pRecipe, boolean pPlaceAll) {
      if (pRecipe != null && pPlayer.getRecipeBook().contains(pRecipe)) {
         this.inventory = pPlayer.getInventory();
         if (this.testClearGrid() || pPlayer.isCreative()) {
            this.stackedContents.clear();
            pPlayer.getInventory().fillStackedContents(this.stackedContents);
            this.menu.fillCraftSlotsStackedContents(this.stackedContents);
            if (this.stackedContents.canCraft(pRecipe.value(), null)) {
               this.handleRecipeClicked(pRecipe, pPlaceAll);
            } else {
               this.clearGrid();
               pPlayer.connection.send(new ClientboundPlaceGhostRecipePacket(pPlayer.containerMenu.containerId, pRecipe));
            }

            pPlayer.getInventory().setChanged();
         }
      }
   }

   protected void clearGrid() {
      for (int $$0 = 0; $$0 < this.menu.getSize(); $$0++) {
         if (this.menu.shouldMoveToInventory($$0)) {
            ItemStack $$1 = this.menu.getSlot($$0).getItem().copy();
            this.inventory.placeItemBackInInventory($$1, false);
            this.menu.getSlot($$0).set($$1);
         }
      }

      this.menu.clearCraftingContent();
   }

   protected void handleRecipeClicked(RecipeHolder<R> pRecipe, boolean pPlaceAll) {
      boolean $$2 = this.menu.recipeMatches(pRecipe);
      int $$3 = this.stackedContents.getBiggestCraftableStack(pRecipe, null);
      if ($$2) {
         for (int $$4 = 0; $$4 < this.menu.getGridHeight() * this.menu.getGridWidth() + 1; $$4++) {
            if ($$4 != this.menu.getResultSlotIndex()) {
               ItemStack $$5 = this.menu.getSlot($$4).getItem();
               if (!$$5.isEmpty() && Math.min($$3, $$5.getMaxStackSize()) < $$5.getCount() + 1) {
                  return;
               }
            }
         }
      }

      int $$6 = this.getStackSize(pPlaceAll, $$3, $$2);
      IntList $$7 = new IntArrayList();
      if (this.stackedContents.canCraft(pRecipe.value(), $$7, $$6)) {
         int $$8 = $$6;
         IntListIterator var8 = $$7.iterator();

         while (var8.hasNext()) {
            int $$9 = (Integer)var8.next();
            ItemStack $$10 = StackedContents.fromStackingIndex($$9);
            if (!$$10.isEmpty()) {
               int $$11 = $$10.getMaxStackSize();
               if ($$11 < $$8) {
                  $$8 = $$11;
               }
            }
         }

         if (this.stackedContents.canCraft(pRecipe.value(), $$7, $$8)) {
            this.clearGrid();
            this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), pRecipe, $$7.iterator(), $$8);
         }
      }
   }

   public void addItemToSlot(Integer pItem, int pSlot, int pMaxAmount, int pX, int pY) {
      Slot $$5 = this.menu.getSlot(pSlot);
      ItemStack $$6 = StackedContents.fromStackingIndex(pItem);
      if (!$$6.isEmpty()) {
         int $$7 = pMaxAmount;

         while ($$7 > 0) {
            $$7 = this.moveItemToGrid($$5, $$6, $$7);
            if ($$7 == -1) {
               return;
            }
         }
      }
   }

   protected int getStackSize(boolean pPlaceAll, int pMaxPossible, boolean pRecipeMatches) {
      int $$3 = 1;
      if (pPlaceAll) {
         $$3 = pMaxPossible;
      } else if (pRecipeMatches) {
         $$3 = Integer.MAX_VALUE;

         for (int $$4 = 0; $$4 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; $$4++) {
            if ($$4 != this.menu.getResultSlotIndex()) {
               ItemStack $$5 = this.menu.getSlot($$4).getItem();
               if (!$$5.isEmpty() && $$3 > $$5.getCount()) {
                  $$3 = $$5.getCount();
               }
            }
         }

         if ($$3 != Integer.MAX_VALUE) {
            $$3++;
         }
      }

      return $$3;
   }

   protected int moveItemToGrid(Slot pSlot, ItemStack pStack, int pMaxAmount) {
      int $$3 = this.inventory.findSlotMatchingUnusedItem(pStack);
      if ($$3 == -1) {
         return -1;
      } else {
         ItemStack $$4 = this.inventory.getItem($$3);
         int $$5;
         if (pMaxAmount < $$4.getCount()) {
            this.inventory.removeItem($$3, pMaxAmount);
            $$5 = pMaxAmount;
         } else {
            this.inventory.removeItemNoUpdate($$3);
            $$5 = $$4.getCount();
         }

         if (pSlot.getItem().isEmpty()) {
            pSlot.set($$4.copyWithCount($$5));
         } else {
            pSlot.getItem().grow($$5);
         }

         return pMaxAmount - $$5;
      }
   }

   private boolean testClearGrid() {
      List<ItemStack> $$0 = Lists.newArrayList();
      int $$1 = this.getAmountOfFreeSlotsInInventory();

      for (int $$2 = 0; $$2 < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; $$2++) {
         if ($$2 != this.menu.getResultSlotIndex()) {
            ItemStack $$3 = this.menu.getSlot($$2).getItem().copy();
            if (!$$3.isEmpty()) {
               int $$4 = this.inventory.getSlotWithRemainingSpace($$3);
               if ($$4 == -1 && $$0.size() <= $$1) {
                  for (ItemStack $$5 : $$0) {
                     if (ItemStack.isSameItem($$5, $$3) && $$5.getCount() != $$5.getMaxStackSize() && $$5.getCount() + $$3.getCount() <= $$5.getMaxStackSize()) {
                        $$5.grow($$3.getCount());
                        $$3.setCount(0);
                        break;
                     }
                  }

                  if (!$$3.isEmpty()) {
                     if ($$0.size() >= $$1) {
                        return false;
                     }

                     $$0.add($$3);
                  }
               } else if ($$4 == -1) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private int getAmountOfFreeSlotsInInventory() {
      int $$0 = 0;

      for (ItemStack $$1 : this.inventory.items) {
         if ($$1.isEmpty()) {
            $$0++;
         }
      }

      return $$0;
   }
}
