package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentPredicate implements Predicate<DataComponentMap> {
   public static final Codec<DataComponentPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
      .xmap(
         p_336043_ -> new DataComponentPredicate(p_336043_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
         p_335229_ -> p_335229_.expectedComponents
               .stream()
               .filter(p_332263_ -> !p_332263_.type().isTransient())
               .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(DataComponentPredicate::new, p_334923_ -> p_334923_.expectedComponents);
   public static final DataComponentPredicate EMPTY = new DataComponentPredicate(List.of());
   private final List<TypedDataComponent<?>> expectedComponents;

   DataComponentPredicate(List<TypedDataComponent<?>> p_328927_) {
      this.expectedComponents = p_328927_;
   }

   public static DataComponentPredicate.Builder builder() {
      return new DataComponentPredicate.Builder();
   }

   public static DataComponentPredicate allOf(DataComponentMap pExpectedComponents) {
      return new DataComponentPredicate(ImmutableList.copyOf(pExpectedComponents));
   }

   @Override
   public boolean equals(Object pOther) {
      if (pOther instanceof DataComponentPredicate $$1 && this.expectedComponents.equals($$1.expectedComponents)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.expectedComponents.hashCode();
   }

   @Override
   public String toString() {
      return this.expectedComponents.toString();
   }

   public boolean test(DataComponentMap pComponents) {
      for (TypedDataComponent<?> $$1 : this.expectedComponents) {
         Object $$2 = pComponents.get($$1.type());
         if (!Objects.equals($$1.value(), $$2)) {
            return false;
         }
      }

      return true;
   }

   public boolean test(DataComponentHolder pComponents) {
      return this.test(pComponents.getComponents());
   }

   public boolean alwaysMatches() {
      return this.expectedComponents.isEmpty();
   }

   public DataComponentPatch asPatch() {
      DataComponentPatch.Builder $$0 = DataComponentPatch.builder();

      for (TypedDataComponent<?> $$1 : this.expectedComponents) {
         $$0.set($$1);
      }

      return $$0.build();
   }

   public static class Builder {
      private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

      Builder() {
      }

      public <T> DataComponentPredicate.Builder expect(DataComponentType<? super T> pComponent, T pValue) {
         for (TypedDataComponent<?> $$2 : this.expectedComponents) {
            if ($$2.type() == pComponent) {
               throw new IllegalArgumentException("Predicate already has component of type: '" + pComponent + "'");
            }
         }

         this.expectedComponents.add(new TypedDataComponent<>(pComponent, pValue));
         return this;
      }

      public DataComponentPredicate build() {
         return new DataComponentPredicate(List.copyOf(this.expectedComponents));
      }
   }
}
