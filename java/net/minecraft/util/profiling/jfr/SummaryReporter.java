package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.profiling.jfr.parse.JfrStatsParser;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class SummaryReporter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Runnable onDeregistration;

   protected SummaryReporter(Runnable pOnDeregistration) {
      this.onDeregistration = pOnDeregistration;
   }

   public void recordingStopped(@Nullable Path pOutputPath) {
      if (pOutputPath != null) {
         this.onDeregistration.run();
         infoWithFallback(() -> "Dumped flight recorder profiling to " + pOutputPath);

         JfrStatsResult $$1;
         try {
            $$1 = JfrStatsParser.parse(pOutputPath);
         } catch (Throwable var5) {
            warnWithFallback(() -> "Failed to parse JFR recording", var5);
            return;
         }

         try {
            infoWithFallback($$1::asJson);
            Path $$4 = pOutputPath.resolveSibling("jfr-report-" + StringUtils.substringBefore(pOutputPath.getFileName().toString(), ".jfr") + ".json");
            Files.writeString($$4, $$1.asJson(), StandardOpenOption.CREATE);
            infoWithFallback(() -> "Dumped recording summary to " + $$4);
         } catch (Throwable var4) {
            warnWithFallback(() -> "Failed to output JFR report", var4);
         }
      }
   }

   private static void infoWithFallback(Supplier<String> pMessage) {
      if (LogUtils.isLoggerActive()) {
         LOGGER.info(pMessage.get());
      } else {
         Bootstrap.realStdoutPrintln(pMessage.get());
      }
   }

   private static void warnWithFallback(Supplier<String> pMessage, Throwable pThrowable) {
      if (LogUtils.isLoggerActive()) {
         LOGGER.warn(pMessage.get(), pThrowable);
      } else {
         Bootstrap.realStdoutPrintln(pMessage.get());
         pThrowable.printStackTrace(Bootstrap.STDOUT);
      }
   }
}
