package net.minecraft.util;

import java.util.Locale;
import java.util.function.Consumer;

public class StaticCache2D<T> {
   private final int minX;
   private final int minZ;
   private final int sizeX;
   private final int sizeZ;
   private final Object[] cache;

   public static <T> StaticCache2D<T> create(int pCenterX, int pCenterZ, int pSize, StaticCache2D.Initializer<T> pInitializer) {
      int $$4 = pCenterX - pSize;
      int $$5 = pCenterZ - pSize;
      int $$6 = 2 * pSize + 1;
      return new StaticCache2D<>($$4, $$5, $$6, $$6, pInitializer);
   }

   private StaticCache2D(int pMinX, int pMinZ, int pSizeX, int pSizeZ, StaticCache2D.Initializer<T> pInitializer) {
      this.minX = pMinX;
      this.minZ = pMinZ;
      this.sizeX = pSizeX;
      this.sizeZ = pSizeZ;
      this.cache = new Object[this.sizeX * this.sizeZ];

      for (int $$5 = pMinX; $$5 < pMinX + pSizeX; $$5++) {
         for (int $$6 = pMinZ; $$6 < pMinZ + pSizeZ; $$6++) {
            this.cache[this.getIndex($$5, $$6)] = pInitializer.get($$5, $$6);
         }
      }
   }

   public void forEach(Consumer<T> pAction) {
      for (Object $$1 : this.cache) {
         pAction.accept((T)$$1);
      }
   }

   public T get(int pX, int pZ) {
      if (!this.contains(pX, pZ)) {
         throw new IllegalArgumentException("Requested out of range value (" + pX + "," + pZ + ") from " + this);
      } else {
         return (T)this.cache[this.getIndex(pX, pZ)];
      }
   }

   public boolean contains(int pX, int pZ) {
      int $$2 = pX - this.minX;
      int $$3 = pZ - this.minZ;
      return $$2 >= 0 && $$2 < this.sizeX && $$3 >= 0 && $$3 < this.sizeZ;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "StaticCache2D[%d, %d, %d, %d]", this.minX, this.minZ, this.minX + this.sizeX, this.minZ + this.sizeZ);
   }

   private int getIndex(int pX, int pZ) {
      int $$2 = pX - this.minX;
      int $$3 = pZ - this.minZ;
      return $$2 * this.sizeZ + $$3;
   }

   @FunctionalInterface
   public interface Initializer<T> {
      T get(int pX, int pZ);
   }
}
