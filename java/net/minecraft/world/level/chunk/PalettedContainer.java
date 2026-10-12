package net.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T> {
   private static final int MIN_PALETTE_BITS = 0;
   private final PaletteResize<T> dummyPaletteResize = (p_238275_, p_238276_) -> 0;
   private final IdMap<T> registry;
   private volatile PalettedContainer.Data<T> data;
   private final PalettedContainer.Strategy strategy;
   private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

   public void acquire() {
      this.threadingDetector.checkAndLock();
   }

   public void release() {
      this.threadingDetector.checkAndUnlock();
   }

   public static <T> Codec<PalettedContainer<T>> codecRW(IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainer<T>> $$4 = PalettedContainer::unpack;
      return codec(pRegistry, pCodec, pStrategy, pValue, $$4);
   }

   public static <T> Codec<PalettedContainerRO<T>> codecRO(IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> $$4 = (p_327412_, p_327413_, p_327414_) -> unpack(p_327412_, p_327413_, p_327414_)
            .map(p_238264_ -> p_238264_);
      return codec(pRegistry, pCodec, pStrategy, pValue, $$4);
   }

   private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
      IdMap<T> pRegistry, Codec<T> pCodec, PalettedContainer.Strategy pStrategy, T pValue, PalettedContainerRO.Unpacker<T, C> pUnpacker
   ) {
      return RecordCodecBuilder.create(
            p_327417_ -> p_327417_.group(
                     pCodec.mapResult(ExtraCodecs.orElsePartial(pValue)).listOf().fieldOf("palette").forGetter(PalettedContainerRO.PackedData::paletteEntries),
                     Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
                  )
                  .apply(p_327417_, PalettedContainerRO.PackedData::new)
         )
         .comapFlatMap(p_238262_ -> pUnpacker.read(pRegistry, pStrategy, p_238262_), p_238263_ -> p_238263_.pack(pRegistry, pStrategy));
   }

   public PalettedContainer(
      IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy, PalettedContainer.Configuration<T> pConfiguration, BitStorage pStorage, List<T> pValues
   ) {
      this.registry = pRegistry;
      this.strategy = pStrategy;
      this.data = new PalettedContainer.Data<>(pConfiguration, pStorage, pConfiguration.factory().create(pConfiguration.bits(), pRegistry, this, pValues));
   }

   private PalettedContainer(IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy, PalettedContainer.Data<T> pData) {
      this.registry = pRegistry;
      this.strategy = pStrategy;
      this.data = pData;
   }

   public PalettedContainer(IdMap<T> pRegistry, T pPalette, PalettedContainer.Strategy pStrategy) {
      this.strategy = pStrategy;
      this.registry = pRegistry;
      this.data = this.createOrReuseData(null, 0);
      this.data.palette.idFor(pPalette);
   }

   private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> pData, int pId) {
      PalettedContainer.Configuration<T> $$2 = this.strategy.getConfiguration(this.registry, pId);
      return pData != null && $$2.equals(pData.configuration()) ? pData : $$2.createData(this.registry, this, this.strategy.size());
   }

   @Override
   public int onResize(int pBits, T pObjectAdded) {
      PalettedContainer.Data<T> $$2 = this.data;
      PalettedContainer.Data<T> $$3 = this.createOrReuseData($$2, pBits);
      $$3.copyFrom($$2.palette, $$2.storage);
      this.data = $$3;
      return $$3.palette.idFor(pObjectAdded);
   }

   public T getAndSet(int pX, int pY, int pZ, T pState) {
      this.acquire();

      Object var5;
      try {
         var5 = this.getAndSet(this.strategy.getIndex(pX, pY, pZ), pState);
      } finally {
         this.release();
      }

      return (T)var5;
   }

   public T getAndSetUnchecked(int pX, int pY, int pZ, T pState) {
      return this.getAndSet(this.strategy.getIndex(pX, pY, pZ), pState);
   }

   private T getAndSet(int pIndex, T pState) {
      int $$2 = this.data.palette.idFor(pState);
      int $$3 = this.data.storage.getAndSet(pIndex, $$2);
      return this.data.palette.valueFor($$3);
   }

   public void set(int pX, int pY, int pZ, T pState) {
      this.acquire();

      try {
         this.set(this.strategy.getIndex(pX, pY, pZ), pState);
      } finally {
         this.release();
      }
   }

   private void set(int pIndex, T pState) {
      int $$2 = this.data.palette.idFor(pState);
      this.data.storage.set(pIndex, $$2);
   }

   @Override
   public T get(int pX, int pY, int pZ) {
      return this.get(this.strategy.getIndex(pX, pY, pZ));
   }

   protected T get(int pIndex) {
      PalettedContainer.Data<T> $$1 = this.data;
      return $$1.palette.valueFor($$1.storage.get(pIndex));
   }

   @Override
   public void getAll(Consumer<T> pConsumer) {
      Palette<T> $$1 = this.data.palette();
      IntSet $$2 = new IntArraySet();
      this.data.storage.getAll($$2::add);
      $$2.forEach(p_238274_ -> pConsumer.accept($$1.valueFor(p_238274_)));
   }

   public void read(FriendlyByteBuf pBuffer) {
      this.acquire();

      try {
         int $$1 = pBuffer.readByte();
         PalettedContainer.Data<T> $$2 = this.createOrReuseData(this.data, $$1);
         $$2.palette.read(pBuffer);
         pBuffer.readLongArray($$2.storage.getRaw());
         this.data = $$2;
      } finally {
         this.release();
      }
   }

   @Override
   public void write(FriendlyByteBuf pBuffer) {
      this.acquire();

      try {
         this.data.write(pBuffer);
      } finally {
         this.release();
      }
   }

   private static <T> DataResult<PalettedContainer<T>> unpack(
      IdMap<T> p_188068_, PalettedContainer.Strategy p_188069_, PalettedContainerRO.PackedData<T> p_238258_
   ) {
      List<T> $$3 = p_238258_.paletteEntries();
      int $$4 = p_188069_.size();
      int $$5 = p_188069_.calculateBitsForSerialization(p_188068_, $$3.size());
      PalettedContainer.Configuration<T> $$6 = p_188069_.getConfiguration(p_188068_, $$5);
      BitStorage $$7;
      if ($$5 == 0) {
         $$7 = new ZeroBitStorage($$4);
      } else {
         Optional<LongStream> $$8 = p_238258_.storage();
         if ($$8.isEmpty()) {
            return DataResult.error(() -> "Missing values for non-zero storage");
         }

         long[] $$9 = $$8.get().toArray();

         try {
            if ($$6.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY) {
               Palette<T> $$10 = new HashMapPalette<>(p_188068_, $$5, (p_238278_, p_238279_) -> 0, $$3);
               SimpleBitStorage $$11 = new SimpleBitStorage($$5, $$4, $$9);
               int[] $$12 = new int[$$4];
               $$11.unpack($$12);
               swapPalette($$12, p_238283_ -> p_188068_.getId($$10.valueFor(p_238283_)));
               $$7 = new SimpleBitStorage($$6.bits(), $$4, $$12);
            } else {
               $$7 = new SimpleBitStorage($$6.bits(), $$4, $$9);
            }
         } catch (SimpleBitStorage.InitializationException var13) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + var13.getMessage());
         }
      }

      return DataResult.success(new PalettedContainer<>(p_188068_, p_188069_, $$6, $$7, $$3));
   }

   @Override
   public PalettedContainerRO.PackedData<T> pack(IdMap<T> pRegistry, PalettedContainer.Strategy pStrategy) {
      this.acquire();

      PalettedContainerRO.PackedData var12;
      try {
         HashMapPalette<T> $$2 = new HashMapPalette<>(pRegistry, this.data.storage.getBits(), this.dummyPaletteResize);
         int $$3 = pStrategy.size();
         int[] $$4 = new int[$$3];
         this.data.storage.unpack($$4);
         swapPalette($$4, p_198178_ -> $$2.idFor(this.data.palette.valueFor(p_198178_)));
         int $$5 = pStrategy.calculateBitsForSerialization(pRegistry, $$2.getSize());
         Optional<LongStream> $$7;
         if ($$5 != 0) {
            SimpleBitStorage $$6 = new SimpleBitStorage($$5, $$3, $$4);
            $$7 = Optional.of(Arrays.stream($$6.getRaw()));
         } else {
            $$7 = Optional.empty();
         }

         var12 = new PalettedContainerRO.PackedData<>($$2.getEntries(), $$7);
      } finally {
         this.release();
      }

      return var12;
   }

   private static <T> void swapPalette(int[] pBits, IntUnaryOperator pOperator) {
      int $$2 = -1;
      int $$3 = -1;

      for (int $$4 = 0; $$4 < pBits.length; $$4++) {
         int $$5 = pBits[$$4];
         if ($$5 != $$2) {
            $$2 = $$5;
            $$3 = pOperator.applyAsInt($$5);
         }

         pBits[$$4] = $$3;
      }
   }

   @Override
   public int getSerializedSize() {
      return this.data.getSerializedSize();
   }

   @Override
   public boolean maybeHas(Predicate<T> pPredicate) {
      return this.data.palette.maybeHas(pPredicate);
   }

   public PalettedContainer<T> copy() {
      return new PalettedContainer<>(this.registry, this.strategy, this.data.copy());
   }

   @Override
   public PalettedContainer<T> recreate() {
      return new PalettedContainer<>(this.registry, this.data.palette.valueFor(0), this.strategy);
   }

   @Override
   public void count(PalettedContainer.CountConsumer<T> pCountConsumer) {
      if (this.data.palette.getSize() == 1) {
         pCountConsumer.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
      } else {
         Int2IntOpenHashMap $$1 = new Int2IntOpenHashMap();
         this.data.storage.getAll(p_238269_ -> $$1.addTo(p_238269_, 1));
         $$1.int2IntEntrySet().forEach(p_238271_ -> pCountConsumer.accept(this.data.palette.valueFor(p_238271_.getIntKey()), p_238271_.getIntValue()));
      }
   }

   static record Configuration<T>(Palette.Factory factory, int bits) {
      public PalettedContainer.Data<T> createData(IdMap<T> pRegistry, PaletteResize<T> pPaletteResize, int pSize) {
         BitStorage $$3 = (BitStorage)(this.bits == 0 ? new ZeroBitStorage(pSize) : new SimpleBitStorage(this.bits, pSize));
         Palette<T> $$4 = this.factory.create(this.bits, pRegistry, pPaletteResize, List.of());
         return new PalettedContainer.Data<>(this, $$3, $$4);
      }
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(T pState, int pCount);
   }

   static record Data<T>(PalettedContainer.Configuration<T> configuration, BitStorage storage, Palette<T> palette) {
      public void copyFrom(Palette<T> pPalette, BitStorage pBitStorage) {
         for (int $$2 = 0; $$2 < pBitStorage.getSize(); $$2++) {
            T $$3 = pPalette.valueFor(pBitStorage.get($$2));
            this.storage.set($$2, this.palette.idFor($$3));
         }
      }

      public int getSerializedSize() {
         return 1 + this.palette.getSerializedSize() + VarInt.getByteSize(this.storage.getRaw().length) + this.storage.getRaw().length * 8;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeByte(this.storage.getBits());
         this.palette.write(pBuffer);
         pBuffer.writeLongArray(this.storage.getRaw());
      }

      public PalettedContainer.Data<T> copy() {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
      }
   }

   public abstract static class Strategy {
      public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
      public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
      public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
      static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
      public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4) {
         @Override
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188157_, int p_188158_) {
            return switch (p_188158_) {
               case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188158_);
               case 1, 2, 3, 4 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);
               case 5, 6, 7, 8 -> new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, p_188158_);
               default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188157_.size()));
            };
         }
      };
      public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2) {
         @Override
         public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> p_188162_, int p_188163_) {
            return switch (p_188163_) {
               case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, p_188163_);
               case 1, 2, 3 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, p_188163_);
               default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(p_188162_.size()));
            };
         }
      };
      private final int sizeBits;

      Strategy(int pSizeBits) {
         this.sizeBits = pSizeBits;
      }

      public int size() {
         return 1 << this.sizeBits * 3;
      }

      public int getIndex(int pX, int pY, int pZ) {
         return (pY << this.sizeBits | pZ) << this.sizeBits | pX;
      }

      public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> pRegistry, int pSize);

      <A> int calculateBitsForSerialization(IdMap<A> pRegistry, int pSize) {
         int $$2 = Mth.ceillog2(pSize);
         PalettedContainer.Configuration<A> $$3 = this.getConfiguration(pRegistry, $$2);
         return $$3.factory() == GLOBAL_PALETTE_FACTORY ? $$2 : $$3.bits();
      }
   }
}
