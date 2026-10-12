package net.minecraft.core.component;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class PatchedDataComponentMap implements DataComponentMap {
   private final DataComponentMap prototype;
   private Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch;
   private boolean copyOnWrite;

   public PatchedDataComponentMap(DataComponentMap pPrototype) {
      this(pPrototype, Reference2ObjectMaps.emptyMap(), true);
   }

   private PatchedDataComponentMap(DataComponentMap pPrototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> pPatch, boolean pCopyOnWtite) {
      this.prototype = pPrototype;
      this.patch = pPatch;
      this.copyOnWrite = pCopyOnWtite;
   }

   public static PatchedDataComponentMap fromPatch(DataComponentMap pPrototype, DataComponentPatch pPatch) {
      if (isPatchSanitized(pPrototype, pPatch.map)) {
         return new PatchedDataComponentMap(pPrototype, pPatch.map, true);
      } else {
         PatchedDataComponentMap $$2 = new PatchedDataComponentMap(pPrototype);
         $$2.applyPatch(pPatch);
         return $$2;
      }
   }

   private static boolean isPatchSanitized(DataComponentMap pPrototype, Reference2ObjectMap<DataComponentType<?>, Optional<?>> pMap) {
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(pMap).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> $$2 = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         Object $$3 = pPrototype.get($$2.getKey());
         Optional<?> $$4 = $$2.getValue();
         if ($$4.isPresent() && $$4.get().equals($$3)) {
            return false;
         }

         if ($$4.isEmpty() && $$3 == null) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   @Override
   public <T> T get(DataComponentType<? extends T> pComponent) {
      Optional<? extends T> $$1 = (Optional<? extends T>)this.patch.get(pComponent);
      return (T)($$1 != null ? $$1.orElse(null) : this.prototype.get(pComponent));
   }

   @Nullable
   public <T> T set(DataComponentType<? super T> pComponent, @Nullable T pValue) {
      this.ensureMapOwnership();
      T $$2 = this.prototype.get((DataComponentType<? extends T>)pComponent);
      Optional<T> $$3;
      if (Objects.equals(pValue, $$2)) {
         $$3 = (Optional<T>)this.patch.remove(pComponent);
      } else {
         $$3 = (Optional<T>)this.patch.put(pComponent, Optional.ofNullable(pValue));
      }

      return $$3 != null ? $$3.orElse($$2) : $$2;
   }

   @Nullable
   public <T> T remove(DataComponentType<? extends T> pComponent) {
      this.ensureMapOwnership();
      T $$1 = this.prototype.get(pComponent);
      Optional<? extends T> $$2;
      if ($$1 != null) {
         $$2 = (Optional<? extends T>)this.patch.put(pComponent, Optional.empty());
      } else {
         $$2 = (Optional<? extends T>)this.patch.remove(pComponent);
      }

      return (T)($$2 != null ? $$2.orElse(null) : $$1);
   }

   public void applyPatch(DataComponentPatch pPatch) {
      this.ensureMapOwnership();
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(pPatch.map).iterator();

      while (var2.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> $$1 = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
         this.applyPatch($$1.getKey(), $$1.getValue());
      }
   }

   private void applyPatch(DataComponentType<?> pComponent, Optional<?> pValue) {
      Object $$2 = this.prototype.get(pComponent);
      if (pValue.isPresent()) {
         if (pValue.get().equals($$2)) {
            this.patch.remove(pComponent);
         } else {
            this.patch.put(pComponent, pValue);
         }
      } else if ($$2 != null) {
         this.patch.put(pComponent, Optional.empty());
      } else {
         this.patch.remove(pComponent);
      }
   }

   public void restorePatch(DataComponentPatch pPatch) {
      this.ensureMapOwnership();
      this.patch.clear();
      this.patch.putAll(pPatch.map);
   }

   public void setAll(DataComponentMap pMap) {
      for (TypedDataComponent<?> $$1 : pMap) {
         $$1.applyTo(this);
      }
   }

   private void ensureMapOwnership() {
      if (this.copyOnWrite) {
         this.patch = new Reference2ObjectArrayMap(this.patch);
         this.copyOnWrite = false;
      }
   }

   @Override
   public Set<DataComponentType<?>> keySet() {
      if (this.patch.isEmpty()) {
         return this.prototype.keySet();
      } else {
         Set<DataComponentType<?>> $$0 = new ReferenceArraySet(this.prototype.keySet());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$1 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
            Optional<?> $$2 = (Optional<?>)$$1.getValue();
            if ($$2.isPresent()) {
               $$0.add((DataComponentType<?>)$$1.getKey());
            } else {
               $$0.remove($$1.getKey());
            }
         }

         return $$0;
      }
   }

   @Override
   public Iterator<TypedDataComponent<?>> iterator() {
      if (this.patch.isEmpty()) {
         return this.prototype.iterator();
      } else {
         List<TypedDataComponent<?>> $$0 = new ArrayList<>(this.patch.size() + this.prototype.size());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$1 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
            if (((Optional)$$1.getValue()).isPresent()) {
               $$0.add(TypedDataComponent.createUnchecked((DataComponentType)$$1.getKey(), ((Optional)$$1.getValue()).get()));
            }
         }

         for (TypedDataComponent<?> $$2 : this.prototype) {
            if (!this.patch.containsKey($$2.type())) {
               $$0.add($$2);
            }
         }

         return $$0.iterator();
      }
   }

   @Override
   public int size() {
      int $$0 = this.prototype.size();
      ObjectIterator var2 = Reference2ObjectMaps.fastIterable(this.patch).iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$1 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var2.next();
         boolean $$2 = ((Optional)$$1.getValue()).isPresent();
         boolean $$3 = this.prototype.has((DataComponentType<?>)$$1.getKey());
         if ($$2 != $$3) {
            $$0 += $$2 ? 1 : -1;
         }
      }

      return $$0;
   }

   public DataComponentPatch asPatch() {
      if (this.patch.isEmpty()) {
         return DataComponentPatch.EMPTY;
      } else {
         this.copyOnWrite = true;
         return new DataComponentPatch(this.patch);
      }
   }

   public PatchedDataComponentMap copy() {
      this.copyOnWrite = true;
      return new PatchedDataComponentMap(this.prototype, this.patch, true);
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof PatchedDataComponentMap $$1 && this.prototype.equals($$1.prototype) && this.patch.equals($$1.patch)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.prototype.hashCode() + this.patch.hashCode() * 31;
   }

   @Override
   public String toString() {
      return "{" + this.stream().map(TypedDataComponent::toString).collect(Collectors.joining(", ")) + "}";
   }
}
