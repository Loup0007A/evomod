package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IForgeShearable;

public interface Shearable extends IForgeShearable {
   /** @deprecated */
   void shear(SoundSource pSource);

   /** @deprecated */
   boolean readyForShearing();

   @Override
   default boolean isShearable(ItemStack item, Level level, BlockPos pos) {
      return this.readyForShearing();
   }
}
