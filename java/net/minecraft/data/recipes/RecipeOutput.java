package net.minecraft.data.recipes;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeOutput {
   default void accept(ResourceLocation pLocation, Recipe<?> pRecipe, @Nullable AdvancementHolder pAdvancement) {
      if (pAdvancement == null) {
         this.accept(pLocation, pRecipe, null, null);
      } else {
         RegistryOps<JsonElement> ops = this.registry().createSerializationContext(JsonOps.INSTANCE);
         JsonElement json = (JsonElement)Advancement.CODEC.encodeStart(ops, pAdvancement.value()).getOrThrow(IllegalStateException::new);
         this.accept(pLocation, pRecipe, pAdvancement.id(), json);
      }
   }

   void accept(ResourceLocation var1, Recipe<?> var2, @Nullable ResourceLocation var3, @Nullable JsonElement var4);

   HolderLookup.Provider registry();

   Advancement.Builder advancement();
}
