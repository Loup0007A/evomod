package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlameParticle extends RisingParticle {
   FlameParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public void move(double pX, double pY, double pZ) {
      this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
      this.setLocationFromBoundingbox();
   }

   @Override
   public float getQuadSize(float pScaleFactor) {
      float $$1 = ((float)this.age + pScaleFactor) / (float)this.lifetime;
      return this.quadSize * (1.0F - $$1 * $$1 * 0.5F);
   }

   @Override
   public int getLightColor(float pPartialTick) {
      float $$1 = ((float)this.age + pPartialTick) / (float)this.lifetime;
      $$1 = Mth.clamp($$1, 0.0F, 1.0F);
      int $$2 = super.getLightColor(pPartialTick);
      int $$3 = $$2 & 0xFF;
      int $$4 = $$2 >> 16 & 0xFF;
      $$3 += (int)($$1 * 15.0F * 16.0F);
      if ($$3 > 240) {
         $$3 = 240;
      }

      return $$3 | $$4 << 16;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         FlameParticle $$8 = new FlameParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SmallFlameProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         FlameParticle $$8 = new FlameParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         $$8.scale(0.5F);
         return $$8;
      }
   }
}
