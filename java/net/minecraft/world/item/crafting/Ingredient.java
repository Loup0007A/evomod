package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;

public class Ingredient implements Predicate<ItemStack> {
   private static final AtomicInteger INVALIDATION_COUNTER = new AtomicInteger();
   public static final Ingredient EMPTY = new Ingredient(Stream.empty());
   public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = ForgeHooks.ingredientStreamCodec();
   private final Ingredient.Value[] values;
   @Nullable
   private ItemStack[] itemStacks;
   @Nullable
   private IntList stackingIds;
   public static final Codec<Ingredient> VANILLA_CODEC = codec(true);
   public static final MapCodec<Ingredient> VANILLA_MAP_CODEC = MapCodec.assumeMapUnsafe(VANILLA_CODEC);
   public static final Codec<Ingredient> CODEC = ForgeHooks.enhanceIngredientCodec(VANILLA_CODEC);
   private static final Codec<Ingredient> VANILLA_CODEC_NONEMPTY = codec(false);
   public static final Codec<Ingredient> CODEC_NONEMPTY = ForgeHooks.enhanceIngredientCodec(VANILLA_CODEC_NONEMPTY);
   private int invalidationCounter;
   private final boolean isVanilla = this.getClass() == Ingredient.class;

   public static void invalidateAll() {
      INVALIDATION_COUNTER.incrementAndGet();
   }

   protected Ingredient(Stream<? extends Ingredient.Value> pValues) {
      this.values = pValues.toArray(Ingredient.Value[]::new);
   }

   private Ingredient(Ingredient.Value[] p_301101_) {
      this.values = p_301101_;
   }

   public ItemStack[] getItems() {
      if (this.itemStacks == null || this.checkInvalidation()) {
         this.itemStacks = Arrays.stream(this.values).flatMap(p_43916_ -> p_43916_.getItems().stream()).distinct().toArray(ItemStack[]::new);
      }

      return this.itemStacks;
   }

   public boolean test(@Nullable ItemStack pStack) {
      if (pStack == null) {
         return false;
      } else if (this.isEmpty()) {
         return pStack.isEmpty();
      } else {
         for (ItemStack itemstack : this.getItems()) {
            if (itemstack.is(pStack.getItem())) {
               return true;
            }
         }

         return false;
      }
   }

   public IntList getStackingIds() {
      if (this.stackingIds == null || this.checkInvalidation()) {
         this.markValid();
         ItemStack[] aitemstack = this.getItems();
         this.stackingIds = new IntArrayList(aitemstack.length);

         for (ItemStack itemstack : aitemstack) {
            this.stackingIds.add(StackedContents.getStackingIndex(itemstack));
         }

         this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.stackingIds;
   }

   public boolean isEmpty() {
      return this.values.length == 0;
   }

   @Override
   public boolean equals(Object pOther) {
      return pOther instanceof Ingredient ingredient ? Arrays.equals(this.values, ingredient.values) : false;
   }

   public static Ingredient fromValues(Stream<? extends Ingredient.Value> pStream) {
      Ingredient ingredient = new Ingredient(pStream);
      return ingredient.isEmpty() ? EMPTY : ingredient;
   }

   public static Ingredient of() {
      return EMPTY;
   }

   public static Ingredient of(ItemLike... pItems) {
      return of(Arrays.stream(pItems).map(ItemStack::new));
   }

   public static Ingredient of(ItemStack... pStacks) {
      return of(Arrays.stream(pStacks));
   }

   public static Ingredient of(Stream<ItemStack> pStacks) {
      return fromValues(pStacks.filter(p_43944_ -> !p_43944_.isEmpty()).map(Ingredient.ItemValue::new));
   }

   public static Ingredient of(TagKey<Item> pTag) {
      return fromValues(Stream.of(new Ingredient.TagValue(pTag)));
   }

   public final boolean checkInvalidation() {
      int currentInvalidationCounter = INVALIDATION_COUNTER.get();
      if (this.invalidationCounter != currentInvalidationCounter) {
         this.invalidate();
         return true;
      } else {
         return false;
      }
   }

   protected final void markValid() {
      this.invalidationCounter = INVALIDATION_COUNTER.get();
   }

   protected void invalidate() {
      this.itemStacks = null;
      this.stackingIds = null;
   }

   public boolean isSimple() {
      return true;
   }

   public final boolean isVanilla() {
      return this.isVanilla;
   }

   public IIngredientSerializer<? extends Ingredient> serializer() {
      if (!this.isVanilla()) {
         throw new IllegalStateException("Modders must implement Ingredient.codec in their custom Ingredients: " + this.getClass());
      } else {
         return IIngredientSerializer.VANILLA;
      }
   }

   @Override
   public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append("Ingredient[");

      for (int x = 0; x < this.values.length; x++) {
         if (x != 0) {
            buf.append(", ");
         }

         if (this.values[x] instanceof Ingredient.TagValue tag) {
            buf.append('#').append(tag.tag().location());
         } else if (this.values[x] instanceof Ingredient.ItemValue item) {
            buf.append(item.item());
         } else {
            buf.append(this.values[x]);
         }
      }

      buf.append(']');
      return buf.toString();
   }

