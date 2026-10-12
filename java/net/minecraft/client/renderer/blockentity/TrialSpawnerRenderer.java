package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrialSpawnerRenderer implements BlockEntityRenderer<TrialSpawnerBlockEntity> {
   private final EntityRenderDispatcher entityRenderer;

   public TrialSpawnerRenderer(BlockEntityRendererProvider.Context pContext) {
      this.entityRenderer = pContext.getEntityRenderer();
   }

   public void render(
      TrialSpawnerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      Level $$6 = pBlockEntity.getLevel();
      if ($$6 != null) {
         TrialSpawner $$7 = pBlockEntity.getTrialSpawner();
         TrialSpawnerData $$8 = $$7.getData();
         Entity $$9 = $$8.getOrCreateDisplayEntity($$7, $$6, $$7.getState());
         if ($$9 != null) {
            SpawnerRenderer.renderEntityInSpawner(
               pPartialTick, pPoseStack, pBufferSource, pPackedLight, $$9, this.entityRenderer, $$8.getOSpin(), $$8.getSpin()
            );
         }
      }
   }
}
