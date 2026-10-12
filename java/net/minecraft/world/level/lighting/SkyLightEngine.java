package net.minecraft.world.level.lighting;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jetbrains.annotations.VisibleForTesting;

public final class SkyLightEngine extends LightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
   private static final long REMOVE_TOP_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(15);
   private static final long REMOVE_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseSkipOneDirection(15, Direction.UP);
   private static final long ADD_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.increaseSkipOneDirection(15, false, Direction.UP);
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
   private final ChunkSkyLightSources emptyChunkSources;

   public SkyLightEngine(LightChunkGetter pChunkSource) {
      this(pChunkSource, new SkyLightSectionStorage(pChunkSource));
   }

   @VisibleForTesting
   protected SkyLightEngine(LightChunkGetter pChunkSource, SkyLightSectionStorage pSectionStorage) {
      super(pChunkSource, pSectionStorage);
      this.emptyChunkSources = new ChunkSkyLightSources(pChunkSource.getLevel());
   }

   private static boolean isSourceLevel(int pLevel) {
      return pLevel == 15;
   }

   private int getLowestSourceY(int pX, int pZ, int pDefaultReturnValue) {
      ChunkSkyLightSources $$3 = this.getChunkSources(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ));
      return $$3 == null ? pDefaultReturnValue : $$3.getLowestSourceY(SectionPos.sectionRelative(pX), SectionPos.sectionRelative(pZ));
   }

   @Nullable
   private ChunkSkyLightSources getChunkSources(int pChunkX, int pChunkZ) {
      LightChunk $$2 = this.chunkSource.getChunkForLighting(pChunkX, pChunkZ);
      return $$2 != null ? $$2.getSkyLightSources() : null;
   }

   @Override
   protected void checkNode(long pLevelPos) {
      int $$1 = BlockPos.getX(pLevelPos);
      int $$2 = BlockPos.getY(pLevelPos);
      int $$3 = BlockPos.getZ(pLevelPos);
      long $$4 = SectionPos.blockToSection(pLevelPos);
      int $$5 = this.storage.lightOnInSection($$4) ? this.getLowestSourceY($$1, $$3, Integer.MAX_VALUE) : Integer.MAX_VALUE;
      if ($$5 != Integer.MAX_VALUE) {
         this.updateSourcesInColumn($$1, $$3, $$5);
      }

      if (this.storage.storingLightForSection($$4)) {
         boolean $$6 = $$2 >= $$5;
         if ($$6) {
            this.enqueueDecrease(pLevelPos, REMOVE_SKY_SOURCE_ENTRY);
            this.enqueueIncrease(pLevelPos, ADD_SKY_SOURCE_ENTRY);
         } else {
            int $$7 = this.storage.getStoredLevel(pLevelPos);
            if ($$7 > 0) {
               this.storage.setStoredLevel(pLevelPos, 0);
               this.enqueueDecrease(pLevelPos, LightEngine.QueueEntry.decreaseAllDirections($$7));
            } else {
               this.enqueueDecrease(pLevelPos, PULL_LIGHT_IN_ENTRY);
            }
         }
      }
   }

   private void updateSourcesInColumn(int pX, int pZ, int pLowestY) {
      int $$3 = SectionPos.sectionToBlockCoord(this.storage.getBottomSectionY());
      this.removeSourcesBelow(pX, pZ, pLowestY, $$3);
      this.addSourcesAbove(pX, pZ, pLowestY, $$3);
   }

   private void removeSourcesBelow(int pX, int pZ, int pMinY, int pBottomSectionY) {
      if (pMinY > pBottomSectionY) {
         int $$4 = SectionPos.blockToSectionCoord(pX);
         int $$5 = SectionPos.blockToSectionCoord(pZ);
         int $$6 = pMinY - 1;

         for (int $$7 = SectionPos.blockToSectionCoord($$6); this.storage.hasLightDataAtOrBelow($$7); $$7--) {
            if (this.storage.storingLightForSection(SectionPos.asLong($$4, $$7, $$5))) {
               int $$8 = SectionPos.sectionToBlockCoord($$7);
               int $$9 = $$8 + 15;

               for (int $$10 = Math.min($$9, $$6); $$10 >= $$8; $$10--) {
                  long $$11 = BlockPos.asLong(pX, $$10, pZ);
                  if (!isSourceLevel(this.storage.getStoredLevel($$11))) {
                     return;
                  }

                  this.storage.setStoredLevel($$11, 0);
                  this.enqueueDecrease($$11, $$10 == pMinY - 1 ? REMOVE_TOP_SKY_SOURCE_ENTRY : REMOVE_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   private void addSourcesAbove(int pX, int pZ, int pMaxY, int pBottomSectionY) {
      int $$4 = SectionPos.blockToSectionCoord(pX);
      int $$5 = SectionPos.blockToSectionCoord(pZ);
      int $$6 = Math.max(
         Math.max(this.getLowestSourceY(pX - 1, pZ, Integer.MIN_VALUE), this.getLowestSourceY(pX + 1, pZ, Integer.MIN_VALUE)),
         Math.max(this.getLowestSourceY(pX, pZ - 1, Integer.MIN_VALUE), this.getLowestSourceY(pX, pZ + 1, Integer.MIN_VALUE))
      );
      int $$7 = Math.max(pMaxY, pBottomSectionY);

      for (long $$8 = SectionPos.asLong($$4, SectionPos.blockToSectionCoord($$7), $$5);
         !this.storage.isAboveData($$8);
         $$8 = SectionPos.offset($$8, Direction.UP)
      ) {
         if (this.storage.storingLightForSection($$8)) {
            int $$9 = SectionPos.sectionToBlockCoord(SectionPos.y($$8));
            int $$10 = $$9 + 15;

            for (int $$11 = Math.max($$9, $$7); $$11 <= $$10; $$11++) {
               long $$12 = BlockPos.asLong(pX, $$11, pZ);
               if (isSourceLevel(this.storage.getStoredLevel($$12))) {
                  return;
               }

               this.storage.setStoredLevel($$12, 15);
               if ($$11 < $$6 || $$11 == pMaxY) {
                  this.enqueueIncrease($$12, ADD_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   @Override
   protected void propagateIncrease(long pPackedPos, long pQueueEntry, int pLightLevel) {
      BlockState $$3 = null;
      int $$4 = this.countEmptySectionsBelowIfAtBorder(pPackedPos);

      for (Direction $$5 : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(pQueueEntry, $$5)) {
            long $$6 = BlockPos.offset(pPackedPos, $$5);
            if (this.storage.storingLightForSection(SectionPos.blockToSection($$6))) {
               int $$7 = this.storage.getStoredLevel($$6);
               int $$8 = pLightLevel - 1;
               if ($$8 > $$7) {
                  this.mutablePos.set($$6);
                  BlockState $$9 = this.getState(this.mutablePos);
                  int $$10 = pLightLevel - this.getOpacity($$9, this.mutablePos);
                  if ($$10 > $$7) {
                     if ($$3 == null) {
                        $$3 = LightEngine.QueueEntry.isFromEmptyShape(pQueueEntry)
                           ? Blocks.AIR.defaultBlockState()
                           : this.getState(this.mutablePos.set(pPackedPos));
                     }

                     if (!this.shapeOccludes(pPackedPos, $$3, $$6, $$9, $$5)) {
                        this.storage.setStoredLevel($$6, $$10);
                        if ($$10 > 1) {
                           this.enqueueIncrease($$6, LightEngine.QueueEntry.increaseSkipOneDirection($$10, isEmptyShape($$9), $$5.getOpposite()));
                        }

                        this.propagateFromEmptySections($$6, $$5, $$10, true, $$4);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void propagateDecrease(long pPackedPos, long pLightLevel) {
      int $$2 = this.countEmptySectionsBelowIfAtBorder(pPackedPos);
      int $$3 = LightEngine.QueueEntry.getFromLevel(pLightLevel);

      for (Direction $$4 : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(pLightLevel, $$4)) {
            long $$5 = BlockPos.offset(pPackedPos, $$4);
            if (this.storage.storingLightForSection(SectionPos.blockToSection($$5))) {
               int $$6 = this.storage.getStoredLevel($$5);
               if ($$6 != 0) {
                  if ($$6 <= $$3 - 1) {
                     this.storage.setStoredLevel($$5, 0);
                     this.enqueueDecrease($$5, LightEngine.QueueEntry.decreaseSkipOneDirection($$6, $$4.getOpposite()));
                     this.propagateFromEmptySections($$5, $$4, $$6, false, $$2);
                  } else {
                     this.enqueueIncrease($$5, LightEngine.QueueEntry.increaseOnlyOneDirection($$6, false, $$4.getOpposite()));
                  }
               }
            }
         }
      }
   }

   private int countEmptySectionsBelowIfAtBorder(long pPackedPos) {
      int $$1 = BlockPos.getY(pPackedPos);
      int $$2 = SectionPos.sectionRelative($$1);
      if ($$2 != 0) {
         return 0;
      } else {
         int $$3 = BlockPos.getX(pPackedPos);
         int $$4 = BlockPos.getZ(pPackedPos);
         int $$5 = SectionPos.sectionRelative($$3);
         int $$6 = SectionPos.sectionRelative($$4);
         if ($$5 != 0 && $$5 != 15 && $$6 != 0 && $$6 != 15) {
            return 0;
         } else {
            int $$7 = SectionPos.blockToSectionCoord($$3);
            int $$8 = SectionPos.blockToSectionCoord($$1);
            int $$9 = SectionPos.blockToSectionCoord($$4);
            int $$10 = 0;

            while (!this.storage.storingLightForSection(SectionPos.asLong($$7, $$8 - $$10 - 1, $$9)) && this.storage.hasLightDataAtOrBelow($$8 - $$10 - 1)) {
               $$10++;
            }

            return $$10;
         }
      }
   }

   private void propagateFromEmptySections(long pPackedPos, Direction pDirection, int pLevel, boolean pShouldIncrease, int pEmptySections) {
      if (pEmptySections != 0) {
         int $$5 = BlockPos.getX(pPackedPos);
         int $$6 = BlockPos.getZ(pPackedPos);
         if (crossedSectionEdge(pDirection, SectionPos.sectionRelative($$5), SectionPos.sectionRelative($$6))) {
            int $$7 = BlockPos.getY(pPackedPos);
            int $$8 = SectionPos.blockToSectionCoord($$5);
            int $$9 = SectionPos.blockToSectionCoord($$6);
            int $$10 = SectionPos.blockToSectionCoord($$7) - 1;
            int $$11 = $$10 - pEmptySections + 1;

            while ($$10 >= $$11) {
               if (!this.storage.storingLightForSection(SectionPos.asLong($$8, $$10, $$9))) {
                  $$10--;
               } else {
                  int $$12 = SectionPos.sectionToBlockCoord($$10);

                  for (int $$13 = 15; $$13 >= 0; $$13--) {
                     long $$14 = BlockPos.asLong($$5, $$12 + $$13, $$6);
                     if (pShouldIncrease) {
                        this.storage.setStoredLevel($$14, pLevel);
                        if (pLevel > 1) {
                           this.enqueueIncrease($$14, LightEngine.QueueEntry.increaseSkipOneDirection(pLevel, true, pDirection.getOpposite()));
                        }
                     } else {
                        this.storage.setStoredLevel($$14, 0);
                        this.enqueueDecrease($$14, LightEngine.QueueEntry.decreaseSkipOneDirection(pLevel, pDirection.getOpposite()));
                     }
                  }

                  $$10--;
               }
            }
         }
      }
   }

   private static boolean crossedSectionEdge(Direction pDirection, int pX, int pZ) {
      return switch (pDirection) {
         case NORTH -> pZ == 15;
         case SOUTH -> pZ == 0;
         case WEST -> pX == 15;
         case EAST -> pX == 0;
         default -> false;
      };
   }

   @Override
   public void setLightEnabled(ChunkPos pChunkPos, boolean pLightEnabled) {
      super.setLightEnabled(pChunkPos, pLightEnabled);
      if (pLightEnabled) {
         ChunkSkyLightSources $$2 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z), this.emptyChunkSources);
         int $$3 = $$2.getHighestLowestSourceY() - 1;
         int $$4 = SectionPos.blockToSectionCoord($$3) + 1;
         long $$5 = SectionPos.getZeroNode(pChunkPos.x, pChunkPos.z);
         int $$6 = this.storage.getTopSectionY($$5);
         int $$7 = Math.max(this.storage.getBottomSectionY(), $$4);

         for (int $$8 = $$6 - 1; $$8 >= $$7; $$8--) {
            DataLayer $$9 = this.storage.getDataLayerToWrite(SectionPos.asLong(pChunkPos.x, $$8, pChunkPos.z));
            if ($$9 != null && $$9.isEmpty()) {
               $$9.fill(15);
            }
         }
      }
   }

   @Override
   public void propagateLightSources(ChunkPos pChunkPos) {
      long $$1 = SectionPos.getZeroNode(pChunkPos.x, pChunkPos.z);
      this.storage.setLightEnabled($$1, true);
      ChunkSkyLightSources $$2 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z), this.emptyChunkSources);
      ChunkSkyLightSources $$3 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z - 1), this.emptyChunkSources);
      ChunkSkyLightSources $$4 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z + 1), this.emptyChunkSources);
      ChunkSkyLightSources $$5 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x - 1, pChunkPos.z), this.emptyChunkSources);
      ChunkSkyLightSources $$6 = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x + 1, pChunkPos.z), this.emptyChunkSources);
      int $$7 = this.storage.getTopSectionY($$1);
      int $$8 = this.storage.getBottomSectionY();
      int $$9 = SectionPos.sectionToBlockCoord(pChunkPos.x);
      int $$10 = SectionPos.sectionToBlockCoord(pChunkPos.z);

      for (int $$11 = $$7 - 1; $$11 >= $$8; $$11--) {
         long $$12 = SectionPos.asLong(pChunkPos.x, $$11, pChunkPos.z);
         DataLayer $$13 = this.storage.getDataLayerToWrite($$12);
         if ($$13 != null) {
            int $$14 = SectionPos.sectionToBlockCoord($$11);
            int $$15 = $$14 + 15;
            boolean $$16 = false;

            for (int $$17 = 0; $$17 < 16; $$17++) {
               for (int $$18 = 0; $$18 < 16; $$18++) {
                  int $$19 = $$2.getLowestSourceY($$18, $$17);
                  if ($$19 <= $$15) {
                     int $$20 = $$17 == 0 ? $$3.getLowestSourceY($$18, 15) : $$2.getLowestSourceY($$18, $$17 - 1);
                     int $$21 = $$17 == 15 ? $$4.getLowestSourceY($$18, 0) : $$2.getLowestSourceY($$18, $$17 + 1);
                     int $$22 = $$18 == 0 ? $$5.getLowestSourceY(15, $$17) : $$2.getLowestSourceY($$18 - 1, $$17);
                     int $$23 = $$18 == 15 ? $$6.getLowestSourceY(0, $$17) : $$2.getLowestSourceY($$18 + 1, $$17);
                     int $$24 = Math.max(Math.max($$20, $$21), Math.max($$22, $$23));

                     for (int $$25 = $$15; $$25 >= Math.max($$14, $$19); $$25--) {
                        $$13.set($$18, SectionPos.sectionRelative($$25), $$17, 15);
                        if ($$25 == $$19 || $$25 < $$24) {
                           long $$26 = BlockPos.asLong($$9 + $$18, $$25, $$10 + $$17);
                           this.enqueueIncrease(
                              $$26, LightEngine.QueueEntry.increaseSkySourceInDirections($$25 == $$19, $$25 < $$20, $$25 < $$21, $$25 < $$22, $$25 < $$23)
                           );
                        }
                     }

                     if ($$19 < $$14) {
                        $$16 = true;
                     }
                  }
               }
            }

            if (!$$16) {
               break;
            }
         }
      }
   }
}
