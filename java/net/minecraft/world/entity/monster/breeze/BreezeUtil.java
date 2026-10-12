package net.minecraft.world.entity.monster.breeze;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BreezeUtil {
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 50.0;

   public static Vec3 randomPointBehindTarget(LivingEntity pTarget, RandomSource pRandom) {
      int $$2 = 90;
      float $$3 = pTarget.yHeadRot + 180.0F + (float)pRandom.nextGaussian() * 90.0F / 2.0F;
      float $$4 = Mth.lerp(pRandom.nextFloat(), 4.0F, 8.0F);
      Vec3 $$5 = Vec3.directionFromRotation(0.0F, $$3).scale((double)$$4);
      return pTarget.position().add($$5);
   }

   public static boolean hasLineOfSight(Breeze pBreeze, Vec3 pPos) {
      Vec3 $$2 = new Vec3(pBreeze.getX(), pBreeze.getY(), pBreeze.getZ());
      return pPos.distanceTo($$2) > 50.0
         ? false
         : pBreeze.level().clip(new ClipContext($$2, pPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pBreeze)).getType() == HitResult.Type.MISS;
   }
}
