package com.mojang.blaze3d.platform;

import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class ClipboardManager {
   public static final int FORMAT_UNAVAILABLE = 65545;
   private final ByteBuffer clipboardScratchBuffer = BufferUtils.createByteBuffer(8192);

   public String getClipboard(long pWindow, GLFWErrorCallbackI pErrorCallback) {
      GLFWErrorCallback $$2 = GLFW.glfwSetErrorCallback(pErrorCallback);
      String $$3 = GLFW.glfwGetClipboardString(pWindow);
      $$3 = $$3 != null ? StringDecomposer.filterBrokenSurrogates($$3) : "";
      GLFWErrorCallback $$4 = GLFW.glfwSetErrorCallback($$2);
      if ($$4 != null) {
         $$4.free();
      }

      return $$3;
   }

   private static void pushClipboard(long pWindow, ByteBuffer pBuffer, byte[] pClipboardContent) {
      pBuffer.clear();
      pBuffer.put(pClipboardContent);
      pBuffer.put((byte)0);
      pBuffer.flip();
      GLFW.glfwSetClipboardString(pWindow, pBuffer);
   }

   public void setClipboard(long pWindow, String pClipboardContent) {
      byte[] $$2 = pClipboardContent.getBytes(Charsets.UTF_8);
      int $$3 = $$2.length + 1;
      if ($$3 < this.clipboardScratchBuffer.capacity()) {
         pushClipboard(pWindow, this.clipboardScratchBuffer, $$2);
      } else {
         ByteBuffer $$4 = MemoryUtil.memAlloc($$3);

         try {
            pushClipboard(pWindow, $$4, $$2);
         } finally {
            MemoryUtil.memFree($$4);
         }
      }
   }
}
