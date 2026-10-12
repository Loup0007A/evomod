package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;

public class BlockColumnFeature extends Feature<BlockColumnConfiguration> {
   public BlockColumnFeature(Codec<BlockColumnConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockColumnConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockColumnConfiguration $$2 = pContext.config();
      RandomSource $$3 = pContext.random();
      int $$4 = $$2.layers().size();
      int[] $$5 = new int[$$4];
      int $$6 = 0;

      for (int $$7 = 0; $$7 < $$4; $$7++) {
         $$5[$$7] = $$2.layers().get($$7).height().sample($$3);
         $$6 += $$5[$$7];
      }

      if ($$6 == 0) {
         return false;
      } else {
         BlockPos.MutableBlockPos $$8 = pContext.origin().mutable();
         BlockPos.MutableBlockPos $$9 = $$8.mutable().move($$2.direction());

         for (int $$10 = 0; $$10 < $$6; $$10++) {
            if (!$$2.allowedPlacement().test($$1, $$9)) {
               truncate($$5, $$6, $$10, $$2.prioritizeTip());
               break;
            }

            $$9.move($$2.direction());
         }

         for (int $$11 = 0; $$11 < $$4; $$11++) {
            int $$12 = $$5[$$11];
            if ($$12 != 0) {
               BlockColumnConfiguration.Layer $$13 = $$2.layers().get($$11);

               for (int $$14 = 0; $$14 < $$12; $$14++) {
                  $$1.setBlock($$8, $$13.state().getState($$3, $$8), 2);
                  $$8.move($$2.direction());
               }
            }
         }

         return true;
      }
   }

   private static void truncate(int[] pLayerHeights, int pTotalHeight, int pCurrentHeight, boolean pPrioritizeTip) {
      int $$4 = pTotalHeight - pCurrentHeight;
      int $$5 = pPrioritizeTip ? 1 : -1;
      int $$6 = pPrioritizeTip ? 0 : pLayerHeights.length - 1;
      int $$7 = pPrioritizeTip ? pLayerHeights.length : -1;

      for (int $$8 = $$6; $$8 != $$7 && $$4 > 0; $$8 += $$5) {
         int $$9 = pLayerHeights[$$8];
         int $$10 = Math.min($$9, $$4);
         $$4 -= $$10;
         pLayerHeights[$$8] -= $$10;
      }
   }
}
