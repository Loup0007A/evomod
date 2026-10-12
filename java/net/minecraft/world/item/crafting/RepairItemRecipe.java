package net.minecraft.world.item.crafting;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class RepairItemRecipe extends CustomRecipe {
   public RepairItemRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   @Nullable
   private Pair<ItemStack, ItemStack> getItemsToCombine(CraftingInput pInput) {
      ItemStack $$1 = null;
      ItemStack $$2 = null;

      for (int $$3 = 0; $$3 < pInput.size(); $$3++) {
         ItemStack $$4 = pInput.getItem($$3);
         if (!$$4.isEmpty()) {
            if ($$1 == null) {
               $$1 = $$4;
            } else {
               if ($$2 != null) {
                  return null;
               }

               $$2 = $$4;
            }
         }
      }

      return $$1 != null && $$2 != null && canCombine($$1, $$2) ? Pair.of($$1, $$2) : null;
   }

   private static boolean canCombine(ItemStack pStack1, ItemStack pStack2) {
      return pStack2.is(pStack1.getItem())
         && pStack1.getCount() == 1
         && pStack2.getCount() == 1
         && pStack1.has(DataComponents.MAX_DAMAGE)
         && pStack2.has(DataComponents.MAX_DAMAGE)
         && pStack1.has(DataComponents.DAMAGE)
         && pStack2.has(DataComponents.DAMAGE);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      return this.getItemsToCombine(pInput) != null;
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      Pair<ItemStack, ItemStack> $$2 = this.getItemsToCombine(pInput);
      if ($$2 == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$3 = (ItemStack)$$2.getFirst();
         ItemStack $$4 = (ItemStack)$$2.getSecond();
         int $$5 = Math.max($$3.getMaxDamage(), $$4.getMaxDamage());
         int $$6 = $$3.getMaxDamage() - $$3.getDamageValue();
         int $$7 = $$4.getMaxDamage() - $$4.getDamageValue();
         int $$8 = $$6 + $$7 + $$5 * 5 / 100;
         ItemStack $$9 = new ItemStack($$3.getItem());
         $$9.set(DataComponents.MAX_DAMAGE, $$5);
         $$9.setDamageValue(Math.max($$5 - $$8, 0));
         ItemEnchantments $$10 = EnchantmentHelper.getEnchantmentsForCrafting($$3);
         ItemEnchantments $$11 = EnchantmentHelper.getEnchantmentsForCrafting($$4);
         EnchantmentHelper.updateEnchantments(
            $$9,
            p_341594_ -> pRegistries.lookupOrThrow(Registries.ENCHANTMENT)
                  .listElements()
                  .filter(p_341586_ -> p_341586_.is(EnchantmentTags.CURSE))
                  .forEach(p_341590_ -> {
                     int $$4x = Math.max($$10.getLevel(p_341590_), $$11.getLevel(p_341590_));
                     if ($$4x > 0) {
                        p_341594_.upgrade(p_341590_, $$4x);
                     }
                  })
         );
         return $$9;
      }
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth * pHeight >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.REPAIR_ITEM;
   }
}
