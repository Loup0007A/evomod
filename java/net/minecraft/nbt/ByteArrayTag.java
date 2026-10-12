package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class ByteArrayTag extends CollectionTag<ByteTag> {
   private static final int SELF_SIZE_IN_BYTES = 24;
   public static final TagType<ByteArrayTag> TYPE = new TagType.VariableSize<ByteArrayTag>() {
      public ByteArrayTag load(DataInput p_128247_, NbtAccounter p_128249_) throws IOException {
         return new ByteArrayTag(readAccounted(p_128247_, p_128249_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197433_, StreamTagVisitor p_197434_, NbtAccounter p_301760_) throws IOException {
         return p_197434_.visit(readAccounted(p_197433_, p_301760_));
      }

      private static byte[] readAccounted(DataInput p_301772_, NbtAccounter p_301697_) throws IOException {
         p_301697_.accountBytes(24L);
         int $$2 = p_301772_.readInt();
         p_301697_.accountBytes(1L, (long)$$2);
         byte[] $$3 = new byte[$$2];
         p_301772_.readFully($$3);
         return $$3;
      }

      @Override
      public void skip(DataInput p_197431_, NbtAccounter p_301779_) throws IOException {
         p_197431_.skipBytes(p_197431_.readInt() * 1);
      }

      @Override
      public String getName() {
         return "BYTE[]";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Byte_Array";
      }
   };
   private byte[] data;

   public ByteArrayTag(byte[] pData) {
      this.data = pData;
   }

   public ByteArrayTag(List<Byte> pDataList) {
      this(toArray(pDataList));
   }

   private static byte[] toArray(List<Byte> pDataList) {
      byte[] $$1 = new byte[pDataList.size()];

      for (int $$2 = 0; $$2 < pDataList.size(); $$2++) {
         Byte $$3 = pDataList.get($$2);
         $$1[$$2] = $$3 == null ? 0 : $$3;
      }

      return $$1;
   }

   @Override
   public void write(DataOutput pOutput) throws IOException {
      pOutput.writeInt(this.data.length);
      pOutput.write(this.data);
   }

   @Override
   public int sizeInBytes() {
      return 24 + 1 * this.data.length;
   }

   @Override
   public byte getId() {
      return 7;
   }

   @Override
   public TagType<ByteArrayTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return this.getAsString();
   }

   @Override
   public Tag copy() {
      byte[] $$0 = new byte[this.data.length];
      System.arraycopy(this.data, 0, $$0, 0, this.data.length);
      return new ByteArrayTag($$0);
   }

   @Override
   public boolean equals(Object pOther) {
      return this == pOther ? true : pOther instanceof ByteArrayTag && Arrays.equals(this.data, ((ByteArrayTag)pOther).data);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }

   @Override
   public void accept(TagVisitor pVisitor) {
      pVisitor.visitByteArray(this);
   }

   public byte[] getAsByteArray() {
      return this.data;
   }

   @Override
   public int size() {
      return this.data.length;
   }

   public ByteTag get(int pIndex) {
      return ByteTag.valueOf(this.data[pIndex]);
   }

   public ByteTag set(int pIndex, ByteTag pTag) {
      byte $$2 = this.data[pIndex];
      this.data[pIndex] = pTag.getAsByte();
      return ByteTag.valueOf($$2);
   }

   public void add(int pIndex, ByteTag pTag) {
      this.data = ArrayUtils.add(this.data, pIndex, pTag.getAsByte());
   }

   @Override
   public boolean setTag(int pIndex, Tag pNbt) {
      if (pNbt instanceof NumericTag) {
         this.data[pIndex] = ((NumericTag)pNbt).getAsByte();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean addTag(int pIndex, Tag pNbt) {
      if (pNbt instanceof NumericTag) {
         this.data = ArrayUtils.add(this.data, pIndex, ((NumericTag)pNbt).getAsByte());
         return true;
      } else {
         return false;
      }
   }

   public ByteTag remove(int pIndex) {
      byte $$1 = this.data[pIndex];
      this.data = ArrayUtils.remove(this.data, pIndex);
      return ByteTag.valueOf($$1);
   }

   @Override
   public byte getElementType() {
      return 1;
   }

   @Override
   public void clear() {
      this.data = new byte[0];
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
      return pVisitor.visit(this.data);
   }
}
