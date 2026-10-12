package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class StringTag implements Tag {
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
      public StringTag load(DataInput p_129315_, NbtAccounter p_129317_) throws IOException {
         return StringTag.valueOf(readAccounted(p_129315_, p_129317_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197570_, StreamTagVisitor p_197571_, NbtAccounter p_301725_) throws IOException {
         return p_197571_.visit(readAccounted(p_197570_, p_301725_));
      }

      private static String readAccounted(DataInput p_301750_, NbtAccounter p_301732_) throws IOException {
         p_301732_.accountBytes(36L);
         String $$2 = p_301750_.readUTF();
         p_301732_.accountBytes(2L, (long)$$2.length());
         return $$2;
      }

      @Override
      public void skip(DataInput p_197568_, NbtAccounter p_301752_) throws IOException {
         StringTag.skipString(p_197568_);
      }

      @Override
      public String getName() {
         return "STRING";
      }

      @Override
      public String getPrettyName() {
         return "TAG_String";
      }

      @Override
      public boolean isValue() {
         return true;
      }
   };
   private static final StringTag EMPTY = new StringTag("");
   private static final char DOUBLE_QUOTE = '"';
   private static final char SINGLE_QUOTE = '\'';
   private static final char ESCAPE = '\\';
   private static final char NOT_SET = '\u0000';
   private final String data;

   public static void skipString(DataInput pInput) throws IOException {
      pInput.skipBytes(pInput.readUnsignedShort());
   }

   private StringTag(String pData) {
      Objects.requireNonNull(pData, "Null string not allowed");
      this.data = pData;
   }

   public static StringTag valueOf(String pData) {
      return pData.isEmpty() ? EMPTY : new StringTag(pData);
   }

   @Override
   public void write(DataOutput pOutput) throws IOException {
      pOutput.writeUTF(this.data);
   }

   @Override
   public int sizeInBytes() {
      return 36 + 2 * this.data.length();
   }

   @Override
   public byte getId() {
      return 8;
   }

   @Override
   public TagType<StringTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      return Tag.super.getAsString();
   }

   public StringTag copy() {
      return this;
   }

   @Override
   public boolean equals(Object pOther) {
      return this == pOther ? true : pOther instanceof StringTag && Objects.equals(this.data, ((StringTag)pOther).data);
   }

   @Override
   public int hashCode() {
      return this.data.hashCode();
   }

   @Override
   public String getAsString() {
      return this.data;
   }

   @Override
   public void accept(TagVisitor pVisitor) {
      pVisitor.visitString(this);
   }

   public static String quoteAndEscape(String pText) {
      StringBuilder $$1 = new StringBuilder(" ");
      char $$2 = 0;

      for (int $$3 = 0; $$3 < pText.length(); $$3++) {
         char $$4 = pText.charAt($$3);
         if ($$4 == '\\') {
            $$1.append('\\');
         } else if ($$4 == '"' || $$4 == '\'') {
            if ($$2 == 0) {
               $$2 = (char)($$4 == '"' ? 39 : 34);
            }

            if ($$2 == $$4) {
               $$1.append('\\');
            }
         }

         $$1.append($$4);
      }

      if ($$2 == 0) {
         $$2 = '"';
      }

      $$1.setCharAt(0, $$2);
      $$1.append($$2);
      return $$1.toString();
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
      return pVisitor.visit(this.data);
   }
}
