package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlyNodeEvaluator extends WalkNodeEvaluator {
   private final Long2ObjectMap<PathType> pathTypeByPosCache = new Long2ObjectOpenHashMap();
   private static final float SMALL_MOB_SIZE = 1.0F;
   private static final float SMALL_MOB_INFLATED_START_NODE_BOUNDING_BOX = 1.1F;
   private static final int MAX_START_NODE_CANDIDATES = 10;

   @Override
   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      this.pathTypeByPosCache.clear();
      pMob.onPathfindingStart();
   }

   @Override
   public void done() {
      this.mob.onPathfindingDone();
      this.pathTypeByPosCache.clear();
      super.done();
   }

   @Override
   public Node getStart() {
      int $$0;
      if (this.canFloat() && this.mob.isInWater()) {
         $$0 = this.mob.getBlockY();
         BlockPos.MutableBlockPos $$1 = new BlockPos.MutableBlockPos(this.mob.getX(), (double)$$0, this.mob.getZ());

         for (BlockState $$2 = this.currentContext.getBlockState($$1); $$2.is(Blocks.WATER); $$2 = this.currentContext.getBlockState($$1)) {
            $$1.set(this.mob.getX(), (double)(++$$0), this.mob.getZ());
         }
      } else {
         $$0 = Mth.floor(this.mob.getY() + 0.5);
      }

      BlockPos $$4 = BlockPos.containing(this.mob.getX(), (double)$$0, this.mob.getZ());
      if (!this.canStartAt($$4)) {
         for (BlockPos $$5 : this.iteratePathfindingStartNodeCandidatePositions(this.mob)) {
            if (this.canStartAt($$5)) {
               return super.getStartNode($$5);
            }
         }
      }

      return super.getStartNode($$4);
   }

   @Override
   protected boolean canStartAt(BlockPos pPos) {
      PathType $$1 = this.getCachedPathType(pPos.getX(), pPos.getY(), pPos.getZ());
      return this.mob.getPathfindingMalus($$1) >= 0.0F;
   }

   @Override
   public Target getTarget(double pX, double pY, double pZ) {
      return this.getTargetNodeAt(pX, pY, pZ);
   }

   @Override
   public int getNeighbors(Node[] pOutputArray, Node pNode) {
      int $$2 = 0;
      Node $$3 = this.findAcceptedNode(pNode.x, pNode.y, pNode.z + 1);
      if (this.isOpen($$3)) {
         pOutputArray[$$2++] = $$3;
      }

      Node $$4 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z);
      if (this.isOpen($$4)) {
         pOutputArray[$$2++] = $$4;
      }

      Node $$5 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z);
      if (this.isOpen($$5)) {
         pOutputArray[$$2++] = $$5;
      }

      Node $$6 = this.findAcceptedNode(pNode.x, pNode.y, pNode.z - 1);
      if (this.isOpen($$6)) {
         pOutputArray[$$2++] = $$6;
      }

      Node $$7 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z);
      if (this.isOpen($$7)) {
         pOutputArray[$$2++] = $$7;
      }

      Node $$8 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z);
      if (this.isOpen($$8)) {
         pOutputArray[$$2++] = $$8;
      }

      Node $$9 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z + 1);
      if (this.isOpen($$9) && this.hasMalus($$3) && this.hasMalus($$7)) {
         pOutputArray[$$2++] = $$9;
      }

      Node $$10 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z);
      if (this.isOpen($$10) && this.hasMalus($$4) && this.hasMalus($$7)) {
         pOutputArray[$$2++] = $$10;
      }

      Node $$11 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z);
      if (this.isOpen($$11) && this.hasMalus($$5) && this.hasMalus($$7)) {
         pOutputArray[$$2++] = $$11;
      }

      Node $$12 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z - 1);
      if (this.isOpen($$12) && this.hasMalus($$6) && this.hasMalus($$7)) {
         pOutputArray[$$2++] = $$12;
      }

      Node $$13 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z + 1);
      if (this.isOpen($$13) && this.hasMalus($$3) && this.hasMalus($$8)) {
         pOutputArray[$$2++] = $$13;
      }

      Node $$14 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z);
      if (this.isOpen($$14) && this.hasMalus($$4) && this.hasMalus($$8)) {
         pOutputArray[$$2++] = $$14;
      }

      Node $$15 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z);
      if (this.isOpen($$15) && this.hasMalus($$5) && this.hasMalus($$8)) {
         pOutputArray[$$2++] = $$15;
      }

      Node $$16 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z - 1);
      if (this.isOpen($$16) && this.hasMalus($$6) && this.hasMalus($$8)) {
         pOutputArray[$$2++] = $$16;
      }

      Node $$17 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z - 1);
      if (this.isOpen($$17) && this.hasMalus($$6) && this.hasMalus($$5)) {
         pOutputArray[$$2++] = $$17;
      }

      Node $$18 = this.findAcceptedNode(pNode.x + 1, pNode.y, pNode.z + 1);
      if (this.isOpen($$18) && this.hasMalus($$3) && this.hasMalus($$5)) {
         pOutputArray[$$2++] = $$18;
      }

      Node $$19 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z - 1);
      if (this.isOpen($$19) && this.hasMalus($$6) && this.hasMalus($$4)) {
         pOutputArray[$$2++] = $$19;
      }

      Node $$20 = this.findAcceptedNode(pNode.x - 1, pNode.y, pNode.z + 1);
      if (this.isOpen($$20) && this.hasMalus($$3) && this.hasMalus($$4)) {
         pOutputArray[$$2++] = $$20;
      }

      Node $$21 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z - 1);
      if (this.isOpen($$21)
         && this.hasMalus($$17)
         && this.hasMalus($$6)
         && this.hasMalus($$5)
         && this.hasMalus($$7)
         && this.hasMalus($$12)
         && this.hasMalus($$11)) {
         pOutputArray[$$2++] = $$21;
      }

      Node $$22 = this.findAcceptedNode(pNode.x + 1, pNode.y + 1, pNode.z + 1);
      if (this.isOpen($$22)
         && this.hasMalus($$18)
         && this.hasMalus($$3)
         && this.hasMalus($$5)
         && this.hasMalus($$7)
         && this.hasMalus($$9)
         && this.hasMalus($$11)) {
         pOutputArray[$$2++] = $$22;
      }

      Node $$23 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z - 1);
      if (this.isOpen($$23)
         && this.hasMalus($$19)
         && this.hasMalus($$6)
         && this.hasMalus($$4)
         && this.hasMalus($$7)
         && this.hasMalus($$12)
         && this.hasMalus($$10)) {
         pOutputArray[$$2++] = $$23;
      }

      Node $$24 = this.findAcceptedNode(pNode.x - 1, pNode.y + 1, pNode.z + 1);
      if (this.isOpen($$24)
         && this.hasMalus($$20)
         && this.hasMalus($$3)
         && this.hasMalus($$4)
         && this.hasMalus($$7)
         && this.hasMalus($$9)
         && this.hasMalus($$10)) {
         pOutputArray[$$2++] = $$24;
      }

      Node $$25 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z - 1);
      if (this.isOpen($$25)
         && this.hasMalus($$17)
         && this.hasMalus($$6)
         && this.hasMalus($$5)
         && this.hasMalus($$8)
         && this.hasMalus($$16)
         && this.hasMalus($$15)) {
         pOutputArray[$$2++] = $$25;
      }

      Node $$26 = this.findAcceptedNode(pNode.x + 1, pNode.y - 1, pNode.z + 1);
      if (this.isOpen($$26)
         && this.hasMalus($$18)
         && this.hasMalus($$3)
         && this.hasMalus($$5)
         && this.hasMalus($$8)
         && this.hasMalus($$13)
         && this.hasMalus($$15)) {
         pOutputArray[$$2++] = $$26;
      }

      Node $$27 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z - 1);
      if (this.isOpen($$27)
         && this.hasMalus($$19)
         && this.hasMalus($$6)
         && this.hasMalus($$4)
         && this.hasMalus($$8)
         && this.hasMalus($$16)
         && this.hasMalus($$14)) {
         pOutputArray[$$2++] = $$27;
      }

      Node $$28 = this.findAcceptedNode(pNode.x - 1, pNode.y - 1, pNode.z + 1);
      if (this.isOpen($$28)
         && this.hasMalus($$20)
         && this.hasMalus($$3)
         && this.hasMalus($$4)
         && this.hasMalus($$8)
         && this.hasMalus($$13)
         && this.hasMalus($$14)) {
         pOutputArray[$$2++] = $$28;
      }

      return $$2;
   }

   private boolean hasMalus(@Nullable Node pNode) {
      return pNode != null && pNode.costMalus >= 0.0F;
   }

   private boolean isOpen(@Nullable Node pNode) {
      return pNode != null && !pNode.closed;
   }

   @Nullable
   protected Node findAcceptedNode(int pX, int pY, int pZ) {
      Node $$3 = null;
      PathType $$4 = this.getCachedPathType(pX, pY, pZ);
      float $$5 = this.mob.getPathfindingMalus($$4);
      if ($$5 >= 0.0F) {
         $$3 = this.getNode(pX, pY, pZ);
         $$3.type = $$4;
         $$3.costMalus = Math.max($$3.costMalus, $$5);
         if ($$4 == PathType.WALKABLE) {
            $$3.costMalus++;
         }
      }

      return $$3;
   }

   @Override
   protected PathType getCachedPathType(int pX, int pY, int pZ) {
      return (PathType)this.pathTypeByPosCache
         .computeIfAbsent(BlockPos.asLong(pX, pY, pZ), p_327510_ -> this.getPathTypeOfMob(this.currentContext, pX, pY, pZ, this.mob));
   }

   @Override
   public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
      PathType $$4 = pContext.getPathTypeFromState(pX, pY, pZ);
      if ($$4 == PathType.OPEN && pY >= pContext.level().getMinBuildHeight() + 1) {
         BlockPos $$5 = new BlockPos(pX, pY - 1, pZ);
         PathType $$6 = pContext.getPathTypeFromState($$5.getX(), $$5.getY(), $$5.getZ());
         if ($$6 == PathType.DAMAGE_FIRE || $$6 == PathType.LAVA) {
            $$4 = PathType.DAMAGE_FIRE;
         } else if ($$6 == PathType.DAMAGE_OTHER) {
            $$4 = PathType.DAMAGE_OTHER;
         } else if ($$6 == PathType.COCOA) {
            $$4 = PathType.COCOA;
         } else if ($$6 == PathType.FENCE) {
            if (!$$5.equals(pContext.mobPosition())) {
               $$4 = PathType.FENCE;
            }
         } else {
            $$4 = $$6 != PathType.WALKABLE && $$6 != PathType.OPEN && $$6 != PathType.WATER ? PathType.WALKABLE : PathType.OPEN;
         }
      }

      if ($$4 == PathType.WALKABLE || $$4 == PathType.OPEN) {
         $$4 = checkNeighbourBlocks(pContext, pX, pY, pZ, $$4);
      }

      return $$4;
   }

   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob pMob) {
      AABB $$1 = pMob.getBoundingBox();
      boolean $$2 = $$1.getSize() < 1.0;
      if (!$$2) {
         return List.of(
            BlockPos.containing($$1.minX, (double)pMob.getBlockY(), $$1.minZ),
            BlockPos.containing($$1.minX, (double)pMob.getBlockY(), $$1.maxZ),
            BlockPos.containing($$1.maxX, (double)pMob.getBlockY(), $$1.minZ),
            BlockPos.containing($$1.maxX, (double)pMob.getBlockY(), $$1.maxZ)
         );
      } else {
         double $$3 = Math.max(0.0, 1.1F - $$1.getZsize());
         double $$4 = Math.max(0.0, 1.1F - $$1.getXsize());
         double $$5 = Math.max(0.0, 1.1F - $$1.getYsize());
         AABB $$6 = $$1.inflate($$4, $$5, $$3);
         return BlockPos.randomBetweenClosed(
            pMob.getRandom(), 10, Mth.floor($$6.minX), Mth.floor($$6.minY), Mth.floor($$6.minZ), Mth.floor($$6.maxX), Mth.floor($$6.maxY), Mth.floor($$6.maxZ)
         );
      }
   }
}
