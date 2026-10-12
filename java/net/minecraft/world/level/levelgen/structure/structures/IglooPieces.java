package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class IglooPieces {
   public static final int GENERATION_HEIGHT = 90;
   static final ResourceLocation STRUCTURE_LOCATION_IGLOO = ResourceLocation.withDefaultNamespace("igloo/top");
   private static final ResourceLocation STRUCTURE_LOCATION_LADDER = ResourceLocation.withDefaultNamespace("igloo/middle");
   private static final ResourceLocation STRUCTURE_LOCATION_LABORATORY = ResourceLocation.withDefaultNamespace("igloo/bottom");
   static final Map<ResourceLocation, BlockPos> PIVOTS = ImmutableMap.of(
      STRUCTURE_LOCATION_IGLOO, new BlockPos(3, 5, 5), STRUCTURE_LOCATION_LADDER, new BlockPos(1, 3, 1), STRUCTURE_LOCATION_LABORATORY, new BlockPos(3, 6, 7)
   );
   static final Map<ResourceLocation, BlockPos> OFFSETS = ImmutableMap.of(
      STRUCTURE_LOCATION_IGLOO, BlockPos.ZERO, STRUCTURE_LOCATION_LADDER, new BlockPos(2, -3, 4), STRUCTURE_LOCATION_LABORATORY, new BlockPos(0, -3, -2)
   );

   public static void addPieces(
      StructureTemplateManager pStructureTemplateManager, BlockPos pStartPos, Rotation pRotation, StructurePieceAccessor pPieces, RandomSource pRandom
   ) {
      if (pRandom.nextDouble() < 0.5) {
         int $$5 = pRandom.nextInt(8) + 4;
         pPieces.addPiece(new IglooPieces.IglooPiece(pStructureTemplateManager, STRUCTURE_LOCATION_LABORATORY, pStartPos, pRotation, $$5 * 3));

         for (int $$6 = 0; $$6 < $$5 - 1; $$6++) {
            pPieces.addPiece(new IglooPieces.IglooPiece(pStructureTemplateManager, STRUCTURE_LOCATION_LADDER, pStartPos, pRotation, $$6 * 3));
         }
      }

      pPieces.addPiece(new IglooPieces.IglooPiece(pStructureTemplateManager, STRUCTURE_LOCATION_IGLOO, pStartPos, pRotation, 0));
   }

   public static class IglooPiece extends TemplateStructurePiece {
      public IglooPiece(StructureTemplateManager pStructureTemplateManager, ResourceLocation pLocation, BlockPos pStartPos, Rotation pRotation, int pDown) {
         super(
            StructurePieceType.IGLOO,
            0,
            pStructureTemplateManager,
            pLocation,
            pLocation.toString(),
            makeSettings(pRotation, pLocation),
            makePosition(pLocation, pStartPos, pDown)
         );
      }

      public IglooPiece(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
         super(StructurePieceType.IGLOO, pTag, pStructureTemplateManager, p_227589_ -> makeSettings(Rotation.valueOf(pTag.getString("Rot")), p_227589_));
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation, ResourceLocation pLocation) {
         return new StructurePlaceSettings()
            .setRotation(pRotation)
            .setMirror(Mirror.NONE)
            .setRotationPivot(IglooPieces.PIVOTS.get(pLocation))
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
            .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
      }

      private static BlockPos makePosition(ResourceLocation pLocation, BlockPos pPos, int pDown) {
         return pPos.offset(IglooPieces.OFFSETS.get(pLocation)).below(pDown);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
      }

      @Override
      protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
         if ("chest".equals(pName)) {
            pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
            BlockEntity $$5 = pLevel.getBlockEntity(pPos.below());
            if ($$5 instanceof ChestBlockEntity) {
               ((ChestBlockEntity)$$5).setLootTable(BuiltInLootTables.IGLOO_CHEST, pRandom.nextLong());
            }
         }
      }

      @Override
      public void postProcess(
         WorldGenLevel pLevel,
         StructureManager pStructureManager,
         ChunkGenerator pGenerator,
         RandomSource pRandom,
         BoundingBox pBox,
         ChunkPos pChunkPos,
         BlockPos pPos
      ) {
         ResourceLocation $$7 = ResourceLocation.parse(this.templateName);
         StructurePlaceSettings $$8 = makeSettings(this.placeSettings.getRotation(), $$7);
         BlockPos $$9 = IglooPieces.OFFSETS.get($$7);
         BlockPos $$10 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition($$8, new BlockPos(3 - $$9.getX(), 0, -$$9.getZ())));
         int $$11 = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$10.getX(), $$10.getZ());
         BlockPos $$12 = this.templatePosition;
         this.templatePosition = this.templatePosition.offset(0, $$11 - 90 - 1, 0);
         super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
         if ($$7.equals(IglooPieces.STRUCTURE_LOCATION_IGLOO)) {
            BlockPos $$13 = this.templatePosition.offset(StructureTemplate.calculateRelativePosition($$8, new BlockPos(3, 0, 5)));
            BlockState $$14 = pLevel.getBlockState($$13.below());
            if (!$$14.isAir() && !$$14.is(Blocks.LADDER)) {
               pLevel.setBlock($$13, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
            }
         }

         this.templatePosition = $$12;
      }
   }
}
