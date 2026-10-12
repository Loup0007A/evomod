package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;

public abstract class BiomeSource implements BiomeResolver {
   public static final Codec<BiomeSource> CODEC = BuiltInRegistries.BIOME_SOURCE.byNameCodec().dispatchStable(BiomeSource::codec, Function.identity());
   private final Supplier<Set<Holder<Biome>>> possibleBiomes = Suppliers.memoize(
      () -> this.collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet())
   );

   protected BiomeSource() {
   }

   protected abstract MapCodec<? extends BiomeSource> codec();

   protected abstract Stream<Holder<Biome>> collectPossibleBiomes();

   public Set<Holder<Biome>> possibleBiomes() {
      return this.possibleBiomes.get();
   }

   public Set<Holder<Biome>> getBiomesWithin(int pX, int pY, int pZ, int pRadius, Climate.Sampler pSampler) {
      int $$5 = QuartPos.fromBlock(pX - pRadius);
      int $$6 = QuartPos.fromBlock(pY - pRadius);
      int $$7 = QuartPos.fromBlock(pZ - pRadius);
      int $$8 = QuartPos.fromBlock(pX + pRadius);
      int $$9 = QuartPos.fromBlock(pY + pRadius);
      int $$10 = QuartPos.fromBlock(pZ + pRadius);
      int $$11 = $$8 - $$5 + 1;
      int $$12 = $$9 - $$6 + 1;
      int $$13 = $$10 - $$7 + 1;
      Set<Holder<Biome>> $$14 = Sets.newHashSet();

      for (int $$15 = 0; $$15 < $$13; $$15++) {
         for (int $$16 = 0; $$16 < $$11; $$16++) {
            for (int $$17 = 0; $$17 < $$12; $$17++) {
               int $$18 = $$5 + $$16;
               int $$19 = $$6 + $$17;
               int $$20 = $$7 + $$15;
               $$14.add(this.getNoiseBiome($$18, $$19, $$20, pSampler));
            }
         }
      }

      return $$14;
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int pX, int pY, int pZ, int pRadius, Predicate<Holder<Biome>> pBiomePredicate, RandomSource pRandom, Climate.Sampler pSampler
   ) {
      return this.findBiomeHorizontal(pX, pY, pZ, pRadius, 1, pBiomePredicate, pRandom, false, pSampler);
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
      BlockPos pPos,
      int pRadius,
      int pHorizontalStep,
      int pVerticalStep,
      Predicate<Holder<Biome>> pBiomePredicate,
      Climate.Sampler pSampler,
      LevelReader pLevel
   ) {
      Set<Holder<Biome>> $$7 = this.possibleBiomes().stream().filter(pBiomePredicate).collect(Collectors.toUnmodifiableSet());
      if ($$7.isEmpty()) {
         return null;
      } else {
         int $$8 = Math.floorDiv(pRadius, pHorizontalStep);
         int[] $$9 = Mth.outFromOrigin(pPos.getY(), pLevel.getMinBuildHeight() + 1, pLevel.getMaxBuildHeight(), pVerticalStep).toArray();

         for (BlockPos.MutableBlockPos $$10 : BlockPos.spiralAround(BlockPos.ZERO, $$8, Direction.EAST, Direction.SOUTH)) {
            int $$11 = pPos.getX() + $$10.getX() * pHorizontalStep;
            int $$12 = pPos.getZ() + $$10.getZ() * pHorizontalStep;
            int $$13 = QuartPos.fromBlock($$11);
            int $$14 = QuartPos.fromBlock($$12);

            for (int $$15 : $$9) {
               int $$16 = QuartPos.fromBlock($$15);
               Holder<Biome> $$17 = this.getNoiseBiome($$13, $$16, $$14, pSampler);
               if ($$7.contains($$17)) {
                  return Pair.of(new BlockPos($$11, $$15, $$12), $$17);
               }
            }
         }

         return null;
      }
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(
      int pX,
      int pY,
      int pZ,
      int pRadius,
      int pIncrement,
      Predicate<Holder<Biome>> pBiomePredicate,
      RandomSource pRandom,
      boolean pFindClosest,
      Climate.Sampler pSampler
   ) {
      int $$9 = QuartPos.fromBlock(pX);
      int $$10 = QuartPos.fromBlock(pZ);
      int $$11 = QuartPos.fromBlock(pRadius);
      int $$12 = QuartPos.fromBlock(pY);
      Pair<BlockPos, Holder<Biome>> $$13 = null;
      int $$14 = 0;
      int $$15 = pFindClosest ? 0 : $$11;
      int $$16 = $$15;

      while ($$16 <= $$11) {
         for (int $$17 = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -$$16; $$17 <= $$16; $$17 += pIncrement) {
            boolean $$18 = Math.abs($$17) == $$16;

            for (int $$19 = -$$16; $$19 <= $$16; $$19 += pIncrement) {
               if (pFindClosest) {
                  boolean $$20 = Math.abs($$19) == $$16;
                  if (!$$20 && !$$18) {
                     continue;
                  }
               }

               int $$21 = $$9 + $$19;
               int $$22 = $$10 + $$17;
               Holder<Biome> $$23 = this.getNoiseBiome($$21, $$12, $$22, pSampler);
               if (pBiomePredicate.test($$23)) {
                  if ($$13 == null || pRandom.nextInt($$14 + 1) == 0) {
                     BlockPos $$24 = new BlockPos(QuartPos.toBlock($$21), pY, QuartPos.toBlock($$22));
                     if (pFindClosest) {
                        return Pair.of($$24, $$23);
                     }

                     $$13 = Pair.of($$24, $$23);
                  }

                  $$14++;
               }
            }
         }

         $$16 += pIncrement;
      }

      return $$13;
   }

   @Override
   public abstract Holder<Biome> getNoiseBiome(int pX, int pY, int pZ, Climate.Sampler pSampler);

   public void addDebugInfo(List<String> pInfo, BlockPos pPos, Climate.Sampler pSampler) {
   }
}
