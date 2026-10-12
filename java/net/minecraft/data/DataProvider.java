package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public interface DataProvider {
   ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap(), p_236070_ -> {
      p_236070_.put("type", 0);
      p_236070_.put("parent", 1);
      p_236070_.defaultReturnValue(2);
   });
   Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(p_236077_ -> (String)p_236077_);
   Logger LOGGER = LogUtils.getLogger();

   CompletableFuture<?> run(CachedOutput pOutput);

   String getName();

   static <T> CompletableFuture<?> saveStable(CachedOutput pOutput, HolderLookup.Provider pRegistries, Codec<T> pCodec, T pValue, Path pPath) {
      RegistryOps<JsonElement> $$5 = pRegistries.createSerializationContext(JsonOps.INSTANCE);
      JsonElement $$6 = (JsonElement)pCodec.encodeStart($$5, pValue).getOrThrow();
      return saveStable(pOutput, $$6, pPath);
   }

   static CompletableFuture<?> saveStable(CachedOutput pOutput, JsonElement pJson, Path pPath) {
      return CompletableFuture.runAsync(() -> {
         try {
            ByteArrayOutputStream $$3 = new ByteArrayOutputStream();
            HashingOutputStream $$4 = new HashingOutputStream(Hashing.sha1(), $$3);
            JsonWriter $$5 = new JsonWriter(new OutputStreamWriter($$4, StandardCharsets.UTF_8));

            try {
               $$5.setSerializeNulls(false);
               $$5.setIndent("  ");
               GsonHelper.writeValue($$5, pJson, KEY_COMPARATOR);
            } catch (Throwable var9) {
               try {
                  $$5.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            $$5.close();
            pOutput.writeIfNeeded(pPath, $$3.toByteArray(), $$4.hash());
         } catch (IOException var10) {
            LOGGER.error("Failed to save file to {}", pPath, var10);
         }
      }, Util.backgroundExecutor());
   }

   @FunctionalInterface
   public interface Factory<T extends DataProvider> {
      T create(PackOutput pOutput);
   }
}
