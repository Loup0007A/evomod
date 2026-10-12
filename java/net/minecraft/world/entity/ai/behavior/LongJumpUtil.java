package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

public final class LongJumpUtil {
   public static Optional<Vec3> calculateJumpVectorForAngle(Mob pMob, Vec3 pTarget, float pMaxJumpVelocity, int pAngle, boolean pRequireClearTransition) {
      Vec3 $$5 = pMob.position();
      Vec3 $$6 = new Vec3(pTarget.x - $$5.x, 0.0, pTarget.z - $$5.z).normalize().scale(0.5);
      Vec3 $$7 = pTarget.subtract($$6);
      Vec3 $$8 = $$7.subtract($$5);
      float $$9 = (float)pAngle * (float) Math.PI / 180.0F;
      double $$10 = Math.atan2($$8.z, $$8.x);
      double $$11 = $$8.subtract(0.0, $$8.y, 0.0).lengthSqr();
      double $$12 = Math.sqrt($$11);
      double $$13 = $$8.y;
      double $$14 = pMob.getGravity();
      double $$15 = Math.sin((double)(2.0F * $$9));
      double $$16 = Math.pow(Math.cos((double)$$9), 2.0);
      double $$17 = Math.sin((double)$$9);
      double $$18 = Math.cos((double)$$9);
      double $$19 = Math.sin($$10);
      double $$20 = Math.cos($$10);
      double $$21 = $$11 * $$14 / ($$12 * $$15 - 2.0 * $$13 * $$16);
      if ($$21 < 0.0) {
         return Optional.empty();
      } else {
         double $$22 = Math.sqrt($$21);
         if ($$22 > (double)pMaxJumpVelocity) {
            return Optional.empty();
         } else {
            double $$23 = $$22 * $$18;
            double $$24 = $$22 * $$17;
            if (pRequireClearTransition) {
               int $$25 = Mth.ceil($$12 / $$23) * 2;
               double $$26 = 0.0;
               Vec3 $$27 = null;
               EntityDimensions $$28 = pMob.getDimensions(Pose.LONG_JUMPING);

               for (int $$29 = 0; $$29 < $$25 - 1; $$29++) {
                  $$26 += $$12 / (double)$$25;
                  double $$30 = $$17 / $$18 * $$26 - Math.pow($$26, 2.0) * $$14 / (2.0 * $$21 * Math.pow($$18, 2.0));
                  double $$31 = $$26 * $$20;
                  double $$32 = $$26 * $$19;
                  Vec3 $$33 = new Vec3($$5.x + $$31, $$5.y + $$30, $$5.z + $$32);
                  if ($$27 != null && !isClearTransition(pMob, $$28, $$27, $$33)) {
                     return Optional.empty();
                  }

                  $$27 = $$33;
               }
            }

            return Optional.of(new Vec3($$23 * $$20, $$24, $$23 * $$19).scale(0.95F));
         }
      }
   }

   private static boolean isClearTransition(Mob pMob, EntityDimensions pDimensions, Vec3 pStartPos, Vec3 pEndPos) {
      Vec3 $$4 = pEndPos.subtract(pStartPos);
      double $$5 = (double)Math.min(pDimensions.width(), pDimensions.height());
      int $$6 = Mth.ceil($$4.length() / $$5);
      Vec3 $$7 = $$4.normalize();
      Vec3 $$8 = pStartPos;

      for (int $$9 = 0; $$9 < $$6; $$9++) {
         $$8 = $$9 == $$6 - 1 ? pEndPos : $$8.add($$7.scale($$5 * 0.9F));
         if (!pMob.level().noCollision(pMob, pDimensions.makeBoundingBox($$8))) {
            return false;
         }
      }

      return true;
   }
}
