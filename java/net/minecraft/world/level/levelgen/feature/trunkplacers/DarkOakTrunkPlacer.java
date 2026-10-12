package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70090_ -> trunkPlacerParts(p_70090_).apply(p_70090_, DarkOakTrunkPlacer::new)
   );

   public DarkOakTrunkPlacer(int p_70077_, int p_70078_, int p_70079_) {
      super(p_70077_, p_70078_, p_70079_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   @Override
   public List<FoliagePlacer.FoliageAttachment> placeTrunk(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      int pFreeTreeHeight,
      BlockPos pPos,
      TreeConfiguration pConfig
   ) {
      List<FoliagePlacer.FoliageAttachment> $$6 = Lists.newArrayList();
      BlockPos $$7 = pPos.below();
      setDirtAt(pLevel, pBlockSetter, pRandom, $$7, pConfig);
      setDirtAt(pLevel, pBlockSetter, pRandom, $$7.east(), pConfig);
      setDirtAt(pLevel, pBlockSetter, pRandom, $$7.south(), pConfig);
      setDirtAt(pLevel, pBlockSetter, pRandom, $$7.south().east(), pConfig);
      Direction $$8 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      int $$9 = pFreeTreeHeight - pRandom.nextInt(4);
      int $$10 = 2 - pRandom.nextInt(3);
      int $$11 = pPos.getX();
      int $$12 = pPos.getY();
      int $$13 = pPos.getZ();
      int $$14 = $$11;
      int $$15 = $$13;
      int $$16 = $$12 + pFreeTreeHeight - 1;

      for (int $$17 = 0; $$17 < pFreeTreeHeight; $$17++) {
         if ($$17 >= $$9 && $$10 > 0) {
            $$14 += $$8.getStepX();
            $$15 += $$8.getStepZ();
            $$10--;
         }

         int $$18 = $$12 + $$17;
         BlockPos $$19 = new BlockPos($$14, $$18, $$15);
         if (TreeFeature.isAirOrLeaves(pLevel, $$19)) {
            this.placeLog(pLevel, pBlockSetter, pRandom, $$19, pConfig);
            this.placeLog(pLevel, pBlockSetter, pRandom, $$19.east(), pConfig);
            this.placeLog(pLevel, pBlockSetter, pRandom, $$19.south(), pConfig);
            this.placeLog(pLevel, pBlockSetter, pRandom, $$19.east().south(), pConfig);
         }
      }

      $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$14, $$16, $$15), 0, true));

      for (int $$20 = -1; $$20 <= 2; $$20++) {
         for (int $$21 = -1; $$21 <= 2; $$21++) {
            if (($$20 < 0 || $$20 > 1 || $$21 < 0 || $$21 > 1) && pRandom.nextInt(3) <= 0) {
               int $$22 = pRandom.nextInt(3) + 2;

               for (int $$23 = 0; $$23 < $$22; $$23++) {
                  this.placeLog(pLevel, pBlockSetter, pRandom, new BlockPos($$11 + $$20, $$16 - $$23 - 1, $$13 + $$21), pConfig);
               }

               $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$14 + $$20, $$16, $$15 + $$21), 0, false));
            }
         }
      }

      return $$6;
   }
}
