package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class OceanRuinPieces {
   static final StructureProcessor WARM_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.SAND, Blocks.SUSPICIOUS_SAND, BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY
   );
   static final StructureProcessor COLD_SUSPICIOUS_BLOCK_PROCESSOR = archyRuleProcessor(
      Blocks.GRAVEL, Blocks.SUSPICIOUS_GRAVEL, BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY
   );
   private static final ResourceLocation[] WARM_RUINS = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/warm_8")
   };
   private static final ResourceLocation[] RUINS_BRICK = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/brick_8")
   };
   private static final ResourceLocation[] RUINS_CRACKED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/cracked_8")
   };
   private static final ResourceLocation[] RUINS_MOSSY = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_7"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_BRICK = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_brick_8")
   };
   private static final ResourceLocation[] BIG_RUINS_MOSSY = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_mossy_8")
   };
   private static final ResourceLocation[] BIG_RUINS_CRACKED = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_1"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_2"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_3"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_cracked_8")
   };
   private static final ResourceLocation[] BIG_WARM_RUINS = new ResourceLocation[]{
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_4"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_5"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_6"),
      ResourceLocation.withDefaultNamespace("underwater_ruin/big_warm_7")
   };

   private static StructureProcessor archyRuleProcessor(Block pBlock, Block pSuspiciousBlock, ResourceKey<LootTable> pLootTable) {
      return new CappedProcessor(
         new RuleProcessor(
            List.of(
               new ProcessorRule(
                  new BlockMatchTest(pBlock),
                  AlwaysTrueTest.INSTANCE,
                  PosAlwaysTrueTest.INSTANCE,
                  pSuspiciousBlock.defaultBlockState(),
                  new AppendLoot(pLootTable)
               )
            )
         ),
         ConstantInt.of(5)
      );
   }

   private static ResourceLocation getSmallWarmRuin(RandomSource pRandom) {
      return Util.getRandom(WARM_RUINS, pRandom);
   }

   private static ResourceLocation getBigWarmRuin(RandomSource pRandom) {
      return Util.getRandom(BIG_WARM_RUINS, pRandom);
   }

   public static void addPieces(
      StructureTemplateManager pStructureTemplateManager,
      BlockPos pPos,
      Rotation pRotation,
      StructurePieceAccessor pStructurePieceAccessor,
      RandomSource pRandom,
      OceanRuinStructure pStructure
   ) {
      boolean $$6 = pRandom.nextFloat() <= pStructure.largeProbability;
      float $$7 = $$6 ? 0.9F : 0.8F;
      addPiece(pStructureTemplateManager, pPos, pRotation, pStructurePieceAccessor, pRandom, pStructure, $$6, $$7);
      if ($$6 && pRandom.nextFloat() <= pStructure.clusterProbability) {
         addClusterRuins(pStructureTemplateManager, pRandom, pRotation, pPos, pStructure, pStructurePieceAccessor);
      }
   }

   private static void addClusterRuins(
      StructureTemplateManager pStructureTemplateManager,
      RandomSource pRandom,
      Rotation pRotation,
      BlockPos pPos,
      OceanRuinStructure pStructure,
      StructurePieceAccessor pStructurePieceAccessor
   ) {
      BlockPos $$6 = new BlockPos(pPos.getX(), 90, pPos.getZ());
      BlockPos $$7 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, pRotation, BlockPos.ZERO).offset($$6);
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      BlockPos $$9 = new BlockPos(Math.min($$6.getX(), $$7.getX()), $$6.getY(), Math.min($$6.getZ(), $$7.getZ()));
      List<BlockPos> $$10 = allPositions(pRandom, $$9);
      int $$11 = Mth.nextInt(pRandom, 4, 8);

      for (int $$12 = 0; $$12 < $$11; $$12++) {
         if (!$$10.isEmpty()) {
            int $$13 = pRandom.nextInt($$10.size());
            BlockPos $$14 = $$10.remove($$13);
            Rotation $$15 = Rotation.getRandom(pRandom);
            BlockPos $$16 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, $$15, BlockPos.ZERO).offset($$14);
            BoundingBox $$17 = BoundingBox.fromCorners($$14, $$16);
            if (!$$17.intersects($$8)) {
               addPiece(pStructureTemplateManager, $$14, $$15, pStructurePieceAccessor, pRandom, pStructure, false, 0.8F);
            }
         }
      }
   }

   private static List<BlockPos> allPositions(RandomSource pRandom, BlockPos pPos) {
      List<BlockPos> $$2 = Lists.newArrayList();
      $$2.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, 16 + Mth.nextInt(pRandom, 1, 7)));
      $$2.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, Mth.nextInt(pRandom, 1, 7)));
      $$2.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, -16 + Mth.nextInt(pRandom, 4, 8)));
      $$2.add(pPos.offset(Mth.nextInt(pRandom, 1, 7), 0, 16 + Mth.nextInt(pRandom, 1, 7)));
      $$2.add(pPos.offset(Mth.nextInt(pRandom, 1, 7), 0, -16 + Mth.nextInt(pRandom, 4, 6)));
      $$2.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, 16 + Mth.nextInt(pRandom, 3, 8)));
      $$2.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, Mth.nextInt(pRandom, 1, 7)));
      $$2.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, -16 + Mth.nextInt(pRandom, 4, 8)));
      return $$2;
   }

   private static void addPiece(
      StructureTemplateManager pStructureTemplateManager,
      BlockPos pPos,
      Rotation pRotation,
      StructurePieceAccessor pStructurePieceAccessor,
      RandomSource pRandom,
      OceanRuinStructure pStructure,
      boolean pIsLarge,
      float pIntegrity
   ) {
      switch (pStructure.biomeTemp) {
         case WARM:
         default:
            ResourceLocation $$8 = pIsLarge ? getBigWarmRuin(pRandom) : getSmallWarmRuin(pRandom);
            pStructurePieceAccessor.addPiece(
               new OceanRuinPieces.OceanRuinPiece(pStructureTemplateManager, $$8, pPos, pRotation, pIntegrity, pStructure.biomeTemp, pIsLarge)
            );
            break;
         case COLD:
            ResourceLocation[] $$9 = pIsLarge ? BIG_RUINS_BRICK : RUINS_BRICK;
            ResourceLocation[] $$10 = pIsLarge ? BIG_RUINS_CRACKED : RUINS_CRACKED;
            ResourceLocation[] $$11 = pIsLarge ? BIG_RUINS_MOSSY : RUINS_MOSSY;
            int $$12 = pRandom.nextInt($$9.length);
            pStructurePieceAccessor.addPiece(
               new OceanRuinPieces.OceanRuinPiece(pStructureTemplateManager, $$9[$$12], pPos, pRotation, pIntegrity, pStructure.biomeTemp, pIsLarge)
            );
            pStructurePieceAccessor.addPiece(
               new OceanRuinPieces.OceanRuinPiece(pStructureTemplateManager, $$10[$$12], pPos, pRotation, 0.7F, pStructure.biomeTemp, pIsLarge)
            );
            pStructurePieceAccessor.addPiece(
               new OceanRuinPieces.OceanRuinPiece(pStructureTemplateManager, $$11[$$12], pPos, pRotation, 0.5F, pStructure.biomeTemp, pIsLarge)
            );
      }
   }

   public static class OceanRuinPiece extends TemplateStructurePiece {
      private final OceanRuinStructure.Type biomeType;
      private final float integrity;
      private final boolean isLarge;

      public OceanRuinPiece(
         StructureTemplateManager pStructureTemplateManager,
         ResourceLocation pLocation,
         BlockPos pPos,
         Rotation pRotation,
         float pIntegrity,
         OceanRuinStructure.Type pBiomeType,
         boolean pIsLarge
      ) {
         super(
            StructurePieceType.OCEAN_RUIN, 0, pStructureTemplateManager, pLocation, pLocation.toString(), makeSettings(pRotation, pIntegrity, pBiomeType), pPos
         );
         this.integrity = pIntegrity;
         this.biomeType = pBiomeType;
         this.isLarge = pIsLarge;
      }

      private OceanRuinPiece(
         StructureTemplateManager pStructureTemplateManager,
         CompoundTag pGenDepth,
         Rotation pRotation,
         float pIntegrity,
         OceanRuinStructure.Type pBiomeType,
         boolean pIsLarge
      ) {
         super(StructurePieceType.OCEAN_RUIN, pGenDepth, pStructureTemplateManager, p_277332_ -> makeSettings(pRotation, pIntegrity, pBiomeType));
         this.integrity = pIntegrity;
         this.biomeType = pBiomeType;
         this.isLarge = pIsLarge;
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation, float pIntegrity, OceanRuinStructure.Type pStructureType) {
         StructureProcessor $$3 = pStructureType == OceanRuinStructure.Type.COLD
            ? OceanRuinPieces.COLD_SUSPICIOUS_BLOCK_PROCESSOR
            : OceanRuinPieces.WARM_SUSPICIOUS_BLOCK_PROCESSOR;
         return new StructurePlaceSettings()
            .setRotation(pRotation)
            .setMirror(Mirror.NONE)
            .addProcessor(new BlockRotProcessor(pIntegrity))
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
            .addProcessor($$3);
      }

      public static OceanRuinPieces.OceanRuinPiece create(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
         Rotation $$2 = Rotation.valueOf(pTag.getString("Rot"));
         float $$3 = pTag.getFloat("Integrity");
         OceanRuinStructure.Type $$4 = OceanRuinStructure.Type.valueOf(pTag.getString("BiomeType"));
         boolean $$5 = pTag.getBoolean("IsLarge");
         return new OceanRuinPieces.OceanRuinPiece(pStructureTemplateManager, pTag, $$2, $$3, $$4, $$5);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
         pTag.putFloat("Integrity", this.integrity);
         pTag.putString("BiomeType", this.biomeType.toString());
         pTag.putBoolean("IsLarge", this.isLarge);
      }

      @Override
      protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
         if ("chest".equals(pName)) {
            pLevel.setBlock(
               pPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, Boolean.valueOf(pLevel.getFluidState(pPos).is(FluidTags.WATER))), 2
            );
            BlockEntity $$5 = pLevel.getBlockEntity(pPos);
            if ($$5 instanceof ChestBlockEntity) {
               ((ChestBlockEntity)$$5)
                  .setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, pRandom.nextLong());
            }
         } else if ("drowned".equals(pName)) {
            Drowned $$6 = EntityType.DROWNED.create(pLevel.getLevel());
            if ($$6 != null) {
               $$6.setPersistenceRequired();
               $$6.moveTo(pPos, 0.0F, 0.0F);
               $$6.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(pPos), MobSpawnType.STRUCTURE, null);
               pLevel.addFreshEntityWithPassengers($$6);
               if (pPos.getY() > pLevel.getSeaLevel()) {
                  pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 2);
               } else {
                  pLevel.setBlock(pPos, Blocks.WATER.defaultBlockState(), 2);
               }
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
         int $$7 = pLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
         this.templatePosition = new BlockPos(this.templatePosition.getX(), $$7, this.templatePosition.getZ());
         BlockPos $$8 = StructureTemplate.transform(
               new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1),
               Mirror.NONE,
               this.placeSettings.getRotation(),
               BlockPos.ZERO
            )
            .offset(this.templatePosition);
         this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, pLevel, $$8), this.templatePosition.getZ());
         super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
      }

      private int getHeight(BlockPos pTemplatePos, BlockGetter pLevel, BlockPos pPos) {
         int $$3 = pTemplatePos.getY();
         int $$4 = 512;
         int $$5 = $$3 - 1;
         int $$6 = 0;

         for (BlockPos $$7 : BlockPos.betweenClosed(pTemplatePos, pPos)) {
            int $$8 = $$7.getX();
            int $$9 = $$7.getZ();
            int $$10 = pTemplatePos.getY() - 1;
            BlockPos.MutableBlockPos $$11 = new BlockPos.MutableBlockPos($$8, $$10, $$9);
            BlockState $$12 = pLevel.getBlockState($$11);

            for (FluidState $$13 = pLevel.getFluidState($$11);
               ($$12.isAir() || $$13.is(FluidTags.WATER) || $$12.is(BlockTags.ICE)) && $$10 > pLevel.getMinBuildHeight() + 1;
               $$13 = pLevel.getFluidState($$11)
            ) {
               $$11.set($$8, --$$10, $$9);
               $$12 = pLevel.getBlockState($$11);
            }

            $$4 = Math.min($$4, $$10);
            if ($$10 < $$5 - 2) {
               $$6++;
            }
         }

         int $$14 = Math.abs(pTemplatePos.getX() - pPos.getX());
         if ($$5 - $$4 > 2 && $$6 > $$14 - 2) {
            $$3 = $$4 + 1;
         }

         return $$3;
      }
   }
}
