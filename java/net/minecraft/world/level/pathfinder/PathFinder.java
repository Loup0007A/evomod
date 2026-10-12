package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
   private static final float FUDGING = 1.5F;
   private final Node[] neighbors = new Node[32];
   private final int maxVisitedNodes;
   private final NodeEvaluator nodeEvaluator;
   private static final boolean DEBUG = false;
   private final BinaryHeap openSet = new BinaryHeap();

   public PathFinder(NodeEvaluator pNodeEvaluator, int pMaxVisitedNodes) {
      this.nodeEvaluator = pNodeEvaluator;
      this.maxVisitedNodes = pMaxVisitedNodes;
   }

   @Nullable
   public Path findPath(PathNavigationRegion pRegion, Mob pMob, Set<BlockPos> pTargetPositions, float pMaxRange, int pAccuracy, float pSearchDepthMultiplier) {
      this.openSet.clear();
      this.nodeEvaluator.prepare(pRegion, pMob);
      Node $$6 = this.nodeEvaluator.getStart();
      if ($$6 == null) {
         return null;
      } else {
         Map<Target, BlockPos> $$7 = pTargetPositions.stream()
            .collect(
               Collectors.toMap(
                  p_327511_ -> this.nodeEvaluator.getTarget((double)p_327511_.getX(), (double)p_327511_.getY(), (double)p_327511_.getZ()), Function.identity()
               )
            );
         Path $$8 = this.findPath(pRegion.getProfiler(), $$6, $$7, pMaxRange, pAccuracy, pSearchDepthMultiplier);
         this.nodeEvaluator.done();
         return $$8;
      }
   }

   @Nullable
   private Path findPath(ProfilerFiller pProfiler, Node pNode, Map<Target, BlockPos> pTargetPos, float pMaxRange, int pAccuracy, float pSearchDepthMultiplier) {
      pProfiler.push("find_path");
      pProfiler.markForCharting(MetricCategory.PATH_FINDING);
      Set<Target> $$6 = pTargetPos.keySet();
      pNode.g = 0.0F;
      pNode.h = this.getBestH(pNode, $$6);
      pNode.f = pNode.h;
      this.openSet.clear();
      this.openSet.insert(pNode);
      Set<Node> $$7 = ImmutableSet.of();
      int $$8 = 0;
      Set<Target> $$9 = Sets.newHashSetWithExpectedSize($$6.size());
      int $$10 = (int)((float)this.maxVisitedNodes * pSearchDepthMultiplier);

      while (!this.openSet.isEmpty()) {
         if (++$$8 >= $$10) {
            break;
         }

         Node $$11 = this.openSet.pop();
         $$11.closed = true;

         for (Target $$12 : $$6) {
            if ($$11.distanceManhattan($$12) <= (float)pAccuracy) {
               $$12.setReached();
               $$9.add($$12);
            }
         }

         if (!$$9.isEmpty()) {
            break;
         }

         if (!($$11.distanceTo(pNode) >= pMaxRange)) {
            int $$13 = this.nodeEvaluator.getNeighbors(this.neighbors, $$11);

            for (int $$14 = 0; $$14 < $$13; $$14++) {
               Node $$15 = this.neighbors[$$14];
               float $$16 = this.distance($$11, $$15);
               $$15.walkedDistance = $$11.walkedDistance + $$16;
               float $$17 = $$11.g + $$16 + $$15.costMalus;
               if ($$15.walkedDistance < pMaxRange && (!$$15.inOpenSet() || $$17 < $$15.g)) {
                  $$15.cameFrom = $$11;
                  $$15.g = $$17;
                  $$15.h = this.getBestH($$15, $$6) * 1.5F;
                  if ($$15.inOpenSet()) {
                     this.openSet.changeCost($$15, $$15.g + $$15.h);
                  } else {
                     $$15.f = $$15.g + $$15.h;
                     this.openSet.insert($$15);
                  }
               }
            }
         }
      }

      Optional<Path> $$18 = !$$9.isEmpty()
         ? $$9.stream()
            .map(p_77454_ -> this.reconstructPath(p_77454_.getBestNode(), pTargetPos.get(p_77454_), true))
            .min(Comparator.comparingInt(Path::getNodeCount))
         : $$6.stream()
            .map(p_77451_ -> this.reconstructPath(p_77451_.getBestNode(), pTargetPos.get(p_77451_), false))
            .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
      pProfiler.pop();
      return $$18.isEmpty() ? null : $$18.get();
   }

   protected float distance(Node pFirst, Node pSecond) {
      return pFirst.distanceTo(pSecond);
   }

   private float getBestH(Node pNode, Set<Target> pTargets) {
      float $$2 = Float.MAX_VALUE;

      for (Target $$3 : pTargets) {
         float $$4 = pNode.distanceTo($$3);
         $$3.updateBest($$4, pNode);
         $$2 = Math.min($$4, $$2);
      }

      return $$2;
   }

   private Path reconstructPath(Node pPoint, BlockPos pTargetPos, boolean pReachesTarget) {
      List<Node> $$3 = Lists.newArrayList();
      Node $$4 = pPoint;
      $$3.add(0, pPoint);

      while ($$4.cameFrom != null) {
         $$4 = $$4.cameFrom;
         $$3.add(0, $$4);
      }

      return new Path($$3, pTargetPos, pReachesTarget);
   }
}
