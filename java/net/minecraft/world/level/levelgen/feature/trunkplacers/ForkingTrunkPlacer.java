package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class ForkingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70161_ -> trunkPlacerParts(p_70161_).apply(p_70161_, ForkingTrunkPlacer::new)
   );

   public ForkingTrunkPlacer(int p_70148_, int p_70149_, int p_70150_) {
      super(p_70148_, p_70149_, p_70150_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FORKING_TRUNK_PLACER;
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
      setDirtAt(pLevel, pBlockSetter, pRandom, pPos.below(), pConfig);
      List<FoliagePlacer.FoliageAttachment> $$6 = Lists.newArrayList();
      Direction $$7 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      int $$8 = pFreeTreeHeight - pRandom.nextInt(4) - 1;
      int $$9 = 3 - pRandom.nextInt(3);
      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();
      int $$11 = pPos.getX();
      int $$12 = pPos.getZ();
      OptionalInt $$13 = OptionalInt.empty();

      for (int $$14 = 0; $$14 < pFreeTreeHeight; $$14++) {
         int $$15 = pPos.getY() + $$14;
         if ($$14 >= $$8 && $$9 > 0) {
            $$11 += $$7.getStepX();
            $$12 += $$7.getStepZ();
            $$9--;
         }

         if (this.placeLog(pLevel, pBlockSetter, pRandom, $$10.set($$11, $$15, $$12), pConfig)) {
            $$13 = OptionalInt.of($$15 + 1);
         }
      }

      if ($$13.isPresent()) {
         $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$11, $$13.getAsInt(), $$12), 1, false));
      }

      $$11 = pPos.getX();
      $$12 = pPos.getZ();
      Direction $$16 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      if ($$16 != $$7) {
         int $$17 = $$8 - pRandom.nextInt(2) - 1;
         int $$18 = 1 + pRandom.nextInt(3);
         $$13 = OptionalInt.empty();

         for (int $$19 = $$17; $$19 < pFreeTreeHeight && $$18 > 0; $$18--) {
            if ($$19 >= 1) {
               int $$20 = pPos.getY() + $$19;
               $$11 += $$16.getStepX();
               $$12 += $$16.getStepZ();
               if (this.placeLog(pLevel, pBlockSetter, pRandom, $$10.set($$11, $$20, $$12), pConfig)) {
                  $$13 = OptionalInt.of($$20 + 1);
               }
            }

            $$19++;
         }

         if ($$13.isPresent()) {
            $$6.add(new FoliagePlacer.FoliageAttachment(new BlockPos($$11, $$13.getAsInt(), $$12), 0, false));
         }
      }

      return $$6;
   }
}
