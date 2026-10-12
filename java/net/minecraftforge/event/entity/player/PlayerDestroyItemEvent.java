package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDestroyItemEvent extends PlayerEvent {
   @NotNull
   private final ItemStack original;
   @Nullable
   private final EquipmentSlot slot;

   public PlayerDestroyItemEvent(Player player, @NotNull ItemStack original, @Nullable EquipmentSlot slot) {
      super(player);
      this.original = original;
      this.slot = slot;
   }

   @NotNull
   public ItemStack getOriginal() {
      return this.original;
   }

   @Nullable
   public EquipmentSlot getSlot() {
      return this.slot;
   }
}
