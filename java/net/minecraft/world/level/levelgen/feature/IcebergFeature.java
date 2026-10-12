package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

public class IcebergFeature extends Feature<BlockStateConfiguration> {
   public IcebergFeature(Codec<BlockStateConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockStateConfiguration> pContext) {
      BlockPos $$1 = pContext.origin();
      WorldGenLevel $$2 = pContext.level();
      $$1 = new BlockPos($$1.getX(), pContext.chunkGenerator().getSeaLevel(), $$1.getZ());
      RandomSource $$3 = pContext.random();
      boolean $$4 = $$3.nextDouble() > 0.7;
      BlockState $$5 = pContext.config().state;
      double $$6 = $$3.nextDouble() * 2.0 * Math.PI;
      int $$7 = 11 - $$3.nextInt(5);
      int $$8 = 3 + $$3.nextInt(3);
      boolean $$9 = $$3.nextDouble() > 0.7;
      int $$10 = 11;
      int $$11 = $$9 ? $$3.nextInt(6) + 6 : $$3.nextInt(15) + 3;
      if (!$$9 && $$3.nextDouble() > 0.9) {
         $$11 += $$3.nextInt(19) + 7;
      }

      int $$12 = Math.min($$11 + $$3.nextInt(11), 18);
      int $$13 = Math.min($$11 + $$3.nextInt(7) - $$3.nextInt(5), 11);
      int $$14 = $$9 ? $$7 : 11;

      for (int $$15 = -$$14; $$15 < $$14; $$15++) {
         for (int $$16 = -$$14; $$16 < $$14; $$16++) {
            for (int $$17 = 0; $$17 < $$11; $$17++) {
               int $$18 = $$9 ? this.heightDependentRadiusEllipse($$17, $$11, $$13) : this.heightDependentRadiusRound($$3, $$17, $$11, $$13);
               if ($$9 || $$15 < $$18) {
                  this.generateIcebergBlock($$2, $$3, $$1, $$11, $$15, $$17, $$16, $$18, $$14, $$9, $$8, $$6, $$4, $$5);
               }
            }
         }
      }

      this.smooth($$2, $$1, $$13, $$11, $$9, $$7);

      for (int $$19 = -$$14; $$19 < $$14; $$19++) {
         for (int $$20 = -$$14; $$20 < $$14; $$20++) {
            for (int $$21 = -1; $$21 > -$$12; $$21--) {
               int $$22 = $$9 ? Mth.ceil((float)$$14 * (1.0F - (float)Math.pow((double)$$21, 2.0) / ((float)$$12 * 8.0F))) : $$14;
               int $$23 = this.heightDependentRadiusSteep($$3, -$$21, $$12, $$13);
               if ($$19 < $$23) {
                  this.generateIcebergBlock($$2, $$3, $$1, $$12, $$19, $$21, $$20, $$23, $$22, $$9, $$8, $$6, $$4, $$5);
               }
            }
         }
      }

      boolean $$24 = $$9 ? $$3.nextDouble() > 0.1 : $$3.nextDouble() > 0.7;
      if ($$24) {
         this.generateCutOut($$3, $$2, $$13, $$11, $$1, $$9, $$7, $$6, $$8);
      }

      return true;
   }

   private void generateCutOut(
      RandomSource pRandom,
      LevelAccessor pLevel,
      int pMajorAxis,
      int pHeight,
      BlockPos pPos,
      boolean pElliptical,
      int pEllipseRadius,
      double pAngle,
      int pMinorAxis
   ) {
      int $$9 = pRandom.nextBoolean() ? -1 : 1;
      int $$10 = pRandom.nextBoolean() ? -1 : 1;
      int $$11 = pRandom.nextInt(Math.max(pMajorAxis / 2 - 2, 1));
      if (pRandom.nextBoolean()) {
         $$11 = pMajorAxis / 2 + 1 - pRandom.nextInt(Math.max(pMajorAxis - pMajorAxis / 2 - 1, 1));
      }

      int $$12 = pRandom.nextInt(Math.max(pMajorAxis / 2 - 2, 1));
      if (pRandom.nextBoolean()) {
         $$12 = pMajorAxis / 2 + 1 - pRandom.nextInt(Math.max(pMajorAxis - pMajorAxis / 2 - 1, 1));
      }

      if (pElliptical) {
         $$11 = $$12 = pRandom.nextInt(Math.max(pEllipseRadius - 5, 1));
      }

      BlockPos $$13 = new BlockPos($$9 * $$11, 0, $$10 * $$12);
      double $$14 = pElliptical ? pAngle + (Math.PI / 2) : pRandom.nextDouble() * 2.0 * Math.PI;

      for (int $$15 = 0; $$15 < pHeight - 3; $$15++) {
         int $$16 = this.heightDependentRadiusRound(pRandom, $$15, pHeight, pMajorAxis);
         this.carve($$16, $$15, pPos, pLevel, false, $$14, $$13, pEllipseRadius, pMinorAxis);
      }

      for (int $$17 = -1; $$17 > -pHeight + pRandom.nextInt(5); $$17--) {
         int $$18 = this.heightDependentRadiusSteep(pRandom, -$$17, pHeight, pMajorAxis);
         this.carve($$18, $$17, pPos, pLevel, true, $$14, $$13, pEllipseRadius, pMinorAxis);
      }
   }

