package com.mojang.blaze3d.platform;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

@OnlyIn(Dist.CLIENT)
public final class VideoMode {
   private final int width;
   private final int height;
   private final int redBits;
   private final int greenBits;
   private final int blueBits;
   private final int refreshRate;
   private static final Pattern PATTERN = Pattern.compile("(\\d+)x(\\d+)(?:@(\\d+)(?::(\\d+))?)?");

   public VideoMode(int pWidth, int pHeight, int pRedBits, int pGreenBits, int pBlueBits, int pRefreshRate) {
      this.width = pWidth;
      this.height = pHeight;
      this.redBits = pRedBits;
      this.greenBits = pGreenBits;
      this.blueBits = pBlueBits;
      this.refreshRate = pRefreshRate;
   }

   public VideoMode(Buffer pBufferVideoMode) {
      this.width = pBufferVideoMode.width();
      this.height = pBufferVideoMode.height();
      this.redBits = pBufferVideoMode.redBits();
      this.greenBits = pBufferVideoMode.greenBits();
      this.blueBits = pBufferVideoMode.blueBits();
      this.refreshRate = pBufferVideoMode.refreshRate();
   }

   public VideoMode(GLFWVidMode pGlfwVideoMode) {
      this.width = pGlfwVideoMode.width();
      this.height = pGlfwVideoMode.height();
      this.redBits = pGlfwVideoMode.redBits();
      this.greenBits = pGlfwVideoMode.greenBits();
      this.blueBits = pGlfwVideoMode.blueBits();
      this.refreshRate = pGlfwVideoMode.refreshRate();
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getRedBits() {
      return this.redBits;
   }

   public int getGreenBits() {
      return this.greenBits;
   }

   public int getBlueBits() {
      return this.blueBits;
   }

   public int getRefreshRate() {
      return this.refreshRate;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         VideoMode $$1 = (VideoMode)pOther;
         return this.width == $$1.width
            && this.height == $$1.height
            && this.redBits == $$1.redBits
            && this.greenBits == $$1.greenBits
            && this.blueBits == $$1.blueBits
            && this.refreshRate == $$1.refreshRate;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.width, this.height, this.redBits, this.greenBits, this.blueBits, this.refreshRate);
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "%sx%s@%s (%sbit)", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }

   public static Optional<VideoMode> read(@Nullable String pVideoMode) {
      if (pVideoMode == null) {
         return Optional.empty();
      } else {
         try {
            Matcher $$1 = PATTERN.matcher(pVideoMode);
            if ($$1.matches()) {
               int $$2 = Integer.parseInt($$1.group(1));
               int $$3 = Integer.parseInt($$1.group(2));
               String $$4 = $$1.group(3);
               int $$5;
               if ($$4 == null) {
                  $$5 = 60;
               } else {
                  $$5 = Integer.parseInt($$4);
               }

               String $$7 = $$1.group(4);
               int $$8;
               if ($$7 == null) {
                  $$8 = 24;
               } else {
                  $$8 = Integer.parseInt($$7);
               }

               int $$10 = $$8 / 3;
               return Optional.of(new VideoMode($$2, $$3, $$10, $$10, $$10, $$5));
            }
         } catch (Exception var9) {
         }

         return Optional.empty();
      }
   }

   public String write() {
      return String.format(Locale.ROOT, "%sx%s@%s:%s", this.width, this.height, this.refreshRate, this.redBits + this.greenBits + this.blueBits);
   }
}
