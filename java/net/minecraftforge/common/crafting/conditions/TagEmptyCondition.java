package net.minecraftforge.common.crafting.conditions;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record TagEmptyCondition(TagKey<Item> tag) implements ICondition {
   public static final MapCodec<TagEmptyCondition> CODEC = RecordCodecBuilder.mapCodec(
      b -> b.group(ResourceLocation.CODEC.xmap(loc -> TagKey.create(Registries.ITEM, loc), TagKey::location).fieldOf("tag").forGetter(TagEmptyCondition::tag))
            .apply(b, TagEmptyCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context, DynamicOps<?> ops) {
      if (ops instanceof RegistryOps<?> reg) {
         Optional<HolderGetter<Item>> items = reg.getter(Registries.ITEM);
         if (items.isPresent()) {
            Optional<HolderSet.Named<Item>> optional = items.get().get(this.tag);
            return optional.isPresent();
         }
      }

      return context.getTag(this.tag).isEmpty();
   }

   @Override
   public String toString() {
      return "tag_empty(\"" + this.tag.location() + "\")";
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
