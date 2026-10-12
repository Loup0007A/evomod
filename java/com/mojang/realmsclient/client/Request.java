package com.mojang.realmsclient.client;

import com.mojang.realmsclient.exception.RealmsHttpException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Request<T extends Request<T>> {
   protected HttpURLConnection connection;
   private boolean connected;
   protected String url;
   private static final int DEFAULT_READ_TIMEOUT = 60000;
   private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
   private static final String IS_SNAPSHOT_KEY = "Is-Prerelease";
   private static final String COOKIE_KEY = "Cookie";

   public Request(String pUrl, int pConnectTimeout, int pReadTimeout) {
      try {
         this.url = pUrl;
         Proxy $$3 = RealmsClientConfig.getProxy();
         if ($$3 != null) {
            this.connection = (HttpURLConnection)new URL(pUrl).openConnection($$3);
         } else {
            this.connection = (HttpURLConnection)new URL(pUrl).openConnection();
         }

         this.connection.setConnectTimeout(pConnectTimeout);
         this.connection.setReadTimeout(pReadTimeout);
      } catch (MalformedURLException var5) {
         throw new RealmsHttpException(var5.getMessage(), var5);
      } catch (IOException var6) {
         throw new RealmsHttpException(var6.getMessage(), var6);
      }
   }

   public void cookie(String pKey, String pValue) {
      cookie(this.connection, pKey, pValue);
   }

   public static void cookie(HttpURLConnection pConnection, String pKey, String pValue) {
      String $$3 = pConnection.getRequestProperty("Cookie");
      if ($$3 == null) {
         pConnection.setRequestProperty("Cookie", pKey + "=" + pValue);
      } else {
         pConnection.setRequestProperty("Cookie", $$3 + ";" + pKey + "=" + pValue);
      }
   }

   public void addSnapshotHeader(boolean pIsSnapshot) {
      this.connection.addRequestProperty("Is-Prerelease", String.valueOf(pIsSnapshot));
   }

   public int getRetryAfterHeader() {
      return getRetryAfterHeader(this.connection);
   }

   public static int getRetryAfterHeader(HttpURLConnection pConnection) {
      String $$1 = pConnection.getHeaderField("Retry-After");

      try {
         return Integer.valueOf($$1);
      } catch (Exception var3) {
         return 5;
      }
   }

   public int responseCode() {
      try {
         this.connect();
         return this.connection.getResponseCode();
      } catch (Exception var2) {
         throw new RealmsHttpException(var2.getMessage(), var2);
      }
   }

   public String text() {
      try {
         this.connect();
         String $$0;
         if (this.responseCode() >= 400) {
            $$0 = this.read(this.connection.getErrorStream());
         } else {
            $$0 = this.read(this.connection.getInputStream());
         }

         this.dispose();
         return $$0;
      } catch (IOException var2) {
         throw new RealmsHttpException(var2.getMessage(), var2);
      }
   }

   private String read(@Nullable InputStream pIn) throws IOException {
      if (pIn == null) {
         return "";
      } else {
         InputStreamReader $$1 = new InputStreamReader(pIn, StandardCharsets.UTF_8);
         StringBuilder $$2 = new StringBuilder();

         for (int $$3 = $$1.read(); $$3 != -1; $$3 = $$1.read()) {
            $$2.append((char)$$3);
         }

         return $$2.toString();
      }
   }

   private void dispose() {
      byte[] $$0 = new byte[1024];

      try {
         InputStream $$1 = this.connection.getInputStream();

         while ($$1.read($$0) > 0) {
         }

         $$1.close();
         return;
      } catch (Exception var9) {
         try {
            InputStream $$3 = this.connection.getErrorStream();
            if ($$3 != null) {
               while ($$3.read($$0) > 0) {
               }

               $$3.close();
               return;
            }
         } catch (IOException var8) {
            return;
         }
      } finally {
         if (this.connection != null) {
            this.connection.disconnect();
         }
      }
   }

   protected T connect() {
      if (this.connected) {
         return (T)this;
      } else {
         T $$0 = this.doConnect();
         this.connected = true;
         return $$0;
      }
   }

   protected abstract T doConnect();

   public static Request<?> get(String pUrl) {
      return new Request.Get(pUrl, 5000, 60000);
   }

   public static Request<?> get(String pUrl, int pConnectTimeout, int pReadTimeout) {
      return new Request.Get(pUrl, pConnectTimeout, pReadTimeout);
   }

   public static Request<?> post(String pUrl, String pContent) {
      return new Request.Post(pUrl, pContent, 5000, 60000);
   }

   public static Request<?> post(String pUrl, String pContent, int pConnectTimeout, int pReadTimeout) {
      return new Request.Post(pUrl, pContent, pConnectTimeout, pReadTimeout);
   }

   public static Request<?> delete(String pUrl) {
      return new Request.Delete(pUrl, 5000, 60000);
   }

   public static Request<?> put(String pUrl, String pContent) {
      return new Request.Put(pUrl, pContent, 5000, 60000);
   }

   public static Request<?> put(String pUrl, String pContent, int pConnectTimeout, int pReadTimeout) {
      return new Request.Put(pUrl, pContent, pConnectTimeout, pReadTimeout);
   }

   public String getHeader(String pName) {
      return getHeader(this.connection, pName);
   }

   public static String getHeader(HttpURLConnection pConnection, String pName) {
      try {
         return pConnection.getHeaderField(pName);
      } catch (Exception var3) {
         return "";
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Delete extends Request<Request.Delete> {
      public Delete(String p_87359_, int p_87360_, int p_87361_) {
         super(p_87359_, p_87360_, p_87361_);
      }

      public Request.Delete doConnect() {
         try {
            this.connection.setDoOutput(true);
            this.connection.setRequestMethod("DELETE");
            this.connection.connect();
            return this;
         } catch (Exception var2) {
            throw new RealmsHttpException(var2.getMessage(), var2);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Get extends Request<Request.Get> {
      public Get(String p_87365_, int p_87366_, int p_87367_) {
         super(p_87365_, p_87366_, p_87367_);
      }

      public Request.Get doConnect() {
         try {
            this.connection.setDoInput(true);
            this.connection.setDoOutput(true);
            this.connection.setUseCaches(false);
            this.connection.setRequestMethod("GET");
            return this;
         } catch (Exception var2) {
            throw new RealmsHttpException(var2.getMessage(), var2);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Post extends Request<Request.Post> {
      private final String content;

      public Post(String pUrl, String pContent, int pConnectTimeout, int pReadTimeout) {
         super(pUrl, pConnectTimeout, pReadTimeout);
         this.content = pContent;
      }

      public Request.Post doConnect() {
         try {
            if (this.content != null) {
               this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            }

            this.connection.setDoInput(true);
            this.connection.setDoOutput(true);
            this.connection.setUseCaches(false);
            this.connection.setRequestMethod("POST");
            OutputStream $$0 = this.connection.getOutputStream();
            OutputStreamWriter $$1 = new OutputStreamWriter($$0, "UTF-8");
            $$1.write(this.content);
            $$1.close();
            $$0.flush();
            return this;
         } catch (Exception var3) {
            throw new RealmsHttpException(var3.getMessage(), var3);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Put extends Request<Request.Put> {
      private final String content;

      public Put(String pUrl, String pContent, int pConnectTimeout, int pReadTimeout) {
         super(pUrl, pConnectTimeout, pReadTimeout);
         this.content = pContent;
      }

      public Request.Put doConnect() {
         try {
            if (this.content != null) {
               this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            }

            this.connection.setDoOutput(true);
            this.connection.setDoInput(true);
            this.connection.setRequestMethod("PUT");
            OutputStream $$0 = this.connection.getOutputStream();
            OutputStreamWriter $$1 = new OutputStreamWriter($$0, "UTF-8");
            $$1.write(this.content);
            $$1.close();
            $$0.flush();
            return this;
         } catch (Exception var3) {
            throw new RealmsHttpException(var3.getMessage(), var3);
         }
      }
   }
}
