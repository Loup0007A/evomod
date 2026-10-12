package net.minecraft.world.level.saveddata;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import org.slf4j.Logger;

public abstract class SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean dirty;

   public abstract CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries);

   public void setDirty() {
      this.setDirty(true);
   }

   public void setDirty(boolean pDirty) {
      this.dirty = pDirty;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void save(File pFile, HolderLookup.Provider pRegistries) {
      if (this.isDirty()) {
         CompoundTag $$2 = new CompoundTag();
         $$2.put("data", this.save(new CompoundTag(), pRegistries));
         NbtUtils.addCurrentDataVersion($$2);

         try {
            NbtIo.writeCompressed($$2, pFile.toPath());
         } catch (IOException var5) {
            LOGGER.error("Could not save data {}", this, var5);
         }

         this.setDirty(false);
      }
   }

   public static record Factory<T extends SavedData>(Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer, DataFixTypes type) {
   }
}
