package net.minecraftforge.versions.mcp;

import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.loading.JarVersionLookupHandler;
import net.minecraftforge.fml.loading.JarVersionLookupHandler.Info;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCPVersion {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String mcVersion;
   private static final String mcpVersion;

   public static String getMCVersion() {
      return mcVersion;
   }

   public static String getMCPVersion() {
      return mcpVersion;
   }

   public static String getMCPandMCVersion() {
      return mcVersion + "-" + mcpVersion;
   }

   static {
      LOGGER.debug(Logging.CORE, "MCP Version package {} from {}", MCPVersion.class.getPackage(), MCPVersion.class.getClassLoader());
      Info info = JarVersionLookupHandler.getInfo(MCPVersion.class);
      if (!info.spec().version().isEmpty() && !info.impl().version().isEmpty()) {
         mcVersion = (String)info.spec().version().get();
         mcpVersion = (String)info.impl().version().get();
         LOGGER.debug(Logging.CORE, "Found MC version information {}", mcVersion);
         LOGGER.debug(Logging.CORE, "Found MCP version information {}", mcpVersion);
      } else {
         throw new IllegalStateException("Failed to find version for package " + MCPVersion.class.getPackageName() + " This is an invalid environment");
      }
   }
}
