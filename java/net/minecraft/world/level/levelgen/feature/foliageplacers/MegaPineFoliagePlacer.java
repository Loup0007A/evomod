package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class MegaPineFoliagePlacer extends FoliagePlacer {
   public static final MapCodec<MegaPineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_68664_ -> foliagePlacerParts(p_68664_)
            .and(IntProvider.codec(0, 24).fieldOf("crown_height").forGetter(p_161484_ -> p_161484_.crownHeight))
            .apply(p_68664_, MegaPineFoliagePlacer::new)
   );
   private final IntProvider crownHeight;

   public MegaPineFoliagePlacer(IntProvider p_161470_, IntProvider p_161471_, IntProvider p_161472_) {
      super(p_161470_, p_161471_);
      this.crownHeight = p_161472_;
   }

   @Override
   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.MEGA_PINE_FOLIAGE_PLACER;
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
      int $$10 = 0;

      for (int $$11 = $$9.getY() - pFoliageHeight + pOffset; $$11 <= $$9.getY() + pOffset; $$11++) {
         int $$12 = $$9.getY() - $$11;
         int $$13 = pFoliageRadius + pAttachment.radiusOffset() + Mth.floor((float)$$12 / (float)pFoliageHeight * 3.5F);
         int $$14;
         if ($$12 > 0 && $$13 == $$10 && ($$11 & 1) == 0) {
            $$14 = $$13 + 1;
         } else {
            $$14 = $$13;
         }

         this.placeLeavesRow(pLevel, pBlockSetter, pRandom, pConfig, new BlockPos($$9.getX(), $$11, $$9.getZ()), $$14, 0, pAttachment.doubleTrunk());
         $$10 = $$13;
      }
   }

   @Override
   public int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig) {
      return this.crownHeight.sample(pRandom);
   }

   @Override
   protected boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      return pLocalX + pLocalZ >= 7 ? true : pLocalX * pLocalX + pLocalZ * pLocalZ > pRange * pRange;
   }
}
