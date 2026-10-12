package net.minecraft.core.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public final class DataComponentPatch {
   public static final DataComponentPatch EMPTY = new DataComponentPatch(Reference2ObjectMaps.emptyMap());
   public static final Codec<DataComponentPatch> CODEC = Codec.dispatchedMap(DataComponentPatch.PatchKey.CODEC, DataComponentPatch.PatchKey::valueCodec)
      .xmap(p_330428_ -> {
         if (p_330428_.isEmpty()) {
            return EMPTY;
         } else {
            Reference2ObjectMap<DataComponentType<?>, Optional<?>> $$1 = new Reference2ObjectArrayMap(p_330428_.size());

            for (Entry<DataComponentPatch.PatchKey, ?> $$2 : p_330428_.entrySet()) {
               DataComponentPatch.PatchKey $$3 = $$2.getKey();
               if ($$3.removed()) {
                  $$1.put($$3.type(), Optional.empty());
               } else {
                  $$1.put($$3.type(), Optional.of($$2.getValue()));
               }
            }

            return new DataComponentPatch($$1);
         }
      }, p_335950_ -> {
         Reference2ObjectMap<DataComponentPatch.PatchKey, Object> $$1 = new Reference2ObjectArrayMap(p_335950_.map.size());
         ObjectIterator var2 = Reference2ObjectMaps.fastIterable(p_335950_.map).iterator();

         while (var2.hasNext()) {
            Entry<DataComponentType<?>, Optional<?>> $$2 = (Entry<DataComponentType<?>, Optional<?>>)var2.next();
            DataComponentType<?> $$3 = $$2.getKey();
            if (!$$3.isTransient()) {
               Optional<?> $$4 = $$2.getValue();
               if ($$4.isPresent()) {
                  $$1.put(new DataComponentPatch.PatchKey($$3, false), $$4.get());
               } else {
                  $$1.put(new DataComponentPatch.PatchKey($$3, true), Unit.INSTANCE);
               }
            }
         }

         return $$1;
      });
   public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch>() {
      public DataComponentPatch decode(RegistryFriendlyByteBuf p_330298_) {
         int $$1 = p_330298_.readVarInt();
         int $$2 = p_330298_.readVarInt();
         if ($$1 == 0 && $$2 == 0) {
            return DataComponentPatch.EMPTY;
         } else {
            int $$3 = $$1 + $$2;
            Reference2ObjectMap<DataComponentType<?>, Optional<?>> $$4 = new Reference2ObjectArrayMap(Math.min($$3, 65536));

            for (int $$5 = 0; $$5 < $$1; $$5++) {
               DataComponentType<?> $$6 = DataComponentType.STREAM_CODEC.decode(p_330298_);
               Object $$7 = $$6.streamCodec().decode(p_330298_);
               $$4.put($$6, Optional.of($$7));
            }

            for (int $$8 = 0; $$8 < $$2; $$8++) {
               DataComponentType<?> $$9 = DataComponentType.STREAM_CODEC.decode(p_330298_);
               $$4.put($$9, Optional.empty());
            }

            return new DataComponentPatch($$4);
         }
      }

      public void encode(RegistryFriendlyByteBuf p_334360_, DataComponentPatch p_336144_) {
         if (p_336144_.isEmpty()) {
            p_334360_.writeVarInt(0);
            p_334360_.writeVarInt(0);
         } else {
            int $$2 = 0;
            int $$3 = 0;
            ObjectIterator var5 = Reference2ObjectMaps.fastIterable(p_336144_.map).iterator();

            while (var5.hasNext()) {
               it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$4 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
               if (((Optional)$$4.getValue()).isPresent()) {
                  $$2++;
               } else {
                  $$3++;
               }
            }

            p_334360_.writeVarInt($$2);
            p_334360_.writeVarInt($$3);
            var5 = Reference2ObjectMaps.fastIterable(p_336144_.map).iterator();

            while (var5.hasNext()) {
               it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$5 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
               Optional<?> $$6 = (Optional<?>)$$5.getValue();
               if ($$6.isPresent()) {
                  DataComponentType<?> $$7 = (DataComponentType<?>)$$5.getKey();
                  DataComponentType.STREAM_CODEC.encode(p_334360_, $$7);
                  encodeComponent(p_334360_, $$7, $$6.get());
               }
            }

            var5 = Reference2ObjectMaps.fastIterable(p_336144_.map).iterator();

            while (var5.hasNext()) {
               it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> $$8 = (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>>)var5.next();
               if (((Optional)$$8.getValue()).isEmpty()) {
                  DataComponentType<?> $$9 = (DataComponentType<?>)$$8.getKey();
                  DataComponentType.STREAM_CODEC.encode(p_334360_, $$9);
               }
            }
         }
      }

      private static <T> void encodeComponent(RegistryFriendlyByteBuf p_331641_, DataComponentType<T> p_335861_, Object p_335951_) {
         p_335861_.streamCodec().encode(p_331641_, (T)p_335951_);
      }
   };
   private static final String REMOVED_PREFIX = "!";
   final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

   DataComponentPatch(Reference2ObjectMap<DataComponentType<?>, Optional<?>> pMap) {
      this.map = pMap;
   }

   public static DataComponentPatch.Builder builder() {
      return new DataComponentPatch.Builder();
   }

   @Nullable
   public <T> Optional<? extends T> get(DataComponentType<? extends T> pComponent) {
      return (Optional<? extends T>)this.map.get(pComponent);
   }

   public Set<Entry<DataComponentType<?>, Optional<?>>> entrySet() {
      return this.map.entrySet();
   }

   public int size() {
      return this.map.size();
   }

   public DataComponentPatch forget(Predicate<DataComponentType<?>> pPredicate) {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         Reference2ObjectMap<DataComponentType<?>, Optional<?>> $$1 = new Reference2ObjectArrayMap(this.map);
         $$1.keySet().removeIf(pPredicate);
         return $$1.isEmpty() ? EMPTY : new DataComponentPatch($$1);
      }
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public DataComponentPatch.SplitResult split() {
      if (this.isEmpty()) {
         return DataComponentPatch.SplitResult.EMPTY;
      } else {
         DataComponentMap.Builder $$0 = DataComponentMap.builder();
         Set<DataComponentType<?>> $$1 = Sets.newIdentityHashSet();
         this.map.forEach((p_336136_, p_328765_) -> {
            if (p_328765_.isPresent()) {
               $$0.setUnchecked(p_336136_, p_328765_.get());
            } else {
               $$1.add(p_336136_);
            }
         });
         return new DataComponentPatch.SplitResult($$0.build(), $$1);
      }
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof DataComponentPatch $$1 && this.map.equals($$1.map)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.map.hashCode();
   }

   @Override
   public String toString() {
      return toString(this.map);
   }

   static String toString(Reference2ObjectMap<DataComponentType<?>, Optional<?>> pMap) {
      StringBuilder $$1 = new StringBuilder();
      $$1.append('{');
      boolean $$2 = true;
      ObjectIterator var3 = Reference2ObjectMaps.fastIterable(pMap).iterator();

      while (var3.hasNext()) {
         Entry<DataComponentType<?>, Optional<?>> $$3 = (Entry<DataComponentType<?>, Optional<?>>)var3.next();
         if ($$2) {
            $$2 = false;
         } else {
            $$1.append(", ");
         }

         Optional<?> $$4 = $$3.getValue();
         if ($$4.isPresent()) {
            $$1.append($$3.getKey());
            $$1.append("=>");
            $$1.append($$4.get());
         } else {
            $$1.append("!");
            $$1.append($$3.getKey());
         }
      }

      $$1.append('}');
      return $$1.toString();
   }

   public static class Builder {
      private final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap();

      Builder() {
      }

      public <T> DataComponentPatch.Builder set(DataComponentType<T> pComponent, T pValue) {
         this.map.put(pComponent, Optional.of(pValue));
         return this;
      }

      public <T> DataComponentPatch.Builder remove(DataComponentType<T> pComponent) {
         this.map.put(pComponent, Optional.empty());
         return this;
      }

      public <T> DataComponentPatch.Builder set(TypedDataComponent<T> pComponent) {
         return this.set(pComponent.type(), pComponent.value());
      }

      public DataComponentPatch build() {
         return this.map.isEmpty() ? DataComponentPatch.EMPTY : new DataComponentPatch(this.map);
      }
   }

   static record PatchKey(DataComponentType<?> type, boolean removed) {
      public static final Codec<DataComponentPatch.PatchKey> CODEC = Codec.STRING
         .flatXmap(
            p_330758_ -> {
               boolean $$1 = p_330758_.startsWith("!");
               if ($$1) {
                  p_330758_ = p_330758_.substring("!".length());
               }

               ResourceLocation $$2 = ResourceLocation.tryParse(p_330758_);
               DataComponentType<?> $$3 = BuiltInRegistries.DATA_COMPONENT_TYPE.get($$2);
               if ($$3 == null) {
                  return DataResult.error(() -> "No component with type: '" + $$2 + "'");
               } else {
                  return $$3.isTransient()
                     ? DataResult.error(() -> "'" + $$2 + "' is not a persistent component")
                     : DataResult.success(new DataComponentPatch.PatchKey($$3, $$1));
               }
            },
            p_329482_ -> {
               DataComponentType<?> $$1 = p_329482_.type();
               ResourceLocation $$2 = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey($$1);
               return $$2 == null
                  ? DataResult.error(() -> "Unregistered component: " + $$1)
                  : DataResult.success(p_329482_.removed() ? "!" + $$2 : $$2.toString());
            }
         );

      public Codec<?> valueCodec() {
         return this.removed ? Codec.EMPTY.codec() : this.type.codecOrThrow();
      }
   }

   public static record SplitResult(DataComponentMap added, Set<DataComponentType<?>> removed) {
      public static final DataComponentPatch.SplitResult EMPTY = new DataComponentPatch.SplitResult(DataComponentMap.EMPTY, Set.of());
   }
}
