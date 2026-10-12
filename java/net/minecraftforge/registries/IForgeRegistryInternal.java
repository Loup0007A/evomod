package net.minecraftforge.registries;

import net.minecraft.resources.ResourceLocation;

public interface IForgeRegistryInternal<V> extends IForgeRegistry<V> {
   <T> void setSlaveMap(IForgeRegistry.SlaveKey<T> var1, T var2);

   void register(int var1, ResourceLocation var2, V var3);

   V getValue(int var1);
}
