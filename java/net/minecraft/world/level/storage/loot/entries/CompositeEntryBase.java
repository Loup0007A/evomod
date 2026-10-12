package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class CompositeEntryBase extends LootPoolEntryContainer {
   protected final List<LootPoolEntryContainer> children;
   private final ComposableEntryContainer composedChildren;

   protected CompositeEntryBase(List<LootPoolEntryContainer> pChildren, List<LootItemCondition> pConditions) {
      super(pConditions);
      this.children = pChildren;
      this.composedChildren = this.compose(pChildren);
   }

   @Override
   public void validate(ValidationContext pValidationContext) {
      super.validate(pValidationContext);
      if (this.children.isEmpty()) {
         pValidationContext.reportProblem("Empty children list");
      }

      for (int $$1 = 0; $$1 < this.children.size(); $$1++) {
         this.children.get($$1).validate(pValidationContext.forChild(".entry[" + $$1 + "]"));
      }
   }

   protected abstract ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren);

   @Override
   public final boolean expand(LootContext pLootContext, Consumer<LootPoolEntry> pEntryConsumer) {
      return !this.canRun(pLootContext) ? false : this.composedChildren.expand(pLootContext, pEntryConsumer);
   }

   public static <T extends CompositeEntryBase> MapCodec<T> createCodec(CompositeEntryBase.CompositeEntryConstructor<T> pFactory) {
      return RecordCodecBuilder.mapCodec(
         p_327559_ -> p_327559_.group(LootPoolEntries.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(p_300130_ -> p_300130_.children))
               .and(commonFields(p_327559_).t1())
               .apply(p_327559_, pFactory::create)
      );
   }

   @FunctionalInterface
   public interface CompositeEntryConstructor<T extends CompositeEntryBase> {
      T create(List<LootPoolEntryContainer> pChildren, List<LootItemCondition> pConditions);
   }
}
