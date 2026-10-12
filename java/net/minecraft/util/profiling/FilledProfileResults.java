package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ReportType;
import net.minecraft.SharedConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class FilledProfileResults implements ProfileResults {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProfilerPathEntry EMPTY = new ProfilerPathEntry() {
      @Override
      public long getDuration() {
         return 0L;
      }

      @Override
      public long getMaxDuration() {
         return 0L;
      }

      @Override
      public long getCount() {
         return 0L;
      }

      @Override
      public Object2LongMap<String> getCounters() {
         return Object2LongMaps.emptyMap();
      }
   };
   private static final Splitter SPLITTER = Splitter.on('\u001e');
   private static final Comparator<Entry<String, FilledProfileResults.CounterCollector>> COUNTER_ENTRY_COMPARATOR = Entry.<String, FilledProfileResults.CounterCollector>comparingByValue(
         Comparator.comparingLong(p_18489_ -> p_18489_.totalValue)
      )
      .reversed();
   private final Map<String, ? extends ProfilerPathEntry> entries;
   private final long startTimeNano;
   private final int startTimeTicks;
   private final long endTimeNano;
   private final int endTimeTicks;
   private final int tickDuration;

   public FilledProfileResults(
      Map<String, ? extends ProfilerPathEntry> pEntries, long pStartTimeNano, int pStartTimeTicks, long pEndTimeNano, int pEndTimeTicks
   ) {
      this.entries = pEntries;
      this.startTimeNano = pStartTimeNano;
      this.startTimeTicks = pStartTimeTicks;
      this.endTimeNano = pEndTimeNano;
      this.endTimeTicks = pEndTimeTicks;
      this.tickDuration = pEndTimeTicks - pStartTimeTicks;
   }

   private ProfilerPathEntry getEntry(String pKey) {
      ProfilerPathEntry $$1 = this.entries.get(pKey);
      return $$1 != null ? $$1 : EMPTY;
   }

   @Override
   public List<ResultField> getTimes(String pSectionPath) {
      String $$1 = pSectionPath;
      ProfilerPathEntry $$2 = this.getEntry("root");
      long $$3 = $$2.getDuration();
      ProfilerPathEntry $$4 = this.getEntry(pSectionPath);
      long $$5 = $$4.getDuration();
      long $$6 = $$4.getCount();
      List<ResultField> $$7 = Lists.newArrayList();
      if (!pSectionPath.isEmpty()) {
         pSectionPath = pSectionPath + "\u001e";
      }

      long $$8 = 0L;

      for (String $$9 : this.entries.keySet()) {
         if (isDirectChild(pSectionPath, $$9)) {
            $$8 += this.getEntry($$9).getDuration();
         }
      }

      float $$10 = (float)$$8;
      if ($$8 < $$5) {
         $$8 = $$5;
      }

      if ($$3 < $$8) {
         $$3 = $$8;
      }

      for (String $$11 : this.entries.keySet()) {
         if (isDirectChild(pSectionPath, $$11)) {
            ProfilerPathEntry $$12 = this.getEntry($$11);
            long $$13 = $$12.getDuration();
            double $$14 = (double)$$13 * 100.0 / (double)$$8;
            double $$15 = (double)$$13 * 100.0 / (double)$$3;
            String $$16 = $$11.substring(pSectionPath.length());
            $$7.add(new ResultField($$16, $$14, $$15, $$12.getCount()));
         }
      }

      if ((float)$$8 > $$10) {
         $$7.add(new ResultField("unspecified", (double)((float)$$8 - $$10) * 100.0 / (double)$$8, (double)((float)$$8 - $$10) * 100.0 / (double)$$3, $$6));
      }

      Collections.sort($$7);
      $$7.add(0, new ResultField($$1, 100.0, (double)$$8 * 100.0 / (double)$$3, $$6));
      return $$7;
   }

   private static boolean isDirectChild(String pSectionPath, String pEntry) {
      return pEntry.length() > pSectionPath.length() && pEntry.startsWith(pSectionPath) && pEntry.indexOf(30, pSectionPath.length() + 1) < 0;
   }

   private Map<String, FilledProfileResults.CounterCollector> getCounterValues() {
      Map<String, FilledProfileResults.CounterCollector> $$0 = Maps.newTreeMap();
      this.entries
         .forEach(
            (p_18512_, p_18513_) -> {
               Object2LongMap<String> $$3 = p_18513_.getCounters();
               if (!$$3.isEmpty()) {
                  List<String> $$4 = SPLITTER.splitToList(p_18512_);
                  $$3.forEach(
                     (p_145944_, p_145945_) -> $$0.computeIfAbsent(p_145944_, p_145947_ -> new FilledProfileResults.CounterCollector())
                           .addValue($$4.iterator(), p_145945_)
                  );
               }
            }
         );
      return $$0;
   }

   @Override
   public long getStartTimeNano() {
      return this.startTimeNano;
   }

   @Override
   public int getStartTimeTicks() {
      return this.startTimeTicks;
   }

   @Override
   public long getEndTimeNano() {
      return this.endTimeNano;
   }

   @Override
   public int getEndTimeTicks() {
      return this.endTimeTicks;
   }

   @Override
   public boolean saveResults(Path pPath) {
      Writer $$1 = null;

      boolean var4;
      try {
         Files.createDirectories(pPath.getParent());
         $$1 = Files.newBufferedWriter(pPath, StandardCharsets.UTF_8);
         $$1.write(this.getProfilerResults(this.getNanoDuration(), this.getTickDuration()));
         return true;
      } catch (Throwable var8) {
         LOGGER.error("Could not save profiler results to {}", pPath, var8);
         var4 = false;
      } finally {
         IOUtils.closeQuietly($$1);
      }

      return var4;
   }

   protected String getProfilerResults(long pTimeSpan, int pTickSpan) {
      StringBuilder $$2 = new StringBuilder();
      ReportType.PROFILE.appendHeader($$2, List.of());
      $$2.append("Version: ").append(SharedConstants.getCurrentVersion().getId()).append('\n');
      $$2.append("Time span: ").append(pTimeSpan / 1000000L).append(" ms\n");
      $$2.append("Tick span: ").append(pTickSpan).append(" ticks\n");
      $$2.append("// This is approximately ")
         .append(String.format(Locale.ROOT, "%.2f", (float)pTickSpan / ((float)pTimeSpan / 1.0E9F)))
         .append(" ticks per second. It should be ")
         .append(20)
         .append(" ticks per second\n\n");
      $$2.append("--- BEGIN PROFILE DUMP ---\n\n");
      this.appendProfilerResults(0, "root", $$2);
      $$2.append("--- END PROFILE DUMP ---\n\n");
      Map<String, FilledProfileResults.CounterCollector> $$3 = this.getCounterValues();
      if (!$$3.isEmpty()) {
         $$2.append("--- BEGIN COUNTER DUMP ---\n\n");
         this.appendCounters($$3, $$2, pTickSpan);
         $$2.append("--- END COUNTER DUMP ---\n\n");
      }

      return $$2.toString();
   }

   @Override
   public String getProfilerResults() {
      StringBuilder $$0 = new StringBuilder();
      this.appendProfilerResults(0, "root", $$0);
      return $$0.toString();
   }

   private static StringBuilder indentLine(StringBuilder pBuilder, int pIndents) {
      pBuilder.append(String.format(Locale.ROOT, "[%02d] ", pIndents));

      for (int $$2 = 0; $$2 < pIndents; $$2++) {
         pBuilder.append("|   ");
      }

      return pBuilder;
   }

   private void appendProfilerResults(int pDepth, String pSectionPath, StringBuilder pBuilder) {
      List<ResultField> $$3 = this.getTimes(pSectionPath);
      Object2LongMap<String> $$4 = ((ProfilerPathEntry)ObjectUtils.firstNonNull(new ProfilerPathEntry[]{this.entries.get(pSectionPath), EMPTY})).getCounters();
      $$4.forEach(
         (p_18508_, p_18509_) -> indentLine(pBuilder, pDepth)
               .append('#')
               .append(p_18508_)
               .append(' ')
               .append(p_18509_)
               .append('/')
               .append(p_18509_ / (long)this.tickDuration)
               .append('\n')
      );
      if ($$3.size() >= 3) {
         for (int $$5 = 1; $$5 < $$3.size(); $$5++) {
            ResultField $$6 = $$3.get($$5);
            indentLine(pBuilder, pDepth)
               .append($$6.name)
               .append('(')
               .append($$6.count)
               .append('/')
               .append(String.format(Locale.ROOT, "%.0f", (float)$$6.count / (float)this.tickDuration))
               .append(')')
               .append(" - ")
               .append(String.format(Locale.ROOT, "%.2f", $$6.percentage))
               .append("%/")
               .append(String.format(Locale.ROOT, "%.2f", $$6.globalPercentage))
               .append("%\n");
            if (!"unspecified".equals($$6.name)) {
               try {
                  this.appendProfilerResults(pDepth + 1, pSectionPath + "\u001e" + $$6.name, pBuilder);
               } catch (Exception var9) {
                  pBuilder.append("[[ EXCEPTION ").append(var9).append(" ]]");
               }
            }
         }
      }
   }

   private void appendCounterResults(int pIndents, String pName, FilledProfileResults.CounterCollector pCollector, int pTickSpan, StringBuilder pBuilder) {
      indentLine(pBuilder, pIndents)
         .append(pName)
         .append(" total:")
         .append(pCollector.selfValue)
         .append('/')
         .append(pCollector.totalValue)
         .append(" average: ")
         .append(pCollector.selfValue / (long)pTickSpan)
         .append('/')
         .append(pCollector.totalValue / (long)pTickSpan)
         .append('\n');
      pCollector.children
         .entrySet()
         .stream()
         .sorted(COUNTER_ENTRY_COMPARATOR)
         .forEach(p_18474_ -> this.appendCounterResults(pIndents + 1, p_18474_.getKey(), p_18474_.getValue(), pTickSpan, pBuilder));
   }

   private void appendCounters(Map<String, FilledProfileResults.CounterCollector> pCounters, StringBuilder pBuilder, int pTickSpan) {
      pCounters.forEach((p_18503_, p_18504_) -> {
         pBuilder.append("-- Counter: ").append(p_18503_).append(" --\n");
         this.appendCounterResults(0, "root", p_18504_.children.get("root"), pTickSpan, pBuilder);
         pBuilder.append("\n\n");
      });
   }

   @Override
   public int getTickDuration() {
      return this.tickDuration;
   }

   static class CounterCollector {
      long selfValue;
      long totalValue;
      final Map<String, FilledProfileResults.CounterCollector> children = Maps.newHashMap();

      public void addValue(Iterator<String> pCounters, long pValue) {
         this.totalValue += pValue;
         if (!pCounters.hasNext()) {
            this.selfValue += pValue;
         } else {
            this.children.computeIfAbsent(pCounters.next(), p_18546_ -> new FilledProfileResults.CounterCollector()).addValue(pCounters, pValue);
         }
      }
   }
}
