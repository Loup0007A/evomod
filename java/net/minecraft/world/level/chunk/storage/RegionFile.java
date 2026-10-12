package net.minecraft.world.level.chunk.storage;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class RegionFile implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SECTOR_BYTES = 4096;
   @VisibleForTesting
   protected static final int SECTOR_INTS = 1024;
   private static final int CHUNK_HEADER_SIZE = 5;
   private static final int HEADER_OFFSET = 0;
   private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
   private static final String EXTERNAL_FILE_EXTENSION = ".mcc";
   private static final int EXTERNAL_STREAM_FLAG = 128;
   private static final int EXTERNAL_CHUNK_THRESHOLD = 256;
   private static final int CHUNK_NOT_PRESENT = 0;
   final RegionStorageInfo info;
   private final Path path;
   private final FileChannel file;
   private final Path externalFileDir;
   final RegionFileVersion version;
   private final ByteBuffer header = ByteBuffer.allocateDirect(8192);
   private final IntBuffer offsets;
   private final IntBuffer timestamps;
   @VisibleForTesting
   protected final RegionBitmap usedSectors = new RegionBitmap();

   public RegionFile(RegionStorageInfo pInfo, Path pPath, Path pExternalFileDir, boolean pSync) throws IOException {
      this(pInfo, pPath, pExternalFileDir, RegionFileVersion.getSelected(), pSync);
   }

   public RegionFile(RegionStorageInfo pInfo, Path pPath, Path pExternalFileDir, RegionFileVersion pVersion, boolean pSync) throws IOException {
      this.info = pInfo;
      this.path = pPath;
      this.version = pVersion;
      if (!Files.isDirectory(pExternalFileDir)) {
         throw new IllegalArgumentException("Expected directory, got " + pExternalFileDir.toAbsolutePath());
      } else {
         this.externalFileDir = pExternalFileDir;
         this.offsets = this.header.asIntBuffer();
         this.offsets.limit(1024);
         this.header.position(4096);
         this.timestamps = this.header.asIntBuffer();
         if (pSync) {
            this.file = FileChannel.open(pPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
         } else {
            this.file = FileChannel.open(pPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
         }

         this.usedSectors.force(0, 2);
         this.header.position(0);
         int $$5 = this.file.read(this.header, 0L);
         if ($$5 != -1) {
            if ($$5 != 8192) {
               LOGGER.warn("Region file {} has truncated header: {}", pPath, $$5);
            }

            long $$6 = Files.size(pPath);

            for (int $$7 = 0; $$7 < 1024; $$7++) {
               int $$8 = this.offsets.get($$7);
               if ($$8 != 0) {
                  int $$9 = getSectorNumber($$8);
                  int $$10 = getNumSectors($$8);
                  if ($$9 < 2) {
                     LOGGER.warn("Region file {} has invalid sector at index: {}; sector {} overlaps with header", new Object[]{pPath, $$7, $$9});
                     this.offsets.put($$7, 0);
                  } else if ($$10 == 0) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; size has to be > 0", pPath, $$7);
                     this.offsets.put($$7, 0);
                  } else if ((long)$$9 * 4096L > $$6) {
                     LOGGER.warn("Region file {} has an invalid sector at index: {}; sector {} is out of bounds", new Object[]{pPath, $$7, $$9});
                     this.offsets.put($$7, 0);
                  } else {
                     this.usedSectors.force($$9, $$10);
                  }
               }
            }
         }
      }
   }

   public Path getPath() {
      return this.path;
   }

   private Path getExternalChunkPath(ChunkPos pChunkPos) {
      String $$1 = "c." + pChunkPos.x + "." + pChunkPos.z + ".mcc";
      return this.externalFileDir.resolve($$1);
   }

   @Nullable
   public synchronized DataInputStream getChunkDataInputStream(ChunkPos pChunkPos) throws IOException {
      int $$1 = this.getOffset(pChunkPos);
      if ($$1 == 0) {
         return null;
      } else {
         int $$2 = getSectorNumber($$1);
         int $$3 = getNumSectors($$1);
         int $$4 = $$3 * 4096;
         ByteBuffer $$5 = ByteBuffer.allocate($$4);
         this.file.read($$5, (long)($$2 * 4096));
         $$5.flip();
         if ($$5.remaining() < 5) {
            LOGGER.error("Chunk {} header is truncated: expected {} but read {}", new Object[]{pChunkPos, $$4, $$5.remaining()});
            return null;
         } else {
            int $$6 = $$5.getInt();
            byte $$7 = $$5.get();
            if ($$6 == 0) {
               LOGGER.warn("Chunk {} is allocated, but stream is missing", pChunkPos);
               return null;
            } else {
               int $$8 = $$6 - 1;
               if (isExternalStreamChunk($$7)) {
                  if ($$8 != 0) {
                     LOGGER.warn("Chunk has both internal and external streams");
                  }

                  return this.createExternalChunkInputStream(pChunkPos, getExternalChunkVersion($$7));
               } else if ($$8 > $$5.remaining()) {
                  LOGGER.error("Chunk {} stream is truncated: expected {} but read {}", new Object[]{pChunkPos, $$8, $$5.remaining()});
                  return null;
               } else if ($$8 < 0) {
                  LOGGER.error("Declared size {} of chunk {} is negative", $$6, pChunkPos);
                  return null;
               } else {
                  JvmProfiler.INSTANCE.onRegionFileRead(this.info, pChunkPos, this.version, $$8);
                  return this.createChunkInputStream(pChunkPos, $$7, createStream($$5, $$8));
               }
            }
         }
      }
   }

   private static int getTimestamp() {
      return (int)(Util.getEpochMillis() / 1000L);
   }

   private static boolean isExternalStreamChunk(byte pVersionByte) {
      return (pVersionByte & 128) != 0;
   }

   private static byte getExternalChunkVersion(byte pVersionByte) {
      return (byte)(pVersionByte & -129);
   }

   @Nullable
   private DataInputStream createChunkInputStream(ChunkPos pChunkPos, byte pVersionByte, InputStream pInputStream) throws IOException {
      RegionFileVersion $$3 = RegionFileVersion.fromId(pVersionByte);
      if ($$3 == RegionFileVersion.VERSION_CUSTOM) {
         String $$4 = new DataInputStream(pInputStream).readUTF();
         ResourceLocation $$5 = ResourceLocation.tryParse($$4);
         if ($$5 != null) {
            LOGGER.error("Unrecognized custom compression {}", $$5);
            return null;
         } else {
            LOGGER.error("Invalid custom compression id {}", $$4);
            return null;
         }
      } else if ($$3 == null) {
         LOGGER.error("Chunk {} has invalid chunk stream version {}", pChunkPos, pVersionByte);
         return null;
      } else {
         return new DataInputStream($$3.wrap(pInputStream));
      }
   }

   @Nullable
   private DataInputStream createExternalChunkInputStream(ChunkPos pChunkPos, byte pVersionByte) throws IOException {
      Path $$2 = this.getExternalChunkPath(pChunkPos);
      if (!Files.isRegularFile($$2)) {
         LOGGER.error("External chunk path {} is not file", $$2);
         return null;
      } else {
         return this.createChunkInputStream(pChunkPos, pVersionByte, Files.newInputStream($$2));
      }
   }

   private static ByteArrayInputStream createStream(ByteBuffer pSourceBuffer, int pLength) {
      return new ByteArrayInputStream(pSourceBuffer.array(), pSourceBuffer.position(), pLength);
   }

   private int packSectorOffset(int pSectorOffset, int pSectorCount) {
      return pSectorOffset << 8 | pSectorCount;
   }

   private static int getNumSectors(int pPackedSectorOffset) {
      return pPackedSectorOffset & 0xFF;
   }

   private static int getSectorNumber(int pPackedSectorOffset) {
      return pPackedSectorOffset >> 8 & 16777215;
   }

   private static int sizeToSectors(int pSize) {
      return (pSize + 4096 - 1) / 4096;
   }

   public boolean doesChunkExist(ChunkPos pChunkPos) {
      int $$1 = this.getOffset(pChunkPos);
      if ($$1 == 0) {
         return false;
      } else {
         int $$2 = getSectorNumber($$1);
         int $$3 = getNumSectors($$1);
         ByteBuffer $$4 = ByteBuffer.allocate(5);

         try {
            this.file.read($$4, (long)($$2 * 4096));
            $$4.flip();
            if ($$4.remaining() != 5) {
               return false;
            } else {
               int $$5 = $$4.getInt();
               byte $$6 = $$4.get();
               if (isExternalStreamChunk($$6)) {
                  if (!RegionFileVersion.isValidVersion(getExternalChunkVersion($$6))) {
                     return false;
                  }

                  if (!Files.isRegularFile(this.getExternalChunkPath(pChunkPos))) {
                     return false;
                  }
               } else {
                  if (!RegionFileVersion.isValidVersion($$6)) {
                     return false;
                  }

                  if ($$5 == 0) {
                     return false;
                  }

                  int $$7 = $$5 - 1;
                  if ($$7 < 0 || $$7 > 4096 * $$3) {
                     return false;
                  }
               }

               return true;
            }
         } catch (IOException var9) {
            return false;
         }
      }
   }

   public DataOutputStream getChunkDataOutputStream(ChunkPos pChunkPos) throws IOException {
      return new DataOutputStream(this.version.wrap(new RegionFile.ChunkBuffer(pChunkPos)));
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   public void clear(ChunkPos pChunkPos) throws IOException {
      int $$1 = getOffsetIndex(pChunkPos);
      int $$2 = this.offsets.get($$1);
      if ($$2 != 0) {
         this.offsets.put($$1, 0);
         this.timestamps.put($$1, getTimestamp());
         this.writeHeader();
         Files.deleteIfExists(this.getExternalChunkPath(pChunkPos));
         this.usedSectors.free(getSectorNumber($$2), getNumSectors($$2));
      }
   }

   protected synchronized void write(ChunkPos pChunkPos, ByteBuffer pChunkData) throws IOException {
      int $$2 = getOffsetIndex(pChunkPos);
      int $$3 = this.offsets.get($$2);
      int $$4 = getSectorNumber($$3);
      int $$5 = getNumSectors($$3);
      int $$6 = pChunkData.remaining();
      int $$7 = sizeToSectors($$6);
      int $$9;
      RegionFile.CommitOp $$10;
      if ($$7 >= 256) {
         Path $$8 = this.getExternalChunkPath(pChunkPos);
         LOGGER.warn("Saving oversized chunk {} ({} bytes} to external file {}", new Object[]{pChunkPos, $$6, $$8});
         $$7 = 1;
         $$9 = this.usedSectors.allocate($$7);
         $$10 = this.writeToExternalFile($$8, pChunkData);
         ByteBuffer $$11 = this.createExternalStub();
         this.file.write($$11, (long)($$9 * 4096));
      } else {
         $$9 = this.usedSectors.allocate($$7);
         $$10 = () -> Files.deleteIfExists(this.getExternalChunkPath(pChunkPos));
         this.file.write(pChunkData, (long)($$9 * 4096));
      }

      this.offsets.put($$2, this.packSectorOffset($$9, $$7));
      this.timestamps.put($$2, getTimestamp());
      this.writeHeader();
      $$10.run();
      if ($$4 != 0) {
         this.usedSectors.free($$4, $$5);
      }
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer $$0 = ByteBuffer.allocate(5);
      $$0.putInt(1);
      $$0.put((byte)(this.version.getId() | 128));
      $$0.flip();
      return $$0;
   }

   private RegionFile.CommitOp writeToExternalFile(Path pExternalChunkFile, ByteBuffer pChunkData) throws IOException {
      Path $$2 = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel $$3 = FileChannel.open($$2, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         pChunkData.position(5);
         $$3.write(pChunkData);
      }

      return () -> Files.move($$2, pExternalChunkFile, StandardCopyOption.REPLACE_EXISTING);
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private int getOffset(ChunkPos pChunkPos) {
      return this.offsets.get(getOffsetIndex(pChunkPos));
   }

   public boolean hasChunk(ChunkPos pChunkPos) {
      return this.getOffset(pChunkPos) != 0;
   }

   private static int getOffsetIndex(ChunkPos pChunkPos) {
      return pChunkPos.getRegionLocalX() + pChunkPos.getRegionLocalZ() * 32;
   }

   @Override
   public void close() throws IOException {
      try {
         this.padToFullSector();
      } finally {
         try {
            this.file.force(true);
         } finally {
            this.file.close();
         }
      }
   }

   private void padToFullSector() throws IOException {
      int $$0 = (int)this.file.size();
      int $$1 = sizeToSectors($$0) * 4096;
      if ($$0 != $$1) {
         ByteBuffer $$2 = PADDING_BUFFER.duplicate();
         $$2.position(0);
         this.file.write($$2, (long)($$1 - 1));
      }
   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private final ChunkPos pos;

      public ChunkBuffer(final ChunkPos pPos) {
         super(8096);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(RegionFile.this.version.getId());
         this.pos = pPos;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer $$0 = ByteBuffer.wrap(this.buf, 0, this.count);
         int $$1 = this.count - 5 + 1;
         JvmProfiler.INSTANCE.onRegionFileWrite(RegionFile.this.info, this.pos, RegionFile.this.version, $$1);
         $$0.putInt(0, $$1);
         RegionFile.this.write(this.pos, $$0);
      }
   }

   interface CommitOp {
      void run() throws IOException;
   }
}
