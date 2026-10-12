package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class StrongholdStructure extends Structure {
   public static final MapCodec<StrongholdStructure> CODEC = simpleCodec(StrongholdStructure::new);

   public StrongholdStructure(Structure.StructureSettings p_229939_) {
      super(p_229939_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      return Optional.of(
         new Structure.GenerationStub(
            pContext.chunkPos().getWorldPosition(), (Consumer<StructurePiecesBuilder>)(p_229944_ -> generatePieces(p_229944_, pContext))
         )
      );
   }

   private static void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
      int $$2 = 0;

      StrongholdPieces.StartPiece $$3;
      do {
         pBuilder.clear();
         pContext.random().setLargeFeatureSeed(pContext.seed() + (long)($$2++), pContext.chunkPos().x, pContext.chunkPos().z);
         StrongholdPieces.resetPieces();
         $$3 = new StrongholdPieces.StartPiece(pContext.random(), pContext.chunkPos().getBlockX(2), pContext.chunkPos().getBlockZ(2));
         pBuilder.addPiece($$3);
         $$3.addChildren($$3, pBuilder, pContext.random());
         List<StructurePiece> $$4 = $$3.pendingChildren;

         while (!$$4.isEmpty()) {
            int $$5 = pContext.random().nextInt($$4.size());
            StructurePiece $$6 = $$4.remove($$5);
            $$6.addChildren($$3, pBuilder, pContext.random());
         }

         pBuilder.moveBelowSeaLevel(pContext.chunkGenerator().getSeaLevel(), pContext.chunkGenerator().getMinY(), pContext.random(), 10);
      } while (pBuilder.isEmpty() || $$3.portalRoomPiece == null);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.STRONGHOLD;
   }
}
