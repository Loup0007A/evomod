package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeLootItemCondition implements LootItemCondition {
   protected final List<LootItemCondition> terms;
   private final Predicate<LootContext> composedPredicate;

   protected CompositeLootItemCondition(List<LootItemCondition> pTerms, Predicate<LootContext> pComposedPredicate) {
      this.terms = pTerms;
      this.composedPredicate = pComposedPredicate;
   }

   protected static <T extends CompositeLootItemCondition> MapCodec<T> createCodec(Function<List<LootItemCondition>, T> pFactory) {
      return RecordCodecBuilder.mapCodec(
         p_342025_ -> p_342025_.group(LootItemCondition.DIRECT_CODEC.listOf().fieldOf("terms").forGetter(p_297812_ -> p_297812_.terms))
               .apply(p_342025_, pFactory)
      );
   }

   protected static <T extends CompositeLootItemCondition> Codec<T> createInlineCodec(Function<List<LootItemCondition>, T> pFactory) {
      return LootItemCondition.DIRECT_CODEC.listOf().xmap(pFactory, p_300100_ -> p_300100_.terms);
   }

   public final boolean test(LootContext pContext) {
      return this.composedPredicate.test(pContext);
   }

   @Override
   public void validate(ValidationContext pContext) {
      LootItemCondition.super.validate(pContext);

      for (int $$1 = 0; $$1 < this.terms.size(); $$1++) {
         this.terms.get($$1).validate(pContext.forChild(".term[" + $$1 + "]"));
      }
   }

   public abstract static class Builder implements LootItemCondition.Builder {
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> terms = ImmutableList.builder();

      protected Builder(LootItemCondition.Builder... pConditions) {
         for (LootItemCondition.Builder $$1 : pConditions) {
            this.terms.add($$1.build());
         }
      }

      public void addTerm(LootItemCondition.Builder pCondition) {
         this.terms.add(pCondition.build());
      }

      @Override
      public LootItemCondition build() {
         return this.create(this.terms.build());
      }

      protected abstract LootItemCondition create(List<LootItemCondition> pConditions);
   }
}
