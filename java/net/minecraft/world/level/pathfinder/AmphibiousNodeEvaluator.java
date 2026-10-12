package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
   private final boolean prefersShallowSwimming;
   private float oldWalkableCost;
   private float oldWaterBorderCost;

   public AmphibiousNodeEvaluator(boolean pPrefersShallowSwimming) {
      this.prefersShallowSwimming = pPrefersShallowSwimming;
   }

   @Override
   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      pMob.setPathfindingMalus(PathType.WATER, 0.0F);
      this.oldWalkableCost = pMob.getPathfindingMalus(PathType.WALKABLE);
      pMob.setPathfindingMalus(PathType.WALKABLE, 6.0F);
      this.oldWaterBorderCost = pMob.getPathfindingMalus(PathType.WATER_BORDER);
      pMob.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
   }

   @Override
   public void done() {
      this.mob.setPathfindingMalus(PathType.WALKABLE, this.oldWalkableCost);
      this.mob.setPathfindingMalus(PathType.WATER_BORDER, this.oldWaterBorderCost);
      super.done();
   }

   @Override
   public Node getStart() {
      return !this.mob.isInWater()
         ? super.getStart()
         : this.getStartNode(
            new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ))
         );
   }

   @Override
   public Target getTarget(double pX, double pY, double pZ) {
      return this.getTargetNodeAt(pX, pY + 0.5, pZ);
   }

   @Override
   public int getNeighbors(Node[] pOutputArray, Node pNode) {
      int $$2 = super.getNeighbors(pOutputArray, pNode);
      PathType $$3 = this.getCachedPathType(pNode.x, pNode.y + 1, pNode.z);
      PathType $$4 = this.getCachedPathType(pNode.x, pNode.y, pNode.z);
      int $$5;
      if (this.mob.getPathfindingMalus($$3) >= 0.0F && $$4 != PathType.STICKY_HONEY) {
         $$5 = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
      } else {
         $$5 = 0;
      }

      double $$7 = this.getFloorLevel(new BlockPos(pNode.x, pNode.y, pNode.z));
      Node $$8 = this.findAcceptedNode(pNode.x, pNode.y + 1, pNode.z, Math.max(0, $$5 - 1), $$7, Direction.UP, $$4);
      Node $$9 = this.findAcceptedNode(pNode.x, pNode.y - 1, pNode.z, $$5, $$7, Direction.DOWN, $$4);
      if (this.isVerticalNeighborValid($$8, pNode)) {
         pOutputArray[$$2++] = $$8;
      }

      if (this.isVerticalNeighborValid($$9, pNode) && $$4 != PathType.TRAPDOOR) {
         pOutputArray[$$2++] = $$9;
      }

      for (int $$10 = 0; $$10 < $$2; $$10++) {
         Node $$11 = pOutputArray[$$10];
         if ($$11.type == PathType.WATER && this.prefersShallowSwimming && $$11.y < this.mob.level().getSeaLevel() - 10) {
            $$11.costMalus++;
         }
      }

      return $$2;
   }

   private boolean isVerticalNeighborValid(@Nullable Node pNeighbor, Node pNode) {
      return this.isNeighborValid(pNeighbor, pNode) && pNeighbor.type == PathType.WATER;
   }

   @Override
   protected boolean isAmphibious() {
      return true;
   }

   @Override
   public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
      PathType $$4 = pContext.getPathTypeFromState(pX, pY, pZ);
      if ($$4 == PathType.WATER) {
         BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

         for (Direction $$6 : Direction.values()) {
            $$5.set(pX, pY, pZ).move($$6);
            PathType $$7 = pContext.getPathTypeFromState($$5.getX(), $$5.getY(), $$5.getZ());
            if ($$7 == PathType.BLOCKED) {
               return PathType.WATER_BORDER;
            }
         }

         return PathType.WATER;
      } else {
         return super.getPathType(pContext, pX, pY, pZ);
      }
   }
}
