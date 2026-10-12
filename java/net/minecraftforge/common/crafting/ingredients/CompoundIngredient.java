package net.minecraftforge.common.crafting.ingredients;

import com.google.common.collect.Lists;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundIngredient extends AbstractIngredient {
   private final List<Ingredient> children;
   private ItemStack[] stacks;
   private IntList itemIds;
   private final boolean isSimple;
   public static final MapCodec<CompoundIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Ingredient.CODEC_NONEMPTY.listOf().fieldOf("children").forGetter(i -> i.children)).apply(builder, CompoundIngredient::new)
   );
   public static final IIngredientSerializer<CompoundIngredient> SERIALIZER = new IIngredientSerializer<CompoundIngredient>() {
      @Override
      public MapCodec<CompoundIngredient> codec() {
         return CompoundIngredient.CODEC;
      }

      public void write(RegistryFriendlyByteBuf buffer, CompoundIngredient value) {
         buffer.writeCollection(value.children, (buf, child) -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, child));
      }

      public CompoundIngredient read(RegistryFriendlyByteBuf buffer) {
         ArrayList<Ingredient> children = buffer.readCollection(ArrayList::new, buf -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
         return new CompoundIngredient(children);
      }
   };

   public static Ingredient of(Ingredient... children) {
      if (children.length == 0) {
         throw new IllegalArgumentException("Cannot create a compound ingredient with no children, use Ingredient.of() to create an empty ingredient");
      } else {
         return (Ingredient)(children.length == 1 ? children[0] : new CompoundIngredient(Arrays.asList(children)));
      }
   }

   private CompoundIngredient(List<Ingredient> children) {
      this.children = Collections.unmodifiableList(children);
      this.isSimple = children.stream().allMatch(Ingredient::isSimple);
   }

   @NotNull
   @Override
   public ItemStack[] getItems() {
      if (this.stacks == null) {
         List<ItemStack> tmp = Lists.newArrayList();

         for (Ingredient child : this.children) {
            Collections.addAll(tmp, child.getItems());
         }

         this.stacks = tmp.toArray(new ItemStack[tmp.size()]);
      }

      return this.stacks;
   }

   @NotNull
   @Override
   public IntList getStackingIds() {
      boolean childrenNeedInvalidation = false;

      for (Ingredient child : this.children) {
         childrenNeedInvalidation |= child.checkInvalidation();
      }

      if (childrenNeedInvalidation || this.itemIds == null || this.checkInvalidation()) {
         this.markValid();
         this.itemIds = new IntArrayList();

         for (Ingredient child : this.children) {
            this.itemIds.addAll(child.getStackingIds());
         }

         this.itemIds.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.itemIds;
   }

   @Override
   public boolean test(@Nullable ItemStack target) {
      return target == null ? false : this.children.stream().anyMatch(c -> c.test(target));
   }

   @Override
   public boolean isSimple() {
      return this.isSimple;
   }

   @Override
   public boolean isEmpty() {
      return this.children.stream().allMatch(Ingredient::isEmpty);
   }

   @Override
   public IIngredientSerializer<? extends Ingredient> serializer() {
      return SERIALIZER;
   }
}
