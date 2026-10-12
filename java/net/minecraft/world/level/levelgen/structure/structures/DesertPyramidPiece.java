package net.minecraft.world.level.levelgen.structure.structures;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidPiece extends ScatteredFeaturePiece {
   public static final int WIDTH = 21;
   public static final int DEPTH = 21;
   private final boolean[] hasPlacedChest = new boolean[4];
   private final List<BlockPos> potentialSuspiciousSandWorldPositions = new ArrayList<>();
   private BlockPos randomCollapsedRoofPos = BlockPos.ZERO;

   public DesertPyramidPiece(RandomSource pRandom, int pX, int pZ) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, pX, 64, pZ, 21, 15, 21, getRandomHorizontalDirection(pRandom));
   }

   public DesertPyramidPiece(CompoundTag pTag) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, pTag);
      this.hasPlacedChest[0] = pTag.getBoolean("hasPlacedChest0");
      this.hasPlacedChest[1] = pTag.getBoolean("hasPlacedChest1");
      this.hasPlacedChest[2] = pTag.getBoolean("hasPlacedChest2");
      this.hasPlacedChest[3] = pTag.getBoolean("hasPlacedChest3");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      super.addAdditionalSaveData(pContext, pTag);
      pTag.putBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
      pTag.putBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
      pTag.putBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
      pTag.putBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
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
      if (this.updateHeightPositionToLowestGroundHeight(pLevel, -pRandom.nextInt(3))) {
         this.generateBox(
            pLevel, pBox, 0, -4, 0, this.width - 1, 0, this.depth - 1, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );

         for (int $$7 = 1; $$7 <= 9; $$7++) {
            this.generateBox(
               pLevel,
               pBox,
               $$7,
               $$7,
               $$7,
               this.width - 1 - $$7,
               $$7,
               this.depth - 1 - $$7,
               Blocks.SANDSTONE.defaultBlockState(),
               Blocks.SANDSTONE.defaultBlockState(),
               false
            );
            this.generateBox(
               pLevel,
               pBox,
               $$7 + 1,
               $$7,
               $$7 + 1,
               this.width - 2 - $$7,
               $$7,
               this.depth - 2 - $$7,
               Blocks.AIR.defaultBlockState(),
               Blocks.AIR.defaultBlockState(),
               false
            );
         }

         for (int $$8 = 0; $$8 < this.width; $$8++) {
            for (int $$9 = 0; $$9 < this.depth; $$9++) {
               int $$10 = -5;
               this.fillColumnDown(pLevel, Blocks.SANDSTONE.defaultBlockState(), $$8, -5, $$9, pBox);
            }
         }

         BlockState $$11 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState $$12 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState $$13 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState $$14 = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.placeBlock(pLevel, $$11, 2, 10, 0, pBox);
         this.placeBlock(pLevel, $$12, 2, 10, 4, pBox);
         this.placeBlock(pLevel, $$13, 0, 10, 2, pBox);
         this.placeBlock(pLevel, $$14, 4, 10, 2, pBox);
         this.generateBox(pLevel, pBox, this.width - 5, 0, 0, this.width - 1, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, this.width - 4, 10, 1, this.width - 2, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.placeBlock(pLevel, $$11, this.width - 3, 10, 0, pBox);
         this.placeBlock(pLevel, $$12, this.width - 3, 10, 4, pBox);
         this.placeBlock(pLevel, $$13, this.width - 5, 10, 2, pBox);
         this.placeBlock(pLevel, $$14, this.width - 1, 10, 2, pBox);
         this.generateBox(pLevel, pBox, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 1, 0, 11, 3, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 1, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 2, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 3, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, 3, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 3, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 2, 1, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 1, 1, pBox);
         this.generateBox(pLevel, pBox, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 12, 1, 2, 16, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, 5, 4, 5, this.width - 6, 4, this.depth - 6, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(pLevel, pBox, 9, 4, 9, 11, 4, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, this.width - 5, 1, 5, this.width - 2, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(pLevel, pBox, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, this.width - 7, 7, 9, this.width - 7, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(pLevel, pBox, 5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel,
            pBox,
            this.width - 6,
            5,
            9,
            this.width - 6,
            7,
            11,
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            false
         );
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 5, 5, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 5, 6, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 6, 6, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), this.width - 6, 5, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), this.width - 6, 6, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), this.width - 7, 6, 10, pBox);
         this.generateBox(pLevel, pBox, 2, 4, 4, 2, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, this.width - 3, 4, 4, this.width - 3, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(pLevel, $$11, 2, 4, 5, pBox);
         this.placeBlock(pLevel, $$11, 2, 3, 4, pBox);
         this.placeBlock(pLevel, $$11, this.width - 3, 4, 5, pBox);
         this.placeBlock(pLevel, $$11, this.width - 3, 3, 4, pBox);
         this.generateBox(pLevel, pBox, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, this.width - 3, 1, 3, this.width - 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.placeBlock(pLevel, Blocks.SANDSTONE.defaultBlockState(), 1, 1, 2, pBox);
         this.placeBlock(pLevel, Blocks.SANDSTONE.defaultBlockState(), this.width - 2, 1, 2, pBox);
         this.placeBlock(pLevel, Blocks.SANDSTONE_SLAB.defaultBlockState(), 1, 2, 2, pBox);
         this.placeBlock(pLevel, Blocks.SANDSTONE_SLAB.defaultBlockState(), this.width - 2, 2, 2, pBox);
         this.placeBlock(pLevel, $$14, 2, 1, 2, pBox);
         this.placeBlock(pLevel, $$13, this.width - 3, 1, 2, pBox);
         this.generateBox(pLevel, pBox, 4, 3, 5, 4, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, this.width - 5, 3, 5, this.width - 5, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(pLevel, pBox, 3, 1, 5, 4, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, this.width - 6, 1, 5, this.width - 5, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);

         for (int $$15 = 5; $$15 <= 17; $$15 += 2) {
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 4, 1, $$15, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 4, 2, $$15, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), this.width - 5, 1, $$15, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), this.width - 5, 2, $$15, pBox);
         }

         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 7, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 8, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 9, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 9, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 8, 0, 10, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 12, 0, 10, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 7, 0, 10, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 13, 0, 10, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 0, 11, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 0, 11, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 12, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 10, 0, 13, pBox);
         this.placeBlock(pLevel, Blocks.BLUE_TERRACOTTA.defaultBlockState(), 10, 0, 10, pBox);

         for (int $$16 = 0; $$16 <= this.width - 1; $$16 += this.width - 1) {
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 2, 1, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 2, 2, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 2, 3, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 3, 1, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 3, 2, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 3, 3, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 4, 1, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$16, 4, 2, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 4, 3, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 5, 1, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 5, 2, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 5, 3, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 6, 1, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$16, 6, 2, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 6, 3, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 1, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 2, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$16, 7, 3, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 1, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 2, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$16, 8, 3, pBox);
         }

         for (int $$17 = 2; $$17 <= this.width - 3; $$17 += this.width - 3 - 2) {
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 2, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 2, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 2, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 3, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 3, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 3, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 4, 0, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$17, 4, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 4, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 5, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 5, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 5, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 6, 0, pBox);
            this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), $$17, 6, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 6, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 - 1, 7, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17, 7, 0, pBox);
            this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), $$17 + 1, 7, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 - 1, 8, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17, 8, 0, pBox);
            this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), $$17 + 1, 8, 0, pBox);
         }

         this.generateBox(pLevel, pBox, 8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 8, 6, 0, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 12, 6, 0, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 9, 5, 0, pBox);
         this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, 5, 0, pBox);
         this.placeBlock(pLevel, Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 11, 5, 0, pBox);
         this.generateBox(pLevel, pBox, 8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            pLevel, pBox, 8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(pLevel, pBox, 8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, -11, 9, 11, -1, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 10, -11, 10, pBox);
         this.generateBox(pLevel, pBox, 9, -13, 9, 11, -13, 11, Blocks.TNT.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 8, -11, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 8, -10, 10, pBox);
         this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 7, -10, 10, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 7, -11, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 12, -11, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 12, -10, 10, pBox);
         this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 13, -10, 10, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 13, -11, 10, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 10, -11, 8, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 10, -10, 8, pBox);
         this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 7, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 7, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 10, -11, 12, pBox);
         this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), 10, -10, 12, pBox);
         this.placeBlock(pLevel, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 13, pBox);
         this.placeBlock(pLevel, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 13, pBox);

         for (Direction $$18 : Direction.Plane.HORIZONTAL) {
            if (!this.hasPlacedChest[$$18.get2DDataValue()]) {
               int $$19 = $$18.getStepX() * 2;
               int $$20 = $$18.getStepZ() * 2;
               this.hasPlacedChest[$$18.get2DDataValue()] = this.createChest(pLevel, pBox, pRandom, 10 + $$19, -11, 10 + $$20, BuiltInLootTables.DESERT_PYRAMID);
            }
         }

         this.addCellar(pLevel, pBox);
      }
   }

   private void addCellar(WorldGenLevel pLevel, BoundingBox pBox) {
      BlockPos $$2 = new BlockPos(16, -4, 13);
      this.addCellarStairs($$2, pLevel, pBox);
      this.addCellarRoom($$2, pLevel, pBox);
   }

   private void addCellarStairs(BlockPos pPos, WorldGenLevel pLevel, BoundingBox pBox) {
      int $$3 = pPos.getX();
      int $$4 = pPos.getY();
      int $$5 = pPos.getZ();
      BlockState $$6 = Blocks.SANDSTONE_STAIRS.defaultBlockState();
      this.placeBlock(pLevel, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 13, -1, 17, pBox);
      this.placeBlock(pLevel, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 14, -2, 17, pBox);
      this.placeBlock(pLevel, $$6.rotate(Rotation.COUNTERCLOCKWISE_90), 15, -3, 17, pBox);
      BlockState $$7 = Blocks.SAND.defaultBlockState();
      BlockState $$8 = Blocks.SANDSTONE.defaultBlockState();
      boolean $$9 = pLevel.getRandom().nextBoolean();
      this.placeBlock(pLevel, $$7, $$3 - 4, $$4 + 4, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 3, $$4 + 4, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 2, $$4 + 4, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 1, $$4 + 4, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3, $$4 + 4, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 2, $$4 + 3, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$9 ? $$7 : $$8, $$3 - 1, $$4 + 3, $$5 + 4, pBox);
      this.placeBlock(pLevel, !$$9 ? $$7 : $$8, $$3, $$4 + 3, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 1, $$4 + 2, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$8, $$3, $$4 + 2, $$5 + 4, pBox);
      this.placeBlock(pLevel, $$7, $$3, $$4 + 1, $$5 + 4, pBox);
   }

   private void addCellarRoom(BlockPos pPos, WorldGenLevel pLevel, BoundingBox pBox) {
      int $$3 = pPos.getX();
      int $$4 = pPos.getY();
      int $$5 = pPos.getZ();
      BlockState $$6 = Blocks.CUT_SANDSTONE.defaultBlockState();
      BlockState $$7 = Blocks.CHISELED_SANDSTONE.defaultBlockState();
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 1, $$5 - 3, $$3 - 3, $$4 + 1, $$5 + 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 + 3, $$4 + 1, $$5 - 3, $$3 + 3, $$4 + 1, $$5 + 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 1, $$5 - 3, $$3 + 3, $$4 + 1, $$5 - 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 1, $$5 + 3, $$3 + 3, $$4 + 1, $$5 + 3, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 2, $$5 - 3, $$3 - 3, $$4 + 2, $$5 + 2, $$7, $$7, true);
      this.generateBox(pLevel, pBox, $$3 + 3, $$4 + 2, $$5 - 3, $$3 + 3, $$4 + 2, $$5 + 2, $$7, $$7, true);
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 2, $$5 - 3, $$3 + 3, $$4 + 2, $$5 - 2, $$7, $$7, true);
      this.generateBox(pLevel, pBox, $$3 - 3, $$4 + 2, $$5 + 3, $$3 + 3, $$4 + 2, $$5 + 3, $$7, $$7, true);
      this.generateBox(pLevel, pBox, $$3 - 3, -1, $$5 - 3, $$3 - 3, -1, $$5 + 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 + 3, -1, $$5 - 3, $$3 + 3, -1, $$5 + 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 - 3, -1, $$5 - 3, $$3 + 3, -1, $$5 - 2, $$6, $$6, true);
      this.generateBox(pLevel, pBox, $$3 - 3, -1, $$5 + 3, $$3 + 3, -1, $$5 + 3, $$6, $$6, true);
      this.placeSandBox($$3 - 2, $$4 + 1, $$5 - 2, $$3 + 2, $$4 + 3, $$5 + 2);
      this.placeCollapsedRoof(pLevel, pBox, $$3 - 2, $$4 + 4, $$5 - 2, $$3 + 2, $$5 + 2);
      BlockState $$8 = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      BlockState $$9 = Blocks.BLUE_TERRACOTTA.defaultBlockState();
      this.placeBlock(pLevel, $$9, $$3, $$4, $$5, pBox);
      this.placeBlock(pLevel, $$8, $$3 + 1, $$4, $$5 - 1, pBox);
      this.placeBlock(pLevel, $$8, $$3 + 1, $$4, $$5 + 1, pBox);
      this.placeBlock(pLevel, $$8, $$3 - 1, $$4, $$5 - 1, pBox);
      this.placeBlock(pLevel, $$8, $$3 - 1, $$4, $$5 + 1, pBox);
      this.placeBlock(pLevel, $$8, $$3 + 2, $$4, $$5, pBox);
      this.placeBlock(pLevel, $$8, $$3 - 2, $$4, $$5, pBox);
      this.placeBlock(pLevel, $$8, $$3, $$4, $$5 + 2, pBox);
      this.placeBlock(pLevel, $$8, $$3, $$4, $$5 - 2, pBox);
      this.placeBlock(pLevel, $$8, $$3 + 3, $$4, $$5, pBox);
      this.placeSand($$3 + 3, $$4 + 1, $$5);
      this.placeSand($$3 + 3, $$4 + 2, $$5);
      this.placeBlock(pLevel, $$6, $$3 + 4, $$4 + 1, $$5, pBox);
      this.placeBlock(pLevel, $$7, $$3 + 4, $$4 + 2, $$5, pBox);
      this.placeBlock(pLevel, $$8, $$3 - 3, $$4, $$5, pBox);
      this.placeSand($$3 - 3, $$4 + 1, $$5);
      this.placeSand($$3 - 3, $$4 + 2, $$5);
      this.placeBlock(pLevel, $$6, $$3 - 4, $$4 + 1, $$5, pBox);
      this.placeBlock(pLevel, $$7, $$3 - 4, $$4 + 2, $$5, pBox);
      this.placeBlock(pLevel, $$8, $$3, $$4, $$5 + 3, pBox);
      this.placeSand($$3, $$4 + 1, $$5 + 3);
      this.placeSand($$3, $$4 + 2, $$5 + 3);
      this.placeBlock(pLevel, $$8, $$3, $$4, $$5 - 3, pBox);
      this.placeSand($$3, $$4 + 1, $$5 - 3);
      this.placeSand($$3, $$4 + 2, $$5 - 3);
      this.placeBlock(pLevel, $$6, $$3, $$4 + 1, $$5 - 4, pBox);
      this.placeBlock(pLevel, $$7, $$3, -2, $$5 - 4, pBox);
   }

   private void placeSand(int pX, int pY, int pZ) {
      BlockPos $$3 = this.getWorldPos(pX, pY, pZ);
      this.potentialSuspiciousSandWorldPositions.add($$3);
   }

   private void placeSandBox(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
      for (int $$6 = pMinY; $$6 <= pMaxY; $$6++) {
         for (int $$7 = pMinX; $$7 <= pMaxX; $$7++) {
            for (int $$8 = pMinZ; $$8 <= pMaxZ; $$8++) {
               this.placeSand($$7, $$6, $$8);
            }
         }
      }
   }

   private void placeCollapsedRoofPiece(WorldGenLevel pLevel, int pX, int pY, int pZ, BoundingBox pBox) {
      if (pLevel.getRandom().nextFloat() < 0.33F) {
         BlockState $$5 = Blocks.SANDSTONE.defaultBlockState();
         this.placeBlock(pLevel, $$5, pX, pY, pZ, pBox);
      } else {
         BlockState $$6 = Blocks.SAND.defaultBlockState();
         this.placeBlock(pLevel, $$6, pX, pY, pZ, pBox);
      }
   }

   private void placeCollapsedRoof(WorldGenLevel pLevel, BoundingBox pBox, int pMinX, int pY, int pMinZ, int pMaxX, int pMaxZ) {
      for (int $$7 = pMinX; $$7 <= pMaxX; $$7++) {
         for (int $$8 = pMinZ; $$8 <= pMaxZ; $$8++) {
            this.placeCollapsedRoofPiece(pLevel, $$7, pY, $$8, pBox);
         }
      }

      RandomSource $$9 = RandomSource.create(pLevel.getSeed()).forkPositional().at(this.getWorldPos(pMinX, pY, pMinZ));
      int $$10 = $$9.nextIntBetweenInclusive(pMinX, pMaxX);
      int $$11 = $$9.nextIntBetweenInclusive(pMinZ, pMaxZ);
      this.randomCollapsedRoofPos = new BlockPos(this.getWorldX($$10, $$11), this.getWorldY(pY), this.getWorldZ($$10, $$11));
   }

   public List<BlockPos> getPotentialSuspiciousSandWorldPositions() {
      return this.potentialSuspiciousSandWorldPositions;
   }

   public BlockPos getRandomCollapsedRoofPos() {
      return this.randomCollapsedRoofPos;
   }
}
