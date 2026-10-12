package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record RemoveBinomial(LevelBasedValue chance) implements EnchantmentValueEffect {
   public static final MapCodec<RemoveBinomial> CODEC = RecordCodecBuilder.mapCodec(
      p_345282_ -> p_345282_.group(LevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomial::chance)).apply(p_345282_, RemoveBinomial::new)
   );

   @Override
   public float process(int pEnchantmentLevel, RandomSource pRandom, float pValue) {
      float $$3 = this.chance.calculate(pEnchantmentLevel);
      int $$4 = 0;

      for (int $$5 = 0; (float)$$5 < pValue; $$5++) {
         if (pRandom.nextFloat() < $$3) {
            $$4++;
         }
      }

      return pValue - (float)$$4;
   }

   @Override
   public MapCodec<RemoveBinomial> codec() {
      return CODEC;
   }
}
