package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaJungleFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaJungleFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68630_ -> foliagePlacerParts(p_68630_)
            .and(Codec.intRange(0, 16).fieldOf("height").forGetter(p_161468_ -> p_161468_.height))
            .apply(p_68630_, MegaJungleFoliagePlacer::new)
   );
   protected final int height;

   public MegaJungleFoliagePlacer(IntProvider p_161454_, IntProvider p_161455_, int p_161456_) {
      super(p_161454_, p_161455_);
      this.height = p_161456_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_JUNGLE_FOLIAGE_PLACER;
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
      int $$9 = pAttachment.doubleTrunk() ? pFoliageHeight : 1 + pRandom.nextInt(2);

      for (int $$10 = pOffset; $$10 >= pOffset - $$9; $$10--) {
         int $$11 = pFoliageRadius + pAttachment.radiusOffset() + 1 - $$10;
         this.placeLeavesRow(pLevel, pBlockSetter, pRandom, pConfig, pAttachment.pos(), $$11, $$10, pAttachment.doubleTrunk());
      }
   }

   @Override
   public int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig) {
      return this.height;
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      return pLocalX + pLocalZ >= 7 ? true : pLocalX * pLocalX + pLocalZ * pLocalZ > pRange * pRange;
   }
}
