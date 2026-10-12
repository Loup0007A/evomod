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

public final class RegistryFileCodec<E> implements Codec<Holder<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<E> elementCodec;
   private final boolean allowInline;

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      return create(pRegistryKey, pElementCodec, true);
   }

   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
      return new RegistryFileCodec<>(pRegistryKey, pElementCodec, pAllowInline);
   }

   private RegistryFileCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
      this.registryKey = pRegistryKey;
      this.elementCodec = pElementCodec;
      this.allowInline = pAllowInline;
   }

   public <T> DataResult<T> encode(Holder<E> pInput, DynamicOps<T> pOps, T pPrefix) {
      if (pOps instanceof RegistryOps<?> $$3) {
         Optional<HolderOwner<E>> $$4 = $$3.owner(this.registryKey);
         if ($$4.isPresent()) {
            if (!pInput.canSerializeIn($$4.get())) {
               return DataResult.error(() -> "Element " + pInput + " is not valid in current registry set");
            }

            return (DataResult<T>)pInput.unwrap()
               .map(
                  p_206714_ -> ResourceLocation.CODEC.encode(p_206714_.location(), pOps, pPrefix),
                  p_206710_ -> this.elementCodec.encode(p_206710_, pOps, pPrefix)
               );
         }
      }

      return this.elementCodec.encode(pInput.value(), pOps, pPrefix);
   }

   public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
      if (pOps instanceof RegistryOps<?> $$2) {
         Optional<HolderGetter<E>> $$3 = $$2.getter(this.registryKey);
         if ($$3.isEmpty()) {
            return DataResult.error(() -> "Registry does not exist: " + this.registryKey);
         } else {
            HolderGetter<E> $$4 = $$3.get();
            DataResult<Pair<ResourceLocation, T>> $$5 = ResourceLocation.CODEC.decode(pOps, pInput);
            if ($$5.result().isEmpty()) {
               return !this.allowInline
                  ? DataResult.error(() -> "Inline definitions not allowed here")
                  : this.elementCodec.decode(pOps, pInput).map(p_206720_ -> p_206720_.mapFirst(Holder::direct));
            } else {
               Pair<ResourceLocation, T> $$6 = (Pair<ResourceLocation, T>)$$5.result().get();
               ResourceKey<E> $$7 = ResourceKey.create(this.registryKey, (ResourceLocation)$$6.getFirst());
               return $$4.get($$7)
                  .<DataResult>map(DataResult::success)
                  .orElseGet(() -> DataResult.error(() -> "Failed to get element " + $$7))
                  .map(p_255658_ -> Pair.of(p_255658_, $$6.getSecond()))
                  .setLifecycle(Lifecycle.stable());
            }
         }
      } else {
         return this.elementCodec.decode(pOps, pInput).map(p_214212_ -> p_214212_.mapFirst(Holder::direct));
      }
   }

   @Override
   public String toString() {
      return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
   }
}
