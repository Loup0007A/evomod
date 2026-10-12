package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.slf4j.Logger;

public class WorldLoader {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <D, R> CompletableFuture<R> load(
      WorldLoader.InitConfig pInitConfig,
      WorldLoader.WorldDataSupplier<D> pWorldDataSupplier,
      WorldLoader.ResultFactory<D, R> pResultFactory,
      Executor pBackgroundExecutor,
      Executor pGameExecutor
   ) {
      try {
         Pair<WorldDataConfiguration, CloseableResourceManager> $$5 = pInitConfig.packConfig.createResourceManager();
         CloseableResourceManager $$6 = (CloseableResourceManager)$$5.getSecond();
         LayeredRegistryAccess<RegistryLayer> $$7 = RegistryLayer.createRegistryAccess();
         LayeredRegistryAccess<RegistryLayer> $$8 = loadAndReplaceLayer($$6, $$7, RegistryLayer.WORLDGEN, RegistryDataLoader.WORLDGEN_REGISTRIES);
         RegistryAccess.Frozen $$9 = $$8.getAccessForLoading(RegistryLayer.DIMENSIONS);
         RegistryAccess.Frozen $$10 = RegistryDataLoader.load($$6, $$9, RegistryDataLoader.DIMENSION_REGISTRIES);
         WorldDataConfiguration $$11 = (WorldDataConfiguration)$$5.getFirst();
         WorldLoader.DataLoadOutput<D> $$12 = pWorldDataSupplier.get(new WorldLoader.DataLoadContext($$6, $$11, $$9, $$10));
         LayeredRegistryAccess<RegistryLayer> $$13 = $$8.replaceFrom(RegistryLayer.DIMENSIONS, $$12.finalDimensions);
         return ReloadableServerResources.loadResources(
               $$6, $$13, $$11.enabledFeatures(), pInitConfig.commandSelection(), pInitConfig.functionCompilationLevel(), pBackgroundExecutor, pGameExecutor
            )
            .whenComplete((p_214370_, p_214371_) -> {
               if (p_214371_ != null) {
                  $$6.close();
               }
            })
            .thenApplyAsync(p_326210_ -> {
               p_326210_.updateRegistryTags();
               return pResultFactory.create($$6, p_326210_, $$13, $$12.cookie);
            }, pGameExecutor);
      } catch (Exception var14) {
         return CompletableFuture.failedFuture(var14);
      }
   }

   private static RegistryAccess.Frozen loadLayer(
      ResourceManager pResourceManager,
      LayeredRegistryAccess<RegistryLayer> pRegistryAccess,
      RegistryLayer pRegistryLayer,
      List<RegistryDataLoader.RegistryData<?>> pRegistryData
   ) {
      RegistryAccess.Frozen $$4 = pRegistryAccess.getAccessForLoading(pRegistryLayer);
      return RegistryDataLoader.load(pResourceManager, $$4, pRegistryData);
   }

   private static LayeredRegistryAccess<RegistryLayer> loadAndReplaceLayer(
      ResourceManager pResourceManager,
      LayeredRegistryAccess<RegistryLayer> pRegistryAccess,
      RegistryLayer pRegistryLayer,
      List<RegistryDataLoader.RegistryData<?>> pRegistryData
   ) {
      RegistryAccess.Frozen $$4 = loadLayer(pResourceManager, pRegistryAccess, pRegistryLayer, pRegistryData);
      return pRegistryAccess.replaceFrom(pRegistryLayer, $$4);
   }

   public static record DataLoadContext(
      ResourceManager resources, WorldDataConfiguration dataConfiguration, RegistryAccess.Frozen datapackWorldgen, RegistryAccess.Frozen datapackDimensions
   ) {
   }

   public static record DataLoadOutput<D>(D cookie, RegistryAccess.Frozen finalDimensions) {
   }

   public static record InitConfig(WorldLoader.PackConfig packConfig, Commands.CommandSelection commandSelection, int functionCompilationLevel) {
   }

   public static record PackConfig(PackRepository packRepository, WorldDataConfiguration initialDataConfig, boolean safeMode, boolean initMode) {
      public Pair<WorldDataConfiguration, CloseableResourceManager> createResourceManager() {
         WorldDataConfiguration $$0 = MinecraftServer.configurePackRepository(this.packRepository, this.initialDataConfig, this.initMode, this.safeMode);
         List<PackResources> $$1 = this.packRepository.openAllSelected();
         CloseableResourceManager $$2 = new MultiPackResourceManager(PackType.SERVER_DATA, $$1);
         return Pair.of($$0, $$2);
      }
   }

   @FunctionalInterface
   public interface ResultFactory<D, R> {
      R create(CloseableResourceManager pManager, ReloadableServerResources pResources, LayeredRegistryAccess<RegistryLayer> pRegistryAccess, D pCookie);
   }

   @FunctionalInterface
   public interface WorldDataSupplier<D> {
      WorldLoader.DataLoadOutput<D> get(WorldLoader.DataLoadContext pContext);
   }
}
