package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.holdersets.ICustomHolderSet;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Holder<E>> elementCodec;
   private final Codec<List<Holder<E>>> homogenousListCodec;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;
   private final Codec<ICustomHolderSet<E>> forgeDispatchCodec;
   private final Codec<Either<ICustomHolderSet<E>, Either<TagKey<E>, List<Holder<E>>>>> combinedCodec;

   private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
      Codec<List<Holder<E>>> codec = pHolderCodec.listOf().validate(ExtraCodecs.ensureHomogenous(Holder::kind));
      return pDisallowInline
         ? codec
         : Codec.either(codec, pHolderCodec)
            .xmap(
               p_206664_ -> (List)p_206664_.map(p_206694_ -> p_206694_, List::of),
               p_206684_ -> p_206684_.size() == 1 ? Either.right((Holder)p_206684_.get(0)) : Either.left(p_206684_)
            );
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
      return new HolderSetCodec<>(pRegistryKey, pHolderCodec, pDisallowInline);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<Holder<E>> pElementCodec, boolean pDisallowInline) {
      this.registryKey = pRegistryKey;
      this.elementCodec = pElementCodec;
      this.homogenousListCodec = homogenousList(pElementCodec, pDisallowInline);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(pRegistryKey), this.homogenousListCodec);
      this.forgeDispatchCodec = Codec.lazyInitialized(() -> ForgeRegistries.HOLDER_SET_TYPES.get().getCodec())
         .dispatch(ICustomHolderSet::type, type -> type.makeCodec(pRegistryKey, pElementCodec, pDisallowInline));
      this.combinedCodec = Codec.either(this.forgeDispatchCodec, this.registryAwareCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
      if (pOps instanceof RegistryOps<T> registryops) {
         Optional<HolderGetter<E>> optional = registryops.getter(this.registryKey);
         if (optional.isPresent()) {
            HolderGetter<E> holdergetter = optional.get();
            return this.combinedCodec
               .decode(pOps, pInput)
               .flatMap(
                  p_326147_ -> {
                     DataResult<HolderSet<E>> dataresult = (DataResult<HolderSet<E>>)((Either)p_326147_.getFirst())
                        .map(
                           custom -> DataResult.success(custom),
                           tagOrList -> (DataResult)tagOrList.map(
                                 p_326145_ -> lookupTag(holdergetter, p_326145_), p_326140_ -> DataResult.success(HolderSet.direct(p_326140_))
                              )
                        );
                     return dataresult.map(p_326149_ -> Pair.of(p_326149_, p_326147_.getSecond()));
                  }
               );
         }
      }

      return this.decodeWithoutRegistry(pOps, pInput);
   }

   private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> pInput, TagKey<E> pTagKey) {
      return pInput.get(pTagKey)
         .<DataResult<HolderSet<E>>>map(DataResult::success)
         .orElseGet(() -> DataResult.error(() -> "Missing tag: '" + pTagKey.location() + "' in '" + pTagKey.registry().location() + "'"));
   }

   public <T> DataResult<T> encode(HolderSet<E> pInput, DynamicOps<T> pOps, T pPrefix) {
      if (pOps instanceof RegistryOps<T> registryops) {
         Optional<HolderOwner<E>> optional = registryops.owner(this.registryKey);
         if (optional.isPresent()) {
            if (!pInput.canSerializeIn(optional.get())) {
               return DataResult.error(() -> "HolderSet " + pInput + " is not valid in current registry set");
            }

            return this.registryAwareCodec.encode(pInput.unwrap().mapRight(List::copyOf), pOps, pPrefix);
         }
      }

      return this.encodeWithoutRegistry(pInput, pOps, pPrefix);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> pOps, T pInput) {
      return this.homogenousListCodec.decode(pOps, pInput).flatMap(p_206666_ -> {
         List<Holder.Direct<E>> list = new ArrayList<>();

         for (Holder<E> holder : (List)p_206666_.getFirst()) {
            if (!(holder instanceof Holder.Direct<E> direct)) {
               return DataResult.error(() -> "Can't decode element " + holder + " without registry");
            }

            list.add(direct);
         }

         return DataResult.success(new Pair(HolderSet.direct(list), p_206666_.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> pInput, DynamicOps<T> pOps, T pPrefix) {
      return pInput instanceof ICustomHolderSet<E> customHolderSet
         ? this.forgeDispatchCodec.encode(customHolderSet, pOps, pPrefix)
         : this.homogenousListCodec.encode(pInput.stream().toList(), pOps, pPrefix);
   }
}
