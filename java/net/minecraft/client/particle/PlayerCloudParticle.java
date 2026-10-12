package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerCloudParticle extends TextureSheetParticle {
   private final SpriteSet sprites;

   PlayerCloudParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
      this.friction = 0.96F;
      this.sprites = pSprites;
      float $$8 = 2.5F;
      this.xd *= 0.1F;
      this.yd *= 0.1F;
      this.zd *= 0.1F;
      this.xd += pXSpeed;
      this.yd += pYSpeed;
      this.zd += pZSpeed;
      float $$9 = 1.0F - (float)(Math.random() * 0.3F);
      this.rCol = $$9;
      this.gCol = $$9;
      this.bCol = $$9;
      this.quadSize *= 1.875F;
      int $$10 = (int)(8.0 / (Math.random() * 0.8 + 0.3));
      this.lifetime = (int)Math.max((float)$$10 * 2.5F, 1.0F);
      this.hasPhysics = false;
      this.setSpriteFromAge(pSprites);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Override
   public float getQuadSize(float pScaleFactor) {
      return this.quadSize * Mth.clamp(((float)this.age + pScaleFactor) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
         Player $$0 = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0, false);
         if ($$0 != null) {
            double $$1 = $$0.getY();
            if (this.y > $$1) {
               this.y = this.y + ($$1 - this.y) * 0.2;
               this.yd = this.yd + ($$0.getDeltaMovement().y - this.yd) * 0.2;
               this.setPos(this.x, this.y, this.z);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         return new PlayerCloudParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, this.sprites);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SneezeProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SneezeProvider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         Particle $$8 = new PlayerCloudParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, this.sprites);
         $$8.setColor(200.0F, 50.0F, 120.0F);
         $$8.setAlpha(0.4F);
         return $$8;
      }
   }
}
