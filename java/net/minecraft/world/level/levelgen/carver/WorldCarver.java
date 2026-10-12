package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldCarver<C extends CarverConfiguration> {
   public static final WorldCarver<CaveCarverConfiguration> CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
   public static final WorldCarver<CanyonCarverConfiguration> CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
   protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
   protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
   protected static final FluidState WATER = Fluids.WATER.defaultFluidState();
   protected static final FluidState LAVA = Fluids.LAVA.defaultFluidState();
   protected Set<Fluid> liquids = ImmutableSet.of(Fluids.WATER);
   private final MapCodec<ConfiguredWorldCarver<C>> configuredCodec;

   private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String pKey, F pCarver) {
      return Registry.register(BuiltInRegistries.CARVER, pKey, pCarver);
   }

   public WorldCarver(Codec<C> pCodec) {
      this.configuredCodec = pCodec.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config);
   }

   public ConfiguredWorldCarver<C> configured(C p_65064_) {
      return new ConfiguredWorldCarver<>(this, p_65064_);
   }

   public MapCodec<ConfiguredWorldCarver<C>> configuredCodec() {
      return this.configuredCodec;
   }

   public int getRange() {
      return 4;
   }

   protected boolean carveEllipsoid(
      CarvingContext pContext,
      C pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      Aquifer pAquifer,
      double pX,
      double pY,
      double pZ,
      double pHorizontalRadius,
      double pVerticalRadius,
      CarvingMask pCarvingMask,
      WorldCarver.CarveSkipChecker pSkipChecker
   ) {
      ChunkPos $$12 = pChunk.getPos();
      double $$13 = (double)$$12.getMiddleBlockX();
      double $$14 = (double)$$12.getMiddleBlockZ();
      double $$15 = 16.0 + pHorizontalRadius * 2.0;
      if (!(Math.abs(pX - $$13) > $$15) && !(Math.abs(pZ - $$14) > $$15)) {
         int $$16 = $$12.getMinBlockX();
         int $$17 = $$12.getMinBlockZ();
         int $$18 = Math.max(Mth.floor(pX - pHorizontalRadius) - $$16 - 1, 0);
         int $$19 = Math.min(Mth.floor(pX + pHorizontalRadius) - $$16, 15);
         int $$20 = Math.max(Mth.floor(pY - pVerticalRadius) - 1, pContext.getMinGenY() + 1);
         int $$21 = pChunk.isUpgrading() ? 0 : 7;
         int $$22 = Math.min(Mth.floor(pY + pVerticalRadius) + 1, pContext.getMinGenY() + pContext.getGenDepth() - 1 - $$21);
         int $$23 = Math.max(Mth.floor(pZ - pHorizontalRadius) - $$17 - 1, 0);
         int $$24 = Math.min(Mth.floor(pZ + pHorizontalRadius) - $$17, 15);
         boolean $$25 = false;
         BlockPos.MutableBlockPos $$26 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos $$27 = new BlockPos.MutableBlockPos();

         for (int $$28 = $$18; $$28 <= $$19; $$28++) {
            int $$29 = $$12.getBlockX($$28);
            double $$30 = ((double)$$29 + 0.5 - pX) / pHorizontalRadius;

            for (int $$31 = $$23; $$31 <= $$24; $$31++) {
               int $$32 = $$12.getBlockZ($$31);
               double $$33 = ((double)$$32 + 0.5 - pZ) / pHorizontalRadius;
               if (!($$30 * $$30 + $$33 * $$33 >= 1.0)) {
                  MutableBoolean $$34 = new MutableBoolean(false);

                  for (int $$35 = $$22; $$35 > $$20; $$35--) {
                     double $$36 = ((double)$$35 - 0.5 - pY) / pVerticalRadius;
                     if (!pSkipChecker.shouldSkip(pContext, $$30, $$36, $$33, $$35) && (!pCarvingMask.get($$28, $$35, $$31) || isDebugEnabled(pConfig))) {
                        pCarvingMask.set($$28, $$35, $$31);
                        $$26.set($$29, $$35, $$32);
                        $$25 |= this.carveBlock(pContext, pConfig, pChunk, pBiomeAccessor, pCarvingMask, $$26, $$27, pAquifer, $$34);
                     }
                  }
               }
            }
         }

         return $$25;
      } else {
         return false;
      }
   }

   protected boolean carveBlock(
      CarvingContext pContext,
      C pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeGetter,
      CarvingMask pCarvingMask,
      BlockPos.MutableBlockPos pPos,
      BlockPos.MutableBlockPos pCheckPos,
      Aquifer pAquifer,
      MutableBoolean pReachedSurface
   ) {
      BlockState $$9 = pChunk.getBlockState(pPos);
      if ($$9.is(Blocks.GRASS_BLOCK) || $$9.is(Blocks.MYCELIUM)) {
         pReachedSurface.setTrue();
      }

      if (!this.canReplaceBlock(pConfig, $$9) && !isDebugEnabled(pConfig)) {
         return false;
      } else {
         BlockState $$10 = this.getCarveState(pContext, pConfig, pPos, pAquifer);
         if ($$10 == null) {
            return false;
         } else {
            pChunk.setBlockState(pPos, $$10, false);
            if (pAquifer.shouldScheduleFluidUpdate() && !$$10.getFluidState().isEmpty()) {
               pChunk.markPosForPostprocessing(pPos);
            }

            if (pReachedSurface.isTrue()) {
               pCheckPos.setWithOffset(pPos, Direction.DOWN);
               if (pChunk.getBlockState(pCheckPos).is(Blocks.DIRT)) {
                  pContext.topMaterial(pBiomeGetter, pChunk, pCheckPos, !$$10.getFluidState().isEmpty()).ifPresent(p_284918_ -> {
                     pChunk.setBlockState(pCheckPos, p_284918_, false);
                     if (!p_284918_.getFluidState().isEmpty()) {
                        pChunk.markPosForPostprocessing(pCheckPos);
                     }
                  });
               }
            }

            return true;
         }
      }
   }

   @Nullable
   private BlockState getCarveState(CarvingContext pContext, C pConfig, BlockPos pPos, Aquifer pAquifer) {
      if (pPos.getY() <= pConfig.lavaLevel.resolveY(pContext)) {
         return LAVA.createLegacyBlock();
      } else {
         BlockState $$4 = pAquifer.computeSubstance(new DensityFunction.SinglePointContext(pPos.getX(), pPos.getY(), pPos.getZ()), 0.0);
         if ($$4 == null) {
            return isDebugEnabled(pConfig) ? pConfig.debugSettings.getBarrierState() : null;
         } else {
            return isDebugEnabled(pConfig) ? getDebugState(pConfig, $$4) : $$4;
         }
      }
   }

   private static BlockState getDebugState(CarverConfiguration pConfig, BlockState pState) {
      if (pState.is(Blocks.AIR)) {
         return pConfig.debugSettings.getAirState();
      } else if (pState.is(Blocks.WATER)) {
         BlockState $$2 = pConfig.debugSettings.getWaterState();
         return $$2.hasProperty(BlockStateProperties.WATERLOGGED) ? $$2.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)) : $$2;
      } else {
         return pState.is(Blocks.LAVA) ? pConfig.debugSettings.getLavaState() : pState;
      }
   }

   public abstract boolean carve(
      CarvingContext pContext,
      C pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      RandomSource pRandom,
      Aquifer pAquifer,
      ChunkPos pChunkPos,
      CarvingMask pCarvingMask
   );

   public abstract boolean isStartChunk(C pConfig, RandomSource pRandom);

   protected boolean canReplaceBlock(C pConfig, BlockState pState) {
      return pState.is(pConfig.replaceable);
   }

   protected static boolean canReach(ChunkPos pChunkPos, double pX, double pZ, int pBranchIndex, int pBranchCount, float pWidth) {
      double $$6 = (double)pChunkPos.getMiddleBlockX();
      double $$7 = (double)pChunkPos.getMiddleBlockZ();
      double $$8 = pX - $$6;
      double $$9 = pZ - $$7;
      double $$10 = (double)(pBranchCount - pBranchIndex);
      double $$11 = (double)(pWidth + 2.0F + 16.0F);
      return $$8 * $$8 + $$9 * $$9 - $$10 * $$10 <= $$11 * $$11;
   }

   private static boolean isDebugEnabled(CarverConfiguration pConfig) {
      return pConfig.debugSettings.isDebugMode();
   }

   public interface CarveSkipChecker {
      boolean shouldSkip(CarvingContext pContext, double pRelativeX, double pRelativeY, double pRelativeZ, int pY);
   }
}
