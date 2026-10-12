package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileDownload {
   static final Logger LOGGER = LogUtils.getLogger();
   volatile boolean cancelled;
   volatile boolean finished;
   volatile boolean error;
   volatile boolean extracting;
   @Nullable
   private volatile File tempFile;
   volatile File resourcePackPath;
   @Nullable
   private volatile HttpGet request;
   @Nullable
   private Thread currentThread;
   private final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();
   private static final String[] INVALID_FILE_NAMES = new String[]{
      "CON",
      "COM",
      "PRN",
      "AUX",
      "CLOCK$",
      "NUL",
      "COM1",
      "COM2",
      "COM3",
      "COM4",
      "COM5",
      "COM6",
      "COM7",
      "COM8",
      "COM9",
      "LPT1",
      "LPT2",
      "LPT3",
      "LPT4",
      "LPT5",
      "LPT6",
      "LPT7",
      "LPT8",
      "LPT9"
   };

   public long contentLength(String pUri) {
      CloseableHttpClient $$1 = null;
      HttpGet $$2 = null;

      long var5;
      try {
         $$2 = new HttpGet(pUri);
         $$1 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
         CloseableHttpResponse $$3 = $$1.execute($$2);
         return Long.parseLong($$3.getFirstHeader("Content-Length").getValue());
      } catch (Throwable var16) {
         LOGGER.error("Unable to get content length for download");
         var5 = 0L;
      } finally {
         if ($$2 != null) {
            $$2.releaseConnection();
         }

         if ($$1 != null) {
            try {
               $$1.close();
            } catch (IOException var15) {
               LOGGER.error("Could not close http client", var15);
            }
         }
      }

      return var5;
   }

   public void download(WorldDownload pDownload, String pWorldName, RealmsDownloadLatestWorldScreen.DownloadStatus pStatus, LevelStorageSource pSource) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            CloseableHttpClient $$4 = null;

            try {
               this.tempFile = File.createTempFile("backup", ".tar.gz");
               this.request = new HttpGet(pDownload.downloadLink);
               $$4 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
               HttpResponse $$5 = $$4.execute(this.request);
               pStatus.totalBytes = Long.parseLong($$5.getFirstHeader("Content-Length").getValue());
               if ($$5.getStatusLine().getStatusCode() == 200) {
                  OutputStream $$12 = new FileOutputStream(this.tempFile);
                  FileDownload.ProgressListener $$13 = new FileDownload.ProgressListener(pWorldName.trim(), this.tempFile, pSource, pStatus);
                  FileDownload.DownloadCountingOutputStream $$14 = new FileDownload.DownloadCountingOutputStream($$12);
                  $$14.setListener($$13);
                  IOUtils.copy($$5.getEntity().getContent(), $$14);
                  return;
               }

               this.error = true;
               this.request.abort();
            } catch (Exception var93) {
               LOGGER.error("Caught exception while downloading: {}", var93.getMessage());
               this.error = true;
               return;
            } finally {
               this.request.releaseConnection();
               if (this.tempFile != null) {
                  this.tempFile.delete();
               }

               if (!this.error) {
                  if (!pDownload.resourcePackUrl.isEmpty() && !pDownload.resourcePackHash.isEmpty()) {
                     try {
                        this.tempFile = File.createTempFile("resources", ".tar.gz");
                        this.request = new HttpGet(pDownload.resourcePackUrl);
                        HttpResponse $$28 = $$4.execute(this.request);
                        pStatus.totalBytes = Long.parseLong($$28.getFirstHeader("Content-Length").getValue());
                        if ($$28.getStatusLine().getStatusCode() != 200) {
                           this.error = true;
                           this.request.abort();
                           return;
                        }

                        OutputStream $$29 = new FileOutputStream(this.tempFile);
                        FileDownload.ResourcePackProgressListener $$30 = new FileDownload.ResourcePackProgressListener(this.tempFile, pStatus, pDownload);
                        FileDownload.DownloadCountingOutputStream $$31 = new FileDownload.DownloadCountingOutputStream($$29);
                        $$31.setListener($$30);
                        IOUtils.copy($$28.getEntity().getContent(), $$31);
                     } catch (Exception var91) {
                        LOGGER.error("Caught exception while downloading: {}", var91.getMessage());
                        this.error = true;
                     } finally {
                        this.request.releaseConnection();
                        if (this.tempFile != null) {
                           this.tempFile.delete();
                        }
                     }
                  } else {
                     this.finished = true;
                  }
               }

               if ($$4 != null) {
                  try {
                     $$4.close();
                  } catch (IOException var90) {
                     LOGGER.error("Failed to close Realms download client");
                  }
               }
            }
         });
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   public void cancel() {
      if (this.request != null) {
         this.request.abort();
      }

      if (this.tempFile != null) {
         this.tempFile.delete();
      }

      this.cancelled = true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isError() {
      return this.error;
   }

   public boolean isExtracting() {
      return this.extracting;
   }

   public static String findAvailableFolderName(String pFolderName) {
      pFolderName = pFolderName.replaceAll("[\\./\"]", "_");

      for (String $$1 : INVALID_FILE_NAMES) {
         if (pFolderName.equalsIgnoreCase($$1)) {
            pFolderName = "_" + pFolderName + "_";
         }
      }

      return pFolderName;
   }

   void untarGzipArchive(String pWorldName, @Nullable File pTempFile, LevelStorageSource pLevelStorageSource) throws IOException {
      Pattern $$3 = Pattern.compile(".*-([0-9]+)$");
      int $$4 = 1;

      for (char $$5 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         pWorldName = pWorldName.replace($$5, '_');
      }

      if (StringUtils.isEmpty(pWorldName)) {
         pWorldName = "Realm";
      }

      pWorldName = findAvailableFolderName(pWorldName);

      try {
         for (LevelStorageSource.LevelDirectory $$6 : pLevelStorageSource.findLevelCandidates()) {
            String $$7 = $$6.directoryName();
            if ($$7.toLowerCase(Locale.ROOT).startsWith(pWorldName.toLowerCase(Locale.ROOT))) {
               Matcher $$8 = $$3.matcher($$7);
               if ($$8.matches()) {
                  int $$9 = Integer.parseInt($$8.group(1));
                  if ($$9 > $$4) {
                     $$4 = $$9;
                  }
               } else {
                  $$4++;
               }
            }
         }
      } catch (Exception var43) {
         LOGGER.error("Error getting level list", var43);
         this.error = true;
         return;
      }

      String $$13;
      if (pLevelStorageSource.isNewLevelIdAcceptable(pWorldName) && $$4 <= 1) {
         $$13 = pWorldName;
      } else {
         $$13 = pWorldName + ($$4 == 1 ? "" : "-" + $$4);
         if (!pLevelStorageSource.isNewLevelIdAcceptable($$13)) {
            boolean $$12 = false;

            while (!$$12) {
               $$4++;
               $$13 = pWorldName + ($$4 == 1 ? "" : "-" + $$4);
               if (pLevelStorageSource.isNewLevelIdAcceptable($$13)) {
                  $$12 = true;
               }
            }
         }
      }

      TarArchiveInputStream $$14 = null;
      File $$15 = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "saves");

      try {
         $$15.mkdir();
         $$14 = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(pTempFile))));

         for (TarArchiveEntry $$16 = $$14.getNextTarEntry(); $$16 != null; $$16 = $$14.getNextTarEntry()) {
            File $$17 = new File($$15, $$16.getName().replace("world", $$13));
            if ($$16.isDirectory()) {
               $$17.mkdirs();
            } else {
               $$17.createNewFile();

               try (FileOutputStream $$18 = new FileOutputStream($$17)) {
                  IOUtils.copy($$14, $$18);
               }
            }
         }
      } catch (Exception var41) {
         LOGGER.error("Error extracting world", var41);
         this.error = true;
      } finally {
         if ($$14 != null) {
            $$14.close();
         }

         if (pTempFile != null) {
            pTempFile.delete();
         }

         try (LevelStorageSource.LevelStorageAccess $$26 = pLevelStorageSource.validateAndCreateAccess($$13)) {
            $$26.renameAndDropPlayer($$13);
         } catch (NbtException | ReportedNbtException | IOException var39) {
            LOGGER.error("Failed to modify unpacked realms level {}", $$13, var39);
         } catch (ContentValidationException var40) {
            LOGGER.warn("{}", var40.getMessage());
         }

         this.resourcePackPath = new File($$15, $$13 + File.separator + "resources.zip");
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DownloadCountingOutputStream extends CountingOutputStream {
      @Nullable
      private ActionListener listener;

      public DownloadCountingOutputStream(OutputStream pOut) {
         super(pOut);
      }

      public void setListener(ActionListener pListener) {
         this.listener = pListener;
      }

      protected void afterWrite(int pBytesWritten) throws IOException {
         super.afterWrite(pBytesWritten);
         if (this.listener != null) {
            this.listener.actionPerformed(new ActionEvent(this, 0, null));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ProgressListener implements ActionListener {
      private final String worldName;
      private final File tempFile;
      private final LevelStorageSource levelStorageSource;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      ProgressListener(
         final String pWorldName,
         final File pTempFile,
         final LevelStorageSource pLevelStorageSource,
         final RealmsDownloadLatestWorldScreen.DownloadStatus pDownloadStatus
      ) {
         this.worldName = pWorldName;
         this.tempFile = pTempFile;
         this.levelStorageSource = pLevelStorageSource;
         this.downloadStatus = pDownloadStatus;
      }

      @Override
      public void actionPerformed(ActionEvent pAction) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)pAction.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled && !FileDownload.this.error) {
            try {
               FileDownload.this.extracting = true;
               FileDownload.this.untarGzipArchive(this.worldName, this.tempFile, this.levelStorageSource);
            } catch (IOException var3) {
               FileDownload.LOGGER.error("Error extracting archive", var3);
               FileDownload.this.error = true;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ResourcePackProgressListener implements ActionListener {
      private final File tempFile;
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
      private final WorldDownload worldDownload;

      ResourcePackProgressListener(
         final File pTempFile, final RealmsDownloadLatestWorldScreen.DownloadStatus pDownloadStatus, final WorldDownload pWorldDownload
      ) {
         this.tempFile = pTempFile;
         this.downloadStatus = pDownloadStatus;
         this.worldDownload = pWorldDownload;
      }

      @Override
      public void actionPerformed(ActionEvent pAction) {
         this.downloadStatus.bytesWritten = ((FileDownload.DownloadCountingOutputStream)pAction.getSource()).getByteCount();
         if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled) {
            try {
               String $$1 = Hashing.sha1().hashBytes(Files.toByteArray(this.tempFile)).toString();
               if ($$1.equals(this.worldDownload.resourcePackHash)) {
                  FileUtils.copyFile(this.tempFile, FileDownload.this.resourcePackPath);
                  FileDownload.this.finished = true;
               } else {
                  FileDownload.LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", this.worldDownload.resourcePackHash, $$1);
                  FileUtils.deleteQuietly(this.tempFile);
                  FileDownload.this.error = true;
               }
            } catch (IOException var3) {
               FileDownload.LOGGER.error("Error copying resourcepack file: {}", var3.getMessage());
               FileDownload.this.error = true;
            }
         }
      }
   }
}
