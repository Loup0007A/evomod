package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PufferfishBigModel<T extends Entity> extends HierarchicalModel<T> {
   private final ModelPart root;
   private final ModelPart leftBlueFin;
   private final ModelPart rightBlueFin;

   public PufferfishBigModel(ModelPart pRoot) {
      this.root = pRoot;
      this.leftBlueFin = pRoot.getChild("left_blue_fin");
      this.rightBlueFin = pRoot.getChild("right_blue_fin");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      int $$2 = 22;
      $$1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
      $$1.addOrReplaceChild(
         "right_blue_fin", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(-4.0F, 15.0F, -2.0F)
      );
      $$1.addOrReplaceChild(
         "left_blue_fin", CubeListBuilder.create().texOffs(24, 3).addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(4.0F, 15.0F, -2.0F)
      );
      $$1.addOrReplaceChild(
         "top_front_fin",
         CubeListBuilder.create().texOffs(15, 17).addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 14.0F, -4.0F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "top_middle_fin", CubeListBuilder.create().texOffs(14, 16).addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 14.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "top_back_fin",
         CubeListBuilder.create().texOffs(23, 18).addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 14.0F, 4.0F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "right_front_fin",
         CubeListBuilder.create().texOffs(5, 17).addBox(-1.0F, -8.0F, 0.0F, 1.0F, 8.0F, 0.0F),
         PartPose.offsetAndRotation(-4.0F, 22.0F, -4.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      $$1.addOrReplaceChild(
         "left_front_fin",
         CubeListBuilder.create().texOffs(1, 17).addBox(0.0F, -8.0F, 0.0F, 1.0F, 8.0F, 0.0F),
         PartPose.offsetAndRotation(4.0F, 22.0F, -4.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      $$1.addOrReplaceChild(
         "bottom_front_fin",
         CubeListBuilder.create().texOffs(15, 20).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 22.0F, -4.0F, (float) (-Math.PI / 4), 0.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "bottom_middle_fin", CubeListBuilder.create().texOffs(15, 20).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 0.0F), PartPose.offset(0.0F, 22.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "bottom_back_fin",
         CubeListBuilder.create().texOffs(15, 20).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 0.0F),
         PartPose.offsetAndRotation(0.0F, 22.0F, 4.0F, (float) (Math.PI / 4), 0.0F, 0.0F)
      );
      $$1.addOrReplaceChild(
         "right_back_fin",
         CubeListBuilder.create().texOffs(9, 17).addBox(-1.0F, -8.0F, 0.0F, 1.0F, 8.0F, 0.0F),
         PartPose.offsetAndRotation(-4.0F, 22.0F, 4.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      $$1.addOrReplaceChild(
         "left_back_fin",
         CubeListBuilder.create().texOffs(9, 17).addBox(0.0F, -8.0F, 0.0F, 1.0F, 8.0F, 0.0F),
         PartPose.offsetAndRotation(4.0F, 22.0F, 4.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      return LayerDefinition.create($$0, 32, 32);
   }

   @Override
   public ModelPart root() {
      return this.root;
   }

   @Override
   public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      this.rightBlueFin.zRot = -0.2F + 0.4F * Mth.sin(pAgeInTicks * 0.2F);
      this.leftBlueFin.zRot = 0.2F - 0.4F * Mth.sin(pAgeInTicks * 0.2F);
   }
}
