package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowLayer<T extends LivingEntity, M extends PlayerModel<T>> extends StuckInBodyLayer<T, M> {
   private final EntityRenderDispatcher dispatcher;

   public ArrowLayer(EntityRendererProvider.Context pContext, LivingEntityRenderer<T, M> pRenderer) {
      super(pRenderer);
      this.dispatcher = pContext.getEntityRenderDispatcher();
   }

   @Override
   protected int numStuck(T pEntity) {
      return pEntity.getArrowCount();
   }

   @Override
   protected void renderStuckItem(
      PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Entity pEntity, float pX, float pY, float pZ, float pPartialTick
   ) {
      float $$8 = Mth.sqrt(pX * pX + pZ * pZ);
      Arrow $$9 = new Arrow(pEntity.level(), pEntity.getX(), pEntity.getY(), pEntity.getZ(), ItemStack.EMPTY, null);
      $$9.setYRot((float)(Math.atan2((double)pX, (double)pZ) * 180.0F / (float)Math.PI));
      $$9.setXRot((float)(Math.atan2((double)pY, (double)$$8) * 180.0F / (float)Math.PI));
      $$9.yRotO = $$9.getYRot();
      $$9.xRotO = $$9.getXRot();
      this.dispatcher.render($$9, 0.0, 0.0, 0.0, 0.0F, pPartialTick, pPoseStack, pBuffer, pPackedLight);
   }
}
