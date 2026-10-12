package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class OreFeature extends Feature<OreConfiguration> {
   public OreFeature(Codec<OreConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> pContext) {
      RandomSource $$1 = pContext.random();
      BlockPos $$2 = pContext.origin();
      WorldGenLevel $$3 = pContext.level();
      OreConfiguration $$4 = pContext.config();
      float $$5 = $$1.nextFloat() * (float) Math.PI;
      float $$6 = (float)$$4.size / 8.0F;
      int $$7 = Mth.ceil(((float)$$4.size / 16.0F * 2.0F + 1.0F) / 2.0F);
      double $$8 = (double)$$2.getX() + Math.sin((double)$$5) * (double)$$6;
      double $$9 = (double)$$2.getX() - Math.sin((double)$$5) * (double)$$6;
      double $$10 = (double)$$2.getZ() + Math.cos((double)$$5) * (double)$$6;
      double $$11 = (double)$$2.getZ() - Math.cos((double)$$5) * (double)$$6;
      int $$12 = 2;
      double $$13 = (double)($$2.getY() + $$1.nextInt(3) - 2);
      double $$14 = (double)($$2.getY() + $$1.nextInt(3) - 2);
      int $$15 = $$2.getX() - Mth.ceil($$6) - $$7;
      int $$16 = $$2.getY() - 2 - $$7;
      int $$17 = $$2.getZ() - Mth.ceil($$6) - $$7;
      int $$18 = 2 * (Mth.ceil($$6) + $$7);
      int $$19 = 2 * (2 + $$7);

      for (int $$20 = $$15; $$20 <= $$15 + $$18; $$20++) {
         for (int $$21 = $$17; $$21 <= $$17 + $$18; $$21++) {
            if ($$16 <= $$3.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, $$20, $$21)) {
               return this.doPlace($$3, $$1, $$4, $$8, $$9, $$10, $$11, $$13, $$14, $$15, $$16, $$17, $$18, $$19);
            }
         }
      }

      return false;
   }

   protected boolean doPlace(
      WorldGenLevel pLevel,
      RandomSource pRandom,
      OreConfiguration pConfig,
      double pMinX,
      double pMaxX,
      double pMinZ,
      double pMaxZ,
      double pMinY,
      double pMaxY,
      int pX,
      int pY,
      int pZ,
      int pWidth,
      int pHeight
   ) {
      int $$14 = 0;
      BitSet $$15 = new BitSet(pWidth * pHeight * pWidth);
      BlockPos.MutableBlockPos $$16 = new BlockPos.MutableBlockPos();
      int $$17 = pConfig.size;
      double[] $$18 = new double[$$17 * 4];

      for (int $$19 = 0; $$19 < $$17; $$19++) {
         float $$20 = (float)$$19 / (float)$$17;
         double $$21 = Mth.lerp((double)$$20, pMinX, pMaxX);
         double $$22 = Mth.lerp((double)$$20, pMinY, pMaxY);
         double $$23 = Mth.lerp((double)$$20, pMinZ, pMaxZ);
         double $$24 = pRandom.nextDouble() * (double)$$17 / 16.0;
         double $$25 = ((double)(Mth.sin((float) Math.PI * $$20) + 1.0F) * $$24 + 1.0) / 2.0;
         $$18[$$19 * 4 + 0] = $$21;
         $$18[$$19 * 4 + 1] = $$22;
         $$18[$$19 * 4 + 2] = $$23;
         $$18[$$19 * 4 + 3] = $$25;
      }

      for (int $$26 = 0; $$26 < $$17 - 1; $$26++) {
         if (!($$18[$$26 * 4 + 3] <= 0.0)) {
            for (int $$27 = $$26 + 1; $$27 < $$17; $$27++) {
               if (!($$18[$$27 * 4 + 3] <= 0.0)) {
                  double $$28 = $$18[$$26 * 4 + 0] - $$18[$$27 * 4 + 0];
                  double $$29 = $$18[$$26 * 4 + 1] - $$18[$$27 * 4 + 1];
                  double $$30 = $$18[$$26 * 4 + 2] - $$18[$$27 * 4 + 2];
                  double $$31 = $$18[$$26 * 4 + 3] - $$18[$$27 * 4 + 3];
                  if ($$31 * $$31 > $$28 * $$28 + $$29 * $$29 + $$30 * $$30) {
                     if ($$31 > 0.0) {
                        $$18[$$27 * 4 + 3] = -1.0;
                     } else {
                        $$18[$$26 * 4 + 3] = -1.0;
                     }
                  }
               }
            }
         }
      }

      try (BulkSectionAccess $$32 = new BulkSectionAccess(pLevel)) {
         for (int $$33 = 0; $$33 < $$17; $$33++) {
            double $$34 = $$18[$$33 * 4 + 3];
            if (!($$34 < 0.0)) {
               double $$35 = $$18[$$33 * 4 + 0];
               double $$36 = $$18[$$33 * 4 + 1];
               double $$37 = $$18[$$33 * 4 + 2];
               int $$38 = Math.max(Mth.floor($$35 - $$34), pX);
               int $$39 = Math.max(Mth.floor($$36 - $$34), pY);
               int $$40 = Math.max(Mth.floor($$37 - $$34), pZ);
               int $$41 = Math.max(Mth.floor($$35 + $$34), $$38);
               int $$42 = Math.max(Mth.floor($$36 + $$34), $$39);
               int $$43 = Math.max(Mth.floor($$37 + $$34), $$40);

               for (int $$44 = $$38; $$44 <= $$41; $$44++) {
                  double $$45 = ((double)$$44 + 0.5 - $$35) / $$34;
                  if ($$45 * $$45 < 1.0) {
                     for (int $$46 = $$39; $$46 <= $$42; $$46++) {
                        double $$47 = ((double)$$46 + 0.5 - $$36) / $$34;
                        if ($$45 * $$45 + $$47 * $$47 < 1.0) {
                           for (int $$48 = $$40; $$48 <= $$43; $$48++) {
                              double $$49 = ((double)$$48 + 0.5 - $$37) / $$34;
                              if ($$45 * $$45 + $$47 * $$47 + $$49 * $$49 < 1.0 && !pLevel.isOutsideBuildHeight($$46)) {
                                 int $$50 = $$44 - pX + ($$46 - pY) * pWidth + ($$48 - pZ) * pWidth * pHeight;
                                 if (!$$15.get($$50)) {
                                    $$15.set($$50);
                                    $$16.set($$44, $$46, $$48);
                                    if (pLevel.ensureCanWrite($$16)) {
                                       LevelChunkSection $$51 = $$32.getSection($$16);
                                       if ($$51 != null) {
                                          int $$52 = SectionPos.sectionRelative($$44);
                                          int $$53 = SectionPos.sectionRelative($$46);
                                          int $$54 = SectionPos.sectionRelative($$48);
                                          BlockState $$55 = $$51.getBlockState($$52, $$53, $$54);

                                          for (OreConfiguration.TargetBlockState $$56 : pConfig.targetStates) {
                                             if (canPlaceOre($$55, $$32::getBlockState, pRandom, pConfig, $$56, $$16)) {
                                                $$51.setBlockState($$52, $$53, $$54, $$56.state, false);
                                                $$14++;
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return $$14 > 0;
   }

   public static boolean canPlaceOre(
      BlockState pState,
      Function<BlockPos, BlockState> pAdjacentStateAccessor,
      RandomSource pRandom,
      OreConfiguration pConfig,
      OreConfiguration.TargetBlockState pTargetState,
      BlockPos.MutableBlockPos pMutablePos
   ) {
      if (!pTargetState.target.test(pState, pRandom)) {
         return false;
      } else {
         return shouldSkipAirCheck(pRandom, pConfig.discardChanceOnAirExposure) ? true : !isAdjacentToAir(pAdjacentStateAccessor, pMutablePos);
      }
   }

   protected static boolean shouldSkipAirCheck(RandomSource pRandom, float pChance) {
      if (pChance <= 0.0F) {
         return true;
      } else {
         return pChance >= 1.0F ? false : pRandom.nextFloat() >= pChance;
      }
   }
}
