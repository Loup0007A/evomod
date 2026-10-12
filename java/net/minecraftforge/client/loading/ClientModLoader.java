package net.minecraftforge.client.loading;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.DataPackConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.ModWorkManager;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.ModWorkManager.DrivenExecutor;
import net.minecraftforge.fml.VersionChecker.CheckResult;
import net.minecraftforge.fml.VersionChecker.Status;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import net.minecraftforge.internal.BrandingControl;
import net.minecraftforge.logging.CrashReportExtender;
import net.minecraftforge.resource.ResourcePackLoader;
import net.minecraftforge.server.LanguageHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static boolean loading;
   private static Minecraft mc;
   private static boolean loadingComplete;
   private static LoadingFailedException error;

   public static void begin(Minecraft minecraft, PackRepository defaultResourcePacks, ReloadableResourceManager mcResourceManager) {
      Runtime.getRuntime().addShutdownHook(new Thread(LogManager::shutdown));
      ImmediateWindowHandler.updateProgress("Loading mods");
      loading = true;
      mc = minecraft;
      LogicalSidedProvider.setClient(() -> minecraft);
      LanguageHook.loadForgeAndMCLangs();
      createRunnableWithCatch(
            () -> ModLoader.get().gatherAndInitializeMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), ImmediateWindowHandler::renderTick)
         )
         .run();
      if (error == null) {
         ResourcePackLoader.loadResourcePacks(defaultResourcePacks, true);
         ModLoader.get().postEvent(new AddPackFindersEvent(PackType.CLIENT_RESOURCES, defaultResourcePacks::addPackFinder));
         DataPackConfig.DEFAULT.addModPacks(ResourcePackLoader.getPackNames());
         mcResourceManager.registerReloadListener(ClientModLoader::onResourceReload);
         mcResourceManager.registerReloadListener(BrandingControl.resourceManagerReloadListener());
      }
   }

   private static CompletableFuture<Void> onResourceReload(
      PreparableReloadListener.PreparationBarrier stage,
      ResourceManager resourceManager,
      ProfilerFiller prepareProfiler,
      ProfilerFiller executeProfiler,
      Executor asyncExecutor,
      Executor syncExecutor
   ) {
      return CompletableFuture.runAsync(
            createRunnableWithCatch(() -> startModLoading(ModWorkManager.wrappedExecutor(syncExecutor), asyncExecutor)), ModWorkManager.parallelExecutor()
         )
         .thenCompose(stage::wait)
         .thenRunAsync(() -> finishModLoading(ModWorkManager.wrappedExecutor(syncExecutor), asyncExecutor), ModWorkManager.parallelExecutor());
   }

   private static Runnable createRunnableWithCatch(Runnable r) {
      return () -> {
         if (!loadingComplete) {
            try {
               r.run();
            } catch (LoadingFailedException var2) {
               if (error == null) {
                  error = var2;
               }
            }
         }
      };
   }

   private static void startModLoading(DrivenExecutor syncExecutor, Executor parallelExecutor) {
      createRunnableWithCatch(() -> ModLoader.get().loadMods(syncExecutor, parallelExecutor, ImmediateWindowHandler::renderTick)).run();
   }

   private static void finishModLoading(DrivenExecutor syncExecutor, Executor parallelExecutor) {
      createRunnableWithCatch(() -> ModLoader.get().finishMods(syncExecutor, parallelExecutor, ImmediateWindowHandler::renderTick)).run();
      loading = false;
      loadingComplete = true;
      syncExecutor.execute(() -> mc.options.load(true));
   }

   public static Status checkForUpdates() {
      boolean anyOutdated = ModList.get().getMods().stream().map(VersionChecker::getResult).map(CheckResult::status).anyMatch(Status::isOutdated);
      return anyOutdated ? Status.OUTDATED : null;
   }

   public static boolean completeModLoading() {
      List<ModLoadingWarning> warnings = ModLoader.get().getWarnings();
      boolean showWarnings = ForgeConfig.CLIENT.showLoadWarnings();
      if (!showWarnings) {
         if (!warnings.isEmpty()) {
            LOGGER.warn(Logging.LOADING, "Mods loaded with {} warning(s)", warnings.size());
            warnings.forEach(warning -> LOGGER.warn(Logging.LOADING, warning.formatToString()));
         }

         warnings = Collections.emptyList();
      }

      File dumpedLocation = null;
      if (error == null) {
         MinecraftForge.EVENT_BUS.start();
      } else {
         LanguageHook.loadForgeAndMCLangs();
         dumpedLocation = CrashReportExtender.dumpModLoadingCrashReport(LOGGER, error, mc.gameDirectory);
      }

      if (error == null && warnings.isEmpty()) {
         return false;
      } else {
         mc.setScreen(new LoadingErrorScreen(error, warnings, dumpedLocation));
         return true;
      }
   }

   public static boolean isLoading() {
      return loading;
   }
}
