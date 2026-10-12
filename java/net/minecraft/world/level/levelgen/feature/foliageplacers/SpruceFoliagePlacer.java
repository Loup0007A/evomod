package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class SpruceFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<SpruceFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68735_ -> foliagePlacerParts(p_68735_)
            .and(IntProvider.codec(0, 24).fieldOf("trunk_height").forGetter(p_161553_ -> p_161553_.trunkHeight))
            .apply(p_68735_, SpruceFoliagePlacer::new)
   );
   private final IntProvider trunkHeight;

   public SpruceFoliagePlacer(IntProvider p_161539_, IntProvider p_161540_, IntProvider p_161541_) {
      super(p_161539_, p_161540_);
      this.trunkHeight = p_161541_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.SPRUCE_FOLIAGE_PLACER;
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
      int $$10 = pRandom.nextInt(2);
      int $$11 = 1;
      int $$12 = 0;

      for (int $$13 = pOffset; $$13 >= -pFoliageHeight; $$13--) {
         this.placeLeavesRow(pLevel, pBlockSetter, pRandom, pConfig, $$9, $$10, $$13, pAttachment.doubleTrunk());
         if ($$10 >= $$11) {
            $$10 = $$12;
            $$12 = 1;
            $$11 = Math.min($$11 + 1, pFoliageRadius + pAttachment.radiusOffset());
         } else {
            $$10++;
         }
      }
   }

   @Override
   public int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig) {
      return Math.max(4, pHeight - this.trunkHeight.sample(pRandom));
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      return pLocalX == pRange && pLocalZ == pRange && pRange > 0;
   }
}
