package net.minecraftforge.versions.forge;

import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.VersionChecker.CheckResult;
import net.minecraftforge.fml.VersionChecker.Status;
import net.minecraftforge.fml.loading.JarVersionLookupHandler;
import net.minecraftforge.fml.loading.JarVersionLookupHandler.Info;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ForgeVersion {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final String MOD_ID = "forge";
   private static final String forgeVersion;
   private static final String forgeSpec;
   private static final String forgeGroup;

   public static String getVersion() {
      return forgeVersion;
   }

   public static Status getStatus() {
      return VersionChecker.getResult((IModInfo)ModList.get().getModFileById("forge").getMods().get(0)).status();
   }

   @Nullable
   public static String getTarget() {
      CheckResult res = VersionChecker.getResult((IModInfo)ModList.get().getModFileById("forge").getMods().get(0));
      return res.target() == null ? "" : res.target().toString();
   }

   public static String getSpec() {
      return forgeSpec;
   }

   public static String getGroup() {
      return forgeGroup;
   }

   static {
      LOGGER.debug(Logging.CORE, "Forge Version package {} from {}", ForgeVersion.class.getPackage(), ForgeVersion.class.getClassLoader());
      Info info = JarVersionLookupHandler.getInfo(ForgeVersion.class);
      if (!info.impl().version().isEmpty() && !info.spec().version().isEmpty()) {
         forgeSpec = (String)info.spec().version().get();
         forgeVersion = (String)info.impl().version().get();
         forgeGroup = "net.minecraftforge";
         LOGGER.debug(Logging.CORE, "Found Forge version {}", forgeVersion);
         LOGGER.debug(Logging.CORE, "Found Forge spec {}", forgeSpec);
         LOGGER.debug(Logging.CORE, "Found Forge group {}", forgeGroup);
      } else {
         throw new IllegalStateException("Failed to find version for package " + ForgeVersion.class.getPackageName() + " This is an invalid environment");
      }
   }
}
