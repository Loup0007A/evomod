package net.minecraft.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;

public class LayeredRegistryAccess<T> {
   private final List<T> keys;
   private final List<RegistryAccess.Frozen> values;
   private final RegistryAccess.Frozen composite;

   public LayeredRegistryAccess(List<T> pKeys) {
      this(pKeys, Util.make(() -> {
         RegistryAccess.Frozen[] $$1 = new RegistryAccess.Frozen[pKeys.size()];
         Arrays.fill($$1, RegistryAccess.EMPTY);
         return Arrays.asList($$1);
      }));
   }

   private LayeredRegistryAccess(List<T> pKeys, List<RegistryAccess.Frozen> pValues) {
      this.keys = List.copyOf(pKeys);
      this.values = List.copyOf(pValues);
      this.composite = new RegistryAccess.ImmutableRegistryAccess(collectRegistries(pValues.stream())).freeze();
   }

   private int getLayerIndexOrThrow(T pKey) {
      int $$1 = this.keys.indexOf(pKey);
      if ($$1 == -1) {
         throw new IllegalStateException("Can't find " + pKey + " inside " + this.keys);
      } else {
         return $$1;
      }
   }

   public RegistryAccess.Frozen getLayer(T pKey) {
      int $$1 = this.getLayerIndexOrThrow(pKey);
      return this.values.get($$1);
   }

   public RegistryAccess.Frozen getAccessForLoading(T pKey) {
      int $$1 = this.getLayerIndexOrThrow(pKey);
      return this.getCompositeAccessForLayers(0, $$1);
   }

   public RegistryAccess.Frozen getAccessFrom(T pKey) {
      int $$1 = this.getLayerIndexOrThrow(pKey);
      return this.getCompositeAccessForLayers($$1, this.values.size());
   }

   private RegistryAccess.Frozen getCompositeAccessForLayers(int pStartIndex, int pEndIndex) {
      return new RegistryAccess.ImmutableRegistryAccess(collectRegistries(this.values.subList(pStartIndex, pEndIndex).stream())).freeze();
   }

   public LayeredRegistryAccess<T> replaceFrom(T pKey, RegistryAccess.Frozen... pValues) {
      return this.replaceFrom(pKey, Arrays.asList(pValues));
   }

   public LayeredRegistryAccess<T> replaceFrom(T pKey, List<RegistryAccess.Frozen> pValues) {
      int $$2 = this.getLayerIndexOrThrow(pKey);
      if (pValues.size() > this.values.size() - $$2) {
         throw new IllegalStateException("Too many values to replace");
      } else {
         List<RegistryAccess.Frozen> $$3 = new ArrayList<>();

         for (int $$4 = 0; $$4 < $$2; $$4++) {
            $$3.add(this.values.get($$4));
         }

         $$3.addAll(pValues);

         while ($$3.size() < this.values.size()) {
            $$3.add(RegistryAccess.EMPTY);
         }

         return new LayeredRegistryAccess<>(this.keys, $$3);
      }
   }

   public RegistryAccess.Frozen compositeAccess() {
      return this.composite;
   }

   private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(Stream<? extends RegistryAccess> pAccesses) {
      Map<ResourceKey<? extends Registry<?>>, Registry<?>> $$1 = new HashMap<>();
      pAccesses.forEach(p_252003_ -> p_252003_.registries().forEach(p_325671_ -> {
            if ($$1.put(p_325671_.key(), p_325671_.value()) != null) {
               throw new IllegalStateException("Duplicated registry " + p_325671_.key());
            }
         }));
      return $$1;
   }
}
