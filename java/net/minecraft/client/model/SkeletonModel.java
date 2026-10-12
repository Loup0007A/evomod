package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonModel<T extends Mob & RangedAttackMob> extends HumanoidModel<T> {
   public SkeletonModel(ModelPart pRoot) {
      super(pRoot);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition $$1 = $$0.getRoot();
      createDefaultSkeletonMesh($$1);
      return LayerDefinition.create($$0, 64, 32);
   }

   protected static void createDefaultSkeletonMesh(PartDefinition pPartDefinition) {
      pPartDefinition.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      pPartDefinition.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      pPartDefinition.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      pPartDefinition.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
   }

   public void prepareMobModel(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
      this.rightArmPose = HumanoidModel.ArmPose.EMPTY;
      this.leftArmPose = HumanoidModel.ArmPose.EMPTY;
      ItemStack $$4 = pEntity.getItemInHand(InteractionHand.MAIN_HAND);
      if ($$4.is(Items.BOW) && pEntity.isAggressive()) {
         if (pEntity.getMainArm() == HumanoidArm.RIGHT) {
            this.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
         } else {
            this.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
         }
      }

      super.prepareMobModel(pEntity, pLimbSwing, pLimbSwingAmount, pPartialTick);
   }

   public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
      ItemStack $$6 = pEntity.getMainHandItem();
      if (pEntity.isAggressive() && ($$6.isEmpty() || !$$6.is(Items.BOW))) {
         float $$7 = Mth.sin(this.attackTime * (float) Math.PI);
         float $$8 = Mth.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * (float) Math.PI);
         this.rightArm.zRot = 0.0F;
         this.leftArm.zRot = 0.0F;
         this.rightArm.yRot = -(0.1F - $$7 * 0.6F);
         this.leftArm.yRot = 0.1F - $$7 * 0.6F;
         this.rightArm.xRot = (float) (-Math.PI / 2);
         this.leftArm.xRot = (float) (-Math.PI / 2);
         this.rightArm.xRot -= $$7 * 1.2F - $$8 * 0.4F;
         this.leftArm.xRot -= $$7 * 1.2F - $$8 * 0.4F;
         AnimationUtils.bobArms(this.rightArm, this.leftArm, pAgeInTicks);
      }
   }

   @Override
   public void translateToHand(HumanoidArm pSide, PoseStack pPoseStack) {
      float $$2 = pSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      ModelPart $$3 = this.getArm(pSide);
      $$3.x += $$2;
      $$3.translateAndRotate(pPoseStack);
      $$3.x -= $$2;
   }
}
