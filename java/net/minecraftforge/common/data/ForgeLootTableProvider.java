package net.minecraftforge.common.data;

import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.loot.CanToolPerformAction;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public final class ForgeLootTableProvider extends LootTableProvider {
   private static final String POOLS = "f_79156_";
   private static final String ENTRIES = "f_79023_";
   private static final String CONDITIONS = "f_79024_";
   private static final String CHILDREN = "f_79428_";
   private static final String ENTRY_CONDITION = "f_79636_";
   private static final String TERMS = "f_285609_";

   public ForgeLootTableProvider(PackOutput pack, CompletableFuture<HolderLookup.Provider> lookup) {
      super(pack, Set.of(), VanillaLootTableProvider.create(pack, lookup).getTables(), lookup);
   }

   @Override
   protected void validate(Registry<LootTable> map, ValidationContext validationcontext, ProblemReporter report) {
   }

   @Override
   public List<LootTableProvider.SubProviderEntry> getTables() {
      return super.getTables()
         .stream()
         .map(entry -> new LootTableProvider.SubProviderEntry(provider -> this.replaceAndFilterChangesOnly(entry.provider().apply(provider)), entry.paramSet()))
         .collect(Collectors.toList());
   }

   private LootTableSubProvider replaceAndFilterChangesOnly(LootTableSubProvider subProvider) {
      return newConsumer -> subProvider.generate((resourceLocation, builder) -> {
            if (this.findAndReplaceInLootTableBuilder(builder, Items.SHEARS, ToolActions.SHEARS_DIG)) {
               newConsumer.accept(resourceLocation, builder);
            }
         });
   }

   private boolean findAndReplaceInLootTableBuilder(LootTable.Builder builder, Item from, ToolAction toolAction) {
      Builder<LootPool> lootPools = (Builder<LootPool>)ObfuscationReflectionHelper.getPrivateValue(LootTable.Builder.class, builder, "f_79156_");
      boolean found = false;
      if (lootPools == null) {
         throw new IllegalStateException(LootTable.Builder.class.getName() + " is missing field f_79156_");
      } else {
         UnmodifiableIterator var6 = lootPools.build().iterator();

         while (var6.hasNext()) {
            LootPool lootPool = (LootPool)var6.next();
            if (this.findAndReplaceInLootPool(lootPool, from, toolAction)) {
               found = true;
            }
         }

         return found;
      }
   }

   private boolean findAndReplaceInLootPool(LootPool lootPool, Item from, ToolAction toolAction) {
      List<LootPoolEntryContainer> lootEntries = (List<LootPoolEntryContainer>)ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, "f_79023_");
      List<LootItemCondition> lootConditions = (List<LootItemCondition>)ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, "f_79024_");
      boolean found = false;
      if (lootEntries == null) {
         throw new IllegalStateException(LootPool.class.getName() + " is missing field f_79023_");
      } else {
         for (LootPoolEntryContainer lootEntry : lootEntries) {
            if (this.findAndReplaceInLootEntry(lootEntry, from, toolAction)) {
               found = true;
            }

            if (lootEntry instanceof CompositeEntryBase && this.findAndReplaceInParentedLootEntry((CompositeEntryBase)lootEntry, from, toolAction)) {
               found = true;
            }
         }

         if (lootConditions == null) {
            throw new IllegalStateException(LootPool.class.getName() + " is missing field f_79024_");
         } else {
            List<LootItemCondition> var14 = new ArrayList<>(lootConditions);
            ObfuscationReflectionHelper.setPrivateValue(LootPool.class, lootPool, var14, "f_79024_");

            for (int i = 0; i < var14.size(); i++) {
               LootItemCondition lootCondition = (LootItemCondition)var14.get(i);
               if (lootCondition instanceof MatchTool matchTool && this.checkMatchTool(matchTool, from)) {
                  var14.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
                  found = true;
                  continue;
               }

               if (lootCondition instanceof InvertedLootItemCondition inverted) {
                  LootItemCondition invLootCondition = inverted.term();
                  if (invLootCondition instanceof MatchTool matchTool && this.checkMatchTool(matchTool, from)) {
                     var14.set(i, InvertedLootItemCondition.invert(CanToolPerformAction.canToolPerformAction(toolAction)).build());
                     found = true;
                     continue;
                  }

                  if (invLootCondition instanceof CompositeLootItemCondition compositeLootItemCondition
                     && this.findAndReplaceInComposite(compositeLootItemCondition, from, toolAction)) {
                     found = true;
                  }
               }
            }

            return found;
         }
      }
   }

   private boolean findAndReplaceInParentedLootEntry(CompositeEntryBase entry, Item from, ToolAction toolAction) {
      List<LootPoolEntryContainer> lootEntries = (List<LootPoolEntryContainer>)ObfuscationReflectionHelper.getPrivateValue(
         CompositeEntryBase.class, entry, "f_79428_"
      );
      boolean found = false;
      if (lootEntries == null) {
         throw new IllegalStateException(CompositeEntryBase.class.getName() + " is missing field f_79428_");
      } else {
         for (LootPoolEntryContainer lootEntry : lootEntries) {
            if (this.findAndReplaceInLootEntry(lootEntry, from, toolAction)) {
               found = true;
            }
         }

         return found;
      }
   }

   private boolean findAndReplaceInLootEntry(LootPoolEntryContainer entry, Item from, ToolAction toolAction) {
      List<LootItemCondition> lootConditions = (List<LootItemCondition>)ObfuscationReflectionHelper.getPrivateValue(
         LootPoolEntryContainer.class, entry, "f_79636_"
      );
      boolean found = false;
      if (lootConditions == null) {
         throw new IllegalStateException(LootPoolEntryContainer.class.getName() + " is missing field f_79636_");
      } else {
         List<LootItemCondition> var10 = new ArrayList<>(lootConditions);
         ObfuscationReflectionHelper.setPrivateValue(LootPoolEntryContainer.class, entry, var10, "f_79636_");

         for (int i = 0; i < var10.size(); i++) {
            LootItemCondition condition = (LootItemCondition)var10.get(i);
            if (condition instanceof CompositeLootItemCondition composite && this.findAndReplaceInComposite(composite, from, toolAction)) {
               found = true;
               continue;
            }

            if (condition instanceof MatchTool matchTool && this.checkMatchTool(matchTool, from)) {
               var10.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
               found = true;
            }
         }

         return found;
      }
   }

   private boolean findAndReplaceInComposite(CompositeLootItemCondition alternative, Item from, ToolAction toolAction) {
      List<LootItemCondition> lootConditions = (List<LootItemCondition>)ObfuscationReflectionHelper.getPrivateValue(
         CompositeLootItemCondition.class, alternative, "f_285609_"
      );
      boolean found = false;
      if (lootConditions == null) {
         throw new IllegalStateException(CompositeLootItemCondition.class.getName() + " is missing field f_285609_");
      } else {
         List<LootItemCondition> var9 = new ArrayList<>(lootConditions);
         ObfuscationReflectionHelper.setPrivateValue(CompositeLootItemCondition.class, alternative, var9, "f_285609_");

         for (int i = 0; i < var9.size(); i++) {
            Object var8 = var9.get(i);
            if (var8 instanceof MatchTool) {
               MatchTool matchTool = (MatchTool)var8;
               if (this.checkMatchTool(matchTool, from)) {
                  var9.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
                  found = true;
               }
            }
         }

         return found;
      }
   }

   private boolean checkMatchTool(MatchTool lootCondition, Item expected) {
      return lootCondition.predicate().flatMap(ItemPredicate::items).filter(s -> s.contains(expected.builtInRegistryHolder())).isPresent();
   }
}
