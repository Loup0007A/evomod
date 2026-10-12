package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class OceanMonumentPieces {
   private OceanMonumentPieces() {
   }

   static class FitDoubleXRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228592_) {
         return p_228592_.hasOpening[Direction.EAST.get3DDataValue()] && !p_228592_.connections[Direction.EAST.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228594_, OceanMonumentPieces.RoomDefinition p_228595_, RandomSource p_228596_) {
         p_228595_.claimed = true;
         p_228595_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXRoom(p_228594_, p_228595_);
      }
   }

   static class FitDoubleXYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228599_) {
         if (p_228599_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228599_.connections[Direction.EAST.get3DDataValue()].claimed
            && p_228599_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228599_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition $$1 = p_228599_.connections[Direction.EAST.get3DDataValue()];
            return $$1.hasOpening[Direction.UP.get3DDataValue()] && !$$1.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228601_, OceanMonumentPieces.RoomDefinition p_228602_, RandomSource p_228603_) {
         p_228602_.claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXYRoom(p_228601_, p_228602_);
      }
   }

   static class FitDoubleYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228606_) {
         return p_228606_.hasOpening[Direction.UP.get3DDataValue()] && !p_228606_.connections[Direction.UP.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228608_, OceanMonumentPieces.RoomDefinition p_228609_, RandomSource p_228610_) {
         p_228609_.claimed = true;
         p_228609_.connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYRoom(p_228608_, p_228609_);
      }
   }

   static class FitDoubleYZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228613_) {
         if (p_228613_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228613_.connections[Direction.NORTH.get3DDataValue()].claimed
            && p_228613_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228613_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition $$1 = p_228613_.connections[Direction.NORTH.get3DDataValue()];
            return $$1.hasOpening[Direction.UP.get3DDataValue()] && !$$1.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228615_, OceanMonumentPieces.RoomDefinition p_228616_, RandomSource p_228617_) {
         p_228616_.claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYZRoom(p_228615_, p_228616_);
      }
   }

   static class FitDoubleZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228620_) {
         return p_228620_.hasOpening[Direction.NORTH.get3DDataValue()] && !p_228620_.connections[Direction.NORTH.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228622_, OceanMonumentPieces.RoomDefinition p_228623_, RandomSource p_228624_) {
         OceanMonumentPieces.RoomDefinition $$3 = p_228623_;
         if (!p_228623_.hasOpening[Direction.NORTH.get3DDataValue()] || p_228623_.connections[Direction.NORTH.get3DDataValue()].claimed) {
            $$3 = p_228623_.connections[Direction.SOUTH.get3DDataValue()];
         }

         $$3.claimed = true;
         $$3.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleZRoom(p_228622_, $$3);
      }
   }

   static class FitSimpleRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228627_) {
         return true;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228629_, OceanMonumentPieces.RoomDefinition p_228630_, RandomSource p_228631_) {
         p_228630_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleRoom(p_228629_, p_228630_, p_228631_);
      }
   }

   static class FitSimpleTopRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228634_) {
         return !p_228634_.hasOpening[Direction.WEST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.SOUTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.UP.get3DDataValue()];
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228636_, OceanMonumentPieces.RoomDefinition p_228637_, RandomSource p_228638_) {
         p_228637_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleTopRoom(p_228636_, p_228637_);
      }
   }

   public static class MonumentBuilding extends OceanMonumentPieces.OceanMonumentPiece {
      private static final int WIDTH = 58;
      private static final int HEIGHT = 22;
      private static final int DEPTH = 58;
      public static final int BIOME_RANGE_CHECK = 29;
      private static final int TOP_POSITION = 61;
      private OceanMonumentPieces.RoomDefinition sourceRoom;
      private OceanMonumentPieces.RoomDefinition coreRoom;
      private final List<OceanMonumentPieces.OceanMonumentPiece> childPieces = Lists.newArrayList();

      public MonumentBuilding(RandomSource pRandom, int pX, int pZ, Direction pOrientation) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, pOrientation, 0, makeBoundingBox(pX, 39, pZ, pOrientation, 58, 23, 58));
         this.setOrientation(pOrientation);
         List<OceanMonumentPieces.RoomDefinition> $$4 = this.generateRoomGraph(pRandom);
         this.sourceRoom.claimed = true;
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentEntryRoom(pOrientation, this.sourceRoom));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentCoreRoom(pOrientation, this.coreRoom));
         List<OceanMonumentPieces.MonumentRoomFitter> $$5 = Lists.newArrayList();
         $$5.add(new OceanMonumentPieces.FitDoubleXYRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleYZRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleZRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleXRoom());
         $$5.add(new OceanMonumentPieces.FitDoubleYRoom());
         $$5.add(new OceanMonumentPieces.FitSimpleTopRoom());
         $$5.add(new OceanMonumentPieces.FitSimpleRoom());

         for (OceanMonumentPieces.RoomDefinition $$6 : $$4) {
            if (!$$6.claimed && !$$6.isSpecial()) {
               for (OceanMonumentPieces.MonumentRoomFitter $$7 : $$5) {
                  if ($$7.fits($$6)) {
                     this.childPieces.add($$7.create(pOrientation, $$6, pRandom));
                     break;
                  }
               }
            }
         }

         BlockPos $$8 = this.getWorldPos(9, 0, 22);

         for (OceanMonumentPieces.OceanMonumentPiece $$9 : this.childPieces) {
            $$9.getBoundingBox().move($$8);
         }

         BoundingBox $$10 = BoundingBox.fromCorners(this.getWorldPos(1, 1, 1), this.getWorldPos(23, 8, 21));
         BoundingBox $$11 = BoundingBox.fromCorners(this.getWorldPos(34, 1, 1), this.getWorldPos(56, 8, 21));
         BoundingBox $$12 = BoundingBox.fromCorners(this.getWorldPos(22, 13, 22), this.getWorldPos(35, 17, 35));
         int $$13 = pRandom.nextInt();
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(pOrientation, $$10, $$13++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(pOrientation, $$11, $$13++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentPenthouse(pOrientation, $$12));
      }

      public MonumentBuilding(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, pTag);
      }

      private List<OceanMonumentPieces.RoomDefinition> generateRoomGraph(RandomSource pRandom) {
         OceanMonumentPieces.RoomDefinition[] $$1 = new OceanMonumentPieces.RoomDefinition[75];

         for (int $$2 = 0; $$2 < 5; $$2++) {
            for (int $$3 = 0; $$3 < 4; $$3++) {
               int $$4 = 0;
               int $$5 = getRoomIndex($$2, 0, $$3);
               $$1[$$5] = new OceanMonumentPieces.RoomDefinition($$5);
            }
         }

         for (int $$6 = 0; $$6 < 5; $$6++) {
            for (int $$7 = 0; $$7 < 4; $$7++) {
               int $$8 = 1;
               int $$9 = getRoomIndex($$6, 1, $$7);
               $$1[$$9] = new OceanMonumentPieces.RoomDefinition($$9);
            }
         }

         for (int $$10 = 1; $$10 < 4; $$10++) {
            for (int $$11 = 0; $$11 < 2; $$11++) {
               int $$12 = 2;
               int $$13 = getRoomIndex($$10, 2, $$11);
               $$1[$$13] = new OceanMonumentPieces.RoomDefinition($$13);
            }
         }

         this.sourceRoom = $$1[GRIDROOM_SOURCE_INDEX];

         for (int $$14 = 0; $$14 < 5; $$14++) {
            for (int $$15 = 0; $$15 < 5; $$15++) {
               for (int $$16 = 0; $$16 < 3; $$16++) {
                  int $$17 = getRoomIndex($$14, $$16, $$15);
                  if ($$1[$$17] != null) {
                     for (Direction $$18 : Direction.values()) {
                        int $$19 = $$14 + $$18.getStepX();
                        int $$20 = $$16 + $$18.getStepY();
                        int $$21 = $$15 + $$18.getStepZ();
                        if ($$19 >= 0 && $$19 < 5 && $$21 >= 0 && $$21 < 5 && $$20 >= 0 && $$20 < 3) {
                           int $$22 = getRoomIndex($$19, $$20, $$21);
                           if ($$1[$$22] != null) {
                              if ($$21 == $$15) {
                                 $$1[$$17].setConnection($$18, $$1[$$22]);
                              } else {
                                 $$1[$$17].setConnection($$18.getOpposite(), $$1[$$22]);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         OceanMonumentPieces.RoomDefinition $$23 = new OceanMonumentPieces.RoomDefinition(1003);
         OceanMonumentPieces.RoomDefinition $$24 = new OceanMonumentPieces.RoomDefinition(1001);
         OceanMonumentPieces.RoomDefinition $$25 = new OceanMonumentPieces.RoomDefinition(1002);
         $$1[GRIDROOM_TOP_CONNECT_INDEX].setConnection(Direction.UP, $$23);
         $$1[GRIDROOM_LEFTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, $$24);
         $$1[GRIDROOM_RIGHTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, $$25);
         $$23.claimed = true;
         $$24.claimed = true;
         $$25.claimed = true;
         this.sourceRoom.isSource = true;
         this.coreRoom = $$1[getRoomIndex(pRandom.nextInt(4), 0, 2)];
         this.coreRoom.claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         ObjectArrayList<OceanMonumentPieces.RoomDefinition> $$26 = new ObjectArrayList();

         for (OceanMonumentPieces.RoomDefinition $$27 : $$1) {
            if ($$27 != null) {
               $$27.updateOpenings();
               $$26.add($$27);
            }
         }

         $$23.updateOpenings();
         Util.shuffle($$26, pRandom);
         int $$28 = 1;
         ObjectListIterator var34 = $$26.iterator();

         while (var34.hasNext()) {
            OceanMonumentPieces.RoomDefinition $$29 = (OceanMonumentPieces.RoomDefinition)var34.next();
            int $$30 = 0;
            int $$31 = 0;

            while ($$30 < 2 && $$31 < 5) {
               $$31++;
               int $$32 = pRandom.nextInt(6);
               if ($$29.hasOpening[$$32]) {
                  int $$33 = Direction.from3DDataValue($$32).getOpposite().get3DDataValue();
                  $$29.hasOpening[$$32] = false;
                  $$29.connections[$$32].hasOpening[$$33] = false;
                  if ($$29.findSource($$28++) && $$29.connections[$$32].findSource($$28++)) {
                     $$30++;
                  } else {
                     $$29.hasOpening[$$32] = true;
                     $$29.connections[$$32].hasOpening[$$33] = true;
                  }
               }
            }
         }

         $$26.add($$23);
         $$26.add($$24);
         $$26.add($$25);
         return $$26;
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
         int $$7 = Math.max(pLevel.getSeaLevel(), 64) - this.boundingBox.minY();
         this.generateWaterBox(pLevel, pBox, 0, 0, 0, 58, $$7, 58);
         this.generateWing(false, 0, pLevel, pRandom, pBox);
         this.generateWing(true, 33, pLevel, pRandom, pBox);
         this.generateEntranceArchs(pLevel, pRandom, pBox);
         this.generateEntranceWall(pLevel, pRandom, pBox);
         this.generateRoofPiece(pLevel, pRandom, pBox);
         this.generateLowerWall(pLevel, pRandom, pBox);
         this.generateMiddleWall(pLevel, pRandom, pBox);
         this.generateUpperWall(pLevel, pRandom, pBox);

         for (int $$8 = 0; $$8 < 7; $$8++) {
            int $$9 = 0;

            while ($$9 < 7) {
               if ($$9 == 0 && $$8 == 3) {
                  $$9 = 6;
               }

               int $$10 = $$8 * 9;
               int $$11 = $$9 * 9;

               for (int $$12 = 0; $$12 < 4; $$12++) {
                  for (int $$13 = 0; $$13 < 4; $$13++) {
                     this.placeBlock(pLevel, BASE_LIGHT, $$10 + $$12, 0, $$11 + $$13, pBox);
                     this.fillColumnDown(pLevel, BASE_LIGHT, $$10 + $$12, -1, $$11 + $$13, pBox);
                  }
               }

               if ($$8 != 0 && $$8 != 6) {
                  $$9 += 6;
               } else {
                  $$9++;
               }
            }
         }

         for (int $$14 = 0; $$14 < 5; $$14++) {
            this.generateWaterBox(pLevel, pBox, -1 - $$14, 0 + $$14 * 2, -1 - $$14, -1 - $$14, 23, 58 + $$14);
            this.generateWaterBox(pLevel, pBox, 58 + $$14, 0 + $$14 * 2, -1 - $$14, 58 + $$14, 23, 58 + $$14);
            this.generateWaterBox(pLevel, pBox, 0 - $$14, 0 + $$14 * 2, -1 - $$14, 57 + $$14, 23, -1 - $$14);
            this.generateWaterBox(pLevel, pBox, 0 - $$14, 0 + $$14 * 2, 58 + $$14, 57 + $$14, 23, 58 + $$14);
         }

         for (OceanMonumentPieces.OceanMonumentPiece $$15 : this.childPieces) {
            if ($$15.getBoundingBox().intersects(pBox)) {
               $$15.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
            }
         }
      }

      private void generateWing(boolean pWing, int pX, WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         int $$5 = 24;
         if (this.chunkIntersects(pBox, pX, 0, pX + 23, 20)) {
            this.generateBox(pLevel, pBox, pX + 0, 0, 0, pX + 24, 0, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, pX + 0, 1, 0, pX + 24, 10, 20);

            for (int $$6 = 0; $$6 < 4; $$6++) {
               this.generateBox(pLevel, pBox, pX + $$6, $$6 + 1, $$6, pX + $$6, $$6 + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, pX + $$6 + 7, $$6 + 5, $$6 + 7, pX + $$6 + 7, $$6 + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, pX + 17 - $$6, $$6 + 5, $$6 + 7, pX + 17 - $$6, $$6 + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, pX + 24 - $$6, $$6 + 1, $$6, pX + 24 - $$6, $$6 + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, pX + $$6 + 1, $$6 + 1, $$6, pX + 23 - $$6, $$6 + 1, $$6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, pX + $$6 + 8, $$6 + 5, $$6 + 7, pX + 16 - $$6, $$6 + 5, $$6 + 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(pLevel, pBox, pX + 4, 4, 4, pX + 6, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 7, 4, 4, pX + 17, 4, 6, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 18, 4, 4, pX + 20, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 11, 8, 11, pX + 13, 8, 20, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(pLevel, DOT_DECO_DATA, pX + 12, 9, 12, pBox);
            this.placeBlock(pLevel, DOT_DECO_DATA, pX + 12, 9, 15, pBox);
            this.placeBlock(pLevel, DOT_DECO_DATA, pX + 12, 9, 18, pBox);
            int $$7 = pX + (pWing ? 19 : 5);
            int $$8 = pX + (pWing ? 5 : 19);

            for (int $$9 = 20; $$9 >= 5; $$9 -= 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 5, $$9, pBox);
            }

            for (int $$10 = 19; $$10 >= 7; $$10 -= 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, $$8, 5, $$10, pBox);
            }

            for (int $$11 = 0; $$11 < 4; $$11++) {
               int $$12 = pWing ? pX + 24 - (17 - $$11 * 3) : pX + 17 - $$11 * 3;
               this.placeBlock(pLevel, DOT_DECO_DATA, $$12, 5, 5, pBox);
            }

            this.placeBlock(pLevel, DOT_DECO_DATA, $$8, 5, 5, pBox);
            this.generateBox(pLevel, pBox, pX + 11, 1, 12, pX + 13, 7, 12, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 12, 1, 11, pX + 12, 7, 13, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateEntranceArchs(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 22, 5, 35, 17)) {
            this.generateWaterBox(pLevel, pBox, 25, 0, 0, 32, 8, 20);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(pLevel, pBox, 24, 2, 5 + $$3 * 4, 24, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 22, 4, 5 + $$3 * 4, 23, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(pLevel, BASE_LIGHT, 25, 5, 5 + $$3 * 4, pBox);
               this.placeBlock(pLevel, BASE_LIGHT, 26, 6, 5 + $$3 * 4, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, 26, 5, 5 + $$3 * 4, pBox);
               this.generateBox(pLevel, pBox, 33, 2, 5 + $$3 * 4, 33, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 34, 4, 5 + $$3 * 4, 35, 4, 5 + $$3 * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(pLevel, BASE_LIGHT, 32, 5, 5 + $$3 * 4, pBox);
               this.placeBlock(pLevel, BASE_LIGHT, 31, 6, 5 + $$3 * 4, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, 31, 5, 5 + $$3 * 4, pBox);
               this.generateBox(pLevel, pBox, 27, 6, 5 + $$3 * 4, 30, 6, 5 + $$3 * 4, BASE_GRAY, BASE_GRAY, false);
            }
         }
      }

      private void generateEntranceWall(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 15, 20, 42, 21)) {
            this.generateBox(pLevel, pBox, 15, 0, 21, 42, 0, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 26, 1, 21, 31, 3, 21);
            this.generateBox(pLevel, pBox, 21, 12, 21, 36, 12, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 17, 11, 21, 40, 11, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 16, 10, 21, 41, 10, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 15, 7, 21, 42, 9, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 16, 6, 21, 41, 6, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 17, 5, 21, 40, 5, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 21, 4, 21, 36, 4, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 22, 3, 21, 26, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 31, 3, 21, 35, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 23, 2, 21, 25, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 32, 2, 21, 34, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 28, 4, 20, 29, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(pLevel, BASE_LIGHT, 27, 3, 21, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 30, 3, 21, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 26, 2, 21, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 31, 2, 21, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 25, 1, 21, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 32, 1, 21, pBox);

            for (int $$3 = 0; $$3 < 7; $$3++) {
               this.placeBlock(pLevel, BASE_BLACK, 28 - $$3, 6 + $$3, 21, pBox);
               this.placeBlock(pLevel, BASE_BLACK, 29 + $$3, 6 + $$3, 21, pBox);
            }

            for (int $$4 = 0; $$4 < 4; $$4++) {
               this.placeBlock(pLevel, BASE_BLACK, 28 - $$4, 9 + $$4, 21, pBox);
               this.placeBlock(pLevel, BASE_BLACK, 29 + $$4, 9 + $$4, 21, pBox);
            }

            this.placeBlock(pLevel, BASE_BLACK, 28, 12, 21, pBox);
            this.placeBlock(pLevel, BASE_BLACK, 29, 12, 21, pBox);

            for (int $$5 = 0; $$5 < 3; $$5++) {
               this.placeBlock(pLevel, BASE_BLACK, 22 - $$5 * 2, 8, 21, pBox);
               this.placeBlock(pLevel, BASE_BLACK, 22 - $$5 * 2, 9, 21, pBox);
               this.placeBlock(pLevel, BASE_BLACK, 35 + $$5 * 2, 8, 21, pBox);
               this.placeBlock(pLevel, BASE_BLACK, 35 + $$5 * 2, 9, 21, pBox);
            }

            this.generateWaterBox(pLevel, pBox, 15, 13, 21, 42, 15, 21);
            this.generateWaterBox(pLevel, pBox, 15, 1, 21, 15, 6, 21);
            this.generateWaterBox(pLevel, pBox, 16, 1, 21, 16, 5, 21);
            this.generateWaterBox(pLevel, pBox, 17, 1, 21, 20, 4, 21);
            this.generateWaterBox(pLevel, pBox, 21, 1, 21, 21, 3, 21);
            this.generateWaterBox(pLevel, pBox, 22, 1, 21, 22, 2, 21);
            this.generateWaterBox(pLevel, pBox, 23, 1, 21, 24, 1, 21);
            this.generateWaterBox(pLevel, pBox, 42, 1, 21, 42, 6, 21);
            this.generateWaterBox(pLevel, pBox, 41, 1, 21, 41, 5, 21);
            this.generateWaterBox(pLevel, pBox, 37, 1, 21, 40, 4, 21);
            this.generateWaterBox(pLevel, pBox, 36, 1, 21, 36, 3, 21);
            this.generateWaterBox(pLevel, pBox, 33, 1, 21, 34, 1, 21);
            this.generateWaterBox(pLevel, pBox, 35, 1, 21, 35, 2, 21);
         }
      }

      private void generateRoofPiece(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 21, 21, 36, 36)) {
            this.generateBox(pLevel, pBox, 21, 0, 22, 36, 0, 36, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 21, 1, 22, 36, 23, 36);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(pLevel, pBox, 21 + $$3, 13 + $$3, 21 + $$3, 36 - $$3, 13 + $$3, 21 + $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 21 + $$3, 13 + $$3, 36 - $$3, 36 - $$3, 13 + $$3, 36 - $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 21 + $$3, 13 + $$3, 22 + $$3, 21 + $$3, 13 + $$3, 35 - $$3, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 36 - $$3, 13 + $$3, 22 + $$3, 36 - $$3, 13 + $$3, 35 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(pLevel, pBox, 25, 16, 25, 32, 16, 32, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 25, 17, 25, 25, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 32, 17, 25, 32, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 25, 17, 32, 25, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 32, 17, 32, 32, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(pLevel, BASE_LIGHT, 26, 20, 26, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 27, 21, 27, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 27, 20, 27, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 26, 20, 31, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 27, 21, 30, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 27, 20, 30, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 31, 20, 31, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 30, 21, 30, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 30, 20, 30, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 31, 20, 26, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 30, 21, 27, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 30, 20, 27, pBox);
            this.generateBox(pLevel, pBox, 28, 21, 27, 29, 21, 27, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 27, 21, 28, 27, 21, 29, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 28, 21, 30, 29, 21, 30, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 30, 21, 28, 30, 21, 29, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateLowerWall(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 0, 21, 6, 58)) {
            this.generateBox(pLevel, pBox, 0, 0, 21, 6, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 0, 1, 21, 6, 7, 57);
            this.generateBox(pLevel, pBox, 4, 4, 21, 6, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(pLevel, pBox, $$3, $$3 + 1, 21, $$3, $$3 + 1, 57 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 23; $$4 < 53; $$4 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 5, 5, $$4, pBox);
            }

            this.placeBlock(pLevel, DOT_DECO_DATA, 5, 5, 52, pBox);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(pLevel, pBox, $$5, $$5 + 1, 21, $$5, $$5 + 1, 57 - $$5, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(pLevel, pBox, 4, 1, 52, 6, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 5, 1, 51, 5, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(pBox, 51, 21, 58, 58)) {
            this.generateBox(pLevel, pBox, 51, 0, 21, 57, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 51, 1, 21, 57, 7, 57);
            this.generateBox(pLevel, pBox, 51, 4, 21, 53, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$6 = 0; $$6 < 4; $$6++) {
               this.generateBox(pLevel, pBox, 57 - $$6, $$6 + 1, 21, 57 - $$6, $$6 + 1, 57 - $$6, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$7 = 23; $$7 < 53; $$7 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 52, 5, $$7, pBox);
            }

            this.placeBlock(pLevel, DOT_DECO_DATA, 52, 5, 52, pBox);
            this.generateBox(pLevel, pBox, 51, 1, 52, 53, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 52, 1, 51, 52, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(pBox, 0, 51, 57, 57)) {
            this.generateBox(pLevel, pBox, 7, 0, 51, 50, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 7, 1, 51, 50, 10, 57);

            for (int $$8 = 0; $$8 < 4; $$8++) {
               this.generateBox(pLevel, pBox, $$8 + 1, $$8 + 1, 57 - $$8, 56 - $$8, $$8 + 1, 57 - $$8, BASE_LIGHT, BASE_LIGHT, false);
            }
         }
      }

      private void generateMiddleWall(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 7, 21, 13, 50)) {
            this.generateBox(pLevel, pBox, 7, 0, 21, 13, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 7, 1, 21, 13, 10, 50);
            this.generateBox(pLevel, pBox, 11, 8, 21, 13, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(pLevel, pBox, $$3 + 7, $$3 + 5, 21, $$3 + 7, $$3 + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 21; $$4 <= 45; $$4 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 12, 9, $$4, pBox);
            }
         }

         if (this.chunkIntersects(pBox, 44, 21, 50, 54)) {
            this.generateBox(pLevel, pBox, 44, 0, 21, 50, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 44, 1, 21, 50, 10, 50);
            this.generateBox(pLevel, pBox, 44, 8, 21, 46, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(pLevel, pBox, 50 - $$5, $$5 + 5, 21, 50 - $$5, $$5 + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$6 = 21; $$6 <= 45; $$6 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 45, 9, $$6, pBox);
            }
         }

         if (this.chunkIntersects(pBox, 8, 44, 49, 54)) {
            this.generateBox(pLevel, pBox, 14, 0, 44, 43, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 14, 1, 44, 43, 10, 50);

            for (int $$7 = 12; $$7 <= 45; $$7 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 9, 45, pBox);
               this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 9, 52, pBox);
               if ($$7 == 12 || $$7 == 18 || $$7 == 24 || $$7 == 33 || $$7 == 39 || $$7 == 45) {
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 9, 47, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 9, 50, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 10, 45, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 10, 46, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 10, 51, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 10, 52, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 11, 47, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 11, 50, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 12, 48, pBox);
                  this.placeBlock(pLevel, DOT_DECO_DATA, $$7, 12, 49, pBox);
               }
            }

            for (int $$8 = 0; $$8 < 3; $$8++) {
               this.generateBox(pLevel, pBox, 8 + $$8, 5 + $$8, 54, 49 - $$8, 5 + $$8, 54, BASE_GRAY, BASE_GRAY, false);
            }

            this.generateBox(pLevel, pBox, 11, 8, 54, 46, 8, 54, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 14, 8, 44, 43, 8, 53, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateUpperWall(WorldGenLevel pLevel, RandomSource pRandom, BoundingBox pBox) {
         if (this.chunkIntersects(pBox, 14, 21, 20, 43)) {
            this.generateBox(pLevel, pBox, 14, 0, 21, 20, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 14, 1, 22, 20, 14, 43);
            this.generateBox(pLevel, pBox, 18, 12, 22, 20, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 18, 12, 21, 20, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int $$3 = 0; $$3 < 4; $$3++) {
               this.generateBox(pLevel, pBox, $$3 + 14, $$3 + 9, 21, $$3 + 14, $$3 + 9, 43 - $$3, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$4 = 23; $$4 <= 39; $$4 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 19, 13, $$4, pBox);
            }
         }

         if (this.chunkIntersects(pBox, 37, 21, 43, 43)) {
            this.generateBox(pLevel, pBox, 37, 0, 21, 43, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 37, 1, 22, 43, 14, 43);
            this.generateBox(pLevel, pBox, 37, 12, 22, 39, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 37, 12, 21, 39, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int $$5 = 0; $$5 < 4; $$5++) {
               this.generateBox(pLevel, pBox, 43 - $$5, $$5 + 9, 21, 43 - $$5, $$5 + 9, 43 - $$5, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$6 = 23; $$6 <= 39; $$6 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, 38, 13, $$6, pBox);
            }
         }

         if (this.chunkIntersects(pBox, 15, 37, 42, 43)) {
            this.generateBox(pLevel, pBox, 21, 0, 37, 36, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(pLevel, pBox, 21, 1, 37, 36, 14, 43);
            this.generateBox(pLevel, pBox, 21, 12, 37, 36, 12, 39, BASE_GRAY, BASE_GRAY, false);

            for (int $$7 = 0; $$7 < 4; $$7++) {
               this.generateBox(pLevel, pBox, 15 + $$7, $$7 + 9, 43 - $$7, 42 - $$7, $$7 + 9, 43 - $$7, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int $$8 = 21; $$8 <= 36; $$8 += 3) {
               this.placeBlock(pLevel, DOT_DECO_DATA, $$8, 13, 38, pBox);
            }
         }
      }
   }

   interface MonumentRoomFitter {
      boolean fits(OceanMonumentPieces.RoomDefinition pRoom);

      OceanMonumentPieces.OceanMonumentPiece create(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom, RandomSource pRandom);
   }

   public static class OceanMonumentCoreRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentCoreRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, 1, pDirection, pRoom, 2, 2, 2);
      }

      public OceanMonumentCoreRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, pTag);
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
         this.generateBoxOnFillOnly(pLevel, pBox, 1, 8, 0, 14, 8, 14, BASE_GRAY);
         int $$7 = 7;
         BlockState $$8 = BASE_LIGHT;
         this.generateBox(pLevel, pBox, 0, 7, 0, 0, 7, 15, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 15, 7, 0, 15, 7, 15, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 1, 7, 0, 15, 7, 0, $$8, $$8, false);
         this.generateBox(pLevel, pBox, 1, 7, 15, 14, 7, 15, $$8, $$8, false);

         for (int $$9 = 1; $$9 <= 6; $$9++) {
            $$8 = BASE_LIGHT;
            if ($$9 == 2 || $$9 == 6) {
               $$8 = BASE_GRAY;
            }

            for (int $$11 = 0; $$11 <= 15; $$11 += 15) {
               this.generateBox(pLevel, pBox, $$11, $$9, 0, $$11, $$9, 1, $$8, $$8, false);
               this.generateBox(pLevel, pBox, $$11, $$9, 6, $$11, $$9, 9, $$8, $$8, false);
               this.generateBox(pLevel, pBox, $$11, $$9, 14, $$11, $$9, 15, $$8, $$8, false);
            }

            this.generateBox(pLevel, pBox, 1, $$9, 0, 1, $$9, 0, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 6, $$9, 0, 9, $$9, 0, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 14, $$9, 0, 14, $$9, 0, $$8, $$8, false);
            this.generateBox(pLevel, pBox, 1, $$9, 15, 14, $$9, 15, $$8, $$8, false);
         }

         this.generateBox(pLevel, pBox, 6, 3, 6, 9, 6, 9, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 7, 4, 7, 8, 5, 8, Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), false);

         for (int $$12 = 3; $$12 <= 6; $$12 += 3) {
            for (int $$13 = 6; $$13 <= 9; $$13 += 3) {
               this.placeBlock(pLevel, LAMP_BLOCK, $$13, $$12, 6, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, $$13, $$12, 9, pBox);
            }
         }

         this.generateBox(pLevel, pBox, 5, 1, 6, 5, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 9, 5, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 1, 6, 10, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 1, 9, 10, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 1, 5, 6, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 1, 5, 9, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 1, 10, 6, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 1, 10, 9, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 2, 5, 5, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 2, 10, 5, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 2, 5, 10, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 2, 10, 10, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 7, 1, 5, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 7, 1, 10, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 7, 9, 5, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 7, 9, 10, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 7, 5, 6, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 7, 10, 6, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 7, 5, 14, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 7, 10, 14, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 1, 2, 2, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 1, 2, 3, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 13, 1, 2, 13, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 12, 1, 2, 12, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 1, 12, 2, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 1, 13, 3, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 13, 1, 12, 13, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 12, 1, 13, 12, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
      }
   }

   public static class OceanMonumentDoubleXRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, 1, pDirection, pRoom, 2, 1, 1);
      }

      public OceanMonumentDoubleXRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, pTag);
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
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 8, 0, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(pLevel, pBox, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$8.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 4, 1, 7, 4, 6, BASE_GRAY);
         }

         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 8, 4, 1, 14, 4, 6, BASE_GRAY);
         }

         this.generateBox(pLevel, pBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 15, 3, 0, 15, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 0, 15, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 7, 14, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 15, 2, 0, 15, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 1, 2, 0, 15, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 1, 2, 7, 14, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 15, 1, 0, 15, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 0, 15, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 0, 10, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 2, 0, 9, 2, 3, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 5, 3, 0, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(pLevel, LAMP_BLOCK, 6, 2, 3, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 9, 2, 3, pBox);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 7, 4, 2, 7);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 1, 0, 12, 2, 0);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 1, 7, 12, 2, 7);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 15, 1, 3, 15, 2, 4);
         }
      }
   }

   public static class OceanMonumentDoubleXYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXYRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, 1, pDirection, pRoom, 2, 2, 1);
      }

      public OceanMonumentDoubleXYRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, pTag);
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
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition $$9 = $$8.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$10 = $$7.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 8, 0, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(pLevel, pBox, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$9.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 8, 1, 7, 8, 6, BASE_GRAY);
         }

         if ($$10.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 8, 8, 1, 14, 8, 6, BASE_GRAY);
         }

         for (int $$11 = 1; $$11 <= 7; $$11++) {
            BlockState $$12 = BASE_LIGHT;
            if ($$11 == 2 || $$11 == 6) {
               $$12 = BASE_GRAY;
            }

            this.generateBox(pLevel, pBox, 0, $$11, 0, 0, $$11, 7, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 15, $$11, 0, 15, $$11, 7, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 1, $$11, 0, 15, $$11, 0, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 1, $$11, 7, 14, $$11, 7, $$12, $$12, false);
         }

         this.generateBox(pLevel, pBox, 2, 1, 3, 2, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 1, 2, 4, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 1, 5, 4, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 13, 1, 3, 13, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 11, 1, 2, 12, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 11, 1, 5, 12, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 3, 5, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 1, 3, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 7, 2, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 5, 2, 5, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 5, 2, 10, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 5, 5, 5, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 10, 5, 5, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(pLevel, BASE_LIGHT, 6, 6, 2, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 9, 6, 2, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 6, 6, 5, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 9, 6, 5, pBox);
         this.generateBox(pLevel, pBox, 5, 4, 3, 6, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 4, 3, 10, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(pLevel, LAMP_BLOCK, 5, 4, 2, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 5, 4, 5, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 10, 4, 2, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 10, 4, 5, pBox);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 7, 4, 2, 7);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 1, 0, 12, 2, 0);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 1, 7, 12, 2, 7);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 15, 1, 3, 15, 2, 4);
         }

         if ($$9.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 5, 0, 4, 6, 0);
         }

         if ($$9.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 5, 7, 4, 6, 7);
         }

         if ($$9.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 5, 3, 0, 6, 4);
         }

         if ($$10.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 5, 0, 12, 6, 0);
         }

         if ($$10.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 11, 5, 7, 12, 6, 7);
         }

         if ($$10.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 15, 5, 3, 15, 6, 4);
         }
      }
   }

   public static class OceanMonumentDoubleYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, 1, pDirection, pRoom, 1, 2, 1);
      }

      public OceanMonumentDoubleYRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, pTag);
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
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.UP.get3DDataValue()];
         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 8, 1, 6, 8, 6, BASE_GRAY);
         }

         this.generateBox(pLevel, pBox, 0, 4, 0, 0, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 4, 0, 7, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 4, 0, 6, 4, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 4, 7, 6, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 4, 1, 2, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 4, 2, 1, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 4, 1, 5, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 4, 2, 6, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 4, 5, 2, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 4, 5, 1, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 4, 5, 5, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 4, 5, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;

         for (int $$9 = 1; $$9 <= 5; $$9 += 4) {
            int $$10 = 0;
            if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 2, $$9, $$10, 2, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 5, $$9, $$10, 5, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 3, $$9 + 2, $$10, 4, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 0, $$9, $$10, 7, $$9 + 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 0, $$9 + 1, $$10, 7, $$9 + 1, $$10, BASE_GRAY, BASE_GRAY, false);
            }

            int var13 = 7;
            if ($$8.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 2, $$9, var13, 2, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 5, $$9, var13, 5, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 3, $$9 + 2, var13, 4, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 0, $$9, var13, 7, $$9 + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 0, $$9 + 1, var13, 7, $$9 + 1, var13, BASE_GRAY, BASE_GRAY, false);
            }

            int $$11 = 0;
            if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, $$11, $$9, 2, $$11, $$9 + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, $$11, $$9, 5, $$11, $$9 + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, $$11, $$9 + 2, 3, $$11, $$9 + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, $$11, $$9, 0, $$11, $$9 + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, $$11, $$9 + 1, 0, $$11, $$9 + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            int var14 = 7;
            if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, var14, $$9, 2, var14, $$9 + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, var14, $$9, 5, var14, $$9 + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, var14, $$9 + 2, 3, var14, $$9 + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, var14, $$9, 0, var14, $$9 + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, var14, $$9 + 1, 0, var14, $$9 + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            $$8 = $$7;
         }
      }
   }

   public static class OceanMonumentDoubleYZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYZRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, 1, pDirection, pRoom, 1, 2, 2);
      }

      public OceanMonumentDoubleYZRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, pTag);
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
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition $$9 = $$7.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$10 = $$8.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 0, 8, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(pLevel, pBox, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$10.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 8, 1, 6, 8, 7, BASE_GRAY);
         }

         if ($$9.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 8, 8, 6, 8, 14, BASE_GRAY);
         }

         for (int $$11 = 1; $$11 <= 7; $$11++) {
            BlockState $$12 = BASE_LIGHT;
            if ($$11 == 2 || $$11 == 6) {
               $$12 = BASE_GRAY;
            }

            this.generateBox(pLevel, pBox, 0, $$11, 0, 0, $$11, 15, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 7, $$11, 0, 7, $$11, 15, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 1, $$11, 0, 6, $$11, 0, $$12, $$12, false);
            this.generateBox(pLevel, pBox, 1, $$11, 15, 6, $$11, 15, $$12, $$12, false);
         }

         for (int $$13 = 1; $$13 <= 7; $$13++) {
            BlockState $$14 = BASE_BLACK;
            if ($$13 == 2 || $$13 == 6) {
               $$14 = LAMP_BLOCK;
            }

            this.generateBox(pLevel, pBox, 3, $$13, 7, 4, $$13, 8, $$14, $$14, false);
         }

         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 1, 3, 7, 2, 4);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 15, 4, 2, 15);
         }

         if ($$7.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 11, 0, 2, 12);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 1, 11, 7, 2, 12);
         }

         if ($$10.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 5, 0, 4, 6, 0);
         }

         if ($$10.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 5, 3, 7, 6, 4);
            this.generateBox(pLevel, pBox, 5, 4, 2, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 2, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 5, 6, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$10.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 5, 3, 0, 6, 4);
            this.generateBox(pLevel, pBox, 1, 4, 2, 2, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 2, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 5, 1, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$9.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 5, 15, 4, 6, 15);
         }

         if ($$9.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 5, 11, 0, 6, 12);
            this.generateBox(pLevel, pBox, 1, 4, 10, 2, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 10, 1, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 13, 1, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }

         if ($$9.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 5, 11, 7, 6, 12);
            this.generateBox(pLevel, pBox, 5, 4, 10, 6, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 10, 6, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 13, 6, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentDoubleZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleZRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, 1, pDirection, pRoom, 1, 1, 2);
      }

      public OceanMonumentDoubleZRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, pTag);
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
         OceanMonumentPieces.RoomDefinition $$7 = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition $$8 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 0, 8, $$7.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(pLevel, pBox, 0, 0, $$8.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if ($$8.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 4, 1, 6, 4, 7, BASE_GRAY);
         }

         if ($$7.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 4, 8, 6, 4, 14, BASE_GRAY);
         }

         this.generateBox(pLevel, pBox, 0, 3, 0, 0, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 3, 0, 7, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 15, 6, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 7, 2, 0, 7, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 1, 2, 0, 7, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 1, 2, 15, 6, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 0, 1, 0, 0, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 1, 0, 7, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 0, 7, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 15, 6, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 1, 1, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 1, 1, 6, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 1, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 3, 1, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 13, 1, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 1, 13, 6, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 13, 1, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 3, 13, 6, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 1, 6, 2, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 6, 5, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 1, 9, 2, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 9, 5, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 2, 6, 4, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 3, 2, 9, 4, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 2, 2, 7, 2, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 2, 7, 5, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(pLevel, LAMP_BLOCK, 2, 2, 5, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 5, 2, 5, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 2, 2, 10, pBox);
         this.placeBlock(pLevel, LAMP_BLOCK, 5, 2, 10, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 2, 3, 5, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 5, 3, 5, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 2, 3, 10, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 5, 3, 10, pBox);
         if ($$8.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
         }

         if ($$8.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 1, 3, 7, 2, 4);
         }

         if ($$8.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 3, 0, 2, 4);
         }

         if ($$7.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 15, 4, 2, 15);
         }

         if ($$7.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 11, 0, 2, 12);
         }

         if ($$7.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 7, 1, 11, 7, 2, 12);
         }
      }
   }

   public static class OceanMonumentEntryRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentEntryRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, 1, pDirection, pRoom, 1, 1, 1);
      }

      public OceanMonumentEntryRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, pTag);
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
         this.generateBox(pLevel, pBox, 0, 3, 0, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 1, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, 2, 0, 7, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 0, 2, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 5, 1, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 7, 4, 2, 7);
         }

         if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 0, 1, 3, 1, 2, 4);
         }

         if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 6, 1, 3, 7, 2, 4);
         }
      }
   }

   public static class OceanMonumentPenthouse extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentPenthouse(Direction pDirection, BoundingBox pBox) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, pDirection, 1, pBox);
      }

      public OceanMonumentPenthouse(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, pTag);
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
         this.generateBox(pLevel, pBox, 2, -1, 2, 11, -1, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, -1, 0, 1, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 12, -1, 0, 13, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 2, -1, 0, 11, -1, 1, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 2, -1, 12, 11, -1, 13, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(pLevel, pBox, 0, 0, 0, 0, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 13, 0, 0, 13, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 0, 0, 12, 0, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 0, 13, 12, 0, 13, BASE_LIGHT, BASE_LIGHT, false);

         for (int $$7 = 2; $$7 <= 11; $$7 += 3) {
            this.placeBlock(pLevel, LAMP_BLOCK, 0, 0, $$7, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 13, 0, $$7, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, $$7, 0, 0, pBox);
         }

         this.generateBox(pLevel, pBox, 2, 0, 3, 4, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 9, 0, 3, 11, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 4, 0, 9, 9, 0, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(pLevel, BASE_LIGHT, 5, 0, 8, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 8, 0, 8, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 10, 0, 10, pBox);
         this.placeBlock(pLevel, BASE_LIGHT, 3, 0, 10, pBox);
         this.generateBox(pLevel, pBox, 3, 0, 3, 3, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 10, 0, 3, 10, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 6, 0, 10, 7, 0, 10, BASE_BLACK, BASE_BLACK, false);
         int $$8 = 3;

         for (int $$9 = 0; $$9 < 2; $$9++) {
            for (int $$10 = 2; $$10 <= 8; $$10 += 3) {
               this.generateBox(pLevel, pBox, $$8, 0, $$10, $$8, 2, $$10, BASE_LIGHT, BASE_LIGHT, false);
            }

            $$8 = 10;
         }

         this.generateBox(pLevel, pBox, 5, 0, 10, 5, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 8, 0, 10, 8, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 6, -1, 7, 7, -1, 8, BASE_BLACK, BASE_BLACK, false);
         this.generateWaterBox(pLevel, pBox, 6, -1, 3, 7, -1, 4);
         this.spawnElder(pLevel, pBox, 6, 1, 6);
      }
   }

   protected abstract static class OceanMonumentPiece extends StructurePiece {
      protected static final BlockState BASE_GRAY = Blocks.PRISMARINE.defaultBlockState();
      protected static final BlockState BASE_LIGHT = Blocks.PRISMARINE_BRICKS.defaultBlockState();
      protected static final BlockState BASE_BLACK = Blocks.DARK_PRISMARINE.defaultBlockState();
      protected static final BlockState DOT_DECO_DATA = BASE_LIGHT;
      protected static final BlockState LAMP_BLOCK = Blocks.SEA_LANTERN.defaultBlockState();
      protected static final boolean DO_FILL = true;
      protected static final BlockState FILL_BLOCK = Blocks.WATER.defaultBlockState();
      protected static final Set<Block> FILL_KEEP = ImmutableSet.builder()
         .add(Blocks.ICE)
         .add(Blocks.PACKED_ICE)
         .add(Blocks.BLUE_ICE)
         .add(FILL_BLOCK.getBlock())
         .build();
      protected static final int GRIDROOM_WIDTH = 8;
      protected static final int GRIDROOM_DEPTH = 8;
      protected static final int GRIDROOM_HEIGHT = 4;
      protected static final int GRID_WIDTH = 5;
      protected static final int GRID_DEPTH = 5;
      protected static final int GRID_HEIGHT = 3;
      protected static final int GRID_FLOOR_COUNT = 25;
      protected static final int GRID_SIZE = 75;
      protected static final int GRIDROOM_SOURCE_INDEX = getRoomIndex(2, 0, 0);
      protected static final int GRIDROOM_TOP_CONNECT_INDEX = getRoomIndex(2, 2, 0);
      protected static final int GRIDROOM_LEFTWING_CONNECT_INDEX = getRoomIndex(0, 1, 0);
      protected static final int GRIDROOM_RIGHTWING_CONNECT_INDEX = getRoomIndex(4, 1, 0);
      protected static final int LEFTWING_INDEX = 1001;
      protected static final int RIGHTWING_INDEX = 1002;
      protected static final int PENTHOUSE_INDEX = 1003;
      protected OceanMonumentPieces.RoomDefinition roomDefinition;

      protected static int getRoomIndex(int pX, int pY, int pZ) {
         return pY * 25 + pZ * 5 + pX;
      }

      public OceanMonumentPiece(StructurePieceType pType, Direction pOrientation, int pGenDepth, BoundingBox pBox) {
         super(pType, pGenDepth, pBox);
         this.setOrientation(pOrientation);
      }

      protected OceanMonumentPiece(
         StructurePieceType pType, int pGenDepth, Direction pOrientation, OceanMonumentPieces.RoomDefinition pRoomDefinition, int pX, int pY, int pZ
      ) {
         super(pType, pGenDepth, makeBoundingBox(pOrientation, pRoomDefinition, pX, pY, pZ));
         this.setOrientation(pOrientation);
         this.roomDefinition = pRoomDefinition;
      }

      private static BoundingBox makeBoundingBox(Direction pDirection, OceanMonumentPieces.RoomDefinition pDefinition, int pX, int pY, int pZ) {
         int $$5 = pDefinition.index;
         int $$6 = $$5 % 5;
         int $$7 = $$5 / 5 % 5;
         int $$8 = $$5 / 25;
         BoundingBox $$9 = makeBoundingBox(0, 0, 0, pDirection, pX * 8, pY * 4, pZ * 8);
         switch (pDirection) {
            case NORTH:
               $$9.move($$6 * 8, $$8 * 4, -($$7 + pZ) * 8 + 1);
               break;
            case SOUTH:
               $$9.move($$6 * 8, $$8 * 4, $$7 * 8);
               break;
            case WEST:
               $$9.move(-($$7 + pZ) * 8 + 1, $$8 * 4, $$6 * 8);
               break;
            case EAST:
            default:
               $$9.move($$7 * 8, $$8 * 4, $$6 * 8);
         }

         return $$9;
      }

      public OceanMonumentPiece(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      }

      protected void generateWaterBox(WorldGenLevel pLevel, BoundingBox pBoundingBox, int pX1, int pY1, int pZ1, int pX2, int pY2, int pZ2) {
         for (int $$8 = pY1; $$8 <= pY2; $$8++) {
            for (int $$9 = pX1; $$9 <= pX2; $$9++) {
               for (int $$10 = pZ1; $$10 <= pZ2; $$10++) {
                  BlockState $$11 = this.getBlock(pLevel, $$9, $$8, $$10, pBoundingBox);
                  if (!FILL_KEEP.contains($$11.getBlock())) {
                     if (this.getWorldY($$8) >= pLevel.getSeaLevel() && $$11 != FILL_BLOCK) {
                        this.placeBlock(pLevel, Blocks.AIR.defaultBlockState(), $$9, $$8, $$10, pBoundingBox);
                     } else {
                        this.placeBlock(pLevel, FILL_BLOCK, $$9, $$8, $$10, pBoundingBox);
                     }
                  }
               }
            }
         }
      }

      protected void generateDefaultFloor(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pZ, boolean pHasOpeningDownwards) {
         if (pHasOpeningDownwards) {
            this.generateBox(pLevel, pBox, pX + 0, 0, pZ + 0, pX + 2, 0, pZ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 5, 0, pZ + 0, pX + 8 - 1, 0, pZ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 3, 0, pZ + 0, pX + 4, 0, pZ + 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 3, 0, pZ + 5, pX + 4, 0, pZ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, pX + 3, 0, pZ + 2, pX + 4, 0, pZ + 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, pX + 3, 0, pZ + 5, pX + 4, 0, pZ + 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, pX + 2, 0, pZ + 3, pX + 2, 0, pZ + 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, pX + 5, 0, pZ + 3, pX + 5, 0, pZ + 4, BASE_LIGHT, BASE_LIGHT, false);
         } else {
            this.generateBox(pLevel, pBox, pX + 0, 0, pZ + 0, pX + 8 - 1, 0, pZ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
         }
      }

      protected void generateBoxOnFillOnly(
         WorldGenLevel pLevel, BoundingBox pBox, int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ, BlockState pState
      ) {
         for (int $$9 = pMinY; $$9 <= pMaxY; $$9++) {
            for (int $$10 = pMinX; $$10 <= pMaxX; $$10++) {
               for (int $$11 = pMinZ; $$11 <= pMaxZ; $$11++) {
                  if (this.getBlock(pLevel, $$10, $$9, $$11, pBox) == FILL_BLOCK) {
                     this.placeBlock(pLevel, pState, $$10, $$9, $$11, pBox);
                  }
               }
            }
         }
      }

      protected boolean chunkIntersects(BoundingBox pBox, int pMinX, int pMinZ, int pMaxX, int pMaxZ) {
         int $$5 = this.getWorldX(pMinX, pMinZ);
         int $$6 = this.getWorldZ(pMinX, pMinZ);
         int $$7 = this.getWorldX(pMaxX, pMaxZ);
         int $$8 = this.getWorldZ(pMaxX, pMaxZ);
         return pBox.intersects(Math.min($$5, $$7), Math.min($$6, $$8), Math.max($$5, $$7), Math.max($$6, $$8));
      }

      protected void spawnElder(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ) {
         BlockPos $$5 = this.getWorldPos(pX, pY, pZ);
         if (pBox.isInside($$5)) {
            ElderGuardian $$6 = EntityType.ELDER_GUARDIAN.create(pLevel.getLevel());
            if ($$6 != null) {
               $$6.heal($$6.getMaxHealth());
               $$6.moveTo((double)$$5.getX() + 0.5, (double)$$5.getY(), (double)$$5.getZ() + 0.5, 0.0F, 0.0F);
               $$6.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt($$6.blockPosition()), MobSpawnType.STRUCTURE, null);
               pLevel.addFreshEntityWithPassengers($$6);
            }
         }
      }
   }

   public static class OceanMonumentSimpleRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentSimpleRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom, RandomSource pRandom) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, 1, pDirection, pRoom, 1, 1, 1);
         this.mainDesign = pRandom.nextInt(3);
      }

      public OceanMonumentSimpleRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, pTag);
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
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         boolean $$7 = this.mainDesign != 0
            && pRandom.nextBoolean()
            && !this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]
            && !this.roomDefinition.hasOpening[Direction.UP.get3DDataValue()]
            && this.roomDefinition.countOpenings() > 1;
         if (this.mainDesign == 0) {
            this.generateBox(pLevel, pBox, 0, 1, 0, 2, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 3, 0, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 2, 0, 0, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 1, 2, 0, 2, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(pLevel, LAMP_BLOCK, 1, 2, 1, pBox);
            this.generateBox(pLevel, pBox, 5, 1, 0, 7, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 5, 3, 0, 7, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 2, 0, 7, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 5, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(pLevel, LAMP_BLOCK, 6, 2, 1, pBox);
            this.generateBox(pLevel, pBox, 0, 1, 5, 2, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 3, 5, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 2, 5, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 1, 2, 7, 2, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(pLevel, LAMP_BLOCK, 1, 2, 6, pBox);
            this.generateBox(pLevel, pBox, 5, 1, 5, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 5, 3, 5, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 2, 5, 7, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 5, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(pLevel, LAMP_BLOCK, 6, 2, 6, pBox);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 3, 3, 0, 4, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 3, 3, 0, 4, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 3, 2, 0, 4, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 3, 1, 0, 4, 1, 1, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 3, 3, 7, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 3, 3, 6, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 3, 2, 7, 4, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 3, 1, 6, 4, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 0, 3, 3, 0, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 0, 3, 3, 1, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 0, 2, 3, 0, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 0, 1, 3, 1, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 7, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(pLevel, pBox, 6, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 7, 2, 3, 7, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 6, 1, 3, 7, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 1) {
            this.generateBox(pLevel, pBox, 2, 1, 2, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 2, 1, 5, 2, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 5, 1, 5, 5, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 5, 1, 2, 5, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(pLevel, LAMP_BLOCK, 2, 2, 2, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 2, 2, 5, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 5, 2, 5, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 5, 2, 2, pBox);
            this.generateBox(pLevel, pBox, 0, 1, 0, 1, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 1, 1, 0, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 1, 7, 1, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 1, 6, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 1, 6, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 1, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 1, 1, 7, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(pLevel, BASE_GRAY, 1, 2, 0, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 0, 2, 1, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 1, 2, 7, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 0, 2, 6, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 6, 2, 7, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 7, 2, 6, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 6, 2, 0, pBox);
            this.placeBlock(pLevel, BASE_GRAY, 7, 2, 1, pBox);
            if (!this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 1, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 1, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 0, 3, 1, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 0, 2, 1, 0, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 0, 1, 1, 0, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(pLevel, pBox, 7, 3, 1, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, 7, 2, 1, 7, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(pLevel, pBox, 7, 1, 1, 7, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 2) {
            this.generateBox(pLevel, pBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateWaterBox(pLevel, pBox, 3, 1, 7, 4, 2, 7);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateWaterBox(pLevel, pBox, 0, 1, 3, 0, 2, 4);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateWaterBox(pLevel, pBox, 7, 1, 3, 7, 2, 4);
            }
         }

         if ($$7) {
            this.generateBox(pLevel, pBox, 3, 1, 3, 4, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 3, 2, 3, 4, 2, 4, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(pLevel, pBox, 3, 3, 3, 4, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentSimpleTopRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentSimpleTopRoom(Direction pDirection, OceanMonumentPieces.RoomDefinition pRoom) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, 1, pDirection, pRoom, 1, 1, 1);
      }

      public OceanMonumentSimpleTopRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, pTag);
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
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(pLevel, pBox, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(pLevel, pBox, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         for (int $$7 = 1; $$7 <= 6; $$7++) {
            for (int $$8 = 1; $$8 <= 6; $$8++) {
               if (pRandom.nextInt(3) != 0) {
                  int $$9 = 2 + (pRandom.nextInt(4) == 0 ? 0 : 1);
                  BlockState $$10 = Blocks.WET_SPONGE.defaultBlockState();
                  this.generateBox(pLevel, pBox, $$7, $$9, $$8, $$7, 3, $$8, $$10, $$10, false);
               }
            }
         }

         this.generateBox(pLevel, pBox, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(pLevel, pBox, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(pLevel, pBox, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
         if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(pLevel, pBox, 3, 1, 0, 4, 2, 0);
         }
      }
   }

   public static class OceanMonumentWingRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentWingRoom(Direction pDirection, BoundingBox pBox, int pFlag) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, pDirection, 1, pBox);
         this.mainDesign = pFlag & 1;
      }

      public OceanMonumentWingRoom(CompoundTag pTag) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, pTag);
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
         if (this.mainDesign == 0) {
            for (int $$7 = 0; $$7 < 4; $$7++) {
               this.generateBox(pLevel, pBox, 10 - $$7, 3 - $$7, 20 - $$7, 12 + $$7, 3 - $$7, 20, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(pLevel, pBox, 7, 0, 6, 15, 0, 16, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 6, 0, 6, 6, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 16, 0, 6, 16, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 1, 7, 7, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 15, 1, 7, 15, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 7, 1, 6, 9, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 13, 1, 6, 15, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 8, 1, 7, 9, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 13, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 9, 0, 5, 13, 0, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 10, 0, 7, 12, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 8, 0, 10, 8, 0, 12, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 14, 0, 10, 14, 0, 12, BASE_BLACK, BASE_BLACK, false);

            for (int $$8 = 18; $$8 >= 7; $$8 -= 3) {
               this.placeBlock(pLevel, LAMP_BLOCK, 6, 3, $$8, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, 16, 3, $$8, pBox);
            }

            this.placeBlock(pLevel, LAMP_BLOCK, 10, 0, 10, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 12, 0, 10, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 10, 0, 12, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 12, 0, 12, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 8, 3, 6, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 14, 3, 6, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 4, 2, 4, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 4, 1, 4, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 4, 0, 4, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 18, 2, 4, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 18, 1, 4, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 18, 0, 4, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 4, 2, 18, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 4, 1, 18, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 4, 0, 18, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 18, 2, 18, pBox);
            this.placeBlock(pLevel, LAMP_BLOCK, 18, 1, 18, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 18, 0, 18, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 9, 7, 20, pBox);
            this.placeBlock(pLevel, BASE_LIGHT, 13, 7, 20, pBox);
            this.generateBox(pLevel, pBox, 6, 0, 21, 7, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 15, 0, 21, 16, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.spawnElder(pLevel, pBox, 11, 2, 16);
         } else if (this.mainDesign == 1) {
            this.generateBox(pLevel, pBox, 9, 3, 18, 13, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 9, 0, 18, 9, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(pLevel, pBox, 13, 0, 18, 13, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            int $$9 = 9;
            int $$10 = 20;
            int $$11 = 5;

            for (int $$12 = 0; $$12 < 2; $$12++) {
               this.placeBlock(pLevel, BASE_LIGHT, $$9, 6, 20, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, $$9, 5, 20, pBox);
               this.placeBlock(pLevel, BASE_LIGHT, $$9, 4, 20, pBox);
               $$9 = 13;
            }

            this.generateBox(pLevel, pBox, 7, 3, 7, 15, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            int var14 = 10;

            for (int $$13 = 0; $$13 < 2; $$13++) {
               this.generateBox(pLevel, pBox, var14, 0, 10, var14, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, var14, 0, 12, var14, 6, 12, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(pLevel, LAMP_BLOCK, var14, 0, 10, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, var14, 0, 12, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, var14, 4, 10, pBox);
               this.placeBlock(pLevel, LAMP_BLOCK, var14, 4, 12, pBox);
               var14 = 12;
            }

            var14 = 8;

            for (int $$14 = 0; $$14 < 2; $$14++) {
               this.generateBox(pLevel, pBox, var14, 0, 7, var14, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(pLevel, pBox, var14, 0, 14, var14, 2, 14, BASE_LIGHT, BASE_LIGHT, false);
               var14 = 14;
            }

            this.generateBox(pLevel, pBox, 8, 3, 8, 8, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(pLevel, pBox, 14, 3, 8, 14, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.spawnElder(pLevel, pBox, 11, 5, 13);
         }
      }
   }

   static class RoomDefinition {
      final int index;
      final OceanMonumentPieces.RoomDefinition[] connections = new OceanMonumentPieces.RoomDefinition[6];
      final boolean[] hasOpening = new boolean[6];
      boolean claimed;
      boolean isSource;
      private int scanIndex;

      public RoomDefinition(int pIndex) {
         this.index = pIndex;
      }

      public void setConnection(Direction pDirection, OceanMonumentPieces.RoomDefinition pConnectingRoom) {
         this.connections[pDirection.get3DDataValue()] = pConnectingRoom;
         pConnectingRoom.connections[pDirection.getOpposite().get3DDataValue()] = this;
      }

      public void updateOpenings() {
         for (int $$0 = 0; $$0 < 6; $$0++) {
            this.hasOpening[$$0] = this.connections[$$0] != null;
         }
      }

      public boolean findSource(int pIndex) {
         if (this.isSource) {
            return true;
         } else {
            this.scanIndex = pIndex;

            for (int $$1 = 0; $$1 < 6; $$1++) {
               if (this.connections[$$1] != null
                  && this.hasOpening[$$1]
                  && this.connections[$$1].scanIndex != pIndex
                  && this.connections[$$1].findSource(pIndex)) {
                  return true;
               }
            }

            return false;
         }
      }

      public boolean isSpecial() {
         return this.index >= 75;
      }

      public int countOpenings() {
         int $$0 = 0;

         for (int $$1 = 0; $$1 < 6; $$1++) {
            if (this.hasOpening[$$1]) {
               $$0++;
            }
         }

         return $$0;
      }
   }
}
