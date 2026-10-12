package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SignRenderer implements BlockEntityRenderer<SignBlockEntity> {
   private static final String STICK = "stick";
   private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private static final float RENDER_SCALE = 0.6666667F;
   private static final Vec3 TEXT_OFFSET = new Vec3(0.0, 0.33333334F, 0.046666667F);
   private final Map<WoodType, SignRenderer.SignModel> signModels;
   private final Font font;

   public SignRenderer(BlockEntityRendererProvider.Context pContext) {
      this.signModels = WoodType.values()
         .collect(
            ImmutableMap.toImmutableMap(
               p_173645_ -> p_173645_, p_173651_ -> new SignRenderer.SignModel(pContext.bakeLayer(ModelLayers.createSignModelName(p_173651_)))
            )
         );
      this.font = pContext.getFont();
   }

   public void render(
      SignBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      BlockState $$6 = pBlockEntity.getBlockState();
      SignBlock $$7 = (SignBlock)$$6.getBlock();
      WoodType $$8 = SignBlock.getWoodType($$7);
      SignRenderer.SignModel $$9 = this.signModels.get($$8);
      $$9.stick.visible = $$6.getBlock() instanceof StandingSignBlock;
      this.renderSignWithText(pBlockEntity, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, $$6, $$7, $$8, $$9);
   }

   public float getSignModelRenderScale() {
      return 0.6666667F;
   }

   public float getSignTextRenderScale() {
      return 0.6666667F;
   }

   void renderSignWithText(
      SignBlockEntity pSignEntity,
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      int pPackedOverlay,
      BlockState pState,
      SignBlock pSignBlock,
      WoodType pWoodType,
      Model pModel
   ) {
      pPoseStack.pushPose();
      this.translateSign(pPoseStack, -pSignBlock.getYRotationDegrees(pState), pState);
      this.renderSign(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pWoodType, pModel);
      this.renderSignText(
         pSignEntity.getBlockPos(),
         pSignEntity.getFrontText(),
         pPoseStack,
         pBuffer,
         pPackedLight,
         pSignEntity.getTextLineHeight(),
         pSignEntity.getMaxTextLineWidth(),
         true
      );
      this.renderSignText(
         pSignEntity.getBlockPos(),
         pSignEntity.getBackText(),
         pPoseStack,
         pBuffer,
         pPackedLight,
         pSignEntity.getTextLineHeight(),
         pSignEntity.getMaxTextLineWidth(),
         false
      );
      pPoseStack.popPose();
   }

   void translateSign(PoseStack pPoseStack, float pYRot, BlockState pState) {
      pPoseStack.translate(0.5F, 0.75F * this.getSignModelRenderScale(), 0.5F);
      pPoseStack.mulPose(Axis.YP.rotationDegrees(pYRot));
      if (!(pState.getBlock() instanceof StandingSignBlock)) {
         pPoseStack.translate(0.0F, -0.3125F, -0.4375F);
      }
   }

   void renderSign(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, WoodType pWoodType, Model pModel) {
      pPoseStack.pushPose();
      float $$6 = this.getSignModelRenderScale();
      pPoseStack.scale($$6, -$$6, -$$6);
      Material $$7 = this.getSignMaterial(pWoodType);
      VertexConsumer $$8 = $$7.buffer(pBuffer, pModel::renderType);
      this.renderSignModel(pPoseStack, pPackedLight, pPackedOverlay, pModel, $$8);
      pPoseStack.popPose();
   }

   void renderSignModel(PoseStack pPoseStack, int pPackedLight, int pPackedOverlay, Model pModel, VertexConsumer pVertexConsumer) {
      SignRenderer.SignModel $$5 = (SignRenderer.SignModel)pModel;
      $$5.root.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay);
   }

   Material getSignMaterial(WoodType pWoodType) {
      return Sheets.getSignMaterial(pWoodType);
   }

   void renderSignText(
      BlockPos pPos, SignText pText, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pLineHeight, int pMaxWidth, boolean pIsFrontText
   ) {
      pPoseStack.pushPose();
      this.translateSignText(pPoseStack, pIsFrontText, this.getTextOffset());
      int $$8 = getDarkColor(pText);
      int $$9 = 4 * pLineHeight / 2;
      FormattedCharSequence[] $$10 = pText.getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(), p_277227_ -> {
         List<FormattedCharSequence> $$2 = this.font.split(p_277227_, pMaxWidth);
         return $$2.isEmpty() ? FormattedCharSequence.EMPTY : $$2.get(0);
      });
      int $$11;
      boolean $$12;
      int $$13;
      if (pText.hasGlowingText()) {
         $$11 = pText.getColor().getTextColor();
         $$12 = isOutlineVisible(pPos, $$11);
         $$13 = 15728880;
      } else {
         $$11 = $$8;
         $$12 = false;
         $$13 = pPackedLight;
      }

      for (int $$17 = 0; $$17 < 4; $$17++) {
         FormattedCharSequence $$18 = $$10[$$17];
         float $$19 = (float)(-this.font.width($$18) / 2);
         if ($$12) {
            this.font.drawInBatch8xOutline($$18, $$19, (float)($$17 * pLineHeight - $$9), $$11, $$8, pPoseStack.last().pose(), pBuffer, $$13);
         } else {
            this.font
               .drawInBatch(
                  $$18, $$19, (float)($$17 * pLineHeight - $$9), $$11, false, pPoseStack.last().pose(), pBuffer, Font.DisplayMode.POLYGON_OFFSET, 0, $$13
               );
         }
      }

      pPoseStack.popPose();
   }

   private void translateSignText(PoseStack pPoseStack, boolean pIsFrontText, Vec3 pOffset) {
      if (!pIsFrontText) {
         pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      }

      float $$3 = 0.015625F * this.getSignTextRenderScale();
      pPoseStack.translate(pOffset.x, pOffset.y, pOffset.z);
      pPoseStack.scale($$3, -$$3, $$3);
   }

   Vec3 getTextOffset() {
      return TEXT_OFFSET;
   }

   static boolean isOutlineVisible(BlockPos pPos, int pTextColor) {
      if (pTextColor == DyeColor.BLACK.getTextColor()) {
         return true;
      } else {
         Minecraft $$2 = Minecraft.getInstance();
         LocalPlayer $$3 = $$2.player;
         if ($$3 != null && $$2.options.getCameraType().isFirstPerson() && $$3.isScoping()) {
            return true;
         } else {
            Entity $$4 = $$2.getCameraEntity();
            return $$4 != null && $$4.distanceToSqr(Vec3.atCenterOf(pPos)) < (double)OUTLINE_RENDER_DISTANCE;
         }
      }
   }

   public static int getDarkColor(SignText pSignText) {
      int $$1 = pSignText.getColor().getTextColor();
      if ($$1 == DyeColor.BLACK.getTextColor() && pSignText.hasGlowingText()) {
         return -988212;
      } else {
         double $$2 = 0.4;
         int $$3 = (int)((double)FastColor.ARGB32.red($$1) * 0.4);
         int $$4 = (int)((double)FastColor.ARGB32.green($$1) * 0.4);
         int $$5 = (int)((double)FastColor.ARGB32.blue($$1) * 0.4);
         return FastColor.ARGB32.color(0, $$3, $$4, $$5);
      }
   }

   public static SignRenderer.SignModel createSignModel(EntityModelSet pEntityModelSet, WoodType pWoodType) {
      return new SignRenderer.SignModel(pEntityModelSet.bakeLayer(ModelLayers.createSignModelName(pWoodType)));
   }

   public static LayerDefinition createSignLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      $$1.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
      $$1.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO);
      return LayerDefinition.create($$0, 64, 32);
   }

   @OnlyIn(Dist.CLIENT)
   public static final class SignModel extends Model {
      public final ModelPart root;
      public final ModelPart stick;

      public SignModel(ModelPart pRoot) {
         super(RenderType::entityCutoutNoCull);
         this.root = pRoot;
         this.stick = pRoot.getChild("stick");
      }

      @Override
      public void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
         this.root.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
      }
   }
}
