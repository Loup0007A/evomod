package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class EndCityStructure extends Structure {
   public static final MapCodec<EndCityStructure> CODEC = simpleCodec(EndCityStructure::new);

   public EndCityStructure(Structure.StructureSettings p_227526_) {
      super(p_227526_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      Rotation $$1 = Rotation.getRandom(pContext.random());
      BlockPos $$2 = this.getLowestYIn5by5BoxOffset7Blocks(pContext, $$1);
      return $$2.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub($$2, (Consumer<StructurePiecesBuilder>)(p_227538_ -> this.generatePieces(p_227538_, $$2, $$1, pContext))));
   }

   private void generatePieces(StructurePiecesBuilder pBuilder, BlockPos pStartPos, Rotation pRotation, Structure.GenerationContext pContext) {
      List<StructurePiece> $$4 = Lists.newArrayList();
      EndCityPieces.startHouseTower(pContext.structureTemplateManager(), pStartPos, pRotation, $$4, pContext.random());
      $$4.forEach(pBuilder::addPiece);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.END_CITY;
   }
}
