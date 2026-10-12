package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public WaterDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      BlockPos $$5 = this.minecraft.player.blockPosition();
      LevelReader $$6 = this.minecraft.player.level();

      for (BlockPos $$7 : BlockPos.betweenClosed($$5.offset(-10, -10, -10), $$5.offset(10, 10, 10))) {
         FluidState $$8 = $$6.getFluidState($$7);
         if ($$8.is(FluidTags.WATER)) {
            double $$9 = (double)((float)$$7.getY() + $$8.getHeight($$6, $$7));
            DebugRenderer.renderFilledBox(
               pPoseStack,
               pBufferSource,
               new AABB(
                     (double)((float)$$7.getX() + 0.01F),
                     (double)((float)$$7.getY() + 0.01F),
                     (double)((float)$$7.getZ() + 0.01F),
                     (double)((float)$$7.getX() + 0.99F),
                     $$9,
                     (double)((float)$$7.getZ() + 0.99F)
                  )
                  .move(-pCamX, -pCamY, -pCamZ),
               0.0F,
               1.0F,
               0.0F,
               0.15F
            );
         }
      }

      for (BlockPos $$10 : BlockPos.betweenClosed($$5.offset(-10, -10, -10), $$5.offset(10, 10, 10))) {
         FluidState $$11 = $$6.getFluidState($$10);
         if ($$11.is(FluidTags.WATER)) {
            DebugRenderer.renderFloatingText(
               pPoseStack,
               pBufferSource,
               String.valueOf($$11.getAmount()),
               (double)$$10.getX() + 0.5,
               (double)((float)$$10.getY() + $$11.getHeight($$6, $$10)),
               (double)$$10.getZ() + 0.5,
               -16777216
            );
         }
      }
   }
}
