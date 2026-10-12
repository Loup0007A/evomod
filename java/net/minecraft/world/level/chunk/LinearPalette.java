package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
   private final IdMap<T> registry;
   private final T[] values;
   private final PaletteResize<T> resizeHandler;
   private final int bits;
   private int size;

   private LinearPalette(IdMap<T> pRegistry, int pBits, PaletteResize<T> pResizeHandler, List<T> pValues) {
      this.registry = pRegistry;
      this.values = (T[])(new Object[1 << pBits]);
      this.bits = pBits;
      this.resizeHandler = pResizeHandler;
      Validate.isTrue(
         pValues.size() <= this.values.length, "Can't initialize LinearPalette of size %d with %d entries", new Object[]{this.values.length, pValues.size()}
      );

      for (int $$4 = 0; $$4 < pValues.size(); $$4++) {
         this.values[$$4] = pValues.get($$4);
      }

      this.size = pValues.size();
   }

   private LinearPalette(IdMap<T> pRegistry, T[] pValues, PaletteResize<T> pResizeHandler, int pBits, int pSize) {
      this.registry = pRegistry;
      this.values = pValues;
      this.resizeHandler = pResizeHandler;
      this.bits = pBits;
      this.size = pSize;
   }

   public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValues) {
      return new LinearPalette<>(pRegistry, pBits, pResizeHandler, pValues);
   }

   @Override
   public int idFor(T pState) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         if (this.values[$$1] == pState) {
            return $$1;
         }
      }

      int $$2 = this.size;
      if ($$2 < this.values.length) {
         this.values[$$2] = pState;
         this.size++;
         return $$2;
      } else {
         return this.resizeHandler.onResize(this.bits + 1, pState);
      }
   }

   @Override
   public boolean maybeHas(Predicate<T> pFilter) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         if (pFilter.test(this.values[$$1])) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int pId) {
      if (pId >= 0 && pId < this.size) {
         return this.values[pId];
      } else {
         throw new MissingPaletteEntryException(pId);
      }
   }

   @Override
   public void read(FriendlyByteBuf pBuffer) {
      this.size = pBuffer.readVarInt();

      for (int $$1 = 0; $$1 < this.size; $$1++) {
         this.values[$$1] = this.registry.byIdOrThrow(pBuffer.readVarInt());
      }
   }

   @Override
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.size);

      for (int $$1 = 0; $$1 < this.size; $$1++) {
         pBuffer.writeVarInt(this.registry.getId(this.values[$$1]));
      }
   }

   @Override
   public int getSerializedSize() {
      int $$0 = VarInt.getByteSize(this.getSize());

      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         $$0 += VarInt.getByteSize(this.registry.getId(this.values[$$1]));
      }

      return $$0;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public Palette<T> copy() {
      return new LinearPalette<>(this.registry, (T[])((Object[])this.values.clone()), this.resizeHandler, this.bits, this.size);
   }
}
