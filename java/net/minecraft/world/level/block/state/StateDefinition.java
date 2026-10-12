package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public class StateDefinition<O, S extends StateHolder<O, S>> {
   static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
   private final O owner;
   private final ImmutableSortedMap<String, Property<?>> propertiesByName;
   private final ImmutableList<S> states;

   protected StateDefinition(
      Function<O, S> pStateValueFunction, O pOwner, StateDefinition.Factory<O, S> pValueFunction, Map<String, Property<?>> pPropertiesByName
   ) {
      this.owner = pOwner;
      this.propertiesByName = ImmutableSortedMap.copyOf(pPropertiesByName);
      Supplier<S> $$4 = () -> pStateValueFunction.apply(pOwner);
      MapCodec<S> $$5 = MapCodec.of(Encoder.empty(), Decoder.unit($$4));
      UnmodifiableIterator $$7 = this.propertiesByName.entrySet().iterator();

      while ($$7.hasNext()) {
         Entry<String, Property<?>> $$6 = (Entry<String, Property<?>>)$$7.next();
         $$5 = appendPropertyCodec($$5, $$4, $$6.getKey(), $$6.getValue());
      }

      MapCodec<S> $$7x = $$5;
      Map<Map<Property<?>, Comparable<?>>, S> $$8 = Maps.newLinkedHashMap();
      List<S> $$9 = Lists.newArrayList();
      Stream<List<Pair<Property<?>, Comparable<?>>>> $$10 = Stream.of(Collections.emptyList());
      UnmodifiableIterator var11 = this.propertiesByName.values().iterator();

      while (var11.hasNext()) {
         Property<?> $$11 = (Property<?>)var11.next();
         $$10 = $$10.flatMap(p_61072_ -> $$11.getPossibleValues().stream().map(p_155961_ -> {
               List<Pair<Property<?>, Comparable<?>>> $$3 = Lists.newArrayList(p_61072_);
               $$3.add(Pair.of($$11, p_155961_));
               return $$3;
            }));
      }

      $$10.forEach(p_327405_ -> {
         Reference2ObjectArrayMap<Property<?>, Comparable<?>> $$6 = new Reference2ObjectArrayMap(p_327405_.size());

         for (Pair<Property<?>, Comparable<?>> $$7xx : p_327405_) {
            $$6.put((Property)$$7xx.getFirst(), (Comparable)$$7xx.getSecond());
         }

         S $$8x = pValueFunction.create(pOwner, $$6, $$7);
         $$8.put($$6, $$8x);
         $$9.add($$8x);
      });

      for (S $$12 : $$9) {
         $$12.populateNeighbours($$8);
      }

      this.states = ImmutableList.copyOf($$9);
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(
      MapCodec<S> pPropertyCodec, Supplier<S> pHolderSupplier, String pValue, Property<T> pProperty
   ) {
      return Codec.mapPair(pPropertyCodec, pProperty.valueCodec().fieldOf(pValue).orElseGet(p_187541_ -> {
         }, () -> pProperty.value(pHolderSupplier.get())))
         .xmap(
            p_187536_ -> (StateHolder)((StateHolder)p_187536_.getFirst()).setValue(pProperty, ((Property.Value)p_187536_.getSecond()).value()),
            p_187533_ -> Pair.of(p_187533_, pProperty.value(p_187533_))
         );
   }

   public ImmutableList<S> getPossibleStates() {
      return this.states;
   }

   public S any() {
      return (S)this.states.get(0);
   }

   public O getOwner() {
      return this.owner;
   }

   public Collection<Property<?>> getProperties() {
      return this.propertiesByName.values();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
         .add("block", this.owner)
         .add("properties", this.propertiesByName.values().stream().map(Property::getName).collect(Collectors.toList()))
         .toString();
   }

   @Nullable
   public Property<?> getProperty(String pPropertyName) {
      return (Property<?>)this.propertiesByName.get(pPropertyName);
   }

   public static class Builder<O, S extends StateHolder<O, S>> {
      private final O owner;
      private final Map<String, Property<?>> properties = Maps.newHashMap();

      public Builder(O pOwner) {
         this.owner = pOwner;
      }

      public StateDefinition.Builder<O, S> add(Property<?>... pProperties) {
         for (Property<?> $$1 : pProperties) {
            this.validateProperty($$1);
            this.properties.put($$1.getName(), $$1);
         }

         return this;
      }

      private <T extends Comparable<T>> void validateProperty(Property<T> pProperty) {
         String $$1 = pProperty.getName();
         if (!StateDefinition.NAME_PATTERN.matcher($$1).matches()) {
            throw new IllegalArgumentException(this.owner + " has invalidly named property: " + $$1);
         } else {
            Collection<T> $$2 = pProperty.getPossibleValues();
            if ($$2.size() <= 1) {
               throw new IllegalArgumentException(this.owner + " attempted use property " + $$1 + " with <= 1 possible values");
            } else {
               for (T $$3 : $$2) {
                  String $$4 = pProperty.getName($$3);
                  if (!StateDefinition.NAME_PATTERN.matcher($$4).matches()) {
                     throw new IllegalArgumentException(this.owner + " has property: " + $$1 + " with invalidly named value: " + $$4);
                  }
               }

               if (this.properties.containsKey($$1)) {
                  throw new IllegalArgumentException(this.owner + " has duplicate property: " + $$1);
               }
            }
         }
      }

      public StateDefinition<O, S> create(Function<O, S> pStateValueFunction, StateDefinition.Factory<O, S> pStateFunction) {
         return new StateDefinition<>(pStateValueFunction, this.owner, pStateFunction, this.properties);
      }
   }

   public interface Factory<O, S> {
      S create(O pOwner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> pValues, MapCodec<S> pPropertiesCodec);
   }
}
