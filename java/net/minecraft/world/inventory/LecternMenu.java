package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LecternMenu extends AbstractContainerMenu {
   private static final int DATA_COUNT = 1;
   private static final int SLOT_COUNT = 1;
   public static final int BUTTON_PREV_PAGE = 1;
   public static final int BUTTON_NEXT_PAGE = 2;
   public static final int BUTTON_TAKE_BOOK = 3;
   public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
   private final Container lectern;
   private final ContainerData lecternData;

   public LecternMenu(int pContainerId) {
      this(pContainerId, new SimpleContainer(1), new SimpleContainerData(1));
   }

   public LecternMenu(int pContainerId, Container pLectern, ContainerData pLecternData) {
      super(MenuType.LECTERN, pContainerId);
      checkContainerSize(pLectern, 1);
      checkContainerDataCount(pLecternData, 1);
      this.lectern = pLectern;
      this.lecternData = pLecternData;
      this.addSlot(new Slot(pLectern, 0, 0, 0) {
         @Override
         public void setChanged() {
            super.setChanged();
            LecternMenu.this.slotsChanged(this.container);
         }
      });
      this.addDataSlots(pLecternData);
   }

   @Override
   public boolean clickMenuButton(Player pPlayer, int pId) {
      if (pId >= 100) {
         int $$2 = pId - 100;
         this.setData(0, $$2);
         return true;
      } else {
         switch (pId) {
            case 1:
               int $$4 = this.lecternData.get(0);
               this.setData(0, $$4 - 1);
               return true;
            case 2:
               int $$3 = this.lecternData.get(0);
               this.setData(0, $$3 + 1);
               return true;
            case 3:
               if (!pPlayer.mayBuild()) {
                  return false;
               }

               ItemStack $$5 = this.lectern.removeItemNoUpdate(0);
               this.lectern.setChanged();
               if (!pPlayer.getInventory().add($$5)) {
                  pPlayer.drop($$5, false);
               }

               return true;
            default:
               return false;
         }
      }
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      return ItemStack.EMPTY;
   }

   @Override
   public void setData(int pId, int pData) {
      super.setData(pId, pData);
      this.broadcastChanges();
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.lectern.stillValid(pPlayer);
   }

   public ItemStack getBook() {
      return this.lectern.getItem(0);
   }

   public int getPage() {
      return this.lecternData.get(0);
   }
}
