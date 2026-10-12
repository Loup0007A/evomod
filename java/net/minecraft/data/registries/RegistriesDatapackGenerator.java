package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

public class RegistriesDatapackGenerator implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;
   private final Predicate<String> namespacePredicate;

   public RegistriesDatapackGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
      this(pOutput, pRegistries, null);
   }

   public RegistriesDatapackGenerator(PackOutput p_256643_, CompletableFuture<HolderLookup.Provider> p_255780_, @Nullable Set<String> modIds) {
      this.registries = p_255780_;
      this.output = p_256643_;
      this.namespacePredicate = modIds == null ? namespace -> true : modIds::contains;
   }

   public CompletableFuture<HolderLookup.Provider> getRegistryProvider() {
      return this.registries;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput pOutput) {
      return this.registries
         .thenCompose(
            p_325918_ -> {
               DynamicOps<JsonElement> dynamicops = p_325918_.createSerializationContext(JsonOps.INSTANCE);
               return CompletableFuture.allOf(
                  RegistryDataLoader.getWorldGenAndDimensionStream()
                     .flatMap(p_256552_ -> this.dumpRegistryCap(pOutput, p_325918_, dynamicops, (RegistryDataLoader.RegistryData<?>)p_256552_).stream())
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
      CachedOutput pOutput, HolderLookup.Provider pRegistries, DynamicOps<JsonElement> pOps, RegistryDataLoader.RegistryData<T> pRegistryData
   ) {
      ResourceKey<? extends Registry<T>> resourcekey = pRegistryData.key();
      return pRegistries.lookup(resourcekey)
         .map(
            p_341079_ -> {
               PackOutput.PathProvider packoutput$pathprovider = this.output.createRegistryElementsPathProvider(resourcekey);
               return CompletableFuture.allOf(
                  p_341079_.listElements()
                     .filter(holder -> this.namespacePredicate.test(holder.key().location().getNamespace()))
                     .map(
                        p_256105_ -> dumpValue(
                              packoutput$pathprovider.json(p_256105_.key().location()), pOutput, pOps, pRegistryData.elementCodec(), p_256105_.value()
                           )
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   private static <E> CompletableFuture<?> dumpValue(Path pValuePath, CachedOutput pOutput, DynamicOps<JsonElement> pOps, Encoder<E> pEncoder, E pValue) {
      return (CompletableFuture<?>)pEncoder.encodeStart(pOps, pValue)
         .mapOrElse(
            p_341074_ -> DataProvider.saveStable(pOutput, p_341074_, pValuePath),
            p_341071_ -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + pValuePath + "': " + p_341071_.message()))
         );
   }

   @Override
   public String getName() {
      return "Registries";
   }
}
