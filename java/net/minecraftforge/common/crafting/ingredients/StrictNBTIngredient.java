package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class StrictNBTIngredient extends AbstractIngredient {
   private final ItemStack stack;
   public static final MapCodec<StrictNBTIngredient> CODEC = RecordCodecBuilder.mapCodec(
      b -> b.group(ItemStack.CODEC.fieldOf("stack").forGetter(i -> i.stack)).apply(b, StrictNBTIngredient::new)
   );
   public static final IIngredientSerializer<StrictNBTIngredient> SERIALIZER = new IIngredientSerializer<StrictNBTIngredient>() {
      @Override
      public MapCodec<? extends StrictNBTIngredient> codec() {
         return StrictNBTIngredient.CODEC;
      }

      public void write(RegistryFriendlyByteBuf buffer, StrictNBTIngredient value) {
         ItemStack.STREAM_CODEC.encode(buffer, value.stack);
      }

      public StrictNBTIngredient read(RegistryFriendlyByteBuf buffer) {
         ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
         return new StrictNBTIngredient(stack);
      }
   };

   public static StrictNBTIngredient of(ItemStack stack) {
      return new StrictNBTIngredient(stack);
   }

   private StrictNBTIngredient(ItemStack stack) {
      super(Stream.of(new Ingredient.ItemValue(stack)));
      this.stack = stack;
   }

   @Override
   public boolean test(@Nullable ItemStack input) {
      if (input == null) {
         return false;
      } else {
         CustomData inbt = input.get(DataComponents.CUSTOM_DATA);
         CustomData tnbt = this.stack.get(DataComponents.CUSTOM_DATA);
         return inbt != null && tnbt != null ? this.stack.getItem() == input.getItem() && tnbt.matchedBy(inbt.copyTag()) : false;
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
}
