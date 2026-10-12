package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
   private final boolean allowBreaching;
   private final Long2ObjectMap<PathType> pathTypesByPosCache = new Long2ObjectOpenHashMap();

   public SwimNodeEvaluator(boolean pAllowBreaching) {
      this.allowBreaching = pAllowBreaching;
   }

   @Override
   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      this.pathTypesByPosCache.clear();
   }

   @Override
   public void done() {
      super.done();
      this.pathTypesByPosCache.clear();
   }

   @Override
   public Node getStart() {
      return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5), Mth.floor(this.mob.getBoundingBox().minZ));
   }

   @Override
   public Target getTarget(double pX, double pY, double pZ) {
      return this.getTargetNodeAt(pX, pY, pZ);
   }

   @Override
   public int getNeighbors(Node[] pOutputArray, Node pNode) {
      int $$2 = 0;
      Map<Direction, Node> $$3 = Maps.newEnumMap(Direction.class);

      for (Direction $$4 : Direction.values()) {
         Node $$5 = this.findAcceptedNode(pNode.x + $$4.getStepX(), pNode.y + $$4.getStepY(), pNode.z + $$4.getStepZ());
         $$3.put($$4, $$5);
         if (this.isNodeValid($$5)) {
            pOutputArray[$$2++] = $$5;
         }
      }

      for (Direction $$6 : Direction.Plane.HORIZONTAL) {
         Direction $$7 = $$6.getClockWise();
         if (hasMalus($$3.get($$6)) && hasMalus($$3.get($$7))) {
            Node $$8 = this.findAcceptedNode(pNode.x + $$6.getStepX() + $$7.getStepX(), pNode.y, pNode.z + $$6.getStepZ() + $$7.getStepZ());
            if (this.isNodeValid($$8)) {
               pOutputArray[$$2++] = $$8;
            }
         }
      }

      return $$2;
   }

   protected boolean isNodeValid(@Nullable Node pNode) {
      return pNode != null && !pNode.closed;
   }

   private static boolean hasMalus(@Nullable Node pNode) {
      return pNode != null && pNode.costMalus >= 0.0F;
   }

   @Nullable
   protected Node findAcceptedNode(int pX, int pY, int pZ) {
      Node $$3 = null;
      PathType $$4 = this.getCachedBlockType(pX, pY, pZ);
      if (this.allowBreaching && $$4 == PathType.BREACH || $$4 == PathType.WATER) {
         float $$5 = this.mob.getPathfindingMalus($$4);
         if ($$5 >= 0.0F) {
            $$3 = this.getNode(pX, pY, pZ);
            $$3.type = $$4;
            $$3.costMalus = Math.max($$3.costMalus, $$5);
            if (this.currentContext.level().getFluidState(new BlockPos(pX, pY, pZ)).isEmpty()) {
               $$3.costMalus += 8.0F;
            }
         }
      }

      return $$3;
   }

   protected PathType getCachedBlockType(int pX, int pY, int pZ) {
      return (PathType)this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(pX, pY, pZ), p_327515_ -> this.getPathType(this.currentContext, pX, pY, pZ));
   }

   @Override
   public PathType getPathType(PathfindingContext pContext, int pX, int pY, int pZ) {
      return this.getPathTypeOfMob(pContext, pX, pY, pZ, this.mob);
   }

   @Override
   public PathType getPathTypeOfMob(PathfindingContext pContext, int pX, int pY, int pZ, Mob pMob) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (int $$6 = pX; $$6 < pX + this.entityWidth; $$6++) {
         for (int $$7 = pY; $$7 < pY + this.entityHeight; $$7++) {
            for (int $$8 = pZ; $$8 < pZ + this.entityDepth; $$8++) {
               BlockState $$9 = pContext.getBlockState($$5.set($$6, $$7, $$8));
               FluidState $$10 = $$9.getFluidState();
               if ($$10.isEmpty() && $$9.isPathfindable(PathComputationType.WATER) && $$9.isAir()) {
                  return PathType.BREACH;
               }

               if (!$$10.is(FluidTags.WATER)) {
                  return PathType.BLOCKED;
               }
            }
         }
      }

      BlockState $$11 = pContext.getBlockState($$5);
      return $$11.isPathfindable(PathComputationType.WATER) ? PathType.WATER : PathType.BLOCKED;
   }
}
