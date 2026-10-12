package net.minecraftforge.registries;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

class NamespacedWrapper<T> extends MappedRegistry<T> implements ILockableRegistry {
   static final Logger LOGGER = LogUtils.getLogger();
   private final ForgeRegistry<T> delegate;
   @Nullable
   private final Function<T, Holder.Reference<T>> intrusiveHolderCallback;
   private final Multimap<TagKey<T>, Supplier<T>> optionalTags = Multimaps.newSetMultimap(new IdentityHashMap(), HashSet::new);
   boolean locked = false;
   Lifecycle registryLifecycle = Lifecycle.stable();
   private boolean frozen = false;
   private List<Holder.Reference<T>> holdersSorted;
   private final ObjectList<Holder.Reference<T>> holdersById = new ObjectArrayList(256);
   private final Map<ResourceLocation, Holder.Reference<T>> holdersByName = new HashMap<>();
   private final Map<T, Holder.Reference<T>> holders = new IdentityHashMap<>();
   private final RegistryManager stage;
   private volatile Map<TagKey<T>, HolderSet.Named<T>> tags = new IdentityHashMap<>();
   private final Map<ResourceKey<T>, RegistrationInfo> registrationInfos = new IdentityHashMap<>();

   NamespacedWrapper(ForgeRegistry<T> fowner, Function<T, Holder.Reference<T>> intrusiveHolderCallback, RegistryManager stage) {
      super(fowner.getRegistryKey(), Lifecycle.stable(), intrusiveHolderCallback != null);
      this.delegate = fowner;
      this.intrusiveHolderCallback = intrusiveHolderCallback;
      this.stage = stage;
   }

