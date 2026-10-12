package net.minecraft.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

   public static CompletableFuture<LayeredRegistryAccess<RegistryLayer>> reload(
      LayeredRegistryAccess<RegistryLayer> pRegistries, ResourceManager pResourceManager, Executor pBackgroundExecutor
   ) {
      RegistryAccess.Frozen $$3 = pRegistries.getAccessForLoading(RegistryLayer.RELOADABLE);
      RegistryOps<JsonElement> $$4 = new ReloadableServerRegistries.EmptyTagLookupWrapper($$3).createSerializationContext(JsonOps.INSTANCE);
      List<CompletableFuture<WritableRegistry<?>>> $$5 = LootDataType.values()
         .map(p_335637_ -> scheduleElementParse((LootDataType<?>)p_335637_, $$4, pResourceManager, pBackgroundExecutor))
         .toList();
      CompletableFuture<List<WritableRegistry<?>>> $$6 = Util.sequence($$5);
      return $$6.thenApplyAsync(p_335751_ -> apply(pRegistries, (List<WritableRegistry<?>>)p_335751_), pBackgroundExecutor);
   }

   private static <T> CompletableFuture<WritableRegistry<?>> scheduleElementParse(
      LootDataType<T> pLootDataType, RegistryOps<JsonElement> pRegistryOps, ResourceManager pResourceManager, Executor pBackgroundExecutor
   ) {
      return CompletableFuture.supplyAsync(
         () -> {
            WritableRegistry<T> $$3 = new MappedRegistry<>(pLootDataType.registryKey(), Lifecycle.experimental());
            Map<ResourceLocation, JsonElement> $$4 = new HashMap<>();
            String $$5 = Registries.elementsDirPath(pLootDataType.registryKey());
            SimpleJsonResourceReloadListener.scanDirectory(pResourceManager, $$5, GSON, $$4);
            $$4.forEach(
               (p_328381_, p_334327_) -> pLootDataType.deserialize(p_328381_, pRegistryOps, p_334327_)
                     .ifPresent(p_332628_ -> $$3.register(ResourceKey.create(pLootDataType.registryKey(), p_328381_), (T)p_332628_, DEFAULT_REGISTRATION_INFO))
            );
            return $$3;
         },
         pBackgroundExecutor
      );
   }

   private static LayeredRegistryAccess<RegistryLayer> apply(LayeredRegistryAccess<RegistryLayer> pRegistryAccess, List<WritableRegistry<?>> pRegistries) {
      LayeredRegistryAccess<RegistryLayer> $$2 = createUpdatedRegistries(pRegistryAccess, pRegistries);
      ProblemReporter.Collector $$3 = new ProblemReporter.Collector();
      RegistryAccess.Frozen $$4 = $$2.compositeAccess();
      ValidationContext $$5 = new ValidationContext($$3, LootContextParamSets.ALL_PARAMS, $$4.asGetterLookup());
      LootDataType.values().forEach(p_332567_ -> validateRegistry($$5, (LootDataType<?>)p_332567_, $$4));
      $$3.get().forEach((p_336191_, p_332871_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_336191_, p_332871_));
      return $$2;
   }

   private static LayeredRegistryAccess<RegistryLayer> createUpdatedRegistries(
      LayeredRegistryAccess<RegistryLayer> pRegistryAccess, List<WritableRegistry<?>> pRegistries
   ) {
      RegistryAccess $$2 = new RegistryAccess.ImmutableRegistryAccess(pRegistries);
      ((WritableRegistry)$$2.<LootTable>registryOrThrow(Registries.LOOT_TABLE)).register(BuiltInLootTables.EMPTY, LootTable.EMPTY, DEFAULT_REGISTRATION_INFO);
      return pRegistryAccess.replaceFrom(RegistryLayer.RELOADABLE, $$2.freeze());
   }

   private static <T> void validateRegistry(ValidationContext pContext, LootDataType<T> pLootDataType, RegistryAccess pRegistryAccess) {
      Registry<T> $$3 = pRegistryAccess.registryOrThrow(pLootDataType.registryKey());
      $$3.holders().forEach(p_334560_ -> pLootDataType.runValidation(pContext, p_334560_.key(), p_334560_.value()));
   }

   static class EmptyTagLookupWrapper implements HolderLookup.Provider {
      private final RegistryAccess registryAccess;

      EmptyTagLookupWrapper(RegistryAccess pRegistryAccess) {
         this.registryAccess = pRegistryAccess;
      }

      @Override
      public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
         return this.registryAccess.listRegistries();
      }

      @Override
      public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> pRegistryKey) {
         return this.registryAccess.registry(pRegistryKey).map(Registry::asTagAddingLookup);
      }
   }

   public static class Holder {
      private final RegistryAccess.Frozen registries;

      public Holder(RegistryAccess.Frozen pRegistries) {
         this.registries = pRegistries;
      }

      public RegistryAccess.Frozen get() {
         return this.registries;
      }

      public HolderGetter.Provider lookup() {
         return this.registries.asGetterLookup();
      }

      public Collection<ResourceLocation> getKeys(ResourceKey<? extends Registry<?>> pRegistryKey) {
         return this.registries.registry(pRegistryKey).stream().flatMap(p_332262_ -> p_332262_.holders().map(p_333743_ -> p_333743_.key().location())).toList();
      }

      public LootTable getLootTable(ResourceKey<LootTable> pLootTableKey) {
         return this.registries
            .lookup(Registries.LOOT_TABLE)
            .flatMap(p_328118_ -> p_328118_.get(pLootTableKey))
            .map(net.minecraft.core.Holder::value)
            .orElse(LootTable.EMPTY);
      }
   }
}
