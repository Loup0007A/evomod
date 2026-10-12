package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DustColorTransitionParticle extends DustParticleBase<DustColorTransitionOptions> {
   private final Vector3f fromColor;
   private final Vector3f toColor;

   protected DustColorTransitionParticle(
      ClientLevel pLevel,
      double pX,
      double pY,
      double pZ,
      double pXSpeed,
      double pYSpeed,
      double pZSpeed,
      DustColorTransitionOptions pOptions,
      SpriteSet pSprites
   ) {
      super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pOptions, pSprites);
      float $$9 = this.random.nextFloat() * 0.4F + 0.6F;
      this.fromColor = this.randomizeColor(pOptions.getFromColor(), $$9);
      this.toColor = this.randomizeColor(pOptions.getToColor(), $$9);
   }

   private Vector3f randomizeColor(Vector3f pVector, float pMultiplier) {
      return new Vector3f(
         this.randomizeColor(pVector.x(), pMultiplier), this.randomizeColor(pVector.y(), pMultiplier), this.randomizeColor(pVector.z(), pMultiplier)
      );
   }

   private void lerpColors(float pPartialTick) {
      float $$1 = ((float)this.age + pPartialTick) / ((float)this.lifetime + 1.0F);
      Vector3f $$2 = new Vector3f(this.fromColor).lerp(this.toColor, $$1);
      this.rCol = $$2.x();
      this.gCol = $$2.y();
      this.bCol = $$2.z();
   }

   @Override
   public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
      this.lerpColors(pPartialTicks);
      super.render(pBuffer, pRenderInfo, pPartialTicks);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<DustColorTransitionOptions> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(
         DustColorTransitionOptions pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         return new DustColorTransitionParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pType, this.sprites);
      }
   }
}
