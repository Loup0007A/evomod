package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiBufferSource {
   static MultiBufferSource.BufferSource immediate(ByteBufferBuilder pSharedBuffer) {
      return immediateWithBuffers(Object2ObjectSortedMaps.emptyMap(), pSharedBuffer);
   }

   static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers, ByteBufferBuilder pSharedBuffer) {
      return new MultiBufferSource.BufferSource(pSharedBuffer, pFixedBuffers);
   }

   VertexConsumer getBuffer(RenderType pRenderType);

   @OnlyIn(Dist.CLIENT)
   public static class BufferSource implements MultiBufferSource {
      protected final ByteBufferBuilder sharedBuffer;
      protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
      protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
      @Nullable
      protected RenderType lastSharedType;

      protected BufferSource(ByteBufferBuilder pSharedBuffer, SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers) {
         this.sharedBuffer = pSharedBuffer;
         this.fixedBuffers = pFixedBuffers;
      }

      @Override
      public VertexConsumer getBuffer(RenderType pRenderType) {
         BufferBuilder $$1 = this.startedBuilders.get(pRenderType);
         if ($$1 != null && !pRenderType.canConsolidateConsecutiveGeometry()) {
            this.endBatch(pRenderType, $$1);
            $$1 = null;
         }

         if ($$1 != null) {
            return $$1;
         } else {
            ByteBufferBuilder $$2 = this.fixedBuffers.get(pRenderType);
            if ($$2 != null) {
               $$1 = new BufferBuilder($$2, pRenderType.mode(), pRenderType.format());
            } else {
               if (this.lastSharedType != null) {
                  this.endBatch(this.lastSharedType);
               }

               $$1 = new BufferBuilder(this.sharedBuffer, pRenderType.mode(), pRenderType.format());
               this.lastSharedType = pRenderType;
            }

            this.startedBuilders.put(pRenderType, $$1);
            return $$1;
         }
      }

      public void endLastBatch() {
         if (this.lastSharedType != null) {
            this.endBatch(this.lastSharedType);
            this.lastSharedType = null;
         }
      }

      public void endBatch() {
         this.endLastBatch();

         for (RenderType $$0 : this.fixedBuffers.keySet()) {
            this.endBatch($$0);
         }
      }

      public void endBatch(RenderType pRenderType) {
         BufferBuilder $$1 = this.startedBuilders.remove(pRenderType);
         if ($$1 != null) {
            this.endBatch(pRenderType, $$1);
         }
      }

      private void endBatch(RenderType pRenderType, BufferBuilder pBuilder) {
         MeshData $$2 = pBuilder.build();
         if ($$2 != null) {
            if (pRenderType.sortOnUpload()) {
               ByteBufferBuilder $$3 = this.fixedBuffers.getOrDefault(pRenderType, this.sharedBuffer);
               $$2.sortQuads($$3, RenderSystem.getVertexSorting());
            }

            pRenderType.draw($$2);
         }

         if (pRenderType.equals(this.lastSharedType)) {
            this.lastSharedType = null;
         }
      }
   }
}
