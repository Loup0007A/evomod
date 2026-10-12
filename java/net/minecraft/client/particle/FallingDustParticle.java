package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingDustParticle extends TextureSheetParticle {
   private final float rotSpeed;
   private final SpriteSet sprites;

   FallingDustParticle(ClientLevel pLevel, double pX, double pY, double pZ, float pXSpeed, float pYSpeed, float pZSpeed, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ);
      this.sprites = pSprites;
      this.rCol = pXSpeed;
      this.gCol = pYSpeed;
      this.bCol = pZSpeed;
      float $$8 = 0.9F;
      this.quadSize *= 0.67499995F;
      int $$9 = (int)(32.0 / (Math.random() * 0.8 + 0.2));
      this.lifetime = (int)Math.max((float)$$9 * 0.9F, 1.0F);
      this.setSpriteFromAge(pSprites);
      this.rotSpeed = ((float)Math.random() - 0.5F) * 0.1F;
      this.roll = (float)Math.random() * (float) (Math.PI * 2);
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public float getQuadSize(float pScaleFactor) {
      return this.quadSize * Mth.clamp(((float)this.age + pScaleFactor) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
         this.oRoll = this.roll;
         this.roll = this.roll + (float) Math.PI * this.rotSpeed * 2.0F;
         if (this.onGround) {
            this.oRoll = this.roll = 0.0F;
         }

         this.move(this.xd, this.yd, this.zd);
         this.yd -= 0.003F;
         this.yd = Math.max(this.yd, -0.14F);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<BlockParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      @Nullable
      public Particle createParticle(
         BlockParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         BlockState $$8 = pType.getState();
         if (!$$8.isAir() && $$8.getRenderShape() == RenderShape.INVISIBLE) {
            return null;
         } else {
            BlockPos $$9 = BlockPos.containing(pX, pY, pZ);
            int $$10 = Minecraft.getInstance().getBlockColors().getColor($$8, pLevel, $$9);
            if ($$8.getBlock() instanceof FallingBlock) {
               $$10 = ((FallingBlock)$$8.getBlock()).getDustColor($$8, pLevel, $$9);
            }

            float $$11 = (float)($$10 >> 16 & 0xFF) / 255.0F;
            float $$12 = (float)($$10 >> 8 & 0xFF) / 255.0F;
            float $$13 = (float)($$10 & 0xFF) / 255.0F;
            return new FallingDustParticle(pLevel, pX, pY, pZ, $$11, $$12, $$13, this.sprite);
         }
      }
   }
}
