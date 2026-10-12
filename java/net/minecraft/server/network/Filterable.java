package net.minecraft.server.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Filterable<T>(T raw, Optional<T> filtered) {
   public static <T> Codec<Filterable<T>> codec(Codec<T> pCodec) {
      Codec<Filterable<T>> $$1 = RecordCodecBuilder.create(
         p_328042_ -> p_328042_.group(pCodec.fieldOf("raw").forGetter(Filterable::raw), pCodec.optionalFieldOf("filtered").forGetter(Filterable::filtered))
               .apply(p_328042_, Filterable::new)
      );
      Codec<Filterable<T>> $$2 = pCodec.xmap(Filterable::passThrough, Filterable::raw);
      return Codec.withAlternative($$1, $$2);
   }

   public static <B extends ByteBuf, T> StreamCodec<B, Filterable<T>> streamCodec(StreamCodec<B, T> pCodec) {
      return StreamCodec.composite(pCodec, Filterable::raw, pCodec.apply(ByteBufCodecs::optional), Filterable::filtered, Filterable::new);
   }

   public static <T> Filterable<T> passThrough(T p_333360_) {
      return new Filterable<>(p_333360_, Optional.empty());
   }

   public static Filterable<String> from(FilteredText pFilteredText) {
      return new Filterable<>(pFilteredText.raw(), pFilteredText.isFiltered() ? Optional.of(pFilteredText.filteredOrEmpty()) : Optional.empty());
   }

   public T get(boolean pFiltered) {
      return pFiltered ? this.filtered.orElse(this.raw) : this.raw;
   }

   public <U> Filterable<U> map(Function<T, U> pMappingFunction) {
      return new Filterable<>(pMappingFunction.apply(this.raw), this.filtered.map(pMappingFunction));
   }

   public <U> Optional<Filterable<U>> resolve(Function<T, Optional<U>> pResolver) {
      Optional<U> $$1 = pResolver.apply(this.raw);
      if ($$1.isEmpty()) {
         return Optional.empty();
      } else if (this.filtered.isPresent()) {
         Optional<U> $$2 = pResolver.apply(this.filtered.get());
         return $$2.isEmpty() ? Optional.empty() : Optional.of(new Filterable<>($$1.get(), $$2));
      } else {
         return Optional.of(new Filterable<>($$1.get(), Optional.empty()));
      }
   }
}
