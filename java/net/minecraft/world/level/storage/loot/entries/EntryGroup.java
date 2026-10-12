package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class EntryGroup extends CompositeEntryBase {
   public static final MapCodec<EntryGroup> CODEC = createCodec(EntryGroup::new);

   EntryGroup(List<LootPoolEntryContainer> p_300347_, List<LootItemCondition> p_300424_) {
      super(p_300347_, p_300424_);
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.GROUP;
   }

   @Override
   protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> pChildren) {
      return switch (pChildren.size()) {
         case 0 -> ALWAYS_TRUE;
         case 1 -> (ComposableEntryContainer)pChildren.get(0);
         case 2 -> {
            ComposableEntryContainer $$1 = pChildren.get(0);
            ComposableEntryContainer $$2 = pChildren.get(1);
            yield (p_79556_, p_79557_) -> {
               $$1.expand(p_79556_, p_79557_);
               $$2.expand(p_79556_, p_79557_);
               return true;
            };
         }
         default -> (p_297026_, p_297027_) -> {
         for (ComposableEntryContainer $$3 : pChildren) {
            $$3.expand(p_297026_, p_297027_);
         }

         return true;
      };
      };
   }

   public static EntryGroup.Builder list(LootPoolEntryContainer.Builder<?>... pChildren) {
      return new EntryGroup.Builder(pChildren);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<EntryGroup.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
         for (LootPoolEntryContainer.Builder<?> $$1 : pChildren) {
            this.entries.add($$1.build());
         }
      }

      protected EntryGroup.Builder getThis() {
         return this;
      }

      @Override
      public EntryGroup.Builder append(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         this.entries.add(pChildBuilder.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new EntryGroup(this.entries.build(), this.getConditions());
      }
   }
}
