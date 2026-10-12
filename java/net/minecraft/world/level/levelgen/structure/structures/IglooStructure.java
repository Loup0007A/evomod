package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class IglooStructure extends Structure {
   public static final MapCodec<IglooStructure> CODEC = simpleCodec(IglooStructure::new);

   public IglooStructure(Structure.StructureSettings p_227593_) {
      super(p_227593_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      return onTopOfChunkCenter(pContext, Heightmap.Types.WORLD_SURFACE_WG, p_227598_ -> this.generatePieces(p_227598_, pContext));
   }

   private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
      ChunkPos $$2 = pContext.chunkPos();
      WorldgenRandom $$3 = pContext.random();
      BlockPos $$4 = new BlockPos($$2.getMinBlockX(), 90, $$2.getMinBlockZ());
      Rotation $$5 = Rotation.getRandom($$3);
      IglooPieces.addPieces(pContext.structureTemplateManager(), $$4, $$5, pBuilder, $$3);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.IGLOO;
   }
}