   private void carve(
      int pRadius,
      int pLocalY,
      BlockPos pPos,
      LevelAccessor pLevel,
      boolean pPlaceWater,
      double pPerpendicularAngle,
      BlockPos pEllipseOrigin,
      int pMajorRadius,
      int pMinorRadius
   ) {
      int $$9 = pRadius + 1 + pMajorRadius / 3;
      int $$10 = Math.min(pRadius - 3, 3) + pMinorRadius / 2 - 1;

      for (int $$11 = -$$9; $$11 < $$9; $$11++) {
         for (int $$12 = -$$9; $$12 < $$9; $$12++) {
            double $$13 = this.signedDistanceEllipse($$11, $$12, pEllipseOrigin, $$9, $$10, pPerpendicularAngle);
            if ($$13 < 0.0) {
               BlockPos $$14 = pPos.offset($$11, pLocalY, $$12);
               BlockState $$15 = pLevel.getBlockState($$14);
               if (isIcebergState($$15) || $$15.is(Blocks.SNOW_BLOCK)) {
                  if (pPlaceWater) {
                     this.setBlock(pLevel, $$14, Blocks.WATER.defaultBlockState());
                  } else {
                     this.setBlock(pLevel, $$14, Blocks.AIR.defaultBlockState());
                     this.removeFloatingSnowLayer(pLevel, $$14);
                  }
               }
            }
         }
      }
   }

   private void removeFloatingSnowLayer(LevelAccessor pLevel, BlockPos pPos) {
      if (pLevel.getBlockState(pPos.above()).is(Blocks.SNOW)) {
         this.setBlock(pLevel, pPos.above(), Blocks.AIR.defaultBlockState());
      }
   }

   private void generateIcebergBlock(
      LevelAccessor pLevel,
      RandomSource pRandom,
      BlockPos pPos,
      int pHeight,
      int pLocalX,
      int pLocalY,
      int pLocalZ,
      int pRadius,
      int pMajorRadius,
      boolean pElliptical,
      int pMinorRadius,
      double pAngle,
      boolean pPlaceSnow,
      BlockState pState
   ) {
      double $$14 = pElliptical
         ? this.signedDistanceEllipse(pLocalX, pLocalZ, BlockPos.ZERO, pMajorRadius, this.getEllipseC(pLocalY, pHeight, pMinorRadius), pAngle)
         : this.signedDistanceCircle(pLocalX, pLocalZ, BlockPos.ZERO, pRadius, pRandom);
      if ($$14 < 0.0) {
         BlockPos $$15 = pPos.offset(pLocalX, pLocalY, pLocalZ);
         double $$16 = pElliptical ? -0.5 : (double)(-6 - pRandom.nextInt(3));
         if ($$14 > $$16 && pRandom.nextDouble() > 0.9) {
            return;
         }

         this.setIcebergBlock($$15, pLevel, pRandom, pHeight - pLocalY, pHeight, pElliptical, pPlaceSnow, pState);
      }
   }

   private void setIcebergBlock(
      BlockPos pPos, LevelAccessor pLevel, RandomSource pRandom, int pHeightRemaining, int pHeight, boolean pElliptical, boolean pPlaceSnow, BlockState pState
   ) {
      BlockState $$8 = pLevel.getBlockState(pPos);
      if ($$8.isAir() || $$8.is(Blocks.SNOW_BLOCK) || $$8.is(Blocks.ICE) || $$8.is(Blocks.WATER)) {
         boolean $$9 = !pElliptical || pRandom.nextDouble() > 0.05;
         int $$10 = pElliptical ? 3 : 2;
         if (pPlaceSnow
            && !$$8.is(Blocks.WATER)
            && (double)pHeightRemaining <= (double)pRandom.nextInt(Math.max(1, pHeight / $$10)) + (double)pHeight * 0.6
            && $$9) {
            this.setBlock(pLevel, pPos, Blocks.SNOW_BLOCK.defaultBlockState());
         } else {
            this.setBlock(pLevel, pPos, pState);
         }
      }
   }

