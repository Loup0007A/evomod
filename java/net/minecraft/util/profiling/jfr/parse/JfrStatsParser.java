package net.minecraft.util.profiling.jfr.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.ChunkIdentification;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.IoSummary;
import net.minecraft.util.profiling.jfr.stats.PacketIdentification;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;

public class JfrStatsParser {
   private Instant recordingStarted = Instant.EPOCH;
   private Instant recordingEnded = Instant.EPOCH;
   private final List<ChunkGenStat> chunkGenStats = Lists.newArrayList();
   private final List<CpuLoadStat> cpuLoadStat = Lists.newArrayList();
   private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> receivedPackets = Maps.newHashMap();
   private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> sentPackets = Maps.newHashMap();
   private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> readChunks = Maps.newHashMap();
   private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> writtenChunks = Maps.newHashMap();
   private final List<FileIOStat> fileWrites = Lists.newArrayList();
   private final List<FileIOStat> fileReads = Lists.newArrayList();
   private int garbageCollections;
   private Duration gcTotalDuration = Duration.ZERO;
   private final List<GcHeapStat> gcHeapStats = Lists.newArrayList();
   private final List<ThreadAllocationStat> threadAllocationStats = Lists.newArrayList();
   private final List<TickTimeStat> tickTimes = Lists.newArrayList();
   @Nullable
   private Duration worldCreationDuration = null;

   private JfrStatsParser(Stream<RecordedEvent> pEvents) {
      this.capture(pEvents);
   }

   public static JfrStatsResult parse(Path pFile) {
      try {
         JfrStatsResult var4;
         try (final RecordingFile $$1 = new RecordingFile(pFile)) {
            Iterator<RecordedEvent> $$2 = new Iterator<RecordedEvent>() {
               @Override
               public boolean hasNext() {
                  return $$1.hasMoreEvents();
               }

               public RecordedEvent next() {
                  if (!this.hasNext()) {
                     throw new NoSuchElementException();
                  } else {
                     try {
                        return $$1.readEvent();
                     } catch (IOException var2) {
                        throw new UncheckedIOException(var2);
                     }
                  }
               }
            };
            Stream<RecordedEvent> $$3 = StreamSupport.stream(Spliterators.spliteratorUnknownSize($$2, 1297), false);
            var4 = new JfrStatsParser($$3).results();
         }

         return var4;
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   private JfrStatsResult results() {
      Duration $$0 = Duration.between(this.recordingStarted, this.recordingEnded);
      return new JfrStatsResult(
         this.recordingStarted,
         this.recordingEnded,
         $$0,
         this.worldCreationDuration,
         this.tickTimes,
         this.cpuLoadStat,
         GcHeapStat.summary($$0, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections),
         ThreadAllocationStat.summary(this.threadAllocationStats),
         collectIoStats($$0, this.receivedPackets),
         collectIoStats($$0, this.sentPackets),
         collectIoStats($$0, this.writtenChunks),
         collectIoStats($$0, this.readChunks),
         FileIOStat.summary($$0, this.fileWrites),
         FileIOStat.summary($$0, this.fileReads),
         this.chunkGenStats
      );
   }

   private void capture(Stream<RecordedEvent> pEvents) {
      pEvents.forEach(p_326727_ -> {
         if (p_326727_.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
            this.recordingEnded = p_326727_.getEndTime();
         }

         if (p_326727_.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
            this.recordingStarted = p_326727_.getStartTime();
         }

         String var2 = p_326727_.getEventType().getName();
         switch (var2) {
            case "minecraft.ChunkGeneration":
               this.chunkGenStats.add(ChunkGenStat.from(p_326727_));
               break;
            case "minecraft.LoadWorld":
               this.worldCreationDuration = p_326727_.getDuration();
               break;
            case "minecraft.ServerTickTime":
               this.tickTimes.add(TickTimeStat.from(p_326727_));
               break;
            case "minecraft.PacketReceived":
               this.incrementPacket(p_326727_, p_326727_.getInt("bytes"), this.receivedPackets);
               break;
            case "minecraft.PacketSent":
               this.incrementPacket(p_326727_, p_326727_.getInt("bytes"), this.sentPackets);
               break;
            case "minecraft.ChunkRegionRead":
               this.incrementChunk(p_326727_, p_326727_.getInt("bytes"), this.readChunks);
               break;
            case "minecraft.ChunkRegionWrite":
               this.incrementChunk(p_326727_, p_326727_.getInt("bytes"), this.writtenChunks);
               break;
            case "jdk.ThreadAllocationStatistics":
               this.threadAllocationStats.add(ThreadAllocationStat.from(p_326727_));
               break;
            case "jdk.GCHeapSummary":
               this.gcHeapStats.add(GcHeapStat.from(p_326727_));
               break;
            case "jdk.CPULoad":
               this.cpuLoadStat.add(CpuLoadStat.from(p_326727_));
               break;
            case "jdk.FileWrite":
               this.appendFileIO(p_326727_, this.fileWrites, "bytesWritten");
               break;
            case "jdk.FileRead":
               this.appendFileIO(p_326727_, this.fileReads, "bytesRead");
               break;
            case "jdk.GarbageCollection":
               this.garbageCollections++;
               this.gcTotalDuration = this.gcTotalDuration.plus(p_326727_.getDuration());
         }
      });
   }

   private void incrementPacket(RecordedEvent pEvent, int pIncrement, Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> pPackets) {
      pPackets.computeIfAbsent(PacketIdentification.from(pEvent), p_326728_ -> new JfrStatsParser.MutableCountAndSize()).increment(pIncrement);
   }

   private void incrementChunk(RecordedEvent pEvent, int pIncrement, Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> pChunks) {
      pChunks.computeIfAbsent(ChunkIdentification.from(pEvent), p_332913_ -> new JfrStatsParser.MutableCountAndSize()).increment(pIncrement);
   }

   private void appendFileIO(RecordedEvent pEvent, List<FileIOStat> pStats, String pId) {
      pStats.add(new FileIOStat(pEvent.getDuration(), pEvent.getString("path"), pEvent.getLong(pId)));
   }

   private static <T> IoSummary<T> collectIoStats(Duration pRecordingDuration, Map<T, JfrStatsParser.MutableCountAndSize> pEntries) {
      List<Pair<T, IoSummary.CountAndSize>> $$2 = pEntries.entrySet()
         .stream()
         .map(p_326729_ -> Pair.of(p_326729_.getKey(), p_326729_.getValue().toCountAndSize()))
         .toList();
      return new IoSummary<>(pRecordingDuration, $$2);
   }

   public static final class MutableCountAndSize {
      private long count;
      private long totalSize;

      public void increment(int pIncrement) {
         this.totalSize += (long)pIncrement;
         this.count++;
      }

      public IoSummary.CountAndSize toCountAndSize() {
         return new IoSummary.CountAndSize(this.count, this.totalSize);
      }
   }
}
