package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class DifferenceIngredient extends AbstractIngredient {
   private final Ingredient base;
   private final Ingredient subtracted;
   private ItemStack[] filteredMatchingStacks;
   private IntList packedMatchingStacks;
   public static final MapCodec<DifferenceIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Ingredient.CODEC.fieldOf("base").forGetter(i -> i.base), Ingredient.CODEC.fieldOf("subtracted").forGetter(i -> i.subtracted))
            .apply(builder, DifferenceIngredient::new)
   );
   public static final IIngredientSerializer<DifferenceIngredient> SERIALIZER = new IIngredientSerializer<DifferenceIngredient>() {
      @Override
      public MapCodec<? extends DifferenceIngredient> codec() {
         return DifferenceIngredient.CODEC;
      }

      public DifferenceIngredient read(RegistryFriendlyByteBuf buffer) {
         Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
         Ingredient without = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
         return new DifferenceIngredient(base, without);
      }

      public void write(RegistryFriendlyByteBuf buffer, DifferenceIngredient ingredient) {
         Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.base);
         Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.subtracted);
      }
   };

   public static DifferenceIngredient of(Ingredient base, Ingredient subtracted) {
      return new DifferenceIngredient(base, subtracted);
   }

   private DifferenceIngredient(Ingredient base, Ingredient subtracted) {
      this.base = base;
      this.subtracted = subtracted;
   }

   @Override
   public boolean test(@Nullable ItemStack stack) {
      return stack != null && !stack.isEmpty() ? this.base.test(stack) && !this.subtracted.test(stack) : false;
   }

   @Override
   public ItemStack[] getItems() {
      if (this.filteredMatchingStacks == null) {
         this.filteredMatchingStacks = Arrays.stream(this.base.getItems()).filter(stack -> !this.subtracted.test(stack)).toArray(ItemStack[]::new);
      }

      return this.filteredMatchingStacks;
   }

   @Override
   public boolean isEmpty() {
      return this.base.isEmpty();
   }

   @Override
   public boolean isSimple() {
      return this.base.isSimple() && this.subtracted.isSimple();
   }

   @Override
   protected void invalidate() {
      super.invalidate();
      this.filteredMatchingStacks = null;
      this.packedMatchingStacks = null;
   }

   @Override
   public IntList getStackingIds() {
      if (this.packedMatchingStacks == null || this.checkInvalidation()) {
         this.markValid();
         ItemStack[] matchingStacks = this.getItems();
         this.packedMatchingStacks = new IntArrayList(matchingStacks.length);

         for (ItemStack stack : matchingStacks) {
            this.packedMatchingStacks.add(StackedContents.getStackingIndex(stack));
         }

         this.packedMatchingStacks.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.packedMatchingStacks;
   }

   @Override
   public IIngredientSerializer<DifferenceIngredient> serializer() {
      return SERIALIZER;
   }
}
