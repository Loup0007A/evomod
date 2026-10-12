package net.minecraft.util.profiling.jfr.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public record GcHeapStat(Instant timestamp, long heapUsed, GcHeapStat.Timing timing) {
   public static GcHeapStat from(RecordedEvent pEvent) {
      return new GcHeapStat(
         pEvent.getStartTime(),
         pEvent.getLong("heapUsed"),
         pEvent.getString("when").equalsIgnoreCase("before gc") ? GcHeapStat.Timing.BEFORE_GC : GcHeapStat.Timing.AFTER_GC
      );
   }

   public static GcHeapStat.Summary summary(Duration pDuration, List<GcHeapStat> pStats, Duration pGcTotalDuration, int pTotalGCs) {
      return new GcHeapStat.Summary(pDuration, pGcTotalDuration, pTotalGCs, calculateAllocationRatePerSecond(pStats));
   }

   private static double calculateAllocationRatePerSecond(List<GcHeapStat> pStats) {
      long $$1 = 0L;
      Map<GcHeapStat.Timing, List<GcHeapStat>> $$2 = pStats.stream().collect(Collectors.groupingBy(p_185689_ -> p_185689_.timing));
      List<GcHeapStat> $$3 = $$2.get(GcHeapStat.Timing.BEFORE_GC);
      List<GcHeapStat> $$4 = $$2.get(GcHeapStat.Timing.AFTER_GC);

      for (int $$5 = 1; $$5 < $$3.size(); $$5++) {
         GcHeapStat $$6 = $$3.get($$5);
         GcHeapStat $$7 = $$4.get($$5 - 1);
         $$1 += $$6.heapUsed - $$7.heapUsed;
      }

      Duration $$8 = Duration.between(pStats.get(1).timestamp, pStats.get(pStats.size() - 1).timestamp);
      return (double)$$1 / (double)$$8.getSeconds();
   }

   public static record Summary(Duration duration, Duration gcTotalDuration, int totalGCs, double allocationRateBytesPerSecond) {
      public float gcOverHead() {
         return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
      }
   }

   static enum Timing {
      BEFORE_GC,
      AFTER_GC;
   }
}
