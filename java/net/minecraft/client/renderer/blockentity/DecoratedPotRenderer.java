package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.Optional;
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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity> {
   private static final String NECK = "neck";
   private static final String FRONT = "front";
   private static final String BACK = "back";
   private static final String LEFT = "left";
   private static final String RIGHT = "right";
   private static final String TOP = "top";
   private static final String BOTTOM = "bottom";
   private final ModelPart neck;
   private final ModelPart frontSide;
   private final ModelPart backSide;
   private final ModelPart leftSide;
   private final ModelPart rightSide;
   private final ModelPart top;
   private final ModelPart bottom;
   private static final float WOBBLE_AMPLITUDE = 0.125F;

   public DecoratedPotRenderer(BlockEntityRendererProvider.Context pContext) {
      ModelPart $$1 = pContext.bakeLayer(ModelLayers.DECORATED_POT_BASE);
      this.neck = $$1.getChild("neck");
      this.top = $$1.getChild("top");
      this.bottom = $$1.getChild("bottom");
      ModelPart $$2 = pContext.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
      this.frontSide = $$2.getChild("front");
      this.backSide = $$2.getChild("back");
      this.leftSide = $$2.getChild("left");
      this.rightSide = $$2.getChild("right");
   }

   public static LayerDefinition createBaseLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      CubeDeformation $$2 = new CubeDeformation(0.2F);
      CubeDeformation $$3 = new CubeDeformation(-0.1F);
      $$1.addOrReplaceChild(
         "neck",
         CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, 17.0F, 4.0F, 8.0F, 3.0F, 8.0F, $$3).texOffs(0, 5).addBox(5.0F, 20.0F, 5.0F, 6.0F, 1.0F, 6.0F, $$2),
         PartPose.offsetAndRotation(0.0F, 37.0F, 16.0F, (float) Math.PI, 0.0F, 0.0F)
      );
      CubeListBuilder $$4 = CubeListBuilder.create().texOffs(-14, 13).addBox(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 14.0F);
      $$1.addOrReplaceChild("top", $$4, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, 0.0F, 0.0F));
      $$1.addOrReplaceChild("bottom", $$4, PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
      return LayerDefinition.create($$0, 32, 32);
   }

   public static LayerDefinition createSidesLayer() {
      MeshDefinition $$0 = new MeshDefinition();
      PartDefinition $$1 = $$0.getRoot();
      CubeListBuilder $$2 = CubeListBuilder.create().texOffs(1, 0).addBox(0.0F, 0.0F, 0.0F, 14.0F, 16.0F, 0.0F, EnumSet.of(Direction.NORTH));
      $$1.addOrReplaceChild("back", $$2, PartPose.offsetAndRotation(15.0F, 16.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
      $$1.addOrReplaceChild("left", $$2, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, (float) (-Math.PI / 2), (float) Math.PI));
      $$1.addOrReplaceChild("right", $$2, PartPose.offsetAndRotation(15.0F, 16.0F, 15.0F, 0.0F, (float) (Math.PI / 2), (float) Math.PI));
      $$1.addOrReplaceChild("front", $$2, PartPose.offsetAndRotation(1.0F, 16.0F, 15.0F, (float) Math.PI, 0.0F, 0.0F));
      return LayerDefinition.create($$0, 16, 16);
   }

   private static Material getSideMaterial(Optional<Item> pItem) {
      if (pItem.isPresent()) {
         Material $$1 = Sheets.getDecoratedPotMaterial(DecoratedPotPatterns.getPatternFromItem(pItem.get()));
         if ($$1 != null) {
            return $$1;
         }
      }

      return Sheets.DECORATED_POT_SIDE;
   }

   public void render(
      DecoratedPotBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      pPoseStack.pushPose();
      Direction $$6 = pBlockEntity.getDirection();
      pPoseStack.translate(0.5, 0.0, 0.5);
      pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F - $$6.toYRot()));
      pPoseStack.translate(-0.5, 0.0, -0.5);
      DecoratedPotBlockEntity.WobbleStyle $$7 = pBlockEntity.lastWobbleStyle;
      if ($$7 != null && pBlockEntity.getLevel() != null) {
         float $$8 = ((float)(pBlockEntity.getLevel().getGameTime() - pBlockEntity.wobbleStartedAtTick) + pPartialTick) / (float)$$7.duration;
         if ($$8 >= 0.0F && $$8 <= 1.0F) {
            if ($$7 == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
               float $$9 = 0.015625F;
               float $$10 = $$8 * (float) (Math.PI * 2);
               float $$11 = -1.5F * (Mth.cos($$10) + 0.5F) * Mth.sin($$10 / 2.0F);
               pPoseStack.rotateAround(Axis.XP.rotation($$11 * 0.015625F), 0.5F, 0.0F, 0.5F);
               float $$12 = Mth.sin($$10);
               pPoseStack.rotateAround(Axis.ZP.rotation($$12 * 0.015625F), 0.5F, 0.0F, 0.5F);
            } else {
               float $$13 = Mth.sin(-$$8 * 3.0F * (float) Math.PI) * 0.125F;
               float $$14 = 1.0F - $$8;
               pPoseStack.rotateAround(Axis.YP.rotation($$13 * $$14), 0.5F, 0.0F, 0.5F);
            }
         }
      }

      VertexConsumer $$15 = Sheets.DECORATED_POT_BASE.buffer(pBufferSource, RenderType::entitySolid);
      this.neck.render(pPoseStack, $$15, pPackedLight, pPackedOverlay);
      this.top.render(pPoseStack, $$15, pPackedLight, pPackedOverlay);
      this.bottom.render(pPoseStack, $$15, pPackedLight, pPackedOverlay);
      PotDecorations $$16 = pBlockEntity.getDecorations();
      this.renderSide(this.frontSide, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, getSideMaterial($$16.front()));
      this.renderSide(this.backSide, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, getSideMaterial($$16.back()));
      this.renderSide(this.leftSide, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, getSideMaterial($$16.left()));
      this.renderSide(this.rightSide, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, getSideMaterial($$16.right()));
      pPoseStack.popPose();
   }

   private void renderSide(ModelPart pModelPart, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, Material pMaterial) {
      pModelPart.render(pPoseStack, pMaterial.buffer(pBuffer, RenderType::entitySolid), pPackedLight, pPackedOverlay);
   }
}
