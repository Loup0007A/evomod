package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TrackingEmitter extends NoRenderParticle {
   private final Entity entity;
   private int life;
   private final int lifeTime;
   private final ParticleOptions particleType;

   public TrackingEmitter(ClientLevel pLevel, Entity pEntity, ParticleOptions pParticleType) {
      this(pLevel, pEntity, pParticleType, 3);
   }

   public TrackingEmitter(ClientLevel pLevel, Entity pEntity, ParticleOptions pParticleType, int pLifetime) {
      this(pLevel, pEntity, pParticleType, pLifetime, pEntity.getDeltaMovement());
   }

   private TrackingEmitter(ClientLevel pLevel, Entity pEntity, ParticleOptions pParticleType, int pLifetime, Vec3 pSpeedVector) {
      super(pLevel, pEntity.getX(), pEntity.getY(0.5), pEntity.getZ(), pSpeedVector.x, pSpeedVector.y, pSpeedVector.z);
      this.entity = pEntity;
      this.lifeTime = pLifetime;
      this.particleType = pParticleType;
      this.tick();
   }

   @Override
   public void tick() {
      for (int $$0 = 0; $$0 < 16; $$0++) {
         double $$1 = (double)(this.random.nextFloat() * 2.0F - 1.0F);
         double $$2 = (double)(this.random.nextFloat() * 2.0F - 1.0F);
         double $$3 = (double)(this.random.nextFloat() * 2.0F - 1.0F);
         if (!($$1 * $$1 + $$2 * $$2 + $$3 * $$3 > 1.0)) {
            double $$4 = this.entity.getX($$1 / 4.0);
            double $$5 = this.entity.getY(0.5 + $$2 / 4.0);
            double $$6 = this.entity.getZ($$3 / 4.0);
            this.level.addParticle(this.particleType, false, $$4, $$5, $$6, $$1, $$2 + 0.2, $$3);
         }
      }

      this.life++;
      if (this.life >= this.lifeTime) {
         this.remove();
      }
   }
}
