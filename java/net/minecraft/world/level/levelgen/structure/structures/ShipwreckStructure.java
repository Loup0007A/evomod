package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class ShipwreckStructure extends Structure {
   public static final MapCodec<ShipwreckStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_229401_ -> p_229401_.group(settingsCodec(p_229401_), Codec.BOOL.fieldOf("is_beached").forGetter(p_229399_ -> p_229399_.isBeached))
            .apply(p_229401_, ShipwreckStructure::new)
   );
   public final boolean isBeached;

   public ShipwreckStructure(Structure.StructureSettings p_229388_, boolean p_229389_) {
      super(p_229388_);
      this.isBeached = p_229389_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      Heightmap.Types $$1 = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
      return onTopOfChunkCenter(pContext, $$1, p_229394_ -> this.generatePieces(p_229394_, pContext));
   }

   private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
      Rotation $$2 = Rotation.getRandom(pContext.random());
      BlockPos $$3 = new BlockPos(pContext.chunkPos().getMinBlockX(), 90, pContext.chunkPos().getMinBlockZ());
      ShipwreckPieces.ShipwreckPiece $$4 = ShipwreckPieces.addRandomPiece(
         pContext.structureTemplateManager(), $$3, $$2, pBuilder, pContext.random(), this.isBeached
      );
      if ($$4.isTooBigToFitInWorldGenRegion()) {
         BoundingBox $$5 = $$4.getBoundingBox();
         int $$7;
         if (this.isBeached) {
            int $$6 = Structure.getLowestY(pContext, $$5.minX(), $$5.getXSpan(), $$5.minZ(), $$5.getZSpan());
            $$7 = $$4.calculateBeachedPosition($$6, pContext.random());
         } else {
            $$7 = Structure.getMeanFirstOccupiedHeight(pContext, $$5.minX(), $$5.getXSpan(), $$5.minZ(), $$5.getZSpan());
         }

         $$4.adjustPositionHeight($$7);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.SHIPWRECK;
   }
}
