package net.minecraft.world.level.pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Path {
   private final List<Node> nodes;
   @Nullable
   private Path.DebugData debugData;
   private int nextNodeIndex;
   private final BlockPos target;
   private final float distToTarget;
   private final boolean reached;

   public Path(List<Node> pNodes, BlockPos pTarget, boolean pReached) {
      this.nodes = pNodes;
      this.target = pTarget;
      this.distToTarget = pNodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
      this.reached = pReached;
   }

   public void advance() {
      this.nextNodeIndex++;
   }

   public boolean notStarted() {
      return this.nextNodeIndex <= 0;
   }

   public boolean isDone() {
      return this.nextNodeIndex >= this.nodes.size();
   }

   @Nullable
   public Node getEndNode() {
      return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
   }

   public Node getNode(int pIndex) {
      return this.nodes.get(pIndex);
   }

   public void truncateNodes(int pLength) {
      if (this.nodes.size() > pLength) {
         this.nodes.subList(pLength, this.nodes.size()).clear();
      }
   }

   public void replaceNode(int pIndex, Node pPoint) {
      this.nodes.set(pIndex, pPoint);
   }

   public int getNodeCount() {
      return this.nodes.size();
   }

   public int getNextNodeIndex() {
      return this.nextNodeIndex;
   }

   public void setNextNodeIndex(int pCurrentPathIndex) {
      this.nextNodeIndex = pCurrentPathIndex;
   }

   public Vec3 getEntityPosAtNode(Entity pEntity, int pIndex) {
      Node $$2 = this.nodes.get(pIndex);
      double $$3 = (double)$$2.x + (double)((int)(pEntity.getBbWidth() + 1.0F)) * 0.5;
      double $$4 = (double)$$2.y;
      double $$5 = (double)$$2.z + (double)((int)(pEntity.getBbWidth() + 1.0F)) * 0.5;
      return new Vec3($$3, $$4, $$5);
   }

   public BlockPos getNodePos(int pIndex) {
      return this.nodes.get(pIndex).asBlockPos();
   }

   public Vec3 getNextEntityPos(Entity pEntity) {
      return this.getEntityPosAtNode(pEntity, this.nextNodeIndex);
   }

   public BlockPos getNextNodePos() {
      return this.nodes.get(this.nextNodeIndex).asBlockPos();
   }

   public Node getNextNode() {
      return this.nodes.get(this.nextNodeIndex);
   }

   @Nullable
   public Node getPreviousNode() {
      return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
   }

   public boolean sameAs(@Nullable Path pPathentity) {
      if (pPathentity == null) {
         return false;
      } else if (pPathentity.nodes.size() != this.nodes.size()) {
         return false;
      } else {
         for (int $$1 = 0; $$1 < this.nodes.size(); $$1++) {
            Node $$2 = this.nodes.get($$1);
            Node $$3 = pPathentity.nodes.get($$1);
            if ($$2.x != $$3.x || $$2.y != $$3.y || $$2.z != $$3.z) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean canReach() {
      return this.reached;
   }

   @VisibleForDebug
   void setDebug(Node[] pOpenSet, Node[] pClosedSet, Set<Target> pTargetNodes) {
      this.debugData = new Path.DebugData(pOpenSet, pClosedSet, pTargetNodes);
   }

   @Nullable
   public Path.DebugData debugData() {
      return this.debugData;
   }

   public void writeToStream(FriendlyByteBuf pBuffer) {
      if (this.debugData != null && !this.debugData.targetNodes.isEmpty()) {
         pBuffer.writeBoolean(this.reached);
         pBuffer.writeInt(this.nextNodeIndex);
         pBuffer.writeBlockPos(this.target);
         pBuffer.writeCollection(this.nodes, (p_296986_, p_296987_) -> p_296987_.writeToStream(p_296986_));
         this.debugData.write(pBuffer);
      }
   }

   public static Path createFromStream(FriendlyByteBuf pBuf) {
      boolean $$1 = pBuf.readBoolean();
      int $$2 = pBuf.readInt();
      BlockPos $$3 = pBuf.readBlockPos();
      List<Node> $$4 = pBuf.readList(Node::createFromStream);
      Path.DebugData $$5 = Path.DebugData.read(pBuf);
      Path $$6 = new Path($$4, $$3, $$1);
      $$6.debugData = $$5;
      $$6.nextNodeIndex = $$2;
      return $$6;
   }

   @Override
   public String toString() {
      return "Path(length=" + this.nodes.size() + ")";
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public float getDistToTarget() {
      return this.distToTarget;
   }

   static Node[] readNodeArray(FriendlyByteBuf pBuffer) {
      Node[] $$1 = new Node[pBuffer.readVarInt()];

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         $$1[$$2] = Node.createFromStream(pBuffer);
      }

      return $$1;
   }

   static void writeNodeArray(FriendlyByteBuf pBuffer, Node[] pNodeArray) {
      pBuffer.writeVarInt(pNodeArray.length);

      for (Node $$2 : pNodeArray) {
         $$2.writeToStream(pBuffer);
      }
   }

   public Path copy() {
      Path $$0 = new Path(this.nodes, this.target, this.reached);
      $$0.debugData = this.debugData;
      $$0.nextNodeIndex = this.nextNodeIndex;
      return $$0;
   }

   public static record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeCollection(this.targetNodes, (p_300614_, p_300157_) -> p_300157_.writeToStream(p_300614_));
         Path.writeNodeArray(pBuffer, this.openSet);
         Path.writeNodeArray(pBuffer, this.closedSet);
      }

      public static Path.DebugData read(FriendlyByteBuf pBuffer) {
         HashSet<Target> $$1 = pBuffer.readCollection(HashSet::new, Target::createFromStream);
         Node[] $$2 = Path.readNodeArray(pBuffer);
         Node[] $$3 = Path.readNodeArray(pBuffer);
         return new Path.DebugData($$2, $$3, $$1);
      }
   }
}
