package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(
      ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, float pQuadSizeMultiplier, SpriteSet pSprites
   ) {
      super(pLevel, pX, pY, pZ, 0.1F, -0.1F, 0.1F, pXSpeed, pYSpeed, pZSpeed, pQuadSizeMultiplier, pSprites, 0.0F, 20, 0.0125F, false);
      this.rCol = (float)FastColor.ARGB32.red(12235202) / 255.0F;
      this.gCol = (float)FastColor.ARGB32.green(12235202) / 255.0F;
      this.bCol = (float)FastColor.ARGB32.blue(12235202) / 255.0F;
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
         RandomSource $$8 = pLevel.random;
         double $$9 = (double)$$8.nextFloat() * -1.9 * (double)$$8.nextFloat() * 0.1;
         double $$10 = (double)$$8.nextFloat() * -0.5 * (double)$$8.nextFloat() * 0.1 * 5.0;
         double $$11 = (double)$$8.nextFloat() * -1.9 * (double)$$8.nextFloat() * 0.1;
         return new WhiteAshParticle(pLevel, pX, pY, pZ, $$9, $$10, $$11, 1.0F, this.sprites);
      }
   }
}
