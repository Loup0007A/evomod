package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
   T apply(LootItemFunction.Builder pFunctionBuilder);

   default <E> T apply(Iterable<E> pBuilderSources, Function<E, LootItemFunction.Builder> pToBuilderFunction) {
      T $$2 = this.unwrap();

      for (E $$3 : pBuilderSources) {
         $$2 = $$2.apply(pToBuilderFunction.apply($$3));
      }

      return $$2;
   }

   default <E> T apply(E[] pBuilderSources, Function<E, LootItemFunction.Builder> pToBuilderFunction) {
      return this.apply(Arrays.asList(pBuilderSources), pToBuilderFunction);
   }

   T unwrap();
}
