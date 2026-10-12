package net.minecraft.world.level.levelgen.structure.structures;

import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class ShipwreckPieces {
   private static final int NUMBER_OF_BLOCKS_ALLOWED_IN_WORLD_GEN_REGION = 32;
   static final BlockPos PIVOT = new BlockPos(4, 0, 15);
   private static final ResourceLocation[] STRUCTURE_LOCATION_BEACHED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   private static final ResourceLocation[] STRUCTURE_LOCATION_OCEAN = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf"),
      ResourceLocation.withDefaultNamespace("shipwreck/with_mast_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/upsidedown_backhalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/sideways_backhalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_full_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_fronthalf_degraded"),
      ResourceLocation.withDefaultNamespace("shipwreck/rightsideup_backhalf_degraded")
   };
   static final Map<String, ResourceKey<LootTable>> MARKERS_TO_LOOT = Map.of(
      "map_chest", BuiltInLootTables.SHIPWRECK_MAP, "treasure_chest", BuiltInLootTables.SHIPWRECK_TREASURE, "supply_chest", BuiltInLootTables.SHIPWRECK_SUPPLY
   );

   public static ShipwreckPieces.ShipwreckPiece addRandomPiece(
      StructureTemplateManager pStructureTemplateManager,
      BlockPos pPos,
      Rotation pRotation,
      StructurePieceAccessor pPieces,
      RandomSource pRandom,
      boolean pIsBeached
   ) {
      ResourceLocation $$6 = Util.getRandom(pIsBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, pRandom);
      ShipwreckPieces.ShipwreckPiece $$7 = new ShipwreckPieces.ShipwreckPiece(pStructureTemplateManager, $$6, pPos, pRotation, pIsBeached);
      pPieces.addPiece($$7);
      return $$7;
   }

   public static class ShipwreckPiece extends TemplateStructurePiece {
      private final boolean isBeached;

      public ShipwreckPiece(
         StructureTemplateManager pStructureTemplateManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation, boolean pIsBeached
      ) {
         super(StructurePieceType.SHIPWRECK_PIECE, 0, pStructureTemplateManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
         this.isBeached = pIsBeached;
      }

      public ShipwreckPiece(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
         super(StructurePieceType.SHIPWRECK_PIECE, pTag, pStructureTemplateManager, p_229383_ -> makeSettings(Rotation.valueOf(pTag.getString("Rot"))));
         this.isBeached = pTag.getBoolean("isBeached");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("isBeached", this.isBeached);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation) {
         return new StructurePlaceSettings()
            .setRotation(pRotation)
            .setMirror(Mirror.NONE)
            .setRotationPivot(ShipwreckPieces.PIVOT)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      @Override
      protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
         ResourceKey<LootTable> $$5 = ShipwreckPieces.MARKERS_TO_LOOT.get(pName);
         if ($$5 != null) {
            RandomizableContainer.setBlockEntityLootTable(pLevel, pRandom, pPos.below(), $$5);
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
         if (this.isTooBigToFitInWorldGenRegion()) {
            super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
         } else {
            int $$7 = pLevel.getMaxBuildHeight();
            int $$8 = 0;
            Vec3i $$9 = this.template.getSize();
            Heightmap.Types $$10 = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
            int $$11 = $$9.getX() * $$9.getZ();
            if ($$11 == 0) {
               $$8 = pLevel.getHeight($$10, this.templatePosition.getX(), this.templatePosition.getZ());
            } else {
               BlockPos $$12 = this.templatePosition.offset($$9.getX() - 1, 0, $$9.getZ() - 1);

               for (BlockPos $$13 : BlockPos.betweenClosed(this.templatePosition, $$12)) {
                  int $$14 = pLevel.getHeight($$10, $$13.getX(), $$13.getZ());
                  $$8 += $$14;
                  $$7 = Math.min($$7, $$14);
               }

               $$8 /= $$11;
            }

            this.adjustPositionHeight(this.isBeached ? this.calculateBeachedPosition($$7, pRandom) : $$8);
            super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
         }
      }

      public boolean isTooBigToFitInWorldGenRegion() {
         Vec3i $$0 = this.template.getSize();
         return $$0.getX() > 32 || $$0.getY() > 32;
      }

      public int calculateBeachedPosition(int pMaxHeight, RandomSource pRandom) {
         return pMaxHeight - this.template.getSize().getY() / 2 - pRandom.nextInt(3);
      }

      public void adjustPositionHeight(int pHeight) {
         this.templatePosition = new BlockPos(this.templatePosition.getX(), pHeight, this.templatePosition.getZ());
      }
   }
}
