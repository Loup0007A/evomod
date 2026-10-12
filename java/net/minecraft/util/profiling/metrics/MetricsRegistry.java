package net.minecraft.util.profiling.metrics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class MetricsRegistry {
   public static final MetricsRegistry INSTANCE = new MetricsRegistry();
   private final WeakHashMap<ProfilerMeasured, Void> measuredInstances = new WeakHashMap<>();

   private MetricsRegistry() {
   }

   public void add(ProfilerMeasured pKey) {
      this.measuredInstances.put(pKey, null);
   }

   public List<MetricSampler> getRegisteredSamplers() {
      Map<String, List<MetricSampler>> $$0 = this.measuredInstances
         .keySet()
         .stream()
         .flatMap(p_146079_ -> p_146079_.profiledMetrics().stream())
         .collect(Collectors.groupingBy(MetricSampler::getName));
      return aggregateDuplicates($$0);
   }

   private static List<MetricSampler> aggregateDuplicates(Map<String, List<MetricSampler>> pSamplers) {
      return pSamplers.entrySet().stream().map(p_146075_ -> {
         String $$1 = p_146075_.getKey();
         List<MetricSampler> $$2 = p_146075_.getValue();
         return (MetricSampler)($$2.size() > 1 ? new MetricsRegistry.AggregatedMetricSampler($$1, $$2) : $$2.get(0));
      }).collect(Collectors.toList());
   }

   static class AggregatedMetricSampler extends MetricSampler {
      private final List<MetricSampler> delegates;

      AggregatedMetricSampler(String pName, List<MetricSampler> pDelegates) {
         super(pName, pDelegates.get(0).getCategory(), () -> averageValueFromDelegates(pDelegates), () -> beforeTick(pDelegates), thresholdTest(pDelegates));
         this.delegates = pDelegates;
      }

      private static MetricSampler.ThresholdTest thresholdTest(List<MetricSampler> pSamplers) {
         return p_146091_ -> pSamplers.stream().anyMatch(p_146086_ -> p_146086_.thresholdTest != null ? p_146086_.thresholdTest.test(p_146091_) : false);
      }

      private static void beforeTick(List<MetricSampler> pSamplers) {
         for (MetricSampler $$1 : pSamplers) {
            $$1.onStartTick();
         }
      }

      private static double averageValueFromDelegates(List<MetricSampler> pSamplers) {
         double $$1 = 0.0;

         for (MetricSampler $$2 : pSamplers) {
            $$1 += $$2.getSampler().getAsDouble();
         }

         return $$1 / (double)pSamplers.size();
      }

      @Override
      public boolean equals(@Nullable Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther == null || this.getClass() != pOther.getClass()) {
            return false;
         } else if (!super.equals(pOther)) {
            return false;
         } else {
            MetricsRegistry.AggregatedMetricSampler $$1 = (MetricsRegistry.AggregatedMetricSampler)pOther;
            return this.delegates.equals($$1.delegates);
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(super.hashCode(), this.delegates);
      }
   }
}
