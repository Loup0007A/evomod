package net.minecraft.nbt.visitors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class CollectFields extends CollectToTag {
   private int fieldsToGetCount;
   private final Set<TagType<?>> wantedTypes;
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public CollectFields(FieldSelector... pSelectors) {
      this.fieldsToGetCount = pSelectors.length;
      Builder<TagType<?>> $$1 = ImmutableSet.builder();
      FieldTree $$2 = FieldTree.createRoot();

      for (FieldSelector $$3 : pSelectors) {
         $$2.addEntry($$3);
         $$1.add($$3.type());
      }

      this.stack.push($$2);
      $$1.add(CompoundTag.TYPE);
      this.wantedTypes = $$1.build();
   }

   @Override
   public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> pType) {
      return pType != CompoundTag.TYPE ? StreamTagVisitor.ValueResult.HALT : super.visitRootEntry(pType);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> pType) {
      FieldTree $$1 = this.stack.element();
      if (this.depth() > $$1.depth()) {
         return super.visitEntry(pType);
      } else if (this.fieldsToGetCount <= 0) {
         return StreamTagVisitor.EntryResult.HALT;
      } else {
         return !this.wantedTypes.contains(pType) ? StreamTagVisitor.EntryResult.SKIP : super.visitEntry(pType);
      }
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> pType, String pId) {
      FieldTree $$2 = this.stack.element();
      if (this.depth() > $$2.depth()) {
         return super.visitEntry(pType, pId);
      } else if ($$2.selectedFields().remove(pId, pType)) {
         this.fieldsToGetCount--;
         return super.visitEntry(pType, pId);
      } else {
         if (pType == CompoundTag.TYPE) {
            FieldTree $$3 = $$2.fieldsToRecurse().get(pId);
            if ($$3 != null) {
               this.stack.push($$3);
               return super.visitEntry(pType, pId);
            }
         }

         return StreamTagVisitor.EntryResult.SKIP;
      }
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }

   public int getMissingFieldCount() {
      return this.fieldsToGetCount;
   }
}
