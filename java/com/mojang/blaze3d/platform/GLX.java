package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GLX {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static String cpuInfo;

   public static String getOpenGLVersionString() {
      RenderSystem.assertOnRenderThread();
      return GLFW.glfwGetCurrentContext() == 0L
         ? "NO CONTEXT"
         : GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
   }

   public static int _getRefreshRate(Window pWindow) {
      RenderSystem.assertOnRenderThread();
      long $$1 = GLFW.glfwGetWindowMonitor(pWindow.getWindow());
      if ($$1 == 0L) {
         $$1 = GLFW.glfwGetPrimaryMonitor();
      }

      GLFWVidMode $$2 = $$1 == 0L ? null : GLFW.glfwGetVideoMode($$1);
      return $$2 == null ? 0 : $$2.refreshRate();
   }

   public static String _getLWJGLVersion() {
      return Version.getVersion();
   }

   public static LongSupplier _initGlfw() {
      Window.checkGlfwError((p_242032_, p_242033_) -> {
         throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", p_242032_, p_242033_));
      });
      List<String> $$0 = Lists.newArrayList();
      GLFWErrorCallback $$1 = GLFW.glfwSetErrorCallback((p_69365_, p_69366_) -> {
         String $$3x = p_69366_ == 0L ? "" : MemoryUtil.memUTF8(p_69366_);
         $$0.add(String.format(Locale.ROOT, "GLFW error during init: [0x%X]%s", p_69365_, $$3x));
      });
      if (!GLFW.glfwInit()) {
         throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join($$0));
      } else {
         LongSupplier $$2 = () -> (long)(GLFW.glfwGetTime() * 1.0E9);

         for (String $$3 : $$0) {
            LOGGER.error("GLFW error collected during initialization: {}", $$3);
         }

         RenderSystem.setErrorCallback($$1);
         return $$2;
      }
   }

   public static void _setGlfwErrorCallback(GLFWErrorCallbackI pErrorCallback) {
      GLFWErrorCallback $$1 = GLFW.glfwSetErrorCallback(pErrorCallback);
      if ($$1 != null) {
         $$1.free();
      }
   }

   public static boolean _shouldClose(Window pWindow) {
      return GLFW.glfwWindowShouldClose(pWindow.getWindow());
   }

   public static void _init(int pDebugVerbosity, boolean pSynchronous) {
      try {
         CentralProcessor $$2 = new SystemInfo().getHardware().getProcessor();
         cpuInfo = String.format(Locale.ROOT, "%dx %s", $$2.getLogicalProcessorCount(), $$2.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
      } catch (Throwable var3) {
      }

      GlDebug.enableDebugCallback(pDebugVerbosity, pSynchronous);
   }

   public static String _getCpuInfo() {
      return cpuInfo == null ? "<unknown>" : cpuInfo;
   }

   public static void _renderCrosshair(int pLineLength, boolean pRenderX, boolean pRenderY, boolean pRenderZ) {
      if (pRenderX || pRenderY || pRenderZ) {
         RenderSystem.assertOnRenderThread();
         GlStateManager._depthMask(false);
         GlStateManager._disableCull();
         RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
         Tesselator $$4 = RenderSystem.renderThreadTesselator();
         BufferBuilder $$5 = $$4.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         RenderSystem.lineWidth(4.0F);
         if (pRenderX) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F);
            $$5.addVertex((float)pLineLength, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F);
         }

         if (pRenderY) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F);
            $$5.addVertex(0.0F, (float)pLineLength, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F);
         }

         if (pRenderZ) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F);
            $$5.addVertex(0.0F, 0.0F, (float)pLineLength).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F);
         }

         BufferUploader.drawWithShader($$5.buildOrThrow());
         RenderSystem.lineWidth(2.0F);
         $$5 = $$4.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         if (pRenderX) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
            $$5.addVertex((float)pLineLength, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
         }

         if (pRenderY) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
            $$5.addVertex(0.0F, (float)pLineLength, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
         }

         if (pRenderZ) {
            $$5.addVertex(0.0F, 0.0F, 0.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);
            $$5.addVertex(0.0F, 0.0F, (float)pLineLength).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);
         }

         BufferUploader.drawWithShader($$5.buildOrThrow());
         RenderSystem.lineWidth(1.0F);
         GlStateManager._enableCull();
         GlStateManager._depthMask(true);
      }
   }

   public static <T> T make(Supplier<T> pSupplier) {
      return pSupplier.get();
   }

   public static <T> T make(T pValue, Consumer<T> pConsumer) {
      pConsumer.accept(pValue);
      return pValue;
   }
}
