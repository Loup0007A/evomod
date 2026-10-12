package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SequentialEntry extends CompositeEntryBase {
   public static final MapCodec<SequentialEntry> CODEC = createCodec(SequentialEntry::new);

   SequentialEntry(List<LootPoolEntryContainer> p_297558_, List<LootItemCondition> p_299835_) {
      super(p_297558_, p_299835_);
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.SEQUENCE;
   }

   @Override
   protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
      return switch (pChildren.size()) {
         case 0 -> ALWAYS_TRUE;
         case 1 -> (ComposableEntryContainer)pChildren.get(0);
         case 2 -> pChildren.get(0).and(pChildren.get(1));
         default -> (p_297043_, p_297044_) -> {
         for (ComposableEntryContainer $$3 : pChildren) {
            if (!$$3.expand(p_297043_, p_297044_)) {
               return false;
            }
         }

         return true;
      };
      };
   }

   public static SequentialEntry.Builder sequential(LootPoolEntryContainer.Builder<?>... pChildren) {
      return new SequentialEntry.Builder(pChildren);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<SequentialEntry.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
         for (LootPoolEntryContainer.Builder<?> $$1 : pChildren) {
            this.entries.add($$1.build());
         }
      }

      protected SequentialEntry.Builder getThis() {
         return this;
      }

      @Override
      public SequentialEntry.Builder then(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         this.entries.add(pChildBuilder.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new SequentialEntry(this.entries.build(), this.getConditions());
      }
   }
}
