package net.minecraft.world.level.pathfinder;

import java.util.Arrays;

public class BinaryHeap {
   private Node[] heap = new Node[128];
   private int size;

   public Node insert(Node pPoint) {
      if (pPoint.heapIdx >= 0) {
         throw new IllegalStateException("OW KNOWS!");
      } else {
         if (this.size == this.heap.length) {
            Node[] $$1 = new Node[this.size << 1];
            System.arraycopy(this.heap, 0, $$1, 0, this.size);
            this.heap = $$1;
         }

         this.heap[this.size] = pPoint;
         pPoint.heapIdx = this.size;
         this.upHeap(this.size++);
         return pPoint;
      }
   }

   public void clear() {
      this.size = 0;
   }

   public Node peek() {
      return this.heap[0];
   }

   public Node pop() {
      Node $$0 = this.heap[0];
      this.heap[0] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > 0) {
         this.downHeap(0);
      }

      $$0.heapIdx = -1;
      return $$0;
   }

   public void remove(Node pNode) {
      this.heap[pNode.heapIdx] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > pNode.heapIdx) {
         if (this.heap[pNode.heapIdx].f < pNode.f) {
            this.upHeap(pNode.heapIdx);
         } else {
            this.downHeap(pNode.heapIdx);
         }
      }

      pNode.heapIdx = -1;
   }

   public void changeCost(Node pPoint, float pCost) {
      float $$2 = pPoint.f;
      pPoint.f = pCost;
      if (pCost < $$2) {
         this.upHeap(pPoint.heapIdx);
      } else {
         this.downHeap(pPoint.heapIdx);
      }
   }

   public int size() {
      return this.size;
   }

   private void upHeap(int pIndex) {
      Node $$1 = this.heap[pIndex];
      float $$2 = $$1.f;

      while (pIndex > 0) {
         int $$3 = pIndex - 1 >> 1;
         Node $$4 = this.heap[$$3];
         if (!($$2 < $$4.f)) {
            break;
         }

         this.heap[pIndex] = $$4;
         $$4.heapIdx = pIndex;
         pIndex = $$3;
      }

      this.heap[pIndex] = $$1;
      $$1.heapIdx = pIndex;
   }

   private void downHeap(int pIndex) {
      Node $$1 = this.heap[pIndex];
      float $$2 = $$1.f;

      while (true) {
         int $$3 = 1 + (pIndex << 1);
         int $$4 = $$3 + 1;
         if ($$3 >= this.size) {
            break;
         }

         Node $$5 = this.heap[$$3];
         float $$6 = $$5.f;
         Node $$7;
         float $$8;
         if ($$4 >= this.size) {
            $$7 = null;
            $$8 = Float.POSITIVE_INFINITY;
         } else {
            $$7 = this.heap[$$4];
            $$8 = $$7.f;
         }

         if ($$6 < $$8) {
            if (!($$6 < $$2)) {
               break;
            }

            this.heap[pIndex] = $$5;
            $$5.heapIdx = pIndex;
            pIndex = $$3;
         } else {
            if (!($$8 < $$2)) {
               break;
            }

            this.heap[pIndex] = $$7;
            $$7.heapIdx = pIndex;
            pIndex = $$4;
         }
      }

      this.heap[pIndex] = $$1;
      $$1.heapIdx = pIndex;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Node[] getHeap() {
      return Arrays.copyOf(this.heap, this.size);
   }
}
