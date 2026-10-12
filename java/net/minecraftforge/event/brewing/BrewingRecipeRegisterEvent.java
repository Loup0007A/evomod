package net.minecraftforge.event.brewing;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class BrewingRecipeRegisterEvent extends Event {
   private final PotionBrewing.Builder builder;
   private final FeatureFlagSet features;

   @Internal
   public BrewingRecipeRegisterEvent(PotionBrewing.Builder builder, FeatureFlagSet features) {
      this.builder = builder;
      this.features = features;
   }

   public PotionBrewing.Builder getBuilder() {
      return this.builder;
   }

   public FeatureFlagSet getFeatures() {
      return this.features;
   }

   public void addRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
      this.addRecipe(new BrewingRecipe(input, ingredient, output));
   }

   public void addRecipe(IBrewingRecipe recipe) {
      this.getBuilder().add(recipe);
   }
}