   private static Codec<Ingredient> codec(boolean pAllowEmpty) {
      Codec<Ingredient.Value[]> codec = Codec.list(Ingredient.Value.CODEC)
         .comapFlatMap(
            p_296902_ -> !pAllowEmpty && p_296902_.size() < 1
                  ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                  : DataResult.success(p_296902_.toArray(new Ingredient.Value[0])),
            List::of
         );
      return Codec.either(codec, Ingredient.Value.CODEC)
         .flatComapMap(
            p_296900_ -> (Ingredient)p_296900_.map(Ingredient::new, p_296903_ -> new Ingredient(new Ingredient.Value[]{p_296903_})),
            p_296899_ -> {
               if (p_296899_.values.length == 1) {
                  return DataResult.success(Either.right(p_296899_.values[0]));
               } else {
                  return p_296899_.values.length == 0 && !pAllowEmpty
                     ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                     : DataResult.success(Either.left(p_296899_.values));
               }
            }
         );
   }

   public static record ItemValue(ItemStack item) implements Ingredient.Value {
      static final Codec<Ingredient.ItemValue> CODEC = RecordCodecBuilder.create(
         p_327194_ -> p_327194_.group(ItemStack.SIMPLE_ITEM_CODEC.fieldOf("item").forGetter(p_299657_ -> p_299657_.item))
               .apply(p_327194_, Ingredient.ItemValue::new)
      );

      @Override
      public boolean equals(Object pOther) {
         return pOther instanceof Ingredient.ItemValue ingredient$itemvalue
            ? ingredient$itemvalue.item.getItem().equals(this.item.getItem()) && ingredient$itemvalue.item.getCount() == this.item.getCount()
            : false;
      }

      @Override
      public Collection<ItemStack> getItems() {
         return Collections.singleton(this.item);
      }
   }

   public static record TagValue(TagKey<Item> tag) implements Ingredient.Value {
      static final Codec<Ingredient.TagValue> CODEC = RecordCodecBuilder.create(
         p_300241_ -> p_300241_.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(p_301340_ -> p_301340_.tag))
               .apply(p_300241_, Ingredient.TagValue::new)
      );

      @Override
      public boolean equals(Object pOther) {
         return pOther instanceof Ingredient.TagValue ingredient$tagvalue ? ingredient$tagvalue.tag.location().equals(this.tag.location()) : false;
      }

      @Override
      public Collection<ItemStack> getItems() {
         List<ItemStack> list = Lists.newArrayList();

         for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
            list.add(new ItemStack(holder));
         }

         if (list.size() == 0) {
            ItemStack marker = new ItemStack(Blocks.BARRIER);
            marker.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + this.tag.location()));
            list.add(marker);
         }

         return list;
      }
   }

   public interface Value {
      Codec<Ingredient.Value> CODEC = Codec.xor(Ingredient.ItemValue.CODEC, Ingredient.TagValue.CODEC)
         .xmap(p_300070_ -> (Ingredient.Value)p_300070_.map(p_301348_ -> p_301348_, p_298354_ -> p_298354_), p_299608_ -> {
            if (p_299608_ instanceof Ingredient.TagValue ingredient$tagvalue) {
               return Either.right(ingredient$tagvalue);
            } else if (p_299608_ instanceof Ingredient.ItemValue ingredient$itemvalue) {
               return Either.left(ingredient$itemvalue);
            } else {
               throw new UnsupportedOperationException("This is neither an item value nor a tag value.");
            }
         });

      Collection<ItemStack> getItems();
   }
}
