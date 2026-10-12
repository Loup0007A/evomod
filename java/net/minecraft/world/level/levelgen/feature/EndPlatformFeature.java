package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPlatformFeature extends Feature<NoneFeatureConfiguration> {
   public EndPlatformFeature(Codec<NoneFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
      createEndPlatform(pContext.level(), pContext.origin(), false);
      return true;
   }

   public static void createEndPlatform(ServerLevelAccessor pLevel, BlockPos pPos, boolean pDropBlocks) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();

      for (int $$4 = -2; $$4 <= 2; $$4++) {
         for (int $$5 = -2; $$5 <= 2; $$5++) {
            for (int $$6 = -1; $$6 < 3; $$6++) {
               BlockPos $$7 = $$3.set(pPos).move($$5, $$6, $$4);
               Block $$8 = $$6 == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
               if (!pLevel.getBlockState($$7).is($$8)) {
                  if (pDropBlocks) {
                     pLevel.destroyBlock($$7, true, null);
                  }

                  pLevel.setBlock($$7, $$8.defaultBlockState(), 3);
               }
            }
         }
      }
   }
}
