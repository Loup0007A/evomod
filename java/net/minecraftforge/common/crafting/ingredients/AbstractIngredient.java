package net.minecraftforge.common.crafting.ingredients;

import java.util.stream.Stream;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public abstract class AbstractIngredient extends Ingredient {
   protected AbstractIngredient() {
      super(Stream.of());
   }

   protected AbstractIngredient(Stream<? extends Ingredient.Value> values) {
      super(values);
   }

   @Override
   public abstract boolean isSimple();

   @Override
   public abstract IIngredientSerializer<? extends Ingredient> serializer();

   @Deprecated
   public static Ingredient fromValues(Stream<? extends Ingredient.Value> values) {
      throw new UnsupportedOperationException("Use Ingredient.fromValues()");
   }

   @Deprecated
   public static Ingredient of() {
      throw new UnsupportedOperationException("Use Ingredient.of()");
   }

   @Deprecated
   public static Ingredient of(ItemLike... items) {
      throw new UnsupportedOperationException("Use Ingredient.of()");
   }

   @Deprecated
   public static Ingredient of(ItemStack... stacks) {
      throw new UnsupportedOperationException("Use Ingredient.of()");
   }

   @Deprecated
   public static Ingredient of(Stream<ItemStack> stacks) {
      throw new UnsupportedOperationException("Use Ingredient.of()");
   }

   @Deprecated
   public static Ingredient of(TagKey<Item> tag) {
      throw new UnsupportedOperationException("Use Ingredient.of()");
   }
}
