package net.minecraft.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

public class TreeNodePosition {
   private final AdvancementNode node;
   @Nullable
   private final TreeNodePosition parent;
   @Nullable
   private final TreeNodePosition previousSibling;
   private final int childIndex;
   private final List<TreeNodePosition> children = Lists.newArrayList();
   private TreeNodePosition ancestor;
   @Nullable
   private TreeNodePosition thread;
   private int x;
   private float y;
   private float mod;
   private float change;
   private float shift;

   public TreeNodePosition(AdvancementNode pNode, @Nullable TreeNodePosition pParent, @Nullable TreeNodePosition pPreviousSibling, int pChildIndex, int pX) {
      if (pNode.advancement().display().isEmpty()) {
         throw new IllegalArgumentException("Can't position an invisible advancement!");
      } else {
         this.node = pNode;
         this.parent = pParent;
         this.previousSibling = pPreviousSibling;
         this.childIndex = pChildIndex;
         this.ancestor = this;
         this.x = pX;
         this.y = -1.0F;
         TreeNodePosition $$5 = null;

         for (AdvancementNode $$6 : pNode.children()) {
            $$5 = this.addChild($$6, $$5);
         }
      }
   }

   @Nullable
   private TreeNodePosition addChild(AdvancementNode pChild, @Nullable TreeNodePosition pPreviousSibling) {
      if (pChild.advancement().display().isPresent()) {
         pPreviousSibling = new TreeNodePosition(pChild, this, pPreviousSibling, this.children.size() + 1, this.x + 1);
         this.children.add(pPreviousSibling);
      } else {
         for (AdvancementNode $$2 : pChild.children()) {
            pPreviousSibling = this.addChild($$2, pPreviousSibling);
         }
      }

      return pPreviousSibling;
   }

   private void firstWalk() {
      if (this.children.isEmpty()) {
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
         } else {
            this.y = 0.0F;
         }
      } else {
         TreeNodePosition $$0 = null;

         for (TreeNodePosition $$1 : this.children) {
            $$1.firstWalk();
            $$0 = $$1.apportion($$0 == null ? $$1 : $$0);
         }

         this.executeShifts();
         float $$2 = (this.children.get(0).y + this.children.get(this.children.size() - 1).y) / 2.0F;
         if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0F;
            this.mod = this.y - $$2;
         } else {
            this.y = $$2;
         }
      }
   }

   private float secondWalk(float pOffsetY, int pColumnX, float pSubtreeTopY) {
      this.y += pOffsetY;
      this.x = pColumnX;
      if (this.y < pSubtreeTopY) {
         pSubtreeTopY = this.y;
      }

      for (TreeNodePosition $$3 : this.children) {
         pSubtreeTopY = $$3.secondWalk(pOffsetY + this.mod, pColumnX + 1, pSubtreeTopY);
      }

      return pSubtreeTopY;
   }

   private void thirdWalk(float pY) {
      this.y += pY;

      for (TreeNodePosition $$1 : this.children) {
         $$1.thirdWalk(pY);
      }
   }

   private void executeShifts() {
      float $$0 = 0.0F;
      float $$1 = 0.0F;

      for (int $$2 = this.children.size() - 1; $$2 >= 0; $$2--) {
         TreeNodePosition $$3 = this.children.get($$2);
         $$3.y += $$0;
         $$3.mod += $$0;
         $$1 += $$3.change;
         $$0 += $$3.shift + $$1;
      }
   }

   @Nullable
   private TreeNodePosition previousOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(0) : null;
      }
   }

   @Nullable
   private TreeNodePosition nextOrThread() {
      if (this.thread != null) {
         return this.thread;
      } else {
         return !this.children.isEmpty() ? this.children.get(this.children.size() - 1) : null;
      }
   }

   private TreeNodePosition apportion(TreeNodePosition pNode) {
      if (this.previousSibling == null) {
         return pNode;
      } else {
         TreeNodePosition $$1 = this;
         TreeNodePosition $$2 = this;
         TreeNodePosition $$3 = this.previousSibling;
         TreeNodePosition $$4 = this.parent.children.get(0);
         float $$5 = this.mod;
         float $$6 = this.mod;
         float $$7 = $$3.mod;

         float $$8;
         for ($$8 = $$4.mod; $$3.nextOrThread() != null && $$1.previousOrThread() != null; $$6 += $$2.mod) {
            $$3 = $$3.nextOrThread();
            $$1 = $$1.previousOrThread();
            $$4 = $$4.previousOrThread();
            $$2 = $$2.nextOrThread();
            $$2.ancestor = this;
            float $$9 = $$3.y + $$7 - ($$1.y + $$5) + 1.0F;
            if ($$9 > 0.0F) {
               $$3.getAncestor(this, pNode).moveSubtree(this, $$9);
               $$5 += $$9;
               $$6 += $$9;
            }

            $$7 += $$3.mod;
            $$5 += $$1.mod;
            $$8 += $$4.mod;
         }

         if ($$3.nextOrThread() != null && $$2.nextOrThread() == null) {
            $$2.thread = $$3.nextOrThread();
            $$2.mod += $$7 - $$6;
         } else {
            if ($$1.previousOrThread() != null && $$4.previousOrThread() == null) {
               $$4.thread = $$1.previousOrThread();
               $$4.mod += $$5 - $$8;
            }

            pNode = this;
         }

         return pNode;
      }
   }

   private void moveSubtree(TreeNodePosition pNode, float pShift) {
      float $$2 = (float)(pNode.childIndex - this.childIndex);
      if ($$2 != 0.0F) {
         pNode.change -= pShift / $$2;
         this.change += pShift / $$2;
      }

      pNode.shift += pShift;
      pNode.y += pShift;
      pNode.mod += pShift;
   }

   private TreeNodePosition getAncestor(TreeNodePosition pSelf, TreeNodePosition pOther) {
      return this.ancestor != null && pSelf.parent.children.contains(this.ancestor) ? this.ancestor : pOther;
   }

   private void finalizePosition() {
      this.node.advancement().display().ifPresent(p_298323_ -> p_298323_.setLocation((float)this.x, this.y));
      if (!this.children.isEmpty()) {
         for (TreeNodePosition $$0 : this.children) {
            $$0.finalizePosition();
         }
      }
   }

   public static void run(AdvancementNode pRootNode) {
      if (pRootNode.advancement().display().isEmpty()) {
         throw new IllegalArgumentException("Can't position children of an invisible root!");
      } else {
         TreeNodePosition $$1 = new TreeNodePosition(pRootNode, null, null, 1, 0);
         $$1.firstWalk();
         float $$2 = $$1.secondWalk(0.0F, 0, $$1.y);
         if ($$2 < 0.0F) {
            $$1.thirdWalk(-$$2);
         }

         $$1.finalizePosition();
      }
   }
}
