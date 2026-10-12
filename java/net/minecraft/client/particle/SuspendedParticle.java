package net.minecraft.client.particle;

import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedParticle extends TextureSheetParticle {
   SuspendedParticle(ClientLevel pLevel, SpriteSet pSprites, double pX, double pY, double pZ) {
      super(pLevel, pX, pY - 0.125, pZ);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(pSprites);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   SuspendedParticle(ClientLevel pLevel, SpriteSet pSprites, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      super(pLevel, pX, pY - 0.125, pZ, pXSpeed, pYSpeed, pZSpeed);
      this.setSize(0.01F, 0.01F);
      this.pickSprite(pSprites);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
      this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      this.hasPhysics = false;
      this.friction = 1.0F;
      this.gravity = 0.0F;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @OnlyIn(Dist.CLIENT)
   public static class CrimsonSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public CrimsonSporeProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         RandomSource $$8 = pLevel.random;
         double $$9 = $$8.nextGaussian() * 1.0E-6F;
         double $$10 = $$8.nextGaussian() * 1.0E-4F;
         double $$11 = $$8.nextGaussian() * 1.0E-6F;
         SuspendedParticle $$12 = new SuspendedParticle(pLevel, this.sprite, pX, pY, pZ, $$9, $$10, $$11);
         $$12.setColor(0.9F, 0.4F, 0.5F);
         return $$12;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SporeBlossomAirProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SporeBlossomAirProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedParticle $$8 = new SuspendedParticle(pLevel, this.sprite, pX, pY, pZ, 0.0, -0.8F, 0.0) {
            @Override
            public Optional<ParticleGroup> getParticleGroup() {
               return Optional.of(ParticleGroup.SPORE_BLOSSOM);
            }
         };
         $$8.lifetime = Mth.randomBetweenInclusive(pLevel.random, 500, 1000);
         $$8.gravity = 0.01F;
         $$8.setColor(0.32F, 0.5F, 0.22F);
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class UnderwaterProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public UnderwaterProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedParticle $$8 = new SuspendedParticle(pLevel, this.sprite, pX, pY, pZ);
         $$8.setColor(0.4F, 0.4F, 0.7F);
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WarpedSporeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public WarpedSporeProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         double $$8 = (double)pLevel.random.nextFloat() * -1.9 * (double)pLevel.random.nextFloat() * 0.1;
         SuspendedParticle $$9 = new SuspendedParticle(pLevel, this.sprite, pX, pY, pZ, 0.0, $$8, 0.0);
         $$9.setColor(0.1F, 0.1F, 0.3F);
         $$9.setSize(0.001F, 0.001F);
         return $$9;
      }
   }
}
