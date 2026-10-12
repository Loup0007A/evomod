package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.Util;
import net.minecraft.util.DelegateDataOutput;
import net.minecraft.util.FastBufferedInputStream;

public class NbtIo {
   private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{
      StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
   };

   public static CompoundTag readCompressed(Path pPath, NbtAccounter pAccounter) throws IOException {
      CompoundTag var4;
      try (
         InputStream $$2 = Files.newInputStream(pPath);
         InputStream $$3 = new FastBufferedInputStream($$2);
      ) {
         var4 = readCompressed($$3, pAccounter);
      }

      return var4;
   }

   private static DataInputStream createDecompressorStream(InputStream pZippedStream) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(pZippedStream)));
   }

   private static DataOutputStream createCompressorStream(OutputStream pOutputSteam) throws IOException {
      return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(pOutputSteam)));
   }

   public static CompoundTag readCompressed(InputStream pZippedStream, NbtAccounter pAccounter) throws IOException {
      CompoundTag var3;
      try (DataInputStream $$2 = createDecompressorStream(pZippedStream)) {
         var3 = read($$2, pAccounter);
      }

      return var3;
   }

   public static void parseCompressed(Path pPath, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
      try (
         InputStream $$3 = Files.newInputStream(pPath);
         InputStream $$4 = new FastBufferedInputStream($$3);
      ) {
         parseCompressed($$4, pVisitor, pAccounter);
      }
   }

   public static void parseCompressed(InputStream pZippedStream, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
      try (DataInputStream $$3 = createDecompressorStream(pZippedStream)) {
         parse($$3, pVisitor, pAccounter);
      }
   }

   public static void writeCompressed(CompoundTag pCompoundTag, Path pPath) throws IOException {
      try (
         OutputStream $$2 = Files.newOutputStream(pPath, SYNC_OUTPUT_OPTIONS);
         OutputStream $$3 = new BufferedOutputStream($$2);
      ) {
         writeCompressed(pCompoundTag, $$3);
      }
   }

   public static void writeCompressed(CompoundTag pCompoundTag, OutputStream pOutputStream) throws IOException {
      try (DataOutputStream $$2 = createCompressorStream(pOutputStream)) {
         write(pCompoundTag, $$2);
      }
   }

   public static void write(CompoundTag pCompoundTag, Path pPath) throws IOException {
      try (
         OutputStream $$2 = Files.newOutputStream(pPath, SYNC_OUTPUT_OPTIONS);
         OutputStream $$3 = new BufferedOutputStream($$2);
         DataOutputStream $$4 = new DataOutputStream($$3);
      ) {
         write(pCompoundTag, $$4);
      }
   }

   @Nullable
   public static CompoundTag read(Path pPath) throws IOException {
      if (!Files.exists(pPath)) {
         return null;
      } else {
         CompoundTag var3;
         try (
            InputStream $$1 = Files.newInputStream(pPath);
            DataInputStream $$2 = new DataInputStream($$1);
         ) {
            var3 = read($$2, NbtAccounter.unlimitedHeap());
         }

         return var3;
      }
   }

   public static CompoundTag read(DataInput pInput) throws IOException {
      return read(pInput, NbtAccounter.unlimitedHeap());
   }

   public static CompoundTag read(DataInput pInput, NbtAccounter pAccounter) throws IOException {
      Tag $$2 = readUnnamedTag(pInput, pAccounter);
      if ($$2 instanceof CompoundTag) {
         return (CompoundTag)$$2;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(CompoundTag pCompoundTag, DataOutput pOutput) throws IOException {
      writeUnnamedTagWithFallback(pCompoundTag, pOutput);
   }

   public static void parse(DataInput pInput, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException {
      TagType<?> $$3 = TagTypes.getType(pInput.readByte());
      if ($$3 == EndTag.TYPE) {
         if (pVisitor.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            pVisitor.visitEnd();
         }
      } else {
         switch (pVisitor.visitRootEntry($$3)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(pInput);
               $$3.skip(pInput, pAccounter);
               break;
            case CONTINUE:
               StringTag.skipString(pInput);
               $$3.parse(pInput, pVisitor, pAccounter);
         }
      }
   }

   public static Tag readAnyTag(DataInput pInput, NbtAccounter pAccounter) throws IOException {
      byte $$2 = pInput.readByte();
      return (Tag)($$2 == 0 ? EndTag.INSTANCE : readTagSafe(pInput, pAccounter, $$2));
   }

   public static void writeAnyTag(Tag pTag, DataOutput pOutput) throws IOException {
      pOutput.writeByte(pTag.getId());
      if (pTag.getId() != 0) {
         pTag.write(pOutput);
      }
   }

   public static void writeUnnamedTag(Tag pTag, DataOutput pOutput) throws IOException {
      pOutput.writeByte(pTag.getId());
      if (pTag.getId() != 0) {
         pOutput.writeUTF("");
         pTag.write(pOutput);
      }
   }

   public static void writeUnnamedTagWithFallback(Tag pTag, DataOutput pOutput) throws IOException {
      writeUnnamedTag(pTag, new NbtIo.StringFallbackDataOutput(pOutput));
   }

   private static Tag readUnnamedTag(DataInput pInput, NbtAccounter pAccounter) throws IOException {
      byte $$2 = pInput.readByte();
      if ($$2 == 0) {
         return EndTag.INSTANCE;
      } else {
         StringTag.skipString(pInput);
         return readTagSafe(pInput, pAccounter, $$2);
      }
   }

   private static Tag readTagSafe(DataInput pInput, NbtAccounter pAccounter, byte pType) {
      try {
         return TagTypes.getType(pType).load(pInput, pAccounter);
      } catch (IOException var6) {
         CrashReport $$4 = CrashReport.forThrowable(var6, "Loading NBT data");
         CrashReportCategory $$5 = $$4.addCategory("NBT Tag");
         $$5.setDetail("Tag type", pType);
         throw new ReportedNbtException($$4);
      }
   }

   public static class StringFallbackDataOutput extends DelegateDataOutput {
      public StringFallbackDataOutput(DataOutput p_311190_) {
         super(p_311190_);
      }

      @Override
      public void writeUTF(String p_311566_) throws IOException {
         try {
            super.writeUTF(p_311566_);
         } catch (UTFDataFormatException var3) {
            Util.logAndPauseIfInIde("Failed to write NBT String", var3);
            super.writeUTF("");
         }
      }
   }
}
