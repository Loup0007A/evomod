package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.slf4j.Logger;

public class UniformHeight extends HeightProvider {
   public static final MapCodec<UniformHeight> CODEC = RecordCodecBuilder.mapCodec(
      p_162033_ -> p_162033_.group(
               VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(p_162043_ -> p_162043_.minInclusive),
               VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(p_162038_ -> p_162038_.maxInclusive)
            )
            .apply(p_162033_, UniformHeight::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final LongSet warnedFor = new LongOpenHashSet();

   private UniformHeight(VerticalAnchor p_162029_, VerticalAnchor p_162030_) {
      this.minInclusive = p_162029_;
      this.maxInclusive = p_162030_;
   }

   public static UniformHeight of(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive) {
      return new UniformHeight(pMinInclusive, pMaxInclusive);
   }

   @Override
   public int sample(RandomSource pRandom, WorldGenerationContext pContext) {
      int $$2 = this.minInclusive.resolveY(pContext);
      int $$3 = this.maxInclusive.resolveY(pContext);
      if ($$2 > $$3) {
         if (this.warnedFor.add((long)$$2 << 32 | (long)$$3)) {
            LOGGER.warn("Empty height range: {}", this);
         }

         return $$2;
      } else {
         return Mth.randomBetweenInclusive(pRandom, $$2, $$3);
      }
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.UNIFORM;
   }

   @Override
   public String toString() {
      return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
   }
}
