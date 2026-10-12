package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class CherryTrunkPlacer extends TrunkPlacer {
   private static final Codec<UniformInt> BRANCH_START_CODEC = UniformInt.CODEC
      .codec()
      .validate(
         p_275181_ -> p_275181_.getMaxValue() - p_275181_.getMinValue() < 1
               ? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
               : DataResult.success(p_275181_)
      );
   public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_327472_ -> trunkPlacerParts(p_327472_)
            .and(
               p_327472_.group(
                  IntProvider.codec(1, 3).fieldOf("branch_count").forGetter(p_272644_ -> p_272644_.branchCount),
                  IntProvider.codec(2, 16).fieldOf("branch_horizontal_length").forGetter(p_273612_ -> p_273612_.branchHorizontalLength),
                  IntProvider.validateCodec(-16, 0, BRANCH_START_CODEC)
                     .fieldOf("branch_start_offset_from_top")
                     .forGetter(p_272705_ -> p_272705_.branchStartOffsetFromTop),
                  IntProvider.codec(-16, 16).fieldOf("branch_end_offset_from_top").forGetter(p_273633_ -> p_273633_.branchEndOffsetFromTop)
               )
            )
            .apply(p_327472_, CherryTrunkPlacer::new)
   );
   private final IntProvider branchCount;
   private final IntProvider branchHorizontalLength;
   private final UniformInt branchStartOffsetFromTop;
   private final UniformInt secondBranchStartOffsetFromTop;
   private final IntProvider branchEndOffsetFromTop;

   public CherryTrunkPlacer(
      int p_273281_, int p_273327_, int p_272619_, IntProvider p_272873_, IntProvider p_272789_, UniformInt p_272917_, IntProvider p_272948_
   ) {
      super(p_273281_, p_273327_, p_272619_);
      this.branchCount = p_272873_;
      this.branchHorizontalLength = p_272789_;
      this.branchStartOffsetFromTop = p_272917_;
      this.secondBranchStartOffsetFromTop = UniformInt.of(p_272917_.getMinValue(), p_272917_.getMaxValue() - 1);
      this.branchEndOffsetFromTop = p_272948_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.CHERRY_TRUNK_PLACER;
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
      int $$6 = Math.max(0, pFreeTreeHeight - 1 + this.branchStartOffsetFromTop.sample(pRandom));
      int $$7 = Math.max(0, pFreeTreeHeight - 1 + this.secondBranchStartOffsetFromTop.sample(pRandom));
      if ($$7 >= $$6) {
         $$7++;
      }

      int $$8 = this.branchCount.sample(pRandom);
      boolean $$9 = $$8 == 3;
      boolean $$10 = $$8 >= 2;
      int $$11;
      if ($$9) {
         $$11 = pFreeTreeHeight;
      } else if ($$10) {
         $$11 = Math.max($$6, $$7) + 1;
      } else {
         $$11 = $$6 + 1;
      }

      for (int $$14 = 0; $$14 < $$11; $$14++) {
         this.placeLog(pLevel, pBlockSetter, pRandom, pPos.above($$14), pConfig);
      }

      List<FoliagePlacer.FoliageAttachment> $$15 = new ArrayList<>();
      if ($$9) {
         $$15.add(new FoliagePlacer.FoliageAttachment(pPos.above($$11), 0, false));
      }

      BlockPos.MutableBlockPos $$16 = new BlockPos.MutableBlockPos();
      Direction $$17 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      Function<BlockState, BlockState> $$18 = p_273382_ -> p_273382_.trySetValue(RotatedPillarBlock.AXIS, $$17.getAxis());
      $$15.add(this.generateBranch(pLevel, pBlockSetter, pRandom, pFreeTreeHeight, pPos, pConfig, $$18, $$17, $$6, $$6 < $$11 - 1, $$16));
      if ($$10) {
         $$15.add(this.generateBranch(pLevel, pBlockSetter, pRandom, pFreeTreeHeight, pPos, pConfig, $$18, $$17.getOpposite(), $$7, $$7 < $$11 - 1, $$16));
      }

      return $$15;
   }

   private FoliagePlacer.FoliageAttachment generateBranch(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      int pFreeTreeHeight,
      BlockPos pPos,
      TreeConfiguration pConfig,
      Function<BlockState, BlockState> pPropertySetter,
      Direction pDirection,
      int pSecondBranchStartOffsetFromTop,
      boolean pDoubleBranch,
      BlockPos.MutableBlockPos pCurrentPos
   ) {
      pCurrentPos.set(pPos).move(Direction.UP, pSecondBranchStartOffsetFromTop);
      int $$11 = pFreeTreeHeight - 1 + this.branchEndOffsetFromTop.sample(pRandom);
      boolean $$12 = pDoubleBranch || $$11 < pSecondBranchStartOffsetFromTop;
      int $$13 = this.branchHorizontalLength.sample(pRandom) + ($$12 ? 1 : 0);
      BlockPos $$14 = pPos.relative(pDirection, $$13).above($$11);
      int $$15 = $$12 ? 2 : 1;

      for (int $$16 = 0; $$16 < $$15; $$16++) {
         this.placeLog(pLevel, pBlockSetter, pRandom, pCurrentPos.move(pDirection), pConfig, pPropertySetter);
      }

      Direction $$17 = $$14.getY() > pCurrentPos.getY() ? Direction.UP : Direction.DOWN;

      while (true) {
         int $$18 = pCurrentPos.distManhattan($$14);
         if ($$18 == 0) {
            return new FoliagePlacer.FoliageAttachment($$14.above(), 0, false);
         }

         float $$19 = (float)Math.abs($$14.getY() - pCurrentPos.getY()) / (float)$$18;
         boolean $$20 = pRandom.nextFloat() < $$19;
         pCurrentPos.move($$20 ? $$17 : pDirection);
         this.placeLog(pLevel, pBlockSetter, pRandom, pCurrentPos, pConfig, $$20 ? Function.identity() : pPropertySetter);
      }
   }
}
