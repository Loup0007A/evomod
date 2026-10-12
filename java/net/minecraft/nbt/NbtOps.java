package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();
   private static final String WRAPPER_MARKER = "";

   protected NbtOps() {
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public <U> U convertTo(DynamicOps<U> pOps, Tag pTag) {
      return (U)(switch (pTag.getId()) {
         case 0 -> (Object)pOps.empty();
         case 1 -> (Object)pOps.createByte(((NumericTag)pTag).getAsByte());
         case 2 -> (Object)pOps.createShort(((NumericTag)pTag).getAsShort());
         case 3 -> (Object)pOps.createInt(((NumericTag)pTag).getAsInt());
         case 4 -> (Object)pOps.createLong(((NumericTag)pTag).getAsLong());
         case 5 -> (Object)pOps.createFloat(((NumericTag)pTag).getAsFloat());
         case 6 -> (Object)pOps.createDouble(((NumericTag)pTag).getAsDouble());
         case 7 -> (Object)pOps.createByteList(ByteBuffer.wrap(((ByteArrayTag)pTag).getAsByteArray()));
         case 8 -> (Object)pOps.createString(pTag.getAsString());
         case 9 -> (Object)this.convertList(pOps, pTag);
         case 10 -> (Object)this.convertMap(pOps, pTag);
         case 11 -> (Object)pOps.createIntList(Arrays.stream(((IntArrayTag)pTag).getAsIntArray()));
         case 12 -> (Object)pOps.createLongList(Arrays.stream(((LongArrayTag)pTag).getAsLongArray()));
         default -> throw new IllegalStateException("Unknown tag type: " + pTag);
      });
   }

   public DataResult<Number> getNumberValue(Tag pTag) {
      return pTag instanceof NumericTag $$1 ? DataResult.success($$1.getAsNumber()) : DataResult.error(() -> "Not a number");
   }

   public Tag createNumeric(Number pData) {
      return DoubleTag.valueOf(pData.doubleValue());
   }

   public Tag createByte(byte pData) {
      return ByteTag.valueOf(pData);
   }

   public Tag createShort(short pData) {
      return ShortTag.valueOf(pData);
   }

   public Tag createInt(int pData) {
      return IntTag.valueOf(pData);
   }

   public Tag createLong(long pData) {
      return LongTag.valueOf(pData);
   }

   public Tag createFloat(float pData) {
      return FloatTag.valueOf(pData);
   }

   public Tag createDouble(double pData) {
      return DoubleTag.valueOf(pData);
   }

   public Tag createBoolean(boolean pData) {
      return ByteTag.valueOf(pData);
   }

   public DataResult<String> getStringValue(Tag pTag) {
      return pTag instanceof StringTag $$1 ? DataResult.success($$1.getAsString()) : DataResult.error(() -> "Not a string");
   }

   public Tag createString(String pData) {
      return StringTag.valueOf(pData);
   }

   public DataResult<Tag> mergeToList(Tag pList, Tag pTag) {
      return createCollector(pList)
         .map(p_248053_ -> DataResult.success(p_248053_.accept(pTag).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
   }

   public DataResult<Tag> mergeToList(Tag pList, List<Tag> pTags) {
      return createCollector(pList)
         .map(p_248048_ -> DataResult.success(p_248048_.acceptAll(pTags).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + pList, pList));
   }

   public DataResult<Tag> mergeToMap(Tag pMap, Tag pKey, Tag pValue) {
      if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
      } else if (!(pKey instanceof StringTag)) {
         return DataResult.error(() -> "key is not a string: " + pKey, pMap);
      } else {
         CompoundTag $$4 = pMap instanceof CompoundTag $$3 ? $$3.shallowCopy() : new CompoundTag();
         $$4.put(pKey.getAsString(), pValue);
         return DataResult.success($$4);
      }
   }

   public DataResult<Tag> mergeToMap(Tag pMap, MapLike<Tag> pOtherMap) {
      if (!(pMap instanceof CompoundTag) && !(pMap instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + pMap, pMap);
      } else {
         CompoundTag $$3 = pMap instanceof CompoundTag $$2 ? $$2.shallowCopy() : new CompoundTag();
         List<Tag> $$4 = new ArrayList<>();
         pOtherMap.entries().forEach(p_128994_ -> {
            Tag $$3x = (Tag)p_128994_.getFirst();
            if (!($$3x instanceof StringTag)) {
               $$4.add($$3x);
            } else {
               $$3.put($$3x.getAsString(), (Tag)p_128994_.getSecond());
            }
         });
         return !$$4.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + $$4, $$3) : DataResult.success($$3);
      }
   }

   public DataResult<Tag> mergeToMap(Tag p_336265_, Map<Tag, Tag> p_331137_) {
      if (!(p_336265_ instanceof CompoundTag) && !(p_336265_ instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + p_336265_, p_336265_);
      } else {
         CompoundTag $$3 = p_336265_ instanceof CompoundTag $$2 ? $$2.shallowCopy() : new CompoundTag();
         List<Tag> $$4 = new ArrayList<>();

         for (Entry<Tag, Tag> $$5 : p_331137_.entrySet()) {
            Tag $$6 = $$5.getKey();
            if ($$6 instanceof StringTag) {
               $$3.put($$6.getAsString(), $$5.getValue());
            } else {
               $$4.add($$6);
            }
         }

         return !$$4.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + $$4, $$3) : DataResult.success($$3);
      }
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag pMap) {
      return pMap instanceof CompoundTag $$1
         ? DataResult.success($$1.entrySet().stream().map(p_326024_ -> Pair.of(this.createString(p_326024_.getKey()), p_326024_.getValue())))
         : DataResult.error(() -> "Not a map: " + pMap);
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag pMap) {
      return pMap instanceof CompoundTag $$1 ? DataResult.success((Consumer<BiConsumer>)p_326020_ -> {
         for (Entry<String, Tag> $$2 : $$1.entrySet()) {
            p_326020_.accept(this.createString($$2.getKey()), $$2.getValue());
         }
      }) : DataResult.error(() -> "Not a map: " + pMap);
   }

   public DataResult<MapLike<Tag>> getMap(Tag pMap) {
      return pMap instanceof CompoundTag $$1 ? DataResult.success(new MapLike<Tag>() {
         @Nullable
         public Tag get(Tag p_129174_) {
            return $$1.get(p_129174_.getAsString());
         }

         @Nullable
         public Tag get(String p_129169_) {
            return $$1.get(p_129169_);
         }

         public Stream<Pair<Tag, Tag>> entries() {
            return $$1.entrySet().stream().map(p_326034_ -> Pair.of(NbtOps.this.createString(p_326034_.getKey()), p_326034_.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + $$1 + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + pMap);
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> pData) {
      CompoundTag $$1 = new CompoundTag();
      pData.forEach(p_129018_ -> $$1.put(((Tag)p_129018_.getFirst()).getAsString(), (Tag)p_129018_.getSecond()));
      return $$1;
   }

   private static Tag tryUnwrap(CompoundTag pTag) {
      if (pTag.size() == 1) {
         Tag $$1 = pTag.get("");
         if ($$1 != null) {
            return $$1;
         }
      }

      return pTag;
   }

   public DataResult<Stream<Tag>> getStream(Tag pTag) {
      if (pTag instanceof ListTag $$1) {
         return $$1.getElementType() == 10
            ? DataResult.success($$1.stream().map(p_248049_ -> tryUnwrap((CompoundTag)p_248049_)))
            : DataResult.success($$1.stream());
      } else {
         return pTag instanceof CollectionTag<?> $$2 ? DataResult.success($$2.stream().map(p_129158_ -> p_129158_)) : DataResult.error(() -> "Not a list");
      }
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag pTag) {
      if (pTag instanceof ListTag $$1) {
         return $$1.getElementType() == 10 ? DataResult.success((Consumer<Consumer>)p_326023_ -> {
            for (Tag $$2x : $$1) {
               p_326023_.accept(tryUnwrap((CompoundTag)$$2x));
            }
         }) : DataResult.success($$1::forEach);
      } else {
         return pTag instanceof CollectionTag<?> $$2 ? DataResult.success($$2::forEach) : DataResult.error(() -> "Not a list: " + pTag);
      }
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag pTag) {
      return pTag instanceof ByteArrayTag $$1 ? DataResult.success(ByteBuffer.wrap($$1.getAsByteArray())) : super.getByteBuffer(pTag);
   }

   public Tag createByteList(ByteBuffer pData) {
      ByteBuffer $$1 = pData.duplicate().clear();
      byte[] $$2 = new byte[pData.capacity()];
      $$1.get(0, $$2, 0, $$2.length);
      return new ByteArrayTag($$2);
   }

   public DataResult<IntStream> getIntStream(Tag pTag) {
      return pTag instanceof IntArrayTag $$1 ? DataResult.success(Arrays.stream($$1.getAsIntArray())) : super.getIntStream(pTag);
   }

   public Tag createIntList(IntStream pData) {
      return new IntArrayTag(pData.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag pTag) {
      return pTag instanceof LongArrayTag $$1 ? DataResult.success(Arrays.stream($$1.getAsLongArray())) : super.getLongStream(pTag);
   }

   public Tag createLongList(LongStream pData) {
      return new LongArrayTag(pData.toArray());
   }

   public Tag createList(Stream<Tag> pData) {
      return NbtOps.InitialListCollector.INSTANCE.acceptAll(pData).result();
   }

   public Tag remove(Tag pMap, String pRemoveKey) {
      if (pMap instanceof CompoundTag $$2) {
         CompoundTag $$3 = $$2.shallowCopy();
         $$3.remove(pRemoveKey);
         return $$3;
      } else {
         return pMap;
      }
   }

   @Override
   public String toString() {
      return "NBT";
   }

   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder();
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag pTag) {
      if (pTag instanceof EndTag) {
         return Optional.of(NbtOps.InitialListCollector.INSTANCE);
      } else {
         if (pTag instanceof CollectionTag<?> $$1) {
            if ($$1.isEmpty()) {
               return Optional.of(NbtOps.InitialListCollector.INSTANCE);
            }

            if ($$1 instanceof ListTag $$2) {
               return switch ($$2.getElementType()) {
                  case 0 -> Optional.of(NbtOps.InitialListCollector.INSTANCE);
                  case 10 -> Optional.of(new NbtOps.HeterogenousListCollector($$2));
                  default -> Optional.of(new NbtOps.HomogenousListCollector($$2));
               };
            }

            if ($$1 instanceof ByteArrayTag $$3) {
               return Optional.of(new NbtOps.ByteListCollector($$3.getAsByteArray()));
            }

            if ($$1 instanceof IntArrayTag $$4) {
               return Optional.of(new NbtOps.IntListCollector($$4.getAsIntArray()));
            }

            if ($$1 instanceof LongArrayTag $$5) {
               return Optional.of(new NbtOps.LongListCollector($$5.getAsLongArray()));
            }
         }

         return Optional.empty();
      }
   }

   static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte pValue) {
         this.values.add(pValue);
      }

      public ByteListCollector(byte[] pValues) {
         this.values.addElements(0, pValues);
      }

      @Override
      public NbtOps.ListCollector accept(Tag pTag) {
         if (pTag instanceof ByteTag $$1) {
            this.values.add($$1.getAsByte());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
         }
      }

      @Override
      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class HeterogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      public HeterogenousListCollector() {
      }

      public HeterogenousListCollector(Collection<Tag> pTags) {
         this.result.addAll(pTags);
      }

      public HeterogenousListCollector(IntArrayList pData) {
         pData.forEach(p_249166_ -> this.result.add(wrapElement(IntTag.valueOf(p_249166_))));
      }

      public HeterogenousListCollector(ByteArrayList pData) {
         pData.forEach(p_249160_ -> this.result.add(wrapElement(ByteTag.valueOf(p_249160_))));
      }

      public HeterogenousListCollector(LongArrayList pData) {
         pData.forEach(p_249754_ -> this.result.add(wrapElement(LongTag.valueOf(p_249754_))));
      }

      private static boolean isWrapper(CompoundTag pTag) {
         return pTag.size() == 1 && pTag.contains("");
      }

      private static Tag wrapIfNeeded(Tag pTag) {
         if (pTag instanceof CompoundTag $$1 && !isWrapper($$1)) {
            return $$1;
         }

         return wrapElement(pTag);
      }

      private static CompoundTag wrapElement(Tag pTag) {
         CompoundTag $$1 = new CompoundTag();
         $$1.put("", pTag);
         return $$1;
      }

      @Override
      public NbtOps.ListCollector accept(Tag pTag) {
         this.result.add(wrapIfNeeded(pTag));
         return this;
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class HomogenousListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      HomogenousListCollector(Tag pValue) {
         this.result.add(pValue);
      }

      HomogenousListCollector(ListTag pValues) {
         this.result.addAll(pValues);
      }

      @Override
      public NbtOps.ListCollector accept(Tag pTag) {
         if (pTag.getId() != this.result.getElementType()) {
            return new NbtOps.HeterogenousListCollector().acceptAll(this.result).accept(pTag);
         } else {
            this.result.add(pTag);
            return this;
         }
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class InitialListCollector implements NbtOps.ListCollector {
      public static final NbtOps.InitialListCollector INSTANCE = new NbtOps.InitialListCollector();

      private InitialListCollector() {
      }

      @Override
      public NbtOps.ListCollector accept(Tag p_251635_) {
         if (p_251635_ instanceof CompoundTag $$1) {
            return new NbtOps.HeterogenousListCollector().accept($$1);
         } else if (p_251635_ instanceof ByteTag $$2) {
            return new NbtOps.ByteListCollector($$2.getAsByte());
         } else if (p_251635_ instanceof IntTag $$3) {
            return new NbtOps.IntListCollector($$3.getAsInt());
         } else {
            return (NbtOps.ListCollector)(p_251635_ instanceof LongTag $$4
               ? new NbtOps.LongListCollector($$4.getAsLong())
               : new NbtOps.HomogenousListCollector(p_251635_));
         }
      }

      @Override
      public Tag result() {
         return new ListTag();
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int pValue) {
         this.values.add(pValue);
      }

      public IntListCollector(int[] pValues) {
         this.values.addElements(0, pValues);
      }

      @Override
      public NbtOps.ListCollector accept(Tag pTag) {
         if (pTag instanceof IntTag $$1) {
            this.values.add($$1.getAsInt());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
         }
      }

      @Override
      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   interface ListCollector {
      NbtOps.ListCollector accept(Tag pTag);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> pTags) {
         NbtOps.ListCollector $$1 = this;

         for (Tag $$2 : pTags) {
            $$1 = $$1.accept($$2);
         }

         return $$1;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> pTags) {
         return this.acceptAll(pTags::iterator);
      }

      Tag result();
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long pValue) {
         this.values.add(pValue);
      }

      public LongListCollector(long[] pValues) {
         this.values.addElements(0, pValues);
      }

      @Override
      public NbtOps.ListCollector accept(Tag pTag) {
         if (pTag instanceof LongTag $$1) {
            this.values.add($$1.getAsLong());
            return this;
         } else {
            return new NbtOps.HeterogenousListCollector(this.values).accept(pTag);
         }
      }

      @Override
      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder() {
         super(NbtOps.this);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String pKey, Tag pValue, CompoundTag pTag) {
         pTag.put(pKey, pValue);
         return pTag;
      }

      protected DataResult<Tag> build(CompoundTag p_129190_, Tag p_129191_) {
         if (p_129191_ == null || p_129191_ == EndTag.INSTANCE) {
            return DataResult.success(p_129190_);
         } else if (!(p_129191_ instanceof CompoundTag $$2)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + p_129191_, p_129191_);
         } else {
            CompoundTag $$3 = $$2.shallowCopy();

            for (Entry<String, Tag> $$4 : p_129190_.entrySet()) {
               $$3.put($$4.getKey(), $$4.getValue());
            }

            return DataResult.success($$3);
         }
      }
   }
}
