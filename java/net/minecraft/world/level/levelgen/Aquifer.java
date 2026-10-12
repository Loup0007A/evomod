package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
   static Aquifer create(
      NoiseChunk pChunk,
      ChunkPos pChunkPos,
      NoiseRouter pNoiseRouter,
      PositionalRandomFactory pPositionalRandomFactory,
      int pMinY,
      int pHeight,
      Aquifer.FluidPicker pGlobalFluidPicker
   ) {
      return new Aquifer.NoiseBasedAquifer(pChunk, pChunkPos, pNoiseRouter, pPositionalRandomFactory, pMinY, pHeight, pGlobalFluidPicker);
   }

   static Aquifer createDisabled(final Aquifer.FluidPicker pDefaultFluid) {
      return new Aquifer() {
         @Nullable
         @Override
         public BlockState computeSubstance(DensityFunction.FunctionContext p_208172_, double p_208173_) {
            return p_208173_ > 0.0 ? null : pDefaultFluid.computeFluid(p_208172_.blockX(), p_208172_.blockY(), p_208172_.blockZ()).at(p_208172_.blockY());
         }

         @Override
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(DensityFunction.FunctionContext pContext, double pSubstance);

   boolean shouldScheduleFluidUpdate();

   public interface FluidPicker {
      Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ);
   }

   public static final class FluidStatus {
      final int fluidLevel;
      final BlockState fluidType;

      public FluidStatus(int pFluidLevel, BlockState pFluidType) {
         this.fluidLevel = pFluidLevel;
         this.fluidType = pFluidType;
      }

      public BlockState at(int pY) {
         return pY < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   public static class NoiseBasedAquifer implements Aquifer {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private final NoiseChunk noiseChunk;
      protected final DensityFunction barrierNoise;
      private final DensityFunction fluidLevelFloodednessNoise;
      private final DensityFunction fluidLevelSpreadNoise;
      protected final DensityFunction lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      protected final Aquifer.FluidStatus[] aquiferCache;
      protected final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      private final DensityFunction erosion;
      private final DensityFunction depth;
      protected boolean shouldScheduleFluidUpdate;
      protected final int minGridX;
      protected final int minGridY;
      protected final int minGridZ;
      protected final int gridSizeX;
      protected final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
         {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
      };

      NoiseBasedAquifer(
         NoiseChunk pNoiseChunk,
         ChunkPos pChunkPos,
         NoiseRouter pNoiseRouter,
         PositionalRandomFactory pPositionalRandomFactory,
         int pMinY,
         int pHeight,
         Aquifer.FluidPicker pGlobalFluidPicker
      ) {
         this.noiseChunk = pNoiseChunk;
         this.barrierNoise = pNoiseRouter.barrierNoise();
         this.fluidLevelFloodednessNoise = pNoiseRouter.fluidLevelFloodednessNoise();
         this.fluidLevelSpreadNoise = pNoiseRouter.fluidLevelSpreadNoise();
         this.lavaNoise = pNoiseRouter.lavaNoise();
         this.erosion = pNoiseRouter.erosion();
         this.depth = pNoiseRouter.depth();
         this.positionalRandomFactory = pPositionalRandomFactory;
         this.minGridX = this.gridX(pChunkPos.getMinBlockX()) - 1;
         this.globalFluidPicker = pGlobalFluidPicker;
         int $$7 = this.gridX(pChunkPos.getMaxBlockX()) + 1;
         this.gridSizeX = $$7 - this.minGridX + 1;
         this.minGridY = this.gridY(pMinY) - 1;
         int $$8 = this.gridY(pMinY + pHeight) + 1;
         int $$9 = $$8 - this.minGridY + 1;
         this.minGridZ = this.gridZ(pChunkPos.getMinBlockZ()) - 1;
         int $$10 = this.gridZ(pChunkPos.getMaxBlockZ()) + 1;
         this.gridSizeZ = $$10 - this.minGridZ + 1;
         int $$11 = this.gridSizeX * $$9 * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[$$11];
         this.aquiferLocationCache = new long[$$11];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
      }

      protected int getIndex(int pGridX, int pGridY, int pGridZ) {
         int $$3 = pGridX - this.minGridX;
         int $$4 = pGridY - this.minGridY;
         int $$5 = pGridZ - this.minGridZ;
         return ($$4 * this.gridSizeZ + $$5) * this.gridSizeX + $$3;
      }

      @Nullable
      @Override
      public BlockState computeSubstance(DensityFunction.FunctionContext pContext, double pSubstance) {
         int $$2 = pContext.blockX();
         int $$3 = pContext.blockY();
         int $$4 = pContext.blockZ();
         if (pSubstance > 0.0) {
            this.shouldScheduleFluidUpdate = false;
            return null;
         } else {
            Aquifer.FluidStatus $$5 = this.globalFluidPicker.computeFluid($$2, $$3, $$4);
            if ($$5.at($$3).is(Blocks.LAVA)) {
               this.shouldScheduleFluidUpdate = false;
               return Blocks.LAVA.defaultBlockState();
            } else {
               int $$6 = Math.floorDiv($$2 - 5, 16);
               int $$7 = Math.floorDiv($$3 + 1, 12);
               int $$8 = Math.floorDiv($$4 - 5, 16);
               int $$9 = Integer.MAX_VALUE;
               int $$10 = Integer.MAX_VALUE;
               int $$11 = Integer.MAX_VALUE;
               long $$12 = 0L;
               long $$13 = 0L;
               long $$14 = 0L;

               for (int $$15 = 0; $$15 <= 1; $$15++) {
                  for (int $$16 = -1; $$16 <= 1; $$16++) {
                     for (int $$17 = 0; $$17 <= 1; $$17++) {
                        int $$18 = $$6 + $$15;
                        int $$19 = $$7 + $$16;
                        int $$20 = $$8 + $$17;
                        int $$21 = this.getIndex($$18, $$19, $$20);
                        long $$22 = this.aquiferLocationCache[$$21];
                        long $$23;
                        if ($$22 != Long.MAX_VALUE) {
                           $$23 = $$22;
                        } else {
                           RandomSource $$24 = this.positionalRandomFactory.at($$18, $$19, $$20);
                           $$23 = BlockPos.asLong($$18 * 16 + $$24.nextInt(10), $$19 * 12 + $$24.nextInt(9), $$20 * 16 + $$24.nextInt(10));
                           this.aquiferLocationCache[$$21] = $$23;
                        }

                        int $$26 = BlockPos.getX($$23) - $$2;
                        int $$27 = BlockPos.getY($$23) - $$3;
                        int $$28 = BlockPos.getZ($$23) - $$4;
                        int $$29 = $$26 * $$26 + $$27 * $$27 + $$28 * $$28;
                        if ($$9 >= $$29) {
                           $$14 = $$13;
                           $$13 = $$12;
                           $$12 = $$23;
                           $$11 = $$10;
                           $$10 = $$9;
                           $$9 = $$29;
                        } else if ($$10 >= $$29) {
                           $$14 = $$13;
                           $$13 = $$23;
                           $$11 = $$10;
                           $$10 = $$29;
                        } else if ($$11 >= $$29) {
                           $$14 = $$23;
                           $$11 = $$29;
                        }
                     }
                  }
               }

               Aquifer.FluidStatus $$30 = this.getAquiferStatus($$12);
               double $$31 = similarity($$9, $$10);
               BlockState $$32 = $$30.at($$3);
               if ($$31 <= 0.0) {
                  this.shouldScheduleFluidUpdate = $$31 >= FLOWING_UPDATE_SIMULARITY;
                  return $$32;
               } else if ($$32.is(Blocks.WATER) && this.globalFluidPicker.computeFluid($$2, $$3 - 1, $$4).at($$3 - 1).is(Blocks.LAVA)) {
                  this.shouldScheduleFluidUpdate = true;
                  return $$32;
               } else {
                  MutableDouble $$34 = new MutableDouble(Double.NaN);
                  Aquifer.FluidStatus $$35 = this.getAquiferStatus($$13);
                  double $$36 = $$31 * this.calculatePressure(pContext, $$34, $$30, $$35);
                  if (pSubstance + $$36 > 0.0) {
                     this.shouldScheduleFluidUpdate = false;
                     return null;
                  } else {
                     Aquifer.FluidStatus $$37 = this.getAquiferStatus($$14);
                     double $$38 = similarity($$9, $$11);
                     if ($$38 > 0.0) {
                        double $$39 = $$31 * $$38 * this.calculatePressure(pContext, $$34, $$30, $$37);
                        if (pSubstance + $$39 > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     double $$40 = similarity($$10, $$11);
                     if ($$40 > 0.0) {
                        double $$41 = $$31 * $$40 * this.calculatePressure(pContext, $$34, $$35, $$37);
                        if (pSubstance + $$41 > 0.0) {
                           this.shouldScheduleFluidUpdate = false;
                           return null;
                        }
                     }

                     this.shouldScheduleFluidUpdate = true;
                     return $$32;
                  }
               }
            }
         }
      }

      @Override
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      protected static double similarity(int pFirstDistance, int pSecondDistance) {
         double $$2 = 25.0;
         return 1.0 - (double)Math.abs(pSecondDistance - pFirstDistance) / 25.0;
      }

      private double calculatePressure(
         DensityFunction.FunctionContext pContext, MutableDouble pSubstance, Aquifer.FluidStatus pFirstFluid, Aquifer.FluidStatus pSecondFluid
      ) {
         int $$4 = pContext.blockY();
         BlockState $$5 = pFirstFluid.at($$4);
         BlockState $$6 = pSecondFluid.at($$4);
         if ((!$$5.is(Blocks.LAVA) || !$$6.is(Blocks.WATER)) && (!$$5.is(Blocks.WATER) || !$$6.is(Blocks.LAVA))) {
            int $$7 = Math.abs(pFirstFluid.fluidLevel - pSecondFluid.fluidLevel);
            if ($$7 == 0) {
               return 0.0;
            } else {
               double $$8 = 0.5 * (double)(pFirstFluid.fluidLevel + pSecondFluid.fluidLevel);
               double $$9 = (double)$$4 + 0.5 - $$8;
               double $$10 = (double)$$7 / 2.0;
               double $$11 = 0.0;
               double $$12 = 2.5;
               double $$13 = 1.5;
               double $$14 = 3.0;
               double $$15 = 10.0;
               double $$16 = 3.0;
               double $$17 = $$10 - Math.abs($$9);
               double $$19;
               if ($$9 > 0.0) {
                  double $$18 = 0.0 + $$17;
                  if ($$18 > 0.0) {
                     $$19 = $$18 / 1.5;
                  } else {
                     $$19 = $$18 / 2.5;
                  }
               } else {
                  double $$21 = 3.0 + $$17;
                  if ($$21 > 0.0) {
                     $$19 = $$21 / 3.0;
                  } else {
                     $$19 = $$21 / 10.0;
                  }
               }

               double $$24 = 2.0;
               double $$28;
               if (!($$19 < -2.0) && !($$19 > 2.0)) {
                  double $$26 = pSubstance.getValue();
                  if (Double.isNaN($$26)) {
                     double $$27 = this.barrierNoise.compute(pContext);
                     pSubstance.setValue($$27);
                     $$28 = $$27;
                  } else {
                     $$28 = $$26;
                  }
               } else {
                  $$28 = 0.0;
               }

               return 2.0 * ($$28 + $$19);
            }
         } else {
            return 2.0;
         }
      }

      protected int gridX(int pX) {
         return Math.floorDiv(pX, 16);
      }

      protected int gridY(int pY) {
         return Math.floorDiv(pY, 12);
      }

      protected int gridZ(int pZ) {
         return Math.floorDiv(pZ, 16);
      }

      private Aquifer.FluidStatus getAquiferStatus(long pPackedPos) {
         int $$1 = BlockPos.getX(pPackedPos);
         int $$2 = BlockPos.getY(pPackedPos);
         int $$3 = BlockPos.getZ(pPackedPos);
         int $$4 = this.gridX($$1);
         int $$5 = this.gridY($$2);
         int $$6 = this.gridZ($$3);
         int $$7 = this.getIndex($$4, $$5, $$6);
         Aquifer.FluidStatus $$8 = this.aquiferCache[$$7];
         if ($$8 != null) {
            return $$8;
         } else {
            Aquifer.FluidStatus $$9 = this.computeFluid($$1, $$2, $$3);
            this.aquiferCache[$$7] = $$9;
            return $$9;
         }
      }

      private Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ) {
         Aquifer.FluidStatus $$3 = this.globalFluidPicker.computeFluid(pX, pY, pZ);
         int $$4 = Integer.MAX_VALUE;
         int $$5 = pY + 12;
         int $$6 = pY - 12;
         boolean $$7 = false;

         for (int[] $$8 : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int $$9 = pX + SectionPos.sectionToBlockCoord($$8[0]);
            int $$10 = pZ + SectionPos.sectionToBlockCoord($$8[1]);
            int $$11 = this.noiseChunk.preliminarySurfaceLevel($$9, $$10);
            int $$12 = $$11 + 8;
            boolean $$13 = $$8[0] == 0 && $$8[1] == 0;
            if ($$13 && $$6 > $$12) {
               return $$3;
            }

            boolean $$14 = $$5 > $$12;
            if ($$14 || $$13) {
               Aquifer.FluidStatus $$15 = this.globalFluidPicker.computeFluid($$9, $$12, $$10);
               if (!$$15.at($$12).isAir()) {
                  if ($$13) {
                     $$7 = true;
                  }

                  if ($$14) {
                     return $$15;
                  }
               }
            }

            $$4 = Math.min($$4, $$11);
         }

         int $$16 = this.computeSurfaceLevel(pX, pY, pZ, $$3, $$4, $$7);
         return new Aquifer.FluidStatus($$16, this.computeFluidType(pX, pY, pZ, $$3, $$16));
      }

      private int computeSurfaceLevel(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pMaxSurfaceLevel, boolean pFluidPresent) {
         DensityFunction.SinglePointContext $$6 = new DensityFunction.SinglePointContext(pX, pY, pZ);
         double $$7;
         double $$8;
         if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, $$6)) {
            $$7 = -1.0;
            $$8 = -1.0;
         } else {
            int $$9 = pMaxSurfaceLevel + 8 - pY;
            int $$10 = 64;
            double $$11 = pFluidPresent ? Mth.clampedMap((double)$$9, 0.0, 64.0, 1.0, 0.0) : 0.0;
            double $$12 = Mth.clamp(this.fluidLevelFloodednessNoise.compute($$6), -1.0, 1.0);
            double $$13 = Mth.map($$11, 1.0, 0.0, -0.3, 0.8);
            double $$14 = Mth.map($$11, 1.0, 0.0, -0.8, 0.4);
            $$7 = $$12 - $$14;
            $$8 = $$12 - $$13;
         }

         int $$17;
         if ($$8 > 0.0) {
            $$17 = pFluidStatus.fluidLevel;
         } else if ($$7 > 0.0) {
            $$17 = this.computeRandomizedFluidSurfaceLevel(pX, pY, pZ, pMaxSurfaceLevel);
         } else {
            $$17 = DimensionType.WAY_BELOW_MIN_Y;
         }

         return $$17;
      }

      private int computeRandomizedFluidSurfaceLevel(int pX, int pY, int pZ, int pMaxSurfaceLevel) {
         int $$4 = 16;
         int $$5 = 40;
         int $$6 = Math.floorDiv(pX, 16);
         int $$7 = Math.floorDiv(pY, 40);
         int $$8 = Math.floorDiv(pZ, 16);
         int $$9 = $$7 * 40 + 20;
         int $$10 = 10;
         double $$11 = this.fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext($$6, $$7, $$8)) * 10.0;
         int $$12 = Mth.quantize($$11, 3);
         int $$13 = $$9 + $$12;
         return Math.min(pMaxSurfaceLevel, $$13);
      }

      private BlockState computeFluidType(int pX, int pY, int pZ, Aquifer.FluidStatus pFluidStatus, int pSurfaceLevel) {
         BlockState $$5 = pFluidStatus.fluidType;
         if (pSurfaceLevel <= -10 && pSurfaceLevel != DimensionType.WAY_BELOW_MIN_Y && pFluidStatus.fluidType != Blocks.LAVA.defaultBlockState()) {
            int $$6 = 64;
            int $$7 = 40;
            int $$8 = Math.floorDiv(pX, 64);
            int $$9 = Math.floorDiv(pY, 40);
            int $$10 = Math.floorDiv(pZ, 64);
            double $$11 = this.lavaNoise.compute(new DensityFunction.SinglePointContext($$8, $$9, $$10));
            if (Math.abs($$11) > 0.3) {
               $$5 = Blocks.LAVA.defaultBlockState();
            }
         }

         return $$5;
      }
   }
}
