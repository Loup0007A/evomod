package net.minecraft;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
   private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
   private static final int MAX_FILE_NAME = 255;
   private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
   private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

   public static String sanitizeName(String pName) {
      for (char $$1 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         pName = pName.replace($$1, '_');
      }

      return pName.replaceAll("[./\"]", "_");
   }

   public static String findAvailableName(Path pDirPath, String pFileName, String pFileFormat) throws IOException {
      pFileName = sanitizeName(pFileName);
      if (RESERVED_WINDOWS_FILENAMES.matcher(pFileName).matches()) {
         pFileName = "_" + pFileName + "_";
      }

      Matcher $$3 = COPY_COUNTER_PATTERN.matcher(pFileName);
      int $$4 = 0;
      if ($$3.matches()) {
         pFileName = $$3.group("name");
         $$4 = Integer.parseInt($$3.group("count"));
      }

      if (pFileName.length() > 255 - pFileFormat.length()) {
         pFileName = pFileName.substring(0, 255 - pFileFormat.length());
      }

      while (true) {
         String $$5 = pFileName;
         if ($$4 != 0) {
            String $$6 = " (" + $$4 + ")";
            int $$7 = 255 - $$6.length();
            if (pFileName.length() > $$7) {
               $$5 = pFileName.substring(0, $$7);
            }

            $$5 = $$5 + $$6;
         }

         $$5 = $$5 + pFileFormat;
         Path $$8 = pDirPath.resolve($$5);

         try {
            Path $$9 = Files.createDirectory($$8);
            Files.deleteIfExists($$9);
            return pDirPath.relativize($$9).toString();
         } catch (FileAlreadyExistsException var8) {
            $$4++;
         }
      }
   }

   public static boolean isPathNormalized(Path pPath) {
      Path $$1 = pPath.normalize();
      return $$1.equals(pPath);
   }

   public static boolean isPathPortable(Path pPath) {
      for (Path $$1 : pPath) {
         if (RESERVED_WINDOWS_FILENAMES.matcher($$1.toString()).matches()) {
            return false;
         }
      }

      return true;
   }

   public static Path createPathToResource(Path pDirPath, String pLocationPath, String pFileFormat) {
      String $$3 = pLocationPath + pFileFormat;
      Path $$4 = Paths.get($$3);
      if ($$4.endsWith(pFileFormat)) {
         throw new InvalidPathException($$3, "empty resource name");
      } else {
         return pDirPath.resolve($$4);
      }
   }

   public static String getFullResourcePath(String pPath) {
      return FilenameUtils.getFullPath(pPath).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(String pPath) {
      return FilenameUtils.normalize(pPath).replace(File.separator, "/");
   }

   public static DataResult<List<String>> decomposePath(String pPath) {
      int $$1 = pPath.indexOf(47);
      if ($$1 == -1) {
         return switch (pPath) {
            case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + pPath + "'");
            default -> !isValidStrictPathSegment(pPath) ? DataResult.error(() -> "Invalid path '" + pPath + "'") : DataResult.success(List.of(pPath));
         };
      } else {
         List<String> $$2 = new ArrayList<>();
         int $$3 = 0;
         boolean $$4 = false;

         while (true) {
            String $$5 = pPath.substring($$3, $$1);
            switch ($$5) {
               case "":
               case ".":
               case "..":
                  return DataResult.error(() -> "Invalid segment '" + $$5 + "' in path '" + pPath + "'");
            }

            if (!isValidStrictPathSegment($$5)) {
               return DataResult.error(() -> "Invalid segment '" + $$5 + "' in path '" + pPath + "'");
            }

            $$2.add($$5);
            if ($$4) {
               return DataResult.success($$2);
            }

            $$3 = $$1 + 1;
            $$1 = pPath.indexOf(47, $$3);
            if ($$1 == -1) {
               $$1 = pPath.length();
               $$4 = true;
            }
         }
      }
   }

   public static Path resolvePath(Path pPath, List<String> pSubdirectories) {
      int $$2 = pSubdirectories.size();

      return switch ($$2) {
         case 0 -> pPath;
         case 1 -> pPath.resolve(pSubdirectories.get(0));
         default -> {
            String[] $$3 = new String[$$2 - 1];

            for (int $$4 = 1; $$4 < $$2; $$4++) {
               $$3[$$4 - 1] = pSubdirectories.get($$4);
            }

            yield pPath.resolve(pPath.getFileSystem().getPath(pSubdirectories.get(0), $$3));
         }
      };
   }

   public static boolean isValidStrictPathSegment(String pSegment) {
      return STRICT_PATH_SEGMENT_CHECK.matcher(pSegment).matches();
   }

   public static void validatePath(String... pElements) {
      if (pElements.length == 0) {
         throw new IllegalArgumentException("Path must have at least one element");
      } else {
         for (String $$1 : pElements) {
            if ($$1.equals("..") || $$1.equals(".") || !isValidStrictPathSegment($$1)) {
               throw new IllegalArgumentException("Illegal segment " + $$1 + " in path " + Arrays.toString((Object[])pElements));
            }
         }
      }
   }

   public static void createDirectoriesSafe(Path pPath) throws IOException {
      Files.createDirectories(Files.exists(pPath) ? pPath.toRealPath() : pPath);
   }
}
