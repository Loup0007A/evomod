package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.ListBuilder.Builder;
import com.mojang.serialization.RecordBuilder.MapBuilder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DelegatingOps<T> implements DynamicOps<T> {
   protected final DynamicOps<T> delegate;
   Map<ResourceLocation, Object> ctx = new HashMap<>();

   protected DelegatingOps(DynamicOps<T> pDelegate) {
      this.delegate = pDelegate;
   }

   public T empty() {
      return (T)this.delegate.empty();
   }

   public T emptyMap() {
      return (T)this.delegate.emptyMap();
   }

   public T emptyList() {
      return (T)this.delegate.emptyList();
   }

   public <U> U convertTo(DynamicOps<U> pOutOps, T pInput) {
      return (U)this.delegate.convertTo(pOutOps, pInput);
   }

   public DataResult<Number> getNumberValue(T pInput) {
      return this.delegate.getNumberValue(pInput);
   }

   public T createNumeric(Number pI) {
      return (T)this.delegate.createNumeric(pI);
   }

   public T createByte(byte pValue) {
      return (T)this.delegate.createByte(pValue);
   }

   public T createShort(short pValue) {
      return (T)this.delegate.createShort(pValue);
   }

   public T createInt(int pValue) {
      return (T)this.delegate.createInt(pValue);
   }

   public T createLong(long pValue) {
      return (T)this.delegate.createLong(pValue);
   }

   public T createFloat(float pValue) {
      return (T)this.delegate.createFloat(pValue);
   }

   public T createDouble(double pValue) {
      return (T)this.delegate.createDouble(pValue);
   }

   public DataResult<Boolean> getBooleanValue(T pInput) {
      return this.delegate.getBooleanValue(pInput);
   }

   public T createBoolean(boolean pValue) {
      return (T)this.delegate.createBoolean(pValue);
   }

   public DataResult<String> getStringValue(T pInput) {
      return this.delegate.getStringValue(pInput);
   }

   public T createString(String pValue) {
      return (T)this.delegate.createString(pValue);
   }

   public DataResult<T> mergeToList(T pList, T pValue) {
      return this.delegate.mergeToList(pList, pValue);
   }

   public DataResult<T> mergeToList(T pList, List<T> pValues) {
      return this.delegate.mergeToList(pList, pValues);
   }

   public DataResult<T> mergeToMap(T pMap, T pKey, T pValue) {
      return this.delegate.mergeToMap(pMap, pKey, pValue);
   }

   public DataResult<T> mergeToMap(T pMap, MapLike<T> pValues) {
      return this.delegate.mergeToMap(pMap, pValues);
   }

   public DataResult<T> mergeToMap(T pMap, Map<T, T> pValues) {
      return this.delegate.mergeToMap(pMap, pValues);
   }

   public DataResult<T> mergeToPrimitive(T pPrefix, T pValue) {
      return this.delegate.mergeToPrimitive(pPrefix, pValue);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(T pInput) {
      return this.delegate.getMapValues(pInput);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T pInput) {
      return this.delegate.getMapEntries(pInput);
   }

   public T createMap(Map<T, T> pMap) {
      return (T)this.delegate.createMap(pMap);
   }

   public T createMap(Stream<Pair<T, T>> pMap) {
      return (T)this.delegate.createMap(pMap);
   }

   public DataResult<MapLike<T>> getMap(T pInput) {
      return this.delegate.getMap(pInput);
   }

   public DataResult<Stream<T>> getStream(T pInput) {
      return this.delegate.getStream(pInput);
   }

   public DataResult<Consumer<Consumer<T>>> getList(T pInput) {
      return this.delegate.getList(pInput);
   }

   public T createList(Stream<T> pInput) {
      return (T)this.delegate.createList(pInput);
   }

   public DataResult<ByteBuffer> getByteBuffer(T pInput) {
      return this.delegate.getByteBuffer(pInput);
   }

   public T createByteList(ByteBuffer pInput) {
      return (T)this.delegate.createByteList(pInput);
   }

   public DataResult<IntStream> getIntStream(T pInput) {
      return this.delegate.getIntStream(pInput);
   }

   public T createIntList(IntStream pInput) {
      return (T)this.delegate.createIntList(pInput);
   }

   public DataResult<LongStream> getLongStream(T pInput) {
      return this.delegate.getLongStream(pInput);
   }

   public T createLongList(LongStream pInput) {
      return (T)this.delegate.createLongList(pInput);
   }

   public T remove(T pInput, String pKey) {
      return (T)this.delegate.remove(pInput, pKey);
   }

   public boolean compressMaps() {
      return this.delegate.compressMaps();
   }

   public ListBuilder<T> listBuilder() {
      return new Builder(this);
   }

   public RecordBuilder<T> mapBuilder() {
      return new MapBuilder(this);
   }

   public <R> R getContext(ResourceLocation rl) {
      R ret = (R)this.ctx.get(rl);
      return ret == null && this.delegate instanceof DelegatingOps parent ? parent.getContext(rl) : ret;
   }

   public <R> R withContext(ResourceLocation key, Object ctx) {
      this.ctx.put(key, ctx);
      return (R)this;
   }
}
