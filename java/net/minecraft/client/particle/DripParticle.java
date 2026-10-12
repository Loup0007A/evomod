package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DripParticle extends TextureSheetParticle {
   private final Fluid type;
   protected boolean isGlowing;

   DripParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType) {
      super(pLevel, pX, pY, pZ);
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.type = pType;
   }

   protected Fluid getType() {
      return this.type;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   @Override
   public int getLightColor(float pPartialTick) {
      return this.isGlowing ? 240 : super.getLightColor(pPartialTick);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.preMoveUpdate();
      if (!this.removed) {
         this.yd = this.yd - (double)this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.postMoveUpdate();
         if (!this.removed) {
            this.xd *= 0.98F;
            this.yd *= 0.98F;
            this.zd *= 0.98F;
            if (this.type != Fluids.EMPTY) {
               BlockPos $$0 = BlockPos.containing(this.x, this.y, this.z);
               FluidState $$1 = this.level.getFluidState($$0);
               if ($$1.getType() == this.type && this.y < (double)((float)$$0.getY() + $$1.getHeight(this.level, $$0))) {
                  this.remove();
               }
            }
         }
      }
   }

   protected void preMoveUpdate() {
      if (this.lifetime-- <= 0) {
         this.remove();
      }
   }

   protected void postMoveUpdate() {
   }

   public static TextureSheetParticle createWaterHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.FALLING_WATER);
      $$8.setColor(0.2F, 0.3F, 1.0F);
      return $$8;
   }

   public static TextureSheetParticle createWaterFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.SPLASH);
      $$8.setColor(0.2F, 0.3F, 1.0F);
      return $$8;
   }

   public static TextureSheetParticle createLavaHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      return new DripParticle.CoolingDripHangParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.FALLING_LAVA);
   }

   public static TextureSheetParticle createLavaFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
      $$8.setColor(1.0F, 0.2857143F, 0.083333336F);
      return $$8;
   }

   public static TextureSheetParticle createLavaLandParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA);
      $$8.setColor(1.0F, 0.2857143F, 0.083333336F);
      return $$8;
   }

   public static TextureSheetParticle createHoneyHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle.DripHangParticle $$8 = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.FALLING_HONEY);
      $$8.gravity *= 0.01F;
      $$8.lifetime = 100;
      $$8.setColor(0.622F, 0.508F, 0.082F);
      return $$8;
   }

   public static TextureSheetParticle createHoneyFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.HoneyFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.LANDING_HONEY);
      $$8.gravity = 0.01F;
      $$8.setColor(0.582F, 0.448F, 0.082F);
      return $$8;
   }

   public static TextureSheetParticle createHoneyLandParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
      $$8.lifetime = (int)(128.0 / (Math.random() * 0.8 + 0.2));
      $$8.setColor(0.522F, 0.408F, 0.082F);
      return $$8;
   }

   public static TextureSheetParticle createDripstoneWaterHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER);
      $$8.setColor(0.2F, 0.3F, 1.0F);
      return $$8;
   }

   public static TextureSheetParticle createDripstoneWaterFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripstoneFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.SPLASH);
      $$8.setColor(0.2F, 0.3F, 1.0F);
      return $$8;
   }

   public static TextureSheetParticle createDripstoneLavaHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      return new DripParticle.CoolingDripHangParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA);
   }

   public static TextureSheetParticle createDripstoneLavaFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripstoneFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
      $$8.setColor(1.0F, 0.2857143F, 0.083333336F);
      return $$8;
   }

   public static TextureSheetParticle createNectarFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.FallingParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
      $$8.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      $$8.gravity = 0.007F;
      $$8.setColor(0.92F, 0.782F, 0.72F);
      return $$8;
   }

   public static TextureSheetParticle createSporeBlossomFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      int $$8 = (int)(64.0F / Mth.randomBetween(pLevel.getRandom(), 0.1F, 0.9F));
      DripParticle $$9 = new DripParticle.FallingParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, $$8);
      $$9.gravity = 0.005F;
      $$9.setColor(0.32F, 0.5F, 0.22F);
      return $$9;
   }

   public static TextureSheetParticle createObsidianTearHangParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle.DripHangParticle $$8 = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR);
      $$8.isGlowing = true;
      $$8.gravity *= 0.01F;
      $$8.lifetime = 100;
      $$8.setColor(0.51171875F, 0.03125F, 0.890625F);
      return $$8;
   }

   public static TextureSheetParticle createObsidianTearFallParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR);
      $$8.isGlowing = true;
      $$8.gravity = 0.01F;
      $$8.setColor(0.51171875F, 0.03125F, 0.890625F);
      return $$8;
   }

   public static TextureSheetParticle createObsidianTearLandParticle(
      SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
   ) {
      DripParticle $$8 = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
      $$8.isGlowing = true;
      $$8.lifetime = (int)(28.0 / (Math.random() * 0.8 + 0.2));
      $$8.setColor(0.51171875F, 0.03125F, 0.890625F);
      return $$8;
   }

   @OnlyIn(Dist.CLIENT)
   static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
      CoolingDripHangParticle(ClientLevel p_106068_, double p_106069_, double p_106070_, double p_106071_, Fluid p_106072_, ParticleOptions p_106073_) {
         super(p_106068_, p_106069_, p_106070_, p_106071_, p_106072_, p_106073_);
      }

      @Override
      protected void preMoveUpdate() {
         this.rCol = 1.0F;
         this.gCol = 16.0F / (float)(40 - this.lifetime + 16);
         this.bCol = 4.0F / (float)(40 - this.lifetime + 8);
         super.preMoveUpdate();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripHangParticle extends DripParticle {
      private final ParticleOptions fallingParticle;

      DripHangParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, ParticleOptions pFallingParticle) {
         super(pLevel, pX, pY, pZ, pType);
         this.fallingParticle = pFallingParticle;
         this.gravity *= 0.02F;
         this.lifetime = 40;
      }

      @Override
      protected void preMoveUpdate() {
         if (this.lifetime-- <= 0) {
            this.remove();
            this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }
      }

      @Override
      protected void postMoveUpdate() {
         this.xd *= 0.02;
         this.yd *= 0.02;
         this.zd *= 0.02;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripLandParticle extends DripParticle {
      DripLandParticle(ClientLevel p_106102_, double p_106103_, double p_106104_, double p_106105_, Fluid p_106106_) {
         super(p_106102_, p_106103_, p_106104_, p_106105_, p_106106_);
         this.lifetime = (int)(16.0 / (Math.random() * 0.8 + 0.2));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
      DripstoneFallAndLandParticle(ClientLevel p_171930_, double p_171931_, double p_171932_, double p_171933_, Fluid p_171934_, ParticleOptions p_171935_) {
         super(p_171930_, p_171931_, p_171932_, p_171933_, p_171934_, p_171935_);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            SoundEvent $$0 = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            float $$1 = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, $$0, SoundSource.BLOCKS, $$1, 1.0F, false);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallAndLandParticle extends DripParticle.FallingParticle {
      protected final ParticleOptions landParticle;

      FallAndLandParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, ParticleOptions pLandParticle) {
         super(pLevel, pX, pY, pZ, pType);
         this.landParticle = pLandParticle;
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallingParticle extends DripParticle {
      FallingParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType) {
         this(pLevel, pX, pY, pZ, pType, (int)(64.0 / (Math.random() * 0.8 + 0.2)));
      }

      FallingParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, int pLifetime) {
         super(pLevel, pX, pY, pZ, pType);
         this.lifetime = pLifetime;
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
      HoneyFallAndLandParticle(ClientLevel p_106146_, double p_106147_, double p_106148_, double p_106149_, Fluid p_106150_, ParticleOptions p_106151_) {
         super(p_106146_, p_106147_, p_106148_, p_106149_, p_106150_, p_106151_);
      }

      @Override
      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
            float $$0 = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, $$0, 1.0F, false);
         }
      }
   }
}
