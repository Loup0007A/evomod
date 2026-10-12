package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class StrongholdPieces {
   private static final int SMALL_DOOR_WIDTH = 3;
   private static final int SMALL_DOOR_HEIGHT = 3;
   private static final int MAX_DEPTH = 50;
   private static final int LOWEST_Y_POSITION = 10;
   private static final boolean CHECK_AIR = true;
   public static final int MAGIC_START_Y = 64;
   private static final StrongholdPieces.PieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.PieceWeight[]{
      new StrongholdPieces.PieceWeight(StrongholdPieces.Straight.class, 40, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.PrisonHall.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.LeftTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RightTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RoomCrossing.class, 10, 6),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StraightStairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.FiveCrossing.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.ChestCorridor.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.Library.class, 10, 2) {
         @Override
         public boolean doPlace(int p_229450_) {
            return super.doPlace(p_229450_) && p_229450_ > 4;
         }
      },
      new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
         @Override
         public boolean doPlace(int p_229456_) {
            return super.doPlace(p_229456_) && p_229456_ > 5;
         }
      }
   };
   private static List<StrongholdPieces.PieceWeight> currentPieces;
   static Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
   private static int totalWeight;
   static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

   public static void resetPieces() {
      currentPieces = Lists.newArrayList();

      for (StrongholdPieces.PieceWeight $$0 : STRONGHOLD_PIECE_WEIGHTS) {
         $$0.placeCount = 0;
         currentPieces.add($$0);
      }

      imposedPiece = null;
   }

   private static boolean updatePieceWeight() {
      boolean $$0 = false;
      totalWeight = 0;

      for (StrongholdPieces.PieceWeight $$1 : currentPieces) {
         if ($$1.maxPlaceCount > 0 && $$1.placeCount < $$1.maxPlaceCount) {
            $$0 = true;
         }

         totalWeight = totalWeight + $$1.weight;
      }

      return $$0;
   }

   private static StrongholdPieces.StrongholdPiece findAndCreatePieceFactory(
      Class<? extends StrongholdPieces.StrongholdPiece> pPieceClass,
      StructurePieceAccessor pPieces,
      RandomSource pRandom,
      int pX,
      int pY,
      int pZ,
      @Nullable Direction pDirection,
      int pGenDepth
   ) {
      StrongholdPieces.StrongholdPiece $$8 = null;
      if (pPieceClass == StrongholdPieces.Straight.class) {
         $$8 = StrongholdPieces.Straight.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.PrisonHall.class) {
         $$8 = StrongholdPieces.PrisonHall.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.LeftTurn.class) {
         $$8 = StrongholdPieces.LeftTurn.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.RightTurn.class) {
         $$8 = StrongholdPieces.RightTurn.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.RoomCrossing.class) {
         $$8 = StrongholdPieces.RoomCrossing.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.StraightStairsDown.class) {
         $$8 = StrongholdPieces.StraightStairsDown.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.StairsDown.class) {
         $$8 = StrongholdPieces.StairsDown.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.FiveCrossing.class) {
         $$8 = StrongholdPieces.FiveCrossing.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.ChestCorridor.class) {
         $$8 = StrongholdPieces.ChestCorridor.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.Library.class) {
         $$8 = StrongholdPieces.Library.createPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
      } else if (pPieceClass == StrongholdPieces.PortalRoom.class) {
         $$8 = StrongholdPieces.PortalRoom.createPiece(pPieces, pX, pY, pZ, pDirection, pGenDepth);
      }

      return $$8;
   }

   private static StrongholdPieces.StrongholdPiece generatePieceFromSmallDoor(
      StrongholdPieces.StartPiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection, int pGenDepth
   ) {
      if (!updatePieceWeight()) {
         return null;
      } else {
         if (imposedPiece != null) {
            StrongholdPieces.StrongholdPiece $$8 = findAndCreatePieceFactory(imposedPiece, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
            imposedPiece = null;
            if ($$8 != null) {
               return $$8;
            }
         }

         int $$9 = 0;

         while ($$9 < 5) {
            $$9++;
            int $$10 = pRandom.nextInt(totalWeight);

            for (StrongholdPieces.PieceWeight $$11 : currentPieces) {
               $$10 -= $$11.weight;
               if ($$10 < 0) {
                  if (!$$11.doPlace(pGenDepth) || $$11 == pPiece.previousPiece) {
                     break;
                  }

                  StrongholdPieces.StrongholdPiece $$12 = findAndCreatePieceFactory($$11.pieceClass, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth);
                  if ($$12 != null) {
                     $$11.placeCount++;
                     pPiece.previousPiece = $$11;
                     if (!$$11.isValid()) {
                        currentPieces.remove($$11);
                     }

                     return $$12;
                  }
               }
            }
         }

         BoundingBox $$13 = StrongholdPieces.FillerCorridor.findPieceBox(pPieces, pRandom, pX, pY, pZ, pDirection);
         return $$13 != null && $$13.minY() > 1 ? new StrongholdPieces.FillerCorridor(pGenDepth, $$13, pDirection) : null;
      }
   }

   static StructurePiece generateAndAddPiece(
      StrongholdPieces.StartPiece pPiece,
      StructurePieceAccessor pPieces,
      RandomSource pRandom,
      int pX,
      int pY,
      int pZ,
      @Nullable Direction pDirection,
      int pGenDepth
   ) {
      if (pGenDepth > 50) {
         return null;
      } else if (Math.abs(pX - pPiece.getBoundingBox().minX()) <= 112 && Math.abs(pZ - pPiece.getBoundingBox().minZ()) <= 112) {
         StructurePiece $$8 = generatePieceFromSmallDoor(pPiece, pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth + 1);
         if ($$8 != null) {
            pPieces.addPiece($$8);
            pPiece.pendingChildren.add($$8);
         }

         return $$8;
      } else {
         return null;
      }
   }

   public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private boolean hasPlacedChest;

      public ChestCorridor(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public ChestCorridor(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, pTag);
         this.hasPlacedChest = pTag.getBoolean("Chest");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Chest", this.hasPlacedChest);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
      }

      public static StrongholdPieces.ChestCorridor createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 7, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.ChestCorridor(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 4, 6, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(pLevel, pRandom, pBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         this.generateBox(pLevel, pBox, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, pBox);

         for (int $$7 = 2; $$7 <= 4; $$7++) {
            this.placeBlock(pLevel, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, $$7, pBox);
         }

         if (!this.hasPlacedChest && pBox.isInside(this.getWorldPos(3, 2, 3))) {
            this.hasPlacedChest = true;
            this.createChest(pLevel, pBox, pRandom, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
         }
      }
   }

   public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
      private final int steps;

      public FillerCorridor(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.steps = pOrientation != Direction.NORTH && pOrientation != Direction.SOUTH ? pBox.getXSpan() : pBox.getZSpan();
      }

      public FillerCorridor(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, pTag);
         this.steps = pTag.getInt("Steps");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putInt("Steps", this.steps);
      }

      public static BoundingBox findPieceBox(StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation) {
         int $$6 = 3;
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 4, pOrientation);
         StructurePiece $$8 = pPieces.findCollisionPiece($$7);
         if ($$8 == null) {
            return null;
         } else {
            if ($$8.getBoundingBox().minY() == $$7.minY()) {
               for (int $$9 = 2; $$9 >= 1; $$9--) {
                  $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, $$9, pOrientation);
                  if (!$$8.getBoundingBox().intersects($$7)) {
                     return BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, $$9 + 1, pOrientation);
                  }
               }
            }

            return null;
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
         for (int $$7 = 0; $$7 < this.steps; $$7++) {
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, $$7, pBox);

            for (int $$8 = 1; $$8 <= 3; $$8++) {
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 0, $$8, $$7, pBox);
               this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), 1, $$8, $$7, pBox);
               this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), 2, $$8, $$7, pBox);
               this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), 3, $$8, $$7, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 4, $$8, $$7, pBox);
            }

            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, $$7, pBox);
            this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, $$7, pBox);
         }
      }
   }

   public static class FiveCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 10;
      protected static final int HEIGHT = 9;
      protected static final int DEPTH = 11;
      private final boolean leftLow;
      private final boolean leftHigh;
      private final boolean rightLow;
      private final boolean rightHigh;

      public FiveCrossing(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
         this.leftLow = pRandom.nextBoolean();
         this.leftHigh = pRandom.nextBoolean();
         this.rightLow = pRandom.nextBoolean();
         this.rightHigh = pRandom.nextInt(3) > 0;
      }

      public FiveCrossing(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, pTag);
         this.leftLow = pTag.getBoolean("leftLow");
         this.leftHigh = pTag.getBoolean("leftHigh");
         this.rightLow = pTag.getBoolean("rightLow");
         this.rightHigh = pTag.getBoolean("rightHigh");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("leftLow", this.leftLow);
         pTag.putBoolean("leftHigh", this.leftHigh);
         pTag.putBoolean("rightLow", this.rightLow);
         pTag.putBoolean("rightHigh", this.rightHigh);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = 3;
         int $$4 = 5;
         Direction $$5 = this.getOrientation();
         if ($$5 == Direction.WEST || $$5 == Direction.NORTH) {
            $$3 = 8 - $$3;
            $$4 = 8 - $$4;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 5, 1);
         if (this.leftLow) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, $$3, 1);
         }

         if (this.leftHigh) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, $$4, 7);
         }

         if (this.rightLow) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, $$3, 1);
         }

         if (this.rightHigh) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, $$4, 7);
         }
      }

      public static StrongholdPieces.FiveCrossing createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -4, -3, 0, 10, 9, 11, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.FiveCrossing(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 9, 8, 10, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 4, 3, 0);
         if (this.leftLow) {
            this.generateBox(pLevel, pBox, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightLow) {
            this.generateBox(pLevel, pBox, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.leftHigh) {
            this.generateBox(pLevel, pBox, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightHigh) {
            this.generateBox(pLevel, pBox, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         this.generateBox(pLevel, pBox, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(pLevel, pBox, 1, 2, 1, 8, 2, 6, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 1, 5, 4, 4, 9, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 8, 1, 5, 8, 4, 9, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 1, 4, 7, 3, 4, 9, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 1, 3, 5, 3, 3, 6, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 1, 7, 7, 1, 8, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(
            pLevel,
            pBox,
            5,
            5,
            7,
            7,
            5,
            9,
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            false
         );
         this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, pBox);
      }
   }

   public static class LeftTurn extends StrongholdPieces.Turn {
      public LeftTurn(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public LeftTurn(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         Direction $$3 = this.getOrientation();
         if ($$3 != Direction.NORTH && $$3 != Direction.EAST) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
         } else {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
         }
      }

      public static StrongholdPieces.LeftTurn createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 5, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.LeftTurn(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 4, 4, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 1, 0);
         Direction $$7 = this.getOrientation();
         if ($$7 != Direction.NORTH && $$7 != Direction.EAST) {
            this.generateBox(pLevel, pBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(pLevel, pBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class Library extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 14;
      protected static final int HEIGHT = 6;
      protected static final int TALL_HEIGHT = 11;
      protected static final int DEPTH = 15;
      private final boolean isTall;

      public Library(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
         this.isTall = pBox.getYSpan() > 6;
      }

      public Library(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, pTag);
         this.isTall = pTag.getBoolean("Tall");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Tall", this.isTall);
      }

      public static StrongholdPieces.Library createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 14, 11, 15, pOrientation);
         if (!isOkBox($$7) || pPieces.findCollisionPiece($$7) != null) {
            $$7 = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 14, 6, 15, pOrientation);
            if (!isOkBox($$7) || pPieces.findCollisionPiece($$7) != null) {
               return null;
            }
         }

         return new StrongholdPieces.Library(pGenDepth, pRandom, $$7, pOrientation);
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
         int $$7 = 11;
         if (!this.isTall) {
            $$7 = 6;
         }

         this.generateBox(pLevel, pBox, 0, 0, 0, 13, $$7 - 1, 14, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 4, 1, 0);
         this.generateMaybeBox(
            pLevel, pBox, pRandom, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false
         );
         int $$8 = 1;
         int $$9 = 12;

         for (int $$10 = 1; $$10 <= 13; $$10++) {
            if (($$10 - 1) % 4 == 0) {
               this.generateBox(pLevel, pBox, 1, 1, $$10, 1, 4, $$10, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.generateBox(pLevel, pBox, 12, 1, $$10, 12, 4, $$10, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, $$10, pBox);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, $$10, pBox);
               if (this.isTall) {
                  this.generateBox(pLevel, pBox, 1, 6, $$10, 1, 9, $$10, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                  this.generateBox(pLevel, pBox, 12, 6, $$10, 12, 9, $$10, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               }
            } else {
               this.generateBox(pLevel, pBox, 1, 1, $$10, 1, 4, $$10, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               this.generateBox(pLevel, pBox, 12, 1, $$10, 12, 4, $$10, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               if (this.isTall) {
                  this.generateBox(pLevel, pBox, 1, 6, $$10, 1, 9, $$10, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                  this.generateBox(pLevel, pBox, 12, 6, $$10, 12, 9, $$10, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               }
            }
         }

         for (int $$11 = 3; $$11 < 12; $$11 += 2) {
            this.generateBox(pLevel, pBox, 3, 1, $$11, 4, 3, $$11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 6, 1, $$11, 7, 3, $$11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 9, 1, $$11, 10, 3, $$11, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
         }

         if (this.isTall) {
            this.generateBox(pLevel, pBox, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, pBox);
            this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, pBox);
            this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, pBox);
            BlockState $$12 = Blocks.OAK_FENCE
               .defaultBlockState()
               .setValue(FenceBlock.WEST, Boolean.valueOf(true))
               .setValue(FenceBlock.EAST, Boolean.valueOf(true));
            BlockState $$13 = Blocks.OAK_FENCE
               .defaultBlockState()
               .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
               .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
            this.generateBox(pLevel, pBox, 3, 6, 3, 3, 6, 11, $$13, $$13, false);
            this.generateBox(pLevel, pBox, 10, 6, 3, 10, 6, 9, $$13, $$13, false);
            this.generateBox(pLevel, pBox, 4, 6, 2, 9, 6, 2, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 4, 6, 12, 7, 6, 12, $$12, $$12, false);
            this.placeBlock(
               pLevel,
               Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
               3,
               6,
               2,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
               3,
               6,
               12,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
               10,
               6,
               2,
               pBox
            );

            for (int $$14 = 0; $$14 <= 2; $$14++) {
               this.placeBlock(
                  pLevel,
                  Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
                  8 + $$14,
                  6,
                  12 - $$14,
                  pBox
               );
               if ($$14 != 2) {
                  this.placeBlock(
                     pLevel,
                     Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
                     8 + $$14,
                     6,
                     11 - $$14,
                     pBox
                  );
               }
            }

            BlockState $$15 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            this.placeBlock(pLevel, $$15, 10, 1, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 2, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 3, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 4, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 5, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 6, 13, pBox);
            this.placeBlock(pLevel, $$15, 10, 7, 13, pBox);
            int $$16 = 7;
            int $$17 = 7;
            BlockState $$18 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true));
            this.placeBlock(pLevel, $$18, 6, 9, 7, pBox);
            BlockState $$19 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true));
            this.placeBlock(pLevel, $$19, 7, 9, 7, pBox);
            this.placeBlock(pLevel, $$18, 6, 8, 7, pBox);
            this.placeBlock(pLevel, $$19, 7, 8, 7, pBox);
            BlockState $$20 = $$13.setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true));
            this.placeBlock(pLevel, $$20, 6, 7, 7, pBox);
            this.placeBlock(pLevel, $$20, 7, 7, 7, pBox);
            this.placeBlock(pLevel, $$18, 5, 7, 7, pBox);
            this.placeBlock(pLevel, $$19, 8, 7, 7, pBox);
            this.placeBlock(pLevel, $$18.setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 6, 7, 6, pBox);
            this.placeBlock(pLevel, $$18.setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 6, 7, 8, pBox);
            this.placeBlock(pLevel, $$19.setValue(FenceBlock.NORTH, Boolean.valueOf(true)), 7, 7, 6, pBox);
            this.placeBlock(pLevel, $$19.setValue(FenceBlock.SOUTH, Boolean.valueOf(true)), 7, 7, 8, pBox);
            BlockState $$21 = Blocks.TORCH.defaultBlockState();
            this.placeBlock(pLevel, $$21, 5, 8, 7, pBox);
            this.placeBlock(pLevel, $$21, 8, 8, 7, pBox);
            this.placeBlock(pLevel, $$21, 6, 8, 6, pBox);
            this.placeBlock(pLevel, $$21, 6, 8, 8, pBox);
            this.placeBlock(pLevel, $$21, 7, 8, 6, pBox);
            this.placeBlock(pLevel, $$21, 7, 8, 8, pBox);
         }

         this.createChest(pLevel, pBox, pRandom, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
         if (this.isTall) {
            this.placeBlock(pLevel, CAVE_AIR, 12, 9, 1, pBox);
            this.createChest(pLevel, pBox, pRandom, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
         }
      }
   }

   static class PieceWeight {
      public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;

      public PieceWeight(Class<? extends StrongholdPieces.StrongholdPiece> pPieceClass, int pWeight, int pMaxPlaceCount) {
         this.pieceClass = pPieceClass;
         this.weight = pWeight;
         this.maxPlaceCount = pMaxPlaceCount;
      }

      public boolean doPlace(int pGenDepth) {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }

      public boolean isValid() {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }
   }

   public static class PortalRoom extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 8;
      protected static final int DEPTH = 16;
      private boolean hasPlacedSpawner;

      public PortalRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public PortalRoom(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, pTag);
         this.hasPlacedSpawner = pTag.getBoolean("Mob");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Mob", this.hasPlacedSpawner);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         if (pPiece != null) {
            ((StrongholdPieces.StartPiece)pPiece).portalRoomPiece = this;
         }
      }

      public static StrongholdPieces.PortalRoom createPiece(StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 11, 8, 16, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new StrongholdPieces.PortalRoom(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 10, 7, 15, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
         int $$7 = 6;
         this.generateBox(pLevel, pBox, 1, 6, 1, 1, 6, 14, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 9, 6, 1, 9, 6, 14, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 2, 6, 1, 8, 6, 2, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 2, 6, 14, 8, 6, 14, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 1, 1, 1, 2, 1, 4, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 8, 1, 1, 9, 1, 4, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 3, 1, 8, 7, 1, 12, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         BlockState $$8 = Blocks.IRON_BARS
            .defaultBlockState()
            .setValue(IronBarsBlock.NORTH, Boolean.valueOf(true))
            .setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true));
         BlockState $$9 = Blocks.IRON_BARS
            .defaultBlockState()
            .setValue(IronBarsBlock.WEST, Boolean.valueOf(true))
            .setValue(IronBarsBlock.EAST, Boolean.valueOf(true));

         for (int $$10 = 3; $$10 < 14; $$10 += 2) {
            this.generateBox(pLevel, pBox, 0, 3, $$10, 0, 4, $$10, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 10, 3, $$10, 10, 4, $$10, $$8, $$8, false);
         }

         for (int $$11 = 2; $$11 < 9; $$11 += 2) {
            this.generateBox(pLevel, pBox, $$11, 3, 15, $$11, 4, 15, $$9, $$9, false);
         }

         BlockState $$12 = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.generateBox(pLevel, pBox, 4, 1, 5, 6, 1, 7, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 2, 6, 6, 2, 7, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 3, 7, 6, 3, 7, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int $$13 = 4; $$13 <= 6; $$13++) {
            this.placeBlock(pLevel, $$12, $$13, 1, 4, pBox);
            this.placeBlock(pLevel, $$12, $$13, 2, 5, pBox);
            this.placeBlock(pLevel, $$12, $$13, 3, 6, pBox);
         }

         BlockState $$14 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
         BlockState $$15 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
         BlockState $$16 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
         BlockState $$17 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
         boolean $$18 = true;
         boolean[] $$19 = new boolean[12];

         for (int $$20 = 0; $$20 < $$19.length; $$20++) {
            $$19[$$20] = pRandom.nextFloat() > 0.9F;
            $$18 &= $$19[$$20];
         }

         this.placeBlock(pLevel, $$14.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[0])), 4, 3, 8, pBox);
         this.placeBlock(pLevel, $$14.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[1])), 5, 3, 8, pBox);
         this.placeBlock(pLevel, $$14.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[2])), 6, 3, 8, pBox);
         this.placeBlock(pLevel, $$15.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[3])), 4, 3, 12, pBox);
         this.placeBlock(pLevel, $$15.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[4])), 5, 3, 12, pBox);
         this.placeBlock(pLevel, $$15.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[5])), 6, 3, 12, pBox);
         this.placeBlock(pLevel, $$16.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[6])), 3, 3, 9, pBox);
         this.placeBlock(pLevel, $$16.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[7])), 3, 3, 10, pBox);
         this.placeBlock(pLevel, $$16.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[8])), 3, 3, 11, pBox);
         this.placeBlock(pLevel, $$17.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[9])), 7, 3, 9, pBox);
         this.placeBlock(pLevel, $$17.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[10])), 7, 3, 10, pBox);
         this.placeBlock(pLevel, $$17.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf($$19[11])), 7, 3, 11, pBox);
         if ($$18) {
            BlockState $$21 = Blocks.END_PORTAL.defaultBlockState();
            this.placeBlock(pLevel, $$21, 4, 3, 9, pBox);
            this.placeBlock(pLevel, $$21, 5, 3, 9, pBox);
            this.placeBlock(pLevel, $$21, 6, 3, 9, pBox);
            this.placeBlock(pLevel, $$21, 4, 3, 10, pBox);
            this.placeBlock(pLevel, $$21, 5, 3, 10, pBox);
            this.placeBlock(pLevel, $$21, 6, 3, 10, pBox);
            this.placeBlock(pLevel, $$21, 4, 3, 11, pBox);
            this.placeBlock(pLevel, $$21, 5, 3, 11, pBox);
            this.placeBlock(pLevel, $$21, 6, 3, 11, pBox);
         }

         if (!this.hasPlacedSpawner) {
            BlockPos $$22 = this.getWorldPos(5, 3, 6);
            if (pBox.isInside($$22)) {
               this.hasPlacedSpawner = true;
               pLevel.setBlock($$22, Blocks.SPAWNER.defaultBlockState(), 2);
               if (pLevel.getBlockEntity($$22) instanceof SpawnerBlockEntity $$24) {
                  $$24.setEntityId(EntityType.SILVERFISH, pRandom);
               }
            }
         }
      }
   }

   public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 9;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 11;

      public PrisonHall(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public PrisonHall(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
      }

      public static StrongholdPieces.PrisonHall createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 9, 5, 11, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.PrisonHall(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 8, 4, 10, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 1, 0);
         this.generateBox(pLevel, pBox, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(pLevel, pBox, 4, 1, 1, 4, 3, 1, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 1, 3, 4, 3, 3, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 1, 7, 4, 3, 7, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(pLevel, pBox, 4, 1, 9, 4, 3, 9, false, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int $$7 = 1; $$7 <= 3; $$7++) {
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)),
               4,
               $$7,
               4,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS
                  .defaultBlockState()
                  .setValue(IronBarsBlock.NORTH, Boolean.valueOf(true))
                  .setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true))
                  .setValue(IronBarsBlock.EAST, Boolean.valueOf(true)),
               4,
               $$7,
               5,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)),
               4,
               $$7,
               6,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)),
               5,
               $$7,
               5,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)),
               6,
               $$7,
               5,
               pBox
            );
            this.placeBlock(
               pLevel,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)).setValue(IronBarsBlock.EAST, Boolean.valueOf(true)),
               7,
               $$7,
               5,
               pBox
            );
         }

         this.placeBlock(
            pLevel,
            Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)),
            4,
            3,
            2,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, Boolean.valueOf(true)).setValue(IronBarsBlock.SOUTH, Boolean.valueOf(true)),
            4,
            3,
            8,
            pBox
         );
         BlockState $$8 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
         BlockState $$9 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
         this.placeBlock(pLevel, $$8, 4, 1, 2, pBox);
         this.placeBlock(pLevel, $$9, 4, 2, 2, pBox);
         this.placeBlock(pLevel, $$8, 4, 1, 8, pBox);
         this.placeBlock(pLevel, $$9, 4, 2, 8, pBox);
      }
   }

   public static class RightTurn extends StrongholdPieces.Turn {
      public RightTurn(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public RightTurn(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         Direction $$3 = this.getOrientation();
         if ($$3 != Direction.NORTH && $$3 != Direction.EAST) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
         } else {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
         }
      }

      public static StrongholdPieces.RightTurn createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 5, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.RightTurn(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 4, 4, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 1, 0);
         Direction $$7 = this.getOrientation();
         if ($$7 != Direction.NORTH && $$7 != Direction.EAST) {
            this.generateBox(pLevel, pBox, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(pLevel, pBox, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 7;
      protected static final int DEPTH = 11;
      protected final int type;

      public RoomCrossing(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
         this.type = pRandom.nextInt(5);
      }

      public RoomCrossing(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, pTag);
         this.type = pTag.getInt("Type");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putInt("Type", this.type);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 4, 1);
         this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 4);
         this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 4);
      }

      public static StrongholdPieces.RoomCrossing createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -4, -1, 0, 11, 7, 11, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.RoomCrossing(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 10, 6, 10, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 4, 1, 0);
         this.generateBox(pLevel, pBox, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(pLevel, pBox, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(pLevel, pBox, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
         switch (this.type) {
            case 0:
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, pBox);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, pBox);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, pBox);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, pBox);
               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, pBox);
               this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, pBox);
               break;
            case 1:
               for (int $$7 = 0; $$7 < 5; $$7++) {
                  this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + $$7, pBox);
                  this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + $$7, pBox);
                  this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3 + $$7, 1, 3, pBox);
                  this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3 + $$7, 1, 7, pBox);
               }

               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, pBox);
               this.placeBlock(pLevel, Blocks.WATER.defaultBlockState(), 5, 4, 5, pBox);
               break;
            case 2:
               for (int $$8 = 1; $$8 <= 9; $$8++) {
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, $$8, pBox);
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, $$8, pBox);
               }

               for (int $$9 = 1; $$9 <= 9; $$9++) {
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), $$9, 3, 1, pBox);
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), $$9, 3, 9, pBox);
               }

               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, pBox);
               this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, pBox);

               for (int $$10 = 1; $$10 <= 3; $$10++) {
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 4, $$10, 4, pBox);
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 6, $$10, 4, pBox);
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 4, $$10, 6, pBox);
                  this.placeBlock(pLevel, Blocks.COBBLESTONE.defaultBlockState(), 6, $$10, 6, pBox);
               }

               this.placeBlock(pLevel, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, pBox);

               for (int $$11 = 2; $$11 <= 8; $$11++) {
                  this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, $$11, pBox);
                  this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, $$11, pBox);
                  if ($$11 <= 3 || $$11 >= 7) {
                     this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, $$11, pBox);
                     this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, $$11, pBox);
                     this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, $$11, pBox);
                  }

                  this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, $$11, pBox);
                  this.placeBlock(pLevel, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, $$11, pBox);
               }

               BlockState $$12 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
               this.placeBlock(pLevel, $$12, 9, 1, 3, pBox);
               this.placeBlock(pLevel, $$12, 9, 2, 3, pBox);
               this.placeBlock(pLevel, $$12, 9, 3, 3, pBox);
               this.createChest(pLevel, pBox, pRandom, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
         }
      }
   }

   static class SmoothStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(RandomSource p_229749_, int p_229750_, int p_229751_, int p_229752_, boolean p_229753_) {
         if (p_229753_) {
            float $$5 = p_229749_.nextFloat();
            if ($$5 < 0.2F) {
               this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            } else if ($$5 < 0.5F) {
               this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else if ($$5 < 0.55F) {
               this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
            } else {
               this.next = Blocks.STONE_BRICKS.defaultBlockState();
            }
         } else {
            this.next = Blocks.CAVE_AIR.defaultBlockState();
         }
      }
   }

   public static class StairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 5;
      private final boolean isSource;

      public StairsDown(StructurePieceType pType, int pGenDepth, int pX, int pZ, Direction pOrientation) {
         super(pType, pGenDepth, makeBoundingBox(pX, 64, pZ, pOrientation, 5, 11, 5));
         this.isSource = true;
         this.setOrientation(pOrientation);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
      }

      public StairsDown(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, pGenDepth, pBox);
         this.isSource = false;
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public StairsDown(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
         this.isSource = pTag.getBoolean("Source");
      }

      public StairsDown(CompoundTag pTag) {
         this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, pTag);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Source", this.isSource);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         if (this.isSource) {
            StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
      }

      public static StrongholdPieces.StairsDown createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 11, 5, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.StairsDown(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 10, 4, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(pLevel, pRandom, pBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, pBox);
         this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, pBox);
         this.placeBlock(pLevel, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, pBox);
      }
   }

   public static class StartPiece extends StrongholdPieces.StairsDown {
      public StrongholdPieces.PieceWeight previousPiece;
      @Nullable
      public StrongholdPieces.PortalRoom portalRoomPiece;
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(RandomSource pRandom, int pX, int pZ) {
         super(StructurePieceType.STRONGHOLD_START, 0, pX, pZ, getRandomHorizontalDirection(pRandom));
      }

      public StartPiece(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_START, pTag);
      }

      @Override
      public BlockPos getLocatorPosition() {
         return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
      }
   }

   public static class Straight extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private final boolean leftChild;
      private final boolean rightChild;

      public Straight(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
         this.leftChild = pRandom.nextInt(2) == 0;
         this.rightChild = pRandom.nextInt(2) == 0;
      }

      public Straight(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, pTag);
         this.leftChild = pTag.getBoolean("Left");
         this.rightChild = pTag.getBoolean("Right");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Left", this.leftChild);
         pTag.putBoolean("Right", this.rightChild);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
         if (this.leftChild) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 2);
         }

         if (this.rightChild) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 2);
         }
      }

      public static StrongholdPieces.Straight createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -1, 0, 5, 5, 7, pDirection);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.Straight(pGenDepth, pRandom, $$7, pDirection) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 4, 6, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(pLevel, pRandom, pBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         BlockState $$7 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
         BlockState $$8 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
         this.maybeGenerateBlock(pLevel, pBox, pRandom, 0.1F, 1, 2, 1, $$7);
         this.maybeGenerateBlock(pLevel, pBox, pRandom, 0.1F, 3, 2, 1, $$8);
         this.maybeGenerateBlock(pLevel, pBox, pRandom, 0.1F, 1, 2, 5, $$7);
         this.maybeGenerateBlock(pLevel, pBox, pRandom, 0.1F, 3, 2, 5, $$8);
         if (this.leftChild) {
            this.generateBox(pLevel, pBox, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightChild) {
            this.generateBox(pLevel, pBox, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 8;

      public StraightStairsDown(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.entryDoor = this.randomSmallDoor(pRandom);
      }

      public StraightStairsDown(CompoundTag pTag) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)pPiece, pPieces, pRandom, 1, 1);
      }

      public static StrongholdPieces.StraightStairsDown createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 11, 8, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new StrongholdPieces.StraightStairsDown(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 10, 7, true, pRandom, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(pLevel, pRandom, pBox, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(pLevel, pRandom, pBox, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
         BlockState $$7 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

         for (int $$8 = 0; $$8 < 6; $$8++) {
            this.placeBlock(pLevel, $$7, 1, 6 - $$8, 1 + $$8, pBox);
            this.placeBlock(pLevel, $$7, 2, 6 - $$8, 1 + $$8, pBox);
            this.placeBlock(pLevel, $$7, 3, 6 - $$8, 1 + $$8, pBox);
            if ($$8 < 5) {
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - $$8, 1 + $$8, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - $$8, 1 + $$8, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - $$8, 1 + $$8, pBox);
            }
         }
      }
   }

   abstract static class StrongholdPiece extends StructurePiece {
      protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

      protected StrongholdPiece(StructurePieceType pType, int pGenDepth, BoundingBox pBoundingBox) {
         super(pType, pGenDepth, pBoundingBox);
      }

      public StrongholdPiece(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.valueOf(pTag.getString("EntryDoor"));
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         pTag.putString("EntryDoor", this.entryDoor.name());
      }

      protected void generateSmallDoor(
         WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox, StrongholdPieces.StrongholdPiece.SmallDoorType pType, int pX, int pY, int pZ
      ) {
         switch (pType) {
            case OPENING:
               this.generateBox(pLevel, pBox, pX, pY, pZ, pX + 3 - 1, pY + 3 - 1, pZ, CAVE_AIR, CAVE_AIR, false);
               break;
            case WOOD_DOOR:
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 1, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.OAK_DOOR.defaultBlockState(), pX + 1, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), pX + 1, pY + 1, pZ, pBox);
               break;
            case GRATES:
               this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), pX + 1, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.CAVE_AIR.defaultBlockState(), pX + 1, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), pX, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, Boolean.valueOf(true)), pX, pY + 1, pZ, pBox);
               this.placeBlock(
                  pLevel,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)),
                  pX,
                  pY + 2,
                  pZ,
                  pBox
               );
               this.placeBlock(
                  pLevel,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)),
                  pX + 1,
                  pY + 2,
                  pZ,
                  pBox
               );
               this.placeBlock(
                  pLevel,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)).setValue(IronBarsBlock.WEST, Boolean.valueOf(true)),
                  pX + 2,
                  pY + 2,
                  pZ,
                  pBox
               );
               this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), pX + 2, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, Boolean.valueOf(true)), pX + 2, pY, pZ, pBox);
               break;
            case IRON_DOOR:
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 1, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 2, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BRICKS.defaultBlockState(), pX + 2, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.IRON_DOOR.defaultBlockState(), pX + 1, pY, pZ, pBox);
               this.placeBlock(pLevel, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), pX + 1, pY + 1, pZ, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), pX + 2, pY + 1, pZ + 1, pBox);
               this.placeBlock(pLevel, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), pX + 2, pY + 1, pZ - 1, pBox);
         }
      }

      protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(RandomSource pRandom) {
         int $$1 = pRandom.nextInt(5);
         switch ($$1) {
            case 0:
            case 1:
            default:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
            case 2:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.WOOD_DOOR;
            case 3:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES;
            case 4:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.IRON_DOOR;
         }
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildForward(
         StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetX, int pOffsetY
      ) {
         Direction $$5 = this.getOrientation();
         if ($$5 != null) {
            switch ($$5) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     $$5,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     $$5,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     $$5,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     $$5,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildLeft(
         StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX
      ) {
         Direction $$5 = this.getOrientation();
         if ($$5 != null) {
            switch ($$5) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateSmallDoorChildRight(
         StrongholdPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX
      ) {
         Direction $$5 = this.getOrientation();
         if ($$5 != null) {
            switch ($$5) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(BoundingBox pBox) {
         return pBox != null && pBox.minY() > 10;
      }

      protected static enum SmallDoorType {
         OPENING,
         WOOD_DOOR,
         GRATES,
         IRON_DOOR;
      }
   }

   public abstract static class Turn extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 5;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 5;

      protected Turn(StructurePieceType p_229930_, int p_229931_, BoundingBox p_229932_) {
         super(p_229930_, p_229931_, p_229932_);
      }

      public Turn(StructurePieceType p_229934_, CompoundTag p_229935_) {
         super(p_229934_, p_229935_);
      }
   }
}
