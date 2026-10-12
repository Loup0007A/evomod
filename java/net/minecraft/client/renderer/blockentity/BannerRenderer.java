package net.minecraft.client.renderer.blockentity;

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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity> {
   private static final int BANNER_WIDTH = 20;
   private static final int BANNER_HEIGHT = 40;
   private static final int MAX_PATTERNS = 16;
   public static final String FLAG = "flag";
   private static final String POLE = "pole";
   private static final String BAR = "bar";
   private final ModelPart flag;
   private final ModelPart pole;
   private final ModelPart bar;

   public BannerRenderer(BlockEntityRendererProvider.Context pContext) {
      ModelPart $$1 = pContext.bakeLayer(ModelLayers.BANNER);
      this.flag = $$1.getChild("flag");
      this.pole = $$1.getChild("pole");
      this.bar = $$1.getChild("bar");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -30.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, -32.0F, -1.0F, 20.0F, 2.0F, 2.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 64);
   }

   public void render(
      BannerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      float $$6 = 0.6666667F;
      boolean $$7 = pBlockEntity.getLevel() == null;
      pPoseStack.pushPose();
      long $$8;
      if ($$7) {
         $$8 = 0L;
         pPoseStack.translate(0.5F, 0.5F, 0.5F);
         this.pole.visible = true;
      } else {
         $$8 = pBlockEntity.getLevel().getGameTime();
         BlockState $$10 = pBlockEntity.getBlockState();
         if ($$10.getBlock() instanceof BannerBlock) {
            pPoseStack.translate(0.5F, 0.5F, 0.5F);
            float $$11 = -RotationSegment.convertToDegrees($$10.getValue(BannerBlock.ROTATION));
            pPoseStack.mulPose(Axis.YP.rotationDegrees($$11));
            this.pole.visible = true;
         } else {
            pPoseStack.translate(0.5F, -0.16666667F, 0.5F);
            float $$12 = -$$10.getValue(WallBannerBlock.FACING).toYRot();
            pPoseStack.mulPose(Axis.YP.rotationDegrees($$12));
            pPoseStack.translate(0.0F, -0.3125F, -0.4375F);
            this.pole.visible = false;
         }
      }

      pPoseStack.pushPose();
      pPoseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
      VertexConsumer $$13 = ModelBakery.BANNER_BASE.buffer(pBufferSource, RenderType::entitySolid);
      this.pole.render(pPoseStack, $$13, pPackedLight, pPackedOverlay);
      this.bar.render(pPoseStack, $$13, pPackedLight, pPackedOverlay);
      BlockPos $$14 = pBlockEntity.getBlockPos();
      float $$15 = ((float)Math.floorMod((long)($$14.getX() * 7 + $$14.getY() * 9 + $$14.getZ() * 13) + $$8, 100L) + pPartialTick) / 100.0F;
      this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * $$15)) * (float) Math.PI;
      this.flag.y = -32.0F;
      renderPatterns(
         pPoseStack,
         pBufferSource,
         pPackedLight,
         pPackedOverlay,
         this.flag,
         ModelBakery.BANNER_BASE,
         true,
         pBlockEntity.getBaseColor(),
         pBlockEntity.getPatterns()
      );
      pPoseStack.popPose();
      pPoseStack.popPose();
   }

   public static void renderPatterns(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      int pPackedOverlay,
      ModelPart pFlagPart,
      Material pFlagMaterial,
      boolean pBanner,
      DyeColor pBaseColor,
      BannerPatternLayers pPatterns
   ) {
      renderPatterns(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, pFlagMaterial, pBanner, pBaseColor, pPatterns, false);
   }

   public static void renderPatterns(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      int pPackedOverlay,
      ModelPart pFlagPart,
      Material pFlagMaterial,
      boolean pBanner,
      DyeColor pBaseColor,
      BannerPatternLayers pPatterns,
      boolean pGlint
   ) {
      pFlagPart.render(pPoseStack, pFlagMaterial.buffer(pBuffer, RenderType::entitySolid, pGlint), pPackedLight, pPackedOverlay);
      renderPatternLayer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, pBanner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE, pBaseColor);

      for (int $$10 = 0; $$10 < 16 && $$10 < pPatterns.layers().size(); $$10++) {
         BannerPatternLayers.Layer $$11 = pPatterns.layers().get($$10);
         Material $$12 = pBanner ? Sheets.getBannerMaterial($$11.pattern()) : Sheets.getShieldMaterial($$11.pattern());
         renderPatternLayer(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pFlagPart, $$12, $$11.color());
      }
   }

   private static void renderPatternLayer(
      PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, ModelPart pFlagPart, Material pMaterial, DyeColor pColor
   ) {
      int $$7 = pColor.getTextureDiffuseColor();
      pFlagPart.render(pPoseStack, pMaterial.buffer(pBuffer, RenderType::entityNoOutline), pPackedLight, pPackedOverlay, $$7);
   }
}
