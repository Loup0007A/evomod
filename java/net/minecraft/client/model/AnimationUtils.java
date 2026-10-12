package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnimationUtils {
   public static void animateCrossbowHold(ModelPart pRightArm, ModelPart pLeftArm, ModelPart pHead, boolean pRightHanded) {
      ModelPart $$4 = pRightHanded ? pRightArm : pLeftArm;
      ModelPart $$5 = pRightHanded ? pLeftArm : pRightArm;
      $$4.yRot = (pRightHanded ? -0.3F : 0.3F) + pHead.yRot;
      $$5.yRot = (pRightHanded ? 0.6F : -0.6F) + pHead.yRot;
      $$4.xRot = (float) (-Math.PI / 2) + pHead.xRot + 0.1F;
      $$5.xRot = -1.5F + pHead.xRot;
   }

   public static void animateCrossbowCharge(ModelPart pRightArm, ModelPart pLeftArm, LivingEntity pLivingEntity, boolean pRightHanded) {
      ModelPart $$4 = pRightHanded ? pRightArm : pLeftArm;
      ModelPart $$5 = pRightHanded ? pLeftArm : pRightArm;
      $$4.yRot = pRightHanded ? -0.8F : 0.8F;
      $$4.xRot = -0.97079635F;
      $$5.xRot = $$4.xRot;
      float $$6 = (float)CrossbowItem.getChargeDuration(pLivingEntity.getUseItem(), pLivingEntity);
      float $$7 = Mth.clamp((float)pLivingEntity.getTicksUsingItem(), 0.0F, $$6);
      float $$8 = $$7 / $$6;
      $$5.yRot = Mth.lerp($$8, 0.4F, 0.85F) * (float)(pRightHanded ? 1 : -1);
      $$5.xRot = Mth.lerp($$8, $$5.xRot, (float) (-Math.PI / 2));
   }

   public static <T extends Mob> void swingWeaponDown(ModelPart pRightArm, ModelPart pLeftArm, T pMob, float pAttackTime, float pAgeInTicks) {
      float $$5 = Mth.sin(pAttackTime * (float) Math.PI);
      float $$6 = Mth.sin((1.0F - (1.0F - pAttackTime) * (1.0F - pAttackTime)) * (float) Math.PI);
      pRightArm.zRot = 0.0F;
      pLeftArm.zRot = 0.0F;
      pRightArm.yRot = (float) (Math.PI / 20);
      pLeftArm.yRot = (float) (-Math.PI / 20);
      if (pMob.getMainArm() == HumanoidArm.RIGHT) {
         pRightArm.xRot = -1.8849558F + Mth.cos(pAgeInTicks * 0.09F) * 0.15F;
         pLeftArm.xRot = -0.0F + Mth.cos(pAgeInTicks * 0.19F) * 0.5F;
         pRightArm.xRot += $$5 * 2.2F - $$6 * 0.4F;
         pLeftArm.xRot += $$5 * 1.2F - $$6 * 0.4F;
      } else {
         pRightArm.xRot = -0.0F + Mth.cos(pAgeInTicks * 0.19F) * 0.5F;
         pLeftArm.xRot = -1.8849558F + Mth.cos(pAgeInTicks * 0.09F) * 0.15F;
         pRightArm.xRot += $$5 * 1.2F - $$6 * 0.4F;
         pLeftArm.xRot += $$5 * 2.2F - $$6 * 0.4F;
      }

      bobArms(pRightArm, pLeftArm, pAgeInTicks);
   }

   public static void bobModelPart(ModelPart pModelPart, float pAgeInTicks, float pMultiplier) {
      pModelPart.zRot = pModelPart.zRot + pMultiplier * (Mth.cos(pAgeInTicks * 0.09F) * 0.05F + 0.05F);
      pModelPart.xRot = pModelPart.xRot + pMultiplier * Mth.sin(pAgeInTicks * 0.067F) * 0.05F;
   }

   public static void bobArms(ModelPart pRightArm, ModelPart pLeftArm, float pAgeInTicks) {
      bobModelPart(pRightArm, pAgeInTicks, 1.0F);
      bobModelPart(pLeftArm, pAgeInTicks, -1.0F);
   }

   public static void animateZombieArms(ModelPart pLeftArm, ModelPart pRightArm, boolean pIsAggressive, float pAttackTime, float pAgeInTicks) {
      float $$5 = Mth.sin(pAttackTime * (float) Math.PI);
      float $$6 = Mth.sin((1.0F - (1.0F - pAttackTime) * (1.0F - pAttackTime)) * (float) Math.PI);
      pRightArm.zRot = 0.0F;
      pLeftArm.zRot = 0.0F;
      pRightArm.yRot = -(0.1F - $$5 * 0.6F);
      pLeftArm.yRot = 0.1F - $$5 * 0.6F;
      float $$7 = (float) -Math.PI / (pIsAggressive ? 1.5F : 2.25F);
      pRightArm.xRot = $$7;
      pLeftArm.xRot = $$7;
      pRightArm.xRot += $$5 * 1.2F - $$6 * 0.4F;
      pLeftArm.xRot += $$5 * 1.2F - $$6 * 0.4F;
      bobArms(pRightArm, pLeftArm, pAgeInTicks);
   }
}
