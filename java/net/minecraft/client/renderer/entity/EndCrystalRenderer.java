package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderer extends EntityRenderer<EndCrystal> {
   private static final ResourceLocation END_CRYSTAL_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(END_CRYSTAL_LOCATION);
   private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
   private static final String GLASS = "glass";
   private static final String BASE = "base";
   private final ModelPart cube;
   private final ModelPart glass;
   private final ModelPart base;

   public EndCrystalRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.shadowRadius = 0.5F;
      ModelPart $$1 = pContext.bakeLayer(ModelLayers.END_CRYSTAL);
      this.glass = $$1.getChild("glass");
      this.cube = $$1.getChild("cube");
      this.base = $$1.getChild("base");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 32);
   }

   public void render(EndCrystal pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      float $$6 = getY(pEntity, pPartialTicks);
      float $$7 = ((float)pEntity.time + pPartialTicks) * 3.0F;
      VertexConsumer $$8 = pBuffer.getBuffer(RENDER_TYPE);
      pPoseStack.pushPose();
      pPoseStack.scale(2.0F, 2.0F, 2.0F);
      pPoseStack.translate(0.0F, -0.5F, 0.0F);
      int $$9 = OverlayTexture.NO_OVERLAY;
      if (pEntity.showsBottom()) {
         this.base.render(pPoseStack, $$8, pPackedLight, $$9);
      }

      pPoseStack.mulPose(Axis.YP.rotationDegrees($$7));
      pPoseStack.translate(0.0F, 1.5F + $$6 / 2.0F, 0.0F);
      pPoseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      this.glass.render(pPoseStack, $$8, pPackedLight, $$9);
      float $$10 = 0.875F;
      pPoseStack.scale(0.875F, 0.875F, 0.875F);
      pPoseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      pPoseStack.mulPose(Axis.YP.rotationDegrees($$7));
      this.glass.render(pPoseStack, $$8, pPackedLight, $$9);
      pPoseStack.scale(0.875F, 0.875F, 0.875F);
      pPoseStack.mulPose(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      pPoseStack.mulPose(Axis.YP.rotationDegrees($$7));
      this.cube.render(pPoseStack, $$8, pPackedLight, $$9);
      pPoseStack.popPose();
      pPoseStack.popPose();
      BlockPos $$11 = pEntity.getBeamTarget();
      if ($$11 != null) {
         float $$12 = (float)$$11.getX() + 0.5F;
         float $$13 = (float)$$11.getY() + 0.5F;
         float $$14 = (float)$$11.getZ() + 0.5F;
         float $$15 = (float)((double)$$12 - pEntity.getX());
         float $$16 = (float)((double)$$13 - pEntity.getY());
         float $$17 = (float)((double)$$14 - pEntity.getZ());
         pPoseStack.translate($$15, $$16, $$17);
         EnderDragonRenderer.renderCrystalBeams(-$$15, -$$16 + $$6, -$$17, pPartialTicks, pEntity.time, pPoseStack, pBuffer, pPackedLight);
      }

      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   public static float getY(EndCrystal pEndCrystal, float pPartialTick) {
      float $$2 = (float)pEndCrystal.time + pPartialTick;
      float $$3 = Mth.sin($$2 * 0.2F) / 2.0F + 0.5F;
      $$3 = ($$3 * $$3 + $$3) * 0.4F;
      return $$3 - 1.4F;
   }

   public ResourceLocation getTextureLocation(EndCrystal pEntity) {
      return END_CRYSTAL_LOCATION;
   }

   public boolean shouldRender(EndCrystal pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
      return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ) || pLivingEntity.getBeamTarget() != null;
   }
}
