package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
   public VegetationPatchFeature(Codec<VegetationPatchConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      VegetationPatchConfiguration $$2 = pContext.config();
      RandomSource $$3 = pContext.random();
      BlockPos $$4 = pContext.origin();
      Predicate<BlockState> $$5 = p_204782_ -> p_204782_.is($$2.replaceable);
      int $$6 = $$2.xzRadius.sample($$3) + 1;
      int $$7 = $$2.xzRadius.sample($$3) + 1;
      Set<BlockPos> $$8 = this.placeGroundPatch($$1, $$2, $$3, $$4, $$5, $$6, $$7);
      this.distributeVegetation(pContext, $$1, $$2, $$3, $$8, $$6, $$7);
      return !$$8.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, RandomSource pRandom, BlockPos pPos, Predicate<BlockState> pState, int pXRadius, int pZRadius
   ) {
      BlockPos.MutableBlockPos $$7 = pPos.mutable();
      BlockPos.MutableBlockPos $$8 = $$7.mutable();
      Direction $$9 = pConfig.surface.getDirection();
      Direction $$10 = $$9.getOpposite();
      Set<BlockPos> $$11 = new HashSet<>();

      for (int $$12 = -pXRadius; $$12 <= pXRadius; $$12++) {
         boolean $$13 = $$12 == -pXRadius || $$12 == pXRadius;

         for (int $$14 = -pZRadius; $$14 <= pZRadius; $$14++) {
            boolean $$15 = $$14 == -pZRadius || $$14 == pZRadius;
            boolean $$16 = $$13 || $$15;
            boolean $$17 = $$13 && $$15;
            boolean $$18 = $$16 && !$$17;
            if (!$$17 && (!$$18 || pConfig.extraEdgeColumnChance != 0.0F && !(pRandom.nextFloat() > pConfig.extraEdgeColumnChance))) {
               $$7.setWithOffset(pPos, $$12, 0, $$14);

               for (int $$19 = 0; pLevel.isStateAtPosition($$7, BlockBehaviour.BlockStateBase::isAir) && $$19 < pConfig.verticalRange; $$19++) {
                  $$7.move($$9);
               }

               for (int var25 = 0; pLevel.isStateAtPosition($$7, p_284926_ -> !p_284926_.isAir()) && var25 < pConfig.verticalRange; var25++) {
                  $$7.move($$10);
               }

               $$8.setWithOffset($$7, pConfig.surface.getDirection());
               BlockState $$20 = pLevel.getBlockState($$8);
               if (pLevel.isEmptyBlock($$7) && $$20.isFaceSturdy(pLevel, $$8, pConfig.surface.getDirection().getOpposite())) {
                  int $$21 = pConfig.depth.sample(pRandom)
                     + (pConfig.extraBottomBlockChance > 0.0F && pRandom.nextFloat() < pConfig.extraBottomBlockChance ? 1 : 0);
                  BlockPos $$22 = $$8.immutable();
                  boolean $$23 = this.placeGround(pLevel, pConfig, pState, pRandom, $$8, $$21);
                  if ($$23) {
                     $$11.add($$22);
                  }
               }
            }
         }
      }

      return $$11;
   }

   protected void distributeVegetation(
      FeaturePlaceContext<VegetationPatchConfiguration> pContext,
      WorldGenLevel pLevel,
      VegetationPatchConfiguration pConfig,
      RandomSource pRandom,
      Set<BlockPos> pPossiblePositions,
      int pXRadius,
      int pZRadius
   ) {
      for (BlockPos $$7 : pPossiblePositions) {
         if (pConfig.vegetationChance > 0.0F && pRandom.nextFloat() < pConfig.vegetationChance) {
            this.placeVegetation(pLevel, pConfig, pContext.chunkGenerator(), pRandom, $$7);
         }
      }
   }

   protected boolean placeVegetation(
      WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, ChunkGenerator pChunkGenerator, RandomSource pRandom, BlockPos pPos
   ) {
      return pConfig.vegetationFeature.value().place(pLevel, pChunkGenerator, pRandom, pPos.relative(pConfig.surface.getDirection().getOpposite()));
   }

   protected boolean placeGround(
      WorldGenLevel pLevel,
      VegetationPatchConfiguration pConfig,
      Predicate<BlockState> pReplaceableblocks,
      RandomSource pRandom,
      BlockPos.MutableBlockPos pMutablePos,
      int pMaxDistance
   ) {
      for (int $$6 = 0; $$6 < pMaxDistance; $$6++) {
         BlockState $$7 = pConfig.groundState.getState(pRandom, pMutablePos);
         BlockState $$8 = pLevel.getBlockState(pMutablePos);
         if (!$$7.is($$8.getBlock())) {
            if (!pReplaceableblocks.test($$8)) {
               return $$6 != 0;
            }

            pLevel.setBlock(pMutablePos, $$7, 2);
            pMutablePos.move(pConfig.surface.getDirection());
         }
      }

      return true;
   }
}
