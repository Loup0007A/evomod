package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon> {
   public static final ResourceLocation CRYSTAL_BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
   private static final ResourceLocation DRAGON_EXPLODING_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
   private static final ResourceLocation DRAGON_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
   private static final ResourceLocation DRAGON_EYES_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(DRAGON_LOCATION);
   private static final RenderType DECAL = RenderType.entityDecal(DRAGON_LOCATION);
   private static final RenderType EYES = RenderType.eyes(DRAGON_EYES_LOCATION);
   private static final RenderType BEAM = RenderType.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
   private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
   private final EnderDragonRenderer.DragonModel model;

   public EnderDragonRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.shadowRadius = 0.5F;
      this.model = new EnderDragonRenderer.DragonModel(pContext.bakeLayer(ModelLayers.ENDER_DRAGON));
   }

   public void render(EnderDragon pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      float $$6 = (float)pEntity.getLatencyPos(7, pPartialTicks)[0];
      float $$7 = (float)(pEntity.getLatencyPos(5, pPartialTicks)[1] - pEntity.getLatencyPos(10, pPartialTicks)[1]);
      pPoseStack.mulPose(Axis.YP.rotationDegrees(-$$6));
      pPoseStack.mulPose(Axis.XP.rotationDegrees($$7 * 10.0F));
      pPoseStack.translate(0.0F, 0.0F, 1.0F);
      pPoseStack.scale(-1.0F, -1.0F, 1.0F);
      pPoseStack.translate(0.0F, -1.501F, 0.0F);
      boolean $$8 = pEntity.hurtTime > 0;
      this.model.prepareMobModel(pEntity, 0.0F, 0.0F, pPartialTicks);
      if (pEntity.dragonDeathTime > 0) {
         float $$9 = (float)pEntity.dragonDeathTime / 200.0F;
         int $$10 = FastColor.ARGB32.color(Mth.floor($$9 * 255.0F), -1);
         VertexConsumer $$11 = pBuffer.getBuffer(RenderType.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION));
         this.model.renderToBuffer(pPoseStack, $$11, pPackedLight, OverlayTexture.NO_OVERLAY, $$10);
         VertexConsumer $$12 = pBuffer.getBuffer(DECAL);
         this.model.renderToBuffer(pPoseStack, $$12, pPackedLight, OverlayTexture.pack(0.0F, $$8));
      } else {
         VertexConsumer $$13 = pBuffer.getBuffer(RENDER_TYPE);
         this.model.renderToBuffer(pPoseStack, $$13, pPackedLight, OverlayTexture.pack(0.0F, $$8));
      }

      VertexConsumer $$14 = pBuffer.getBuffer(EYES);
      this.model.renderToBuffer(pPoseStack, $$14, pPackedLight, OverlayTexture.NO_OVERLAY);
      if (pEntity.dragonDeathTime > 0) {
         float $$15 = ((float)pEntity.dragonDeathTime + pPartialTicks) / 200.0F;
         pPoseStack.pushPose();
         pPoseStack.translate(0.0F, -1.0F, -2.0F);
         renderRays(pPoseStack, $$15, pBuffer.getBuffer(RenderType.dragonRays()));
         renderRays(pPoseStack, $$15, pBuffer.getBuffer(RenderType.dragonRaysDepth()));
         pPoseStack.popPose();
      }

      pPoseStack.popPose();
      if (pEntity.nearestCrystal != null) {
         pPoseStack.pushPose();
         float $$16 = (float)(pEntity.nearestCrystal.getX() - Mth.lerp((double)pPartialTicks, pEntity.xo, pEntity.getX()));
         float $$17 = (float)(pEntity.nearestCrystal.getY() - Mth.lerp((double)pPartialTicks, pEntity.yo, pEntity.getY()));
         float $$18 = (float)(pEntity.nearestCrystal.getZ() - Mth.lerp((double)pPartialTicks, pEntity.zo, pEntity.getZ()));
         renderCrystalBeams(
            $$16,
            $$17 + EndCrystalRenderer.getY(pEntity.nearestCrystal, pPartialTicks),
            $$18,
            pPartialTicks,
            pEntity.tickCount,
            pPoseStack,
            pBuffer,
            pPackedLight
         );
         pPoseStack.popPose();
      }

      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   private static void renderRays(PoseStack pPoseStack, float pDragonDeathCompletion, VertexConsumer pBuffer) {
      pPoseStack.pushPose();
      float $$3 = Math.min(pDragonDeathCompletion > 0.8F ? (pDragonDeathCompletion - 0.8F) / 0.2F : 0.0F, 1.0F);
      int $$4 = FastColor.ARGB32.colorFromFloat(1.0F - $$3, 1.0F, 1.0F, 1.0F);
      int $$5 = 16711935;
      RandomSource $$6 = RandomSource.create(432L);
      Vector3f $$7 = new Vector3f();
      Vector3f $$8 = new Vector3f();
      Vector3f $$9 = new Vector3f();
      Vector3f $$10 = new Vector3f();
      Quaternionf $$11 = new Quaternionf();
      int $$12 = Mth.floor((pDragonDeathCompletion + pDragonDeathCompletion * pDragonDeathCompletion) / 2.0F * 60.0F);

      for (int $$13 = 0; $$13 < $$12; $$13++) {
         $$11.rotationXYZ($$6.nextFloat() * (float) (Math.PI * 2), $$6.nextFloat() * (float) (Math.PI * 2), $$6.nextFloat() * (float) (Math.PI * 2))
            .rotateXYZ(
               $$6.nextFloat() * (float) (Math.PI * 2),
               $$6.nextFloat() * (float) (Math.PI * 2),
               $$6.nextFloat() * (float) (Math.PI * 2) + pDragonDeathCompletion * (float) (Math.PI / 2)
            );
         pPoseStack.mulPose($$11);
         float $$14 = $$6.nextFloat() * 20.0F + 5.0F + $$3 * 10.0F;
         float $$15 = $$6.nextFloat() * 2.0F + 1.0F + $$3 * 2.0F;
         $$8.set(-HALF_SQRT_3 * $$15, $$14, -0.5F * $$15);
         $$9.set(HALF_SQRT_3 * $$15, $$14, -0.5F * $$15);
         $$10.set(0.0F, $$14, $$15);
         PoseStack.Pose $$16 = pPoseStack.last();
         pBuffer.addVertex($$16, $$7).setColor($$4);
         pBuffer.addVertex($$16, $$8).setColor(16711935);
         pBuffer.addVertex($$16, $$9).setColor(16711935);
         pBuffer.addVertex($$16, $$7).setColor($$4);
         pBuffer.addVertex($$16, $$9).setColor(16711935);
         pBuffer.addVertex($$16, $$10).setColor(16711935);
         pBuffer.addVertex($$16, $$7).setColor($$4);
         pBuffer.addVertex($$16, $$10).setColor(16711935);
         pBuffer.addVertex($$16, $$8).setColor(16711935);
      }

      pPoseStack.popPose();
   }

   public static void renderCrystalBeams(
      float pX, float pY, float pZ, float pPartialTick, int pTickCount, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight
   ) {
      float $$8 = Mth.sqrt(pX * pX + pZ * pZ);
      float $$9 = Mth.sqrt(pX * pX + pY * pY + pZ * pZ);
      pPoseStack.pushPose();
      pPoseStack.translate(0.0F, 2.0F, 0.0F);
      pPoseStack.mulPose(Axis.YP.rotation((float)(-Math.atan2((double)pZ, (double)pX)) - (float) (Math.PI / 2)));
      pPoseStack.mulPose(Axis.XP.rotation((float)(-Math.atan2((double)$$8, (double)pY)) - (float) (Math.PI / 2)));
      VertexConsumer $$10 = pBufferSource.getBuffer(BEAM);
      float $$11 = 0.0F - ((float)pTickCount + pPartialTick) * 0.01F;
      float $$12 = Mth.sqrt(pX * pX + pY * pY + pZ * pZ) / 32.0F - ((float)pTickCount + pPartialTick) * 0.01F;
      int $$13 = 8;
      float $$14 = 0.0F;
      float $$15 = 0.75F;
      float $$16 = 0.0F;
      PoseStack.Pose $$17 = pPoseStack.last();

      for (int $$18 = 1; $$18 <= 8; $$18++) {
         float $$19 = Mth.sin((float)$$18 * (float) (Math.PI * 2) / 8.0F) * 0.75F;
         float $$20 = Mth.cos((float)$$18 * (float) (Math.PI * 2) / 8.0F) * 0.75F;
         float $$21 = (float)$$18 / 8.0F;
         $$10.addVertex($$17, $$14 * 0.2F, $$15 * 0.2F, 0.0F)
            .setColor(-16777216)
            .setUv($$16, $$11)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal($$17, 0.0F, -1.0F, 0.0F);
         $$10.addVertex($$17, $$14, $$15, $$9)
            .setColor(-1)
            .setUv($$16, $$12)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal($$17, 0.0F, -1.0F, 0.0F);
         $$10.addVertex($$17, $$19, $$20, $$9)
            .setColor(-1)
            .setUv($$21, $$12)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal($$17, 0.0F, -1.0F, 0.0F);
         $$10.addVertex($$17, $$19 * 0.2F, $$20 * 0.2F, 0.0F)
            .setColor(-16777216)
            .setUv($$21, $$11)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal($$17, 0.0F, -1.0F, 0.0F);
         $$14 = $$19;
         $$15 = $$20;
         $$16 = $$21;
      }

      pPoseStack.popPose();
   }

   public ResourceLocation getTextureLocation(EnderDragon pEntity) {
      return DRAGON_LOCATION;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      float $$2 = -16.0F;
      PartDefinition $$3 = $$1.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .addBox("upperlip", -6.0F, -1.0F, -24.0F, 12, 5, 16, 176, 44)
            .addBox("upperhead", -8.0F, -8.0F, -10.0F, 16, 16, 16, 112, 30)
            .mirror()
            .addBox("scale", -5.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
            .addBox("nostril", -5.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0)
            .mirror()
            .addBox("scale", 3.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
            .addBox("nostril", 3.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0),
         PartPose.ZERO
      );
      $$3.addOrReplaceChild("jaw", CubeListBuilder.create().addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16, 176, 65), PartPose.offset(0.0F, 4.0F, -8.0F));
      $$1.addOrReplaceChild(
         "neck",
         CubeListBuilder.create().addBox("box", -5.0F, -5.0F, -5.0F, 10, 10, 10, 192, 104).addBox("scale", -1.0F, -9.0F, -3.0F, 2, 4, 6, 48, 0),
         PartPose.ZERO
      );
      $$1.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .addBox("body", -12.0F, 0.0F, -16.0F, 24, 24, 64, 0, 0)
            .addBox("scale", -1.0F, -6.0F, -10.0F, 2, 6, 12, 220, 53)
            .addBox("scale", -1.0F, -6.0F, 10.0F, 2, 6, 12, 220, 53)
            .addBox("scale", -1.0F, -6.0F, 30.0F, 2, 6, 12, 220, 53),
         PartPose.offset(0.0F, 4.0F, 8.0F)
      );
      PartDefinition $$4 = $$1.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().mirror().addBox("bone", 0.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
         PartPose.offset(12.0F, 5.0F, 2.0F)
      );
      $$4.addOrReplaceChild(
         "left_wing_tip",
         CubeListBuilder.create().mirror().addBox("bone", 0.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
         PartPose.offset(56.0F, 0.0F, 0.0F)
      );
      PartDefinition $$5 = $$1.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104), PartPose.offset(12.0F, 20.0F, 2.0F)
      );
      PartDefinition $$6 = $$5.addOrReplaceChild(
         "left_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138), PartPose.offset(0.0F, 20.0F, -1.0F)
      );
      $$6.addOrReplaceChild(
         "left_front_foot", CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104), PartPose.offset(0.0F, 23.0F, 0.0F)
      );
      PartDefinition $$7 = $$1.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0), PartPose.offset(16.0F, 16.0F, 42.0F)
      );
      PartDefinition $$8 = $$7.addOrReplaceChild(
         "left_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0), PartPose.offset(0.0F, 32.0F, -4.0F)
      );
      $$8.addOrReplaceChild(
         "left_hind_foot", CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0), PartPose.offset(0.0F, 31.0F, 4.0F)
      );
      PartDefinition $$9 = $$1.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
         PartPose.offset(-12.0F, 5.0F, 2.0F)
      );
      $$9.addOrReplaceChild(
         "right_wing_tip",
         CubeListBuilder.create().addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
         PartPose.offset(-56.0F, 0.0F, 0.0F)
      );
      PartDefinition $$10 = $$1.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104), PartPose.offset(-12.0F, 20.0F, 2.0F)
      );
      PartDefinition $$11 = $$10.addOrReplaceChild(
         "right_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138), PartPose.offset(0.0F, 20.0F, -1.0F)
      );
      $$11.addOrReplaceChild(
         "right_front_foot", CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104), PartPose.offset(0.0F, 23.0F, 0.0F)
      );
      PartDefinition $$12 = $$1.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0), PartPose.offset(-16.0F, 16.0F, 42.0F)
      );
      PartDefinition $$13 = $$12.addOrReplaceChild(
         "right_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0), PartPose.offset(0.0F, 32.0F, -4.0F)
      );
      $$13.addOrReplaceChild(
         "right_hind_foot", CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0), PartPose.offset(0.0F, 31.0F, 4.0F)
      );
      return LayerDefinition.create($$0, 256, 256);
   }

   @OnlyIn(Dist.CLIENT)
   public static class DragonModel extends EntityModel<EnderDragon> {
      private final ModelPart head;
      private final ModelPart neck;
      private final ModelPart jaw;
      private final ModelPart body;
      private final ModelPart leftWing;
      private final ModelPart leftWingTip;
      private final ModelPart leftFrontLeg;
      private final ModelPart leftFrontLegTip;
      private final ModelPart leftFrontFoot;
      private final ModelPart leftRearLeg;
      private final ModelPart leftRearLegTip;
      private final ModelPart leftRearFoot;
      private final ModelPart rightWing;
      private final ModelPart rightWingTip;
      private final ModelPart rightFrontLeg;
      private final ModelPart rightFrontLegTip;
      private final ModelPart rightFrontFoot;
      private final ModelPart rightRearLeg;
      private final ModelPart rightRearLegTip;
      private final ModelPart rightRearFoot;
      @Nullable
      private EnderDragon entity;
      private float a;

      public DragonModel(ModelPart pRoot) {
         this.head = pRoot.getChild("head");
         this.jaw = this.head.getChild("jaw");
         this.neck = pRoot.getChild("neck");
         this.body = pRoot.getChild("body");
         this.leftWing = pRoot.getChild("left_wing");
         this.leftWingTip = this.leftWing.getChild("left_wing_tip");
         this.leftFrontLeg = pRoot.getChild("left_front_leg");
         this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
         this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
         this.leftRearLeg = pRoot.getChild("left_hind_leg");
         this.leftRearLegTip = this.leftRearLeg.getChild("left_hind_leg_tip");
         this.leftRearFoot = this.leftRearLegTip.getChild("left_hind_foot");
         this.rightWing = pRoot.getChild("right_wing");
         this.rightWingTip = this.rightWing.getChild("right_wing_tip");
         this.rightFrontLeg = pRoot.getChild("right_front_leg");
         this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
         this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
         this.rightRearLeg = pRoot.getChild("right_hind_leg");
         this.rightRearLegTip = this.rightRearLeg.getChild("right_hind_leg_tip");
         this.rightRearFoot = this.rightRearLegTip.getChild("right_hind_foot");
      }

      public void prepareMobModel(EnderDragon pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
         this.entity = pEntity;
         this.a = pPartialTick;
      }

      public void setupAnim(EnderDragon pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      }

      @Override
      public void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
         pPoseStack.pushPose();
         float $$5 = Mth.lerp(this.a, this.entity.oFlapTime, this.entity.flapTime);
         this.jaw.xRot = (float)(Math.sin((double)($$5 * (float) (Math.PI * 2))) + 1.0) * 0.2F;
         float $$6 = (float)(Math.sin((double)($$5 * (float) (Math.PI * 2) - 1.0F)) + 1.0);
         $$6 = ($$6 * $$6 + $$6 * 2.0F) * 0.05F;
         pPoseStack.translate(0.0F, $$6 - 2.0F, -3.0F);
         pPoseStack.mulPose(Axis.XP.rotationDegrees($$6 * 2.0F));
         float $$7 = 0.0F;
         float $$8 = 20.0F;
         float $$9 = -12.0F;
         float $$10 = 1.5F;
         double[] $$11 = this.entity.getLatencyPos(6, this.a);
         float $$12 = Mth.wrapDegrees((float)(this.entity.getLatencyPos(5, this.a)[0] - this.entity.getLatencyPos(10, this.a)[0]));
         float $$13 = Mth.wrapDegrees((float)(this.entity.getLatencyPos(5, this.a)[0] + (double)($$12 / 2.0F)));
         float $$14 = $$5 * (float) (Math.PI * 2);

         for (int $$15 = 0; $$15 < 5; $$15++) {
            double[] $$16 = this.entity.getLatencyPos(5 - $$15, this.a);
            float $$17 = (float)Math.cos((double)((float)$$15 * 0.45F + $$14)) * 0.15F;
            this.neck.yRot = Mth.wrapDegrees((float)($$16[0] - $$11[0])) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.xRot = $$17 + this.entity.getHeadPartYOffset($$15, $$11, $$16) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
            this.neck.zRot = -Mth.wrapDegrees((float)($$16[0] - (double)$$13)) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.y = $$8;
            this.neck.z = $$9;
            this.neck.x = $$7;
            $$8 += Mth.sin(this.neck.xRot) * 10.0F;
            $$9 -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            $$7 -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
         }

         this.head.y = $$8;
         this.head.z = $$9;
         this.head.x = $$7;
         double[] $$18 = this.entity.getLatencyPos(0, this.a);
         this.head.yRot = Mth.wrapDegrees((float)($$18[0] - $$11[0])) * (float) (Math.PI / 180.0);
         this.head.xRot = Mth.wrapDegrees(this.entity.getHeadPartYOffset(6, $$11, $$18)) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
         this.head.zRot = -Mth.wrapDegrees((float)($$18[0] - (double)$$13)) * (float) (Math.PI / 180.0);
         this.head.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
         pPoseStack.pushPose();
         pPoseStack.translate(0.0F, 1.0F, 0.0F);
         pPoseStack.mulPose(Axis.ZP.rotationDegrees(-$$12 * 1.5F));
         pPoseStack.translate(0.0F, -1.0F, 0.0F);
         this.body.zRot = 0.0F;
         this.body.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
         float $$19 = $$5 * (float) (Math.PI * 2);
         this.leftWing.xRot = 0.125F - (float)Math.cos((double)$$19) * 0.2F;
         this.leftWing.yRot = -0.25F;
         this.leftWing.zRot = -((float)(Math.sin((double)$$19) + 0.125)) * 0.8F;
         this.leftWingTip.zRot = (float)(Math.sin((double)($$19 + 2.0F)) + 0.5) * 0.75F;
         this.rightWing.xRot = this.leftWing.xRot;
         this.rightWing.yRot = -this.leftWing.yRot;
         this.rightWing.zRot = -this.leftWing.zRot;
         this.rightWingTip.zRot = -this.leftWingTip.zRot;
         this.renderSide(
            pPoseStack,
            pBuffer,
            pPackedLight,
            pPackedOverlay,
            $$6,
            this.leftWing,
            this.leftFrontLeg,
            this.leftFrontLegTip,
            this.leftFrontFoot,
            this.leftRearLeg,
            this.leftRearLegTip,
            this.leftRearFoot,
            pColor
         );
         this.renderSide(
            pPoseStack,
            pBuffer,
            pPackedLight,
            pPackedOverlay,
            $$6,
            this.rightWing,
            this.rightFrontLeg,
            this.rightFrontLegTip,
            this.rightFrontFoot,
            this.rightRearLeg,
            this.rightRearLegTip,
            this.rightRearFoot,
            pColor
         );
         pPoseStack.popPose();
         float $$20 = -Mth.sin($$5 * (float) (Math.PI * 2)) * 0.0F;
         $$14 = $$5 * (float) (Math.PI * 2);
         $$8 = 10.0F;
         $$9 = 60.0F;
         $$7 = 0.0F;
         $$11 = this.entity.getLatencyPos(11, this.a);

         for (int $$21 = 0; $$21 < 12; $$21++) {
            $$18 = this.entity.getLatencyPos(12 + $$21, this.a);
            $$20 += Mth.sin((float)$$21 * 0.45F + $$14) * 0.05F;
            this.neck.yRot = (Mth.wrapDegrees((float)($$18[0] - $$11[0])) * 1.5F + 180.0F) * (float) (Math.PI / 180.0);
            this.neck.xRot = $$20 + (float)($$18[1] - $$11[1]) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
            this.neck.zRot = Mth.wrapDegrees((float)($$18[0] - (double)$$13)) * (float) (Math.PI / 180.0) * 1.5F;
            this.neck.y = $$8;
            this.neck.z = $$9;
            this.neck.x = $$7;
            $$8 += Mth.sin(this.neck.xRot) * 10.0F;
            $$9 -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            $$7 -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
         }

         pPoseStack.popPose();
      }

      private void renderSide(
         PoseStack pPoseStack,
         VertexConsumer pBuffer,
         int pPackedLight,
         int pPackedOverlay,
         float pRotation,
         ModelPart pWing,
         ModelPart pFrontLeg,
         ModelPart pFrontLegTip,
         ModelPart pFrontFoot,
         ModelPart pRearLeg,
         ModelPart pRearLegTip,
         ModelPart pRearFoot,
         int pAlpha
      ) {
         pRearLeg.xRot = 1.0F + pRotation * 0.1F;
         pRearLegTip.xRot = 0.5F + pRotation * 0.1F;
         pRearFoot.xRot = 0.75F + pRotation * 0.1F;
         pFrontLeg.xRot = 1.3F + pRotation * 0.1F;
         pFrontLegTip.xRot = -0.5F - pRotation * 0.1F;
         pFrontFoot.xRot = 0.75F + pRotation * 0.1F;
         pWing.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pAlpha);
         pFrontLeg.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pAlpha);
         pRearLeg.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pAlpha);
      }
   }
}
