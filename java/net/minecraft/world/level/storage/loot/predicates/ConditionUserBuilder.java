package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
   T when(LootItemCondition.Builder pConditionBuilder);

   default <E> T when(Iterable<E> pBuilderSources, Function<E, LootItemCondition.Builder> pToBuilderFunction) {
      T $$2 = this.unwrap();

      for (E $$3 : pBuilderSources) {
         $$2 = $$2.when(pToBuilderFunction.apply($$3));
      }

      return $$2;
   }

   T unwrap();
}
