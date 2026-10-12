package com.mojang.blaze3d.vertex;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableLong;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class MeshData implements AutoCloseable {
   private final ByteBufferBuilder.Result vertexBuffer;
   @Nullable
   private ByteBufferBuilder.Result indexBuffer;
   private final MeshData.DrawState drawState;

   public MeshData(ByteBufferBuilder.Result pVertexBuffer, MeshData.DrawState pDrawState) {
      this.vertexBuffer = pVertexBuffer;
      this.drawState = pDrawState;
   }

   private static Vector3f[] unpackQuadCentroids(ByteBuffer pByteBuffer, int pVertexCount, VertexFormat pFormat) {
      int $$3 = pFormat.getOffset(VertexFormatElement.POSITION);
      if ($$3 == -1) {
         throw new IllegalArgumentException("Cannot identify quad centers with no position element");
      } else {
         FloatBuffer $$4 = pByteBuffer.asFloatBuffer();
         int $$5 = pFormat.getVertexSize() / 4;
         int $$6 = $$5 * 4;
         int $$7 = pVertexCount / 4;
         Vector3f[] $$8 = new Vector3f[$$7];

         for (int $$9 = 0; $$9 < $$7; $$9++) {
            int $$10 = $$9 * $$6 + $$3;
            int $$11 = $$10 + $$5 * 2;
            float $$12 = $$4.get($$10 + 0);
            float $$13 = $$4.get($$10 + 1);
            float $$14 = $$4.get($$10 + 2);
            float $$15 = $$4.get($$11 + 0);
            float $$16 = $$4.get($$11 + 1);
            float $$17 = $$4.get($$11 + 2);
            $$8[$$9] = new Vector3f(($$12 + $$15) / 2.0F, ($$13 + $$16) / 2.0F, ($$14 + $$17) / 2.0F);
         }

         return $$8;
      }
   }

   public ByteBuffer vertexBuffer() {
      return this.vertexBuffer.byteBuffer();
   }

   @Nullable
   public ByteBuffer indexBuffer() {
      return this.indexBuffer != null ? this.indexBuffer.byteBuffer() : null;
   }

   public MeshData.DrawState drawState() {
      return this.drawState;
   }

   @Nullable
   public MeshData.SortState sortQuads(ByteBufferBuilder pBufferBuilder, VertexSorting pSorting) {
      if (this.drawState.mode() != VertexFormat.Mode.QUADS) {
         return null;
      } else {
         Vector3f[] $$2 = unpackQuadCentroids(this.vertexBuffer.byteBuffer(), this.drawState.vertexCount(), this.drawState.format());
         MeshData.SortState $$3 = new MeshData.SortState($$2, this.drawState.indexType());
         this.indexBuffer = $$3.buildSortedIndexBuffer(pBufferBuilder, pSorting);
         return $$3;
      }
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
   }

   @OnlyIn(Dist.CLIENT)
   public static record SortState(Vector3f[] centroids, VertexFormat.IndexType indexType) {
      @Nullable
      public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder pBufferBuilder, VertexSorting pSorting) {
         int[] $$2 = pSorting.sort(this.centroids);
         long $$3 = pBufferBuilder.reserve($$2.length * 6 * this.indexType.bytes);
         IntConsumer $$4 = this.indexWriter($$3, this.indexType);

         for (int $$5 : $$2) {
            $$4.accept($$5 * 4 + 0);
            $$4.accept($$5 * 4 + 1);
            $$4.accept($$5 * 4 + 2);
            $$4.accept($$5 * 4 + 2);
            $$4.accept($$5 * 4 + 3);
            $$4.accept($$5 * 4 + 0);
         }

         return pBufferBuilder.build();
      }

      private IntConsumer indexWriter(long pIndex, VertexFormat.IndexType pType) {
         MutableLong $$2 = new MutableLong(pIndex);

         return switch (pType) {
            case SHORT -> p_344551_ -> MemoryUtil.memPutShort($$2.getAndAdd(2L), (short)p_344551_);
            case INT -> p_342795_ -> MemoryUtil.memPutInt($$2.getAndAdd(4L), p_342795_);
         };
      }
   }
}
