package net.minecraft.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import org.slf4j.Logger;

public class FileZipper implements Closeable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Path outputFile;
   private final Path tempFile;
   private final FileSystem fs;

   public FileZipper(Path pOutputFile) {
      this.outputFile = pOutputFile;
      this.tempFile = pOutputFile.resolveSibling(pOutputFile.getFileName().toString() + "_tmp");

      try {
         this.fs = Util.ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(this.tempFile, ImmutableMap.of("create", "true"));
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   public void add(Path pPath, String pFilename) {
      try {
         Path $$2 = this.fs.getPath(File.separator);
         Path $$3 = $$2.resolve(pPath.toString());
         Files.createDirectories($$3.getParent());
         Files.write($$3, pFilename.getBytes(StandardCharsets.UTF_8));
      } catch (IOException var5) {
         throw new UncheckedIOException(var5);
      }
   }

   public void add(Path pPath, File pFilename) {
      try {
         Path $$2 = this.fs.getPath(File.separator);
         Path $$3 = $$2.resolve(pPath.toString());
         Files.createDirectories($$3.getParent());
         Files.copy(pFilename.toPath(), $$3);
      } catch (IOException var5) {
         throw new UncheckedIOException(var5);
      }
   }

   public void add(Path pPath) {
      try {
         Path $$1 = this.fs.getPath(File.separator);
         if (Files.isRegularFile(pPath)) {
            Path $$2 = $$1.resolve(pPath.getParent().relativize(pPath).toString());
            Files.copy($$2, pPath);
         } else {
            try (Stream<Path> $$3 = Files.find(pPath, Integer.MAX_VALUE, (p_144707_, p_144708_) -> p_144708_.isRegularFile())) {
               for (Path $$4 : $$3.collect(Collectors.toList())) {
                  Path $$5 = $$1.resolve(pPath.relativize($$4).toString());
                  Files.createDirectories($$5.getParent());
                  Files.copy($$4, $$5);
               }
            }
         }
      } catch (IOException var9) {
         throw new UncheckedIOException(var9);
      }
   }

   @Override
   public void close() {
      try {
         this.fs.close();
         Files.move(this.tempFile, this.outputFile);
         LOGGER.info("Compressed to {}", this.outputFile);
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }
   }
}