   private int getEllipseC(int pY, int pHeight, int pMinorAxis) {
      int $$3 = pMinorAxis;
      if (pY > 0 && pHeight - pY <= 3) {
         $$3 = pMinorAxis - (4 - (pHeight - pY));
      }

      return $$3;
   }

   private double signedDistanceCircle(int pX, int pZ, BlockPos pCenter, int pRadius, RandomSource pRandom) {
      float $$5 = 10.0F * Mth.clamp(pRandom.nextFloat(), 0.2F, 0.8F) / (float)pRadius;
      return (double)$$5 + Math.pow((double)(pX - pCenter.getX()), 2.0) + Math.pow((double)(pZ - pCenter.getZ()), 2.0) - Math.pow((double)pRadius, 2.0);
   }

   private double signedDistanceEllipse(int pX, int pZ, BlockPos pCenter, int pMajorRadius, int pMinorRadius, double pAngle) {
      return Math.pow(((double)(pX - pCenter.getX()) * Math.cos(pAngle) - (double)(pZ - pCenter.getZ()) * Math.sin(pAngle)) / (double)pMajorRadius, 2.0)
         + Math.pow(((double)(pX - pCenter.getX()) * Math.sin(pAngle) + (double)(pZ - pCenter.getZ()) * Math.cos(pAngle)) / (double)pMinorRadius, 2.0)
         - 1.0;
   }

   private int heightDependentRadiusRound(RandomSource pRandom, int pY, int pHeight, int pMajorAxis) {
      float $$4 = 3.5F - pRandom.nextFloat();
      float $$5 = (1.0F - (float)Math.pow((double)pY, 2.0) / ((float)pHeight * $$4)) * (float)pMajorAxis;
      if (pHeight > 15 + pRandom.nextInt(5)) {
         int $$6 = pY < 3 + pRandom.nextInt(6) ? pY / 2 : pY;
         $$5 = (1.0F - (float)$$6 / ((float)pHeight * $$4 * 0.4F)) * (float)pMajorAxis;
      }

      return Mth.ceil($$5 / 2.0F);
   }

   private int heightDependentRadiusEllipse(int pY, int pHeight, int pMaxRadius) {
      float $$3 = 1.0F;
      float $$4 = (1.0F - (float)Math.pow((double)pY, 2.0) / ((float)pHeight * 1.0F)) * (float)pMaxRadius;
      return Mth.ceil($$4 / 2.0F);
   }

   private int heightDependentRadiusSteep(RandomSource pRandom, int pY, int pHeight, int pMaxRadius) {
      float $$4 = 1.0F + pRandom.nextFloat() / 2.0F;
      float $$5 = (1.0F - (float)pY / ((float)pHeight * $$4)) * (float)pMaxRadius;
      return Mth.ceil($$5 / 2.0F);
   }

   private static boolean isIcebergState(BlockState pState) {
      return pState.is(Blocks.PACKED_ICE) || pState.is(Blocks.SNOW_BLOCK) || pState.is(Blocks.BLUE_ICE);
   }

   private boolean belowIsAir(BlockGetter pLevel, BlockPos pPos) {
      return pLevel.getBlockState(pPos.below()).isAir();
   }

   private void smooth(LevelAccessor pLevel, BlockPos pPos, int pMajorRadius, int pHeight, boolean pElliptical, int pMinorRadius) {
      int $$6 = pElliptical ? pMinorRadius : pMajorRadius / 2;

      for (int $$7 = -$$6; $$7 <= $$6; $$7++) {
         for (int $$8 = -$$6; $$8 <= $$6; $$8++) {
            for (int $$9 = 0; $$9 <= pHeight; $$9++) {
               BlockPos $$10 = pPos.offset($$7, $$9, $$8);
               BlockState $$11 = pLevel.getBlockState($$10);
               if (isIcebergState($$11) || $$11.is(Blocks.SNOW)) {
                  if (this.belowIsAir(pLevel, $$10)) {
                     this.setBlock(pLevel, $$10, Blocks.AIR.defaultBlockState());
                     this.setBlock(pLevel, $$10.above(), Blocks.AIR.defaultBlockState());
                  } else if (isIcebergState($$11)) {
                     BlockState[] $$12 = new BlockState[]{
                        pLevel.getBlockState($$10.west()),
                        pLevel.getBlockState($$10.east()),
                        pLevel.getBlockState($$10.north()),
                        pLevel.getBlockState($$10.south())
                     };
                     int $$13 = 0;

                     for (BlockState $$14 : $$12) {
                        if (!isIcebergState($$14)) {
                           $$13++;
                        }
                     }

                     if ($$13 >= 3) {
                        this.setBlock(pLevel, $$10, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            }
         }
      }
   }
}
