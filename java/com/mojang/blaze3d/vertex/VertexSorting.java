package com.mojang.blaze3d.vertex;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public interface VertexSorting {
   VertexSorting DISTANCE_TO_ORIGIN = byDistance(0.0F, 0.0F, 0.0F);
   VertexSorting ORTHOGRAPHIC_Z = byDistance((VertexSorting.DistanceFunction)(p_277433_ -> -p_277433_.z()));

   static VertexSorting byDistance(float pX, float pY, float pZ) {
      return byDistance(new Vector3f(pX, pY, pZ));
   }

   static VertexSorting byDistance(Vector3f pVector) {
      return byDistance(pVector::distanceSquared);
   }

   static VertexSorting byDistance(VertexSorting.DistanceFunction pDistanceFunction) {
      return p_278083_ -> {
         float[] $$2 = new float[p_278083_.length];
         int[] $$3 = new int[p_278083_.length];

         for (int $$4 = 0; $$4 < p_278083_.length; $$3[$$4] = $$4++) {
            $$2[$$4] = pDistanceFunction.apply(p_278083_[$$4]);
         }

         IntArrays.mergeSort($$3, (p_277443_, p_277864_) -> Floats.compare($$2[p_277864_], $$2[p_277443_]));
         return $$3;
      };
   }

   int[] sort(Vector3f[] pVectors);

   @OnlyIn(Dist.CLIENT)
   public interface DistanceFunction {
      float apply(Vector3f pVector);
   }
}
