package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
   private final double scale;
   private final int tickDelayInBetween;

   GustSeedParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pScale, int pLifetime, int pTickDelayInBetween) {
      super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
      this.scale = pScale;
      this.lifetime = pLifetime;
      this.tickDelayInBetween = pTickDelayInBetween;
   }

   @Override
   public void tick() {
      if (this.age % (this.tickDelayInBetween + 1) == 0) {
         for (int $$0 = 0; $$0 < 3; $$0++) {
            double $$1 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double $$2 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            double $$3 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * this.scale;
            this.level.addParticle(ParticleTypes.GUST, $$1, $$2, $$3, (double)((float)this.age / (float)this.lifetime), 0.0, 0.0);
         }
      }

      if (this.age++ == this.lifetime) {
         this.remove();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final double scale;
      private final int lifetime;
      private final int tickDelayInBetween;

      public Provider(double pScale, int pLifetime, int pTickDelayInBetween) {
         this.scale = pScale;
         this.lifetime = pLifetime;
         this.tickDelayInBetween = pTickDelayInBetween;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         return new GustSeedParticle(pLevel, pX, pY, pZ, this.scale, this.lifetime, this.tickDelayInBetween);
      }
   }
}
