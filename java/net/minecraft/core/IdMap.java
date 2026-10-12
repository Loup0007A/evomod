package net.minecraft.core;

import javax.annotation.Nullable;

public interface IdMap<T> extends Iterable<T> {
   int DEFAULT = -1;

   int getId(T pValue);

   @Nullable
   T byId(int pId);

   default T byIdOrThrow(int pId) {
      T $$1 = this.byId(pId);
      if ($$1 == null) {
         throw new IllegalArgumentException("No value with id " + pId);
      } else {
         return $$1;
      }
   }

   default int getIdOrThrow(T pValue) {
      int $$1 = this.getId(pValue);
      if ($$1 == -1) {
         throw new IllegalArgumentException("Can't find id for '" + pValue + "' in map " + this);
      } else {
         return $$1;
      }
   }

   int size();
}
