package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidStructure extends SinglePieceStructure {
   public static final MapCodec<DesertPyramidStructure> CODEC = simpleCodec(DesertPyramidStructure::new);

   public DesertPyramidStructure(Structure.StructureSettings p_227418_) {
      super(DesertPyramidPiece::new, 21, 21, p_227418_);
   }

   @Override
   public void afterPlace(
      WorldGenLevel pLevel,
      StructureManager pStructureManager,
      ChunkGenerator pChunkGenerator,
      RandomSource pRandom,
      BoundingBox pBoundingBox,
      ChunkPos pChunkPos,
      PiecesContainer pPieces
   ) {
      Set<BlockPos> $$7 = SortedArraySet.create(Vec3i::compareTo);

      for (StructurePiece $$8 : pPieces.pieces()) {
         if ($$8 instanceof DesertPyramidPiece $$9) {
            $$7.addAll($$9.getPotentialSuspiciousSandWorldPositions());
            placeSuspiciousSand(pBoundingBox, pLevel, $$9.getRandomCollapsedRoofPos());
         }
      }

      ObjectArrayList<BlockPos> $$10 = new ObjectArrayList($$7.stream().toList());
      RandomSource $$11 = RandomSource.create(pLevel.getSeed()).forkPositional().at(pPieces.calculateBoundingBox().getCenter());
      Util.shuffle($$10, $$11);
      int $$12 = Math.min($$7.size(), $$11.nextInt(5, 8));
      ObjectListIterator var12 = $$10.iterator();

      while (var12.hasNext()) {
         BlockPos $$13 = (BlockPos)var12.next();
         if ($$12 > 0) {
            $$12--;
            placeSuspiciousSand(pBoundingBox, pLevel, $$13);
         } else if (pBoundingBox.isInside($$13)) {
            pLevel.setBlock($$13, Blocks.SAND.defaultBlockState(), 2);
         }
      }
   }

   private static void placeSuspiciousSand(BoundingBox pBoundingBox, WorldGenLevel pWorldGenLevel, BlockPos pPos) {
      if (pBoundingBox.isInside(pPos)) {
         pWorldGenLevel.setBlock(pPos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
         pWorldGenLevel.getBlockEntity(pPos, BlockEntityType.BRUSHABLE_BLOCK)
            .ifPresent(p_327492_ -> p_327492_.setLootTable(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, pPos.asLong()));
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.DESERT_PYRAMID;
   }
}
