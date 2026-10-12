package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class TrapezoidHeight extends HeightProvider {
   public static final MapCodec<TrapezoidHeight> CODEC = RecordCodecBuilder.mapCodec(
      p_162005_ -> p_162005_.group(
               VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(p_162021_ -> p_162021_.minInclusive),
               VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(p_162019_ -> p_162019_.maxInclusive),
               Codec.INT.optionalFieldOf("plateau", 0).forGetter(p_162014_ -> p_162014_.plateau)
            )
            .apply(p_162005_, TrapezoidHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int plateau;

   private TrapezoidHeight(VerticalAnchor p_162000_, VerticalAnchor p_162001_, int p_162002_) {
      this.minInclusive = p_162000_;
      this.maxInclusive = p_162001_;
      this.plateau = p_162002_;
   }

   public static TrapezoidHeight of(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive, int pPlateau) {
      return new TrapezoidHeight(pMinInclusive, pMaxInclusive, pPlateau);
   }

   public static TrapezoidHeight of(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive) {
      return of(pMinInclusive, pMaxInclusive, 0);
   }

   @Override
   public int sample(RandomSource pRandom, WorldGenerationContext pContext) {
      int $$2 = this.minInclusive.resolveY(pContext);
      int $$3 = this.maxInclusive.resolveY(pContext);
      if ($$2 > $$3) {
         LOGGER.warn("Empty height range: {}", this);
         return $$2;
      } else {
         int $$4 = $$3 - $$2;
         if (this.plateau >= $$4) {
            return Mth.randomBetweenInclusive(pRandom, $$2, $$3);
         } else {
            int $$5 = ($$4 - this.plateau) / 2;
            int $$6 = $$4 - $$5;
            return $$2 + Mth.randomBetweenInclusive(pRandom, 0, $$6) + Mth.randomBetweenInclusive(pRandom, 0, $$5);
         }
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.TRAPEZOID;
   }

   @Override
   public String toString() {
      return this.plateau == 0
         ? "triangle (" + this.minInclusive + "-" + this.maxInclusive + ")"
         : "trapezoid(" + this.plateau + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
