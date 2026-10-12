package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VisGraph {
   private static final int SIZE_IN_BITS = 4;
   private static final int LEN = 16;
   private static final int MASK = 15;
   private static final int SIZE = 4096;
   private static final int X_SHIFT = 0;
   private static final int Z_SHIFT = 4;
   private static final int Y_SHIFT = 8;
   private static final int DX = (int)Math.pow(16.0, 0.0);
   private static final int DZ = (int)Math.pow(16.0, 1.0);
   private static final int DY = (int)Math.pow(16.0, 2.0);
   private static final int INVALID_INDEX = -1;
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BitSet bitSet = new BitSet(4096);
   private static final int[] INDEX_OF_EDGES = Util.make(new int[1352], p_112974_ -> {
      int $$1 = 0;
      int $$2 = 15;
      int $$3 = 0;

      for (int $$4 = 0; $$4 < 16; $$4++) {
         for (int $$5 = 0; $$5 < 16; $$5++) {
            for (int $$6 = 0; $$6 < 16; $$6++) {
               if ($$4 == 0 || $$4 == 15 || $$5 == 0 || $$5 == 15 || $$6 == 0 || $$6 == 15) {
                  p_112974_[$$3++] = getIndex($$4, $$5, $$6);
               }
            }
         }
      }
   });
   private int empty = 4096;

   public void setOpaque(BlockPos pPos) {
      this.bitSet.set(getIndex(pPos), true);
      this.empty--;
   }

   private static int getIndex(BlockPos pPos) {
      return getIndex(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15);
   }

   private static int getIndex(int pX, int pY, int pZ) {
      return pX << 0 | pY << 8 | pZ << 4;
   }

   public VisibilitySet resolve() {
      VisibilitySet $$0 = new VisibilitySet();
      if (4096 - this.empty < 256) {
         $$0.setAll(true);
      } else if (this.empty == 0) {
         $$0.setAll(false);
      } else {
         for (int $$1 : INDEX_OF_EDGES) {
            if (!this.bitSet.get($$1)) {
               $$0.add(this.floodFill($$1));
            }
         }
      }

      return $$0;
   }

   private Set<Direction> floodFill(int pIndex) {
      Set<Direction> $$1 = EnumSet.noneOf(Direction.class);
      IntPriorityQueue $$2 = new IntArrayFIFOQueue();
      $$2.enqueue(pIndex);
      this.bitSet.set(pIndex, true);

      while (!$$2.isEmpty()) {
         int $$3 = $$2.dequeueInt();
         this.addEdges($$3, $$1);

         for (Direction $$4 : DIRECTIONS) {
            int $$5 = this.getNeighborIndexAtFace($$3, $$4);
            if ($$5 >= 0 && !this.bitSet.get($$5)) {
               this.bitSet.set($$5, true);
               $$2.enqueue($$5);
            }
         }
      }

      return $$1;
   }

   private void addEdges(int pIndex, Set<Direction> pFaces) {
      int $$2 = pIndex >> 0 & 15;
      if ($$2 == 0) {
         pFaces.add(Direction.WEST);
      } else if ($$2 == 15) {
         pFaces.add(Direction.EAST);
      }

      int $$3 = pIndex >> 8 & 15;
      if ($$3 == 0) {
         pFaces.add(Direction.DOWN);
      } else if ($$3 == 15) {
         pFaces.add(Direction.UP);
      }

      int $$4 = pIndex >> 4 & 15;
      if ($$4 == 0) {
         pFaces.add(Direction.NORTH);
      } else if ($$4 == 15) {
         pFaces.add(Direction.SOUTH);
      }
   }

   private int getNeighborIndexAtFace(int pIndex, Direction pFace) {
      switch (pFace) {
         case DOWN:
            if ((pIndex >> 8 & 15) == 0) {
               return -1;
            }

            return pIndex - DY;
         case UP:
            if ((pIndex >> 8 & 15) == 15) {
               return -1;
            }

            return pIndex + DY;
         case NORTH:
            if ((pIndex >> 4 & 15) == 0) {
               return -1;
            }

            return pIndex - DZ;
         case SOUTH:
            if ((pIndex >> 4 & 15) == 15) {
               return -1;
            }

            return pIndex + DZ;
         case WEST:
            if ((pIndex >> 0 & 15) == 0) {
               return -1;
            }

            return pIndex - DX;
         case EAST:
            if ((pIndex >> 0 & 15) == 15) {
               return -1;
            }

            return pIndex + DX;
         default:
            return -1;
      }
   }
}
