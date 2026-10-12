package net.minecraft.world.level.validation;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class PathAllowList implements PathMatcher {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMENT_PREFIX = "#";
   private final List<PathAllowList.ConfigEntry> entries;
   private final Map<String, PathMatcher> compiledPaths = new ConcurrentHashMap<>();

   public PathAllowList(List<PathAllowList.ConfigEntry> pEntries) {
      this.entries = pEntries;
   }

   public PathMatcher getForFileSystem(FileSystem pFileSystem) {
      return this.compiledPaths.computeIfAbsent(pFileSystem.provider().getScheme(), p_289958_ -> {
         List<PathMatcher> $$2;
         try {
            $$2 = this.entries.stream().map(p_289937_ -> p_289937_.compile(pFileSystem)).toList();
         } catch (Exception var5) {
            LOGGER.error("Failed to compile file pattern list", var5);
            return p_289987_ -> false;
         }
         return switch ($$2.size()) {
            case 0 -> p_289982_ -> false;
            case 1 -> (PathMatcher)$$2.get(0);
            default -> p_289927_ -> {
            for (PathMatcher $$2 : $$2) {
               if ($$2.matches(p_289927_)) {
                  return true;
               }
            }

            return false;
         };
         };
      });
   }

   @Override
   public boolean matches(Path pPath) {
      return this.getForFileSystem(pPath.getFileSystem()).matches(pPath);
   }

   public static PathAllowList readPlain(BufferedReader pReader) {
      return new PathAllowList(pReader.lines().flatMap(p_289962_ -> PathAllowList.ConfigEntry.parse(p_289962_).stream()).toList());
   }

   public static record ConfigEntry(PathAllowList.EntryType type, String pattern) {
      public PathMatcher compile(FileSystem pFileSystem) {
         return this.type().compile(pFileSystem, this.pattern);
      }

      static Optional<PathAllowList.ConfigEntry> parse(String pString) {
         if (pString.isBlank() || pString.startsWith("#")) {
            return Optional.empty();
         } else if (!pString.startsWith("[")) {
            return Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, pString));
         } else {
            int $$1 = pString.indexOf(93, 1);
            if ($$1 == -1) {
               throw new IllegalArgumentException("Unterminated type in line '" + pString + "'");
            } else {
               String $$2 = pString.substring(1, $$1);
               String $$3 = pString.substring($$1 + 1);

               return switch ($$2) {
                  case "glob", "regex" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, $$2 + ":" + $$3));
                  case "prefix" -> Optional.of(new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, $$3));
                  default -> throw new IllegalArgumentException("Unsupported definition type in line '" + pString + "'");
               };
            }
         }
      }

      static PathAllowList.ConfigEntry glob(String pGlob) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "glob:" + pGlob);
      }

      static PathAllowList.ConfigEntry regex(String pRegex) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.FILESYSTEM, "regex:" + pRegex);
      }

      static PathAllowList.ConfigEntry prefix(String pPrefix) {
         return new PathAllowList.ConfigEntry(PathAllowList.EntryType.PREFIX, pPrefix);
      }
   }

   @FunctionalInterface
   public interface EntryType {
      PathAllowList.EntryType FILESYSTEM = FileSystem::getPathMatcher;
      PathAllowList.EntryType PREFIX = (p_289949_, p_289938_) -> p_289955_ -> p_289955_.toString().startsWith(p_289938_);

      PathMatcher compile(FileSystem pFileSystem, String pPattern);
   }
}
