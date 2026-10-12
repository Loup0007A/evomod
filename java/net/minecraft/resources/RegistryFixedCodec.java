package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;

public final class RegistryFixedCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;

   public static <E> RegistryFixedCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey) {
      return new RegistryFixedCodec<>(pRegistryKey);
   }

   private RegistryFixedCodec(ResourceKey<? extends Registry<E>> pRegistryKey) {
      this.registryKey = pRegistryKey;
   }

   public <T> DataResult<T> encode(Holder<E> pHolder, DynamicOps<T> pOps, T pValue) {
      if (pOps instanceof RegistryOps<?> $$3) {
         Optional<HolderOwner<E>> $$4 = $$3.owner(this.registryKey);
         if ($$4.isPresent()) {
            if (!pHolder.canSerializeIn($$4.get())) {
               return DataResult.error(() -> "Element " + pHolder + " is not valid in current registry set");
            }

            return (DataResult<T>)pHolder.unwrap()
               .map(
                  p_206727_ -> ResourceLocation.CODEC.encode(p_206727_.location(), pOps, pValue),
                  p_274804_ -> DataResult.error(() -> "Elements from registry " + this.registryKey + " can't be serialized to a value")
               );
         }
      }

      return DataResult.error(() -> "Can't access registry " + this.registryKey);
   }

   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> pOps, T pValue) {
      if (pOps instanceof RegistryOps<?> $$2) {
         Optional<HolderGetter<E>> $$3 = $$2.getter(this.registryKey);
         if ($$3.isPresent()) {
            return ResourceLocation.CODEC
               .decode(pOps, pValue)
               .flatMap(
                  p_326174_ -> {
                     ResourceLocation $$2x = (ResourceLocation)p_326174_.getFirst();
                     return $$3.get()
                        .get(ResourceKey.create(this.registryKey, $$2x))
                        .<DataResult>map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Failed to get element " + $$2x))
                        .map(p_256041_ -> Pair.of(p_256041_, p_326174_.getSecond()))
                        .setLifecycle(Lifecycle.stable());
                  }
               );
         }
      }

      return DataResult.error(() -> "Can't access registry " + this.registryKey);
   }

   @Override
   public String toString() {
      return "RegistryFixedCodec[" + this.registryKey + "]";
   }
}
