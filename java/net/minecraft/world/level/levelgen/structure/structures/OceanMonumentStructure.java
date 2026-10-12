package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanMonumentStructure extends Structure {
   public static final MapCodec<OceanMonumentStructure> CODEC = simpleCodec(OceanMonumentStructure::new);

   public OceanMonumentStructure(Structure.StructureSettings p_228955_) {
      super(p_228955_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      int $$1 = pContext.chunkPos().getBlockX(9);
      int $$2 = pContext.chunkPos().getBlockZ(9);

      for (Holder<Biome> $$4 : pContext.biomeSource().getBiomesWithin($$1, pContext.chunkGenerator().getSeaLevel(), $$2, 29, pContext.randomState().sampler())) {
         if (!$$4.is(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
            return Optional.empty();
         }
      }

      return onTopOfChunkCenter(pContext, Heightmap.Types.OCEAN_FLOOR_WG, p_228967_ -> generatePieces(p_228967_, pContext));
   }

   private static StructurePiece createTopPiece(ChunkPos pChunkPos, WorldgenRandom pRandom) {
      int $$2 = pChunkPos.getMinBlockX() - 29;
      int $$3 = pChunkPos.getMinBlockZ() - 29;
      Direction $$4 = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
      return new OceanMonumentPieces.MonumentBuilding(pRandom, $$2, $$3, $$4);
   }

   private static void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
      pBuilder.addPiece(createTopPiece(pContext.chunkPos(), pContext.random()));
   }

   public static PiecesContainer regeneratePiecesAfterLoad(ChunkPos pChunkPos, long pSeed, PiecesContainer pPiecesContainer) {
      if (pPiecesContainer.isEmpty()) {
         return pPiecesContainer;
      } else {
         WorldgenRandom $$3 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         $$3.setLargeFeatureSeed(pSeed, pChunkPos.x, pChunkPos.z);
         StructurePiece $$4 = pPiecesContainer.pieces().get(0);
         BoundingBox $$5 = $$4.getBoundingBox();
         int $$6 = $$5.minX();
         int $$7 = $$5.minZ();
         Direction $$8 = Direction.Plane.HORIZONTAL.getRandomDirection($$3);
         Direction $$9 = Objects.requireNonNullElse($$4.getOrientation(), $$8);
         StructurePiece $$10 = new OceanMonumentPieces.MonumentBuilding($$3, $$6, $$7, $$9);
         StructurePiecesBuilder $$11 = new StructurePiecesBuilder();
         $$11.addPiece($$10);
         return $$11.build();
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.OCEAN_MONUMENT;
   }
}
