package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class MultiNoiseBiomeSource extends BiomeSource {
   private static final MapCodec<Holder<Biome>> ENTRY_CODEC = Biome.CODEC.fieldOf("biome");
   public static final MapCodec<Climate.ParameterList<Holder<Biome>>> DIRECT_CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes");
   private static final MapCodec<Holder<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC = MultiNoiseBiomeSourceParameterList.CODEC
      .fieldOf("preset")
      .withLifecycle(Lifecycle.stable());
   public static final MapCodec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(DIRECT_CODEC, PRESET_CODEC)
      .xmap(MultiNoiseBiomeSource::new, p_275170_ -> p_275170_.parameters);
   private final Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

   private MultiNoiseBiomeSource(Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> p_275370_) {
      this.parameters = p_275370_;
   }

   public static MultiNoiseBiomeSource createFromList(Climate.ParameterList<Holder<Biome>> pParameters) {
      return new MultiNoiseBiomeSource(Either.left(pParameters));
   }

   public static MultiNoiseBiomeSource createFromPreset(Holder<MultiNoiseBiomeSourceParameterList> pParameters) {
      return new MultiNoiseBiomeSource(Either.right(pParameters));
   }

   private Climate.ParameterList<Holder<Biome>> parameters() {
      return (Climate.ParameterList<Holder<Biome>>)this.parameters
         .map(p_275171_ -> p_275171_, p_275172_ -> ((MultiNoiseBiomeSourceParameterList)p_275172_.value()).parameters());
   }

   @Override
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return this.parameters().values().stream().map(Pair::getSecond);
   }

   @Override
   protected MapCodec<? extends BiomeSource> codec() {
      return CODEC;
   }

   public boolean stable(ResourceKey<MultiNoiseBiomeSourceParameterList> pResourceKey) {
      Optional<Holder<MultiNoiseBiomeSourceParameterList>> $$1 = this.parameters.right();
      return $$1.isPresent() && $$1.get().is(pResourceKey);
   }

   @Override
   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ, Climate.Sampler pSampler) {
      return this.getNoiseBiome(pSampler.sample(pX, pY, pZ));
   }

   @VisibleForDebug
   public Holder<Biome> getNoiseBiome(Climate.TargetPoint pTargetPoint) {
      return this.parameters().findValue(pTargetPoint);
   }

   @Override
   public void addDebugInfo(List<String> pInfo, BlockPos pPos, Climate.Sampler pSampler) {
      int $$3 = QuartPos.fromBlock(pPos.getX());
      int $$4 = QuartPos.fromBlock(pPos.getY());
      int $$5 = QuartPos.fromBlock(pPos.getZ());
      Climate.TargetPoint $$6 = pSampler.sample($$3, $$4, $$5);
      float $$7 = Climate.unquantizeCoord($$6.continentalness());
      float $$8 = Climate.unquantizeCoord($$6.erosion());
      float $$9 = Climate.unquantizeCoord($$6.temperature());
      float $$10 = Climate.unquantizeCoord($$6.humidity());
      float $$11 = Climate.unquantizeCoord($$6.weirdness());
      double $$12 = (double)NoiseRouterData.peaksAndValleys($$11);
      OverworldBiomeBuilder $$13 = new OverworldBiomeBuilder();
      pInfo.add(
         "Biome builder PV: "
            + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys($$12)
            + " C: "
            + $$13.getDebugStringForContinentalness((double)$$7)
            + " E: "
            + $$13.getDebugStringForErosion((double)$$8)
            + " T: "
            + $$13.getDebugStringForTemperature((double)$$9)
            + " H: "
            + $$13.getDebugStringForHumidity((double)$$10)
      );
   }
}
