package net.minecraft.advancements;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import javax.annotation.Nullable;

public class AdvancementNode {
   private final AdvancementHolder holder;
   @Nullable
   private final AdvancementNode parent;
   private final Set<AdvancementNode> children = new ReferenceOpenHashSet();

   @VisibleForTesting
   public AdvancementNode(AdvancementHolder pHolder, @Nullable AdvancementNode pParent) {
      this.holder = pHolder;
      this.parent = pParent;
   }

   public Advancement advancement() {
      return this.holder.value();
   }

   public AdvancementHolder holder() {
      return this.holder;
   }

   @Nullable
   public AdvancementNode parent() {
      return this.parent;
   }

   public AdvancementNode root() {
      return getRoot(this);
   }

   public static AdvancementNode getRoot(AdvancementNode pNode) {
      AdvancementNode $$1 = pNode;

      while (true) {
         AdvancementNode $$2 = $$1.parent();
         if ($$2 == null) {
            return $$1;
         }

         $$1 = $$2;
      }
   }

   public Iterable<AdvancementNode> children() {
      return this.children;
   }

   @VisibleForTesting
   public void addChild(AdvancementNode pChild) {
      this.children.add(pChild);
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof AdvancementNode $$1 && this.holder.equals($$1.holder)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.holder.hashCode();
   }

   @Override
   public String toString() {
      return this.holder.id().toString();
   }
}
