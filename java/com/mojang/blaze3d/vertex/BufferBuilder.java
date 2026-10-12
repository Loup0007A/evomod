package com.mojang.blaze3d.vertex;

import java.nio.ByteOrder;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class BufferBuilder implements VertexConsumer {
   private static final long NOT_BUILDING = -1L;
   private static final long UNKNOWN_ELEMENT = -1L;
   private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
   private final ByteBufferBuilder buffer;
   private long vertexPointer = -1L;
   private int vertices;
   private final VertexFormat format;
   private final VertexFormat.Mode mode;
   private final boolean fastFormat;
   private final boolean fullFormat;
   private final int vertexSize;
   private final int initialElementsToFill;
   private final int[] offsetsByElement;
   private int elementsToFill;
   private boolean building = true;

   public BufferBuilder(ByteBufferBuilder pBuffer, VertexFormat.Mode pMode, VertexFormat pFormat) {
      if (!pFormat.contains(VertexFormatElement.POSITION)) {
         throw new IllegalArgumentException("Cannot build mesh with no position element");
      } else {
         this.buffer = pBuffer;
         this.mode = pMode;
         this.format = pFormat;
         this.vertexSize = pFormat.getVertexSize();
         this.initialElementsToFill = pFormat.getElementsMask() & ~VertexFormatElement.POSITION.mask();
         this.offsetsByElement = pFormat.getOffsetsByElement();
         boolean $$3 = pFormat == DefaultVertexFormat.NEW_ENTITY;
         boolean $$4 = pFormat == DefaultVertexFormat.BLOCK;
         this.fastFormat = $$3 || $$4;
         this.fullFormat = $$3;
      }
   }

   @Nullable
   public MeshData build() {
      this.ensureBuilding();
      this.endLastVertex();
      MeshData $$0 = this.storeMesh();
      this.building = false;
      this.vertexPointer = -1L;
      return $$0;
   }

   public MeshData buildOrThrow() {
      MeshData $$0 = this.build();
      if ($$0 == null) {
         throw new IllegalStateException("BufferBuilder was empty");
      } else {
         return $$0;
      }
   }

   private void ensureBuilding() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   @Nullable
   private MeshData storeMesh() {
      if (this.vertices == 0) {
         return null;
      } else {
         ByteBufferBuilder.Result $$0 = this.buffer.build();
         if ($$0 == null) {
            return null;
         } else {
            int $$1 = this.mode.indexCount(this.vertices);
            VertexFormat.IndexType $$2 = VertexFormat.IndexType.least(this.vertices);
            return new MeshData($$0, new MeshData.DrawState(this.format, this.vertices, $$1, this.mode, $$2));
         }
      }
   }

   private long beginVertex() {
      this.ensureBuilding();
      this.endLastVertex();
      this.vertices++;
      long $$0 = this.buffer.reserve(this.vertexSize);
      this.vertexPointer = $$0;
      return $$0;
   }

   private long beginElement(VertexFormatElement pElement) {
      int $$1 = this.elementsToFill;
      int $$2 = $$1 & ~pElement.mask();
      if ($$2 == $$1) {
         return -1L;
      } else {
         this.elementsToFill = $$2;
         long $$3 = this.vertexPointer;
         if ($$3 == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
         } else {
            return $$3 + (long)this.offsetsByElement[pElement.id()];
         }
      }
   }

   private void endLastVertex() {
      if (this.vertices != 0) {
         if (this.elementsToFill != 0) {
            String $$0 = VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + $$0);
         } else {
            if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
               long $$1 = this.buffer.reserve(this.vertexSize);
               MemoryUtil.memCopy($$1 - (long)this.vertexSize, $$1, (long)this.vertexSize);
               this.vertices++;
            }
         }
      }
   }

   private static void putRgba(long pPointer, int pColor) {
      int $$2 = FastColor.ABGR32.fromArgb32(pColor);
      MemoryUtil.memPutInt(pPointer, IS_LITTLE_ENDIAN ? $$2 : Integer.reverseBytes($$2));
   }

   private static void putPackedUv(long pPointer, int pPackedUv) {
      if (IS_LITTLE_ENDIAN) {
         MemoryUtil.memPutInt(pPointer, pPackedUv);
      } else {
         MemoryUtil.memPutShort(pPointer, (short)(pPackedUv & 65535));
         MemoryUtil.memPutShort(pPointer + 2L, (short)(pPackedUv >> 16 & 65535));
      }
   }

   @Override
   public VertexConsumer addVertex(float pX, float pY, float pZ) {
      long $$3 = this.beginVertex() + (long)this.offsetsByElement[VertexFormatElement.POSITION.id()];
      this.elementsToFill = this.initialElementsToFill;
      MemoryUtil.memPutFloat($$3, pX);
      MemoryUtil.memPutFloat($$3 + 4L, pY);
      MemoryUtil.memPutFloat($$3 + 8L, pZ);
      return this;
   }

   @Override
   public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
      long $$4 = this.beginElement(VertexFormatElement.COLOR);
      if ($$4 != -1L) {
         MemoryUtil.memPutByte($$4, (byte)pRed);
         MemoryUtil.memPutByte($$4 + 1L, (byte)pGreen);
         MemoryUtil.memPutByte($$4 + 2L, (byte)pBlue);
         MemoryUtil.memPutByte($$4 + 3L, (byte)pAlpha);
      }

      return this;
   }

   @Override
   public VertexConsumer setColor(int pColor) {
      long $$1 = this.beginElement(VertexFormatElement.COLOR);
      if ($$1 != -1L) {
         putRgba($$1, pColor);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv(float pU, float pV) {
      long $$2 = this.beginElement(VertexFormatElement.UV0);
      if ($$2 != -1L) {
         MemoryUtil.memPutFloat($$2, pU);
         MemoryUtil.memPutFloat($$2 + 4L, pV);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv1(int pU, int pV) {
      return this.uvShort((short)pU, (short)pV, VertexFormatElement.UV1);
   }

   @Override
   public VertexConsumer setOverlay(int pPackedOverlay) {
      long $$1 = this.beginElement(VertexFormatElement.UV1);
      if ($$1 != -1L) {
         putPackedUv($$1, pPackedOverlay);
      }

      return this;
   }

   @Override
   public VertexConsumer setUv2(int pU, int pV) {
      return this.uvShort((short)pU, (short)pV, VertexFormatElement.UV2);
   }

   @Override
   public VertexConsumer setLight(int pPackedLight) {
      long $$1 = this.beginElement(VertexFormatElement.UV2);
      if ($$1 != -1L) {
         putPackedUv($$1, pPackedLight);
      }

      return this;
   }

   private VertexConsumer uvShort(short pU, short pV, VertexFormatElement pElement) {
      long $$3 = this.beginElement(pElement);
      if ($$3 != -1L) {
         MemoryUtil.memPutShort($$3, pU);
         MemoryUtil.memPutShort($$3 + 2L, pV);
      }

      return this;
   }

   @Override
   public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
      long $$3 = this.beginElement(VertexFormatElement.NORMAL);
      if ($$3 != -1L) {
         MemoryUtil.memPutByte($$3, normalIntValue(pNormalX));
         MemoryUtil.memPutByte($$3 + 1L, normalIntValue(pNormalY));
         MemoryUtil.memPutByte($$3 + 2L, normalIntValue(pNormalZ));
      }

      return this;
   }

   private static byte normalIntValue(float pValue) {
      return (byte)((int)(Mth.clamp(pValue, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   @Override
   public void addVertex(
      float pX, float pY, float pZ, int pColor, float pU, float pV, int pPackedOverlay, int pPackedLight, float pNormalX, float pNormalY, float pNormalZ
   ) {
      if (this.fastFormat) {
         long $$11 = this.beginVertex();
         MemoryUtil.memPutFloat($$11 + 0L, pX);
         MemoryUtil.memPutFloat($$11 + 4L, pY);
         MemoryUtil.memPutFloat($$11 + 8L, pZ);
         putRgba($$11 + 12L, pColor);
         MemoryUtil.memPutFloat($$11 + 16L, pU);
         MemoryUtil.memPutFloat($$11 + 20L, pV);
         long $$12;
         if (this.fullFormat) {
            putPackedUv($$11 + 24L, pPackedOverlay);
            $$12 = $$11 + 28L;
         } else {
            $$12 = $$11 + 24L;
         }

         putPackedUv($$12 + 0L, pPackedLight);
         MemoryUtil.memPutByte($$12 + 4L, normalIntValue(pNormalX));
         MemoryUtil.memPutByte($$12 + 5L, normalIntValue(pNormalY));
         MemoryUtil.memPutByte($$12 + 6L, normalIntValue(pNormalZ));
      } else {
         VertexConsumer.super.addVertex(pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ);
      }
   }
}
