package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class WoodlandMansionPieces {
   public static void generateMansion(
      StructureTemplateManager pStructureTemplateManager,
      BlockPos pPos,
      Rotation pRotation,
      List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
      RandomSource pRandom
   ) {
      WoodlandMansionPieces.MansionGrid $$5 = new WoodlandMansionPieces.MansionGrid(pRandom);
      WoodlandMansionPieces.MansionPiecePlacer $$6 = new WoodlandMansionPieces.MansionPiecePlacer(pStructureTemplateManager, pRandom);
      $$6.createMansion(pPos, pRotation, pPieces, $$5);
   }

   static class FirstFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource p_229995_) {
         return "1x1_a" + (p_229995_.nextInt(5) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource p_230000_) {
         return "1x1_as" + (p_230000_.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource p_229997_, boolean p_229998_) {
         return "1x2_a" + (p_229997_.nextInt(9) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource p_230002_, boolean p_230003_) {
         return "1x2_b" + (p_230002_.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource p_230005_) {
         return "1x2_s" + (p_230005_.nextInt(2) + 1);
      }

      @Override
      public String get2x2(RandomSource p_230007_) {
         return "2x2_a" + (p_230007_.nextInt(4) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource p_230009_) {
         return "2x2_s1";
      }
   }

   abstract static class FloorRoomCollection {
      public abstract String get1x1(RandomSource pRandom);

      public abstract String get1x1Secret(RandomSource pRandom);

      public abstract String get1x2SideEntrance(RandomSource pRandom, boolean pIsStairs);

      public abstract String get1x2FrontEntrance(RandomSource pRandom, boolean pIsStairs);

      public abstract String get1x2Secret(RandomSource pRandom);

      public abstract String get2x2(RandomSource pRandom);

      public abstract String get2x2Secret(RandomSource pRandom);
   }

   static class MansionGrid {
      private static final int DEFAULT_SIZE = 11;
      private static final int CLEAR = 0;
      private static final int CORRIDOR = 1;
      private static final int ROOM = 2;
      private static final int START_ROOM = 3;
      private static final int TEST_ROOM = 4;
      private static final int BLOCKED = 5;
      private static final int ROOM_1x1 = 65536;
      private static final int ROOM_1x2 = 131072;
      private static final int ROOM_2x2 = 262144;
      private static final int ROOM_ORIGIN_FLAG = 1048576;
      private static final int ROOM_DOOR_FLAG = 2097152;
      private static final int ROOM_STAIRS_FLAG = 4194304;
      private static final int ROOM_CORRIDOR_FLAG = 8388608;
      private static final int ROOM_TYPE_MASK = 983040;
      private static final int ROOM_ID_MASK = 65535;
      private final RandomSource random;
      final WoodlandMansionPieces.SimpleGrid baseGrid;
      final WoodlandMansionPieces.SimpleGrid thirdFloorGrid;
      final WoodlandMansionPieces.SimpleGrid[] floorRooms;
      final int entranceX;
      final int entranceY;

      public MansionGrid(RandomSource pRandom) {
         this.random = pRandom;
         int $$1 = 11;
         this.entranceX = 7;
         this.entranceY = 4;
         this.baseGrid = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.baseGrid.set(this.entranceX, this.entranceY, this.entranceX + 1, this.entranceY + 1, 3);
         this.baseGrid.set(this.entranceX - 1, this.entranceY, this.entranceX - 1, this.entranceY + 1, 2);
         this.baseGrid.set(this.entranceX + 2, this.entranceY - 2, this.entranceX + 3, this.entranceY + 3, 5);
         this.baseGrid.set(this.entranceX + 1, this.entranceY - 2, this.entranceX + 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX + 1, this.entranceY + 2, this.entranceX + 1, this.entranceY + 3, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY + 2, 1);
         this.baseGrid.set(0, 0, 11, 1, 5);
         this.baseGrid.set(0, 9, 11, 11, 5);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY - 2, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY + 3, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY - 1, Direction.WEST, 3);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY + 2, Direction.WEST, 3);

         while (this.cleanEdges(this.baseGrid)) {
         }

         this.floorRooms = new WoodlandMansionPieces.SimpleGrid[3];
         this.floorRooms[0] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[1] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[2] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.identifyRooms(this.baseGrid, this.floorRooms[0]);
         this.identifyRooms(this.baseGrid, this.floorRooms[1]);
         this.floorRooms[0].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.floorRooms[1].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.thirdFloorGrid = new WoodlandMansionPieces.SimpleGrid(this.baseGrid.width, this.baseGrid.height, 5);
         this.setupThirdFloor();
         this.identifyRooms(this.thirdFloorGrid, this.floorRooms[2]);
      }

      public static boolean isHouse(WoodlandMansionPieces.SimpleGrid pLayout, int pX, int pY) {
         int $$3 = pLayout.get(pX, pY);
         return $$3 == 1 || $$3 == 2 || $$3 == 3 || $$3 == 4;
      }

      public boolean isRoomId(WoodlandMansionPieces.SimpleGrid pLayout, int pX, int pY, int pFloor, int pRoomId) {
         return (this.floorRooms[pFloor].get(pX, pY) & 65535) == pRoomId;
      }

      @Nullable
      public Direction get1x2RoomDirection(WoodlandMansionPieces.SimpleGrid pLayout, int pX, int pY, int pFloor, int pRoomId) {
         for (Direction $$5 : Direction.Plane.HORIZONTAL) {
            if (this.isRoomId(pLayout, pX + $$5.getStepX(), pY + $$5.getStepZ(), pFloor, pRoomId)) {
               return $$5;
            }
         }

         return null;
      }

      private void recursiveCorridor(WoodlandMansionPieces.SimpleGrid pLayout, int pX, int pY, Direction pDirection, int pLength) {
         if (pLength > 0) {
            pLayout.set(pX, pY, 1);
            pLayout.setif(pX + pDirection.getStepX(), pY + pDirection.getStepZ(), 0, 1);

            for (int $$5 = 0; $$5 < 8; $$5++) {
               Direction $$6 = Direction.from2DDataValue(this.random.nextInt(4));
               if ($$6 != pDirection.getOpposite() && ($$6 != Direction.EAST || !this.random.nextBoolean())) {
                  int $$7 = pX + pDirection.getStepX();
                  int $$8 = pY + pDirection.getStepZ();
                  if (pLayout.get($$7 + $$6.getStepX(), $$8 + $$6.getStepZ()) == 0 && pLayout.get($$7 + $$6.getStepX() * 2, $$8 + $$6.getStepZ() * 2) == 0) {
                     this.recursiveCorridor(pLayout, pX + pDirection.getStepX() + $$6.getStepX(), pY + pDirection.getStepZ() + $$6.getStepZ(), $$6, pLength - 1);
                     break;
                  }
               }
            }

            Direction $$9 = pDirection.getClockWise();
            Direction $$10 = pDirection.getCounterClockWise();
            pLayout.setif(pX + $$9.getStepX(), pY + $$9.getStepZ(), 0, 2);
            pLayout.setif(pX + $$10.getStepX(), pY + $$10.getStepZ(), 0, 2);
            pLayout.setif(pX + pDirection.getStepX() + $$9.getStepX(), pY + pDirection.getStepZ() + $$9.getStepZ(), 0, 2);
            pLayout.setif(pX + pDirection.getStepX() + $$10.getStepX(), pY + pDirection.getStepZ() + $$10.getStepZ(), 0, 2);
            pLayout.setif(pX + pDirection.getStepX() * 2, pY + pDirection.getStepZ() * 2, 0, 2);
            pLayout.setif(pX + $$9.getStepX() * 2, pY + $$9.getStepZ() * 2, 0, 2);
            pLayout.setif(pX + $$10.getStepX() * 2, pY + $$10.getStepZ() * 2, 0, 2);
         }
      }

      private boolean cleanEdges(WoodlandMansionPieces.SimpleGrid pGrid) {
         boolean $$1 = false;

         for (int $$2 = 0; $$2 < pGrid.height; $$2++) {
            for (int $$3 = 0; $$3 < pGrid.width; $$3++) {
               if (pGrid.get($$3, $$2) == 0) {
                  int $$4 = 0;
                  $$4 += isHouse(pGrid, $$3 + 1, $$2) ? 1 : 0;
                  $$4 += isHouse(pGrid, $$3 - 1, $$2) ? 1 : 0;
                  $$4 += isHouse(pGrid, $$3, $$2 + 1) ? 1 : 0;
                  $$4 += isHouse(pGrid, $$3, $$2 - 1) ? 1 : 0;
                  if ($$4 >= 3) {
                     pGrid.set($$3, $$2, 2);
                     $$1 = true;
                  } else if ($$4 == 2) {
                     int $$5 = 0;
                     $$5 += isHouse(pGrid, $$3 + 1, $$2 + 1) ? 1 : 0;
                     $$5 += isHouse(pGrid, $$3 - 1, $$2 + 1) ? 1 : 0;
                     $$5 += isHouse(pGrid, $$3 + 1, $$2 - 1) ? 1 : 0;
                     $$5 += isHouse(pGrid, $$3 - 1, $$2 - 1) ? 1 : 0;
                     if ($$5 <= 1) {
                        pGrid.set($$3, $$2, 2);
                        $$1 = true;
                     }
                  }
               }
            }
         }

         return $$1;
      }

      private void setupThirdFloor() {
         List<Tuple<Integer, Integer>> $$0 = Lists.newArrayList();
         WoodlandMansionPieces.SimpleGrid $$1 = this.floorRooms[1];

         for (int $$2 = 0; $$2 < this.thirdFloorGrid.height; $$2++) {
            for (int $$3 = 0; $$3 < this.thirdFloorGrid.width; $$3++) {
               int $$4 = $$1.get($$3, $$2);
               int $$5 = $$4 & 983040;
               if ($$5 == 131072 && ($$4 & 2097152) == 2097152) {
                  $$0.add(new Tuple<>($$3, $$2));
               }
            }
         }

         if ($$0.isEmpty()) {
            this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
         } else {
            Tuple<Integer, Integer> $$6 = $$0.get(this.random.nextInt($$0.size()));
            int $$7 = $$1.get($$6.getA(), $$6.getB());
            $$1.set($$6.getA(), $$6.getB(), $$7 | 4194304);
            Direction $$8 = this.get1x2RoomDirection(this.baseGrid, $$6.getA(), $$6.getB(), 1, $$7 & 65535);
            int $$9 = $$6.getA() + $$8.getStepX();
            int $$10 = $$6.getB() + $$8.getStepZ();

            for (int $$11 = 0; $$11 < this.thirdFloorGrid.height; $$11++) {
               for (int $$12 = 0; $$12 < this.thirdFloorGrid.width; $$12++) {
                  if (!isHouse(this.baseGrid, $$12, $$11)) {
                     this.thirdFloorGrid.set($$12, $$11, 5);
                  } else if ($$12 == $$6.getA() && $$11 == $$6.getB()) {
                     this.thirdFloorGrid.set($$12, $$11, 3);
                  } else if ($$12 == $$9 && $$11 == $$10) {
                     this.thirdFloorGrid.set($$12, $$11, 3);
                     this.floorRooms[2].set($$12, $$11, 8388608);
                  }
               }
            }

            List<Direction> $$13 = Lists.newArrayList();

            for (Direction $$14 : Direction.Plane.HORIZONTAL) {
               if (this.thirdFloorGrid.get($$9 + $$14.getStepX(), $$10 + $$14.getStepZ()) == 0) {
                  $$13.add($$14);
               }
            }

            if ($$13.isEmpty()) {
               this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
               $$1.set($$6.getA(), $$6.getB(), $$7);
            } else {
               Direction $$15 = $$13.get(this.random.nextInt($$13.size()));
               this.recursiveCorridor(this.thirdFloorGrid, $$9 + $$15.getStepX(), $$10 + $$15.getStepZ(), $$15, 4);

               while (this.cleanEdges(this.thirdFloorGrid)) {
               }
            }
         }
      }

      private void identifyRooms(WoodlandMansionPieces.SimpleGrid pGrid, WoodlandMansionPieces.SimpleGrid pFloorRooms) {
         ObjectArrayList<Tuple<Integer, Integer>> $$2 = new ObjectArrayList();

         for (int $$3 = 0; $$3 < pGrid.height; $$3++) {
            for (int $$4 = 0; $$4 < pGrid.width; $$4++) {
               if (pGrid.get($$4, $$3) == 2) {
                  $$2.add(new Tuple<>($$4, $$3));
               }
            }
         }

         Util.shuffle($$2, this.random);
         int $$5 = 10;
         ObjectListIterator var20 = $$2.iterator();

         while (var20.hasNext()) {
            Tuple<Integer, Integer> $$6 = (Tuple<Integer, Integer>)var20.next();
            int $$7 = $$6.getA();
            int $$8 = $$6.getB();
            if (pFloorRooms.get($$7, $$8) == 0) {
               int $$9 = $$7;
               int $$10 = $$7;
               int $$11 = $$8;
               int $$12 = $$8;
               int $$13 = 65536;
               if (pFloorRooms.get($$7 + 1, $$8) == 0
                  && pFloorRooms.get($$7, $$8 + 1) == 0
                  && pFloorRooms.get($$7 + 1, $$8 + 1) == 0
                  && pGrid.get($$7 + 1, $$8) == 2
                  && pGrid.get($$7, $$8 + 1) == 2
                  && pGrid.get($$7 + 1, $$8 + 1) == 2) {
                  $$10 = $$7 + 1;
                  $$12 = $$8 + 1;
                  $$13 = 262144;
               } else if (pFloorRooms.get($$7 - 1, $$8) == 0
                  && pFloorRooms.get($$7, $$8 + 1) == 0
                  && pFloorRooms.get($$7 - 1, $$8 + 1) == 0
                  && pGrid.get($$7 - 1, $$8) == 2
                  && pGrid.get($$7, $$8 + 1) == 2
                  && pGrid.get($$7 - 1, $$8 + 1) == 2) {
                  $$9 = $$7 - 1;
                  $$12 = $$8 + 1;
                  $$13 = 262144;
               } else if (pFloorRooms.get($$7 - 1, $$8) == 0
                  && pFloorRooms.get($$7, $$8 - 1) == 0
                  && pFloorRooms.get($$7 - 1, $$8 - 1) == 0
                  && pGrid.get($$7 - 1, $$8) == 2
                  && pGrid.get($$7, $$8 - 1) == 2
                  && pGrid.get($$7 - 1, $$8 - 1) == 2) {
                  $$9 = $$7 - 1;
                  $$11 = $$8 - 1;
                  $$13 = 262144;
               } else if (pFloorRooms.get($$7 + 1, $$8) == 0 && pGrid.get($$7 + 1, $$8) == 2) {
                  $$10 = $$7 + 1;
                  $$13 = 131072;
               } else if (pFloorRooms.get($$7, $$8 + 1) == 0 && pGrid.get($$7, $$8 + 1) == 2) {
                  $$12 = $$8 + 1;
                  $$13 = 131072;
               } else if (pFloorRooms.get($$7 - 1, $$8) == 0 && pGrid.get($$7 - 1, $$8) == 2) {
                  $$9 = $$7 - 1;
                  $$13 = 131072;
               } else if (pFloorRooms.get($$7, $$8 - 1) == 0 && pGrid.get($$7, $$8 - 1) == 2) {
                  $$11 = $$8 - 1;
                  $$13 = 131072;
               }

               int $$14 = this.random.nextBoolean() ? $$9 : $$10;
               int $$15 = this.random.nextBoolean() ? $$11 : $$12;
               int $$16 = 2097152;
               if (!pGrid.edgesTo($$14, $$15, 1)) {
                  $$14 = $$14 == $$9 ? $$10 : $$9;
                  $$15 = $$15 == $$11 ? $$12 : $$11;
                  if (!pGrid.edgesTo($$14, $$15, 1)) {
                     $$15 = $$15 == $$11 ? $$12 : $$11;
                     if (!pGrid.edgesTo($$14, $$15, 1)) {
                        $$14 = $$14 == $$9 ? $$10 : $$9;
                        $$15 = $$15 == $$11 ? $$12 : $$11;
                        if (!pGrid.edgesTo($$14, $$15, 1)) {
                           $$16 = 0;
                           $$14 = $$9;
                           $$15 = $$11;
                        }
                     }
                  }
               }

               for (int $$17 = $$11; $$17 <= $$12; $$17++) {
                  for (int $$18 = $$9; $$18 <= $$10; $$18++) {
                     if ($$18 == $$14 && $$17 == $$15) {
                        pFloorRooms.set($$18, $$17, 1048576 | $$16 | $$13 | $$5);
                     } else {
                        pFloorRooms.set($$18, $$17, $$13 | $$5);
                     }
                  }
               }

               $$5++;
            }
         }
      }
   }

   static class MansionPiecePlacer {
      private final StructureTemplateManager structureTemplateManager;
      private final RandomSource random;
      private int startX;
      private int startY;

      public MansionPiecePlacer(StructureTemplateManager pStructureTemplateManager, RandomSource pRandom) {
         this.structureTemplateManager = pStructureTemplateManager;
         this.random = pRandom;
      }

      public void createMansion(
         BlockPos pPos, Rotation pRotation, List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, WoodlandMansionPieces.MansionGrid pGrid
      ) {
         WoodlandMansionPieces.PlacementData $$4 = new WoodlandMansionPieces.PlacementData();
         $$4.position = pPos;
         $$4.rotation = pRotation;
         $$4.wallType = "wall_flat";
         WoodlandMansionPieces.PlacementData $$5 = new WoodlandMansionPieces.PlacementData();
         this.entrance(pPieces, $$4);
         $$5.position = $$4.position.above(8);
         $$5.rotation = $$4.rotation;
         $$5.wallType = "wall_window";
         if (!pPieces.isEmpty()) {
         }

         WoodlandMansionPieces.SimpleGrid $$6 = pGrid.baseGrid;
         WoodlandMansionPieces.SimpleGrid $$7 = pGrid.thirdFloorGrid;
         this.startX = pGrid.entranceX + 1;
         this.startY = pGrid.entranceY + 1;
         int $$8 = pGrid.entranceX + 1;
         int $$9 = pGrid.entranceY;
         this.traverseOuterWalls(pPieces, $$4, $$6, Direction.SOUTH, this.startX, this.startY, $$8, $$9);
         this.traverseOuterWalls(pPieces, $$5, $$6, Direction.SOUTH, this.startX, this.startY, $$8, $$9);
         WoodlandMansionPieces.PlacementData $$10 = new WoodlandMansionPieces.PlacementData();
         $$10.position = $$4.position.above(19);
         $$10.rotation = $$4.rotation;
         $$10.wallType = "wall_window";
         boolean $$11 = false;

         for (int $$12 = 0; $$12 < $$7.height && !$$11; $$12++) {
            for (int $$13 = $$7.width - 1; $$13 >= 0 && !$$11; $$13--) {
               if (WoodlandMansionPieces.MansionGrid.isHouse($$7, $$13, $$12)) {
                  $$10.position = $$10.position.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$12 - this.startY) * 8);
                  $$10.position = $$10.position.relative(pRotation.rotate(Direction.EAST), ($$13 - this.startX) * 8);
                  this.traverseWallPiece(pPieces, $$10);
                  this.traverseOuterWalls(pPieces, $$10, $$7, Direction.SOUTH, $$13, $$12, $$13, $$12);
                  $$11 = true;
               }
            }
         }

         this.createRoof(pPieces, pPos.above(16), pRotation, $$6, $$7);
         this.createRoof(pPieces, pPos.above(27), pRotation, $$7, null);
         if (!pPieces.isEmpty()) {
         }

         WoodlandMansionPieces.FloorRoomCollection[] $$14 = new WoodlandMansionPieces.FloorRoomCollection[]{
            new WoodlandMansionPieces.FirstFloorRoomCollection(),
            new WoodlandMansionPieces.SecondFloorRoomCollection(),
            new WoodlandMansionPieces.ThirdFloorRoomCollection()
         };

         for (int $$15 = 0; $$15 < 3; $$15++) {
            BlockPos $$16 = pPos.above(8 * $$15 + ($$15 == 2 ? 3 : 0));
            WoodlandMansionPieces.SimpleGrid $$17 = pGrid.floorRooms[$$15];
            WoodlandMansionPieces.SimpleGrid $$18 = $$15 == 2 ? $$7 : $$6;
            String $$19 = $$15 == 0 ? "carpet_south_1" : "carpet_south_2";
            String $$20 = $$15 == 0 ? "carpet_west_1" : "carpet_west_2";

            for (int $$21 = 0; $$21 < $$18.height; $$21++) {
               for (int $$22 = 0; $$22 < $$18.width; $$22++) {
                  if ($$18.get($$22, $$21) == 1) {
                     BlockPos $$23 = $$16.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$21 - this.startY) * 8);
                     $$23 = $$23.relative(pRotation.rotate(Direction.EAST), ($$22 - this.startX) * 8);
                     pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "corridor_floor", $$23, pRotation));
                     if ($$18.get($$22, $$21 - 1) == 1 || ($$17.get($$22, $$21 - 1) & 8388608) == 8388608) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "carpet_north", $$23.relative(pRotation.rotate(Direction.EAST), 1).above(), pRotation
                           )
                        );
                     }

                     if ($$18.get($$22 + 1, $$21) == 1 || ($$17.get($$22 + 1, $$21) & 8388608) == 8388608) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              "carpet_east",
                              $$23.relative(pRotation.rotate(Direction.SOUTH), 1).relative(pRotation.rotate(Direction.EAST), 5).above(),
                              pRotation
                           )
                        );
                     }

                     if ($$18.get($$22, $$21 + 1) == 1 || ($$17.get($$22, $$21 + 1) & 8388608) == 8388608) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              $$19,
                              $$23.relative(pRotation.rotate(Direction.SOUTH), 5).relative(pRotation.rotate(Direction.WEST), 1),
                              pRotation
                           )
                        );
                     }

                     if ($$18.get($$22 - 1, $$21) == 1 || ($$17.get($$22 - 1, $$21) & 8388608) == 8388608) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              $$20,
                              $$23.relative(pRotation.rotate(Direction.WEST), 1).relative(pRotation.rotate(Direction.NORTH), 1),
                              pRotation
                           )
                        );
                     }
                  }
               }
            }

            String $$24 = $$15 == 0 ? "indoors_wall_1" : "indoors_wall_2";
            String $$25 = $$15 == 0 ? "indoors_door_1" : "indoors_door_2";
            List<Direction> $$26 = Lists.newArrayList();

            for (int $$27 = 0; $$27 < $$18.height; $$27++) {
               for (int $$28 = 0; $$28 < $$18.width; $$28++) {
                  boolean $$29 = $$15 == 2 && $$18.get($$28, $$27) == 3;
                  if ($$18.get($$28, $$27) == 2 || $$29) {
                     int $$30 = $$17.get($$28, $$27);
                     int $$31 = $$30 & 983040;
                     int $$32 = $$30 & 65535;
                     $$29 = $$29 && ($$30 & 8388608) == 8388608;
                     $$26.clear();
                     if (($$30 & 2097152) == 2097152) {
                        for (Direction $$33 : Direction.Plane.HORIZONTAL) {
                           if ($$18.get($$28 + $$33.getStepX(), $$27 + $$33.getStepZ()) == 1) {
                              $$26.add($$33);
                           }
                        }
                     }

                     Direction $$34 = null;
                     if (!$$26.isEmpty()) {
                        $$34 = $$26.get(this.random.nextInt($$26.size()));
                     } else if (($$30 & 1048576) == 1048576) {
                        $$34 = Direction.UP;
                     }

                     BlockPos $$35 = $$16.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$27 - this.startY) * 8);
                     $$35 = $$35.relative(pRotation.rotate(Direction.EAST), -1 + ($$28 - this.startX) * 8);
                     if (WoodlandMansionPieces.MansionGrid.isHouse($$18, $$28 - 1, $$27) && !pGrid.isRoomId($$18, $$28 - 1, $$27, $$15, $$32)) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$34 == Direction.WEST ? $$25 : $$24, $$35, pRotation)
                        );
                     }

                     if ($$18.get($$28 + 1, $$27) == 1 && !$$29) {
                        BlockPos $$36 = $$35.relative(pRotation.rotate(Direction.EAST), 8);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$34 == Direction.EAST ? $$25 : $$24, $$36, pRotation)
                        );
                     }

                     if (WoodlandMansionPieces.MansionGrid.isHouse($$18, $$28, $$27 + 1) && !pGrid.isRoomId($$18, $$28, $$27 + 1, $$15, $$32)) {
                        BlockPos $$37 = $$35.relative(pRotation.rotate(Direction.SOUTH), 7);
                        $$37 = $$37.relative(pRotation.rotate(Direction.EAST), 7);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, $$34 == Direction.SOUTH ? $$25 : $$24, $$37, pRotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if ($$18.get($$28, $$27 - 1) == 1 && !$$29) {
                        BlockPos $$38 = $$35.relative(pRotation.rotate(Direction.NORTH), 1);
                        $$38 = $$38.relative(pRotation.rotate(Direction.EAST), 7);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, $$34 == Direction.NORTH ? $$25 : $$24, $$38, pRotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if ($$31 == 65536) {
                        this.addRoom1x1(pPieces, $$35, pRotation, $$34, $$14[$$15]);
                     } else if ($$31 == 131072 && $$34 != null) {
                        Direction $$39 = pGrid.get1x2RoomDirection($$18, $$28, $$27, $$15, $$32);
                        boolean $$40 = ($$30 & 4194304) == 4194304;
                        this.addRoom1x2(pPieces, $$35, pRotation, $$39, $$34, $$14[$$15], $$40);
                     } else if ($$31 == 262144 && $$34 != null && $$34 != Direction.UP) {
                        Direction $$41 = $$34.getClockWise();
                        if (!pGrid.isRoomId($$18, $$28 + $$41.getStepX(), $$27 + $$41.getStepZ(), $$15, $$32)) {
                           $$41 = $$41.getOpposite();
                        }

                        this.addRoom2x2(pPieces, $$35, pRotation, $$41, $$34, $$14[$$15]);
                     } else if ($$31 == 262144 && $$34 == Direction.UP) {
                        this.addRoom2x2Secret(pPieces, $$35, pRotation, $$14[$$15]);
                     }
                  }
               }
            }
         }
      }

      private void traverseOuterWalls(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
         WoodlandMansionPieces.PlacementData pData,
         WoodlandMansionPieces.SimpleGrid pLayout,
         Direction pDirection,
         int pStartX,
         int pStartY,
         int pEntranceX,
         int pEntranceY
      ) {
         int $$8 = pStartX;
         int $$9 = pStartY;
         Direction $$10 = pDirection;

         do {
            if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$8 + pDirection.getStepX(), $$9 + pDirection.getStepZ())) {
               this.traverseTurn(pPieces, pData);
               pDirection = pDirection.getClockWise();
               if ($$8 != pEntranceX || $$9 != pEntranceY || $$10 != pDirection) {
                  this.traverseWallPiece(pPieces, pData);
               }
            } else if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$8 + pDirection.getStepX(), $$9 + pDirection.getStepZ())
               && WoodlandMansionPieces.MansionGrid.isHouse(
                  pLayout,
                  $$8 + pDirection.getStepX() + pDirection.getCounterClockWise().getStepX(),
                  $$9 + pDirection.getStepZ() + pDirection.getCounterClockWise().getStepZ()
               )) {
               this.traverseInnerTurn(pPieces, pData);
               $$8 += pDirection.getStepX();
               $$9 += pDirection.getStepZ();
               pDirection = pDirection.getCounterClockWise();
            } else {
               $$8 += pDirection.getStepX();
               $$9 += pDirection.getStepZ();
               if ($$8 != pEntranceX || $$9 != pEntranceY || $$10 != pDirection) {
                  this.traverseWallPiece(pPieces, pData);
               }
            }
         } while ($$8 != pEntranceX || $$9 != pEntranceY || $$10 != pDirection);
      }

      private void createRoof(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
         BlockPos pPos,
         Rotation pRotation,
         WoodlandMansionPieces.SimpleGrid pLayout,
         @Nullable WoodlandMansionPieces.SimpleGrid pNextFloorLayout
      ) {
         for (int $$5 = 0; $$5 < pLayout.height; $$5++) {
            for (int $$6 = 0; $$6 < pLayout.width; $$6++) {
               BlockPos $$27 = pPos.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$5 - this.startY) * 8);
               $$27 = $$27.relative(pRotation.rotate(Direction.EAST), ($$6 - this.startX) * 8);
               boolean $$8 = pNextFloorLayout != null && WoodlandMansionPieces.MansionGrid.isHouse(pNextFloorLayout, $$6, $$5);
               if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$6, $$5) && !$$8) {
                  pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof", $$27.above(3), pRotation));
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$6 + 1, $$5)) {
                     BlockPos $$9 = $$27.relative(pRotation.rotate(Direction.EAST), 6);
                     pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_front", $$9, pRotation));
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$6 - 1, $$5)) {
                     BlockPos $$10 = $$27.relative(pRotation.rotate(Direction.EAST), 0);
                     $$10 = $$10.relative(pRotation.rotate(Direction.SOUTH), 7);
                     pPieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$10, pRotation.getRotated(Rotation.CLOCKWISE_180)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$6, $$5 - 1)) {
                     BlockPos $$11 = $$27.relative(pRotation.rotate(Direction.WEST), 1);
                     pPieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$11, pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$6, $$5 + 1)) {
                     BlockPos $$12 = $$27.relative(pRotation.rotate(Direction.EAST), 6);
                     $$12 = $$12.relative(pRotation.rotate(Direction.SOUTH), 6);
                     pPieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", $$12, pRotation.getRotated(Rotation.CLOCKWISE_90)
                        )
                     );
                  }
               }
            }
         }

         if (pNextFloorLayout != null) {
            for (int $$13 = 0; $$13 < pLayout.height; $$13++) {
               for (int $$14 = 0; $$14 < pLayout.width; $$14++) {
                  BlockPos var17 = pPos.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$13 - this.startY) * 8);
                  var17 = var17.relative(pRotation.rotate(Direction.EAST), ($$14 - this.startX) * 8);
                  boolean $$16 = WoodlandMansionPieces.MansionGrid.isHouse(pNextFloorLayout, $$14, $$13);
                  if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13) && $$16) {
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14 + 1, $$13)) {
                        BlockPos $$17 = var17.relative(pRotation.rotate(Direction.EAST), 7);
                        pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall", $$17, pRotation));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14 - 1, $$13)) {
                        BlockPos $$18 = var17.relative(pRotation.rotate(Direction.WEST), 1);
                        $$18 = $$18.relative(pRotation.rotate(Direction.SOUTH), 6);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$18, pRotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 - 1)) {
                        BlockPos $$19 = var17.relative(pRotation.rotate(Direction.WEST), 0);
                        $$19 = $$19.relative(pRotation.rotate(Direction.NORTH), 1);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$19, pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 + 1)) {
                        BlockPos $$20 = var17.relative(pRotation.rotate(Direction.EAST), 6);
                        $$20 = $$20.relative(pRotation.rotate(Direction.SOUTH), 7);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", $$20, pRotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14 + 1, $$13)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 - 1)) {
                           BlockPos $$21 = var17.relative(pRotation.rotate(Direction.EAST), 7);
                           $$21 = $$21.relative(pRotation.rotate(Direction.NORTH), 2);
                           pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall_corner", $$21, pRotation));
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 + 1)) {
                           BlockPos $$22 = var17.relative(pRotation.rotate(Direction.EAST), 8);
                           $$22 = $$22.relative(pRotation.rotate(Direction.SOUTH), 7);
                           pPieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$22, pRotation.getRotated(Rotation.CLOCKWISE_90)
                              )
                           );
                        }
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14 - 1, $$13)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 - 1)) {
                           BlockPos $$23 = var17.relative(pRotation.rotate(Direction.WEST), 2);
                           $$23 = $$23.relative(pRotation.rotate(Direction.NORTH), 1);
                           pPieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$23, pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                              )
                           );
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$14, $$13 + 1)) {
                           BlockPos $$24 = var17.relative(pRotation.rotate(Direction.WEST), 1);
                           $$24 = $$24.relative(pRotation.rotate(Direction.SOUTH), 8);
                           pPieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", $$24, pRotation.getRotated(Rotation.CLOCKWISE_180)
                              )
                           );
                        }
                     }
                  }
               }
            }
         }

         for (int $$25 = 0; $$25 < pLayout.height; $$25++) {
            for (int $$26 = 0; $$26 < pLayout.width; $$26++) {
               BlockPos var19 = pPos.relative(pRotation.rotate(Direction.SOUTH), 8 + ($$25 - this.startY) * 8);
               var19 = var19.relative(pRotation.rotate(Direction.EAST), ($$26 - this.startX) * 8);
               boolean $$28 = pNextFloorLayout != null && WoodlandMansionPieces.MansionGrid.isHouse(pNextFloorLayout, $$26, $$25);
               if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26, $$25) && !$$28) {
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 + 1, $$25)) {
                     BlockPos $$29 = var19.relative(pRotation.rotate(Direction.EAST), 6);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26, $$25 + 1)) {
                        BlockPos $$30 = $$29.relative(pRotation.rotate(Direction.SOUTH), 6);
                        pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_corner", $$30, pRotation));
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 + 1, $$25 + 1)) {
                        BlockPos $$31 = $$29.relative(pRotation.rotate(Direction.SOUTH), 5);
                        pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_inner_corner", $$31, pRotation));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26, $$25 - 1)) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$29, pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 + 1, $$25 - 1)) {
                        BlockPos $$32 = var19.relative(pRotation.rotate(Direction.EAST), 9);
                        $$32 = $$32.relative(pRotation.rotate(Direction.NORTH), 2);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$32, pRotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 - 1, $$25)) {
                     BlockPos $$33 = var19.relative(pRotation.rotate(Direction.EAST), 0);
                     $$33 = $$33.relative(pRotation.rotate(Direction.SOUTH), 0);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26, $$25 + 1)) {
                        BlockPos $$34 = $$33.relative(pRotation.rotate(Direction.SOUTH), 6);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$34, pRotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 - 1, $$25 + 1)) {
                        BlockPos $$35 = $$33.relative(pRotation.rotate(Direction.SOUTH), 8);
                        $$35 = $$35.relative(pRotation.rotate(Direction.WEST), 3);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$35, pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26, $$25 - 1)) {
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", $$33, pRotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(pLayout, $$26 - 1, $$25 - 1)) {
                        BlockPos $$36 = $$33.relative(pRotation.rotate(Direction.SOUTH), 1);
                        pPieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", $$36, pRotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }
                  }
               }
            }
         }
      }

      private void entrance(List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, WoodlandMansionPieces.PlacementData pData) {
         Direction $$2 = pData.rotation.rotate(Direction.WEST);
         pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "entrance", pData.position.relative($$2, 9), pData.rotation));
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.SOUTH), 16);
      }

      private void traverseWallPiece(List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, WoodlandMansionPieces.PlacementData pData) {
         pPieces.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(
               this.structureTemplateManager, pData.wallType, pData.position.relative(pData.rotation.rotate(Direction.EAST), 7), pData.rotation
            )
         );
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.SOUTH), 8);
      }

      private void traverseTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, WoodlandMansionPieces.PlacementData pData) {
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.SOUTH), -1);
         pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "wall_corner", pData.position, pData.rotation));
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.SOUTH), -7);
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.WEST), -6);
         pData.rotation = pData.rotation.getRotated(Rotation.CLOCKWISE_90);
      }

      private void traverseInnerTurn(List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, WoodlandMansionPieces.PlacementData pData) {
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.SOUTH), 6);
         pData.position = pData.position.relative(pData.rotation.rotate(Direction.EAST), 8);
         pData.rotation = pData.rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
      }

      private void addRoom1x1(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
         BlockPos pPos,
         Rotation pRotation,
         Direction pDirection,
         WoodlandMansionPieces.FloorRoomCollection pFloorRooms
      ) {
         Rotation $$5 = Rotation.NONE;
         String $$6 = pFloorRooms.get1x1(this.random);
         if (pDirection != Direction.EAST) {
            if (pDirection == Direction.NORTH) {
               $$5 = $$5.getRotated(Rotation.COUNTERCLOCKWISE_90);
            } else if (pDirection == Direction.WEST) {
               $$5 = $$5.getRotated(Rotation.CLOCKWISE_180);
            } else if (pDirection == Direction.SOUTH) {
               $$5 = $$5.getRotated(Rotation.CLOCKWISE_90);
            } else {
               $$6 = pFloorRooms.get1x1Secret(this.random);
            }
         }

         BlockPos $$7 = StructureTemplate.getZeroPositionWithTransform(new BlockPos(1, 0, 0), Mirror.NONE, $$5, 7, 7);
         $$5 = $$5.getRotated(pRotation);
         $$7 = $$7.rotate(pRotation);
         BlockPos $$8 = pPos.offset($$7.getX(), 0, $$7.getZ());
         pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, $$6, $$8, $$5));
      }

      private void addRoom1x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
         BlockPos pPos,
         Rotation pRotation,
         Direction pFrontDirection,
         Direction pSideDirection,
         WoodlandMansionPieces.FloorRoomCollection pFloorRooms,
         boolean pIsStairs
      ) {
         if (pSideDirection == Direction.EAST && pFrontDirection == Direction.SOUTH) {
            BlockPos $$7 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2SideEntrance(this.random, pIsStairs), $$7, pRotation
               )
            );
         } else if (pSideDirection == Direction.EAST && pFrontDirection == Direction.NORTH) {
            BlockPos $$8 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            $$8 = $$8.relative(pRotation.rotate(Direction.SOUTH), 6);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2SideEntrance(this.random, pIsStairs), $$8, pRotation, Mirror.LEFT_RIGHT
               )
            );
         } else if (pSideDirection == Direction.WEST && pFrontDirection == Direction.NORTH) {
            BlockPos $$9 = pPos.relative(pRotation.rotate(Direction.EAST), 7);
            $$9 = $$9.relative(pRotation.rotate(Direction.SOUTH), 6);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2SideEntrance(this.random, pIsStairs), $$9, pRotation.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (pSideDirection == Direction.WEST && pFrontDirection == Direction.SOUTH) {
            BlockPos $$10 = pPos.relative(pRotation.rotate(Direction.EAST), 7);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2SideEntrance(this.random, pIsStairs), $$10, pRotation, Mirror.FRONT_BACK
               )
            );
         } else if (pSideDirection == Direction.SOUTH && pFrontDirection == Direction.EAST) {
            BlockPos $$11 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  pFloorRooms.get1x2SideEntrance(this.random, pIsStairs),
                  $$11,
                  pRotation.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.LEFT_RIGHT
               )
            );
         } else if (pSideDirection == Direction.SOUTH && pFrontDirection == Direction.WEST) {
            BlockPos $$12 = pPos.relative(pRotation.rotate(Direction.EAST), 7);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2SideEntrance(this.random, pIsStairs), $$12, pRotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (pSideDirection == Direction.NORTH && pFrontDirection == Direction.WEST) {
            BlockPos $$13 = pPos.relative(pRotation.rotate(Direction.EAST), 7);
            $$13 = $$13.relative(pRotation.rotate(Direction.SOUTH), 6);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  pFloorRooms.get1x2SideEntrance(this.random, pIsStairs),
                  $$13,
                  pRotation.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.FRONT_BACK
               )
            );
         } else if (pSideDirection == Direction.NORTH && pFrontDirection == Direction.EAST) {
            BlockPos $$14 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            $$14 = $$14.relative(pRotation.rotate(Direction.SOUTH), 6);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  pFloorRooms.get1x2SideEntrance(this.random, pIsStairs),
                  $$14,
                  pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (pSideDirection == Direction.SOUTH && pFrontDirection == Direction.NORTH) {
            BlockPos $$15 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            $$15 = $$15.relative(pRotation.rotate(Direction.NORTH), 8);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2FrontEntrance(this.random, pIsStairs), $$15, pRotation
               )
            );
         } else if (pSideDirection == Direction.NORTH && pFrontDirection == Direction.SOUTH) {
            BlockPos $$16 = pPos.relative(pRotation.rotate(Direction.EAST), 7);
            $$16 = $$16.relative(pRotation.rotate(Direction.SOUTH), 14);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2FrontEntrance(this.random, pIsStairs), $$16, pRotation.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (pSideDirection == Direction.WEST && pFrontDirection == Direction.EAST) {
            BlockPos $$17 = pPos.relative(pRotation.rotate(Direction.EAST), 15);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2FrontEntrance(this.random, pIsStairs), $$17, pRotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (pSideDirection == Direction.EAST && pFrontDirection == Direction.WEST) {
            BlockPos $$18 = pPos.relative(pRotation.rotate(Direction.WEST), 7);
            $$18 = $$18.relative(pRotation.rotate(Direction.SOUTH), 6);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  pFloorRooms.get1x2FrontEntrance(this.random, pIsStairs),
                  $$18,
                  pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (pSideDirection == Direction.UP && pFrontDirection == Direction.EAST) {
            BlockPos $$19 = pPos.relative(pRotation.rotate(Direction.EAST), 15);
            pPieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, pFloorRooms.get1x2Secret(this.random), $$19, pRotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (pSideDirection == Direction.UP && pFrontDirection == Direction.SOUTH) {
            BlockPos $$20 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
            $$20 = $$20.relative(pRotation.rotate(Direction.NORTH), 0);
            pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, pFloorRooms.get1x2Secret(this.random), $$20, pRotation));
         }
      }

      private void addRoom2x2(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces,
         BlockPos pPos,
         Rotation pRotation,
         Direction pFrontDirection,
         Direction pSideDirection,
         WoodlandMansionPieces.FloorRoomCollection pFloorRooms
      ) {
         int $$6 = 0;
         int $$7 = 0;
         Rotation $$8 = pRotation;
         Mirror $$9 = Mirror.NONE;
         if (pSideDirection == Direction.EAST && pFrontDirection == Direction.SOUTH) {
            $$6 = -7;
         } else if (pSideDirection == Direction.EAST && pFrontDirection == Direction.NORTH) {
            $$6 = -7;
            $$7 = 6;
            $$9 = Mirror.LEFT_RIGHT;
         } else if (pSideDirection == Direction.NORTH && pFrontDirection == Direction.EAST) {
            $$6 = 1;
            $$7 = 14;
            $$8 = pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
         } else if (pSideDirection == Direction.NORTH && pFrontDirection == Direction.WEST) {
            $$6 = 7;
            $$7 = 14;
            $$8 = pRotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
            $$9 = Mirror.LEFT_RIGHT;
         } else if (pSideDirection == Direction.SOUTH && pFrontDirection == Direction.WEST) {
            $$6 = 7;
            $$7 = -8;
            $$8 = pRotation.getRotated(Rotation.CLOCKWISE_90);
         } else if (pSideDirection == Direction.SOUTH && pFrontDirection == Direction.EAST) {
            $$6 = 1;
            $$7 = -8;
            $$8 = pRotation.getRotated(Rotation.CLOCKWISE_90);
            $$9 = Mirror.LEFT_RIGHT;
         } else if (pSideDirection == Direction.WEST && pFrontDirection == Direction.NORTH) {
            $$6 = 15;
            $$7 = 6;
            $$8 = pRotation.getRotated(Rotation.CLOCKWISE_180);
         } else if (pSideDirection == Direction.WEST && pFrontDirection == Direction.SOUTH) {
            $$6 = 15;
            $$9 = Mirror.FRONT_BACK;
         }

         BlockPos $$10 = pPos.relative(pRotation.rotate(Direction.EAST), $$6);
         $$10 = $$10.relative(pRotation.rotate(Direction.SOUTH), $$7);
         pPieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, pFloorRooms.get2x2(this.random), $$10, $$8, $$9));
      }

      private void addRoom2x2Secret(
         List<WoodlandMansionPieces.WoodlandMansionPiece> pPieces, BlockPos pPos, Rotation pRotation, WoodlandMansionPieces.FloorRoomCollection pFloorRooms
      ) {
         BlockPos $$4 = pPos.relative(pRotation.rotate(Direction.EAST), 1);
         pPieces.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, pFloorRooms.get2x2Secret(this.random), $$4, pRotation, Mirror.NONE)
         );
      }
   }

   static class PlacementData {
      public Rotation rotation;
      public BlockPos position;
      public String wallType;
   }

   static class SecondFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(RandomSource p_230144_) {
         return "1x1_b" + (p_230144_.nextInt(4) + 1);
      }

      @Override
      public String get1x1Secret(RandomSource p_230149_) {
         return "1x1_as" + (p_230149_.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(RandomSource p_230146_, boolean p_230147_) {
         return p_230147_ ? "1x2_c_stairs" : "1x2_c" + (p_230146_.nextInt(4) + 1);
      }

      @Override
      public String get1x2FrontEntrance(RandomSource p_230151_, boolean p_230152_) {
         return p_230152_ ? "1x2_d_stairs" : "1x2_d" + (p_230151_.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(RandomSource p_230154_) {
         return "1x2_se" + (p_230154_.nextInt(1) + 1);
      }

      @Override
      public String get2x2(RandomSource p_230156_) {
         return "2x2_b" + (p_230156_.nextInt(5) + 1);
      }

      @Override
      public String get2x2Secret(RandomSource p_230158_) {
         return "2x2_s1";
      }
   }

   static class SimpleGrid {
      private final int[][] grid;
      final int width;
      final int height;
      private final int valueIfOutside;

      public SimpleGrid(int pWidth, int pHeight, int pValueIfOutside) {
         this.width = pWidth;
         this.height = pHeight;
         this.valueIfOutside = pValueIfOutside;
         this.grid = new int[pWidth][pHeight];
      }

      public void set(int pX, int pY, int pValue) {
         if (pX >= 0 && pX < this.width && pY >= 0 && pY < this.height) {
            this.grid[pX][pY] = pValue;
         }
      }

      public void set(int pMinX, int pMinY, int pMaxX, int pMaxY, int pValue) {
         for (int $$5 = pMinY; $$5 <= pMaxY; $$5++) {
            for (int $$6 = pMinX; $$6 <= pMaxX; $$6++) {
               this.set($$6, $$5, pValue);
            }
         }
      }

      public int get(int pX, int pY) {
         return pX >= 0 && pX < this.width && pY >= 0 && pY < this.height ? this.grid[pX][pY] : this.valueIfOutside;
      }

      public void setif(int pX, int pY, int pOldValue, int pNewValue) {
         if (this.get(pX, pY) == pOldValue) {
            this.set(pX, pY, pNewValue);
         }
      }

      public boolean edgesTo(int pX, int pY, int pExpectedValue) {
         return this.get(pX - 1, pY) == pExpectedValue
            || this.get(pX + 1, pY) == pExpectedValue
            || this.get(pX, pY + 1) == pExpectedValue
            || this.get(pX, pY - 1) == pExpectedValue;
      }
   }

   static class ThirdFloorRoomCollection extends WoodlandMansionPieces.SecondFloorRoomCollection {
   }

   public static class WoodlandMansionPiece extends TemplateStructurePiece {
      public WoodlandMansionPiece(StructureTemplateManager pStructureTemplateManager, String pTemplateName, BlockPos pTemplatePosition, Rotation pRotation) {
         this(pStructureTemplateManager, pTemplateName, pTemplatePosition, pRotation, Mirror.NONE);
      }

      public WoodlandMansionPiece(
         StructureTemplateManager pStructureTemplateManager, String pTemplateName, BlockPos pTemplatePosition, Rotation pRotation, Mirror pMirror
      ) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            0,
            pStructureTemplateManager,
            makeLocation(pTemplateName),
            pTemplateName,
            makeSettings(pMirror, pRotation),
            pTemplatePosition
         );
      }

      public WoodlandMansionPiece(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            pTag,
            pStructureTemplateManager,
            p_230220_ -> makeSettings(Mirror.valueOf(pTag.getString("Mi")), Rotation.valueOf(pTag.getString("Rot")))
         );
      }

      @Override
      protected ResourceLocation makeTemplateLocation() {
         return makeLocation(this.templateName);
      }

      private static ResourceLocation makeLocation(String pName) {
         return ResourceLocation.withDefaultNamespace("woodland_mansion/" + pName);
      }

      private static StructurePlaceSettings makeSettings(Mirror pMirror, Rotation pRotation) {
         return new StructurePlaceSettings()
            .setIgnoreEntities(true)
            .setRotation(pRotation)
            .setMirror(pMirror)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
         pTag.putString("Mi", this.placeSettings.getMirror().name());
      }

      @Override
      protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (pName.startsWith("Chest")) {
            Rotation $$5 = this.placeSettings.getRotation();
            BlockState $$6 = Blocks.CHEST.defaultBlockState();
            if ("ChestWest".equals(pName)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.WEST));
            } else if ("ChestEast".equals(pName)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.EAST));
            } else if ("ChestSouth".equals(pName)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.SOUTH));
            } else if ("ChestNorth".equals(pName)) {
               $$6 = $$6.setValue(ChestBlock.FACING, $$5.rotate(Direction.NORTH));
            }

            this.createChest(pLevel, pBox, pRandom, pPos, BuiltInLootTables.WOODLAND_MANSION, $$6);
         } else {
            List<Mob> $$7 = new ArrayList<>();
            switch (pName) {
               case "Mage":
                  $$7.add(EntityType.EVOKER.create(pLevel.getLevel()));
                  break;
               case "Warrior":
                  $$7.add(EntityType.VINDICATOR.create(pLevel.getLevel()));
                  break;
               case "Group of Allays":
                  int $$8 = pLevel.getRandom().nextInt(3) + 1;

                  for (int $$9 = 0; $$9 < $$8; $$9++) {
                     $$7.add(EntityType.ALLAY.create(pLevel.getLevel()));
                  }
                  break;
               default:
                  return;
            }

            for (Mob $$10 : $$7) {
               if ($$10 != null) {
                  $$10.setPersistenceRequired();
                  $$10.moveTo(pPos, 0.0F, 0.0F);
                  $$10.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt($$10.blockPosition()), MobSpawnType.STRUCTURE, null);
                  pLevel.addFreshEntityWithPassengers($$10);
                  pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }
      }
   }
}
