package net.minecraft.client.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelUtils {
   public static float rotlerpRad(float pMin, float pMax, float pDelta) {
      float $$3 = pMax - pMin;

      while ($$3 < (float) -Math.PI) {
         $$3 += (float) (Math.PI * 2);
      }

      while ($$3 >= (float) Math.PI) {
         $$3 -= (float) (Math.PI * 2);
      }

      return pMin + pDelta * $$3;
   }
}