   @Override
   public Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo info) {
      if (this.locked) {
         throw new IllegalStateException("Can not register to a locked registry. Modder should use Forge Register methods.");
      } else {
         Objects.requireNonNull(value);
         this.markKnown();
         this.registrationInfos.put(key, info);
         this.registryLifecycle = this.registryLifecycle.add(info.lifecycle());
         this.delegate.add(-1, key.location(), value);
         return this.getHolder(key, value);
      }
   }

   @Nullable
   @Override
   public T get(@Nullable ResourceLocation name) {
      return this.delegate.getRaw(name);
   }

   @Override
   public Optional<T> getOptional(@Nullable ResourceLocation name) {
      return Optional.ofNullable(this.delegate.getRaw(name));
   }

   @Nullable
   @Override
   public T get(@Nullable ResourceKey<T> name) {
      return name == null ? null : this.delegate.getRaw(name.location());
   }

   @Nullable
   @Override
   public ResourceLocation getKey(T value) {
      return this.delegate.getKey(value);
   }

   @Override
   public Optional<ResourceKey<T>> getResourceKey(T value) {
      return this.delegate.getResourceKey(value);
   }

   @Override
   public boolean containsKey(ResourceLocation key) {
      return this.delegate.containsKey(key);
   }

   @Override
   public boolean containsKey(ResourceKey<T> key) {
      return this.delegate.getRegistryName().equals(key.registry()) && this.containsKey(key.location());
   }

   @Override
   public int getId(@Nullable T value) {
      return this.delegate.getID(value);
   }

   @Nullable
   @Override
   public T byId(int id) {
      return this.delegate.getValue(id);
   }

   @Override
   public Lifecycle registryLifecycle() {
      return this.registryLifecycle;
   }

   @Override
   public Iterator<T> iterator() {
      return this.delegate.iterator();
   }

   @Override
   public Set<ResourceLocation> keySet() {
      return this.delegate.getKeys();
   }

   @Override
   public Set<ResourceKey<T>> registryKeySet() {
      return this.delegate.getResourceKeys();
   }

   @Override
   public Set<Entry<ResourceKey<T>, T>> entrySet() {
      return this.delegate.getEntries();
   }

   @Override
   public boolean isEmpty() {
      return this.delegate.isEmpty();
   }

   @Override
   public int size() {
      return this.delegate.size();
   }

   @Deprecated
   @Override
   public void lock() {
      this.locked = true;
   }

   @Override
   public Optional<Holder.Reference<T>> getHolder(int id) {
      return id >= 0 && id < this.holdersById.size() ? Optional.ofNullable((Holder.Reference<T>)this.holdersById.get(id)) : Optional.empty();
   }

   @Override
   public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> key) {
      return Optional.ofNullable(this.holdersByName.get(key.location()));
   }

   @NotNull
   @Override
   public Holder<T> wrapAsHolder(@NotNull T value) {
      Holder<T> holder = this.holders.get(value);
      return holder == null ? Holder.direct(value) : holder;
   }

   @Override
   public Optional<RegistrationInfo> registrationInfo(ResourceKey<T> p_331530_) {
      return Optional.ofNullable(this.registrationInfos.get(p_331530_));
   }

   @Override
   public Optional<Holder.Reference<T>> getHolder(ResourceLocation location) {
      return Optional.ofNullable(this.holdersByName.get(location));
   }

   Optional<Holder<T>> getHolder(T value) {
      return Optional.ofNullable(this.holders.get(value));
   }

   @Override
   public HolderGetter<T> createRegistrationLookup() {
      this.validateWrite();
      return new HolderGetter<T>() {
         @Override
         public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            return Optional.of(this.getOrThrow(key));
         }

         @Override
         public Holder.Reference<T> getOrThrow(ResourceKey<T> key) {
            return NamespacedWrapper.this.getOrCreateHolderOrThrow(key);
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
            return Optional.of(this.getOrThrow(key));
         }

         @Override
         public HolderSet.Named<T> getOrThrow(TagKey<T> key) {
            return NamespacedWrapper.this.getOrCreateTag(key);
         }
      };
   }

   @Override
   void validateWrite() {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen");
      }
   }

   @Override
   void validateWrite(ResourceKey<T> key) {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen (trying to add key " + key + ")");
      }
   }

   @Override
   protected Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> key) {
      return this.holdersByName.computeIfAbsent(key.location(), k -> {
         if (this.isIntrusive()) {
            throw new IllegalStateException("This registry can't create new holders without value");
         } else {
            this.validateWrite(key);
            return Holder.Reference.createStandAlone(this.holderOwner(), key);
         }
      });
   }

   @Override
   public Optional<Holder.Reference<T>> getRandom(RandomSource rand) {
      return Util.getRandomSafe(this.getSortedHolders(), rand);
   }

   @Override
   public Stream<Holder.Reference<T>> holders() {
      return this.getSortedHolders().stream();
   }

   @Override
   public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
      return this.tags.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
   }

   @Override
   public HolderSet.Named<T> getOrCreateTag(TagKey<T> name) {
      HolderSet.Named<T> named = this.tags.get(name);
      if (named == null) {
         named = this.createTag(name);
         Map<TagKey<T>, HolderSet.Named<T>> map = new IdentityHashMap<>(this.tags);
         map.put(name, named);
         this.tags = map;
      }

      return named;
   }

   void addOptionalTag(TagKey<T> name, @NotNull Set<? extends Supplier<T>> defaults) {
      this.optionalTags.putAll(name, defaults);
   }

   @Override
   public Stream<TagKey<T>> getTagNames() {
      return this.tags.keySet().stream();
   }

   @Override
   public Registry<T> freeze() {
      this.frozen = true;
      List<ResourceLocation> unregistered = this.holdersByName.entrySet().stream().filter(e -> !e.getValue().isBound()).map(Entry::getKey).sorted().toList();
      if (!unregistered.isEmpty()) {
         throw new IllegalStateException(
            "Unbound values in registry " + this.key() + ": " + unregistered.stream().map(ResourceLocation::toString).collect(Collectors.joining(", \n\t"))
         );
      } else if (this.unregisteredIntrusiveHolders != null
         && this.unregisteredIntrusiveHolders.values().stream().anyMatch(r -> !r.isBound() && r.getType() == Holder.Reference.Type.INTRUSIVE)) {
         throw new IllegalStateException(
            "Some intrusive holders were not registered: "
               + this.unregisteredIntrusiveHolders.values()
               + " Hint: Did you register all your registry objects? Registry stage: "
               + this.stage.getName()
         );
      } else {
         return this;
      }
   }

   @Override
   public Holder.Reference<T> createIntrusiveHolder(T value) {
      if (!this.isIntrusive()) {
         throw new IllegalStateException("This registry can't create intrusive holders");
      } else {
         this.validateWrite();
         return super.createIntrusiveHolder(value);
      }
   }

   @Override
   public Optional<HolderSet.Named<T>> getTag(TagKey<T> name) {
      return Optional.ofNullable(this.tags.get(name));
   }

   @Override
   public void bindTags(Map<TagKey<T>, List<Holder<T>>> newTags) {
      Map<Holder.Reference<T>, List<TagKey<T>>> holderToTag = new IdentityHashMap<>();

      for (Holder.Reference<T> tReference : this.holdersByName.values()) {
         holderToTag.put(tReference, new ArrayList<>());
      }

      newTags.forEach((namex, values) -> values.forEach(holderx -> this.addTagToHolder(holderToTag, namex, holderx)));
      Set<TagKey<T>> set = new HashSet<>(Sets.difference(this.tags.keySet(), newTags.keySet()));
      set.removeAll(this.optionalTags.keySet());
      if (!set.isEmpty()) {
         LOGGER.warn(
            "Not all defined tags for registry {} are present in data pack: {}",
            this.key(),
            set.stream().map(k -> k.location().toString()).sorted().collect(Collectors.joining(", \n\t"))
         );
      }

      Map<TagKey<T>, HolderSet.Named<T>> tmpTags = new IdentityHashMap<>(this.tags);
      newTags.forEach((k, v) -> tmpTags.computeIfAbsent((TagKey<T>)k, this::createTag).bind((List<Holder<T>>)v));
      Set<TagKey<T>> defaultedTags = Sets.difference(this.optionalTags.keySet(), newTags.keySet());

      for (TagKey<T> name : defaultedTags) {
         List<Holder<T>> defaults = this.optionalTags
            .get(name)
            .stream()
            .map(valueSupplier -> this.getHolder((T)valueSupplier.get()).orElse(null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();

         for (Holder<T> holder : defaults) {
            this.addTagToHolder(holderToTag, name, holder);
         }

         tmpTags.computeIfAbsent(name, this::createTag).bind(defaults);
      }

      holderToTag.forEach(Holder.Reference::bindTags);
      this.tags = tmpTags;
      this.delegate.onBindTags(this.tags, defaultedTags);
   }

   private void addTagToHolder(Map<Holder.Reference<T>, List<TagKey<T>>> holderToTag, TagKey<T> name, Holder<T> holder) {
      if (!holder.canSerializeIn(this.holderOwner())) {
         throw new IllegalStateException("Can't create named set " + name + " containing value " + holder + " from outside registry " + this);
      } else if (!(holder instanceof Holder.Reference)) {
         throw new IllegalStateException("Found direct holder " + holder + " value in tag " + name);
      } else {
         holderToTag.get(holder).add(name);
      }
   }

   @Override
   public void resetTags() {
      this.tags.values().forEach(t -> t.bind(List.of()));
      this.holders.values().forEach(v -> v.bindTags(Set.of()));
   }

   @Override
   public void unfreeze() {
      this.frozen = false;
   }

   boolean isFrozen() {
      return this.frozen;
   }

   @Override
   public boolean isIntrusive() {
      return this.intrusiveHolderCallback != null && this.stage == RegistryManager.ACTIVE;
   }

   @Nullable
   Holder.Reference<T> onAdded(RegistryManager stage, int id, ResourceKey<T> key, T newValue, T oldValue) {
      Holder.Reference<T> newHolder = this.getHolder(key, newValue);
      this.holdersById.size(Math.max(this.holdersById.size(), id + 1));
      this.holdersById.set(id, newHolder);
      this.holdersByName.put(key.location(), newHolder);
      this.holders.put(newValue, newHolder);
      if (this.unregisteredIntrusiveHolders != null) {
         this.unregisteredIntrusiveHolders.remove(newValue);
         newHolder.bindKey(key);
      }

      newHolder.bindValue(newValue);
      this.holdersSorted = null;
      return newHolder;
   }

   private HolderSet.Named<T> createTag(TagKey<T> name) {
      return new HolderSet.Named<>(this.holderOwner(), name);
   }

   private Holder.Reference<T> getHolder(ResourceKey<T> key, T value) {
      return this.isIntrusive()
         ? this.intrusiveHolderCallback.apply(value)
         : this.holdersByName.computeIfAbsent(key.location(), k -> Holder.Reference.createStandAlone(this.holderOwner(), key));
   }

   private List<Holder.Reference<T>> getSortedHolders() {
      if (this.holdersSorted == null) {
         this.holdersSorted = this.holdersById.stream().filter(Objects::nonNull).toList();
      }

      return this.holdersSorted;
   }
}
