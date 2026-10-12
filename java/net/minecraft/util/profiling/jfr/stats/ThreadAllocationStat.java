package net.minecraft.util.profiling.jfr.stats;

import com.google.common.base.MoreObjects;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public record ThreadAllocationStat(Instant timestamp, String threadName, long totalBytes) {
   private static final String UNKNOWN_THREAD = "unknown";

   public static ThreadAllocationStat from(RecordedEvent pEvent) {
      RecordedThread $$1 = pEvent.getThread("thread");
      String $$2 = $$1 == null ? "unknown" : (String)MoreObjects.firstNonNull($$1.getJavaName(), "unknown");
      return new ThreadAllocationStat(pEvent.getStartTime(), $$2, pEvent.getLong("allocated"));
   }

   public static ThreadAllocationStat.Summary summary(List<ThreadAllocationStat> pStats) {
      Map<String, Double> $$1 = new TreeMap<>();
      Map<String, List<ThreadAllocationStat>> $$2 = pStats.stream().collect(Collectors.groupingBy(p_185796_ -> p_185796_.threadName));
      $$2.forEach((p_185801_, p_185802_) -> {
         if (p_185802_.size() >= 2) {
            ThreadAllocationStat $$3 = p_185802_.get(0);
            ThreadAllocationStat $$4 = p_185802_.get(p_185802_.size() - 1);
            long $$5 = Duration.between($$3.timestamp, $$4.timestamp).getSeconds();
            long $$6 = $$4.totalBytes - $$3.totalBytes;
            $$1.put(p_185801_, (double)$$6 / (double)$$5);
         }
      });
      return new ThreadAllocationStat.Summary($$1);
   }

   public static record Summary(Map<String, Double> allocationsPerSecondByThread) {
   }
}
