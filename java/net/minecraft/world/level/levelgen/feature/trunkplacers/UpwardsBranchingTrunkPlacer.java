package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class UpwardsBranchingTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_259008_ -> trunkPlacerParts(p_259008_)
            .and(
               p_259008_.group(
                  IntProvider.POSITIVE_CODEC.fieldOf("extra_branch_steps").forGetter(p_226242_ -> p_226242_.extraBranchSteps),
                  Codec.floatRange(0.0F, 1.0F).fieldOf("place_branch_per_log_probability").forGetter(p_226240_ -> p_226240_.placeBranchPerLogProbability),
                  IntProvider.NON_NEGATIVE_CODEC.fieldOf("extra_branch_length").forGetter(p_226238_ -> p_226238_.extraBranchLength),
                  RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_grow_through").forGetter(p_226234_ -> p_226234_.canGrowThrough)
               )
            )
            .apply(p_259008_, UpwardsBranchingTrunkPlacer::new)
   );
   private final IntProvider extraBranchSteps;
   private final float placeBranchPerLogProbability;
   private final IntProvider extraBranchLength;
   private final HolderSet<Block> canGrowThrough;

   public UpwardsBranchingTrunkPlacer(
      int p_226201_, int p_226202_, int p_226203_, IntProvider p_226204_, float p_226205_, IntProvider p_226206_, HolderSet<Block> p_226207_
   ) {
      super(p_226201_, p_226202_, p_226203_);
      this.extraBranchSteps = p_226204_;
      this.placeBranchPerLogProbability = p_226205_;
      this.extraBranchLength = p_226206_;
      this.canGrowThrough = p_226207_;
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
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
      BlockPos.MutableBlockPos $$7 = new BlockPos.MutableBlockPos();

      for (int $$8 = 0; $$8 < pFreeTreeHeight; $$8++) {
         int $$9 = pPos.getY() + $$8;
         if (this.placeLog(pLevel, pBlockSetter, pRandom, $$7.set(pPos.getX(), $$9, pPos.getZ()), pConfig)
            && $$8 < pFreeTreeHeight - 1
            && pRandom.nextFloat() < this.placeBranchPerLogProbability) {
            Direction $$10 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
            int $$11 = this.extraBranchLength.sample(pRandom);
            int $$12 = Math.max(0, $$11 - this.extraBranchLength.sample(pRandom) - 1);
            int $$13 = this.extraBranchSteps.sample(pRandom);
            this.placeBranch(pLevel, pBlockSetter, pRandom, pFreeTreeHeight, pConfig, $$6, $$7, $$9, $$10, $$12, $$13);
         }

         if ($$8 == pFreeTreeHeight - 1) {
            $$6.add(new FoliagePlacer.FoliageAttachment($$7.set(pPos.getX(), $$9 + 1, pPos.getZ()), 0, false));
         }
      }

      return $$6;
   }

   private void placeBranch(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      int pFreeTreeHeight,
      TreeConfiguration pTreeConfig,
      List<FoliagePlacer.FoliageAttachment> pFoliageAttachments,
      BlockPos.MutableBlockPos pPos,
      int pY,
      Direction pDirection,
      int pExtraBranchLength,
      int pExtraBranchSteps
   ) {
      int $$11 = pY + pExtraBranchLength;
      int $$12 = pPos.getX();
      int $$13 = pPos.getZ();
      int $$14 = pExtraBranchLength;

      while ($$14 < pFreeTreeHeight && pExtraBranchSteps > 0) {
         if ($$14 >= 1) {
            int $$15 = pY + $$14;
            $$12 += pDirection.getStepX();
            $$13 += pDirection.getStepZ();
            $$11 = $$15;
            if (this.placeLog(pLevel, pBlockSetter, pRandom, pPos.set($$12, $$15, $$13), pTreeConfig)) {
               $$11 = $$15 + 1;
            }

            pFoliageAttachments.add(new FoliagePlacer.FoliageAttachment(pPos.immutable(), 0, false));
         }

         $$14++;
         pExtraBranchSteps--;
      }

      if ($$11 - pY > 1) {
         BlockPos $$16 = new BlockPos($$12, $$11, $$13);
         pFoliageAttachments.add(new FoliagePlacer.FoliageAttachment($$16, 0, false));
         pFoliageAttachments.add(new FoliagePlacer.FoliageAttachment($$16.below(2), 0, false));
      }
   }

   @Override
   protected boolean validTreePos(LevelSimulatedReader pLevel, BlockPos pPos) {
      return super.validTreePos(pLevel, pPos) || pLevel.isStateAtPosition(pPos, p_226232_ -> p_226232_.is(this.canGrowThrough));
   }
}
