package net.minecraftforge.registries;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class DataPackRegistriesHooks {
   private static final List<RegistryDataLoader.RegistryData<?>> SYNCHRONIZED_REGISTRIES = new ArrayList<>();
   private static final List<RegistryDataLoader.RegistryData<?>> WORLDGEN_REGISTRIES = new ArrayList<>();
   private static final Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES = new HashSet<>();
   private static final Set<ResourceKey<? extends Registry<?>>> SYNCHRONIZED_REGISTRIES_CUSTOM = new HashSet<>();
   private static final Set<ResourceKey<? extends Registry<?>>> SYNCHRONIZED_REGISTRIESD_CUSTOM_VIEW = Collections.unmodifiableSet(
      SYNCHRONIZED_REGISTRIES_CUSTOM
   );

   private DataPackRegistriesHooks() {
   }

   public static List<RegistryDataLoader.RegistryData<?>> grabSynchronizedRegistries(RegistryDataLoader.RegistryData<?>... vanilla) {
      if (!StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass().equals(RegistryDataLoader.class)) {
         throw new IllegalCallerException("Attempted to call DataPackRegistriesHooks#grabSynchronizedRegistries!");
      } else {
         for (RegistryDataLoader.RegistryData<?> reg : vanilla) {
            SYNCHRONIZED_REGISTRIES.add(reg);
         }

         return Collections.unmodifiableList(SYNCHRONIZED_REGISTRIES);
      }
   }

   static <T> void addRegistryCodec(DataPackRegistryEvent.DataPackRegistryData<T> data) {
      RegistryDataLoader.RegistryData<T> loaderData = data.loaderData();
      WORLDGEN_REGISTRIES.add(loaderData);
      if (data.networkCodec() != null) {
         NETWORKABLE_REGISTRIES.add(loaderData.key());
         SYNCHRONIZED_REGISTRIES_CUSTOM.add(loaderData.key());
         SYNCHRONIZED_REGISTRIES.add(new RegistryDataLoader.RegistryData<>(loaderData.key(), data.networkCodec(), false));
      }
   }

   public static List<RegistryDataLoader.RegistryData<?>> grabWorldgenRegistries(RegistryDataLoader.RegistryData<?>... vanilla) {
      if (!StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass().equals(RegistryDataLoader.class)) {
         throw new IllegalCallerException("Attempted to call DataPackRegistriesHooks#grabWorldgenRegistries!");
      } else {
         WORLDGEN_REGISTRIES.addAll(Arrays.asList(vanilla));
         return Collections.unmodifiableList(WORLDGEN_REGISTRIES);
      }
   }

   public static Set<ResourceKey<? extends Registry<?>>> grabNetworkableRegistries(Supplier<Set<ResourceKey<? extends Registry<?>>>> vanilla) {
      if (!StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass().equals(RegistrySynchronization.class)) {
         throw new IllegalCallerException("Attempted to call DataPackRegistriesHooks#grabNetworkableRegistries!");
      } else {
         NETWORKABLE_REGISTRIES.addAll(vanilla.get());
         return Collections.unmodifiableSet(NETWORKABLE_REGISTRIES);
      }
   }

   public static Set<ResourceKey<? extends Registry<?>>> getSyncedCustomRegistries() {
      return SYNCHRONIZED_REGISTRIESD_CUSTOM_VIEW;
   }
}
