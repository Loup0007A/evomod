package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ProgramManager {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void glUseProgram(int pProgram) {
      RenderSystem.assertOnRenderThread();
      GlStateManager._glUseProgram(pProgram);
   }

   public static void releaseProgram(Shader pShader) {
      RenderSystem.assertOnRenderThread();
      pShader.getFragmentProgram().close();
      pShader.getVertexProgram().close();
      GlStateManager.glDeleteProgram(pShader.getId());
   }

   public static int createProgram() throws IOException {
      RenderSystem.assertOnRenderThread();
      int $$0 = GlStateManager.glCreateProgram();
      if ($$0 <= 0) {
         throw new IOException("Could not create shader program (returned program ID " + $$0 + ")");
      } else {
         return $$0;
      }
   }

   public static void linkShader(Shader pShader) {
      RenderSystem.assertOnRenderThread();
      pShader.attachToProgram();
      GlStateManager.glLinkProgram(pShader.getId());
      int $$1 = GlStateManager.glGetProgrami(pShader.getId(), 35714);
      if ($$1 == 0) {
         LOGGER.warn(
            "Error encountered when linking program containing VS {} and FS {}. Log output:",
            pShader.getVertexProgram().getName(),
            pShader.getFragmentProgram().getName()
         );
         LOGGER.warn(GlStateManager.glGetProgramInfoLog(pShader.getId(), 32768));
      }
   }
}
