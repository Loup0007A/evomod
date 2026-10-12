package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
   public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> pContext) {
      ReplaceSphereConfiguration $$1 = pContext.config();
      WorldGenLevel $$2 = pContext.level();
      RandomSource $$3 = pContext.random();
      Block $$4 = $$1.targetState.getBlock();
      BlockPos $$5 = findTarget($$2, pContext.origin().mutable().clamp(Direction.Axis.Y, $$2.getMinBuildHeight() + 1, $$2.getMaxBuildHeight() - 1), $$4);
      if ($$5 == null) {
         return false;
      } else {
         int $$6 = $$1.radius().sample($$3);
         int $$7 = $$1.radius().sample($$3);
         int $$8 = $$1.radius().sample($$3);
         int $$9 = Math.max($$6, Math.max($$7, $$8));
         boolean $$10 = false;

         for (BlockPos $$11 : BlockPos.withinManhattan($$5, $$6, $$7, $$8)) {
            if ($$11.distManhattan($$5) > $$9) {
               break;
            }

            BlockState $$12 = $$2.getBlockState($$11);
            if ($$12.is($$4)) {
               this.setBlock($$2, $$11, $$1.replaceState);
               $$10 = true;
            }
         }

         return $$10;
      }
   }

   @Nullable
   private static BlockPos findTarget(LevelAccessor pLevel, BlockPos.MutableBlockPos pTopPos, Block pBlock) {
      while (pTopPos.getY() > pLevel.getMinBuildHeight() + 1) {
         BlockState $$3 = pLevel.getBlockState(pTopPos);
         if ($$3.is(pBlock)) {
            return pTopPos;
         }

         pTopPos.move(Direction.DOWN);
      }

      return null;
   }
}
