package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class MineshaftStructure extends Structure {
   public static final MapCodec<MineshaftStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_227971_ -> p_227971_.group(settingsCodec(p_227971_), MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type").forGetter(p_227969_ -> p_227969_.type))
            .apply(p_227971_, MineshaftStructure::new)
   );
   private final MineshaftStructure.Type type;

   public MineshaftStructure(Structure.StructureSettings p_227961_, MineshaftStructure.Type p_227962_) {
      super(p_227961_);
      this.type = p_227962_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext pContext) {
      pContext.random().nextDouble();
      ChunkPos $$1 = pContext.chunkPos();
      BlockPos $$2 = new BlockPos($$1.getMiddleBlockX(), 50, $$1.getMinBlockZ());
      StructurePiecesBuilder $$3 = new StructurePiecesBuilder();
      int $$4 = this.generatePiecesAndAdjust($$3, pContext);
      return Optional.of(new Structure.GenerationStub($$2.offset(0, $$4, 0), Either.right($$3)));
   }

   private int generatePiecesAndAdjust(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
      ChunkPos $$2 = pContext.chunkPos();
      WorldgenRandom $$3 = pContext.random();
      ChunkGenerator $$4 = pContext.chunkGenerator();
      MineshaftPieces.MineShaftRoom $$5 = new MineshaftPieces.MineShaftRoom(0, $$3, $$2.getBlockX(2), $$2.getBlockZ(2), this.type);
      pBuilder.addPiece($$5);
      $$5.addChildren($$5, pBuilder, $$3);
      int $$6 = $$4.getSeaLevel();
      if (this.type == MineshaftStructure.Type.MESA) {
         BlockPos $$7 = pBuilder.getBoundingBox().getCenter();
         int $$8 = $$4.getBaseHeight($$7.getX(), $$7.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pContext.heightAccessor(), pContext.randomState());
         int $$9 = $$8 <= $$6 ? $$6 : Mth.randomBetweenInclusive($$3, $$6, $$8);
         int $$10 = $$9 - $$7.getY();
         pBuilder.offsetPiecesVertically($$10);
         return $$10;
      } else {
         return pBuilder.moveBelowSeaLevel($$6, $$4.getMinY(), $$3, 10);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.MINESHAFT;
   }

   public static enum Type implements StringRepresentable {
      NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
      MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

      public static final Codec<MineshaftStructure.Type> CODEC = StringRepresentable.fromEnum(MineshaftStructure.Type::values);
      private static final IntFunction<MineshaftStructure.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      private final String name;
      private final BlockState woodState;
      private final BlockState planksState;
      private final BlockState fenceState;

      private Type(final String pName, final Block pWoodBlock, final Block pPlanksBlock, final Block pFenceBlock) {
         this.name = pName;
         this.woodState = pWoodBlock.defaultBlockState();
         this.planksState = pPlanksBlock.defaultBlockState();
         this.fenceState = pFenceBlock.defaultBlockState();
      }

      public String getName() {
         return this.name;
      }

      public static MineshaftStructure.Type byId(int pId) {
         return BY_ID.apply(pId);
      }

      public BlockState getWoodState() {
         return this.woodState;
      }

      public BlockState getPlanksState() {
         return this.planksState;
      }

      public BlockState getFenceState() {
         return this.fenceState;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
