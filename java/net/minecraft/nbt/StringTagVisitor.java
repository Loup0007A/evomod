package net.minecraft.nbt;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class StringTagVisitor implements TagVisitor {
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private final StringBuilder builder = new StringBuilder();

   public String visit(Tag pTag) {
      pTag.accept(this);
      return this.builder.toString();
   }

   @Override
   public void visitString(StringTag pTag) {
      this.builder.append(StringTag.quoteAndEscape(pTag.getAsString()));
   }

   @Override
   public void visitByte(ByteTag pTag) {
      this.builder.append(pTag.getAsNumber()).append('b');
   }

   @Override
   public void visitShort(ShortTag pTag) {
      this.builder.append(pTag.getAsNumber()).append('s');
   }

   @Override
   public void visitInt(IntTag pTag) {
      this.builder.append(pTag.getAsNumber());
   }

   @Override
   public void visitLong(LongTag pTag) {
      this.builder.append(pTag.getAsNumber()).append('L');
   }

   @Override
   public void visitFloat(FloatTag pTag) {
      this.builder.append(pTag.getAsFloat()).append('f');
   }

   @Override
   public void visitDouble(DoubleTag pTag) {
      this.builder.append(pTag.getAsDouble()).append('d');
   }

   @Override
   public void visitByteArray(ByteArrayTag pTag) {
      this.builder.append("[B;");
      byte[] $$1 = pTag.getAsByteArray();

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         if ($$2 != 0) {
            this.builder.append(',');
         }

         this.builder.append($$1[$$2]).append('B');
      }

      this.builder.append(']');
   }

   @Override
   public void visitIntArray(IntArrayTag pTag) {
      this.builder.append("[I;");
      int[] $$1 = pTag.getAsIntArray();

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         if ($$2 != 0) {
            this.builder.append(',');
         }

         this.builder.append($$1[$$2]);
      }

      this.builder.append(']');
   }

   @Override
   public void visitLongArray(LongArrayTag pTag) {
      this.builder.append("[L;");
      long[] $$1 = pTag.getAsLongArray();

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         if ($$2 != 0) {
            this.builder.append(',');
         }

         this.builder.append($$1[$$2]).append('L');
      }

      this.builder.append(']');
   }

   @Override
   public void visitList(ListTag pTag) {
      this.builder.append('[');

      for (int $$1 = 0; $$1 < pTag.size(); $$1++) {
         if ($$1 != 0) {
            this.builder.append(',');
         }

         this.builder.append(new StringTagVisitor().visit(pTag.get($$1)));
      }

      this.builder.append(']');
   }

   @Override
   public void visitCompound(CompoundTag pTag) {
      this.builder.append('{');
      List<String> $$1 = Lists.newArrayList(pTag.getAllKeys());
      Collections.sort($$1);

      for (String $$2 : $$1) {
         if (this.builder.length() != 1) {
            this.builder.append(',');
         }

         this.builder.append(handleEscape($$2)).append(':').append(new StringTagVisitor().visit(pTag.get($$2)));
      }

      this.builder.append('}');
   }

   protected static String handleEscape(String pText) {
      return SIMPLE_VALUE.matcher(pText).matches() ? pText : StringTag.quoteAndEscape(pText);
   }

   @Override
   public void visitEnd(EndTag pTag) {
      this.builder.append("END");
   }
}
