package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
   public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling");
   public static final String METRICS_DIR_NAME = "metrics";
   public static final String DEVIATIONS_DIR_NAME = "deviations";
   public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String rootFolderName;

   public MetricsPersister(String pRootFolderName) {
      this.rootFolderName = pRootFolderName;
   }

   public Path saveReports(Set<MetricSampler> pSamplers, Map<MetricSampler, List<RecordedDeviation>> pDeviations, ProfileResults pResults) {
      try {
         Files.createDirectories(PROFILING_RESULTS_DIR);
      } catch (IOException var8) {
         throw new UncheckedIOException(var8);
      }

      try {
         Path $$4 = Files.createTempDirectory("minecraft-profiling");
         $$4.toFile().deleteOnExit();
         Files.createDirectories(PROFILING_RESULTS_DIR);
         Path $$5 = $$4.resolve(this.rootFolderName);
         Path $$6 = $$5.resolve("metrics");
         this.saveMetrics(pSamplers, $$6);
         if (!pDeviations.isEmpty()) {
            this.saveDeviations(pDeviations, $$5.resolve("deviations"));
         }

         this.saveProfilingTaskExecutionResult(pResults, $$5);
         return $$4;
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   private void saveMetrics(Set<MetricSampler> pSamplers, Path pPath) {
      if (pSamplers.isEmpty()) {
         throw new IllegalArgumentException("Expected at least one sampler to persist");
      } else {
         Map<MetricCategory, List<MetricSampler>> $$2 = pSamplers.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
         $$2.forEach((p_146232_, p_146233_) -> this.saveCategory(p_146232_, (List<MetricSampler>)p_146233_, pPath));
      }
   }

   private void saveCategory(MetricCategory pCategory, List<MetricSampler> pSamplers, Path pPath) {
      Path $$3 = pPath.resolve(Util.sanitizeName(pCategory.getDescription(), ResourceLocation::validPathChar) + ".csv");
      Writer $$4 = null;

      try {
         Files.createDirectories($$3.getParent());
         $$4 = Files.newBufferedWriter($$3, StandardCharsets.UTF_8);
         CsvOutput.Builder $$5 = CsvOutput.builder();
         $$5.addColumn("@tick");

         for (MetricSampler $$6 : pSamplers) {
            $$5.addColumn($$6.getName());
         }

         CsvOutput $$7 = $$5.build($$4);
         List<MetricSampler.SamplerResult> $$8 = pSamplers.stream().map(MetricSampler::result).collect(Collectors.toList());
         int $$9 = $$8.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
         int $$10 = $$8.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();

         for (int $$11 = $$9; $$11 <= $$10; $$11++) {
            int $$12 = $$11;
            Stream<String> $$13 = $$8.stream().map(p_146222_ -> String.valueOf(p_146222_.valueAtTick($$12)));
            Object[] $$14 = Stream.concat(Stream.of(String.valueOf($$11)), $$13).toArray(String[]::new);
            $$7.writeRow($$14);
         }

         LOGGER.info("Flushed metrics to {}", $$3);
      } catch (Exception var18) {
         LOGGER.error("Could not save profiler results to {}", $$3, var18);
      } finally {
         IOUtils.closeQuietly($$4);
      }
   }

   private void saveDeviations(Map<MetricSampler, List<RecordedDeviation>> pDeviations, Path pPath) {
      DateTimeFormatter $$2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
      pDeviations.forEach(
         (p_146242_, p_146243_) -> p_146243_.forEach(
               p_146238_ -> {
                  String $$4 = $$2.format(p_146238_.timestamp);
                  Path $$5 = pPath.resolve(Util.sanitizeName(p_146242_.getName(), ResourceLocation::validPathChar))
                     .resolve(String.format(Locale.ROOT, "%d@%s.txt", p_146238_.tick, $$4));
                  p_146238_.profilerResultAtTick.saveResults($$5);
               }
            )
      );
   }

   private void saveProfilingTaskExecutionResult(ProfileResults pResults, Path pOutputPath) {
      pResults.saveResults(pOutputPath.resolve("profiling.txt"));
   }
}
