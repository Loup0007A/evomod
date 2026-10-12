package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlyStraightTowardsParticle extends TextureSheetParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;
   private final int startColor;
   private final int endColor;

   FlyStraightTowardsParticle(
      ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, int pStartColor, int pEndColor
   ) {
      super(pLevel, pX, pY, pZ);
      this.xd = pXSpeed;
      this.yd = pYSpeed;
      this.zd = pZSpeed;
      this.xStart = pX;
      this.yStart = pY;
      this.zStart = pZ;
      this.xo = pX + pXSpeed;
      this.yo = pY + pYSpeed;
      this.zo = pZ + pZSpeed;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      this.hasPhysics = false;
      this.lifetime = (int)(Math.random() * 5.0) + 25;
      this.startColor = pStartColor;
      this.endColor = pEndColor;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public void move(double pX, double pY, double pZ) {
   }

   @Override
   public int getLightColor(float pPartialTick) {
      return 240;
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float $$0 = (float)this.age / (float)this.lifetime;
         float $$1 = 1.0F - $$0;
         this.x = this.xStart + this.xd * (double)$$1;
         this.y = this.yStart + this.yd * (double)$$1;
         this.z = this.zStart + this.zd * (double)$$1;
         int $$2 = FastColor.ARGB32.lerp($$0, this.startColor, this.endColor);
         this.setColor((float)FastColor.ARGB32.red($$2) / 255.0F, (float)FastColor.ARGB32.green($$2) / 255.0F, (float)FastColor.ARGB32.blue($$2) / 255.0F);
         this.setAlpha((float)FastColor.ARGB32.alpha($$2) / 255.0F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class OminousSpawnProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public OminousSpawnProvider(SpriteSet pSprite) {
         this.sprite = pSprite;
      }

      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         FlyStraightTowardsParticle $$8 = new FlyStraightTowardsParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, -12210434, -1);
         $$8.scale(Mth.randomBetween(pLevel.getRandom(), 3.0F, 5.0F));
         $$8.pickSprite(this.sprite);
         return $$8;
      }
   }
}
