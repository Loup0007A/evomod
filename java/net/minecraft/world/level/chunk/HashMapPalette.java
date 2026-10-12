package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
   private final IdMap<T> registry;
   private final CrudeIncrementalIntIdentityHashBiMap<T> values;
   private final PaletteResize<T> resizeHandler;
   private final int bits;

   public HashMapPalette(IdMap<T> pRegistry, int pBits, PaletteResize<T> pResizeHandler, List<T> pValues) {
      this(pRegistry, pBits, pResizeHandler);
      pValues.forEach(this.values::add);
   }

   public HashMapPalette(IdMap<T> pRegistry, int pBits, PaletteResize<T> pResizeHandler) {
      this(pRegistry, pBits, pResizeHandler, CrudeIncrementalIntIdentityHashBiMap.create(1 << pBits));
   }

   private HashMapPalette(IdMap<T> pRegistry, int pBits, PaletteResize<T> pResizeHandler, CrudeIncrementalIntIdentityHashBiMap<T> pValues) {
      this.registry = pRegistry;
      this.bits = pBits;
      this.resizeHandler = pResizeHandler;
      this.values = pValues;
   }

   public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValues) {
      return new HashMapPalette<>(pRegistry, pBits, pResizeHandler, pValues);
   }

   @Override
   public int idFor(T pState) {
      int $$1 = this.values.getId(pState);
      if ($$1 == -1) {
         $$1 = this.values.add(pState);
         if ($$1 >= 1 << this.bits) {
            $$1 = this.resizeHandler.onResize(this.bits + 1, pState);
         }
      }

      return $$1;
   }

   @Override
   public boolean maybeHas(Predicate<T> pFilter) {
      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         if (pFilter.test(this.values.byId($$1))) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int pId) {
      T $$1 = this.values.byId(pId);
      if ($$1 == null) {
         throw new MissingPaletteEntryException(pId);
      } else {
         return $$1;
      }
   }

   @Override
   public void read(FriendlyByteBuf pBuffer) {
      this.values.clear();
      int $$1 = pBuffer.readVarInt();

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         this.values.add(this.registry.byIdOrThrow(pBuffer.readVarInt()));
      }
   }

   @Override
   public void write(FriendlyByteBuf pBuffer) {
      int $$1 = this.getSize();
      pBuffer.writeVarInt($$1);

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         pBuffer.writeVarInt(this.registry.getId(this.values.byId($$2)));
      }
   }

   @Override
   public int getSerializedSize() {
      int $$0 = VarInt.getByteSize(this.getSize());

      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         $$0 += VarInt.getByteSize(this.registry.getId(this.values.byId($$1)));
      }

      return $$0;
   }

   public List<T> getEntries() {
      ArrayList<T> $$0 = new ArrayList<>();
      this.values.iterator().forEachRemaining($$0::add);
      return $$0;
   }

   @Override
   public int getSize() {
      return this.values.size();
   }

   @Override
   public Palette<T> copy() {
      return new HashMapPalette<>(this.registry, this.bits, this.resizeHandler, this.values.copy());
   }
}
