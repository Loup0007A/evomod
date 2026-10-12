package net.minecraft.client.gui.font;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CodepointMap<T> {
   private static final int BLOCK_BITS = 8;
   private static final int BLOCK_SIZE = 256;
   private static final int IN_BLOCK_MASK = 255;
   private static final int MAX_BLOCK = 4351;
   private static final int BLOCK_COUNT = 4352;
   private final T[] empty;
   private final T[][] blockMap;
   private final IntFunction<T[]> blockConstructor;

   public CodepointMap(IntFunction<T[]> pBlockConstructor, IntFunction<T[][]> pBlockMapConstructor) {
      this.empty = (T[])((Object[])pBlockConstructor.apply(256));
      this.blockMap = (T[][])((Object[][])pBlockMapConstructor.apply(4352));
      Arrays.fill(this.blockMap, this.empty);
      this.blockConstructor = pBlockConstructor;
   }

   public void clear() {
      Arrays.fill(this.blockMap, this.empty);
   }

   @Nullable
   public T get(int pIndex) {
      int $$1 = pIndex >> 8;
      int $$2 = pIndex & 0xFF;
      return this.blockMap[$$1][$$2];
   }

   @Nullable
   public T put(int pIndex, T pValue) {
      int $$2 = pIndex >> 8;
      int $$3 = pIndex & 0xFF;
      T[] $$4 = this.blockMap[$$2];
      if ($$4 == this.empty) {
         $$4 = (T[])((Object[])this.blockConstructor.apply(256));
         this.blockMap[$$2] = $$4;
         $$4[$$3] = pValue;
         return null;
      } else {
         T $$5 = $$4[$$3];
         $$4[$$3] = pValue;
         return $$5;
      }
   }

   public T computeIfAbsent(int pIndex, IntFunction<T> pValueIfAbsentGetter) {
      int $$2 = pIndex >> 8;
      int $$3 = pIndex & 0xFF;
      T[] $$4 = this.blockMap[$$2];
      T $$5 = $$4[$$3];
      if ($$5 != null) {
         return $$5;
      } else {
         if ($$4 == this.empty) {
            $$4 = (T[])((Object[])this.blockConstructor.apply(256));
            this.blockMap[$$2] = $$4;
         }

         T $$6 = pValueIfAbsentGetter.apply(pIndex);
         $$4[$$3] = $$6;
         return $$6;
      }
   }

   @Nullable
   public T remove(int p_285488_) {
      int $$1 = p_285488_ >> 8;
      int $$2 = p_285488_ & 0xFF;
      T[] $$3 = this.blockMap[$$1];
      if ($$3 == this.empty) {
         return null;
      } else {
         T $$4 = $$3[$$2];
         $$3[$$2] = null;
         return $$4;
      }
   }

   public void forEach(CodepointMap.Output<T> pOutput) {
      for (int $$1 = 0; $$1 < this.blockMap.length; $$1++) {
         T[] $$2 = this.blockMap[$$1];
         if ($$2 != this.empty) {
            for (int $$3 = 0; $$3 < $$2.length; $$3++) {
               T $$4 = $$2[$$3];
               if ($$4 != null) {
                  int $$5 = $$1 << 8 | $$3;
                  pOutput.accept($$5, $$4);
               }
            }
         }
      }
   }

   public IntSet keySet() {
      IntOpenHashSet $$0 = new IntOpenHashSet();
      this.forEach((p_285165_, p_285389_) -> $$0.add(p_285165_));
      return $$0;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Output<T> {
      void accept(int pIndex, T pObject);
   }
}
