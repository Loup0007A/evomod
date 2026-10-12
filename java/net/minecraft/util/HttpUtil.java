package net.minecraft.util;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class HttpUtil {
   private static final Logger LOGGER = LogUtils.getLogger();

   private HttpUtil() {
   }

   public static Path downloadFile(
      Path pSaveFile,
      URL pUrl,
      Map<String, String> pRequestProperties,
      HashFunction pHashFunction,
      @Nullable HashCode pHash,
      int pMaxSize,
      Proxy pProxy,
      HttpUtil.DownloadProgressListener pProgressListener
   ) {
      HttpURLConnection $$8 = null;
      InputStream $$9 = null;
      pProgressListener.requestStart();
      Path $$10;
      if (pHash != null) {
         $$10 = cachedFilePath(pSaveFile, pHash);

         try {
            if (checkExistingFile($$10, pHashFunction, pHash)) {
               LOGGER.info("Returning cached file since actual hash matches requested");
               pProgressListener.requestFinished(true);
               updateModificationTime($$10);
               return $$10;
            }
         } catch (IOException var35) {
            LOGGER.warn("Failed to check cached file {}", $$10, var35);
         }

         try {
            LOGGER.warn("Existing file {} not found or had mismatched hash", $$10);
            Files.deleteIfExists($$10);
         } catch (IOException var34) {
            pProgressListener.requestFinished(false);
            throw new UncheckedIOException("Failed to remove existing file " + $$10, var34);
         }
      } else {
         $$10 = null;
      }

      Path $$18;
      try {
         $$8 = (HttpURLConnection)pUrl.openConnection(pProxy);
         $$8.setInstanceFollowRedirects(true);
         pRequestProperties.forEach($$8::setRequestProperty);
         $$9 = $$8.getInputStream();
         long $$14 = $$8.getContentLengthLong();
         OptionalLong $$15 = $$14 != -1L ? OptionalLong.of($$14) : OptionalLong.empty();
         FileUtil.createDirectoriesSafe(pSaveFile);
         pProgressListener.downloadStart($$15);
         if ($$15.isPresent() && $$15.getAsLong() > (long)pMaxSize) {
            throw new IOException("Filesize is bigger than maximum allowed (file is " + $$15 + ", limit is " + pMaxSize + ")");
         }

         if ($$10 == null) {
            Path $$17 = Files.createTempFile(pSaveFile, "download", ".tmp");

            try {
               HashCode $$18x = downloadAndHash(pHashFunction, pMaxSize, pProgressListener, $$9, $$17);
               Path $$19 = cachedFilePath(pSaveFile, $$18x);
               if (!checkExistingFile($$19, pHashFunction, $$18x)) {
                  Files.move($$17, $$19, StandardCopyOption.REPLACE_EXISTING);
               } else {
                  updateModificationTime($$19);
               }

               pProgressListener.requestFinished(true);
               return $$19;
            } finally {
               Files.deleteIfExists($$17);
            }
         }

         HashCode $$16 = downloadAndHash(pHashFunction, pMaxSize, pProgressListener, $$9, $$10);
         if (!$$16.equals(pHash)) {
            throw new IOException("Hash of downloaded file (" + $$16 + ") did not match requested (" + pHash + ")");
         }

         pProgressListener.requestFinished(true);
         $$18 = $$10;
      } catch (Throwable var36) {
         if ($$8 != null) {
            InputStream $$21 = $$8.getErrorStream();
            if ($$21 != null) {
               try {
                  LOGGER.error("HTTP response error: {}", IOUtils.toString($$21, StandardCharsets.UTF_8));
               } catch (Exception var32) {
                  LOGGER.error("Failed to read response from server");
               }
            }
         }

         pProgressListener.requestFinished(false);
         throw new IllegalStateException("Failed to download file " + pUrl, var36);
      } finally {
         IOUtils.closeQuietly($$9);
      }

      return $$18;
   }

   private static void updateModificationTime(Path pPath) {
      try {
         Files.setLastModifiedTime(pPath, FileTime.from(Instant.now()));
      } catch (IOException var2) {
         LOGGER.warn("Failed to update modification time of {}", pPath, var2);
      }
   }

   private static HashCode hashFile(Path pPath, HashFunction pHashFunction) throws IOException {
      Hasher $$2 = pHashFunction.newHasher();

      try (
         OutputStream $$3 = Funnels.asOutputStream($$2);
         InputStream $$4 = Files.newInputStream(pPath);
      ) {
         $$4.transferTo($$3);
      }

      return $$2.hash();
   }

   private static boolean checkExistingFile(Path pPath, HashFunction pHashFunction, HashCode pExpectedHash) throws IOException {
      if (Files.exists(pPath)) {
         HashCode $$3 = hashFile(pPath, pHashFunction);
         if ($$3.equals(pExpectedHash)) {
            return true;
         }

         LOGGER.warn("Mismatched hash of file {}, expected {} but found {}", new Object[]{pPath, pExpectedHash, $$3});
      }

      return false;
   }

   private static Path cachedFilePath(Path pPath, HashCode pHash) {
      return pPath.resolve(pHash.toString());
   }

   private static HashCode downloadAndHash(
      HashFunction pHashFuntion, int pMaxSize, HttpUtil.DownloadProgressListener pProgressListener, InputStream pStream, Path pOutputPath
   ) throws IOException {
      HashCode var11;
      try (OutputStream $$5 = Files.newOutputStream(pOutputPath, StandardOpenOption.CREATE)) {
         Hasher $$6 = pHashFuntion.newHasher();
         byte[] $$7 = new byte[8196];
         long $$8 = 0L;

         int $$9;
         while (($$9 = pStream.read($$7)) >= 0) {
            $$8 += (long)$$9;
            pProgressListener.downloadedBytes($$8);
            if ($$8 > (long)pMaxSize) {
               throw new IOException("Filesize was bigger than maximum allowed (got >= " + $$8 + ", limit was " + pMaxSize + ")");
            }

            if (Thread.interrupted()) {
               LOGGER.error("INTERRUPTED");
               throw new IOException("Download interrupted");
            }

            $$5.write($$7, 0, $$9);
            $$6.putBytes($$7, 0, $$9);
         }

         var11 = $$6.hash();
      }

      return var11;
   }

   public static int getAvailablePort() {
      try {
         int var1;
         try (ServerSocket $$0 = new ServerSocket(0)) {
            var1 = $$0.getLocalPort();
         }

         return var1;
      } catch (IOException var5) {
         return 25564;
      }
   }

   public static boolean isPortAvailable(int pPort) {
      if (pPort >= 0 && pPort <= 65535) {
         try {
            boolean var2;
            try (ServerSocket $$1 = new ServerSocket(pPort)) {
               var2 = $$1.getLocalPort() == pPort;
            }

            return var2;
         } catch (IOException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   public interface DownloadProgressListener {
      void requestStart();

      void downloadStart(OptionalLong pTotalSize);

      void downloadedBytes(long pProgress);

      void requestFinished(boolean pSuccess);
   }
}
