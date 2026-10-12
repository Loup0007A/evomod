package net.minecraft.data.tags;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

public abstract class EnchantmentTagsProvider extends TagsProvider<Enchantment> {
   public EnchantmentTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
      super(pOutput, Registries.ENCHANTMENT, pLookupProvider);
   }

   protected void tooltipOrder(HolderLookup.Provider pProvider, ResourceKey<Enchantment>... pValues) {
      this.tag(EnchantmentTags.TOOLTIP_ORDER).add(pValues);
      Set<ResourceKey<Enchantment>> $$2 = Set.of(pValues);
      List<String> $$3 = pProvider.lookupOrThrow(Registries.ENCHANTMENT)
         .listElements()
         .filter(p_341081_ -> !$$2.contains(p_341081_.unwrapKey().get()))
         .map(Holder::getRegisteredName)
         .collect(Collectors.toList());
      if (!$$3.isEmpty()) {
         throw new IllegalStateException("Not all enchantments were registered for tooltip ordering. Missing: " + String.join(", ", $$3));
      }
   }
}
