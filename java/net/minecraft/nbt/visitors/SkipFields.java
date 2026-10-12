package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class SkipFields extends CollectToTag {
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public SkipFields(FieldSelector... pSelectors) {
      FieldTree $$1 = FieldTree.createRoot();

      for (FieldSelector $$2 : pSelectors) {
         $$1.addEntry($$2);
      }

      this.stack.push($$1);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> pType, String pId) {
      FieldTree $$2 = this.stack.element();
      if ($$2.isSelected(pType, pId)) {
         return StreamTagVisitor.EntryResult.SKIP;
      } else {
         if (pType == CompoundTag.TYPE) {
            FieldTree $$3 = $$2.fieldsToRecurse().get(pId);
            if ($$3 != null) {
               this.stack.push($$3);
            }
         }

         return super.visitEntry(pType, pId);
      }
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }
}
