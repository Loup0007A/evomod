package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@Deprecated
public class CountOnEveryLayerPlacement extends PlacementModifier {
   public static final MapCodec<CountOnEveryLayerPlacement> CODEC = IntProvider.codec(0, 256)
      .fieldOf("count")
      .xmap(CountOnEveryLayerPlacement::new, p_191611_ -> p_191611_.count);
   private final IntProvider count;

   private CountOnEveryLayerPlacement(IntProvider p_191603_) {
      this.count = p_191603_;
   }

   public static CountOnEveryLayerPlacement of(IntProvider pCount) {
      return new CountOnEveryLayerPlacement(pCount);
   }

   public static CountOnEveryLayerPlacement of(int pCount) {
      return of(ConstantInt.of(pCount));
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
      Builder<BlockPos> $$3 = Stream.builder();
      int $$4 = 0;

      boolean $$5;
      do {
         $$5 = false;

         for (int $$6 = 0; $$6 < this.count.sample(pRandom); $$6++) {
            int $$7 = pRandom.nextInt(16) + pPos.getX();
            int $$8 = pRandom.nextInt(16) + pPos.getZ();
            int $$9 = pContext.getHeight(Heightmap.Types.MOTION_BLOCKING, $$7, $$8);
            int $$10 = findOnGroundYPosition(pContext, $$7, $$9, $$8, $$4);
            if ($$10 != Integer.MAX_VALUE) {
               $$3.add(new BlockPos($$7, $$10, $$8));
               $$5 = true;
            }
         }

         $$4++;
      } while ($$5);

      return $$3.build();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.COUNT_ON_EVERY_LAYER;
   }

   private static int findOnGroundYPosition(PlacementContext pContext, int pX, int pY, int pZ, int pCount) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos(pX, pY, pZ);
      int $$6 = 0;
      BlockState $$7 = pContext.getBlockState($$5);

      for (int $$8 = pY; $$8 >= pContext.getMinBuildHeight() + 1; $$8--) {
         $$5.setY($$8 - 1);
         BlockState $$9 = pContext.getBlockState($$5);
         if (!isEmpty($$9) && isEmpty($$7) && !$$9.is(Blocks.BEDROCK)) {
            if ($$6 == pCount) {
               return $$5.getY() + 1;
            }

            $$6++;
         }

         $$7 = $$9;
      }

      return Integer.MAX_VALUE;
   }

   private static boolean isEmpty(BlockState pState) {
      return pState.isAir() || pState.is(Blocks.WATER) || pState.is(Blocks.LAVA);
   }
}
