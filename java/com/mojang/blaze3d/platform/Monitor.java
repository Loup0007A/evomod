package com.mojang.blaze3d.platform;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

@OnlyIn(Dist.CLIENT)
public final class Monitor {
   private final long monitor;
   private final List<VideoMode> videoModes;
   private VideoMode currentMode;
   private int x;
   private int y;

   public Monitor(long pMonitor) {
      this.monitor = pMonitor;
      this.videoModes = Lists.newArrayList();
      this.refreshVideoModes();
   }

   public void refreshVideoModes() {
      this.videoModes.clear();
      Buffer $$0 = GLFW.glfwGetVideoModes(this.monitor);

      for (int $$1 = $$0.limit() - 1; $$1 >= 0; $$1--) {
         $$0.position($$1);
         VideoMode $$2 = new VideoMode($$0);
         if ($$2.getRedBits() >= 8 && $$2.getGreenBits() >= 8 && $$2.getBlueBits() >= 8) {
            this.videoModes.add($$2);
         }
      }

      int[] $$3 = new int[1];
      int[] $$4 = new int[1];
      GLFW.glfwGetMonitorPos(this.monitor, $$3, $$4);
      this.x = $$3[0];
      this.y = $$4[0];
      GLFWVidMode $$5 = GLFW.glfwGetVideoMode(this.monitor);
      this.currentMode = new VideoMode($$5);
   }

   public VideoMode getPreferredVidMode(Optional<VideoMode> pVideoMode) {
      if (pVideoMode.isPresent()) {
         VideoMode $$1 = pVideoMode.get();

         for (VideoMode $$2 : this.videoModes) {
            if ($$2.equals($$1)) {
               return $$2;
            }
         }
      }

      return this.getCurrentMode();
   }

   public int getVideoModeIndex(VideoMode pVideoMode) {
      return this.videoModes.indexOf(pVideoMode);
   }

   public VideoMode getCurrentMode() {
      return this.currentMode;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public VideoMode getMode(int pIndex) {
      return this.videoModes.get(pIndex);
   }

   public int getModeCount() {
      return this.videoModes.size();
   }

   public long getMonitor() {
      return this.monitor;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "Monitor[%s %sx%s %s]", this.monitor, this.x, this.y, this.currentMode);
   }
}
