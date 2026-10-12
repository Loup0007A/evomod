package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

public class MineshaftPieces {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_SHAFT_WIDTH = 3;
   private static final int DEFAULT_SHAFT_HEIGHT = 3;
   private static final int DEFAULT_SHAFT_LENGTH = 5;
   private static final int MAX_PILLAR_HEIGHT = 20;
   private static final int MAX_CHAIN_HEIGHT = 50;
   private static final int MAX_DEPTH = 8;
   public static final int MAGIC_START_Y = 50;

   private static MineshaftPieces.MineShaftPiece createRandomShaftPiece(
      StructurePieceAccessor pPieces,
      RandomSource pRandom,
      int pX,
      int pY,
      int pZ,
      @Nullable Direction pOrientation,
      int pGenDepth,
      MineshaftStructure.Type pType
   ) {
      int $$8 = pRandom.nextInt(100);
      if ($$8 >= 80) {
         BoundingBox $$9 = MineshaftPieces.MineShaftCrossing.findCrossing(pPieces, pRandom, pX, pY, pZ, pOrientation);
         if ($$9 != null) {
            return new MineshaftPieces.MineShaftCrossing(pGenDepth, $$9, pOrientation, pType);
         }
      } else if ($$8 >= 70) {
         BoundingBox $$10 = MineshaftPieces.MineShaftStairs.findStairs(pPieces, pRandom, pX, pY, pZ, pOrientation);
         if ($$10 != null) {
            return new MineshaftPieces.MineShaftStairs(pGenDepth, $$10, pOrientation, pType);
         }
      } else {
         BoundingBox $$11 = MineshaftPieces.MineShaftCorridor.findCorridorSize(pPieces, pRandom, pX, pY, pZ, pOrientation);
         if ($$11 != null) {
            return new MineshaftPieces.MineShaftCorridor(pGenDepth, pRandom, $$11, pOrientation, pType);
         }
      }

      return null;
   }

   static MineshaftPieces.MineShaftPiece generateAndAddPiece(
      StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection, int pGenDepth
   ) {
      if (pGenDepth > 8) {
         return null;
      } else if (Math.abs(pX - pPiece.getBoundingBox().minX()) <= 80 && Math.abs(pZ - pPiece.getBoundingBox().minZ()) <= 80) {
         MineshaftStructure.Type $$8 = ((MineshaftPieces.MineShaftPiece)pPiece).type;
         MineshaftPieces.MineShaftPiece $$9 = createRandomShaftPiece(pPieces, pRandom, pX, pY, pZ, pDirection, pGenDepth + 1, $$8);
         if ($$9 != null) {
            pPieces.addPiece($$9);
            $$9.addChildren(pPiece, pPieces, pRandom);
         }

         return $$9;
      } else {
         return null;
      }
   }

   public static class MineShaftCorridor extends MineshaftPieces.MineShaftPiece {
      private final boolean hasRails;
      private final boolean spiderCorridor;
      private boolean hasPlacedSpider;
      private final int numSections;

