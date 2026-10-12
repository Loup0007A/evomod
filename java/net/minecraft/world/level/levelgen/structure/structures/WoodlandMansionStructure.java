package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class WoodlandMansionStructure extends Structure {
   public static final MapCodec<WoodlandMansionStructure> CODEC = simpleCodec(WoodlandMansionStructure::new);

   public WoodlandMansionStructure(Structure.StructureSettings p_230225_) {
      super(p_230225_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      Rotation $$1 = Rotation.getRandom(pContext.random());
      BlockPos $$2 = this.getLowestYIn5by5BoxOffset7Blocks(pContext, $$1);
      return $$2.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub($$2, (Consumer<StructurePiecesBuilder>)(p_230240_ -> this.generatePieces(p_230240_, pContext, $$2, $$1))));
   }

   private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext, BlockPos pPos, Rotation pRotation) {
      List<WoodlandMansionPieces.WoodlandMansionPiece> $$4 = Lists.newLinkedList();
      WoodlandMansionPieces.generateMansion(pContext.structureTemplateManager(), pPos, pRotation, $$4, pContext.random());
      $$4.forEach(pBuilder::addPiece);
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
      BlockPos.MutableBlockPos $$7 = new BlockPos.MutableBlockPos();
      int $$8 = pLevel.getMinBuildHeight();
      BoundingBox $$9 = pPieces.calculateBoundingBox();
      int $$10 = $$9.minY();

      for (int $$11 = pBoundingBox.minX(); $$11 <= pBoundingBox.maxX(); $$11++) {
         for (int $$12 = pBoundingBox.minZ(); $$12 <= pBoundingBox.maxZ(); $$12++) {
            $$7.set($$11, $$10, $$12);
            if (!pLevel.isEmptyBlock($$7) && $$9.isInside($$7) && pPieces.isInsidePiece($$7)) {
               for (int $$13 = $$10 - 1; $$13 > $$8; $$13--) {
                  $$7.setY($$13);
                  if (!pLevel.isEmptyBlock($$7) && !pLevel.getBlockState($$7).liquid()) {
                     break;
                  }

                  pLevel.setBlock($$7, Blocks.COBBLESTONE.defaultBlockState(), 2);
               }
            }
         }
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.WOODLAND_MANSION;
   }
}
