package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemPickupParticle extends Particle {
   private static final int LIFE_TIME = 3;
   private final RenderBuffers renderBuffers;
   private final Entity itemEntity;
   private final Entity target;
   private int life;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private double targetX;
   private double targetY;
   private double targetZ;
   private double targetXOld;
   private double targetYOld;
   private double targetZOld;

   public ItemPickupParticle(EntityRenderDispatcher pEntityRenderDispatcher, RenderBuffers pBuffers, ClientLevel pLevel, Entity pItemEntity, Entity pTarget) {
      this(pEntityRenderDispatcher, pBuffers, pLevel, pItemEntity, pTarget, pItemEntity.getDeltaMovement());
   }

   private ItemPickupParticle(
      EntityRenderDispatcher pEntityRenderDispatcher, RenderBuffers pBuffers, ClientLevel pLevel, Entity pItemEntity, Entity pTarget, Vec3 pSpeedVector
   ) {
      super(pLevel, pItemEntity.getX(), pItemEntity.getY(), pItemEntity.getZ(), pSpeedVector.x, pSpeedVector.y, pSpeedVector.z);
      this.renderBuffers = pBuffers;
      this.itemEntity = this.getSafeCopy(pItemEntity);
      this.target = pTarget;
      this.entityRenderDispatcher = pEntityRenderDispatcher;
      this.updatePosition();
      this.saveOldPosition();
   }

   private Entity getSafeCopy(Entity pEntity) {
      return (Entity)(!(pEntity instanceof ItemEntity) ? pEntity : ((ItemEntity)pEntity).copy());
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.CUSTOM;
   }

   @Override
   public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
      float $$3 = ((float)this.life + pPartialTicks) / 3.0F;
      $$3 *= $$3;
      double $$4 = Mth.lerp((double)pPartialTicks, this.targetXOld, this.targetX);
      double $$5 = Mth.lerp((double)pPartialTicks, this.targetYOld, this.targetY);
      double $$6 = Mth.lerp((double)pPartialTicks, this.targetZOld, this.targetZ);
      double $$7 = Mth.lerp((double)$$3, this.itemEntity.getX(), $$4);
      double $$8 = Mth.lerp((double)$$3, this.itemEntity.getY(), $$5);
      double $$9 = Mth.lerp((double)$$3, this.itemEntity.getZ(), $$6);
      MultiBufferSource.BufferSource $$10 = this.renderBuffers.bufferSource();
      Vec3 $$11 = pRenderInfo.getPosition();
      this.entityRenderDispatcher
         .render(
            this.itemEntity,
            $$7 - $$11.x(),
            $$8 - $$11.y(),
            $$9 - $$11.z(),
            this.itemEntity.getYRot(),
            pPartialTicks,
            new PoseStack(),
            $$10,
            this.entityRenderDispatcher.getPackedLightCoords(this.itemEntity, pPartialTicks)
         );
      $$10.endBatch();
   }

   @Override
   public void tick() {
      this.life++;
      if (this.life == 3) {
         this.remove();
      }

      this.saveOldPosition();
      this.updatePosition();
   }

   private void updatePosition() {
      this.targetX = this.target.getX();
      this.targetY = (this.target.getY() + this.target.getEyeY()) / 2.0;
      this.targetZ = this.target.getZ();
   }

   private void saveOldPosition() {
      this.targetXOld = this.targetX;
      this.targetYOld = this.targetY;
      this.targetZOld = this.targetZ;
   }
}
