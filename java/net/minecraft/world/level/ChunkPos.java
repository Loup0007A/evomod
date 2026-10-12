package net.minecraft.world.level;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class ChunkPos {
   private static final int SAFETY_MARGIN = 1056;
   public static final long INVALID_CHUNK_POS = asLong(1875066, 1875066);
   public static final ChunkPos ZERO = new ChunkPos(0, 0);
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;
   private static final int REGION_BITS = 5;
   public static final int REGION_SIZE = 32;
   private static final int REGION_MASK = 31;
   public static final int REGION_MAX_INDEX = 31;
   public final int x;
   public final int z;
   private static final int HASH_A = 1664525;
   private static final int HASH_C = 1013904223;
   private static final int HASH_Z_XOR = -559038737;

   public ChunkPos(int pX, int pY) {
      this.x = pX;
      this.z = pY;
   }

   public ChunkPos(BlockPos pPos) {
      this.x = SectionPos.blockToSectionCoord(pPos.getX());
      this.z = SectionPos.blockToSectionCoord(pPos.getZ());
   }

   public ChunkPos(long pPackedPos) {
      this.x = (int)pPackedPos;
      this.z = (int)(pPackedPos >> 32);
   }

   public static ChunkPos minFromRegion(int pChunkX, int pChunkZ) {
      return new ChunkPos(pChunkX << 5, pChunkZ << 5);
   }

   public static ChunkPos maxFromRegion(int pChunkX, int pChunkZ) {
      return new ChunkPos((pChunkX << 5) + 31, (pChunkZ << 5) + 31);
   }

   public long toLong() {
      return asLong(this.x, this.z);
   }

   public static long asLong(int pX, int pZ) {
      return (long)pX & 4294967295L | ((long)pZ & 4294967295L) << 32;
   }

   public static long asLong(BlockPos pPos) {
      return asLong(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
   }

   public static int getX(long pChunkAsLong) {
      return (int)(pChunkAsLong & 4294967295L);
   }

   public static int getZ(long pChunkAsLong) {
      return (int)(pChunkAsLong >>> 32 & 4294967295L);
   }

   @Override
   public int hashCode() {
      return hash(this.x, this.z);
   }

   public static int hash(int pX, int pZ) {
      int $$2 = 1664525 * pX + 1013904223;
      int $$3 = 1664525 * (pZ ^ -559038737) + 1013904223;
      return $$2 ^ $$3;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof ChunkPos $$1) ? false : this.x == $$1.x && this.z == $$1.z;
      }
   }

   public int getMiddleBlockX() {
      return this.getBlockX(8);
   }

   public int getMiddleBlockZ() {
      return this.getBlockZ(8);
   }

   public int getMinBlockX() {
      return SectionPos.sectionToBlockCoord(this.x);
   }

   public int getMinBlockZ() {
      return SectionPos.sectionToBlockCoord(this.z);
   }

   public int getMaxBlockX() {
      return this.getBlockX(15);
   }

   public int getMaxBlockZ() {
      return this.getBlockZ(15);
   }

   public int getRegionX() {
      return this.x >> 5;
   }

   public int getRegionZ() {
      return this.z >> 5;
   }

   public int getRegionLocalX() {
      return this.x & 31;
   }

   public int getRegionLocalZ() {
      return this.z & 31;
   }

   public BlockPos getBlockAt(int pXSection, int pY, int pZSection) {
      return new BlockPos(this.getBlockX(pXSection), pY, this.getBlockZ(pZSection));
   }

   public int getBlockX(int pX) {
      return SectionPos.sectionToBlockCoord(this.x, pX);
   }

   public int getBlockZ(int pZ) {
      return SectionPos.sectionToBlockCoord(this.z, pZ);
   }

   public BlockPos getMiddleBlockPosition(int pY) {
      return new BlockPos(this.getMiddleBlockX(), pY, this.getMiddleBlockZ());
   }

   @Override
   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   public BlockPos getWorldPosition() {
      return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
   }

   public int getChessboardDistance(ChunkPos pChunkPos) {
      return this.getChessboardDistance(pChunkPos.x, pChunkPos.z);
   }

   public int getChessboardDistance(int pX, int pZ) {
      return Math.max(Math.abs(this.x - pX), Math.abs(this.z - pZ));
   }

   public int distanceSquared(ChunkPos pChunkPos) {
      return this.distanceSquared(pChunkPos.x, pChunkPos.z);
   }

   public int distanceSquared(long pPackedPos) {
      return this.distanceSquared(getX(pPackedPos), getZ(pPackedPos));
   }

   private int distanceSquared(int pX, int pZ) {
      int $$2 = pX - this.x;
      int $$3 = pZ - this.z;
      return $$2 * $$2 + $$3 * $$3;
   }

   public static Stream<ChunkPos> rangeClosed(ChunkPos pCenter, int pRadius) {
      return rangeClosed(new ChunkPos(pCenter.x - pRadius, pCenter.z - pRadius), new ChunkPos(pCenter.x + pRadius, pCenter.z + pRadius));
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos pStart, final ChunkPos pEnd) {
      int $$2 = Math.abs(pStart.x - pEnd.x) + 1;
      int $$3 = Math.abs(pStart.z - pEnd.z) + 1;
      final int $$4 = pStart.x < pEnd.x ? 1 : -1;
      final int $$5 = pStart.z < pEnd.z ? 1 : -1;
      return StreamSupport.stream(new AbstractSpliterator<ChunkPos>((long)($$2 * $$3), 64) {
         @Nullable
         private ChunkPos pos;

         @Override
         public boolean tryAdvance(Consumer<? super ChunkPos> p_45630_) {
            if (this.pos == null) {
               this.pos = pStart;
            } else {
               int $$1 = this.pos.x;
               int $$2 = this.pos.z;
               if ($$1 == pEnd.x) {
                  if ($$2 == pEnd.z) {
                     return false;
                  }

                  this.pos = new ChunkPos(pStart.x, $$2 + $$5);
               } else {
                  this.pos = new ChunkPos($$1 + $$4, $$2);
               }
            }

            p_45630_.accept(this.pos);
            return true;
         }
      }, false);
   }
}
