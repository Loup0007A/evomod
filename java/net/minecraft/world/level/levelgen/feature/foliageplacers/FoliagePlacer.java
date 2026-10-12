package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
   public static final Codec<FoliagePlacer> CODEC = BuiltInRegistries.FOLIAGE_PLACER_TYPE.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
   protected final IntProvider radius;
   protected final IntProvider offset;

   protected static <P extends FoliagePlacer> P2<Mu<P>, IntProvider, IntProvider> foliagePlacerParts(Instance<P> pInstance) {
      return pInstance.group(
         IntProvider.codec(0, 16).fieldOf("radius").forGetter(p_161449_ -> p_161449_.radius),
         IntProvider.codec(0, 16).fieldOf("offset").forGetter(p_161447_ -> p_161447_.offset)
      );
   }

   public FoliagePlacer(IntProvider pRadius, IntProvider pOffset) {
      this.radius = pRadius;
      this.offset = pOffset;
   }

   protected abstract FoliagePlacerType<?> type();

   public void createFoliage(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pBlockSetter,
      RandomSource pRandom,
      TreeConfiguration pConfig,
      int pMaxFreeTreeHeight,
      FoliagePlacer.FoliageAttachment pAttachment,
      int pFoliageHeight,
      int pFoliageRadius
   ) {
      this.createFoliage(pLevel, pBlockSetter, pRandom, pConfig, pMaxFreeTreeHeight, pAttachment, pFoliageHeight, pFoliageRadius, this.offset(pRandom));
   }

   protected abstract void createFoliage(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pBlockSetter,
      RandomSource pRandom,
      TreeConfiguration pConfig,
      int pMaxFreeTreeHeight,
      FoliagePlacer.FoliageAttachment pAttachment,
      int pFoliageHeight,
      int pFoliageRadius,
      int pOffset
   );

   public abstract int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig);

   public int foliageRadius(RandomSource pRandom, int pRadius) {
      return this.radius.sample(pRandom);
   }

   private int offset(RandomSource pRandom) {
      return this.offset.sample(pRandom);
   }

   protected abstract boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge);

   protected boolean shouldSkipLocationSigned(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      int $$6;
      int $$7;
      if (pLarge) {
         $$6 = Math.min(Math.abs(pLocalX), Math.abs(pLocalX - 1));
         $$7 = Math.min(Math.abs(pLocalZ), Math.abs(pLocalZ - 1));
      } else {
         $$6 = Math.abs(pLocalX);
         $$7 = Math.abs(pLocalZ);
      }

      return this.shouldSkipLocation(pRandom, $$6, pLocalY, $$7, pRange, pLarge);
   }

   protected void placeLeavesRow(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pFoliageSetter,
      RandomSource pRandom,
      TreeConfiguration pTreeConfiguration,
      BlockPos pPos,
      int pRange,
      int pLocalY,
      boolean pLarge
   ) {
      int $$8 = pLarge ? 1 : 0;
      BlockPos.MutableBlockPos $$9 = new BlockPos.MutableBlockPos();

      for (int $$10 = -pRange; $$10 <= pRange + $$8; $$10++) {
         for (int $$11 = -pRange; $$11 <= pRange + $$8; $$11++) {
            if (!this.shouldSkipLocationSigned(pRandom, $$10, pLocalY, $$11, pRange, pLarge)) {
               $$9.setWithOffset(pPos, $$10, pLocalY, $$11);
               tryPlaceLeaf(pLevel, pFoliageSetter, pRandom, pTreeConfiguration, $$9);
            }
         }
      }
   }

   protected final void placeLeavesRowWithHangingLeavesBelow(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pFoliageSetter,
      RandomSource pRandom,
      TreeConfiguration pTreeConfiguration,
      BlockPos pPos,
      int pRange,
      int pLocalY,
      boolean pLarge,
      float pHangingLeavesChance,
      float pHangingLeavesExtensionChance
   ) {
      this.placeLeavesRow(pLevel, pFoliageSetter, pRandom, pTreeConfiguration, pPos, pRange, pLocalY, pLarge);
      int $$10 = pLarge ? 1 : 0;
      BlockPos $$11 = pPos.below();
      BlockPos.MutableBlockPos $$12 = new BlockPos.MutableBlockPos();

      for (Direction $$13 : Direction.Plane.HORIZONTAL) {
         Direction $$14 = $$13.getClockWise();
         int $$15 = $$14.getAxisDirection() == Direction.AxisDirection.POSITIVE ? pRange + $$10 : pRange;
         $$12.setWithOffset(pPos, 0, pLocalY - 1, 0).move($$14, $$15).move($$13, -pRange);
         int $$16 = -pRange;

         while ($$16 < pRange + $$10) {
            boolean $$17 = pFoliageSetter.isSet($$12.move(Direction.UP));
            $$12.move(Direction.DOWN);
            if ($$17 && tryPlaceExtension(pLevel, pFoliageSetter, pRandom, pTreeConfiguration, pHangingLeavesChance, $$11, $$12)) {
               $$12.move(Direction.DOWN);
               tryPlaceExtension(pLevel, pFoliageSetter, pRandom, pTreeConfiguration, pHangingLeavesExtensionChance, $$11, $$12);
               $$12.move(Direction.UP);
            }

            $$16++;
            $$12.move($$13);
         }
      }
   }

   private static boolean tryPlaceExtension(
      LevelSimulatedReader pLevel,
      FoliagePlacer.FoliageSetter pFoliageSetter,
      RandomSource pRandom,
      TreeConfiguration pTreeConfiguration,
      float pExtensionChance,
      BlockPos pLogPos,
      BlockPos.MutableBlockPos pPos
   ) {
      if (pPos.distManhattan(pLogPos) >= 7) {
         return false;
      } else {
         return pRandom.nextFloat() > pExtensionChance ? false : tryPlaceLeaf(pLevel, pFoliageSetter, pRandom, pTreeConfiguration, pPos);
      }
   }

   protected static boolean tryPlaceLeaf(
      LevelSimulatedReader pLevel, FoliagePlacer.FoliageSetter pFoliageSetter, RandomSource pRandom, TreeConfiguration pTreeConfiguration, BlockPos pPos
   ) {
      if (!TreeFeature.validTreePos(pLevel, pPos)) {
         return false;
      } else {
         BlockState $$5 = pTreeConfiguration.foliageProvider.getState(pRandom, pPos);
         if ($$5.hasProperty(BlockStateProperties.WATERLOGGED)) {
            $$5 = $$5.setValue(
               BlockStateProperties.WATERLOGGED, Boolean.valueOf(pLevel.isFluidAtPosition(pPos, p_225638_ -> p_225638_.isSourceOfType(Fluids.WATER)))
            );
         }

         pFoliageSetter.set(pPos, $$5);
         return true;
      }
   }

   public static final class FoliageAttachment {
      private final BlockPos pos;
      private final int radiusOffset;
      private final boolean doubleTrunk;

      public FoliageAttachment(BlockPos pPos, int pRadiusOffset, boolean pDoubleTrunk) {
         this.pos = pPos;
         this.radiusOffset = pRadiusOffset;
         this.doubleTrunk = pDoubleTrunk;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public int radiusOffset() {
         return this.radiusOffset;
      }

      public boolean doubleTrunk() {
         return this.doubleTrunk;
      }
   }

   public interface FoliageSetter {
      void set(BlockPos pPos, BlockState pState);

      boolean isSet(BlockPos pPos);
   }
}
