package net.minecraftforge.data.loading;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModWorkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatagenModLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static ExistingFileHelper existingFileHelper;
   private static boolean runningDataGen;

   public static boolean isRunningDataGen() {
      return runningDataGen;
   }

   public static void begin(
      Set<String> patterns,
      Path output,
      Collection<Path> inputs,
      Collection<Path> existingPacks,
      Set<String> existingMods,
      boolean genServer,
      boolean genClient,
      boolean genDev,
      boolean genReports,
      boolean validate,
      boolean flat,
      String assetIndex,
      File assetsDir
   ) {
      if (!patterns.contains("minecraft") || patterns.size() != 1) {
         runningDataGen = true;
         Bootstrap.bootStrap();
         ModLoader.get().gatherAndInitializeMods(ModWorkManager.syncExecutor(), ModWorkManager.parallelExecutor(), () -> {
         });
         CompletableFuture<HolderLookup.Provider> lookupProvider = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
         HashSet<String> mods = new HashSet<>();

         for (String pattern : patterns) {
            if (pattern.indexOf(46) == -1) {
               mods.add(pattern);
            }

            Pattern m = Pattern.compile("^" + pattern + "$");
            ModList.get().forEachModInOrder(mc -> {
               String id = mc.getModId();
               if (!"forge".equals(id) && !"minecraft".equals(id) && m.matcher(id).matches()) {
                  mods.add(id);
               }
            });
         }

         LOGGER.info("Initializing Data Gatherer for mods {}", mods);
         GatherDataEvent.DataGeneratorConfig config = new GatherDataEvent.DataGeneratorConfig(
            mods, output, inputs, lookupProvider, genServer, genClient, genDev, genReports, validate, flat
         );
         if (!mods.contains("forge")) {
            existingMods.add("forge");
         }

         existingFileHelper = new ExistingFileHelper(existingPacks, existingMods, validate, assetIndex, assetsDir);
         ModLoader.get()
            .runEventGenerator(
               mc -> new GatherDataEvent(
                     mc,
                     config.makeGenerator(p -> config.isFlat() ? p : p.resolve(mc.getModId()), config.getMods().contains(mc.getModId())),
                     config,
                     existingFileHelper
                  )
            );
         config.runAll();
      }
   }
}
