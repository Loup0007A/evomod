package net.minecraft.util.datafix;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ExtraDataFixUtils {
   public static Dynamic<?> fixBlockPos(Dynamic<?> pData) {
      Optional<Number> $$1 = pData.get("X").asNumber().result();
      Optional<Number> $$2 = pData.get("Y").asNumber().result();
      Optional<Number> $$3 = pData.get("Z").asNumber().result();
      return !$$1.isEmpty() && !$$2.isEmpty() && !$$3.isEmpty()
         ? pData.createIntList(IntStream.of($$1.get().intValue(), $$2.get().intValue(), $$3.get().intValue()))
         : pData;
   }

   public static <T, R> Typed<R> cast(Type<R> pType, Typed<T> pData) {
      return new Typed(pType, pData.getOps(), pData.getValue());
   }

   @SafeVarargs
   public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... pFilters) {
      return p_344666_ -> {
         for (Function<Typed<?>, Typed<?>> $$2 : pFilters) {
            p_344666_ = $$2.apply(p_344666_);
         }

         return p_344666_;
      };
   }
}
