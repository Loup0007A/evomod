package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
   public CoralFeature(Codec<NoneFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
      RandomSource $$1 = pContext.random();
      WorldGenLevel $$2 = pContext.level();
      BlockPos $$3 = pContext.origin();
      Optional<Block> $$4 = BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORAL_BLOCKS, $$1).map(Holder::value);
      return $$4.isEmpty() ? false : this.placeFeature($$2, $$1, $$3, $$4.get().defaultBlockState());
   }

   protected abstract boolean placeFeature(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState);

   protected boolean placeCoralBlock(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      BlockPos $$4 = pPos.above();
      BlockState $$5 = pLevel.getBlockState(pPos);
      if (($$5.is(Blocks.WATER) || $$5.is(BlockTags.CORALS)) && pLevel.getBlockState($$4).is(Blocks.WATER)) {
         pLevel.setBlock(pPos, pState, 3);
         if (pRandom.nextFloat() < 0.25F) {
            BuiltInRegistries.BLOCK
               .getRandomElementOf(BlockTags.CORALS, pRandom)
               .map(Holder::value)
               .ifPresent(p_204720_ -> pLevel.setBlock($$4, p_204720_.defaultBlockState(), 2));
         } else if (pRandom.nextFloat() < 0.05F) {
            pLevel.setBlock($$4, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, Integer.valueOf(pRandom.nextInt(4) + 1)), 2);
         }

         for (Direction $$6 : Direction.Plane.HORIZONTAL) {
            if (pRandom.nextFloat() < 0.2F) {
               BlockPos $$7 = pPos.relative($$6);
               if (pLevel.getBlockState($$7).is(Blocks.WATER)) {
                  BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.WALL_CORALS, pRandom).map(Holder::value).ifPresent(p_204725_ -> {
                     BlockState $$4x = p_204725_.defaultBlockState();
                     if ($$4x.hasProperty(BaseCoralWallFanBlock.FACING)) {
                        $$4x = $$4x.setValue(BaseCoralWallFanBlock.FACING, $$6);
                     }

                     pLevel.setBlock($$7, $$4x, 2);
                  });
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
