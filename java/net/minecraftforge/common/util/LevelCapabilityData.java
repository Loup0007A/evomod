package net.minecraftforge.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

public class LevelCapabilityData extends SavedData {
   public static final String ID = "capabilities";
   private final INBTSerializable<CompoundTag> serializable;
   private CompoundTag capNBT = null;

   public LevelCapabilityData(@Nullable INBTSerializable<CompoundTag> serializable) {
      this.serializable = serializable;
   }

   @Override
   public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
      if (this.serializable != null) {
         nbt = this.serializable.serializeNBT(provider);
      }

      return nbt;
   }

   @Override
   public boolean isDirty() {
      return true;
   }

   public static LevelCapabilityData compute(DimensionDataStorage data, @Nullable INBTSerializable<CompoundTag> caps) {
      SavedData.Factory<LevelCapabilityData> factory = new SavedData.Factory<>(() -> new LevelCapabilityData(caps), (tag, lookup) -> {
         LevelCapabilityData ret = new LevelCapabilityData(caps);
         if (caps != null) {
            caps.deserializeNBT(lookup, tag);
         }

         return ret;
      }, null);
      return data.computeIfAbsent(factory, "capabilities");
   }
}
