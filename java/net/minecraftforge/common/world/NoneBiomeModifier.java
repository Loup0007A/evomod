package net.minecraftforge.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class NoneBiomeModifier implements BiomeModifier {
   public static final NoneBiomeModifier INSTANCE = new NoneBiomeModifier();
   public static final MapCodec<NoneBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

   @Override
   public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
   }

   @Override
   public MapCodec<? extends BiomeModifier> codec() {
      return CODEC;
   }
}
