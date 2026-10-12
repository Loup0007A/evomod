package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> pCodec) {
      super(pCodec);
   }

   private static boolean isVine(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, p_225299_ -> p_225299_.is(Blocks.VINE));
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, p_284924_ -> p_284924_.isAir() || p_284924_.is(BlockTags.LEAVES));
   }

   private static void setBlockKnownShape(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      pLevel.setBlock(pPos, pState, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, p_284925_ -> p_284925_.isAir() || p_284925_.is(BlockTags.REPLACEABLE_BY_TREES));
   }

   private boolean doPlace(
      WorldGenLevel pLevel,
      RandomSource pRandom,
      BlockPos pPos,
      BiConsumer<BlockPos, BlockState> pRootBlockSetter,
      BiConsumer<BlockPos, BlockState> pTrunkBlockSetter,
      FoliagePlacer.FoliageSetter pFoliageBlockSetter,
      TreeConfiguration pConfig
   ) {
      int $$7 = pConfig.trunkPlacer.getTreeHeight(pRandom);
      int $$8 = pConfig.foliagePlacer.foliageHeight(pRandom, $$7, pConfig);
      int $$9 = $$7 - $$8;
      int $$10 = pConfig.foliagePlacer.foliageRadius(pRandom, $$9);
      BlockPos $$11 = pConfig.rootPlacer.<BlockPos>map(p_225286_ -> p_225286_.getTrunkOrigin(pPos, pRandom)).orElse(pPos);
      int $$12 = Math.min(pPos.getY(), $$11.getY());
      int $$13 = Math.max(pPos.getY(), $$11.getY()) + $$7 + 1;
      if ($$12 >= pLevel.getMinBuildHeight() + 1 && $$13 <= pLevel.getMaxBuildHeight()) {
         OptionalInt $$14 = pConfig.minimumSize.minClippedHeight();
         int $$15 = this.getMaxFreeTreeHeight(pLevel, $$7, $$11, pConfig);
         if ($$15 >= $$7 || !$$14.isEmpty() && $$15 >= $$14.getAsInt()) {
            if (pConfig.rootPlacer.isPresent() && !pConfig.rootPlacer.get().placeRoots(pLevel, pRootBlockSetter, pRandom, pPos, $$11, pConfig)) {
               return false;
            } else {
               List<FoliagePlacer.FoliageAttachment> $$16 = pConfig.trunkPlacer.placeTrunk(pLevel, pTrunkBlockSetter, pRandom, $$15, $$11, pConfig);
               $$16.forEach(p_272582_ -> pConfig.foliagePlacer.createFoliage(pLevel, pFoliageBlockSetter, pRandom, pConfig, $$15, p_272582_, $$8, $$10));
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader pLevel, int pTrunkHeight, BlockPos pTopPosition, TreeConfiguration pConfig) {
      BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

      for (int $$5 = 0; $$5 <= pTrunkHeight + 1; $$5++) {
         int $$6 = pConfig.minimumSize.getSizeAtHeight(pTrunkHeight, $$5);

         for (int $$7 = -$$6; $$7 <= $$6; $$7++) {
            for (int $$8 = -$$6; $$8 <= $$6; $$8++) {
               $$4.setWithOffset(pTopPosition, $$7, $$5, $$8);
               if (!pConfig.trunkPlacer.isFree(pLevel, $$4) || !pConfig.ignoreVines && isVine(pLevel, $$4)) {
                  return $$5 - 2;
               }
            }
         }
      }

      return pTrunkHeight;
   }

   @Override
   protected void setBlock(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      setBlockKnownShape(pLevel, pPos, pState);
   }

   @Override
   public final boolean place(FeaturePlaceContext<TreeConfiguration> pContext) {
      final WorldGenLevel $$1 = pContext.level();
      RandomSource $$2 = pContext.random();
      BlockPos $$3 = pContext.origin();
      TreeConfiguration $$4 = pContext.config();
      Set<BlockPos> $$5 = Sets.newHashSet();
      Set<BlockPos> $$6 = Sets.newHashSet();
      final Set<BlockPos> $$7 = Sets.newHashSet();
      Set<BlockPos> $$8 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> $$9 = (p_160555_, p_160556_) -> {
         $$5.add(p_160555_.immutable());
         $$1.setBlock(p_160555_, p_160556_, 19);
      };
      BiConsumer<BlockPos, BlockState> $$10 = (p_160548_, p_160549_) -> {
         $$6.add(p_160548_.immutable());
         $$1.setBlock(p_160548_, p_160549_, 19);
      };
      FoliagePlacer.FoliageSetter $$11 = new FoliagePlacer.FoliageSetter() {
         @Override
         public void set(BlockPos p_272825_, BlockState p_273311_) {
            $$7.add(p_272825_.immutable());
            $$1.setBlock(p_272825_, p_273311_, 19);
         }

         @Override
         public boolean isSet(BlockPos p_272999_) {
            return $$7.contains(p_272999_);
         }
      };
      BiConsumer<BlockPos, BlockState> $$12 = (p_160543_, p_160544_) -> {
         $$8.add(p_160543_.immutable());
         $$1.setBlock(p_160543_, p_160544_, 19);
      };
      boolean $$13 = this.doPlace($$1, $$2, $$3, $$9, $$10, $$11, $$4);
      if ($$13 && (!$$6.isEmpty() || !$$7.isEmpty())) {
         if (!$$4.decorators.isEmpty()) {
            TreeDecorator.Context $$14 = new TreeDecorator.Context($$1, $$12, $$2, $$6, $$7, $$5);
            $$4.decorators.forEach(p_225282_ -> p_225282_.place($$14));
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat($$5, $$6, $$7, $$8)).map(p_225270_ -> {
            DiscreteVoxelShape $$5x = updateLeaves($$1, p_225270_, $$6, $$8, $$5);
            StructureTemplate.updateShapeAtEdge($$1, 3, $$5x, p_225270_.minX(), p_225270_.minY(), p_225270_.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(
      LevelAccessor pLevel, BoundingBox pBox, Set<BlockPos> pRootPositions, Set<BlockPos> pTrunkPositions, Set<BlockPos> pFoliagePositions
   ) {
      DiscreteVoxelShape $$5 = new BitSetDiscreteVoxelShape(pBox.getXSpan(), pBox.getYSpan(), pBox.getZSpan());
      int $$6 = 7;
      List<Set<BlockPos>> $$7 = Lists.newArrayList();

      for (int $$8 = 0; $$8 < 7; $$8++) {
         $$7.add(Sets.newHashSet());
      }

      for (BlockPos $$9 : Lists.newArrayList(Sets.union(pTrunkPositions, pFoliagePositions))) {
         if (pBox.isInside($$9)) {
            $$5.fill($$9.getX() - pBox.minX(), $$9.getY() - pBox.minY(), $$9.getZ() - pBox.minZ());
         }
      }

      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();
      int $$11 = 0;
      $$7.get(0).addAll(pRootPositions);

      while (true) {
         while ($$11 >= 7 || !$$7.get($$11).isEmpty()) {
            if ($$11 >= 7) {
               return $$5;
            }

            Iterator<BlockPos> $$12 = $$7.get($$11).iterator();
            BlockPos $$13 = $$12.next();
            $$12.remove();
            if (pBox.isInside($$13)) {
               if ($$11 != 0) {
                  BlockState $$14 = pLevel.getBlockState($$13);
                  setBlockKnownShape(pLevel, $$13, $$14.setValue(BlockStateProperties.DISTANCE, Integer.valueOf($$11)));
               }

               $$5.fill($$13.getX() - pBox.minX(), $$13.getY() - pBox.minY(), $$13.getZ() - pBox.minZ());

               for (Direction $$15 : Direction.values()) {
                  $$10.setWithOffset($$13, $$15);
                  if (pBox.isInside($$10)) {
                     int $$16 = $$10.getX() - pBox.minX();
                     int $$17 = $$10.getY() - pBox.minY();
                     int $$18 = $$10.getZ() - pBox.minZ();
                     if (!$$5.isFull($$16, $$17, $$18)) {
                        BlockState $$19 = pLevel.getBlockState($$10);
                        OptionalInt $$20 = LeavesBlock.getOptionalDistanceAt($$19);
                        if (!$$20.isEmpty()) {
                           int $$21 = Math.min($$20.getAsInt(), $$11 + 1);
                           if ($$21 < 7) {
                              $$7.get($$21).add($$10.immutable());
                              $$11 = Math.min($$11, $$21);
                           }
                        }
                     }
                  }
               }
            }
         }

         $$11++;
      }
   }
}