      public MineShaftCorridor(CompoundTag pTag) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, pTag);
         this.hasRails = pTag.getBoolean("hr");
         this.spiderCorridor = pTag.getBoolean("sc");
         this.hasPlacedSpider = pTag.getBoolean("hps");
         this.numSections = pTag.getInt("Num");
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("hr", this.hasRails);
         pTag.putBoolean("sc", this.spiderCorridor);
         pTag.putBoolean("hps", this.hasPlacedSpider);
         pTag.putInt("Num", this.numSections);
      }

      public MineShaftCorridor(int pGenDepth, RandomSource pRandom, BoundingBox pBoundingBox, Direction pOrientation, MineshaftStructure.Type pType) {
         super(StructurePieceType.MINE_SHAFT_CORRIDOR, pGenDepth, pType, pBoundingBox);
         this.setOrientation(pOrientation);
         this.hasRails = pRandom.nextInt(3) == 0;
         this.spiderCorridor = !this.hasRails && pRandom.nextInt(23) == 0;
         if (this.getOrientation().getAxis() == Direction.Axis.Z) {
            this.numSections = pBoundingBox.getZSpan() / 5;
         } else {
            this.numSections = pBoundingBox.getXSpan() / 5;
         }
      }

      @Nullable
      public static BoundingBox findCorridorSize(StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection) {
         for (int $$6 = pRandom.nextInt(3) + 2; $$6 > 0; $$6--) {
            int $$7 = $$6 * 5;

            BoundingBox $$11 = switch (pDirection) {
               default -> new BoundingBox(0, 0, -($$7 - 1), 2, 2, 0);
               case SOUTH -> new BoundingBox(0, 0, 0, 2, 2, $$7 - 1);
               case WEST -> new BoundingBox(-($$7 - 1), 0, 0, 0, 2, 2);
               case EAST -> new BoundingBox(0, 0, 0, $$7 - 1, 2, 2);
            };
            $$11.move(pX, pY, pZ);
            if (pPieces.findCollisionPiece($$11) == null) {
               return $$11;
            }
         }

         return null;
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = this.getGenDepth();
         int $$4 = pRandom.nextInt(4);
         Direction $$5 = this.getOrientation();
         if ($$5 != null) {
            switch ($$5) {
               case NORTH:
               default:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.WEST,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ(),
                        Direction.EAST,
                        $$3
                     );
                  }
                  break;
               case SOUTH:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.WEST,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.maxZ() - 3,
                        Direction.EAST,
                        $$3
                     );
                  }
                  break;
               case WEST:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX() - 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ(),
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.minX(),
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        $$3
                     );
                  }
                  break;
               case EAST:
                  if ($$4 <= 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.maxX() + 1,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ(),
                        $$5,
                        $$3
                     );
                  } else if ($$4 == 2) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.minZ() - 1,
                        Direction.NORTH,
                        $$3
                     );
                  } else {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece,
                        pPieces,
                        pRandom,
                        this.boundingBox.maxX() - 3,
                        this.boundingBox.minY() - 1 + pRandom.nextInt(3),
                        this.boundingBox.maxZ() + 1,
                        Direction.SOUTH,
                        $$3
                     );
                  }
            }
         }

         if ($$3 < 8) {
            if ($$5 != Direction.NORTH && $$5 != Direction.SOUTH) {
               for (int $$8 = this.boundingBox.minX() + 3; $$8 + 3 <= this.boundingBox.maxX(); $$8 += 5) {
                  int $$9 = pRandom.nextInt(5);
                  if ($$9 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece, pPieces, pRandom, $$8, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3 + 1
                     );
                  } else if ($$9 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece, pPieces, pRandom, $$8, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3 + 1
                     );
                  }
               }
            } else {
               for (int $$6 = this.boundingBox.minZ() + 3; $$6 + 3 <= this.boundingBox.maxZ(); $$6 += 5) {
                  int $$7 = pRandom.nextInt(5);
                  if ($$7 == 0) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY(), $$6, Direction.WEST, $$3 + 1
                     );
                  } else if ($$7 == 1) {
                     MineshaftPieces.generateAndAddPiece(
                        pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY(), $$6, Direction.EAST, $$3 + 1
                     );
                  }
               }
            }
         }
      }

      @Override
      protected boolean createChest(WorldGenLevel pLevel, BoundingBox pBox, RandomSource pRandom, int pX, int pY, int pZ, ResourceKey<LootTable> pLootTable) {
         BlockPos $$7 = this.getWorldPos(pX, pY, pZ);
         if (pBox.isInside($$7) && pLevel.getBlockState($$7).isAir() && !pLevel.getBlockState($$7.below()).isAir()) {
            BlockState $$8 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, pRandom.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
            this.placeBlock(pLevel, $$8, pX, pY, pZ, pBox);
            MinecartChest $$9 = new MinecartChest(pLevel.getLevel(), (double)$$7.getX() + 0.5, (double)$$7.getY() + 0.5, (double)$$7.getZ() + 0.5);
            $$9.setLootTable(pLootTable, pRandom.nextLong());
            pLevel.addFreshEntity($$9);
            return true;
         } else {
            return false;
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
         if (!this.isInInvalidLocation(pLevel, pBox)) {
            int $$7 = 0;
            int $$8 = 2;
            int $$9 = 0;
            int $$10 = 2;
            int $$11 = this.numSections * 5 - 1;
            BlockState $$12 = this.type.getPlanksState();
            this.generateBox(pLevel, pBox, 0, 0, 0, 2, 1, $$11, CAVE_AIR, CAVE_AIR, false);
            this.generateMaybeBox(pLevel, pBox, pRandom, 0.8F, 0, 2, 0, 2, 2, $$11, CAVE_AIR, CAVE_AIR, false, false);
            if (this.spiderCorridor) {
               this.generateMaybeBox(pLevel, pBox, pRandom, 0.6F, 0, 0, 0, 2, 1, $$11, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
            }

            for (int $$13 = 0; $$13 < this.numSections; $$13++) {
               int $$14 = 2 + $$13 * 5;
               this.placeSupport(pLevel, pBox, 0, 0, $$14, 2, 2, pRandom);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.1F, 0, 2, $$14 - 1);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.1F, 2, 2, $$14 - 1);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.1F, 0, 2, $$14 + 1);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.1F, 2, 2, $$14 + 1);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.05F, 0, 2, $$14 - 2);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.05F, 2, 2, $$14 - 2);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.05F, 0, 2, $$14 + 2);
               this.maybePlaceCobWeb(pLevel, pBox, pRandom, 0.05F, 2, 2, $$14 + 2);
               if (pRandom.nextInt(100) == 0) {
                  this.createChest(pLevel, pBox, pRandom, 2, 0, $$14 - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (pRandom.nextInt(100) == 0) {
                  this.createChest(pLevel, pBox, pRandom, 0, 0, $$14 + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
               }

               if (this.spiderCorridor && !this.hasPlacedSpider) {
                  int $$15 = 1;
                  int $$16 = $$14 - 1 + pRandom.nextInt(3);
                  BlockPos $$17 = this.getWorldPos(1, 0, $$16);
                  if (pBox.isInside($$17) && this.isInterior(pLevel, 1, 0, $$16, pBox)) {
                     this.hasPlacedSpider = true;
                     pLevel.setBlock($$17, Blocks.SPAWNER.defaultBlockState(), 2);
                     if (pLevel.getBlockEntity($$17) instanceof SpawnerBlockEntity $$19) {
                        $$19.setEntityId(EntityType.CAVE_SPIDER, pRandom);
                     }
                  }
               }
            }

            for (int $$20 = 0; $$20 <= 2; $$20++) {
               for (int $$21 = 0; $$21 <= $$11; $$21++) {
                  this.setPlanksBlock(pLevel, pBox, $$12, $$20, -1, $$21);
               }
            }

            int $$22 = 2;
            this.placeDoubleLowerOrUpperSupport(pLevel, pBox, 0, -1, 2);
            if (this.numSections > 1) {
               int $$23 = $$11 - 2;
               this.placeDoubleLowerOrUpperSupport(pLevel, pBox, 0, -1, $$23);
            }

            if (this.hasRails) {
               BlockState $$24 = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

               for (int $$25 = 0; $$25 <= $$11; $$25++) {
                  BlockState $$26 = this.getBlock(pLevel, 1, -1, $$25, pBox);
                  if (!$$26.isAir() && $$26.isSolidRender(pLevel, this.getWorldPos(1, -1, $$25))) {
                     float $$27 = this.isInterior(pLevel, 1, 0, $$25, pBox) ? 0.7F : 0.9F;
                     this.maybeGenerateBlock(pLevel, pBox, pRandom, $$27, 1, 0, $$25, $$24);
                  }
               }
            }
         }
      }

      private void placeDoubleLowerOrUpperSupport(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ) {
         BlockState $$5 = this.type.getWoodState();
         BlockState $$6 = this.type.getPlanksState();
         if (this.getBlock(pLevel, pX, pY, pZ, pBox).is($$6.getBlock())) {
            this.fillPillarDownOrChainUp(pLevel, $$5, pX, pY, pZ, pBox);
         }

         if (this.getBlock(pLevel, pX + 2, pY, pZ, pBox).is($$6.getBlock())) {
            this.fillPillarDownOrChainUp(pLevel, $$5, pX + 2, pY, pZ, pBox);
         }
      }

      @Override
      protected void fillColumnDown(WorldGenLevel pLevel, BlockState pState, int pX, int pY, int pZ, BoundingBox pBox) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(pX, pY, pZ);
         if (pBox.isInside($$6)) {
            int $$7 = $$6.getY();

            while (this.isReplaceableByStructures(pLevel.getBlockState($$6)) && $$6.getY() > pLevel.getMinBuildHeight() + 1) {
               $$6.move(Direction.DOWN);
            }

            if (this.canPlaceColumnOnTopOf(pLevel, $$6, pLevel.getBlockState($$6))) {
               while ($$6.getY() < $$7) {
                  $$6.move(Direction.UP);
                  pLevel.setBlock($$6, pState, 2);
               }
            }
         }
      }

      protected void fillPillarDownOrChainUp(WorldGenLevel pLevel, BlockState pState, int pX, int pY, int pZ, BoundingBox pBox) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(pX, pY, pZ);
         if (pBox.isInside($$6)) {
            int $$7 = $$6.getY();
            int $$8 = 1;
            boolean $$9 = true;

            for (boolean $$10 = true; $$9 || $$10; $$8++) {
               if ($$9) {
                  $$6.setY($$7 - $$8);
                  BlockState $$11 = pLevel.getBlockState($$6);
                  boolean $$12 = this.isReplaceableByStructures($$11) && !$$11.is(Blocks.LAVA);
                  if (!$$12 && this.canPlaceColumnOnTopOf(pLevel, $$6, $$11)) {
                     fillColumnBetween(pLevel, pState, $$6, $$7 - $$8 + 1, $$7);
                     return;
                  }

                  $$9 = $$8 <= 20 && $$12 && $$6.getY() > pLevel.getMinBuildHeight() + 1;
               }

               if ($$10) {
                  $$6.setY($$7 + $$8);
                  BlockState $$13 = pLevel.getBlockState($$6);
                  boolean $$14 = this.isReplaceableByStructures($$13);
                  if (!$$14 && this.canHangChainBelow(pLevel, $$6, $$13)) {
                     pLevel.setBlock($$6.setY($$7 + 1), this.type.getFenceState(), 2);
                     fillColumnBetween(pLevel, Blocks.CHAIN.defaultBlockState(), $$6, $$7 + 2, $$7 + $$8);
                     return;
                  }

                  $$10 = $$8 <= 50 && $$14 && $$6.getY() < pLevel.getMaxBuildHeight() - 1;
               }
            }
         }
      }

      private static void fillColumnBetween(WorldGenLevel pLevel, BlockState pState, BlockPos.MutableBlockPos pPos, int pMinY, int pMaxY) {
         for (int $$5 = pMinY; $$5 < pMaxY; $$5++) {
            pLevel.setBlock(pPos.setY($$5), pState, 2);
         }
      }

      private boolean canPlaceColumnOnTopOf(LevelReader pLevel, BlockPos pPos, BlockState pState) {
         return pState.isFaceSturdy(pLevel, pPos, Direction.UP);
      }

      private boolean canHangChainBelow(LevelReader pLevel, BlockPos pPos, BlockState pState) {
         return Block.canSupportCenter(pLevel, pPos, Direction.DOWN) && !(pState.getBlock() instanceof FallingBlock);
      }

      private void placeSupport(WorldGenLevel pLevel, BoundingBox pBox, int pMinX, int pMinY, int pZ, int pMaxY, int pMaxX, RandomSource pRandom) {
         if (this.isSupportingBox(pLevel, pBox, pMinX, pMaxX, pMaxY, pZ)) {
            BlockState $$8 = this.type.getPlanksState();
            BlockState $$9 = this.type.getFenceState();
            this.generateBox(pLevel, pBox, pMinX, pMinY, pZ, pMinX, pMaxY - 1, pZ, $$9.setValue(FenceBlock.WEST, Boolean.valueOf(true)), CAVE_AIR, false);
            this.generateBox(pLevel, pBox, pMaxX, pMinY, pZ, pMaxX, pMaxY - 1, pZ, $$9.setValue(FenceBlock.EAST, Boolean.valueOf(true)), CAVE_AIR, false);
            if (pRandom.nextInt(4) == 0) {
               this.generateBox(pLevel, pBox, pMinX, pMaxY, pZ, pMinX, pMaxY, pZ, $$8, CAVE_AIR, false);
               this.generateBox(pLevel, pBox, pMaxX, pMaxY, pZ, pMaxX, pMaxY, pZ, $$8, CAVE_AIR, false);
            } else {
               this.generateBox(pLevel, pBox, pMinX, pMaxY, pZ, pMaxX, pMaxY, pZ, $$8, CAVE_AIR, false);
               this.maybeGenerateBlock(
                  pLevel,
                  pBox,
                  pRandom,
                  0.05F,
                  pMinX + 1,
                  pMaxY,
                  pZ - 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH)
               );
               this.maybeGenerateBlock(
                  pLevel,
                  pBox,
                  pRandom,
                  0.05F,
                  pMinX + 1,
                  pMaxY,
                  pZ + 1,
                  Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH)
               );
            }
         }
      }

      private void maybePlaceCobWeb(WorldGenLevel pLevel, BoundingBox pBox, RandomSource pRandom, float pChance, int pX, int pY, int pZ) {
         if (this.isInterior(pLevel, pX, pY, pZ, pBox) && pRandom.nextFloat() < pChance && this.hasSturdyNeighbours(pLevel, pBox, pX, pY, pZ, 2)) {
            this.placeBlock(pLevel, Blocks.COBWEB.defaultBlockState(), pX, pY, pZ, pBox);
         }
      }

      private boolean hasSturdyNeighbours(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ, int pRequired) {
         BlockPos.MutableBlockPos $$6 = this.getWorldPos(pX, pY, pZ);
         int $$7 = 0;

         for (Direction $$8 : Direction.values()) {
            $$6.move($$8);
            if (pBox.isInside($$6) && pLevel.getBlockState($$6).isFaceSturdy(pLevel, $$6, $$8.getOpposite())) {
               if (++$$7 >= pRequired) {
                  return true;
               }
            }

            $$6.move($$8.getOpposite());
         }

         return false;
      }
   }

   public static class MineShaftCrossing extends MineshaftPieces.MineShaftPiece {
      private final Direction direction;
      private final boolean isTwoFloored;

      public MineShaftCrossing(CompoundTag pTag) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, pTag);
         this.isTwoFloored = pTag.getBoolean("tf");
         this.direction = Direction.from2DDataValue(pTag.getInt("D"));
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("tf", this.isTwoFloored);
         pTag.putInt("D", this.direction.get2DDataValue());
      }

      public MineShaftCrossing(int pGenDepth, BoundingBox pBoundingBox, @Nullable Direction pDirection, MineshaftStructure.Type pType) {
         super(StructurePieceType.MINE_SHAFT_CROSSING, pGenDepth, pType, pBoundingBox);
         this.direction = pDirection;
         this.isTwoFloored = pBoundingBox.getYSpan() > 3;
      }

      @Nullable
      public static BoundingBox findCrossing(StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection) {
         int $$6;
         if (pRandom.nextInt(4) == 0) {
            $$6 = 6;
         } else {
            $$6 = 2;
         }
         BoundingBox $$11 = switch (pDirection) {
            default -> new BoundingBox(-1, 0, -4, 3, $$6, 0);
            case SOUTH -> new BoundingBox(-1, 0, 0, 3, $$6, 4);
            case WEST -> new BoundingBox(-4, 0, -1, 0, $$6, 3);
            case EAST -> new BoundingBox(0, 0, -1, 4, $$6, 3);
         };
         $$11.move(pX, pY, pZ);
         return pPieces.findCollisionPiece($$11) != null ? null : $$11;
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = this.getGenDepth();
         switch (this.direction) {
            case NORTH:
            default:
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
               break;
            case SOUTH:
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
               break;
            case WEST:
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
               break;
            case EAST:
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
         }

         if (this.isTwoFloored) {
            if (pRandom.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() - 1, Direction.NORTH, $$3
               );
            }

            if (pRandom.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.WEST, $$3
               );
            }

            if (pRandom.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.EAST, $$3
               );
            }

            if (pRandom.nextBoolean()) {
               MineshaftPieces.generateAndAddPiece(
                  pPiece, pPieces, pRandom, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
               );
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
         if (!this.isInInvalidLocation(pLevel, pBox)) {
            BlockState $$7 = this.type.getPlanksState();
            if (this.isTwoFloored) {
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.minY() + 3 - 1,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX(),
                  this.boundingBox.maxY() - 2,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.minY() + 3,
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            } else {
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX() + 1,
                  this.boundingBox.minY(),
                  this.boundingBox.minZ(),
                  this.boundingBox.maxX() - 1,
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ(),
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
               this.generateBox(
                  pLevel,
                  pBox,
                  this.boundingBox.minX(),
                  this.boundingBox.minY(),
                  this.boundingBox.minZ() + 1,
                  this.boundingBox.maxX(),
                  this.boundingBox.maxY(),
                  this.boundingBox.maxZ() - 1,
                  CAVE_AIR,
                  CAVE_AIR,
                  false
               );
            }

            this.placeSupportPillar(pLevel, pBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(pLevel, pBox, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            this.placeSupportPillar(pLevel, pBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(pLevel, pBox, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            int $$8 = this.boundingBox.minY() - 1;

            for (int $$9 = this.boundingBox.minX(); $$9 <= this.boundingBox.maxX(); $$9++) {
               for (int $$10 = this.boundingBox.minZ(); $$10 <= this.boundingBox.maxZ(); $$10++) {
                  this.setPlanksBlock(pLevel, pBox, $$7, $$9, $$8, $$10);
               }
            }
         }
      }

      private void placeSupportPillar(WorldGenLevel pLevel, BoundingBox pBox, int pX, int pY, int pZ, int pMaxY) {
         if (!this.getBlock(pLevel, pX, pMaxY + 1, pZ, pBox).isAir()) {
            this.generateBox(pLevel, pBox, pX, pY, pZ, pX, pMaxY, pZ, this.type.getPlanksState(), CAVE_AIR, false);
         }
      }
   }

   abstract static class MineShaftPiece extends StructurePiece {
      protected MineshaftStructure.Type type;

      public MineShaftPiece(StructurePieceType pStructurePieceType, int pGenDepth, MineshaftStructure.Type pType, BoundingBox pBoundingBox) {
         super(pStructurePieceType, pGenDepth, pBoundingBox);
         this.type = pType;
      }

      public MineShaftPiece(StructurePieceType pType, CompoundTag pTag) {
         super(pType, pTag);
         this.type = MineshaftStructure.Type.byId(pTag.getInt("MST"));
      }

      @Override
      protected boolean canBeReplaced(LevelReader pLevel, int pX, int pY, int pZ, BoundingBox pBox) {
         BlockState $$5 = this.getBlock(pLevel, pX, pY, pZ, pBox);
         return !$$5.is(this.type.getPlanksState().getBlock())
            && !$$5.is(this.type.getWoodState().getBlock())
            && !$$5.is(this.type.getFenceState().getBlock())
            && !$$5.is(Blocks.CHAIN);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         pTag.putInt("MST", this.type.ordinal());
      }

      protected boolean isSupportingBox(BlockGetter pLevel, BoundingBox pBox, int pXStart, int pXEnd, int pY, int pZ) {
         for (int $$6 = pXStart; $$6 <= pXEnd; $$6++) {
            if (this.getBlock(pLevel, $$6, pY + 1, pZ, pBox).isAir()) {
               return false;
            }
         }

         return true;
      }

      protected boolean isInInvalidLocation(LevelAccessor pLevel, BoundingBox pBoundingBox) {
         int $$2 = Math.max(this.boundingBox.minX() - 1, pBoundingBox.minX());
         int $$3 = Math.max(this.boundingBox.minY() - 1, pBoundingBox.minY());
         int $$4 = Math.max(this.boundingBox.minZ() - 1, pBoundingBox.minZ());
         int $$5 = Math.min(this.boundingBox.maxX() + 1, pBoundingBox.maxX());
         int $$6 = Math.min(this.boundingBox.maxY() + 1, pBoundingBox.maxY());
         int $$7 = Math.min(this.boundingBox.maxZ() + 1, pBoundingBox.maxZ());
         BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos(($$2 + $$5) / 2, ($$3 + $$6) / 2, ($$4 + $$7) / 2);
         if (pLevel.getBiome($$8).is(BiomeTags.MINESHAFT_BLOCKING)) {
            return true;
         } else {
            for (int $$9 = $$2; $$9 <= $$5; $$9++) {
               for (int $$10 = $$4; $$10 <= $$7; $$10++) {
                  if (pLevel.getBlockState($$8.set($$9, $$3, $$10)).liquid()) {
                     return true;
                  }

                  if (pLevel.getBlockState($$8.set($$9, $$6, $$10)).liquid()) {
                     return true;
                  }
               }
            }

            for (int $$11 = $$2; $$11 <= $$5; $$11++) {
               for (int $$12 = $$3; $$12 <= $$6; $$12++) {
                  if (pLevel.getBlockState($$8.set($$11, $$12, $$4)).liquid()) {
                     return true;
                  }

                  if (pLevel.getBlockState($$8.set($$11, $$12, $$7)).liquid()) {
                     return true;
                  }
               }
            }

            for (int $$13 = $$4; $$13 <= $$7; $$13++) {
               for (int $$14 = $$3; $$14 <= $$6; $$14++) {
                  if (pLevel.getBlockState($$8.set($$2, $$14, $$13)).liquid()) {
                     return true;
                  }

                  if (pLevel.getBlockState($$8.set($$5, $$14, $$13)).liquid()) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      protected void setPlanksBlock(WorldGenLevel pLevel, BoundingBox pBox, BlockState pPlankState, int pX, int pY, int pZ) {
         if (this.isInterior(pLevel, pX, pY, pZ, pBox)) {
            BlockPos $$6 = this.getWorldPos(pX, pY, pZ);
            BlockState $$7 = pLevel.getBlockState($$6);
            if (!$$7.isFaceSturdy(pLevel, $$6, Direction.UP)) {
               pLevel.setBlock($$6, pPlankState, 2);
            }
         }
      }
   }

   public static class MineShaftRoom extends MineshaftPieces.MineShaftPiece {
      private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

      public MineShaftRoom(int pGenDepth, RandomSource pRandom, int pX, int pZ, MineshaftStructure.Type pType) {
         super(
            StructurePieceType.MINE_SHAFT_ROOM,
            pGenDepth,
            pType,
            new BoundingBox(pX, 50, pZ, pX + 7 + pRandom.nextInt(6), 54 + pRandom.nextInt(6), pZ + 7 + pRandom.nextInt(6))
         );
         this.type = pType;
      }

      public MineShaftRoom(CompoundTag pTag) {
         super(StructurePieceType.MINE_SHAFT_ROOM, pTag);
         BoundingBox.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, pTag.getList("Entrances", 11))
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(this.childEntranceBoxes::addAll);
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = this.getGenDepth();
         int $$4 = this.boundingBox.getYSpan() - 3 - 1;
         if ($$4 <= 0) {
            $$4 = 1;
         }

         int $$5 = 0;

         while ($$5 < this.boundingBox.getXSpan()) {
            $$5 += pRandom.nextInt(this.boundingBox.getXSpan());
            if ($$5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$6 = MineshaftPieces.generateAndAddPiece(
               pPiece,
               pPieces,
               pRandom,
               this.boundingBox.minX() + $$5,
               this.boundingBox.minY() + pRandom.nextInt($$4) + 1,
               this.boundingBox.minZ() - 1,
               Direction.NORTH,
               $$3
            );
            if ($$6 != null) {
               BoundingBox $$7 = $$6.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox($$7.minX(), $$7.minY(), this.boundingBox.minZ(), $$7.maxX(), $$7.maxY(), this.boundingBox.minZ() + 1));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getXSpan()) {
            $$5 += pRandom.nextInt(this.boundingBox.getXSpan());
            if ($$5 + 3 > this.boundingBox.getXSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$8 = MineshaftPieces.generateAndAddPiece(
               pPiece,
               pPieces,
               pRandom,
               this.boundingBox.minX() + $$5,
               this.boundingBox.minY() + pRandom.nextInt($$4) + 1,
               this.boundingBox.maxZ() + 1,
               Direction.SOUTH,
               $$3
            );
            if ($$8 != null) {
               BoundingBox $$9 = $$8.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox($$9.minX(), $$9.minY(), this.boundingBox.maxZ() - 1, $$9.maxX(), $$9.maxY(), this.boundingBox.maxZ()));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getZSpan()) {
            $$5 += pRandom.nextInt(this.boundingBox.getZSpan());
            if ($$5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            MineshaftPieces.MineShaftPiece $$10 = MineshaftPieces.generateAndAddPiece(
               pPiece,
               pPieces,
               pRandom,
               this.boundingBox.minX() - 1,
               this.boundingBox.minY() + pRandom.nextInt($$4) + 1,
               this.boundingBox.minZ() + $$5,
               Direction.WEST,
               $$3
            );
            if ($$10 != null) {
               BoundingBox $$11 = $$10.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.minX(), $$11.minY(), $$11.minZ(), this.boundingBox.minX() + 1, $$11.maxY(), $$11.maxZ()));
            }

            $$5 += 4;
         }

         $$5 = 0;

         while ($$5 < this.boundingBox.getZSpan()) {
            $$5 += pRandom.nextInt(this.boundingBox.getZSpan());
            if ($$5 + 3 > this.boundingBox.getZSpan()) {
               break;
            }

            StructurePiece $$12 = MineshaftPieces.generateAndAddPiece(
               pPiece,
               pPieces,
               pRandom,
               this.boundingBox.maxX() + 1,
               this.boundingBox.minY() + pRandom.nextInt($$4) + 1,
               this.boundingBox.minZ() + $$5,
               Direction.EAST,
               $$3
            );
            if ($$12 != null) {
               BoundingBox $$13 = $$12.getBoundingBox();
               this.childEntranceBoxes
                  .add(new BoundingBox(this.boundingBox.maxX() - 1, $$13.minY(), $$13.minZ(), this.boundingBox.maxX(), $$13.maxY(), $$13.maxZ()));
            }

            $$5 += 4;
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
         if (!this.isInInvalidLocation(pLevel, pBox)) {
            this.generateBox(
               pLevel,
               pBox,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 1,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               CAVE_AIR,
               false
            );

            for (BoundingBox $$7 : this.childEntranceBoxes) {
               this.generateBox(pLevel, pBox, $$7.minX(), $$7.maxY() - 2, $$7.minZ(), $$7.maxX(), $$7.maxY(), $$7.maxZ(), CAVE_AIR, CAVE_AIR, false);
            }

            this.generateUpperHalfSphere(
               pLevel,
               pBox,
               this.boundingBox.minX(),
               this.boundingBox.minY() + 4,
               this.boundingBox.minZ(),
               this.boundingBox.maxX(),
               this.boundingBox.maxY(),
               this.boundingBox.maxZ(),
               CAVE_AIR,
               false
            );
         }
      }

      @Override
      public void move(int pX, int pY, int pZ) {
         super.move(pX, pY, pZ);

         for (BoundingBox $$3 : this.childEntranceBoxes) {
            $$3.move(pX, pY, pZ);
         }
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         BoundingBox.CODEC
            .listOf()
            .encodeStart(NbtOps.INSTANCE, this.childEntranceBoxes)
            .resultOrPartial(MineshaftPieces.LOGGER::error)
            .ifPresent(p_227930_ -> pTag.put("Entrances", p_227930_));
      }
   }

   public static class MineShaftStairs extends MineshaftPieces.MineShaftPiece {
      public MineShaftStairs(int pGenDepth, BoundingBox pBoundingBox, Direction pOrientation, MineshaftStructure.Type pType) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, pGenDepth, pType, pBoundingBox);
         this.setOrientation(pOrientation);
      }

      public MineShaftStairs(CompoundTag pTag) {
         super(StructurePieceType.MINE_SHAFT_STAIRS, pTag);
      }

      @Nullable
      public static BoundingBox findStairs(StructurePieceAccessor pPieces, RandomSource pRandom, int pX, int pY, int pZ, Direction pDirection) {
         BoundingBox $$9 = switch (pDirection) {
            default -> new BoundingBox(0, -5, -8, 2, 2, 0);
            case SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
            case WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
            case EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
         };
         $$9.move(pX, pY, pZ);
         return pPieces.findCollisionPiece($$9) != null ? null : $$9;
      }

      @Override
      public void addChildren(StructurePiece pPiece, StructurePieceAccessor pPieces, RandomSource pRandom) {
         int $$3 = this.getGenDepth();
         Direction $$4 = this.getOrientation();
         if ($$4 != null) {
            switch ($$4) {
               case NORTH:
               default:
                  MineshaftPieces.generateAndAddPiece(
                     pPiece, pPieces, pRandom, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, $$3
                  );
                  break;
               case SOUTH:
                  MineshaftPieces.generateAndAddPiece(
                     pPiece, pPieces, pRandom, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, $$3
                  );
                  break;
               case WEST:
                  MineshaftPieces.generateAndAddPiece(
                     pPiece, pPieces, pRandom, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, $$3
                  );
                  break;
               case EAST:
                  MineshaftPieces.generateAndAddPiece(
                     pPiece, pPieces, pRandom, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, $$3
                  );
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
         if (!this.isInInvalidLocation(pLevel, pBox)) {
            this.generateBox(pLevel, pBox, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(pLevel, pBox, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);

            for (int $$7 = 0; $$7 < 5; $$7++) {
               this.generateBox(pLevel, pBox, 0, 5 - $$7 - ($$7 < 4 ? 1 : 0), 2 + $$7, 2, 7 - $$7, 2 + $$7, CAVE_AIR, CAVE_AIR, false);
            }
         }
      }
   }
}
