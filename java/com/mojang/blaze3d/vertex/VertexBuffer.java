package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class VertexBuffer implements AutoCloseable {
   private final VertexBuffer.Usage usage;
   private int vertexBufferId;
   private int indexBufferId;
   private int arrayObjectId;
   @Nullable
   private VertexFormat format;
   @Nullable
   private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
   private VertexFormat.IndexType indexType;
   private int indexCount;
   private VertexFormat.Mode mode;

   public VertexBuffer(VertexBuffer.Usage pUsage) {
      this.usage = pUsage;
      RenderSystem.assertOnRenderThread();
      this.vertexBufferId = GlStateManager._glGenBuffers();
      this.indexBufferId = GlStateManager._glGenBuffers();
      this.arrayObjectId = GlStateManager._glGenVertexArrays();
   }

   public void upload(MeshData pMeshData) {
      MeshData var2 = pMeshData;

      label40: {
         try {
            if (this.isInvalid()) {
               break label40;
            }

            RenderSystem.assertOnRenderThread();
            MeshData.DrawState $$1 = pMeshData.drawState();
            this.format = this.uploadVertexBuffer($$1, pMeshData.vertexBuffer());
            this.sequentialIndices = this.uploadIndexBuffer($$1, pMeshData.indexBuffer());
            this.indexCount = $$1.indexCount();
            this.indexType = $$1.indexType();
            this.mode = $$1.mode();
         } catch (Throwable var6) {
            if (pMeshData != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pMeshData != null) {
            pMeshData.close();
         }

         return;
      }

      if (pMeshData != null) {
         pMeshData.close();
      }
   }

   public void uploadIndexBuffer(ByteBufferBuilder.Result pResult) {
      ByteBufferBuilder.Result var2 = pResult;

      label40: {
         try {
            if (this.isInvalid()) {
               break label40;
            }

            RenderSystem.assertOnRenderThread();
            GlStateManager._glBindBuffer(34963, this.indexBufferId);
            RenderSystem.glBufferData(34963, pResult.byteBuffer(), this.usage.id);
            this.sequentialIndices = null;
         } catch (Throwable var6) {
            if (pResult != null) {
               try {
                  var2.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (pResult != null) {
            pResult.close();
         }

         return;
      }

      if (pResult != null) {
         pResult.close();
      }
   }

   private VertexFormat uploadVertexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
      boolean $$2 = false;
      if (!pDrawState.format().equals(this.format)) {
         if (this.format != null) {
            this.format.clearBufferState();
         }

         GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         pDrawState.format().setupBufferState();
         $$2 = true;
      }

      if (pBuffer != null) {
         if (!$$2) {
            GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         }

         RenderSystem.glBufferData(34962, pBuffer, this.usage.id);
      }

      return pDrawState.format();
   }

   @Nullable
   private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
      if (pBuffer != null) {
         GlStateManager._glBindBuffer(34963, this.indexBufferId);
         RenderSystem.glBufferData(34963, pBuffer, this.usage.id);
         return null;
      } else {
         RenderSystem.AutoStorageIndexBuffer $$2 = RenderSystem.getSequentialBuffer(pDrawState.mode());
         if ($$2 != this.sequentialIndices || !$$2.hasStorage(pDrawState.indexCount())) {
            $$2.bind(pDrawState.indexCount());
         }

         return $$2;
      }
   }

   public void bind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(this.arrayObjectId);
   }

   public static void unbind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(0);
   }

   public void draw() {
      RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
   }

   private VertexFormat.IndexType getIndexType() {
      RenderSystem.AutoStorageIndexBuffer $$0 = this.sequentialIndices;
      return $$0 != null ? $$0.type() : this.indexType;
   }

   public void drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShader) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._drawWithShader(new Matrix4f(pModelViewMatrix), new Matrix4f(pProjectionMatrix), pShader));
      } else {
         this._drawWithShader(pModelViewMatrix, pProjectionMatrix, pShader);
      }
   }

   private void _drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShader) {
      pShader.setDefaultUniforms(this.mode, pModelViewMatrix, pProjectionMatrix, Minecraft.getInstance().getWindow());
      pShader.apply();
      this.draw();
      pShader.clear();
   }

   @Override
   public void close() {
      if (this.vertexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = -1;
      }

      if (this.indexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.indexBufferId);
         this.indexBufferId = -1;
      }

      if (this.arrayObjectId >= 0) {
         RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
         this.arrayObjectId = -1;
      }
   }

   public VertexFormat getFormat() {
      return this.format;
   }

   public boolean isInvalid() {
      return this.arrayObjectId == -1;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Usage {
      STATIC(35044),
      DYNAMIC(35048);

      final int id;

      private Usage(final int pId) {
         this.id = pId;
      }
   }
}
