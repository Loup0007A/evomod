package net.minecraft.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;

public class CrudeIncrementalIntIdentityHashBiMap<K> implements IdMap<K> {
   private static final int NOT_FOUND = -1;
   private static final Object EMPTY_SLOT = null;
   private static final float LOADFACTOR = 0.8F;
   private K[] keys;
   private int[] values;
   private K[] byId;
   private int nextId;
   private int size;

   private CrudeIncrementalIntIdentityHashBiMap(int pSize) {
      this.keys = (K[])(new Object[pSize]);
      this.values = new int[pSize];
      this.byId = (K[])(new Object[pSize]);
   }

   private CrudeIncrementalIntIdentityHashBiMap(K[] pKeys, int[] pValues, K[] pById, int pNextId, int pSize) {
      this.keys = pKeys;
      this.values = pValues;
      this.byId = pById;
      this.nextId = pNextId;
      this.size = pSize;
   }

   public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(int pSize) {
      return new CrudeIncrementalIntIdentityHashBiMap((int)((float)pSize / 0.8F));
   }

   @Override
   public int getId(@Nullable K pValue) {
      return this.getValue(this.indexOf(pValue, this.hash(pValue)));
   }

   @Nullable
   @Override
   public K byId(int pValue) {
      return pValue >= 0 && pValue < this.byId.length ? this.byId[pValue] : null;
   }

   private int getValue(int pKey) {
      return pKey == -1 ? -1 : this.values[pKey];
   }

   public boolean contains(K pValue) {
      return this.getId(pValue) != -1;
   }

   public boolean contains(int pValue) {
      return this.byId(pValue) != null;
   }

   public int add(K pObject) {
      int $$1 = this.nextId();
      this.addMapping(pObject, $$1);
      return $$1;
   }

   private int nextId() {
      while (this.nextId < this.byId.length && this.byId[this.nextId] != null) {
         this.nextId++;
      }

      return this.nextId;
   }

   private void grow(int pCapacity) {
      K[] $$1 = this.keys;
      int[] $$2 = this.values;
      CrudeIncrementalIntIdentityHashBiMap<K> $$3 = new CrudeIncrementalIntIdentityHashBiMap<>(pCapacity);

      for (int $$4 = 0; $$4 < $$1.length; $$4++) {
         if ($$1[$$4] != null) {
            $$3.addMapping($$1[$$4], $$2[$$4]);
         }
      }

      this.keys = $$3.keys;
      this.values = $$3.values;
      this.byId = $$3.byId;
      this.nextId = $$3.nextId;
      this.size = $$3.size;
   }

   public void addMapping(K pObject, int pIntKey) {
      int $$2 = Math.max(pIntKey, this.size + 1);
      if ((float)$$2 >= (float)this.keys.length * 0.8F) {
         int $$3 = this.keys.length << 1;

         while ($$3 < pIntKey) {
            $$3 <<= 1;
         }

         this.grow($$3);
      }

      int $$4 = this.findEmpty(this.hash(pObject));
      this.keys[$$4] = pObject;
      this.values[$$4] = pIntKey;
      this.byId[pIntKey] = pObject;
      this.size++;
      if (pIntKey == this.nextId) {
         this.nextId++;
      }
   }

   private int hash(@Nullable K pObject) {
      return (Mth.murmurHash3Mixer(System.identityHashCode(pObject)) & 2147483647) % this.keys.length;
   }

   private int indexOf(@Nullable K pObject, int pStartIndex) {
      for (int $$2 = pStartIndex; $$2 < this.keys.length; $$2++) {
         if (this.keys[$$2] == pObject) {
            return $$2;
         }

         if (this.keys[$$2] == EMPTY_SLOT) {
            return -1;
         }
      }

      for (int $$3 = 0; $$3 < pStartIndex; $$3++) {
         if (this.keys[$$3] == pObject) {
            return $$3;
         }

         if (this.keys[$$3] == EMPTY_SLOT) {
            return -1;
         }
      }

      return -1;
   }

   private int findEmpty(int pStartIndex) {
      for (int $$1 = pStartIndex; $$1 < this.keys.length; $$1++) {
         if (this.keys[$$1] == EMPTY_SLOT) {
            return $$1;
         }
      }

      for (int $$2 = 0; $$2 < pStartIndex; $$2++) {
         if (this.keys[$$2] == EMPTY_SLOT) {
            return $$2;
         }
      }

      throw new RuntimeException("Overflowed :(");
   }

   @Override
   public Iterator<K> iterator() {
      return Iterators.filter(Iterators.forArray(this.byId), Predicates.notNull());
   }

   public void clear() {
      Arrays.fill(this.keys, null);
      Arrays.fill(this.byId, null);
      this.nextId = 0;
      this.size = 0;
   }

   @Override
   public int size() {
      return this.size;
   }

   public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
      return new CrudeIncrementalIntIdentityHashBiMap<>(
         (K[])((Object[])this.keys.clone()), (int[])this.values.clone(), (K[])((Object[])this.byId.clone()), this.nextId, this.size
      );
   }
}
