package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MangroveRootPlacer extends RootPlacer {
   public static final int ROOT_WIDTH_LIMIT = 8;
   public static final int ROOT_LENGTH_LIMIT = 15;
   public static final MapCodec<MangroveRootPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_225856_ -> rootPlacerParts(p_225856_)
            .and(MangroveRootPlacement.CODEC.fieldOf("mangrove_root_placement").forGetter(p_225849_ -> p_225849_.mangroveRootPlacement))
            .apply(p_225856_, MangroveRootPlacer::new)
   );
   private final MangroveRootPlacement mangroveRootPlacement;

   public MangroveRootPlacer(IntProvider p_225817_, BlockStateProvider p_225818_, Optional<AboveRootPlacement> p_225819_, MangroveRootPlacement p_225820_) {
      super(p_225817_, p_225818_, p_225819_);
      this.mangroveRootPlacement = p_225820_;
   }

   @Override
   public boolean placeRoots(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      BlockPos pPos,
      BlockPos pTrunkOrigin,
      TreeConfiguration pTreeConfig
   ) {
      List<BlockPos> $$6 = Lists.newArrayList();
      BlockPos.MutableBlockPos $$7 = pPos.mutable();

      while ($$7.getY() < pTrunkOrigin.getY()) {
         if (!this.canPlaceRoot(pLevel, $$7)) {
            return false;
         }

         $$7.move(Direction.UP);
      }

      $$6.add(pTrunkOrigin.below());

      for (Direction $$8 : Direction.Plane.HORIZONTAL) {
         BlockPos $$9 = pTrunkOrigin.relative($$8);
         List<BlockPos> $$10 = Lists.newArrayList();
         if (!this.simulateRoots(pLevel, pRandom, $$9, $$8, pTrunkOrigin, $$10, 0)) {
            return false;
         }

         $$6.addAll($$10);
         $$6.add(pTrunkOrigin.relative($$8));
      }

      for (BlockPos $$11 : $$6) {
         this.placeRoot(pLevel, pBlockSetter, pRandom, $$11, pTreeConfig);
      }

      return true;
   }

   private boolean simulateRoots(
      LevelSimulatedReader pLevel, RandomSource pRandom, BlockPos pPos, Direction pDirection, BlockPos pTrunkOrigin, List<BlockPos> pRoots, int pLength
   ) {
      int $$7 = this.mangroveRootPlacement.maxRootLength();
      if (pLength != $$7 && pRoots.size() <= $$7) {
         for (BlockPos $$9 : this.potentialRootPositions(pPos, pDirection, pRandom, pTrunkOrigin)) {
            if (this.canPlaceRoot(pLevel, $$9)) {
               pRoots.add($$9);
               if (!this.simulateRoots(pLevel, pRandom, $$9, pDirection, pTrunkOrigin, pRoots, pLength + 1)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   protected List<BlockPos> potentialRootPositions(BlockPos pPos, Direction pDirection, RandomSource pRandom, BlockPos pTrunkOrigin) {
      BlockPos $$4 = pPos.below();
      BlockPos $$5 = pPos.relative(pDirection);
      int $$6 = pPos.distManhattan(pTrunkOrigin);
      int $$7 = this.mangroveRootPlacement.maxRootWidth();
      float $$8 = this.mangroveRootPlacement.randomSkewChance();
      if ($$6 > $$7 - 3 && $$6 <= $$7) {
         return pRandom.nextFloat() < $$8 ? List.of($$4, $$5.below()) : List.of($$4);
      } else if ($$6 > $$7) {
         return List.of($$4);
      } else if (pRandom.nextFloat() < $$8) {
         return List.of($$4);
      } else {
         return pRandom.nextBoolean() ? List.of($$5) : List.of($$4);
      }
   }

   @Override
   protected boolean canPlaceRoot(LevelSimulatedReader pLevel, BlockPos pPos) {
      return super.canPlaceRoot(pLevel, pPos) || pLevel.isStateAtPosition(pPos, p_225858_ -> p_225858_.is(this.mangroveRootPlacement.canGrowThrough()));
   }

   @Override
   protected void placeRoot(
      LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, BlockPos pPos, TreeConfiguration pTreeConfig
   ) {
      if (pLevel.isStateAtPosition(pPos, p_225847_ -> p_225847_.is(this.mangroveRootPlacement.muddyRootsIn()))) {
         BlockState $$5 = this.mangroveRootPlacement.muddyRootsProvider().getState(pRandom, pPos);
         pBlockSetter.accept(pPos, this.getPotentiallyWaterloggedState(pLevel, pPos, $$5));
      } else {
         super.placeRoot(pLevel, pBlockSetter, pRandom, pPos, pTreeConfig);
      }
   }

   @Override
   protected RootPlacerType<?> type() {
      return RootPlacerType.MANGROVE_ROOT_PLACER;
   }
}
