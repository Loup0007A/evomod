package net.minecraftforge.common.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CapabilityDispatcher implements INBTSerializable<CompoundTag>, ICapabilityProvider {
   private final ICapabilityProvider[] caps;
   private final INBTSerializable<Tag>[] writers;
   private final String[] names;
   private final List<Runnable> listeners;

   public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list, List<Runnable> listeners) {
      this(list, listeners, null);
   }

   public CapabilityDispatcher(Map<ResourceLocation, ICapabilityProvider> list, List<Runnable> listeners, @Nullable ICapabilityProvider parent) {
      List<ICapabilityProvider> lstCaps = new ArrayList<>();
      List<INBTSerializable<Tag>> lstWriters = new ArrayList<>();
      List<String> lstNames = new ArrayList<>();
      this.listeners = listeners;
      if (parent != null) {
         lstCaps.add(parent);
         if (parent instanceof INBTSerializable) {
            lstWriters.add((INBTSerializable<Tag>)parent);
            lstNames.add("Parent");
         }
      }

      for (Entry<ResourceLocation, ICapabilityProvider> entry : list.entrySet()) {
         ICapabilityProvider prov = entry.getValue();
         lstCaps.add(prov);
         if (prov instanceof INBTSerializable) {
            lstWriters.add((INBTSerializable<Tag>)prov);
            lstNames.add(entry.getKey().toString());
         }
      }

      this.caps = lstCaps.toArray(new ICapabilityProvider[lstCaps.size()]);
      this.writers = lstWriters.toArray(new INBTSerializable[lstWriters.size()]);
      this.names = lstNames.toArray(new String[lstNames.size()]);
   }

   @Override
   public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
      for (ICapabilityProvider c : this.caps) {
         LazyOptional<T> ret = c.getCapability(cap, side);
         if (ret == null) {
            throw new RuntimeException(
               String.format(Locale.ENGLISH, "Provider %s.getCapability() returned null; return LazyOptional.empty() instead!", c.getClass().getTypeName())
            );
         }

         if (ret.isPresent()) {
            return ret;
         }
      }

      return LazyOptional.empty();
   }

   public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
      CompoundTag nbt = new CompoundTag();

      for (int x = 0; x < this.writers.length; x++) {
         nbt.put(this.names[x], this.writers[x].serializeNBT(registryAccess));
      }

      return nbt;
   }

   public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
      for (int x = 0; x < this.writers.length; x++) {
         if (nbt.contains(this.names[x])) {
            this.writers[x].deserializeNBT(registryAccess, nbt.get(this.names[x]));
         }
      }
   }

   public void invalidate() {
      this.listeners.forEach(Runnable::run);
   }
}
