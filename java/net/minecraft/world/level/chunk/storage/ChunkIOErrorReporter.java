package net.minecraft.world.level.chunk.storage;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.level.ChunkPos;

public interface ChunkIOErrorReporter {
   void reportChunkLoadFailure(Throwable pThrowable, RegionStorageInfo pRegionStorageInfo, ChunkPos pChunkPos);

   void reportChunkSaveFailure(Throwable pThrowable, RegionStorageInfo pRegionStorageInfo, ChunkPos pChunkPos);

   static ReportedException createMisplacedChunkReport(ChunkPos pPos, ChunkPos pExpectedPos) {
      CrashReport $$2 = CrashReport.forThrowable(
         new IllegalStateException("Retrieved chunk position " + pPos + " does not match requested " + pExpectedPos), "Chunk found in invalid location"
      );
      CrashReportCategory $$3 = $$2.addCategory("Misplaced Chunk");
      $$3.setDetail("Stored Position", pPos::toString);
      return new ReportedException($$2);
   }

   default void reportMisplacedChunk(ChunkPos pPos, ChunkPos pExpectedPos, RegionStorageInfo pRegionStorageInfo) {
      this.reportChunkLoadFailure(createMisplacedChunkReport(pPos, pExpectedPos), pRegionStorageInfo, pExpectedPos);
   }
}
