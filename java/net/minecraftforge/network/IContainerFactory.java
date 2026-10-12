package net.minecraftforge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface IContainerFactory<T extends AbstractContainerMenu> extends MenuType.MenuSupplier<T> {
   T create(int var1, Inventory var2, FriendlyByteBuf var3);

   @Override
   default T create(int id, Inventory inv) {
      return this.create(id, inv, null);
   }
}
