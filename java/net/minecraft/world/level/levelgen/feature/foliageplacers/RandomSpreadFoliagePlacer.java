package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class RandomSpreadFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<RandomSpreadFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_161522_ -> foliagePlacerParts(p_161522_)
            .and(
               p_161522_.group(
                  IntProvider.codec(1, 512).fieldOf("foliage_height").forGetter(p_161537_ -> p_161537_.foliageHeight),
                  Codec.intRange(0, 256).fieldOf("leaf_placement_attempts").forGetter(p_161524_ -> p_161524_.leafPlacementAttempts)
               )
            )
            .apply(p_161522_, RandomSpreadFoliagePlacer::new)
   );
   private final IntProvider foliageHeight;
   private final int leafPlacementAttempts;

   public RandomSpreadFoliagePlacer(IntProvider p_161506_, IntProvider p_161507_, IntProvider p_161508_, int p_161509_) {
      super(p_161506_, p_161507_);
      this.foliageHeight = p_161508_;
      this.leafPlacementAttempts = p_161509_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.RANDOM_SPREAD_FOLIAGE_PLACER;
   }

   @Override
   protected void createFoliage(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pBlockSetter,
      RandomSource pRandom,
      TreeConfiguration pConfig,
      int pMaxFreeTreeHeight,
      FoliagePlacer.FoliageAttachment pAttachment,
      int pFoliageHeight,
      int pFoliageRadius,
      int pOffset
   ) {
      BlockPos $$9 = pAttachment.pos();
      BlockPos.MutableBlockPos $$10 = $$9.mutable();

      for (int $$11 = 0; $$11 < this.leafPlacementAttempts; $$11++) {
         $$10.setWithOffset(
            $$9,
            pRandom.nextInt(pFoliageRadius) - pRandom.nextInt(pFoliageRadius),
            pRandom.nextInt(pFoliageHeight) - pRandom.nextInt(pFoliageHeight),
            pRandom.nextInt(pFoliageRadius) - pRandom.nextInt(pFoliageRadius)
         );
         tryPlaceLeaf(pLevel, pBlockSetter, pRandom, pConfig, $$10);
      }
   }

   @Override
   public int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig) {
      return this.foliageHeight.sample(pRandom);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      return false;
   }
}
