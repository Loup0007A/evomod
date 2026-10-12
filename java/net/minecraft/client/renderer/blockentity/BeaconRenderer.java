package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderer implements BlockEntityRenderer<BeaconBlockEntity> {
   public static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
   public static final int MAX_RENDER_Y = 1024;

   public BeaconRenderer(BlockEntityRendererProvider.Context pContext) {
   }

   public void render(
      BeaconBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      long $$6 = pBlockEntity.getLevel().getGameTime();
      List<BeaconBlockEntity.BeaconBeamSection> $$7 = pBlockEntity.getBeamSections();
      int $$8 = 0;

      for (int $$9 = 0; $$9 < $$7.size(); $$9++) {
         BeaconBlockEntity.BeaconBeamSection $$10 = $$7.get($$9);
         renderBeaconBeam(pPoseStack, pBufferSource, pPartialTick, $$6, $$8, $$9 == $$7.size() - 1 ? 1024 : $$10.getHeight(), $$10.getColor());
         $$8 += $$10.getHeight();
      }
   }

   private static void renderBeaconBeam(
      PoseStack pPoseStack, MultiBufferSource pBufferSource, float pPartialTick, long pGameTime, int pYOffset, int pHeight, int pColor
   ) {
      renderBeaconBeam(pPoseStack, pBufferSource, BEAM_LOCATION, pPartialTick, 1.0F, pGameTime, pYOffset, pHeight, pColor, 0.2F, 0.25F);
   }

   public static void renderBeaconBeam(
      PoseStack pPoseStack,
      MultiBufferSource pBufferSource,
      ResourceLocation pBeamLocation,
      float pPartialTick,
      float pTextureScale,
      long pGameTime,
      int pYOffset,
      int pHeight,
      int pColor,
      float pBeamRadius,
      float pGlowRadius
   ) {
      int $$11 = pYOffset + pHeight;
      pPoseStack.pushPose();
      pPoseStack.translate(0.5, 0.0, 0.5);
      float $$12 = (float)Math.floorMod(pGameTime, 40) + pPartialTick;
      float $$13 = pHeight < 0 ? $$12 : -$$12;
      float $$14 = Mth.frac($$13 * 0.2F - (float)Mth.floor($$13 * 0.1F));
      pPoseStack.pushPose();
      pPoseStack.mulPose(Axis.YP.rotationDegrees($$12 * 2.25F - 45.0F));
      float $$15 = 0.0F;
      float $$18 = 0.0F;
      float $$19 = -pBeamRadius;
      float $$20 = 0.0F;
      float $$21 = 0.0F;
      float $$22 = -pBeamRadius;
      float $$23 = 0.0F;
      float $$24 = 1.0F;
      float $$25 = -1.0F + $$14;
      float $$26 = (float)pHeight * pTextureScale * (0.5F / pBeamRadius) + $$25;
      renderPart(
         pPoseStack,
         pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, false)),
         pColor,
         pYOffset,
         $$11,
         0.0F,
         pBeamRadius,
         pBeamRadius,
         0.0F,
         $$19,
         0.0F,
         0.0F,
         $$22,
         0.0F,
         1.0F,
         $$26,
         $$25
      );
      pPoseStack.popPose();
      $$15 = -pGlowRadius;
      float $$28 = -pGlowRadius;
      $$18 = -pGlowRadius;
      $$19 = -pGlowRadius;
      $$23 = 0.0F;
      $$24 = 1.0F;
      $$25 = -1.0F + $$14;
      $$26 = (float)pHeight * pTextureScale + $$25;
      renderPart(
         pPoseStack,
         pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, true)),
         FastColor.ARGB32.color(32, pColor),
         pYOffset,
         $$11,
         $$15,
         $$28,
         pGlowRadius,
         $$18,
         $$19,
         pGlowRadius,
         pGlowRadius,
         pGlowRadius,
         0.0F,
         1.0F,
         $$26,
         $$25
      );
      pPoseStack.popPose();
   }

   private static void renderPart(
      PoseStack pPoseStack,
      VertexConsumer pConsumer,
      int pColor,
      int pMinY,
      int pMaxY,
      float pX1,
      float pZ1,
      float pX2,
      float pZ2,
      float pX3,
      float pZ3,
      float pX4,
      float pZ4,
      float pMinU,
      float pMaxU,
      float pMinV,
      float pMaxV
   ) {
      PoseStack.Pose $$17 = pPoseStack.last();
      renderQuad($$17, pConsumer, pColor, pMinY, pMaxY, pX1, pZ1, pX2, pZ2, pMinU, pMaxU, pMinV, pMaxV);
      renderQuad($$17, pConsumer, pColor, pMinY, pMaxY, pX4, pZ4, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV);
      renderQuad($$17, pConsumer, pColor, pMinY, pMaxY, pX2, pZ2, pX4, pZ4, pMinU, pMaxU, pMinV, pMaxV);
      renderQuad($$17, pConsumer, pColor, pMinY, pMaxY, pX3, pZ3, pX1, pZ1, pMinU, pMaxU, pMinV, pMaxV);
   }

   private static void renderQuad(
      PoseStack.Pose pPose,
      VertexConsumer pConsumer,
      int pColor,
      int pMinY,
      int pMaxY,
      float pMinX,
      float pMinZ,
      float pMaxX,
      float pMaxZ,
      float pMinU,
      float pMaxU,
      float pMinV,
      float pMaxV
   ) {
      addVertex(pPose, pConsumer, pColor, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
      addVertex(pPose, pConsumer, pColor, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
      addVertex(pPose, pConsumer, pColor, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
      addVertex(pPose, pConsumer, pColor, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
   }

   private static void addVertex(PoseStack.Pose pPose, VertexConsumer pConsumer, int pColor, int pY, float pX, float pZ, float pU, float pV) {
      pConsumer.addVertex(pPose, pX, (float)pY, pZ)
         .setColor(pColor)
         .setUv(pU, pV)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pPose, 0.0F, 1.0F, 0.0F);
   }

   public boolean shouldRenderOffScreen(BeaconBlockEntity pBlockEntity) {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 256;
   }

   public boolean shouldRender(BeaconBlockEntity pBlockEntity, Vec3 pCameraPos) {
      return Vec3.atCenterOf(pBlockEntity.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(pCameraPos.multiply(1.0, 0.0, 1.0), (double)this.getViewDistance());
   }
}
