package net.minecraftforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/** @deprecated */
public interface INBTSerializable<T extends Tag> {
   T serializeNBT(HolderLookup.Provider var1);

   void deserializeNBT(HolderLookup.Provider var1, T var2);
}
