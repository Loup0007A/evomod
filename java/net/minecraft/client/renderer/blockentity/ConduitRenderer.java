package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity> {
   public static final Material SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/base"));
   public static final Material ACTIVE_SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/cage"));
   public static final Material WIND_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/wind"));
   public static final Material VERTICAL_WIND_TEXTURE = new Material(
      TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/wind_vertical")
   );
   public static final Material OPEN_EYE_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/open_eye"));
   public static final Material CLOSED_EYE_TEXTURE = new Material(
      TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/conduit/closed_eye")
   );
   private final ModelPart eye;
   private final ModelPart wind;
   private final ModelPart shell;
   private final ModelPart cage;
   private final BlockEntityRenderDispatcher renderer;

   public ConduitRenderer(BlockEntityRendererProvider.Context pContext) {
      this.renderer = pContext.getBlockEntityRenderDispatcher();
      this.eye = pContext.bakeLayer(ModelLayers.CONDUIT_EYE);
      this.wind = pContext.bakeLayer(ModelLayers.CONDUIT_WIND);
      this.shell = pContext.bakeLayer(ModelLayers.CONDUIT_SHELL);
      this.cage = pContext.bakeLayer(ModelLayers.CONDUIT_CAGE);
   }

   public static LayerDefinition createEyeLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild(
         "eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
      );
      return LayerDefinition.create($$0, 16, 16);
   }

   public static LayerDefinition createWindLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 32);
   }

   public static LayerDefinition createShellLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 32, 16);
   }

   public static LayerDefinition createCageLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 32, 16);
   }

   public void render(
      ConduitBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      float $$6 = (float)pBlockEntity.tickCount + pPartialTick;
      if (!pBlockEntity.isActive()) {
         float $$7 = pBlockEntity.getActiveRotation(0.0F);
         VertexConsumer $$8 = SHELL_TEXTURE.buffer(pBufferSource, RenderType::entitySolid);
         pPoseStack.pushPose();
         pPoseStack.translate(0.5F, 0.5F, 0.5F);
         pPoseStack.mulPose(new Quaternionf().rotationY($$7 * (float) (Math.PI / 180.0)));
         this.shell.render(pPoseStack, $$8, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
      } else {
         float $$9 = pBlockEntity.getActiveRotation(pPartialTick) * (180.0F / (float)Math.PI);
         float $$10 = Mth.sin($$6 * 0.1F) / 2.0F + 0.5F;
         $$10 = $$10 * $$10 + $$10;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5F, 0.3F + $$10 * 0.2F, 0.5F);
         Vector3f $$11 = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
         pPoseStack.mulPose(new Quaternionf().rotationAxis($$9 * (float) (Math.PI / 180.0), $$11));
         this.cage.render(pPoseStack, ACTIVE_SHELL_TEXTURE.buffer(pBufferSource, RenderType::entityCutoutNoCull), pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         int $$12 = pBlockEntity.tickCount / 66 % 3;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5F, 0.5F, 0.5F);
         if ($$12 == 1) {
            pPoseStack.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
         } else if ($$12 == 2) {
            pPoseStack.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
         }

         VertexConsumer $$13 = ($$12 == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE).buffer(pBufferSource, RenderType::entityCutoutNoCull);
         this.wind.render(pPoseStack, $$13, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         pPoseStack.pushPose();
         pPoseStack.translate(0.5F, 0.5F, 0.5F);
         pPoseStack.scale(0.875F, 0.875F, 0.875F);
         pPoseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
         this.wind.render(pPoseStack, $$13, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         Camera $$14 = this.renderer.camera;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5F, 0.3F + $$10 * 0.2F, 0.5F);
         pPoseStack.scale(0.5F, 0.5F, 0.5F);
         float $$15 = -$$14.getYRot();
         pPoseStack.mulPose(new Quaternionf().rotationYXZ($$15 * (float) (Math.PI / 180.0), $$14.getXRot() * (float) (Math.PI / 180.0), (float) Math.PI));
         float $$16 = 1.3333334F;
         pPoseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
         this.eye
            .render(
               pPoseStack,
               (pBlockEntity.isHunting() ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE).buffer(pBufferSource, RenderType::entityCutoutNoCull),
               pPackedLight,
               pPackedOverlay
            );
         pPoseStack.popPose();
      }
   }
}
