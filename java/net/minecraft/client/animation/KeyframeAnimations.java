package net.minecraft.client.animation;

import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimations {
   public static void animate(
      HierarchicalModel<?> pModel, AnimationDefinition pAnimationDefinition, long pAccumulatedTime, float pScale, Vector3f pAnimationVecCache
   ) {
      float $$5 = getElapsedSeconds(pAnimationDefinition, pAccumulatedTime);

      for (Entry<String, List<AnimationChannel>> $$6 : pAnimationDefinition.boneAnimations().entrySet()) {
         Optional<ModelPart> $$7 = pModel.getAnyDescendantWithName($$6.getKey());
         List<AnimationChannel> $$8 = $$6.getValue();
         $$7.ifPresent(p_232330_ -> $$8.forEach(p_288241_ -> {
               Keyframe[] $$5x = p_288241_.keyframes();
               int $$6x = Math.max(0, Mth.binarySearch(0, $$5x.length, p_232315_ -> $$5 <= $$5x[p_232315_].timestamp()) - 1);
               int $$7x = Math.min($$5x.length - 1, $$6x + 1);
               Keyframe $$8x = $$5x[$$6x];
               Keyframe $$9 = $$5x[$$7x];
               float $$10 = $$5 - $$8x.timestamp();
               float $$11;
               if ($$7x != $$6x) {
                  $$11 = Mth.clamp($$10 / ($$9.timestamp() - $$8x.timestamp()), 0.0F, 1.0F);
               } else {
                  $$11 = 0.0F;
               }

               $$9.interpolation().apply(pAnimationVecCache, $$11, $$5x, $$6x, $$7x, pScale);
               p_288241_.target().apply(p_232330_, pAnimationVecCache);
            }));
      }
   }

   private static float getElapsedSeconds(AnimationDefinition pAnimationDefinition, long pAccumulatedTime) {
      float $$2 = (float)pAccumulatedTime / 1000.0F;
      return pAnimationDefinition.looping() ? $$2 % pAnimationDefinition.lengthInSeconds() : $$2;
   }

   public static Vector3f posVec(float pX, float pY, float pZ) {
      return new Vector3f(pX, -pY, pZ);
   }

   public static Vector3f degreeVec(float pXDegrees, float pYDegrees, float pZDegrees) {
      return new Vector3f(pXDegrees * (float) (Math.PI / 180.0), pYDegrees * (float) (Math.PI / 180.0), pZDegrees * (float) (Math.PI / 180.0));
   }

   public static Vector3f scaleVec(double pXScale, double pYScale, double pZScale) {
      return new Vector3f((float)(pXScale - 1.0), (float)(pYScale - 1.0), (float)(pZScale - 1.0));
   }
}
