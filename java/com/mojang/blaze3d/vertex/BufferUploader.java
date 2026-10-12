package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BufferUploader {
   @Nullable
   private static VertexBuffer lastImmediateBuffer;

   public static void reset() {
      if (lastImmediateBuffer != null) {
         invalidate();
         VertexBuffer.unbind();
      }
   }

   public static void invalidate() {
      lastImmediateBuffer = null;
   }

   public static void drawWithShader(MeshData pMeshData) {
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> _drawWithShader(pMeshData));
      } else {
         _drawWithShader(pMeshData);
      }
   }

   private static void _drawWithShader(MeshData pMeshData) {
      VertexBuffer $$1 = upload(pMeshData);
      $$1.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
   }

   public static void draw(MeshData pMeshData) {
      VertexBuffer $$1 = upload(pMeshData);
      $$1.draw();
   }

   private static VertexBuffer upload(MeshData pMeshData) {
      RenderSystem.assertOnRenderThread();
      VertexBuffer $$1 = bindImmediateBuffer(pMeshData.drawState().format());
      $$1.upload(pMeshData);
      return $$1;
   }

   private static VertexBuffer bindImmediateBuffer(VertexFormat pFormat) {
      VertexBuffer $$1 = pFormat.getImmediateDrawVertexBuffer();
      bindImmediateBuffer($$1);
      return $$1;
   }

   private static void bindImmediateBuffer(VertexBuffer pBuffer) {
      if (pBuffer != lastImmediateBuffer) {
         pBuffer.bind();
         lastImmediateBuffer = pBuffer;
      }
   }
}
