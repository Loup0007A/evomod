package net.minecraftforge.common.crafting.conditions;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NotCondition(ICondition child) implements ICondition {
   public static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(
      b -> b.group(ICondition.CODEC.fieldOf("value").forGetter(NotCondition::child)).apply(b, NotCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context, DynamicOps<?> ops) {
      return !this.child.test(context, ops);
   }

   @Override
   public String toString() {
      return "!" + this.child;
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }
}
