package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlendMode {
   @Nullable
   private static BlendMode lastApplied;
   private final int srcColorFactor;
   private final int srcAlphaFactor;
   private final int dstColorFactor;
   private final int dstAlphaFactor;
   private final int blendFunc;
   private final boolean separateBlend;
   private final boolean opaque;

   private BlendMode(
      boolean pSeparateBlend, boolean pOpaque, int pSrcColorFactor, int pDstColorFactor, int pSrcAlphaFactor, int pDstAlphaFactor, int pBlendFunc
   ) {
      this.separateBlend = pSeparateBlend;
      this.srcColorFactor = pSrcColorFactor;
      this.dstColorFactor = pDstColorFactor;
      this.srcAlphaFactor = pSrcAlphaFactor;
      this.dstAlphaFactor = pDstAlphaFactor;
      this.opaque = pOpaque;
      this.blendFunc = pBlendFunc;
   }

   public BlendMode() {
      this(false, true, 1, 0, 1, 0, 32774);
   }

   public BlendMode(int pSrcFactor, int pDstFactor, int pBlendFunc) {
      this(false, false, pSrcFactor, pDstFactor, pSrcFactor, pDstFactor, pBlendFunc);
   }

   public BlendMode(int pSrcColorFactor, int pDstColorFactor, int pSrcAlphaFactor, int pDstAlphaFactor, int pBlendFunc) {
      this(true, false, pSrcColorFactor, pDstColorFactor, pSrcAlphaFactor, pDstAlphaFactor, pBlendFunc);
   }

   public void apply() {
      if (!this.equals(lastApplied)) {
         if (lastApplied == null || this.opaque != lastApplied.isOpaque()) {
            lastApplied = this;
            if (this.opaque) {
               RenderSystem.disableBlend();
               return;
            }

            RenderSystem.enableBlend();
         }

         RenderSystem.blendEquation(this.blendFunc);
         if (this.separateBlend) {
            RenderSystem.blendFuncSeparate(this.srcColorFactor, this.dstColorFactor, this.srcAlphaFactor, this.dstAlphaFactor);
         } else {
            RenderSystem.blendFunc(this.srcColorFactor, this.dstColorFactor);
         }
      }
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof BlendMode $$1)) {
         return false;
      } else if (this.blendFunc != $$1.blendFunc) {
         return false;
      } else if (this.dstAlphaFactor != $$1.dstAlphaFactor) {
         return false;
      } else if (this.dstColorFactor != $$1.dstColorFactor) {
         return false;
      } else if (this.opaque != $$1.opaque) {
         return false;
      } else if (this.separateBlend != $$1.separateBlend) {
         return false;
      } else {
         return this.srcAlphaFactor != $$1.srcAlphaFactor ? false : this.srcColorFactor == $$1.srcColorFactor;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.srcColorFactor;
      $$0 = 31 * $$0 + this.srcAlphaFactor;
      $$0 = 31 * $$0 + this.dstColorFactor;
      $$0 = 31 * $$0 + this.dstAlphaFactor;
      $$0 = 31 * $$0 + this.blendFunc;
      $$0 = 31 * $$0 + (this.separateBlend ? 1 : 0);
      return 31 * $$0 + (this.opaque ? 1 : 0);
   }

   public boolean isOpaque() {
      return this.opaque;
   }

   public static int stringToBlendFunc(String pFuncName) {
      String $$1 = pFuncName.trim().toLowerCase(Locale.ROOT);
      if ("add".equals($$1)) {
         return 32774;
      } else if ("subtract".equals($$1)) {
         return 32778;
      } else if ("reversesubtract".equals($$1)) {
         return 32779;
      } else if ("reverse_subtract".equals($$1)) {
         return 32779;
      } else if ("min".equals($$1)) {
         return 32775;
      } else {
         return "max".equals($$1) ? 32776 : 32774;
      }
   }

   public static int stringToBlendFactor(String pFactorName) {
      String $$1 = pFactorName.trim().toLowerCase(Locale.ROOT);
      $$1 = $$1.replaceAll("_", "");
      $$1 = $$1.replaceAll("one", "1");
      $$1 = $$1.replaceAll("zero", "0");
      $$1 = $$1.replaceAll("minus", "-");
      if ("0".equals($$1)) {
         return 0;
      } else if ("1".equals($$1)) {
         return 1;
      } else if ("srccolor".equals($$1)) {
         return 768;
      } else if ("1-srccolor".equals($$1)) {
         return 769;
      } else if ("dstcolor".equals($$1)) {
         return 774;
      } else if ("1-dstcolor".equals($$1)) {
         return 775;
      } else if ("srcalpha".equals($$1)) {
         return 770;
      } else if ("1-srcalpha".equals($$1)) {
         return 771;
      } else if ("dstalpha".equals($$1)) {
         return 772;
      } else {
         return "1-dstalpha".equals($$1) ? 773 : -1;
      }
   }
}
