package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuspendedTownParticle extends TextureSheetParticle {
   SuspendedTownParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
      float $$7 = this.random.nextFloat() * 0.1F + 0.2F;
      this.rCol = $$7;
      this.gCol = $$7;
      this.bCol = $$7;
      this.setSize(0.02F, 0.02F);
      this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.5F);
      this.xd *= 0.02F;
      this.yd *= 0.02F;
      this.zd *= 0.02F;
      this.lifetime = (int)(20.0 / (Math.random() * 0.8 + 0.2));
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
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.lifetime-- <= 0) {
         this.remove();
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 0.99;
         this.yd *= 0.99;
         this.zd *= 0.99;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ComposterFillProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ComposterFillProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedTownParticle $$8 = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         $$8.setColor(1.0F, 1.0F, 1.0F);
         $$8.setLifetime(3 + pLevel.getRandom().nextInt(5));
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DolphinSpeedProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public DolphinSpeedProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedTownParticle $$8 = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.setColor(0.3F, 0.5F, 1.0F);
         $$8.pickSprite(this.sprite);
         $$8.setAlpha(1.0F - pLevel.random.nextFloat() * 0.7F);
         $$8.setLifetime($$8.getLifetime() / 2);
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class EggCrackProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public EggCrackProvider(SpriteSet pSprite) {
         this.sprite = pSprite;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedTownParticle $$8 = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         $$8.setColor(1.0F, 1.0F, 1.0F);
         return $$8;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HappyVillagerProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public HappyVillagerProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         SuspendedTownParticle $$8 = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         $$8.setColor(1.0F, 1.0F, 1.0F);
         return $$8;
      }
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
         SuspendedTownParticle $$8 = new SuspendedTownParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         $$8.pickSprite(this.sprite);
         return $$8;
      }
   }
}
