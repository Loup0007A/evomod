package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NbtPathArgument implements ArgumentType<NbtPathArgument.NbtPath> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
   public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.node.invalid"));
   public static final SimpleCommandExceptionType ERROR_DATA_TOO_DEEP = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.too_deep"));
   public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType(
      p_308351_ -> Component.translatableEscape("arguments.nbtpath.nothing_found", p_308351_)
   );
   static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType(
      p_308352_ -> Component.translatableEscape("commands.data.modify.expected_list", p_308352_)
   );
   static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType(
      p_308350_ -> Component.translatableEscape("commands.data.modify.invalid_index", p_308350_)
   );
   private static final char INDEX_MATCH_START = '[';
   private static final char INDEX_MATCH_END = ']';
   private static final char KEY_MATCH_START = '{';
   private static final char KEY_MATCH_END = '}';
   private static final char QUOTED_KEY_START = '"';
   private static final char SINGLE_QUOTED_KEY_START = '\'';

   public static NbtPathArgument nbtPath() {
      return new NbtPathArgument();
   }

   public static NbtPathArgument.NbtPath getPath(CommandContext<CommandSourceStack> pContext, String pName) {
      return (NbtPathArgument.NbtPath)pContext.getArgument(pName, NbtPathArgument.NbtPath.class);
   }

   public NbtPathArgument.NbtPath parse(StringReader pReader) throws CommandSyntaxException {
      List<NbtPathArgument.Node> $$1 = Lists.newArrayList();
      int $$2 = pReader.getCursor();
      Object2IntMap<NbtPathArgument.Node> $$3 = new Object2IntOpenHashMap();
      boolean $$4 = true;

      while (pReader.canRead() && pReader.peek() != ' ') {
         NbtPathArgument.Node $$5 = parseNode(pReader, $$4);
         $$1.add($$5);
         $$3.put($$5, pReader.getCursor() - $$2);
         $$4 = false;
         if (pReader.canRead()) {
            char $$6 = pReader.peek();
            if ($$6 != ' ' && $$6 != '[' && $$6 != '{') {
               pReader.expect('.');
            }
         }
      }

      return new NbtPathArgument.NbtPath(pReader.getString().substring($$2, pReader.getCursor()), $$1.toArray(new NbtPathArgument.Node[0]), $$3);
   }

   private static NbtPathArgument.Node parseNode(StringReader pReader, boolean pFirst) throws CommandSyntaxException {
      return (NbtPathArgument.Node)(switch (pReader.peek()) {
         case '"', '\'' -> readObjectNode(pReader, pReader.readString());
         case '[' -> {
            pReader.skip();
            int $$3 = pReader.peek();
            if ($$3 == 123) {
               CompoundTag $$4 = new TagParser(pReader).readStruct();
               pReader.expect(']');
               yield new NbtPathArgument.MatchElementNode($$4);
            } else if ($$3 == 93) {
               pReader.skip();
               yield NbtPathArgument.AllElementsNode.INSTANCE;
            } else {
               int $$5 = pReader.readInt();
               pReader.expect(']');
               yield new NbtPathArgument.IndexedElementNode($$5);
            }
         }
         case '{' -> {
            if (!pFirst) {
               throw ERROR_INVALID_NODE.createWithContext(pReader);
            }

            CompoundTag $$2 = new TagParser(pReader).readStruct();
            yield new NbtPathArgument.MatchRootObjectNode($$2);
         }
         default -> readObjectNode(pReader, readUnquotedName(pReader));
      });
   }

   private static NbtPathArgument.Node readObjectNode(StringReader pReader, String pName) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '{') {
         CompoundTag $$2 = new TagParser(pReader).readStruct();
         return new NbtPathArgument.MatchObjectNode(pName, $$2);
      } else {
         return new NbtPathArgument.CompoundChildNode(pName);
      }
   }

   private static String readUnquotedName(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();

      while (pReader.canRead() && isAllowedInUnquotedName(pReader.peek())) {
         pReader.skip();
      }

      if (pReader.getCursor() == $$1) {
         throw ERROR_INVALID_NODE.createWithContext(pReader);
      } else {
         return pReader.getString().substring($$1, pReader.getCursor());
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static boolean isAllowedInUnquotedName(char pCh) {
      return pCh != ' ' && pCh != '"' && pCh != '\'' && pCh != '[' && pCh != ']' && pCh != '.' && pCh != '{' && pCh != '}';
   }

   static Predicate<Tag> createTagPredicate(CompoundTag pTag) {
      return p_99507_ -> NbtUtils.compareNbt(pTag, p_99507_, true);
   }

   static class AllElementsNode implements NbtPathArgument.Node {
      public static final NbtPathArgument.AllElementsNode INSTANCE = new NbtPathArgument.AllElementsNode();

      private AllElementsNode() {
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof CollectionTag) {
            pTags.addAll((CollectionTag)pTag);
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         if (pTag instanceof CollectionTag<?> $$3) {
            if ($$3.isEmpty()) {
               Tag $$4 = pSupplier.get();
               if ($$3.addTag(0, $$4)) {
                  pTags.add($$4);
               }
            } else {
               pTags.addAll((Collection<? extends Tag>)$$3);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         if (!(pTag instanceof CollectionTag<?> $$2)) {
            return 0;
         } else {
            int $$3 = $$2.size();
            if ($$3 == 0) {
               $$2.addTag(0, pSupplier.get());
               return 1;
            } else {
               Tag $$4 = pSupplier.get();
               int $$5 = $$3 - (int)$$2.stream().filter($$4::equals).count();
               if ($$5 == 0) {
                  return 0;
               } else {
                  $$2.clear();
                  if (!$$2.addTag(0, $$4)) {
                     return 0;
                  } else {
                     for (int $$6 = 1; $$6 < $$3; $$6++) {
                        $$2.addTag($$6, pSupplier.get());
                     }

                     return $$5;
                  }
               }
            }
         }
      }

      @Override
      public int removeTag(Tag pTag) {
         if (pTag instanceof CollectionTag<?> $$1) {
            int $$2 = $$1.size();
            if ($$2 > 0) {
               $$1.clear();
               return $$2;
            }
         }

         return 0;
      }
   }

   static class CompoundChildNode implements NbtPathArgument.Node {
      private final String name;

      public CompoundChildNode(String pName) {
         this.name = pName;
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof CompoundTag) {
            Tag $$2 = ((CompoundTag)pTag).get(this.name);
            if ($$2 != null) {
               pTags.add($$2);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         if (pTag instanceof CompoundTag $$3) {
            Tag $$4;
            if ($$3.contains(this.name)) {
               $$4 = $$3.get(this.name);
            } else {
               $$4 = pSupplier.get();
               $$3.put(this.name, $$4);
            }

            pTags.add($$4);
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         if (pTag instanceof CompoundTag $$2) {
            Tag $$3 = pSupplier.get();
            Tag $$4 = $$2.put(this.name, $$3);
            if (!$$3.equals($$4)) {
               return 1;
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag pTag) {
         if (pTag instanceof CompoundTag $$1 && $$1.contains(this.name)) {
            $$1.remove(this.name);
            return 1;
         }

         return 0;
      }
   }

   static class IndexedElementNode implements NbtPathArgument.Node {
      private final int index;

      public IndexedElementNode(int pIndex) {
         this.index = pIndex;
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof CollectionTag<?> $$2) {
            int $$3 = $$2.size();
            int $$4 = this.index < 0 ? $$3 + this.index : this.index;
            if (0 <= $$4 && $$4 < $$3) {
               pTags.add($$2.get($$4));
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         this.getTag(pTag, pTags);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         if (pTag instanceof CollectionTag<?> $$2) {
            int $$3 = $$2.size();
            int $$4 = this.index < 0 ? $$3 + this.index : this.index;
            if (0 <= $$4 && $$4 < $$3) {
               Tag $$5 = $$2.get($$4);
               Tag $$6 = pSupplier.get();
               if (!$$6.equals($$5) && $$2.setTag($$4, $$6)) {
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag pTag) {
         if (pTag instanceof CollectionTag<?> $$1) {
            int $$2 = $$1.size();
            int $$3 = this.index < 0 ? $$2 + this.index : this.index;
            if (0 <= $$3 && $$3 < $$2) {
               $$1.remove($$3);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchElementNode implements NbtPathArgument.Node {
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchElementNode(CompoundTag pPattern) {
         this.pattern = pPattern;
         this.predicate = NbtPathArgument.createTagPredicate(pPattern);
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof ListTag $$2) {
            $$2.stream().filter(this.predicate).forEach(pTags::add);
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         MutableBoolean $$3 = new MutableBoolean();
         if (pTag instanceof ListTag $$4) {
            $$4.stream().filter(this.predicate).forEach(p_99571_ -> {
               pTags.add(p_99571_);
               $$3.setTrue();
            });
            if ($$3.isFalse()) {
               CompoundTag $$5 = this.pattern.copy();
               $$4.add($$5);
               pTags.add($$5);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         int $$2 = 0;
         if (pTag instanceof ListTag $$3) {
            int $$4 = $$3.size();
            if ($$4 == 0) {
               $$3.add(pSupplier.get());
               $$2++;
            } else {
               for (int $$5 = 0; $$5 < $$4; $$5++) {
                  Tag $$6 = $$3.get($$5);
                  if (this.predicate.test($$6)) {
                     Tag $$7 = pSupplier.get();
                     if (!$$7.equals($$6) && $$3.setTag($$5, $$7)) {
                        $$2++;
                     }
                  }
               }
            }
         }

         return $$2;
      }

      @Override
      public int removeTag(Tag pTag) {
         int $$1 = 0;
         if (pTag instanceof ListTag $$2) {
            for (int $$3 = $$2.size() - 1; $$3 >= 0; $$3--) {
               if (this.predicate.test($$2.get($$3))) {
                  $$2.remove($$3);
                  $$1++;
               }
            }
         }

         return $$1;
      }
   }

   static class MatchObjectNode implements NbtPathArgument.Node {
      private final String name;
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchObjectNode(String pName, CompoundTag pPattern) {
         this.name = pName;
         this.pattern = pPattern;
         this.predicate = NbtPathArgument.createTagPredicate(pPattern);
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof CompoundTag) {
            Tag $$2 = ((CompoundTag)pTag).get(this.name);
            if (this.predicate.test($$2)) {
               pTags.add($$2);
            }
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         if (pTag instanceof CompoundTag $$3) {
            Tag $$4 = $$3.get(this.name);
            if ($$4 == null) {
               Tag var6 = this.pattern.copy();
               $$3.put(this.name, var6);
               pTags.add(var6);
            } else if (this.predicate.test($$4)) {
               pTags.add($$4);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         if (pTag instanceof CompoundTag $$2) {
            Tag $$3 = $$2.get(this.name);
            if (this.predicate.test($$3)) {
               Tag $$4 = pSupplier.get();
               if (!$$4.equals($$3)) {
                  $$2.put(this.name, $$4);
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(Tag pTag) {
         if (pTag instanceof CompoundTag $$1) {
            Tag $$2 = $$1.get(this.name);
            if (this.predicate.test($$2)) {
               $$1.remove(this.name);
               return 1;
            }
         }

         return 0;
      }
   }

   static class MatchRootObjectNode implements NbtPathArgument.Node {
      private final Predicate<Tag> predicate;

      public MatchRootObjectNode(CompoundTag pTag) {
         this.predicate = NbtPathArgument.createTagPredicate(pTag);
      }

      @Override
      public void getTag(Tag pTag, List<Tag> pTags) {
         if (pTag instanceof CompoundTag && this.predicate.test(pTag)) {
            pTags.add(pTag);
         }
      }

      @Override
      public void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags) {
         this.getTag(pTag, pTags);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(Tag pTag, Supplier<Tag> pSupplier) {
         return 0;
      }

      @Override
      public int removeTag(Tag pTag) {
         return 0;
      }
   }

   public static class NbtPath {
      private final String original;
      private final Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition;
      private final NbtPathArgument.Node[] nodes;
      public static final Codec<NbtPathArgument.NbtPath> CODEC = Codec.STRING.comapFlatMap(p_325592_ -> {
         try {
            NbtPathArgument.NbtPath $$1 = new NbtPathArgument().parse(new StringReader(p_325592_));
            return DataResult.success($$1);
         } catch (CommandSyntaxException var2) {
            return DataResult.error(() -> "Failed to parse path " + p_325592_ + ": " + var2.getMessage());
         }
      }, NbtPathArgument.NbtPath::asString);

      public static NbtPathArgument.NbtPath of(String pPath) throws CommandSyntaxException {
         return new NbtPathArgument().parse(new StringReader(pPath));
      }

      public NbtPath(String pOriginal, NbtPathArgument.Node[] pNodes, Object2IntMap<NbtPathArgument.Node> pNodeToOriginPosition) {
         this.original = pOriginal;
         this.nodes = pNodes;
         this.nodeToOriginalPosition = pNodeToOriginPosition;
      }

      public List<Tag> get(Tag pTag) throws CommandSyntaxException {
         List<Tag> $$1 = Collections.singletonList(pTag);

         for (NbtPathArgument.Node $$2 : this.nodes) {
            $$1 = $$2.get($$1);
            if ($$1.isEmpty()) {
               throw this.createNotFoundException($$2);
            }
         }

         return $$1;
      }

      public int countMatching(Tag pTag) {
         List<Tag> $$1 = Collections.singletonList(pTag);

         for (NbtPathArgument.Node $$2 : this.nodes) {
            $$1 = $$2.get($$1);
            if ($$1.isEmpty()) {
               return 0;
            }
         }

         return $$1.size();
      }

      private List<Tag> getOrCreateParents(Tag pTag) throws CommandSyntaxException {
         List<Tag> $$1 = Collections.singletonList(pTag);

         for (int $$2 = 0; $$2 < this.nodes.length - 1; $$2++) {
            NbtPathArgument.Node $$3 = this.nodes[$$2];
            int $$4 = $$2 + 1;
            $$1 = $$3.getOrCreate($$1, this.nodes[$$4]::createPreferredParentTag);
            if ($$1.isEmpty()) {
               throw this.createNotFoundException($$3);
            }
         }

         return $$1;
      }

      public List<Tag> getOrCreate(Tag pTag, Supplier<Tag> pSupplier) throws CommandSyntaxException {
         List<Tag> $$2 = this.getOrCreateParents(pTag);
         NbtPathArgument.Node $$3 = this.nodes[this.nodes.length - 1];
         return $$3.getOrCreate($$2, pSupplier);
      }

      private static int apply(List<Tag> pTags, Function<Tag, Integer> pFunction) {
         return pTags.stream().map(pFunction).reduce(0, (p_99633_, p_99634_) -> p_99633_ + p_99634_);
      }

      public static boolean isTooDeep(Tag pTag, int pCurrentDepth) {
         if (pCurrentDepth >= 512) {
            return true;
         } else {
            if (pTag instanceof CompoundTag $$2) {
               for (String $$3 : $$2.getAllKeys()) {
                  Tag $$4 = $$2.get($$3);
                  if ($$4 != null && isTooDeep($$4, pCurrentDepth + 1)) {
                     return true;
                  }
               }
            } else if (pTag instanceof ListTag) {
               for (Tag $$6 : (ListTag)pTag) {
                  if (isTooDeep($$6, pCurrentDepth + 1)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      public int set(Tag pTag, Tag pOther) throws CommandSyntaxException {
         if (isTooDeep(pOther, this.estimatePathDepth())) {
            throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
         } else {
            Tag $$2 = pOther.copy();
            List<Tag> $$3 = this.getOrCreateParents(pTag);
            if ($$3.isEmpty()) {
               return 0;
            } else {
               NbtPathArgument.Node $$4 = this.nodes[this.nodes.length - 1];
               MutableBoolean $$5 = new MutableBoolean(false);
               return apply($$3, p_263259_ -> $$4.setTag(p_263259_, () -> {
                     if ($$5.isFalse()) {
                        $$5.setTrue();
                        return $$2;
                     } else {
                        return $$2.copy();
                     }
                  }));
            }
         }
      }

      private int estimatePathDepth() {
         return this.nodes.length;
      }

      public int insert(int pIndex, CompoundTag pRootTag, List<Tag> pTagsToInsert) throws CommandSyntaxException {
         List<Tag> $$3 = new ArrayList<>(pTagsToInsert.size());

         for (Tag $$4 : pTagsToInsert) {
            Tag $$5 = $$4.copy();
            $$3.add($$5);
            if (isTooDeep($$5, this.estimatePathDepth())) {
               throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
            }
         }

         Collection<Tag> $$6 = this.getOrCreate(pRootTag, ListTag::new);
         int $$7 = 0;
         boolean $$8 = false;

         for (Tag $$9 : $$6) {
            if (!($$9 instanceof CollectionTag<?> $$10)) {
               throw NbtPathArgument.ERROR_EXPECTED_LIST.create($$9);
            }

            boolean $$11 = false;
            int $$12 = pIndex < 0 ? $$10.size() + pIndex + 1 : pIndex;

            for (Tag $$13 : $$3) {
               try {
                  if ($$10.addTag($$12, $$8 ? $$13.copy() : $$13)) {
                     $$12++;
                     $$11 = true;
                  }
               } catch (IndexOutOfBoundsException var16) {
                  throw NbtPathArgument.ERROR_INVALID_INDEX.create($$12);
               }
            }

            $$8 = true;
            $$7 += $$11 ? 1 : 0;
         }

         return $$7;
      }

      public int remove(Tag pTag) {
         List<Tag> $$1 = Collections.singletonList(pTag);

         for (int $$2 = 0; $$2 < this.nodes.length - 1; $$2++) {
            $$1 = this.nodes[$$2].get($$1);
         }

         NbtPathArgument.Node $$3 = this.nodes[this.nodes.length - 1];
         return apply($$1, $$3::removeTag);
      }

      private CommandSyntaxException createNotFoundException(NbtPathArgument.Node pNode) {
         int $$1 = this.nodeToOriginalPosition.getInt(pNode);
         return NbtPathArgument.ERROR_NOTHING_FOUND.create(this.original.substring(0, $$1));
      }

      @Override
      public String toString() {
         return this.original;
      }

      public String asString() {
         return this.original;
      }
   }

   interface Node {
      void getTag(Tag p_99666_, List<Tag> p_99667_);

      void getOrCreateTag(Tag pTag, Supplier<Tag> pSupplier, List<Tag> pTags);

      Tag createPreferredParentTag();

      int setTag(Tag pTag, Supplier<Tag> pSupplier);

      int removeTag(Tag pTag);

      default List<Tag> get(List<Tag> pTags) {
         return this.collect(pTags, this::getTag);
      }

      default List<Tag> getOrCreate(List<Tag> pTags, Supplier<Tag> pSupplier) {
         return this.collect(pTags, (p_99663_, p_99664_) -> this.getOrCreateTag(p_99663_, pSupplier, p_99664_));
      }

      default List<Tag> collect(List<Tag> pTags, BiConsumer<Tag, List<Tag>> pConsumer) {
         List<Tag> $$2 = Lists.newArrayList();

         for (Tag $$3 : pTags) {
            pConsumer.accept($$3, $$2);
         }

         return $$2;
      }
   }
}
