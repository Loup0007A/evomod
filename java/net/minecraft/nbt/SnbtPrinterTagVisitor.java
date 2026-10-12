package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.Util;

public class SnbtPrinterTagVisitor implements TagVisitor {
   private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), p_178114_ -> {
      p_178114_.put("{}", Lists.newArrayList(new String[]{"DataVersion", "author", "size", "data", "entities", "palette", "palettes"}));
      p_178114_.put("{}.data.[].{}", Lists.newArrayList(new String[]{"pos", "state", "nbt"}));
      p_178114_.put("{}.entities.[].{}", Lists.newArrayList(new String[]{"blockPos", "pos"}));
   });
   private static final Set<String> NO_INDENTATION = Sets.newHashSet(new String[]{"{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"});
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private final String indentation;
   private final int depth;
   private final List<String> path;
   private String result = "";

   public SnbtPrinterTagVisitor() {
      this("    ", 0, Lists.newArrayList());
   }

   public SnbtPrinterTagVisitor(String pIndentation, int pDepth, List<String> pPath) {
      this.indentation = pIndentation;
      this.depth = pDepth;
      this.path = pPath;
   }

   public String visit(Tag pTag) {
      pTag.accept(this);
      return this.result;
   }

   @Override
   public void visitString(StringTag pTag) {
      this.result = StringTag.quoteAndEscape(pTag.getAsString());
   }

   @Override
   public void visitByte(ByteTag pTag) {
      this.result = pTag.getAsNumber() + "b";
   }

   @Override
   public void visitShort(ShortTag pTag) {
      this.result = pTag.getAsNumber() + "s";
   }

   @Override
   public void visitInt(IntTag pTag) {
      this.result = String.valueOf(pTag.getAsNumber());
   }

   @Override
   public void visitLong(LongTag pTag) {
      this.result = pTag.getAsNumber() + "L";
   }

   @Override
   public void visitFloat(FloatTag pTag) {
      this.result = pTag.getAsFloat() + "f";
   }

   @Override
   public void visitDouble(DoubleTag pTag) {
      this.result = pTag.getAsDouble() + "d";
   }

   @Override
   public void visitByteArray(ByteArrayTag pTag) {
      StringBuilder $$1 = new StringBuilder("[").append("B").append(";");
      byte[] $$2 = pTag.getAsByteArray();

      for (int $$3 = 0; $$3 < $$2.length; $$3++) {
         $$1.append(" ").append($$2[$$3]).append("B");
         if ($$3 != $$2.length - 1) {
            $$1.append(ELEMENT_SEPARATOR);
         }
      }

      $$1.append("]");
      this.result = $$1.toString();
   }

   @Override
   public void visitIntArray(IntArrayTag pTag) {
      StringBuilder $$1 = new StringBuilder("[").append("I").append(";");
      int[] $$2 = pTag.getAsIntArray();

      for (int $$3 = 0; $$3 < $$2.length; $$3++) {
         $$1.append(" ").append($$2[$$3]);
         if ($$3 != $$2.length - 1) {
            $$1.append(ELEMENT_SEPARATOR);
         }
      }

      $$1.append("]");
      this.result = $$1.toString();
   }

   @Override
   public void visitLongArray(LongArrayTag pTag) {
      String $$1 = "L";
      StringBuilder $$2 = new StringBuilder("[").append("L").append(";");
      long[] $$3 = pTag.getAsLongArray();

      for (int $$4 = 0; $$4 < $$3.length; $$4++) {
         $$2.append(" ").append($$3[$$4]).append("L");
         if ($$4 != $$3.length - 1) {
            $$2.append(ELEMENT_SEPARATOR);
         }
      }

      $$2.append("]");
      this.result = $$2.toString();
   }

   @Override
   public void visitList(ListTag pTag) {
      if (pTag.isEmpty()) {
         this.result = "[]";
      } else {
         StringBuilder $$1 = new StringBuilder("[");
         this.pushPath("[]");
         String $$2 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!$$2.isEmpty()) {
            $$1.append("\n");
         }

         for (int $$3 = 0; $$3 < pTag.size(); $$3++) {
            $$1.append(Strings.repeat($$2, this.depth + 1));
            $$1.append(new SnbtPrinterTagVisitor($$2, this.depth + 1, this.path).visit(pTag.get($$3)));
            if ($$3 != pTag.size() - 1) {
               $$1.append(ELEMENT_SEPARATOR).append($$2.isEmpty() ? " " : "\n");
            }
         }

         if (!$$2.isEmpty()) {
            $$1.append("\n").append(Strings.repeat($$2, this.depth));
         }

         $$1.append("]");
         this.result = $$1.toString();
         this.popPath();
      }
   }

   @Override
   public void visitCompound(CompoundTag pTag) {
      if (pTag.isEmpty()) {
         this.result = "{}";
      } else {
         StringBuilder $$1 = new StringBuilder("{");
         this.pushPath("{}");
         String $$2 = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!$$2.isEmpty()) {
            $$1.append("\n");
         }

         Collection<String> $$3 = this.getKeys(pTag);
         Iterator<String> $$4 = $$3.iterator();

         while ($$4.hasNext()) {
            String $$5 = $$4.next();
            Tag $$6 = pTag.get($$5);
            this.pushPath($$5);
            $$1.append(Strings.repeat($$2, this.depth + 1))
               .append(handleEscapePretty($$5))
               .append(NAME_VALUE_SEPARATOR)
               .append(" ")
               .append(new SnbtPrinterTagVisitor($$2, this.depth + 1, this.path).visit($$6));
            this.popPath();
            if ($$4.hasNext()) {
               $$1.append(ELEMENT_SEPARATOR).append($$2.isEmpty() ? " " : "\n");
            }
         }

         if (!$$2.isEmpty()) {
            $$1.append("\n").append(Strings.repeat($$2, this.depth));
         }

         $$1.append("}");
         this.result = $$1.toString();
         this.popPath();
      }
   }

   private void popPath() {
      this.path.remove(this.path.size() - 1);
   }

   private void pushPath(String pKey) {
      this.path.add(pKey);
   }

   protected List<String> getKeys(CompoundTag pTag) {
      Set<String> $$1 = Sets.newHashSet(pTag.getAllKeys());
      List<String> $$2 = Lists.newArrayList();
      List<String> $$3 = KEY_ORDER.get(this.pathString());
      if ($$3 != null) {
         for (String $$4 : $$3) {
            if ($$1.remove($$4)) {
               $$2.add($$4);
            }
         }

         if (!$$1.isEmpty()) {
            $$1.stream().sorted().forEach($$2::add);
         }
      } else {
         $$2.addAll($$1);
         Collections.sort($$2);
      }

      return $$2;
   }

   public String pathString() {
      return String.join(".", this.path);
   }

   protected static String handleEscapePretty(String pText) {
      return SIMPLE_VALUE.matcher(pText).matches() ? pText : StringTag.quoteAndEscape(pText);
   }

   @Override
   public void visitEnd(EndTag pTag) {
   }
}
