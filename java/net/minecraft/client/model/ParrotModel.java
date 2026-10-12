package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotModel extends HierarchicalModel<Parrot> {
   private static final String FEATHER = "feather";
   private final ModelPart root;
   private final ModelPart body;
   private final ModelPart tail;
   private final ModelPart leftWing;
   private final ModelPart rightWing;
   private final ModelPart head;
   private final ModelPart feather;
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   public ParrotModel(ModelPart pRoot) {
      this.root = pRoot;
      this.body = pRoot.getChild("body");
      this.tail = pRoot.getChild("tail");
      this.leftWing = pRoot.getChild("left_wing");
      this.rightWing = pRoot.getChild("right_wing");
      this.head = pRoot.getChild("head");
      this.feather = this.head.getChild("feather");
      this.leftLeg = pRoot.getChild("left_leg");
      this.rightLeg = pRoot.getChild("right_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(0.0F, 16.5F, -3.0F));
      $$1.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, 1).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 21.07F, 1.16F));
      $$1.addOrReplaceChild(
         "left_wing", CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F), PartPose.offset(1.5F, 16.94F, -2.76F)
      );
      $$1.addOrReplaceChild(
         "right_wing", CubeListBuilder.create().texOffs(19, 8).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 5.0F, 3.0F), PartPose.offset(-1.5F, 16.94F, -2.76F)
      );
      PartDefinition $$2 = $$1.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 15.69F, -2.76F)
      );
      $$2.addOrReplaceChild("head2", CubeListBuilder.create().texOffs(10, 0).addBox(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(0.0F, -2.0F, -1.0F));
      $$2.addOrReplaceChild("beak1", CubeListBuilder.create().texOffs(11, 7).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -0.5F, -1.5F));
      $$2.addOrReplaceChild(
         "beak2", CubeListBuilder.create().texOffs(16, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -1.75F, -2.45F)
      );
      $$2.addOrReplaceChild(
         "feather", CubeListBuilder.create().texOffs(2, 18).addBox(0.0F, -4.0F, -2.0F, 0.0F, 5.0F, 4.0F), PartPose.offset(0.0F, -2.15F, 0.15F)
      );
      CubeListBuilder $$3 = CubeListBuilder.create().texOffs(14, 18).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
      $$1.addOrReplaceChild("left_leg", $$3, PartPose.offset(1.0F, 22.0F, -1.05F));
      $$1.addOrReplaceChild("right_leg", $$3, PartPose.offset(-1.0F, 22.0F, -1.05F));
      return LayerDefinition.create($$0, 32, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   public void setupAnim(Parrot pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      this.setupAnim(getState(pEntity), pEntity.tickCount, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
   }

   public void prepareMobModel(Parrot pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
      this.prepare(getState(pEntity));
   }

   public void renderOnShoulder(
      PoseStack pPoseStack,
      VertexConsumer pBuffer,
      int pPackedLight,
      int pPackedOverlay,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pNetHeadYaw,
      float pHeadPitch,
      int pTickCount
   ) {
      this.prepare(ParrotModel.State.ON_SHOULDER);
      this.setupAnim(ParrotModel.State.ON_SHOULDER, pTickCount, pLimbSwing, pLimbSwingAmount, 0.0F, pNetHeadYaw, pHeadPitch);
      this.root.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
   }

   private void setupAnim(
      ParrotModel.State pState, int pTickCount, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch
   ) {
      this.head.xRot = pHeadPitch * (float) (Math.PI / 180.0);
      this.head.yRot = pNetHeadYaw * (float) (Math.PI / 180.0);
      this.head.zRot = 0.0F;
      this.head.x = 0.0F;
      this.body.x = 0.0F;
      this.tail.x = 0.0F;
      this.rightWing.x = -1.5F;
      this.leftWing.x = 1.5F;
      switch (pState) {
         case STANDING:
            this.leftLeg.xRot = this.leftLeg.xRot + Mth.cos(pLimbSwing * 0.6662F) * 1.4F * pLimbSwingAmount;
            this.rightLeg.xRot = this.rightLeg.xRot + Mth.cos(pLimbSwing * 0.6662F + (float) Math.PI) * 1.4F * pLimbSwingAmount;
         case FLYING:
         case ON_SHOULDER:
         default:
            float $$9 = pAgeInTicks * 0.3F;
            this.head.y = 15.69F + $$9;
            this.tail.xRot = 1.015F + Mth.cos(pLimbSwing * 0.6662F) * 0.3F * pLimbSwingAmount;
            this.tail.y = 21.07F + $$9;
            this.body.y = 16.5F + $$9;
            this.leftWing.zRot = -0.0873F - pAgeInTicks;
            this.leftWing.y = 16.94F + $$9;
            this.rightWing.zRot = 0.0873F + pAgeInTicks;
            this.rightWing.y = 16.94F + $$9;
            this.leftLeg.y = 22.0F + $$9;
            this.rightLeg.y = 22.0F + $$9;
         case SITTING:
            break;
         case PARTY:
            float $$7 = Mth.cos((float)pTickCount);
            float $$8 = Mth.sin((float)pTickCount);
            this.head.x = $$7;
            this.head.y = 15.69F + $$8;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.zRot = Mth.sin((float)pTickCount) * 0.4F;
            this.body.x = $$7;
            this.body.y = 16.5F + $$8;
            this.leftWing.zRot = -0.0873F - pAgeInTicks;
            this.leftWing.x = 1.5F + $$7;
            this.leftWing.y = 16.94F + $$8;
            this.rightWing.zRot = 0.0873F + pAgeInTicks;
            this.rightWing.x = -1.5F + $$7;
            this.rightWing.y = 16.94F + $$8;
            this.tail.x = $$7;
            this.tail.y = 21.07F + $$8;
      }
   }

   private void prepare(ParrotModel.State pState) {
      this.feather.xRot = -0.2214F;
      this.body.xRot = 0.4937F;
      this.leftWing.xRot = -0.6981F;
      this.leftWing.yRot = (float) -Math.PI;
      this.rightWing.xRot = -0.6981F;
      this.rightWing.yRot = (float) -Math.PI;
      this.leftLeg.xRot = -0.0299F;
      this.rightLeg.xRot = -0.0299F;
      this.leftLeg.y = 22.0F;
      this.rightLeg.y = 22.0F;
      this.leftLeg.zRot = 0.0F;
      this.rightLeg.zRot = 0.0F;
      switch (pState) {
         case FLYING:
            this.leftLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
            this.rightLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
         case STANDING:
         case ON_SHOULDER:
         default:
            break;
         case SITTING:
            float $$1 = 1.9F;
            this.head.y = 17.59F;
            this.tail.xRot = 1.5388988F;
            this.tail.y = 22.97F;
            this.body.y = 18.4F;
            this.leftWing.zRot = -0.0873F;
            this.leftWing.y = 18.84F;
            this.rightWing.zRot = 0.0873F;
            this.rightWing.y = 18.84F;
            this.leftLeg.y++;
            this.rightLeg.y++;
            this.leftLeg.xRot++;
            this.rightLeg.xRot++;
            break;
         case PARTY:
            this.leftLeg.zRot = (float) (-Math.PI / 9);
            this.rightLeg.zRot = (float) (Math.PI / 9);
      }
   }

   private static ParrotModel.State getState(Parrot pParrot) {
      if (pParrot.isPartyParrot()) {
         return ParrotModel.State.PARTY;
      } else if (pParrot.isInSittingPose()) {
         return ParrotModel.State.SITTING;
      } else {
         return pParrot.isFlying() ? ParrotModel.State.FLYING : ParrotModel.State.STANDING;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum State {
      FLYING,
      STANDING,
      SITTING,
      PARTY,
      ON_SHOULDER;
   }
}
