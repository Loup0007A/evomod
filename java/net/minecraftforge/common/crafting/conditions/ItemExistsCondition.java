package net.minecraftforge.common.crafting.conditions;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public record ItemExistsCondition(ResourceLocation item) implements ICondition {
   public static final MapCodec<ItemExistsCondition> CODEC = RecordCodecBuilder.mapCodec(
      b -> b.group(ResourceLocation.CODEC.fieldOf("item").forGetter(ItemExistsCondition::item)).apply(b, ItemExistsCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context, DynamicOps<?> ops) {
      if (ops instanceof RegistryOps<?> reg) {
         Optional<HolderGetter<Item>> items = reg.getter(Registries.ITEM);
         if (items.isPresent()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, this.item);
            return items.get().get(key).isPresent();
         }
      }

      return ForgeRegistries.ITEMS.containsKey(this.item);
   }

   @Override
   public String toString() {
      return "item_exists(\"" + this.item + "\")";
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
