package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class BlockAgeProcessor extends StructureProcessor {
   public static final MapCodec<BlockAgeProcessor> CODEC = Codec.FLOAT.fieldOf("mossiness").xmap(BlockAgeProcessor::new, p_74023_ -> p_74023_.mossiness);
   private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15F;
   private static final BlockState[] NON_MOSSY_REPLACEMENTS = new BlockState[]{
      Blocks.STONE_SLAB.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState()
   };
   private final float mossiness;

   public BlockAgeProcessor(float p_74013_) {
      this.mossiness = p_74013_;
   }

   @Nullable
   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader pLevel,
      BlockPos pOffset,
      BlockPos pPos,
      StructureTemplate.StructureBlockInfo pBlockInfo,
      StructureTemplate.StructureBlockInfo pRelativeBlockInfo,
      StructurePlaceSettings pSettings
   ) {
      RandomSource $$6 = pSettings.getRandom(pRelativeBlockInfo.pos());
      BlockState $$7 = pRelativeBlockInfo.state();
      BlockPos $$8 = pRelativeBlockInfo.pos();
      BlockState $$9 = null;
      if ($$7.is(Blocks.STONE_BRICKS) || $$7.is(Blocks.STONE) || $$7.is(Blocks.CHISELED_STONE_BRICKS)) {
         $$9 = this.maybeReplaceFullStoneBlock($$6);
      } else if ($$7.is(BlockTags.STAIRS)) {
         $$9 = this.maybeReplaceStairs($$6, pRelativeBlockInfo.state());
      } else if ($$7.is(BlockTags.SLABS)) {
         $$9 = this.maybeReplaceSlab($$6);
      } else if ($$7.is(BlockTags.WALLS)) {
         $$9 = this.maybeReplaceWall($$6);
      } else if ($$7.is(Blocks.OBSIDIAN)) {
         $$9 = this.maybeReplaceObsidian($$6);
      }

      return $$9 != null ? new StructureTemplate.StructureBlockInfo($$8, $$9, pRelativeBlockInfo.nbt()) : pRelativeBlockInfo;
   }

   @Nullable
   private BlockState maybeReplaceFullStoneBlock(RandomSource pRandom) {
      if (pRandom.nextFloat() >= 0.5F) {
         return null;
      } else {
         BlockState[] $$1 = new BlockState[]{Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(pRandom, Blocks.STONE_BRICK_STAIRS)};
         BlockState[] $$2 = new BlockState[]{Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(pRandom, Blocks.MOSSY_STONE_BRICK_STAIRS)};
         return this.getRandomBlock(pRandom, $$1, $$2);
      }
   }

   @Nullable
   private BlockState maybeReplaceStairs(RandomSource pRandom, BlockState pState) {
      Direction $$2 = pState.getValue(StairBlock.FACING);
      Half $$3 = pState.getValue(StairBlock.HALF);
      if (pRandom.nextFloat() >= 0.5F) {
         return null;
      } else {
         BlockState[] $$4 = new BlockState[]{
            Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, $$2).setValue(StairBlock.HALF, $$3),
            Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()
         };
         return this.getRandomBlock(pRandom, NON_MOSSY_REPLACEMENTS, $$4);
      }
   }

   @Nullable
   private BlockState maybeReplaceSlab(RandomSource pRandom) {
      return pRandom.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState() : null;
   }

   @Nullable
   private BlockState maybeReplaceWall(RandomSource pRandom) {
      return pRandom.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState() : null;
   }

   @Nullable
   private BlockState maybeReplaceObsidian(RandomSource pRandom) {
      return pRandom.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : null;
   }

   private static BlockState getRandomFacingStairs(RandomSource pRandom, Block pStairsBlock) {
      return pStairsBlock.defaultBlockState()
         .setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(pRandom))
         .setValue(StairBlock.HALF, Util.getRandom(Half.values(), pRandom));
   }

   private BlockState getRandomBlock(RandomSource pRandom, BlockState[] pNormalStates, BlockState[] pMossyStates) {
      return pRandom.nextFloat() < this.mossiness ? getRandomBlock(pRandom, pMossyStates) : getRandomBlock(pRandom, pNormalStates);
   }

   private static BlockState getRandomBlock(RandomSource pRandom, BlockState[] pStates) {
      return pStates[pRandom.nextInt(pStates.length)];
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.BLOCK_AGE;
   }
}
