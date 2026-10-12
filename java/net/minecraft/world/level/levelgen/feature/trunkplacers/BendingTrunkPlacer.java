package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_161786_ -> trunkPlacerParts(p_161786_)
            .and(
               p_161786_.group(
                  ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_height_for_leaves", 1).forGetter(p_161788_ -> p_161788_.minHeightForLeaves),
                  IntProvider.codec(1, 64).fieldOf("bend_length").forGetter(p_161784_ -> p_161784_.bendLength)
               )
            )
            .apply(p_161786_, BendingTrunkPlacer::new)
   );
   private final int minHeightForLeaves;
   private final IntProvider bendLength;

   public BendingTrunkPlacer(int p_161770_, int p_161771_, int p_161772_, int p_161773_, IntProvider p_161774_) {
      super(p_161770_, p_161771_, p_161772_);
      this.minHeightForLeaves = p_161773_;
      this.bendLength = p_161774_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.BENDING_TRUNK_PLACER;
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
      Direction $$6 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      int $$7 = pFreeTreeHeight - 1;
      BlockPos.MutableBlockPos $$8 = pPos.mutable();
      BlockPos $$9 = $$8.below();
      setDirtAt(pLevel, pBlockSetter, pRandom, $$9, pConfig);
      List<FoliagePlacer.FoliageAttachment> $$10 = Lists.newArrayList();

      for (int $$11 = 0; $$11 <= $$7; $$11++) {
         if ($$11 + 1 >= $$7 + pRandom.nextInt(2)) {
            $$8.move($$6);
         }

         if (TreeFeature.validTreePos(pLevel, $$8)) {
            this.placeLog(pLevel, pBlockSetter, pRandom, $$8, pConfig);
         }

         if ($$11 >= this.minHeightForLeaves) {
            $$10.add(new FoliagePlacer.FoliageAttachment($$8.immutable(), 0, false));
         }

         $$8.move(Direction.UP);
      }

      int $$12 = this.bendLength.sample(pRandom);

      for (int $$13 = 0; $$13 <= $$12; $$13++) {
         if (TreeFeature.validTreePos(pLevel, $$8)) {
            this.placeLog(pLevel, pBlockSetter, pRandom, $$8, pConfig);
         }

         $$10.add(new FoliagePlacer.FoliageAttachment($$8.immutable(), 0, false));
         $$8.move($$6);
      }

      return $$10;
   }
}
