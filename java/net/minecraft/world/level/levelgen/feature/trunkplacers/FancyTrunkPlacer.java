package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public class FancyTrunkPlacer extends TrunkPlacer {
   public static final MapCodec<FancyTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
      p_70136_ -> trunkPlacerParts(p_70136_).apply(p_70136_, FancyTrunkPlacer::new)
   );
   private static final double TRUNK_HEIGHT_SCALE = 0.618;
   private static final double CLUSTER_DENSITY_MAGIC = 1.382;
   private static final double BRANCH_SLOPE = 0.381;
   private static final double BRANCH_LENGTH_MAGIC = 0.328;

   public FancyTrunkPlacer(int p_70094_, int p_70095_, int p_70096_) {
      super(p_70094_, p_70095_, p_70096_);
   }

   @Override
   protected TrunkPlacerType<?> type() {
      return TrunkPlacerType.FANCY_TRUNK_PLACER;
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
      int $$6 = 5;
      int $$7 = pFreeTreeHeight + 2;
      int $$8 = Mth.floor((double)$$7 * 0.618);
      setDirtAt(pLevel, pBlockSetter, pRandom, pPos.below(), pConfig);
      double $$9 = 1.0;
      int $$10 = Math.min(1, Mth.floor(1.382 + Math.pow(1.0 * (double)$$7 / 13.0, 2.0)));
      int $$11 = pPos.getY() + $$8;
      int $$12 = $$7 - 5;
      List<FancyTrunkPlacer.FoliageCoords> $$13 = Lists.newArrayList();
      $$13.add(new FancyTrunkPlacer.FoliageCoords(pPos.above($$12), $$11));

      for (; $$12 >= 0; $$12--) {
         float $$14 = treeShape($$7, $$12);
         if (!($$14 < 0.0F)) {
            for (int $$15 = 0; $$15 < $$10; $$15++) {
               double $$16 = 1.0;
               double $$17 = 1.0 * (double)$$14 * ((double)pRandom.nextFloat() + 0.328);
               double $$18 = (double)(pRandom.nextFloat() * 2.0F) * Math.PI;
               double $$19 = $$17 * Math.sin($$18) + 0.5;
               double $$20 = $$17 * Math.cos($$18) + 0.5;
               BlockPos $$21 = pPos.offset(Mth.floor($$19), $$12 - 1, Mth.floor($$20));
               BlockPos $$22 = $$21.above(5);
               if (this.makeLimb(pLevel, pBlockSetter, pRandom, $$21, $$22, false, pConfig)) {
                  int $$23 = pPos.getX() - $$21.getX();
                  int $$24 = pPos.getZ() - $$21.getZ();
                  double $$25 = (double)$$21.getY() - Math.sqrt((double)($$23 * $$23 + $$24 * $$24)) * 0.381;
                  int $$26 = $$25 > (double)$$11 ? $$11 : (int)$$25;
                  BlockPos $$27 = new BlockPos(pPos.getX(), $$26, pPos.getZ());
                  if (this.makeLimb(pLevel, pBlockSetter, pRandom, $$27, $$21, false, pConfig)) {
                     $$13.add(new FancyTrunkPlacer.FoliageCoords($$21, $$27.getY()));
                  }
               }
            }
         }
      }

      this.makeLimb(pLevel, pBlockSetter, pRandom, pPos, pPos.above($$8), true, pConfig);
      this.makeBranches(pLevel, pBlockSetter, pRandom, $$7, pPos, $$13, pConfig);
      List<FoliagePlacer.FoliageAttachment> $$28 = Lists.newArrayList();

      for (FancyTrunkPlacer.FoliageCoords $$29 : $$13) {
         if (this.trimBranches($$7, $$29.getBranchBase() - pPos.getY())) {
            $$28.add($$29.attachment);
         }
      }

      return $$28;
   }

   private boolean makeLimb(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      BlockPos pBasePos,
      BlockPos pOffsetPos,
      boolean pModifyWorld,
      TreeConfiguration pConfig
   ) {
      if (!pModifyWorld && Objects.equals(pBasePos, pOffsetPos)) {
         return true;
      } else {
         BlockPos $$7 = pOffsetPos.offset(-pBasePos.getX(), -pBasePos.getY(), -pBasePos.getZ());
         int $$8 = this.getSteps($$7);
         float $$9 = (float)$$7.getX() / (float)$$8;
         float $$10 = (float)$$7.getY() / (float)$$8;
         float $$11 = (float)$$7.getZ() / (float)$$8;

         for (int $$12 = 0; $$12 <= $$8; $$12++) {
            BlockPos $$13 = pBasePos.offset(Mth.floor(0.5F + (float)$$12 * $$9), Mth.floor(0.5F + (float)$$12 * $$10), Mth.floor(0.5F + (float)$$12 * $$11));
            if (pModifyWorld) {
               this.placeLog(
                  pLevel, pBlockSetter, pRandom, $$13, pConfig, p_161826_ -> p_161826_.trySetValue(RotatedPillarBlock.AXIS, this.getLogAxis(pBasePos, $$13))
               );
            } else if (!this.isFree(pLevel, $$13)) {
               return false;
            }
         }

         return true;
      }
   }

   private int getSteps(BlockPos pPos) {
      int $$1 = Mth.abs(pPos.getX());
      int $$2 = Mth.abs(pPos.getY());
      int $$3 = Mth.abs(pPos.getZ());
      return Math.max($$1, Math.max($$2, $$3));
   }

   private Direction.Axis getLogAxis(BlockPos pPos, BlockPos pOtherPos) {
      Direction.Axis $$2 = Direction.Axis.Y;
      int $$3 = Math.abs(pOtherPos.getX() - pPos.getX());
      int $$4 = Math.abs(pOtherPos.getZ() - pPos.getZ());
      int $$5 = Math.max($$3, $$4);
      if ($$5 > 0) {
         if ($$3 == $$5) {
            $$2 = Direction.Axis.X;
         } else {
            $$2 = Direction.Axis.Z;
         }
      }

      return $$2;
   }

   private boolean trimBranches(int pMaxHeight, int pCurrentHeight) {
      return (double)pCurrentHeight >= (double)pMaxHeight * 0.2;
   }

   private void makeBranches(
      LevelSimulatedReader pLevel,
      BiConsumer<BlockPos, BlockState> pBlockSetter,
      RandomSource pRandom,
      int pMaxHeight,
      BlockPos pPos,
      List<FancyTrunkPlacer.FoliageCoords> pFoliageCoords,
      TreeConfiguration pConfig
   ) {
      for (FancyTrunkPlacer.FoliageCoords $$7 : pFoliageCoords) {
         int $$8 = $$7.getBranchBase();
         BlockPos $$9 = new BlockPos(pPos.getX(), $$8, pPos.getZ());
         if (!$$9.equals($$7.attachment.pos()) && this.trimBranches(pMaxHeight, $$8 - pPos.getY())) {
            this.makeLimb(pLevel, pBlockSetter, pRandom, $$9, $$7.attachment.pos(), true, pConfig);
         }
      }
   }

   private static float treeShape(int pHeight, int pCurrentY) {
      if ((float)pCurrentY < (float)pHeight * 0.3F) {
         return -1.0F;
      } else {
         float $$2 = (float)pHeight / 2.0F;
         float $$3 = $$2 - (float)pCurrentY;
         float $$4 = Mth.sqrt($$2 * $$2 - $$3 * $$3);
         if ($$3 == 0.0F) {
            $$4 = $$2;
         } else if (Math.abs($$3) >= $$2) {
            return 0.0F;
         }

         return $$4 * 0.5F;
      }
   }

   static class FoliageCoords {
      final FoliagePlacer.FoliageAttachment attachment;
      private final int branchBase;

      public FoliageCoords(BlockPos pAttachmentPos, int pBranchBase) {
         this.attachment = new FoliagePlacer.FoliageAttachment(pAttachmentPos, 0, false);
         this.branchBase = pBranchBase;
      }

      public int getBranchBase() {
         return this.branchBase;
      }
   }
}
