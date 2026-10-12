package net.minecraftforge.common.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;

public class DatapackBuiltinEntriesProvider extends RegistriesDatapackGenerator {
   private final CompletableFuture<HolderLookup.Provider> fullRegistries;

   public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries, Set<String> modIds) {
      super(output, registries.thenApply(RegistrySetBuilder.PatchedRegistries::patches), modIds);
      this.fullRegistries = registries.thenApply(RegistrySetBuilder.PatchedRegistries::full);
   }

   public DatapackBuiltinEntriesProvider(
      PackOutput output, CompletableFuture<HolderLookup.Provider> registries, RegistrySetBuilder registryBuilder, Set<String> modIds
   ) {
      this(output, RegistryPatchGenerator.createLookup(registries, registryBuilder), modIds);
   }

   public CompletableFuture<HolderLookup.Provider> getFullRegistries() {
      return this.fullRegistries;
   }
}
