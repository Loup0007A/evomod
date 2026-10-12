package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ListTag extends CollectionTag<Tag> {
   private static final int SELF_SIZE_IN_BYTES = 37;
   public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
      public ListTag load(DataInput p_128792_, NbtAccounter p_128794_) throws IOException {
         p_128794_.pushDepth();

         ListTag var3;
         try {
            var3 = loadList(p_128792_, p_128794_);
         } finally {
            p_128794_.popDepth();
         }

         return var3;
      }

      private static ListTag loadList(DataInput p_301758_, NbtAccounter p_301694_) throws IOException {
         p_301694_.accountBytes(37L);
         byte $$2 = p_301758_.readByte();
         int $$3 = p_301758_.readInt();
         if ($$2 == 0 && $$3 > 0) {
            throw new NbtFormatException("Missing type on ListTag");
         } else {
            p_301694_.accountBytes(4L, (long)$$3);
            TagType<?> $$4 = TagTypes.getType($$2);
            List<Tag> $$5 = Lists.newArrayListWithCapacity($$3);

            for (int $$6 = 0; $$6 < $$3; $$6++) {
               $$5.add($$4.load(p_301758_, p_301694_));
            }

            return new ListTag($$5, $$2);
         }
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197491_, StreamTagVisitor p_197492_, NbtAccounter p_301731_) throws IOException {
         p_301731_.pushDepth();

         StreamTagVisitor.ValueResult var4;
         try {
            var4 = parseList(p_197491_, p_197492_, p_301731_);
         } finally {
            p_301731_.popDepth();
         }

         return var4;
      }

      private static StreamTagVisitor.ValueResult parseList(DataInput p_301745_, StreamTagVisitor p_301695_, NbtAccounter p_301734_) throws IOException {
         p_301734_.accountBytes(37L);
         TagType<?> $$3 = TagTypes.getType(p_301745_.readByte());
         int $$4 = p_301745_.readInt();
         switch (p_301695_.visitList($$3, $$4)) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               $$3.skip(p_301745_, $$4, p_301734_);
               return p_301695_.visitContainerEnd();
            default:
               p_301734_.accountBytes(4L, (long)$$4);
               int $$5 = 0;

               while (true) {
                  label41: {
                     if ($$5 < $$4) {
                        switch (p_301695_.visitElement($$3, $$5)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                              $$3.skip(p_301745_, p_301734_);
                              break;
                           case SKIP:
                              $$3.skip(p_301745_, p_301734_);
                              break label41;
                           default:
                              switch ($$3.parse(p_301745_, p_301695_, p_301734_)) {
                                 case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label41;
                              }
                        }
                     }

                     int $$6 = $$4 - 1 - $$5;
                     if ($$6 > 0) {
                        $$3.skip(p_301745_, $$6, p_301734_);
                     }

                     return p_301695_.visitContainerEnd();
                  }

                  $$5++;
               }
         }
      }

      @Override
      public void skip(DataInput p_301743_, NbtAccounter p_301728_) throws IOException {
         p_301728_.pushDepth();

         try {
            TagType<?> $$2 = TagTypes.getType(p_301743_.readByte());
            int $$3 = p_301743_.readInt();
            $$2.skip(p_301743_, $$3, p_301728_);
         } finally {
            p_301728_.popDepth();
         }
      }

      @Override
      public String getName() {
         return "LIST";
      }

      @Override
      public String getPrettyName() {
         return "TAG_List";
      }
   };
   private final List<Tag> list;
   private byte type;

   ListTag(List<Tag> pList, byte pType) {
      this.list = pList;
      this.type = pType;
   }

   public ListTag() {
      this(Lists.newArrayList(), (byte)0);
   }

   @Override
   public void write(DataOutput pOutput) throws IOException {
      if (this.list.isEmpty()) {
         this.type = 0;
      } else {
         this.type = this.list.get(0).getId();
      }

      pOutput.writeByte(this.type);
      pOutput.writeInt(this.list.size());

      for (Tag $$1 : this.list) {
         $$1.write(pOutput);
      }
   }

   @Override
   public int sizeInBytes() {
      int $$0 = 37;
      $$0 += 4 * this.list.size();

      for (Tag $$1 : this.list) {
         $$0 += $$1.sizeInBytes();
      }

      return $$0;
   }

   @Override
   public byte getId() {
      return 9;
   }

   @Override
   public TagType<ListTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   private void updateTypeAfterRemove() {
      if (this.list.isEmpty()) {
         this.type = 0;
      }
   }

   @Override
   public Tag remove(int p_128751_) {
      Tag $$1 = this.list.remove(p_128751_);
      this.updateTypeAfterRemove();
      return $$1;
   }

   @Override
   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public CompoundTag getCompound(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 10) {
            return (CompoundTag)$$1;
         }
      }

      return new CompoundTag();
   }

   public ListTag getList(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 9) {
            return (ListTag)$$1;
         }
      }

      return new ListTag();
   }

   public short getShort(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 2) {
            return ((ShortTag)$$1).getAsShort();
         }
      }

      return 0;
   }

   public int getInt(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 3) {
            return ((IntTag)$$1).getAsInt();
         }
      }

      return 0;
   }

   public int[] getIntArray(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 11) {
            return ((IntArrayTag)$$1).getAsIntArray();
         }
      }

      return new int[0];
   }

   public long[] getLongArray(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 12) {
            return ((LongArrayTag)$$1).getAsLongArray();
         }
      }

      return new long[0];
   }

   public double getDouble(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 6) {
            return ((DoubleTag)$$1).getAsDouble();
         }
      }

      return 0.0;
   }

   public float getFloat(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         if ($$1.getId() == 5) {
            return ((FloatTag)$$1).getAsFloat();
         }
      }

      return 0.0F;
   }

   public String getString(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag $$1 = this.list.get(pIndex);
         return $$1.getId() == 8 ? $$1.getAsString() : $$1.toString();
      } else {
         return "";
      }
   }

   @Override
   public int size() {
      return this.list.size();
   }

   public Tag get(int pIndex) {
      return this.list.get(pIndex);
   }

   @Override
   public Tag set(int p_128760_, Tag p_128761_) {
      Tag $$2 = this.get(p_128760_);
      if (!this.setTag(p_128760_, p_128761_)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", p_128761_.getId(), this.type));
      } else {
         return $$2;
      }
   }

   @Override
   public void add(int p_128753_, Tag p_128754_) {
      if (!this.addTag(p_128753_, p_128754_)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", p_128754_.getId(), this.type));
      }
   }

   @Override
   public boolean setTag(int pIndex, Tag pNbt) {
      if (this.updateType(pNbt)) {
         this.list.set(pIndex, pNbt);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int pIndex, Tag pNbt) {
      if (this.updateType(pNbt)) {
         this.list.add(pIndex, pNbt);
         return true;
      } else {
         return false;
      }
   }

   private boolean updateType(Tag pTag) {
      if (pTag.getId() == 0) {
         return false;
      } else if (this.type == 0) {
         this.type = pTag.getId();
         return true;
      } else {
         return this.type == pTag.getId();
      }
   }

   public ListTag copy() {
      Iterable<Tag> $$0 = (Iterable<Tag>)(TagTypes.getType(this.type).isValue() ? this.list : Iterables.transform(this.list, Tag::copy));
      List<Tag> $$1 = Lists.newArrayList($$0);
      return new ListTag($$1, this.type);
   }

   @Override
   public boolean equals(Object pOther) {
      return this == pOther ? true : pOther instanceof ListTag && Objects.equals(this.list, ((ListTag)pOther).list);
   }

   @Override
   public int hashCode() {
      return this.list.hashCode();
   }

   @Override
   public void accept(TagVisitor pVisitor) {
      pVisitor.visitList(this);
   }

   @Override
   public byte getElementType() {
      return this.type;
   }

   @Override
   public void clear() {
      this.list.clear();
      this.type = 0;
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
      switch (pVisitor.visitList(TagTypes.getType(this.type), this.list.size())) {
         case HALT:
            return StreamTagVisitor.ValueResult.HALT;
         case BREAK:
            return pVisitor.visitContainerEnd();
         default:
            int $$1 = 0;

            while ($$1 < this.list.size()) {
               Tag $$2 = this.list.get($$1);
               switch (pVisitor.visitElement($$2.getType(), $$1)) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return pVisitor.visitContainerEnd();
                  default:
                     switch ($$2.accept(pVisitor)) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return pVisitor.visitContainerEnd();
                     }
                  case SKIP:
                     $$1++;
               }
            }

            return pVisitor.visitContainerEnd();
      }
   }
}
