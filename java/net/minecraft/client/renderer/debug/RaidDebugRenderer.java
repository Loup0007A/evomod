package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private static final float TEXT_SCALE = 0.04F;
   private final Minecraft minecraft;
   private Collection<BlockPos> raidCenters = Lists.newArrayList();

   public RaidDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void setRaidCenters(Collection<BlockPos> pRaidCenters) {
      this.raidCenters = pRaidCenters;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      BlockPos $$5 = this.getCamera().getBlockPosition();

      for (BlockPos $$6 : this.raidCenters) {
         if ($$5.closerThan($$6, 160.0)) {
            highlightRaidCenter(pPoseStack, pBufferSource, $$6);
         }
      }
   }

   private static void highlightRaidCenter(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pPos) {
      DebugRenderer.renderFilledUnitCube(pPoseStack, pBuffer, pPos, 1.0F, 0.0F, 0.0F, 0.15F);
      int $$3 = -65536;
      renderTextOverBlock(pPoseStack, pBuffer, "Raid center", pPos, -65536);
   }

   private static void renderTextOverBlock(PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, BlockPos pPos, int pColor) {
      double $$5 = (double)pPos.getX() + 0.5;
      double $$6 = (double)pPos.getY() + 1.3;
      double $$7 = (double)pPos.getZ() + 0.5;
      DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, $$5, $$6, $$7, pColor, 0.04F, true, 0.0F, true);
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }
}
