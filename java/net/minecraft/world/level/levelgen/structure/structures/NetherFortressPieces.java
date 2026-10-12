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
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class NetherFortressPieces {
   private static final int MAX_DEPTH = 30;
   private static final int LOWEST_Y_POSITION = 10;
   public static final int MAGIC_START_Y = 64;
   static final NetherFortressPieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeStraight.class, 30, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.RoomCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.StairsRoom.class, 10, 3),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.MonsterThrone.class, 5, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleEntrance.class, 5, 1)
   };
   static final NetherFortressPieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorPiece.class, 25, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorCrossingPiece.class, 15, 5),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorStairsPiece.class, 10, 3, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorTBalconyPiece.class, 7, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleStalkRoom.class, 5, 2)
   };

   static NetherFortressPieces.NetherBridgePiece findAndCreateBridgePieceFactory(
      NetherFortressPieces.PieceWeight pWeight,
      StructurePieceAccessor pPieces,
      RandomSource pRandom,
      int pX,
      int pY,
      int pZ,
      Direction pOrientation,
      int pGenDepth
   ) {
      Class<? extends NetherFortressPieces.NetherBridgePiece> $$8 = pWeight.pieceClass;
      NetherFortressPieces.NetherBridgePiece $$9 = null;
      if ($$8 == NetherFortressPieces.BridgeStraight.class) {
         $$9 = NetherFortressPieces.BridgeStraight.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.BridgeCrossing.class) {
         $$9 = NetherFortressPieces.BridgeCrossing.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.RoomCrossing.class) {
         $$9 = NetherFortressPieces.RoomCrossing.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.StairsRoom.class) {
         $$9 = NetherFortressPieces.StairsRoom.createPiece(pPieces, pX, pY, pZ, pGenDepth, pOrientation);
      } else if ($$8 == NetherFortressPieces.MonsterThrone.class) {
         $$9 = NetherFortressPieces.MonsterThrone.createPiece(pPieces, pX, pY, pZ, pGenDepth, pOrientation);
      } else if ($$8 == NetherFortressPieces.CastleEntrance.class) {
         $$9 = NetherFortressPieces.CastleEntrance.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleSmallCorridorPiece.class) {
         $$9 = NetherFortressPieces.CastleSmallCorridorPiece.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class) {
         $$9 = NetherFortressPieces.CastleSmallCorridorRightTurnPiece.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class) {
         $$9 = NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleCorridorStairsPiece.class) {
         $$9 = NetherFortressPieces.CastleCorridorStairsPiece.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleCorridorTBalconyPiece.class) {
         $$9 = NetherFortressPieces.CastleCorridorTBalconyPiece.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleSmallCorridorCrossingPiece.class) {
         $$9 = NetherFortressPieces.CastleSmallCorridorCrossingPiece.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      } else if ($$8 == NetherFortressPieces.CastleStalkRoom.class) {
         $$9 = NetherFortressPieces.CastleStalkRoom.createPiece(pPieces, pX, pY, pZ, pOrientation, pGenDepth);
      }

      return $$9;
   }

   public static class BridgeCrossing extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 19;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeCrossing(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      protected BridgeCrossing(int pX, int pZ, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(pX, 64, pZ, pOrientation, 19, 10, 19));
         this.setOrientation(pOrientation);
      }

      protected BridgeCrossing(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
      }

      public BridgeCrossing(CompoundTag pTag) {
         this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 8, 3, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 3, 8, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 3, 8, false);
      }

      public static NetherFortressPieces.BridgeCrossing createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -8, -3, 0, 19, 10, 19, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new NetherFortressPieces.BridgeCrossing(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$7 = 7; $$7 <= 11; $$7++) {
            for (int $$8 = 0; $$8 <= 2; $$8++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$7, -1, $$8, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$7, -1, 18 - $$8, pBox);
            }
         }

         this.generateBox(pLevel, pBox, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$9 = 0; $$9 <= 2; $$9++) {
            for (int $$10 = 7; $$10 <= 11; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, -1, $$10, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - $$9, -1, $$10, pBox);
            }
         }
      }
   }

   public static class BridgeEndFiller extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 8;
      private final int selfSeed;

      public BridgeEndFiller(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.selfSeed = pRandom.nextInt();
      }

      public BridgeEndFiller(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, pTag);
         this.selfSeed = pTag.getInt("Seed");
      }

      public static NetherFortressPieces.BridgeEndFiller createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -3, 0, 5, 10, 8, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null
            ? new NetherFortressPieces.BridgeEndFiller(pGenDepth, pRandom, $$7, pOrientation)
            : null;
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putInt("Seed", this.selfSeed);
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
         RandomSource $$7 = RandomSource.create((long)this.selfSeed);

         for (int $$8 = 0; $$8 <= 4; $$8++) {
            for (int $$9 = 3; $$9 <= 4; $$9++) {
               int $$10 = $$7.nextInt(8);
               this.generateBox(
                  pLevel, pBox, $$8, $$9, 0, $$8, $$9, $$10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }
         }

         int $$11 = $$7.nextInt(8);
         this.generateBox(pLevel, pBox, 0, 5, 0, 0, 5, $$11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         $$11 = $$7.nextInt(8);
         this.generateBox(pLevel, pBox, 4, 5, 0, 4, 5, $$11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$13 = 0; $$13 <= 4; $$13++) {
            int $$14 = $$7.nextInt(5);
            this.generateBox(pLevel, pBox, $$13, 2, 0, $$13, 2, $$14, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         }

         for (int $$15 = 0; $$15 <= 4; $$15++) {
            for (int $$16 = 0; $$16 <= 1; $$16++) {
               int $$17 = $$7.nextInt(3);
               this.generateBox(
                  pLevel, pBox, $$15, $$16, 0, $$15, $$16, $$17, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }
         }
      }
   }

   public static class BridgeStraight extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeStraight(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public BridgeStraight(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 1, 3, false);
      }

      public static NetherFortressPieces.BridgeStraight createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, -3, 0, 5, 10, 19, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new NetherFortressPieces.BridgeStraight(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$7 = 0; $$7 <= 4; $$7++) {
            for (int $$8 = 0; $$8 <= 2; $$8++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$7, -1, $$8, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$7, -1, 18 - $$8, pBox);
            }
         }

         BlockState $$9 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         BlockState $$10 = $$9.setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$11 = $$9.setValue(FenceBlock.WEST, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 0, 1, 1, 0, 4, 1, $$10, $$10, false);
         this.generateBox(pLevel, pBox, 0, 3, 4, 0, 4, 4, $$10, $$10, false);
         this.generateBox(pLevel, pBox, 0, 3, 14, 0, 4, 14, $$10, $$10, false);
         this.generateBox(pLevel, pBox, 0, 1, 17, 0, 4, 17, $$10, $$10, false);
         this.generateBox(pLevel, pBox, 4, 1, 1, 4, 4, 1, $$11, $$11, false);
         this.generateBox(pLevel, pBox, 4, 3, 4, 4, 4, 4, $$11, $$11, false);
         this.generateBox(pLevel, pBox, 4, 3, 14, 4, 4, 14, $$11, $$11, false);
         this.generateBox(pLevel, pBox, 4, 1, 17, 4, 4, 17, $$11, $$11, false);
      }
   }

   public static class CastleCorridorStairsPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 10;

      public CastleCorridorStairsPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleCorridorStairsPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 1, 0, true);
      }

      public static NetherFortressPieces.CastleCorridorStairsPiece createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -1, -7, 0, 5, 14, 10, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null
            ? new NetherFortressPieces.CastleCorridorStairsPiece(pGenDepth, $$6, pOrientation)
            : null;
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
         BlockState $$7 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

         for (int $$9 = 0; $$9 <= 9; $$9++) {
            int $$10 = Math.max(1, 7 - $$9);
            int $$11 = Math.min(Math.max($$10 + 5, 14 - $$9), 13);
            int $$12 = $$9;
            this.generateBox(pLevel, pBox, 0, 0, $$9, 4, $$10, $$9, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(pLevel, pBox, 1, $$10 + 1, $$9, 3, $$11 - 1, $$9, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            if ($$9 <= 6) {
               this.placeBlock(pLevel, $$7, 1, $$10 + 1, $$9, pBox);
               this.placeBlock(pLevel, $$7, 2, $$10 + 1, $$9, pBox);
               this.placeBlock(pLevel, $$7, 3, $$10 + 1, $$9, pBox);
            }

            this.generateBox(
               pLevel, pBox, 0, $$11, $$9, 4, $$11, $$9, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            this.generateBox(
               pLevel, pBox, 0, $$10 + 1, $$9, 0, $$11 - 1, $$9, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            this.generateBox(
               pLevel, pBox, 4, $$10 + 1, $$9, 4, $$11 - 1, $$9, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            if (($$9 & 1) == 0) {
               this.generateBox(pLevel, pBox, 0, $$10 + 2, $$9, 0, $$10 + 3, $$9, $$8, $$8, false);
               this.generateBox(pLevel, pBox, 4, $$10 + 2, $$9, 4, $$10 + 3, $$9, $$8, $$8, false);
            }

            for (int $$13 = 0; $$13 <= 4; $$13++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$13, -1, $$12, pBox);
            }
         }
      }
   }

   public static class CastleCorridorTBalconyPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 9;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 9;

      public CastleCorridorTBalconyPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleCorridorTBalconyPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = 1;
         Direction $$4 = this.getOrientation();
         if ($$4 == Direction.WEST || $$4 == Direction.NORTH) {
            $$3 = 5;
         }

         this.generateChildLeft((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, $$3, pRandom.nextInt(8) > 0);
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, $$3, pRandom.nextInt(8) > 0);
      }

      public static NetherFortressPieces.CastleCorridorTBalconyPiece createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -3, 0, 0, 9, 7, 9, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null
            ? new NetherFortressPieces.CastleCorridorTBalconyPiece(pGenDepth, $$6, pOrientation)
            : null;
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
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 0, 1, 4, 0, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 7, 3, 0, 7, 4, 0, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 8, 7, 3, 8, $$8, $$8, false);
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
            0,
            3,
            8,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
            8,
            3,
            8,
            pBox
         );
         this.generateBox(pLevel, pBox, 0, 3, 6, 0, 3, 7, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 8, 3, 6, 8, 3, 7, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 4, 5, 1, 5, 5, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 7, 4, 5, 7, 5, 5, $$8, $$8, false);

         for (int $$9 = 0; $$9 <= 5; $$9++) {
            for (int $$10 = 0; $$10 <= 8; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$10, -1, $$9, pBox);
            }
         }
      }
   }

   public static class CastleEntrance extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleEntrance(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleEntrance(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 5, 3, true);
      }

      public static NetherFortressPieces.CastleEntrance createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -5, -3, 0, 13, 14, 13, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null ? new NetherFortressPieces.CastleEntrance(pGenDepth, pRandom, $$7, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));

         for (int $$9 = 1; $$9 <= 11; $$9 += 2) {
            this.generateBox(pLevel, pBox, $$9, 10, 0, $$9, 11, 0, $$7, $$7, false);
            this.generateBox(pLevel, pBox, $$9, 10, 12, $$9, 11, 12, $$7, $$7, false);
            this.generateBox(pLevel, pBox, 0, 10, $$9, 0, 11, $$9, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 12, 10, $$9, 12, 11, $$9, $$8, $$8, false);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, 13, 0, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, 13, 12, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, $$9, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, $$9, pBox);
            if ($$9 != 11) {
               this.placeBlock(pLevel, $$7, $$9 + 1, 13, 0, pBox);
               this.placeBlock(pLevel, $$7, $$9 + 1, 13, 12, pBox);
               this.placeBlock(pLevel, $$8, 0, 13, $$9 + 1, pBox);
               this.placeBlock(pLevel, $$8, 12, 13, $$9 + 1, pBox);
            }
         }

         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
            0,
            13,
            0,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
            0,
            13,
            12,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
            12,
            13,
            12,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
            12,
            13,
            0,
            pBox
         );

         for (int $$10 = 3; $$10 <= 9; $$10 += 2) {
            this.generateBox(
               pLevel,
               pBox,
               1,
               7,
               $$10,
               1,
               8,
               $$10,
               $$8.setValue(FenceBlock.WEST, Boolean.valueOf(true)),
               $$8.setValue(FenceBlock.WEST, Boolean.valueOf(true)),
               false
            );
            this.generateBox(
               pLevel,
               pBox,
               11,
               7,
               $$10,
               11,
               8,
               $$10,
               $$8.setValue(FenceBlock.EAST, Boolean.valueOf(true)),
               $$8.setValue(FenceBlock.EAST, Boolean.valueOf(true)),
               false
            );
         }

         this.generateBox(pLevel, pBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$11 = 4; $$11 <= 8; $$11++) {
            for (int $$12 = 0; $$12 <= 2; $$12++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$11, -1, $$12, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$11, -1, 12 - $$12, pBox);
            }
         }

         for (int $$13 = 0; $$13 <= 2; $$13++) {
            for (int $$14 = 4; $$14 <= 8; $$14++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$13, -1, $$14, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - $$13, -1, $$14, pBox);
            }
         }

         this.generateBox(pLevel, pBox, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, pBox);
         this.placeBlock(pLevel, Blocks.LAVA.defaultBlockState(), 6, 5, 6, pBox);
         BlockPos $$15 = this.getWorldPos(6, 5, 6);
         if (pBox.isInside($$15)) {
            pLevel.scheduleTick($$15, Fluids.LAVA, 0);
         }
      }
   }

   public static class CastleSmallCorridorCrossingPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorCrossingPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleSmallCorridorCrossingPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 1, 0, true);
         this.generateChildLeft((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 1, true);
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 1, true);
      }

      public static NetherFortressPieces.CastleSmallCorridorCrossingPiece createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null
            ? new NetherFortressPieces.CastleSmallCorridorCrossingPiece(pGenDepth, $$6, pOrientation)
            : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$7 = 0; $$7 <= 4; $$7++) {
            for (int $$8 = 0; $$8 <= 4; $$8++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$7, -1, $$8, pBox);
            }
         }
      }
   }

   public static class CastleSmallCorridorLeftTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorLeftTurnPiece(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.isNeedingChest = pRandom.nextInt(3) == 0;
      }

      public CastleSmallCorridorLeftTurnPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, pTag);
         this.isNeedingChest = pTag.getBoolean("Chest");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildLeft((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 1, true);
      }

      public static NetherFortressPieces.CastleSmallCorridorLeftTurnPiece createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null
            ? new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(pGenDepth, pRandom, $$7, pOrientation)
            : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 3, 1, 4, 4, 1, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 4, 3, 3, 4, 4, 3, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 4, 1, 4, 4, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 3, 3, 4, 3, 4, 4, $$7, $$7, false);
         if (this.isNeedingChest && pBox.isInside(this.getWorldPos(3, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(pLevel, pBox, pRandom, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(pLevel, pBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$9 = 0; $$9 <= 4; $$9++) {
            for (int $$10 = 0; $$10 <= 4; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, -1, $$10, pBox);
            }
         }
      }
   }

   public static class CastleSmallCorridorPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorPiece(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleSmallCorridorPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 1, 0, true);
      }

      public static NetherFortressPieces.CastleSmallCorridorPiece createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null
            ? new NetherFortressPieces.CastleSmallCorridorPiece(pGenDepth, $$6, pOrientation)
            : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 3, 1, 0, 4, 1, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 0, 3, 3, 0, 4, 3, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 4, 3, 1, 4, 4, 1, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 4, 3, 3, 4, 4, 3, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$8 = 0; $$8 <= 4; $$8++) {
            for (int $$9 = 0; $$9 <= 4; $$9++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$8, -1, $$9, pBox);
            }
         }
      }
   }

   public static class CastleSmallCorridorRightTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorRightTurnPiece(int pGenDepth, RandomSource pRandom, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, pGenDepth, pBox);
         this.setOrientation(pOrientation);
         this.isNeedingChest = pRandom.nextInt(3) == 0;
      }

      public CastleSmallCorridorRightTurnPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, pTag);
         this.isNeedingChest = pTag.getBoolean("Chest");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 1, true);
      }

      public static NetherFortressPieces.CastleSmallCorridorRightTurnPiece createPiece(
         StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$7 = BoundingBox.orientBox(pX, pY, pZ, -1, 0, 0, 5, 7, 5, pOrientation);
         return isOkBox($$7) && pPieces.findCollisionPiece($$7) == null
            ? new NetherFortressPieces.CastleSmallCorridorRightTurnPiece(pGenDepth, pRandom, $$7, pOrientation)
            : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 3, 1, 0, 4, 1, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 0, 3, 3, 0, 4, 3, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 4, 1, 4, 4, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 3, 3, 4, 3, 4, 4, $$7, $$7, false);
         if (this.isNeedingChest && pBox.isInside(this.getWorldPos(1, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(pLevel, pBox, pRandom, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(pLevel, pBox, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$9 = 0; $$9 <= 4; $$9++) {
            for (int $$10 = 0; $$10 <= 4; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, -1, $$10, pBox);
            }
         }
      }
   }

   public static class CastleStalkRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleStalkRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public CastleStalkRoom(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 5, 3, true);
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 5, 11, true);
      }

      public static NetherFortressPieces.CastleStalkRoom createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -5, -3, 0, 13, 14, 13, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new NetherFortressPieces.CastleStalkRoom(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         BlockState $$9 = $$8.setValue(FenceBlock.WEST, Boolean.valueOf(true));
         BlockState $$10 = $$8.setValue(FenceBlock.EAST, Boolean.valueOf(true));

         for (int $$11 = 1; $$11 <= 11; $$11 += 2) {
            this.generateBox(pLevel, pBox, $$11, 10, 0, $$11, 11, 0, $$7, $$7, false);
            this.generateBox(pLevel, pBox, $$11, 10, 12, $$11, 11, 12, $$7, $$7, false);
            this.generateBox(pLevel, pBox, 0, 10, $$11, 0, 11, $$11, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 12, 10, $$11, 12, 11, $$11, $$8, $$8, false);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$11, 13, 0, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$11, 13, 12, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, $$11, pBox);
            this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, $$11, pBox);
            if ($$11 != 11) {
               this.placeBlock(pLevel, $$7, $$11 + 1, 13, 0, pBox);
               this.placeBlock(pLevel, $$7, $$11 + 1, 13, 12, pBox);
               this.placeBlock(pLevel, $$8, 0, 13, $$11 + 1, pBox);
               this.placeBlock(pLevel, $$8, 12, 13, $$11 + 1, pBox);
            }
         }

         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
            0,
            13,
            0,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.EAST, Boolean.valueOf(true)),
            0,
            13,
            12,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
            12,
            13,
            12,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, Boolean.valueOf(true)).setValue(FenceBlock.WEST, Boolean.valueOf(true)),
            12,
            13,
            0,
            pBox
         );

         for (int $$12 = 3; $$12 <= 9; $$12 += 2) {
            this.generateBox(pLevel, pBox, 1, 7, $$12, 1, 8, $$12, $$9, $$9, false);
            this.generateBox(pLevel, pBox, 11, 7, $$12, 11, 8, $$12, $$10, $$10, false);
         }

         BlockState $$13 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

         for (int $$14 = 0; $$14 <= 6; $$14++) {
            int $$15 = $$14 + 4;

            for (int $$16 = 5; $$16 <= 7; $$16++) {
               this.placeBlock(pLevel, $$13, $$16, 5 + $$14, $$15, pBox);
            }

            if ($$15 >= 5 && $$15 <= 8) {
               this.generateBox(
                  pLevel, pBox, 5, 5, $$15, 7, $$14 + 4, $$15, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            } else if ($$15 >= 9 && $$15 <= 10) {
               this.generateBox(
                  pLevel, pBox, 5, 8, $$15, 7, $$14 + 4, $$15, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }

            if ($$14 >= 1) {
               this.generateBox(pLevel, pBox, 5, 6 + $$14, $$15, 7, 9 + $$14, $$15, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            }
         }

         for (int $$17 = 5; $$17 <= 7; $$17++) {
            this.placeBlock(pLevel, $$13, $$17, 12, 11, pBox);
         }

         this.generateBox(pLevel, pBox, 5, 6, 7, 5, 7, 7, $$10, $$10, false);
         this.generateBox(pLevel, pBox, 7, 6, 7, 7, 7, 7, $$9, $$9, false);
         this.generateBox(pLevel, pBox, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState $$18 = $$13.setValue(StairBlock.FACING, Direction.EAST);
         BlockState $$19 = $$13.setValue(StairBlock.FACING, Direction.WEST);
         this.placeBlock(pLevel, $$19, 4, 5, 2, pBox);
         this.placeBlock(pLevel, $$19, 4, 5, 3, pBox);
         this.placeBlock(pLevel, $$19, 4, 5, 9, pBox);
         this.placeBlock(pLevel, $$19, 4, 5, 10, pBox);
         this.placeBlock(pLevel, $$18, 8, 5, 2, pBox);
         this.placeBlock(pLevel, $$18, 8, 5, 3, pBox);
         this.placeBlock(pLevel, $$18, 8, 5, 9, pBox);
         this.placeBlock(pLevel, $$18, 8, 5, 10, pBox);
         this.generateBox(pLevel, pBox, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int $$20 = 4; $$20 <= 8; $$20++) {
            for (int $$21 = 0; $$21 <= 2; $$21++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$20, -1, $$21, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$20, -1, 12 - $$21, pBox);
            }
         }

         for (int $$22 = 0; $$22 <= 2; $$22++) {
            for (int $$23 = 4; $$23 <= 8; $$23++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$22, -1, $$23, pBox);
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - $$22, -1, $$23, pBox);
            }
         }
      }
   }

   public static class MonsterThrone extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 8;
      private static final int DEPTH = 9;
      private boolean hasPlacedSpawner;

      public MonsterThrone(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public MonsterThrone(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, pTag);
         this.hasPlacedSpawner = pTag.getBoolean("Mob");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("Mob", this.hasPlacedSpawner);
      }

      public static NetherFortressPieces.MonsterThrone createPiece(
         StructurePieceAccessor pPieces, int pX, int pY, int pZ, int pGenDepth, Direction pOrientation
      ) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 8, 9, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new NetherFortressPieces.MonsterThrone(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 1, 6, 3, pBox);
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 5, 6, 3, pBox);
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)),
            0,
            6,
            3,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.NORTH, Boolean.valueOf(true)),
            6,
            6,
            3,
            pBox
         );
         this.generateBox(pLevel, pBox, 0, 6, 4, 0, 6, 7, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 6, 6, 4, 6, 6, 7, $$8, $$8, false);
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
            0,
            6,
            8,
            pBox
         );
         this.placeBlock(
            pLevel,
            Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)).setValue(FenceBlock.SOUTH, Boolean.valueOf(true)),
            6,
            6,
            8,
            pBox
         );
         this.generateBox(pLevel, pBox, 1, 6, 8, 5, 6, 8, $$7, $$7, false);
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 1, 7, 8, pBox);
         this.generateBox(pLevel, pBox, 2, 7, 8, 4, 7, 8, $$7, $$7, false);
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 5, 7, 8, pBox);
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, Boolean.valueOf(true)), 2, 8, 8, pBox);
         this.placeBlock(pLevel, $$7, 3, 8, 8, pBox);
         this.placeBlock(pLevel, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, Boolean.valueOf(true)), 4, 8, 8, pBox);
         if (!this.hasPlacedSpawner) {
            BlockPos $$9 = this.getWorldPos(3, 5, 5);
            if (pBox.isInside($$9)) {
               this.hasPlacedSpawner = true;
               pLevel.setBlock($$9, Blocks.SPAWNER.defaultBlockState(), 2);
               if (pLevel.getBlockEntity($$9) instanceof SpawnerBlockEntity $$11) {
                  $$11.setEntityId(EntityType.BLAZE, pRandom);
               }
            }
         }

         for (int $$12 = 0; $$12 <= 6; $$12++) {
            for (int $$13 = 0; $$13 <= 6; $$13++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$12, -1, $$13, pBox);
            }
         }
      }
   }

   abstract static class NetherBridgePiece extends StructurePiece {
      protected NetherBridgePiece(StructurePieceType pType, int pGenDepth, BoundingBox pBoundingBox) {
         super(pType, pGenDepth, pBoundingBox);
      }

      public NetherBridgePiece(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      }

      private int updatePieceWeight(List<NetherFortressPieces.PieceWeight> pWeights) {
         boolean $$1 = false;
         int $$2 = 0;

         for (NetherFortressPieces.PieceWeight $$3 : pWeights) {
            if ($$3.maxPlaceCount > 0 && $$3.placeCount < $$3.maxPlaceCount) {
               $$1 = true;
            }

            $$2 += $$3.weight;
         }

         return $$1 ? $$2 : -1;
      }

      private NetherFortressPieces.NetherBridgePiece generatePiece(
         NetherFortressPieces.StartPiece pStartPiece,
         List<NetherFortressPieces.PieceWeight> pWeights,
         StructurePieceAccessor pPieces,
         RandomSource pRandom,
         int pX,
         int pY,
         int pZ,
         Direction pOrientation,
         int pGenDepth
      ) {
         int $$9 = this.updatePieceWeight(pWeights);
         boolean $$10 = $$9 > 0 && pGenDepth <= 30;
         int $$11 = 0;

         while ($$11 < 5 && $$10) {
            $$11++;
            int $$12 = pRandom.nextInt($$9);

            for (NetherFortressPieces.PieceWeight $$13 : pWeights) {
               $$12 -= $$13.weight;
               if ($$12 < 0) {
                  if (!$$13.doPlace(pGenDepth) || $$13 == pStartPiece.previousPiece && !$$13.allowInRow) {
                     break;
                  }

                  NetherFortressPieces.NetherBridgePiece $$14 = NetherFortressPieces.findAndCreateBridgePieceFactory(
                     $$13, pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth
                  );
                  if ($$14 != null) {
                     $$13.placeCount++;
                     pStartPiece.previousPiece = $$13;
                     if (!$$13.isValid()) {
                        pWeights.remove($$13);
                     }

                     return $$14;
                  }
               }
            }
         }

         return NetherFortressPieces.BridgeEndFiller.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
      }

      private StructurePiece generateAndAddPiece(
         NetherFortressPieces.StartPiece pStartPiece,
         StructurePieceAccessor pPieces,
         RandomSource pRandom,
         int pX,
         int pY,
         int pZ,
         @Nullable Direction pOrientation,
         int pGenDepth,
         boolean pCastlePiece
      ) {
         if (Math.abs(pX - pStartPiece.getBoundingBox().minX()) <= 112 && Math.abs(pZ - pStartPiece.getBoundingBox().minZ()) <= 112) {
            List<NetherFortressPieces.PieceWeight> $$9 = pStartPiece.availableBridgePieces;
            if (pCastlePiece) {
               $$9 = pStartPiece.availableCastlePieces;
            }

            StructurePiece $$10 = this.generatePiece(pStartPiece, $$9, pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth + 1);
            if ($$10 != null) {
               pPieces.addPiece($$10);
               pStartPiece.pendingChildren.add($$10);
            }

            return $$10;
         } else {
            return NetherFortressPieces.BridgeEndFiller.createPiece(pPieces, pRandom, pX, pY, pZ, pOrientation, pGenDepth);
         }
      }

      @Nullable
      protected StructurePiece generateChildForward(
         NetherFortressPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetX, int pOffsetY, boolean pCastlePiece
      ) {
         Direction $$6 = this.getOrientation();
         if ($$6 != null) {
            switch ($$6) {
               case NORTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     $$6,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     $$6,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     $$6,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     $$6,
                     this.getGenDepth(),
                     pCastlePiece
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateChildLeft(
         NetherFortressPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX, boolean pCastlePiece
      ) {
         Direction $$6 = this.getOrientation();
         if ($$6 != null) {
            switch ($$6) {
               case NORTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.WEST,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.WEST,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     pCastlePiece
                  );
            }
         }

         return null;
      }

      @Nullable
      protected StructurePiece generateChildRight(
         NetherFortressPieces.StartPiece pStartPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pOffsetY, int pOffsetX, boolean pCastlePiece
      ) {
         Direction $$6 = this.getOrientation();
         if ($$6 != null) {
            switch ($$6) {
               case NORTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.EAST,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.minZ() + pOffsetX,
                     Direction.EAST,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     pCastlePiece
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     pStartPiece,
                     pPieces,
                     pRandom,
                     this.boundingBox.minX() + pOffsetX,
                     this.boundingBox.minY() + pOffsetY,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     pCastlePiece
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(BoundingBox pBox) {
         return pBox != null && pBox.minY() > 10;
      }
   }

   static class PieceWeight {
      public final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;
      public final boolean allowInRow;

      public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> pPieceClass, int pWeight, int pMaxPlaceCount, boolean pAllowInRow) {
         this.pieceClass = pPieceClass;
         this.weight = pWeight;
         this.maxPlaceCount = pMaxPlaceCount;
         this.allowInRow = pAllowInRow;
      }

      public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> pPieceClass, int pWeight, int pMaxPlaceCount) {
         this(pPieceClass, pWeight, pMaxPlaceCount, false);
      }

      public boolean doPlace(int pGenDepth) {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }

      public boolean isValid() {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }
   }

   public static class RoomCrossing extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 9;
      private static final int DEPTH = 7;

      public RoomCrossing(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public RoomCrossing(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildForward((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 2, 0, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 2, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 0, 2, false);
      }

      public static NetherFortressPieces.RoomCrossing createPiece(StructurePieceAccessor pPieces, int pX, int pY, int pZ, Direction pOrientation, int pGenDepth) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 9, 7, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new NetherFortressPieces.RoomCrossing(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 0, 4, 5, 0, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 6, 4, 5, 6, $$7, $$7, false);
         this.generateBox(pLevel, pBox, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 5, 2, 0, 5, 4, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 5, 2, 6, 5, 4, $$8, $$8, false);

         for (int $$9 = 0; $$9 <= 6; $$9++) {
            for (int $$10 = 0; $$10 <= 6; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, -1, $$10, pBox);
            }
         }
      }
   }

   public static class StairsRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 7;

      public StairsRoom(int pGenDepth, BoundingBox pBox, Direction pOrientation) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      public StairsRoom(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, pTag);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         this.generateChildRight((NetherFortressPieces.StartPiece)pPiece, pPieces, pRandom, 6, 2, false);
      }

      public static NetherFortressPieces.StairsRoom createPiece(StructurePieceAccessor pPieces, int pX, int pY, int pZ, int pGenDepth, Direction pOrientation) {
         BoundingBox $$6 = BoundingBox.orientBox(pX, pY, pZ, -2, 0, 0, 7, 11, 7, pOrientation);
         return isOkBox($$6) && pPieces.findCollisionPiece($$6) == null ? new NetherFortressPieces.StairsRoom(pGenDepth, $$6, pOrientation) : null;
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
         this.generateBox(pLevel, pBox, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState $$7 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.WEST, Boolean.valueOf(true))
            .setValue(FenceBlock.EAST, Boolean.valueOf(true));
         BlockState $$8 = Blocks.NETHER_BRICK_FENCE
            .defaultBlockState()
            .setValue(FenceBlock.NORTH, Boolean.valueOf(true))
            .setValue(FenceBlock.SOUTH, Boolean.valueOf(true));
         this.generateBox(pLevel, pBox, 0, 3, 2, 0, 5, 4, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 6, 3, 2, 6, 5, 2, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 6, 3, 4, 6, 5, 4, $$8, $$8, false);
         this.placeBlock(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, pBox);
         this.generateBox(pLevel, pBox, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(pLevel, pBox, 2, 5, 0, 4, 5, 0, $$7, $$7, false);

         for (int $$9 = 0; $$9 <= 6; $$9++) {
            for (int $$10 = 0; $$10 <= 6; $$10++) {
               this.fillColumnDown(pLevel, Blocks.NETHER_BRICKS.defaultBlockState(), $$9, -1, $$10, pBox);
            }
         }
      }
   }

   public static class StartPiece extends NetherFortressPieces.BridgeCrossing {
      public NetherFortressPieces.PieceWeight previousPiece;
      public List<NetherFortressPieces.PieceWeight> availableBridgePieces;
      public List<NetherFortressPieces.PieceWeight> availableCastlePieces;
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(RandomSource pRandom, int pX, int pZ) {
         super(pX, pZ, getRandomHorizontalDirection(pRandom));
         this.availableBridgePieces = Lists.newArrayList();

         for (NetherFortressPieces.PieceWeight $$3 : NetherFortressPieces.BRIDGE_PIECE_WEIGHTS) {
            $$3.placeCount = 0;
            this.availableBridgePieces.add($$3);
         }

         this.availableCastlePieces = Lists.newArrayList();

         for (NetherFortressPieces.PieceWeight $$4 : NetherFortressPieces.CASTLE_PIECE_WEIGHTS) {
            $$4.placeCount = 0;
            this.availableCastlePieces.add($$4);
         }
      }

      public StartPiece(CompoundTag pTag) {
         super(StructurePieceType.NETHER_FORTRESS_START, pTag);
      }
   }
}
