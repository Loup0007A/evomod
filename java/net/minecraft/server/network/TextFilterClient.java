package net.minecraft.server.network;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

public class TextFilterClient implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
   private static final ThreadFactory THREAD_FACTORY = p_10148_ -> {
      Thread $$1 = new Thread(p_10148_);
      $$1.setName("Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
      return $$1;
   };
   private static final String DEFAULT_ENDPOINT = "v1/chat";
   private final URL chatEndpoint;
   private final TextFilterClient.MessageEncoder chatEncoder;
   final URL joinEndpoint;
   final TextFilterClient.JoinOrLeaveEncoder joinEncoder;
   final URL leaveEndpoint;
   final TextFilterClient.JoinOrLeaveEncoder leaveEncoder;
   private final String authKey;
   final TextFilterClient.IgnoreStrategy chatIgnoreStrategy;
   final ExecutorService workerPool;

   private TextFilterClient(
      URL pChatEndpoint,
      TextFilterClient.MessageEncoder pChatEncoder,
      URL pJoinEndpoint,
      TextFilterClient.JoinOrLeaveEncoder pJoinEncoder,
      URL pLeaveEndpoint,
      TextFilterClient.JoinOrLeaveEncoder pLeaveEncoder,
      String pAuthKey,
      TextFilterClient.IgnoreStrategy pChatIgnoreStrategy,
      int pWorkerThreadCount
   ) {
      this.authKey = pAuthKey;
      this.chatIgnoreStrategy = pChatIgnoreStrategy;
      this.chatEndpoint = pChatEndpoint;
      this.chatEncoder = pChatEncoder;
      this.joinEndpoint = pJoinEndpoint;
      this.joinEncoder = pJoinEncoder;
      this.leaveEndpoint = pLeaveEndpoint;
      this.leaveEncoder = pLeaveEncoder;
      this.workerPool = Executors.newFixedThreadPool(pWorkerThreadCount, THREAD_FACTORY);
   }

   private static URL getEndpoint(URI pUri, @Nullable JsonObject pJson, String pMemberName, String pFallback) throws MalformedURLException {
      String $$4 = getEndpointFromConfig(pJson, pMemberName, pFallback);
      return pUri.resolve("/" + $$4).toURL();
   }

   private static String getEndpointFromConfig(@Nullable JsonObject pJson, String pMemberName, String pFallback) {
      return pJson != null ? GsonHelper.getAsString(pJson, pMemberName, pFallback) : pFallback;
   }

   @Nullable
   public static TextFilterClient createFromConfig(String pConfig) {
      if (Strings.isNullOrEmpty(pConfig)) {
         return null;
      } else {
         try {
            JsonObject $$1 = GsonHelper.parse(pConfig);
            URI $$2 = new URI(GsonHelper.getAsString($$1, "apiServer"));
            String $$3 = GsonHelper.getAsString($$1, "apiKey");
            if ($$3.isEmpty()) {
               throw new IllegalArgumentException("Missing API key");
            } else {
               int $$4 = GsonHelper.getAsInt($$1, "ruleId", 1);
               String $$5 = GsonHelper.getAsString($$1, "serverId", "");
               String $$6 = GsonHelper.getAsString($$1, "roomId", "Java:Chat");
               int $$7 = GsonHelper.getAsInt($$1, "hashesToDrop", -1);
               int $$8 = GsonHelper.getAsInt($$1, "maxConcurrentRequests", 7);
               JsonObject $$9 = GsonHelper.getAsJsonObject($$1, "endpoints", null);
               String $$10 = getEndpointFromConfig($$9, "chat", "v1/chat");
               boolean $$11 = $$10.equals("v1/chat");
               URL $$12 = $$2.resolve("/" + $$10).toURL();
               URL $$13 = getEndpoint($$2, $$9, "join", "v1/join");
               URL $$14 = getEndpoint($$2, $$9, "leave", "v1/leave");
               TextFilterClient.JoinOrLeaveEncoder $$15 = p_215310_ -> {
                  JsonObject $$3x = new JsonObject();
                  $$3x.addProperty("server", $$5);
                  $$3x.addProperty("room", $$6);
                  $$3x.addProperty("user_id", p_215310_.getId().toString());
                  $$3x.addProperty("user_display_name", p_215310_.getName());
                  return $$3x;
               };
               TextFilterClient.MessageEncoder $$16;
               if ($$11) {
                  $$16 = (p_238214_, p_238215_) -> {
                     JsonObject $$5x = new JsonObject();
                     $$5x.addProperty("rule", $$4);
                     $$5x.addProperty("server", $$5);
                     $$5x.addProperty("room", $$6);
                     $$5x.addProperty("player", p_238214_.getId().toString());
                     $$5x.addProperty("player_display_name", p_238214_.getName());
                     $$5x.addProperty("text", p_238215_);
                     $$5x.addProperty("language", "*");
                     return $$5x;
                  };
               } else {
                  String $$17 = String.valueOf($$4);
                  $$16 = (p_238220_, p_238221_) -> {
                     JsonObject $$5x = new JsonObject();
                     $$5x.addProperty("rule_id", $$17);
                     $$5x.addProperty("category", $$5);
                     $$5x.addProperty("subcategory", $$6);
                     $$5x.addProperty("user_id", p_238220_.getId().toString());
                     $$5x.addProperty("user_display_name", p_238220_.getName());
                     $$5x.addProperty("text", p_238221_);
                     $$5x.addProperty("language", "*");
                     return $$5x;
                  };
               }

               TextFilterClient.IgnoreStrategy $$19 = TextFilterClient.IgnoreStrategy.select($$7);
               String $$20 = Base64.getEncoder().encodeToString($$3.getBytes(StandardCharsets.US_ASCII));
               return new TextFilterClient($$12, $$16, $$13, $$15, $$14, $$15, $$20, $$19, $$8);
            }
         } catch (Exception var19) {
            LOGGER.warn("Failed to parse chat filter config {}", pConfig, var19);
            return null;
         }
      }
   }

   void processJoinOrLeave(GameProfile pProfile, URL pUrl, TextFilterClient.JoinOrLeaveEncoder pEncoder, Executor pExecutor) {
      pExecutor.execute(() -> {
         JsonObject $$3 = pEncoder.encode(pProfile);

         try {
            this.processRequest($$3, pUrl);
         } catch (Exception var6) {
            LOGGER.warn("Failed to send join/leave packet to {} for player {}", new Object[]{pUrl, pProfile, var6});
         }
      });
   }

   CompletableFuture<FilteredText> requestMessageProcessing(
      GameProfile pProfile, String pText, TextFilterClient.IgnoreStrategy pIgnoreStrategy, Executor pExecutor
   ) {
      return pText.isEmpty() ? CompletableFuture.completedFuture(FilteredText.EMPTY) : CompletableFuture.supplyAsync(() -> {
         JsonObject $$3 = this.chatEncoder.encode(pProfile, pText);

         try {
            JsonObject $$4 = this.processRequestResponse($$3, this.chatEndpoint);
            boolean $$5 = GsonHelper.getAsBoolean($$4, "response", false);
            if ($$5) {
               return FilteredText.passThrough(pText);
            } else {
               String $$6 = GsonHelper.getAsString($$4, "hashed", null);
               if ($$6 == null) {
                  return FilteredText.fullyFiltered(pText);
               } else {
                  JsonArray $$7 = GsonHelper.getAsJsonArray($$4, "hashes");
                  FilterMask $$8 = this.parseMask(pText, $$7, pIgnoreStrategy);
                  return new FilteredText(pText, $$8);
               }
            }
         } catch (Exception var10) {
            LOGGER.warn("Failed to validate message '{}'", pText, var10);
            return FilteredText.fullyFiltered(pText);
         }
      }, pExecutor);
   }

   private FilterMask parseMask(String pText, JsonArray pHashes, TextFilterClient.IgnoreStrategy pIgnoreStrategy) {
      if (pHashes.isEmpty()) {
         return FilterMask.PASS_THROUGH;
      } else if (pIgnoreStrategy.shouldIgnore(pText, pHashes.size())) {
         return FilterMask.FULLY_FILTERED;
      } else {
         FilterMask $$3 = new FilterMask(pText.length());

         for (int $$4 = 0; $$4 < pHashes.size(); $$4++) {
            $$3.setFiltered(pHashes.get($$4).getAsInt());
         }

         return $$3;
      }
   }

   @Override
   public void close() {
      this.workerPool.shutdownNow();
   }

   private void drainStream(InputStream pStream) throws IOException {
      byte[] $$1 = new byte[1024];

      while (pStream.read($$1) != -1) {
      }
   }

   private JsonObject processRequestResponse(JsonObject pJson, URL pRequestUrl) throws IOException {
      HttpURLConnection $$2 = this.makeRequest(pJson, pRequestUrl);

      JsonObject var5;
      try (InputStream $$3 = $$2.getInputStream()) {
         if ($$2.getResponseCode() == 204) {
            return new JsonObject();
         }

         try {
            var5 = Streams.parse(new JsonReader(new InputStreamReader($$3, StandardCharsets.UTF_8))).getAsJsonObject();
         } finally {
            this.drainStream($$3);
         }
      }

      return var5;
   }

   private void processRequest(JsonObject pJson, URL pRequestUrl) throws IOException {
      HttpURLConnection $$2 = this.makeRequest(pJson, pRequestUrl);

      try (InputStream $$3 = $$2.getInputStream()) {
         this.drainStream($$3);
      }
   }

   private HttpURLConnection makeRequest(JsonObject pJson, URL pRequestUrl) throws IOException {
      HttpURLConnection $$2 = (HttpURLConnection)pRequestUrl.openConnection();
      $$2.setConnectTimeout(15000);
      $$2.setReadTimeout(2000);
      $$2.setUseCaches(false);
      $$2.setDoOutput(true);
      $$2.setDoInput(true);
      $$2.setRequestMethod("POST");
      $$2.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      $$2.setRequestProperty("Accept", "application/json");
      $$2.setRequestProperty("Authorization", "Basic " + this.authKey);
      $$2.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().getName());
      OutputStreamWriter $$3 = new OutputStreamWriter($$2.getOutputStream(), StandardCharsets.UTF_8);

      try {
         JsonWriter $$4 = new JsonWriter($$3);

         try {
            Streams.write(pJson, $$4);
         } catch (Throwable var10) {
            try {
               $$4.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         $$4.close();
      } catch (Throwable var11) {
         try {
            $$3.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      $$3.close();
      int $$5 = $$2.getResponseCode();
      if ($$5 >= 200 && $$5 < 300) {
         return $$2;
      } else {
         throw new TextFilterClient.RequestFailedException($$5 + " " + $$2.getResponseMessage());
      }
   }

   public TextFilter createContext(GameProfile pProfile) {
      return new TextFilterClient.PlayerContext(pProfile);
   }

   @FunctionalInterface
   public interface IgnoreStrategy {
      TextFilterClient.IgnoreStrategy NEVER_IGNORE = (p_10169_, p_10170_) -> false;
      TextFilterClient.IgnoreStrategy IGNORE_FULLY_FILTERED = (p_10166_, p_10167_) -> p_10166_.length() == p_10167_;

      static TextFilterClient.IgnoreStrategy ignoreOverThreshold(int pThreshold) {
         return (p_143742_, p_143743_) -> p_143743_ >= pThreshold;
      }

      static TextFilterClient.IgnoreStrategy select(int pThreshold) {
         return switch (pThreshold) {
            case -1 -> NEVER_IGNORE;
            case 0 -> IGNORE_FULLY_FILTERED;
            default -> ignoreOverThreshold(pThreshold);
         };
      }

      boolean shouldIgnore(String pText, int pThreshold);
   }

   @FunctionalInterface
   interface JoinOrLeaveEncoder {
      JsonObject encode(GameProfile pProfile);
   }

   @FunctionalInterface
   interface MessageEncoder {
      JsonObject encode(GameProfile pProfile, String pText);
   }

   class PlayerContext implements TextFilter {
      private final GameProfile profile;
      private final Executor streamExecutor;

      PlayerContext(final GameProfile pProfile) {
         this.profile = pProfile;
         ProcessorMailbox<Runnable> $$1 = ProcessorMailbox.create(TextFilterClient.this.workerPool, "chat stream for " + pProfile.getName());
         this.streamExecutor = $$1::tell;
      }

      @Override
      public void join() {
         TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.joinEndpoint, TextFilterClient.this.joinEncoder, this.streamExecutor);
      }

      @Override
      public void leave() {
         TextFilterClient.this.processJoinOrLeave(this.profile, TextFilterClient.this.leaveEndpoint, TextFilterClient.this.leaveEncoder, this.streamExecutor);
      }

      @Override
      public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> pTexts) {
         List<CompletableFuture<FilteredText>> $$1 = pTexts.stream()
            .map(
               p_10195_ -> TextFilterClient.this.requestMessageProcessing(this.profile, p_10195_, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor)
            )
            .collect(ImmutableList.toImmutableList());
         return Util.sequenceFailFast($$1).exceptionally(p_143747_ -> ImmutableList.of());
      }

      @Override
      public CompletableFuture<FilteredText> processStreamMessage(String pText) {
         return TextFilterClient.this.requestMessageProcessing(this.profile, pText, TextFilterClient.this.chatIgnoreStrategy, this.streamExecutor);
      }
   }

   public static class RequestFailedException extends RuntimeException {
      RequestFailedException(String pMessage) {
         super(pMessage);
      }
   }
}
