package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.Util;

public class ClassInstanceMultiMap<T> extends AbstractCollection<T> {
   private final Map<Class<?>, List<T>> byClass = Maps.newHashMap();
   private final Class<T> baseClass;
   private final List<T> allInstances = Lists.newArrayList();

   public ClassInstanceMultiMap(Class<T> pBaseClass) {
      this.baseClass = pBaseClass;
      this.byClass.put(pBaseClass, this.allInstances);
   }

   @Override
   public boolean add(T pValue) {
      boolean $$1 = false;

      for (Entry<Class<?>, List<T>> $$2 : this.byClass.entrySet()) {
         if ($$2.getKey().isInstance(pValue)) {
            $$1 |= $$2.getValue().add(pValue);
         }
      }

      return $$1;
   }

   @Override
   public boolean remove(Object pKey) {
      boolean $$1 = false;

      for (Entry<Class<?>, List<T>> $$2 : this.byClass.entrySet()) {
         if ($$2.getKey().isInstance(pKey)) {
            List<T> $$3 = $$2.getValue();
            $$1 |= $$3.remove(pKey);
         }
      }

      return $$1;
   }

   @Override
   public boolean contains(Object pKey) {
      return this.find(pKey.getClass()).contains(pKey);
   }

   public <S> Collection<S> find(Class<S> pType) {
      if (!this.baseClass.isAssignableFrom(pType)) {
         throw new IllegalArgumentException("Don't know how to search for " + pType);
      } else {
         List<? extends T> $$1 = this.byClass
            .computeIfAbsent(pType, p_326490_ -> this.allInstances.stream().filter(p_326490_::isInstance).collect(Util.toMutableList()));
         return (Collection<S>)Collections.unmodifiableCollection($$1);
      }
   }

   @Override
   public Iterator<T> iterator() {
      return (Iterator<T>)(this.allInstances.isEmpty() ? Collections.emptyIterator() : Iterators.unmodifiableIterator(this.allInstances.iterator()));
   }

   public List<T> getAllInstances() {
      return ImmutableList.copyOf(this.allInstances);
   }

   @Override
   public int size() {
      return this.allInstances.size();
   }
}
