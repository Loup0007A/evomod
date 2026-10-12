package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class PartialNBTIngredient extends AbstractIngredient {
   private final List<Item> items;
   private final CompoundTag nbt;
   private final NbtPredicate predicate;
   public static final MapCodec<PartialNBTIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(
               ForgeRegistries.ITEMS.getCodec().listOf().fieldOf("items").forGetter(i -> i.items), TagParser.AS_CODEC.fieldOf("nbt").forGetter(i -> i.nbt)
            )
            .apply(builder, PartialNBTIngredient::new)
   );
   public static final IIngredientSerializer<PartialNBTIngredient> SERIALIZER = new IIngredientSerializer<PartialNBTIngredient>() {
      @Override
      public MapCodec<? extends PartialNBTIngredient> codec() {
         return PartialNBTIngredient.CODEC;
      }

      public PartialNBTIngredient read(RegistryFriendlyByteBuf buffer) {
         List<Item> items = buffer.readList(b -> Item.STREAM_CODEC.decode(buffer).get());
         CompoundTag nbt = buffer.readNbt();
         return new PartialNBTIngredient(items, Objects.requireNonNull(nbt));
      }

      public void write(RegistryFriendlyByteBuf buffer, PartialNBTIngredient value) {
         buffer.writeCollection(value.items, (b, item) -> Item.STREAM_CODEC.encode(buffer, item.builtInRegistryHolder()));
         buffer.writeNbt(value.nbt);
      }
   };

   public static PartialNBTIngredient.Builder builder() {
      return new PartialNBTIngredient.Builder();
   }

   private PartialNBTIngredient(List<Item> items, CompoundTag nbt) {
      super(items.stream().map(item -> {
         ItemStack stack = new ItemStack(item);
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
         return new Ingredient.ItemValue(stack);
      }));
      if (items.isEmpty()) {
         throw new IllegalArgumentException("Cannot create a PartialNBTIngredient with no items");
      } else {
         this.items = Collections.unmodifiableList(items);
         this.nbt = nbt;
         this.predicate = new NbtPredicate(nbt);
      }
   }

   public static PartialNBTIngredient of(CompoundTag nbt, ItemLike... items) {
      return new PartialNBTIngredient(Arrays.stream(items).map(ItemLike::asItem).toList(), nbt);
   }

   public static PartialNBTIngredient of(ItemLike item, CompoundTag nbt) {
      return new PartialNBTIngredient(List.of(item.asItem()), nbt);
   }

   @Override
   public boolean test(@Nullable ItemStack input) {
      if (input == null) {
         return false;
      } else {
         CustomData nbt = input.get(DataComponents.CUSTOM_DATA);
         return nbt != null && this.items.contains(input.getItem()) && this.predicate.matches(nbt.copyTag());
      }
   }

   @Override
   public boolean isSimple() {
      return false;
   }

   @Override
   public IIngredientSerializer<? extends Ingredient> serializer() {
      return SERIALIZER;
   }

   public static class Builder {
      private final List<ItemLike> items = new ArrayList<>();
      private CompoundTag nbt;

      public PartialNBTIngredient.Builder nbt(CompoundTag value) {
         if (this.nbt != null) {
            throw new IllegalStateException("NBT Tag already set");
         } else {
            this.nbt = value;
            return this;
         }
      }

      public PartialNBTIngredient.Builder item(ItemLike item) {
         this.items.add(item);
         return this;
      }

      public PartialNBTIngredient.Builder items(ItemLike... values) {
         for (ItemLike item : values) {
            this.items.add(item);
         }

         return this;
      }

      public PartialNBTIngredient build() {
         if (this.nbt == null) {
            throw new IllegalStateException("NBT Data not set");
         } else if (this.items.isEmpty()) {
            throw new IllegalStateException("No items added");
         } else {
            return PartialNBTIngredient.of(this.nbt, this.items.stream().toArray(ItemLike[]::new));
         }
      }
   }
}
