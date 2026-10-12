package net.minecraft.world.level.block.state;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class StateHolder<O, S> {
   public static final String NAME_TAG = "Name";
   public static final String PROPERTIES_TAG = "Properties";
   private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>() {
      public String apply(@Nullable Entry<Property<?>, Comparable<?>> p_61155_) {
         if (p_61155_ == null) {
            return "<NULL>";
         } else {
            Property<?> $$1 = p_61155_.getKey();
            return $$1.getName() + "=" + this.getName($$1, p_61155_.getValue());
         }
      }

      private <T extends Comparable<T>> String getName(Property<T> p_61152_, Comparable<?> p_61153_) {
         return p_61152_.getName((T)p_61153_);
      }
   };
   protected final O owner;
   private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;
   private Table<Property<?>, Comparable<?>, S> neighbours;
   protected final MapCodec<S> propertiesCodec;

   protected StateHolder(O pOwner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> pValues, MapCodec<S> pPropertiesCodec) {
      this.owner = pOwner;
      this.values = pValues;
      this.propertiesCodec = pPropertiesCodec;
   }

   public <T extends Comparable<T>> S cycle(Property<T> pProperty) {
      return this.setValue(pProperty, findNextInCollection(pProperty.getPossibleValues(), this.getValue(pProperty)));
   }

   protected static <T> T findNextInCollection(Collection<T> pCollection, T pValue) {
      Iterator<T> $$2 = pCollection.iterator();

      while ($$2.hasNext()) {
         if ($$2.next().equals(pValue)) {
            if ($$2.hasNext()) {
               return $$2.next();
            }

            return pCollection.iterator().next();
         }
      }

      return $$2.next();
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();
      $$0.append(this.owner);
      if (!this.getValues().isEmpty()) {
         $$0.append('[');
         $$0.append(this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
         $$0.append(']');
      }

      return $$0.toString();
   }

   public Collection<Property<?>> getProperties() {
      return Collections.unmodifiableCollection(this.values.keySet());
   }

   public <T extends Comparable<T>> boolean hasProperty(Property<T> pProperty) {
      return this.values.containsKey(pProperty);
   }

   public <T extends Comparable<T>> T getValue(Property<T> pProperty) {
      Comparable<?> $$1 = (Comparable<?>)this.values.get(pProperty);
      if ($$1 == null) {
         throw new IllegalArgumentException("Cannot get property " + pProperty + " as it does not exist in " + this.owner);
      } else {
         return pProperty.getValueClass().cast($$1);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> pProperty) {
      Comparable<?> $$1 = (Comparable<?>)this.values.get(pProperty);
      return $$1 == null ? Optional.empty() : Optional.of(pProperty.getValueClass().cast($$1));
   }

   public <T extends Comparable<T>, V extends T> S setValue(Property<T> pProperty, V pValue) {
      Comparable<?> $$2 = (Comparable<?>)this.values.get(pProperty);
      if ($$2 == null) {
         throw new IllegalArgumentException("Cannot set property " + pProperty + " as it does not exist in " + this.owner);
      } else if ($$2.equals(pValue)) {
         return (S)this;
      } else {
         S $$3 = (S)this.neighbours.get(pProperty, pValue);
         if ($$3 == null) {
            throw new IllegalArgumentException("Cannot set property " + pProperty + " to " + pValue + " on " + this.owner + ", it is not an allowed value");
         } else {
            return $$3;
         }
      }
   }

   public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> pProperty, V pValue) {
      Comparable<?> $$2 = (Comparable<?>)this.values.get(pProperty);
      if ($$2 != null && !$$2.equals(pValue)) {
         S $$3 = (S)this.neighbours.get(pProperty, pValue);
         if ($$3 == null) {
            throw new IllegalArgumentException("Cannot set property " + pProperty + " to " + pValue + " on " + this.owner + ", it is not an allowed value");
         } else {
            return $$3;
         }
      } else {
         return (S)this;
      }
   }

   public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> pPossibleStateMap) {
      if (this.neighbours != null) {
         throw new IllegalStateException();
      } else {
         Table<Property<?>, Comparable<?>, S> $$1 = HashBasedTable.create();
         ObjectIterator var3 = this.values.entrySet().iterator();

         while (var3.hasNext()) {
            Entry<Property<?>, Comparable<?>> $$2 = (Entry<Property<?>, Comparable<?>>)var3.next();
            Property<?> $$3 = $$2.getKey();

            for (Comparable<?> $$4 : $$3.getPossibleValues()) {
               if (!$$4.equals($$2.getValue())) {
                  $$1.put($$3, $$4, pPossibleStateMap.get(this.makeNeighbourValues($$3, $$4)));
               }
            }
         }

         this.neighbours = (Table<Property<?>, Comparable<?>, S>)($$1.isEmpty() ? $$1 : ArrayTable.create($$1));
      }
   }

   private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> pProperty, Comparable<?> pValue) {
      Map<Property<?>, Comparable<?>> $$2 = new Reference2ObjectArrayMap(this.values);
      $$2.put(pProperty, pValue);
      return $$2;
   }

   public Map<Property<?>, Comparable<?>> getValues() {
      return this.values;
   }

   protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> pPropertyMap, Function<O, S> pHolderFunction) {
      return pPropertyMap.dispatch(
         "Name",
         p_61121_ -> p_61121_.owner,
         p_327407_ -> {
            S $$2 = pHolderFunction.apply((O)p_327407_);
            return $$2.getValues().isEmpty()
               ? MapCodec.unit($$2)
               : $$2.propertiesCodec.codec().lenientOptionalFieldOf("Properties").xmap(p_187544_ -> p_187544_.orElse($$2), Optional::of);
         }
      );
   }
}
