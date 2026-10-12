package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;

public class PlayerEnderChestContainer extends SimpleContainer {
   @Nullable
   private EnderChestBlockEntity activeChest;

   public PlayerEnderChestContainer() {
      super(27);
   }

   public void setActiveChest(EnderChestBlockEntity pEnderChestBlockEntity) {
      this.activeChest = pEnderChestBlockEntity;
   }

   public boolean isActiveChest(EnderChestBlockEntity pEnderChest) {
      return this.activeChest == pEnderChest;
   }

   @Override
   public void fromTag(ListTag pTag, HolderLookup.Provider pLevelRegistry) {
      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         this.setItem($$2, ItemStack.EMPTY);
      }

      for (int $$3 = 0; $$3 < pTag.size(); $$3++) {
         CompoundTag $$4 = pTag.getCompound($$3);
         int $$5 = $$4.getByte("Slot") & 255;
         if ($$5 >= 0 && $$5 < this.getContainerSize()) {
            this.setItem($$5, ItemStack.parse(pLevelRegistry, $$4).orElse(ItemStack.EMPTY));
         }
      }
   }

   @Override
   public ListTag createTag(HolderLookup.Provider pLevelRegistry) {
      ListTag $$1 = new ListTag();

      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         ItemStack $$3 = this.getItem($$2);
         if (!$$3.isEmpty()) {
            CompoundTag $$4 = new CompoundTag();
            $$4.putByte("Slot", (byte)$$2);
            $$1.add($$3.save(pLevelRegistry, $$4));
         }
      }

      return $$1;
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.activeChest != null && !this.activeChest.stillValid(pPlayer) ? false : super.stillValid(pPlayer);
   }

   @Override
   public void startOpen(Player pPlayer) {
      if (this.activeChest != null) {
         this.activeChest.startOpen(pPlayer);
      }

      super.startOpen(pPlayer);
   }

   @Override
   public void stopOpen(Player pPlayer) {
      if (this.activeChest != null) {
         this.activeChest.stopOpen(pPlayer);
      }

      super.stopOpen(pPlayer);
      this.activeChest = null;
   }
}
