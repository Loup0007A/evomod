package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class IntersectionIngredient extends AbstractIngredient {
   private final List<Ingredient> children;
   private final boolean isSimple;
   private ItemStack[] intersectedMatchingStacks = null;
   private IntList packedMatchingStacks = null;
   public static final MapCodec<IntersectionIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Ingredient.CODEC.listOf().fieldOf("children").forGetter(i -> i.children)).apply(builder, IntersectionIngredient::new)
   );
   public static final IIngredientSerializer<IntersectionIngredient> SERIALIZER = new IIngredientSerializer<IntersectionIngredient>() {
      @Override
      public MapCodec<? extends IntersectionIngredient> codec() {
         return IntersectionIngredient.CODEC;
      }

      public IntersectionIngredient read(RegistryFriendlyByteBuf buffer) {
         ArrayList<Ingredient> children = buffer.readCollection(ArrayList::new, buf -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
         return new IntersectionIngredient(children);
      }

      public void write(RegistryFriendlyByteBuf buffer, IntersectionIngredient value) {
         buffer.writeCollection(value.children, (b, child) -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, child));
      }
   };

   public static Ingredient of(Ingredient... ingredients) {
      if (ingredients.length == 0) {
         throw new IllegalArgumentException("Cannot create an IntersectionIngredient with no children, use Ingredient.of() to create an empty ingredient");
      } else {
         return (Ingredient)(ingredients.length == 1 ? ingredients[0] : new IntersectionIngredient(Arrays.asList(ingredients)));
      }
   }

   private IntersectionIngredient(List<Ingredient> children) {
      if (children.size() < 2) {
         throw new IllegalArgumentException("Cannot create an IntersectionIngredient with one or no children");
      } else {
         this.children = Collections.unmodifiableList(children);
         this.isSimple = children.stream().allMatch(Ingredient::isSimple);
      }
   }

   @Override
   public boolean test(@Nullable ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         for (Ingredient ingredient : this.children) {
            if (!ingredient.test(stack)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public ItemStack[] getItems() {
      if (this.intersectedMatchingStacks == null) {
         this.intersectedMatchingStacks = Arrays.stream(this.children.get(0).getItems()).filter(stack -> {
            for (int i = 1; i < this.children.size(); i++) {
               if (!this.children.get(i).test(stack)) {
                  return false;
               }
            }

            return true;
         }).toArray(ItemStack[]::new);
      }

      return this.intersectedMatchingStacks;
   }

   @Override
   public boolean isEmpty() {
      return this.children.stream().anyMatch(Ingredient::isEmpty);
   }

   @Override
   public boolean isSimple() {
      return this.isSimple;
   }

   @Override
   protected void invalidate() {
      super.invalidate();
      this.intersectedMatchingStacks = null;
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
   public IIngredientSerializer<IntersectionIngredient> serializer() {
      return SERIALIZER;
   }
}
