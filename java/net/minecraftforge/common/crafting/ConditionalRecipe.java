package net.minecraftforge.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.MapEncoder.Implementation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.conditions.ConditionCodec;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

public class ConditionalRecipe {
   private static final MapCodec<Recipe<?>> CODEC = Codec.of(
      new Implementation<Recipe<?>>() {
         public <T> RecordBuilder<T> encode(Recipe<?> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (!(input instanceof ConditionalRecipe.Wrapper)) {
               new IllegalStateException("ConditionalRecipe.CODEC can only be used during data gen, how did you get here?");
            }

            ConditionalRecipe.Wrapper wrapper = (ConditionalRecipe.Wrapper)input;
            if (wrapper.main != null) {
               prefix.add("forge:condition", ICondition.CODEC.encodeStart(ops, wrapper.main));
            } else if (wrapper.recipes.size() == 1) {
               prefix.add("forge:condition", ICondition.CODEC.encodeStart(ops, wrapper.recipes.get(0).condition()));
            }

            ListBuilder<T> recipes = ops.listBuilder();

            for (ConditionalRecipe.InnerRecipe recipe : wrapper.recipes) {
               RecordBuilder<T> map = ops.mapBuilder();
               if (wrapper.main != null || wrapper.recipes.size() != 1) {
                  map.add("forge:condition", recipe.condition(), ICondition.CODEC);
               }

               map.add("recipe", recipe.recipe(), Recipe.CODEC);
               recipes.add(map.build(ops.emptyMap()));
            }

            prefix.add("recipes", recipes.build(ops.emptyList()));
            return prefix;
         }

         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of((T[])(new Object[]{ops.createString("forge:condition"), ops.createString("recipes")}));
         }
      },
      new com.mojang.serialization.MapDecoder.Implementation<Recipe<?>>() {
         public <T> DataResult<Recipe<?>> decode(DynamicOps<T> ops, MapLike<T> input) {
            ICondition.IContext context = ConditionCodec.getContext(ops);
            return ops.getStream(input.get("recipes"))
               .flatMap(
                  stream -> {
                     ConditionalRecipe.Holder<Integer> count = new ConditionalRecipe.Holder<>();
                     count.value = -1;
                     DataResult<Recipe<?>> ret = stream.<DataResult<Recipe<?>>>map(entry -> this.accept(context, ops, count, (T)entry))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                     return ret != null ? ret : DataResult.error(() -> "No recipe passed conditions, if this is the case, you should have an outer condition.");
                  }
               );
         }

         private <T> DataResult<Recipe<?>> accept(ICondition.IContext context, DynamicOps<T> ops, ConditionalRecipe.Holder<Integer> count, T entry) {
            count.value = count.value + 1;
            MapLike<T> map = (MapLike<T>)ops.getMap(entry).result().orElse(null);
            if (map == null) {
               return DataResult.error(() -> "Entry " + count.value + " was not MapLike " + entry.getClass());
            } else {
               if (map.get("forge:condition") != null) {
                  DataResult<ICondition> parsed = ICondition.SAFE_CODEC.parse(ops, map.get("forge:condition"));
                  if (parsed.result().isPresent()) {
                     ICondition condition = (ICondition)parsed.result().get();
                     if (!condition.test(context, ops)) {
                        return null;
                     }
                  }
               }

               T recipe = (T)map.get("recipe");
               return recipe == null ? DataResult.error(() -> "Missing `recipe` entry " + count.value) : Recipe.CODEC.parse(ops, recipe);
            }
         }

         public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of((T[])(new Object[]{ops.createString("forge:condition"), ops.createString("recipes")}));
         }
      }
   );
   public static final RecipeSerializer<Recipe<?>> SERIALZIER = new RecipeSerializer<Recipe<?>>() {
      @Override
      public MapCodec<Recipe<?>> codec() {
         return ConditionalRecipe.CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> streamCodec() {
         throw new UnsupportedOperationException("ConditionaRecipe.SERIALIZER does not support encoding to network");
      }
   };

   public static ConditionalRecipe.Builder builder() {
      return new ConditionalRecipe.Builder();
   }

   public static class Builder {
      private final List<ConditionalRecipe.InnerRecipe> recipes = new ArrayList<>();
      private final List<ConditionalRecipe.InnerAdvancement> advancements = new ArrayList<>();
      private final RecipeOutput bouncer = new RecipeOutput() {
         @Override
         public void accept(ResourceLocation id, Recipe<?> value, @Nullable AdvancementHolder advancement) {
            Builder.this.recipe(id, value, advancement);
         }

         @Override
         public Advancement.Builder advancement() {
            return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
         }

         @Override
         public void accept(ResourceLocation id, Recipe<?> recipe, ResourceLocation advancementId, JsonElement advancement) {
            AdvancementHolder holder = null;
            if (advancement != null) {
               Advancement adv = (Advancement)Advancement.CODEC.parse(JsonOps.INSTANCE, advancement).getOrThrow(JsonParseException::new);
               holder = new AdvancementHolder(advancementId, adv);
            }

            this.accept(id, recipe, holder);
         }

         @Override
         public HolderLookup.Provider registry() {
            return null;
         }
      };
      @Nullable
      private ICondition condition;
      @Nullable
      private ICondition mainCondition;
      @Nullable
      private ResourceLocation advancementId;

      public ConditionalRecipe.Builder mainCondition(ICondition value) {
         if (this.mainCondition != null) {
            throw new IllegalStateException("Attempted to overrride the main condition, only one is allowed to be set");
         } else {
            this.mainCondition = value;
            return this;
         }
      }

      public ConditionalRecipe.Builder condition(ICondition value) {
         if (this.condition != null) {
            throw new IllegalStateException("Attempted to override a previous set condition before adding a recipe");
         } else {
            this.condition = value;
            return this;
         }
      }

      public ConditionalRecipe.Builder recipe(Consumer<RecipeOutput> callable) {
         callable.accept(this.bouncer);
         return this;
      }

      public ConditionalRecipe.Builder recipe(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
         if (this.condition == null) {
            throw new IllegalStateException("Can not add a recipe with no conditions.");
         } else {
            this.recipes.add(new ConditionalRecipe.InnerRecipe(this.condition, recipe));
            if (advancement != null) {
               this.advancements.add(new ConditionalRecipe.InnerAdvancement(this.condition, advancement, null));
            }

            this.condition = null;
            return this;
         }
      }

      public ConditionalRecipe.Builder advancement(ResourceLocation id) {
         this.advancementId = id;
         return this;
      }

      public void save(RecipeOutput out, String namespace, String path) {
         this.save(out, ResourceLocation.fromNamespaceAndPath(namespace, path));
      }

      public void save(RecipeOutput out, ResourceLocation id) {
         if (this.condition != null) {
            throw new IllegalStateException("Invalid ConditionalRecipe builder, Orphaned conditions");
         } else if (this.recipes.isEmpty()) {
            throw new IllegalStateException("Invalid ConditionalRecipe builder, No recipes");
         } else {
            JsonElement advancement = null;
            if (!this.advancements.isEmpty()) {
               ConditionalAdvancement.Builder adv = ConditionalAdvancement.builder();

               for (ConditionalRecipe.InnerAdvancement data : this.advancements) {
                  adv.condition(data.condition());
                  if (data.json != null) {
                     adv.advancement(data.json());
                  } else {
                     adv.advancement(data.advancement());
                  }
               }

               if (this.advancementId == null) {
                  this.advancementId = id.withPrefix("recipes/");
               }

               advancement = adv.build(out.registry());
            } else {
               this.advancementId = null;
            }

            out.accept(id, new ConditionalRecipe.Wrapper(this.mainCondition, this.recipes), this.advancementId, advancement);
         }
      }
   }

   private static final class Holder<T> {
      private T value;
   }

   private static record InnerAdvancement(ICondition condition, AdvancementHolder advancement, JsonObject json) {
   }

   private static record InnerRecipe(ICondition condition, Recipe<?> recipe) {
   }

   private static class Wrapper implements Recipe<CraftingInput> {
      @Nullable
      private final ICondition main;
      private final List<ConditionalRecipe.InnerRecipe> recipes;

      public boolean matches(CraftingInput inv, Level level) {
         return false;
      }

      public ItemStack assemble(CraftingInput inv, HolderLookup.Provider reg) {
         return null;
      }

      @Override
      public boolean canCraftInDimensions(int width, int height) {
         return false;
      }

      @Override
      public ItemStack getResultItem(HolderLookup.Provider reg) {
         return null;
      }

      @Override
      public RecipeSerializer<?> getSerializer() {
         return ConditionalRecipe.SERIALZIER;
      }

      @Override
      public RecipeType<?> getType() {
         throw new UnsupportedOperationException();
      }

      private Wrapper(@Nullable ICondition main, List<ConditionalRecipe.InnerRecipe> recipes) {
         this.main = main;
         this.recipes = recipes;
      }
   }
}
