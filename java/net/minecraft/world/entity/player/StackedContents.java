package net.minecraft.world.entity.player;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class StackedContents {
   private static final int EMPTY = 0;
   public final Int2IntMap contents = new Int2IntOpenHashMap();

   public void accountSimpleStack(ItemStack pStack) {
      if (!pStack.isDamaged() && !pStack.isEnchanted() && !pStack.has(DataComponents.CUSTOM_NAME)) {
         this.accountStack(pStack);
      }
   }

   public void accountStack(ItemStack pStack) {
      this.accountStack(pStack, pStack.getMaxStackSize());
   }

   public void accountStack(ItemStack pStack, int pAmount) {
      if (!pStack.isEmpty()) {
         int $$2 = getStackingIndex(pStack);
         int $$3 = Math.min(pAmount, pStack.getCount());
         this.put($$2, $$3);
      }
   }

   public static int getStackingIndex(ItemStack pStack) {
      return BuiltInRegistries.ITEM.getId(pStack.getItem());
   }

   boolean has(int pStackingIndex) {
      return this.contents.get(pStackingIndex) > 0;
   }

   int take(int pStackingIndex, int pAmount) {
      int $$2 = this.contents.get(pStackingIndex);
      if ($$2 >= pAmount) {
         this.contents.put(pStackingIndex, $$2 - pAmount);
         return pStackingIndex;
      } else {
         return 0;
      }
   }

   void put(int pStackingIndex, int pIncrement) {
      this.contents.put(pStackingIndex, this.contents.get(pStackingIndex) + pIncrement);
   }

   public boolean canCraft(Recipe<?> pRecipe, @Nullable IntList pStackingIndexList) {
      return this.canCraft(pRecipe, pStackingIndexList, 1);
   }

   public boolean canCraft(Recipe<?> pRecipe, @Nullable IntList pStackingIndexList, int pAmount) {
      return new StackedContents.RecipePicker(pRecipe).tryPick(pAmount, pStackingIndexList);
   }

   public int getBiggestCraftableStack(RecipeHolder<?> pRecipe, @Nullable IntList pStackingIndexList) {
      return this.getBiggestCraftableStack(pRecipe, Integer.MAX_VALUE, pStackingIndexList);
   }

   public int getBiggestCraftableStack(RecipeHolder<?> pRecipe, int pAmount, @Nullable IntList pStackingIndexList) {
      return new StackedContents.RecipePicker(pRecipe.value()).tryPickAll(pAmount, pStackingIndexList);
   }

   public static ItemStack fromStackingIndex(int pStackingIndex) {
      return pStackingIndex == 0 ? ItemStack.EMPTY : new ItemStack(Item.byId(pStackingIndex));
   }

   public void clear() {
      this.contents.clear();
   }

   class RecipePicker {
      private final Recipe<?> recipe;
      private final List<Ingredient> ingredients = Lists.newArrayList();
      private final int ingredientCount;
      private final int[] items;
      private final int itemCount;
      private final BitSet data;
      private final IntList path = new IntArrayList();

      public RecipePicker(final Recipe<?> pRecipe) {
         this.recipe = pRecipe;
         this.ingredients.addAll(pRecipe.getIngredients());
         this.ingredients.removeIf(Ingredient::isEmpty);
         this.ingredientCount = this.ingredients.size();
         this.items = this.getUniqueAvailableIngredientItems();
         this.itemCount = this.items.length;
         this.data = new BitSet(this.ingredientCount + this.itemCount + this.ingredientCount + this.ingredientCount * this.itemCount);

         for (int $$1 = 0; $$1 < this.ingredients.size(); $$1++) {
            IntList $$2 = this.ingredients.get($$1).getStackingIds();

            for (int $$3 = 0; $$3 < this.itemCount; $$3++) {
               if ($$2.contains(this.items[$$3])) {
                  this.data.set(this.getIndex(true, $$3, $$1));
               }
            }
         }
      }

      public boolean tryPick(int pAmount, @Nullable IntList pStackingIndexList) {
         if (pAmount <= 0) {
            return true;
         } else {
            int $$2;
            for ($$2 = 0; this.dfs(pAmount); $$2++) {
               StackedContents.this.take(this.items[this.path.getInt(0)], pAmount);
               int $$3 = this.path.size() - 1;
               this.setSatisfied(this.path.getInt($$3));

               for (int $$4 = 0; $$4 < $$3; $$4++) {
                  this.toggleResidual(($$4 & 1) == 0, this.path.get($$4), this.path.get($$4 + 1));
               }

               this.path.clear();
               this.data.clear(0, this.ingredientCount + this.itemCount);
            }

            boolean $$5 = $$2 == this.ingredientCount;
            boolean $$6 = $$5 && pStackingIndexList != null;
            if ($$6) {
               pStackingIndexList.clear();
            }

            this.data.clear(0, this.ingredientCount + this.itemCount + this.ingredientCount);
            int $$7 = 0;

            for (Ingredient $$9 : this.recipe.getIngredients()) {
               if ($$6 && $$9.isEmpty()) {
                  pStackingIndexList.add(0);
               } else {
                  for (int $$10 = 0; $$10 < this.itemCount; $$10++) {
                     if (this.hasResidual(false, $$7, $$10)) {
                        this.toggleResidual(true, $$10, $$7);
                        StackedContents.this.put(this.items[$$10], pAmount);
                        if ($$6) {
                           pStackingIndexList.add(this.items[$$10]);
                        }
                     }
                  }

                  $$7++;
               }
            }

            return $$5;
         }
      }

      private int[] getUniqueAvailableIngredientItems() {
         IntCollection $$0 = new IntAVLTreeSet();

         for (Ingredient $$1 : this.ingredients) {
            $$0.addAll($$1.getStackingIds());
         }

         IntIterator $$2 = $$0.iterator();

         while ($$2.hasNext()) {
            if (!StackedContents.this.has($$2.nextInt())) {
               $$2.remove();
            }
         }

         return $$0.toIntArray();
      }

      private boolean dfs(int pAmount) {
         int $$1 = this.itemCount;

         for (int $$2 = 0; $$2 < $$1; $$2++) {
            if (StackedContents.this.contents.get(this.items[$$2]) >= pAmount) {
               this.visit(false, $$2);

               while (!this.path.isEmpty()) {
                  int $$3 = this.path.size();
                  boolean $$4 = ($$3 & 1) == 1;
                  int $$5 = this.path.getInt($$3 - 1);
                  if (!$$4 && !this.isSatisfied($$5)) {
                     break;
                  }

                  int $$6 = $$4 ? this.ingredientCount : $$1;
                  int $$7 = 0;

                  while (true) {
                     if ($$7 < $$6) {
                        if (this.hasVisited($$4, $$7) || !this.hasConnection($$4, $$5, $$7) || !this.hasResidual($$4, $$5, $$7)) {
                           $$7++;
                           continue;
                        }

                        this.visit($$4, $$7);
                     }

                     $$7 = this.path.size();
                     if ($$7 == $$3) {
                        this.path.removeInt($$7 - 1);
                     }
                     break;
                  }
               }

               if (!this.path.isEmpty()) {
                  return true;
               }
            }
         }

         return false;
      }

      private boolean isSatisfied(int pStackingIndex) {
         return this.data.get(this.getSatisfiedIndex(pStackingIndex));
      }

      private void setSatisfied(int pStackingIndex) {
         this.data.set(this.getSatisfiedIndex(pStackingIndex));
      }

      private int getSatisfiedIndex(int pStackingIndex) {
         return this.ingredientCount + this.itemCount + pStackingIndex;
      }

      private boolean hasConnection(boolean pIsIngredientPath, int pStackingIndex, int pPathIndex) {
         return this.data.get(this.getIndex(pIsIngredientPath, pStackingIndex, pPathIndex));
      }

      private boolean hasResidual(boolean pIsIngredientPath, int pStackingIndex, int pPathIndex) {
         return pIsIngredientPath != this.data.get(1 + this.getIndex(pIsIngredientPath, pStackingIndex, pPathIndex));
      }

      private void toggleResidual(boolean pIsIngredientPath, int pStackingIndex, int pPathIndex) {
         this.data.flip(1 + this.getIndex(pIsIngredientPath, pStackingIndex, pPathIndex));
      }

      private int getIndex(boolean pIsIngredientPath, int pStackingIndex, int pPathIndex) {
         int $$3 = pIsIngredientPath ? pStackingIndex * this.ingredientCount + pPathIndex : pPathIndex * this.ingredientCount + pStackingIndex;
         return this.ingredientCount + this.itemCount + this.ingredientCount + 2 * $$3;
      }

      private void visit(boolean pIsIngredientPath, int pPathIndex) {
         this.data.set(this.getVisitedIndex(pIsIngredientPath, pPathIndex));
         this.path.add(pPathIndex);
      }

      private boolean hasVisited(boolean pIsIngredientPath, int pPathIndex) {
         return this.data.get(this.getVisitedIndex(pIsIngredientPath, pPathIndex));
      }

      private int getVisitedIndex(boolean pIsIngredientPath, int pPathIndex) {
         return (pIsIngredientPath ? 0 : this.ingredientCount) + pPathIndex;
      }

      public int tryPickAll(int pAmount, @Nullable IntList pStackingIndexList) {
         int $$2 = 0;
         int $$3 = Math.min(pAmount, this.getMinIngredientCount()) + 1;

         while (true) {
            int $$4 = ($$2 + $$3) / 2;
            if (this.tryPick($$4, null)) {
               if ($$3 - $$2 <= 1) {
                  if ($$4 > 0) {
                     this.tryPick($$4, pStackingIndexList);
                  }

                  return $$4;
               }

               $$2 = $$4;
            } else {
               $$3 = $$4;
            }
         }
      }

      private int getMinIngredientCount() {
         int $$0 = Integer.MAX_VALUE;

         for (Ingredient $$1 : this.ingredients) {
            int $$2 = 0;
            IntListIterator var5 = $$1.getStackingIds().iterator();

            while (var5.hasNext()) {
               int $$3 = (Integer)var5.next();
               $$2 = Math.max($$2, StackedContents.this.contents.get($$3));
            }

            if ($$0 > 0) {
               $$0 = Math.min($$0, $$2);
            }
         }

         return $$0;
      }
   }
}
