package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileUpload {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_RETRIES = 5;
   private static final String UPLOAD_PATH = "/upload";
   private final File file;
   private final long realmId;
   private final int slotId;
   private final UploadInfo uploadInfo;
   private final String sessionId;
   private final String username;
   private final String clientVersion;
   private final String worldVersion;
   private final UploadStatus uploadStatus;
   private final AtomicBoolean cancelled = new AtomicBoolean(false);
   @Nullable
   private CompletableFuture<UploadResult> uploadTask;
   private final RequestConfig requestConfig = RequestConfig.custom()
      .setSocketTimeout((int)TimeUnit.MINUTES.toMillis(10L))
      .setConnectTimeout((int)TimeUnit.SECONDS.toMillis(15L))
      .build();

   public FileUpload(
      File pFile, long pRealmId, int pSlotId, UploadInfo pUploadInfo, User pUser, String pClientVersiob, String pWorldVersion, UploadStatus pUploadStatus
   ) {
      this.file = pFile;
      this.realmId = pRealmId;
      this.slotId = pSlotId;
      this.uploadInfo = pUploadInfo;
      this.sessionId = pUser.getSessionId();
      this.username = pUser.getName();
      this.clientVersion = pClientVersiob;
      this.worldVersion = pWorldVersion;
      this.uploadStatus = pUploadStatus;
   }

   public void upload(Consumer<UploadResult> pResultConsumer) {
      if (this.uploadTask == null) {
         this.uploadTask = CompletableFuture.supplyAsync(() -> this.requestUpload(0));
         this.uploadTask.thenAccept(pResultConsumer);
      }
   }

   public void cancel() {
      this.cancelled.set(true);
      if (this.uploadTask != null) {
         this.uploadTask.cancel(false);
         this.uploadTask = null;
      }
   }

   private UploadResult requestUpload(int pRetries) {
      UploadResult.Builder $$1 = new UploadResult.Builder();
      if (this.cancelled.get()) {
         return $$1.build();
      } else {
         this.uploadStatus.totalBytes = this.file.length();
         HttpPost $$2 = new HttpPost(this.uploadInfo.getUploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId));
         CloseableHttpClient $$3 = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();

         UploadResult var8;
         try {
            this.setupRequest($$2);
            HttpResponse $$4 = $$3.execute($$2);
            long $$5 = this.getRetryDelaySeconds($$4);
            if (!this.shouldRetry($$5, pRetries)) {
               this.handleResponse($$4, $$1);
               return $$1.build();
            }

            var8 = this.retryUploadAfter($$5, pRetries);
         } catch (Exception var12) {
            if (!this.cancelled.get()) {
               LOGGER.error("Caught exception while uploading: ", var12);
            }

            return $$1.build();
         } finally {
            this.cleanup($$2, $$3);
         }

         return var8;
      }
   }

   private void cleanup(HttpPost pPost, @Nullable CloseableHttpClient pHttpClient) {
      pPost.releaseConnection();
      if (pHttpClient != null) {
         try {
            pHttpClient.close();
         } catch (IOException var4) {
            LOGGER.error("Failed to close Realms upload client");
         }
      }
   }

   private void setupRequest(HttpPost pPost) throws FileNotFoundException {
      pPost.setHeader(
         "Cookie",
         "sid="
            + this.sessionId
            + ";token="
            + this.uploadInfo.getToken()
            + ";user="
            + this.username
            + ";version="
            + this.clientVersion
            + ";worldVersion="
            + this.worldVersion
      );
      FileUpload.CustomInputStreamEntity $$1 = new FileUpload.CustomInputStreamEntity(new FileInputStream(this.file), this.file.length(), this.uploadStatus);
      $$1.setContentType("application/octet-stream");
      pPost.setEntity($$1);
   }

   private void handleResponse(HttpResponse pResponse, UploadResult.Builder pUploadResult) throws IOException {
      int $$2 = pResponse.getStatusLine().getStatusCode();
      if ($$2 == 401) {
         LOGGER.debug("Realms server returned 401: {}", pResponse.getFirstHeader("WWW-Authenticate"));
      }

      pUploadResult.withStatusCode($$2);
      if (pResponse.getEntity() != null) {
         String $$3 = EntityUtils.toString(pResponse.getEntity(), "UTF-8");
         if ($$3 != null) {
            try {
               JsonParser $$4 = new JsonParser();
               JsonElement $$5 = $$4.parse($$3).getAsJsonObject().get("errorMsg");
               Optional<String> $$6 = Optional.ofNullable($$5).map(JsonElement::getAsString);
               pUploadResult.withErrorMessage($$6.orElse(null));
            } catch (Exception var8) {
            }
         }
      }
   }

   private boolean shouldRetry(long pRetryDelaySeconds, int pRetries) {
      return pRetryDelaySeconds > 0L && pRetries + 1 < 5;
   }

   private UploadResult retryUploadAfter(long pSeconds, int pRetries) throws InterruptedException {
      Thread.sleep(Duration.ofSeconds(pSeconds).toMillis());
      return this.requestUpload(pRetries + 1);
   }

   private long getRetryDelaySeconds(HttpResponse pHttpResponse) {
      return Optional.ofNullable(pHttpResponse.getFirstHeader("Retry-After")).<String>map(NameValuePair::getValue).map(Long::valueOf).orElse(0L);
   }

   public boolean isFinished() {
      return this.uploadTask.isDone() || this.uploadTask.isCancelled();
   }

   @OnlyIn(Dist.CLIENT)
   static class CustomInputStreamEntity extends InputStreamEntity {
      private final long length;
      private final InputStream content;
      private final UploadStatus uploadStatus;

      public CustomInputStreamEntity(InputStream pContent, long pLength, UploadStatus pUploadStatus) {
         super(pContent);
         this.content = pContent;
         this.length = pLength;
         this.uploadStatus = pUploadStatus;
      }

      public void writeTo(OutputStream pOut) throws IOException {
         Args.notNull(pOut, "Output stream");
         InputStream $$1 = this.content;

         try {
            byte[] $$2 = new byte[4096];
            int $$3;
            if (this.length < 0L) {
               while (($$3 = $$1.read($$2)) != -1) {
                  pOut.write($$2, 0, $$3);
                  this.uploadStatus.bytesWritten += (long)$$3;
               }
            } else {
               long $$4 = this.length;

               while ($$4 > 0L) {
                  $$3 = $$1.read($$2, 0, (int)Math.min(4096L, $$4));
                  if ($$3 == -1) {
                     break;
                  }

                  pOut.write($$2, 0, $$3);
                  this.uploadStatus.bytesWritten += (long)$$3;
                  $$4 -= (long)$$3;
                  pOut.flush();
               }
            }
         } finally {
            $$1.close();
         }
      }
   }
}
