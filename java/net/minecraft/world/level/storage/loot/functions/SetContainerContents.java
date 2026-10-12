package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerContents extends LootItemConditionalFunction {
   public static final MapCodec<SetContainerContents> CODEC = RecordCodecBuilder.mapCodec(
      p_327591_ -> commonFields(p_327591_)
            .and(
               p_327591_.group(
                  ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(p_327590_ -> p_327590_.component),
                  LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(p_297115_ -> p_297115_.entries)
               )
            )
            .apply(p_327591_, SetContainerContents::new)
   );
   private final ContainerComponentManipulator<?> component;
   private final List<LootPoolEntryContainer> entries;

   SetContainerContents(List<LootItemCondition> p_193035_, ContainerComponentManipulator<?> p_329803_, List<LootPoolEntryContainer> p_298786_) {
      super(p_193035_);
      this.component = p_329803_;
      this.entries = List.copyOf(p_298786_);
   }

   @Override
   public LootItemFunctionType<SetContainerContents> getType() {
      return LootItemFunctions.SET_CONTENTS;
   }

   @Override
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (pStack.isEmpty()) {
         return pStack;
      } else {
         Stream.Builder<ItemStack> $$2 = Stream.builder();
         this.entries
            .forEach(
               p_80916_ -> p_80916_.expand(
                     pContext, p_287573_ -> p_287573_.createItemStack(LootTable.createStackSplitter(pContext.getLevel(), $$2::add), pContext)
                  )
            );
         this.component.setContents(pStack, $$2.build());
         return pStack;
      }
   }

   @Override
   public void validate(ValidationContext pContext) {
      super.validate(pContext);

      for (int $$1 = 0; $$1 < this.entries.size(); $$1++) {
         this.entries.get($$1).validate(pContext.forChild(".entry[" + $$1 + "]"));
      }
   }

   public static SetContainerContents.Builder setContents(ContainerComponentManipulator<?> pComponent) {
      return new SetContainerContents.Builder(pComponent);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetContainerContents.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
      private final ContainerComponentManipulator<?> component;

      public Builder(ContainerComponentManipulator<?> pComponent) {
         this.component = pComponent;
      }

      protected SetContainerContents.Builder getThis() {
         return this;
      }

      public SetContainerContents.Builder withEntry(LootPoolEntryContainer.Builder<?> pLootEntryBuilder) {
         this.entries.add(pLootEntryBuilder.build());
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetContainerContents(this.getConditions(), this.component, this.entries.build());
      }
   }
}
