package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;

public class LargeDripstoneFeature extends Feature<LargeDripstoneConfiguration> {
   public LargeDripstoneFeature(Codec<LargeDripstoneConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      LargeDripstoneConfiguration $$3 = pContext.config();
      RandomSource $$4 = pContext.random();
      if (!DripstoneUtils.isEmptyOrWater($$1, $$2)) {
         return false;
      } else {
         Optional<Column> $$5 = Column.scan($$1, $$2, $$3.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava);
         if (!$$5.isEmpty() && $$5.get() instanceof Column.Range) {
            Column.Range $$6 = (Column.Range)$$5.get();
            if ($$6.height() < 4) {
               return false;
            } else {
               int $$7 = (int)((float)$$6.height() * $$3.maxColumnRadiusToCaveHeightRatio);
               int $$8 = Mth.clamp($$7, $$3.columnRadius.getMinValue(), $$3.columnRadius.getMaxValue());
               int $$9 = Mth.randomBetweenInclusive($$4, $$3.columnRadius.getMinValue(), $$8);
               LargeDripstoneFeature.LargeDripstone $$10 = makeDripstone($$2.atY($$6.ceiling() - 1), false, $$4, $$9, $$3.stalactiteBluntness, $$3.heightScale);
               LargeDripstoneFeature.LargeDripstone $$11 = makeDripstone($$2.atY($$6.floor() + 1), true, $$4, $$9, $$3.stalagmiteBluntness, $$3.heightScale);
               LargeDripstoneFeature.WindOffsetter $$12;
               if ($$10.isSuitableForWind($$3) && $$11.isSuitableForWind($$3)) {
                  $$12 = new LargeDripstoneFeature.WindOffsetter($$2.getY(), $$4, $$3.windSpeed);
               } else {
                  $$12 = LargeDripstoneFeature.WindOffsetter.noWind();
               }

               boolean $$14 = $$10.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary($$1, $$12);
               boolean $$15 = $$11.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary($$1, $$12);
               if ($$14) {
                  $$10.placeBlocks($$1, $$4, $$12);
               }

               if ($$15) {
                  $$11.placeBlocks($$1, $$4, $$12);
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   private static LargeDripstoneFeature.LargeDripstone makeDripstone(
      BlockPos pRoot, boolean pPointingUp, RandomSource pRandom, int pRadius, FloatProvider pBluntnessBase, FloatProvider pScaleBase
   ) {
      return new LargeDripstoneFeature.LargeDripstone(pRoot, pPointingUp, pRadius, (double)pBluntnessBase.sample(pRandom), (double)pScaleBase.sample(pRandom));
   }

   private void placeDebugMarkers(WorldGenLevel pLevel, BlockPos pPos, Column.Range pRange, LargeDripstoneFeature.WindOffsetter pWindOffsetter) {
      pLevel.setBlock(pWindOffsetter.offset(pPos.atY(pRange.ceiling() - 1)), Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
      pLevel.setBlock(pWindOffsetter.offset(pPos.atY(pRange.floor() + 1)), Blocks.GOLD_BLOCK.defaultBlockState(), 2);

      for (BlockPos.MutableBlockPos $$4 = pPos.atY(pRange.floor() + 2).mutable(); $$4.getY() < pRange.ceiling() - 1; $$4.move(Direction.UP)) {
         BlockPos $$5 = pWindOffsetter.offset($$4);
         if (DripstoneUtils.isEmptyOrWater(pLevel, $$5) || pLevel.getBlockState($$5).is(Blocks.DRIPSTONE_BLOCK)) {
            pLevel.setBlock($$5, Blocks.CREEPER_HEAD.defaultBlockState(), 2);
         }
      }
   }

   static final class LargeDripstone {
      private BlockPos root;
      private final boolean pointingUp;
      private int radius;
      private final double bluntness;
      private final double scale;

      LargeDripstone(BlockPos pRoot, boolean pPointingUp, int pRadius, double pBluntness, double pScale) {
         this.root = pRoot;
         this.pointingUp = pPointingUp;
         this.radius = pRadius;
         this.bluntness = pBluntness;
         this.scale = pScale;
      }

      private int getHeight() {
         return this.getHeightAtRadius(0.0F);
      }

      private int getMinY() {
         return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
      }

      private int getMaxY() {
         return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
      }

      boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(WorldGenLevel pLevel, LargeDripstoneFeature.WindOffsetter pWindOffsetter) {
         while (this.radius > 1) {
            BlockPos.MutableBlockPos $$2 = this.root.mutable();
            int $$3 = Math.min(10, this.getHeight());

            for (int $$4 = 0; $$4 < $$3; $$4++) {
               if (pLevel.getBlockState($$2).is(Blocks.LAVA)) {
                  return false;
               }

               if (DripstoneUtils.isCircleMostlyEmbeddedInStone(pLevel, pWindOffsetter.offset($$2), this.radius)) {
                  this.root = $$2;
                  return true;
               }

               $$2.move(this.pointingUp ? Direction.DOWN : Direction.UP);
            }

            this.radius /= 2;
         }

         return false;
      }

      private int getHeightAtRadius(float pRadius) {
         return (int)DripstoneUtils.getDripstoneHeight((double)pRadius, (double)this.radius, this.scale, this.bluntness);
      }

      void placeBlocks(WorldGenLevel pLevel, RandomSource pRandom, LargeDripstoneFeature.WindOffsetter pWindOffsetter) {
         for (int $$3 = -this.radius; $$3 <= this.radius; $$3++) {
            for (int $$4 = -this.radius; $$4 <= this.radius; $$4++) {
               float $$5 = Mth.sqrt((float)($$3 * $$3 + $$4 * $$4));
               if (!($$5 > (float)this.radius)) {
                  int $$6 = this.getHeightAtRadius($$5);
                  if ($$6 > 0) {
                     if ((double)pRandom.nextFloat() < 0.2) {
                        $$6 = (int)((float)$$6 * Mth.randomBetween(pRandom, 0.8F, 1.0F));
                     }

                     BlockPos.MutableBlockPos $$7 = this.root.offset($$3, 0, $$4).mutable();
                     boolean $$8 = false;
                     int $$9 = this.pointingUp ? pLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$7.getX(), $$7.getZ()) : Integer.MAX_VALUE;

                     for (int $$10 = 0; $$10 < $$6 && $$7.getY() < $$9; $$10++) {
                        BlockPos $$11 = pWindOffsetter.offset($$7);
                        if (DripstoneUtils.isEmptyOrWaterOrLava(pLevel, $$11)) {
                           $$8 = true;
                           Block $$12 = Blocks.DRIPSTONE_BLOCK;
                           pLevel.setBlock($$11, $$12.defaultBlockState(), 2);
                        } else if ($$8 && pLevel.getBlockState($$11).is(BlockTags.BASE_STONE_OVERWORLD)) {
                           break;
                        }

                        $$7.move(this.pointingUp ? Direction.UP : Direction.DOWN);
                     }
                  }
               }
            }
         }
      }

      boolean isSuitableForWind(LargeDripstoneConfiguration pConfig) {
         return this.radius >= pConfig.minRadiusForWind && this.bluntness >= (double)pConfig.minBluntnessForWind;
      }
   }

   static final class WindOffsetter {
      private final int originY;
      @Nullable
      private final Vec3 windSpeed;

      WindOffsetter(int pOriginY, RandomSource pRandom, FloatProvider pMagnitude) {
         this.originY = pOriginY;
         float $$3 = pMagnitude.sample(pRandom);
         float $$4 = Mth.randomBetween(pRandom, 0.0F, (float) Math.PI);
         this.windSpeed = new Vec3((double)(Mth.cos($$4) * $$3), 0.0, (double)(Mth.sin($$4) * $$3));
      }

      private WindOffsetter() {
         this.originY = 0;
         this.windSpeed = null;
      }

      static LargeDripstoneFeature.WindOffsetter noWind() {
         return new LargeDripstoneFeature.WindOffsetter();
      }

      BlockPos offset(BlockPos pPos) {
         if (this.windSpeed == null) {
            return pPos;
         } else {
            int $$1 = this.originY - pPos.getY();
            Vec3 $$2 = this.windSpeed.scale((double)$$1);
            return pPos.offset(Mth.floor($$2.x), 0, Mth.floor($$2.z));
         }
      }
   }
}
