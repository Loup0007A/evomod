package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface IIngredientSerializer<T extends Ingredient> {
   IIngredientSerializer<Ingredient> VANILLA = new IIngredientSerializer<Ingredient>() {
      @Override
      public MapCodec<? extends Ingredient> codec() {
         return Ingredient.VANILLA_MAP_CODEC;
      }

      @Override
      public void write(RegistryFriendlyByteBuf buffer, Ingredient value) {
         ItemStack.LIST_STREAM_CODEC.encode(buffer, Arrays.asList(value.getItems()));
      }

      @Override
      public Ingredient read(RegistryFriendlyByteBuf buffer) {
         return Ingredient.of(ItemStack.LIST_STREAM_CODEC.decode(buffer).stream());
      }
   };

   MapCodec<? extends T> codec();

   void write(RegistryFriendlyByteBuf var1, T var2);

   T read(RegistryFriendlyByteBuf var1);
}
