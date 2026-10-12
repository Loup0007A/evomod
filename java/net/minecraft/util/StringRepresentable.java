package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> pElementsSupplier) {
      return fromEnumWithMapping(pElementsSupplier, p_312201_ -> p_312201_);
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
      Supplier<E[]> pEnumValues, Function<String, String> pKeyFunction
   ) {
      E[] $$2 = (E[])pEnumValues.get();
      Function<String, E> $$3 = createNameLookup($$2, pKeyFunction);
      return new StringRepresentable.EnumCodec<>($$2, $$3);
   }

   static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> pValuesSupplier) {
      T[] $$1 = (T[])pValuesSupplier.get();
      Function<String, T> $$2 = createNameLookup($$1, p_308975_ -> p_308975_);
      ToIntFunction<T> $$3 = Util.createIndexLookup(Arrays.asList($$1));
      return new StringRepresentable.StringRepresentableCodec<>($$1, $$2, $$3);
   }

   static <T extends StringRepresentable> Function<String, T> createNameLookup(T[] pValues, Function<String, String> pKeyFunction) {
      if (pValues.length > 16) {
         Map<String, T> $$2 = Arrays.<StringRepresentable>stream(pValues)
            .collect(Collectors.toMap(p_308977_ -> pKeyFunction.apply(p_308977_.getSerializedName()), p_311743_ -> (T)p_311743_));
         return p_308974_ -> p_308974_ == null ? null : $$2.get(p_308974_);
      } else {
         return p_308972_ -> {
            for (T $$3 : pValues) {
               if (pKeyFunction.apply($$3.getSerializedName()).equals(p_308972_)) {
                  return $$3;
               }
            }

            return null;
         };
      }
   }

   static Keyable keys(final StringRepresentable[] pSerializables) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
            return Arrays.stream(pSerializables).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
         }
      };
   }

   @Deprecated
   public static class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
      private final Function<String, E> resolver;

      public EnumCodec(E[] pValues, Function<String, E> pResolver) {
         super(pValues, pResolver, p_216454_ -> p_216454_.ordinal());
         this.resolver = pResolver;
      }

      @Nullable
      public E byName(@Nullable String pName) {
         return this.resolver.apply(pName);
      }

      public E byName(@Nullable String pName, E pDefaultValue) {
         return Objects.requireNonNullElse(this.byName(pName), pDefaultValue);
      }
   }

   public static class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
      private final Codec<S> codec;

      public StringRepresentableCodec(S[] pValues, Function<String, S> pNameLookup, ToIntFunction<S> pIndexLookup) {
         this.codec = ExtraCodecs.orCompressed(
            Codec.stringResolver(StringRepresentable::getSerializedName, pNameLookup),
            ExtraCodecs.idResolverCodec(pIndexLookup, p_312747_ -> p_312747_ >= 0 && p_312747_ < pValues.length ? pValues[p_312747_] : null, -1)
         );
      }

      public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> pOps, T pValue) {
         return this.codec.decode(pOps, pValue);
      }

      public <T> DataResult<T> encode(S pInput, DynamicOps<T> pOps, T pPrefix) {
         return this.codec.encode(pInput, pOps, pPrefix);
      }
   }
}
