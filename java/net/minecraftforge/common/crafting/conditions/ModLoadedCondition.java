package net.minecraftforge.common.crafting.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.fml.ModList;

public record ModLoadedCondition(String modid) implements ICondition {
   public static final MapCodec<ModLoadedCondition> CODEC = RecordCodecBuilder.mapCodec(
      b -> b.group(Codec.STRING.fieldOf("modid").forGetter(ModLoadedCondition::modid)).apply(b, ModLoadedCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context, DynamicOps<?> ops) {
      return ModList.get().isLoaded(this.modid);
   }

   @Override
   public String toString() {
      return "mod_loaded(\"" + this.modid + "\")";
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
