package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class RandomSequence {
   public static final Codec<RandomSequence> CODEC = RecordCodecBuilder.create(
      p_287586_ -> p_287586_.group(XoroshiroRandomSource.CODEC.fieldOf("source").forGetter(p_287757_ -> p_287757_.source))
            .apply(p_287586_, RandomSequence::new)
   );
   private final XoroshiroRandomSource source;

   public RandomSequence(XoroshiroRandomSource p_287597_) {
      this.source = p_287597_;
   }

   public RandomSequence(long pSeed, ResourceLocation pLocation) {
      this(createSequence(pSeed, Optional.of(pLocation)));
   }

   public RandomSequence(long pSeed, Optional<ResourceLocation> pLocation) {
      this(createSequence(pSeed, pLocation));
   }

   private static XoroshiroRandomSource createSequence(long pSeed, Optional<ResourceLocation> pLocation) {
      RandomSupport.Seed128bit $$2 = RandomSupport.upgradeSeedTo128bitUnmixed(pSeed);
      if (pLocation.isPresent()) {
         $$2 = $$2.xor(seedForKey(pLocation.get()));
      }

      return new XoroshiroRandomSource($$2.mixed());
   }

   public static RandomSupport.Seed128bit seedForKey(ResourceLocation pKey) {
      return RandomSupport.seedFromHashOf(pKey.toString());
   }

   public RandomSource random() {
      return this.source;
   }
}
