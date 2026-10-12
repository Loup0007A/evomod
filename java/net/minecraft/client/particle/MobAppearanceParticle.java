package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MobAppearanceParticle extends Particle {
   private final Model model;
   private final RenderType renderType = RenderType.entityTranslucent(ElderGuardianRenderer.GUARDIAN_ELDER_LOCATION);

   MobAppearanceParticle(ClientLevel pLevel, double pX, double pY, double pZ) {
      super(pLevel, pX, pY, pZ);
      this.model = new GuardianModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELDER_GUARDIAN));
      this.gravity = 0.0F;
      this.lifetime = 30;
   }

   @Override
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.CUSTOM;
   }

   @Override
   public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
      float $$3 = ((float)this.age + pPartialTicks) / (float)this.lifetime;
      float $$4 = 0.05F + 0.5F * Mth.sin($$3 * (float) Math.PI);
      int $$5 = FastColor.ARGB32.colorFromFloat($$4, 1.0F, 1.0F, 1.0F);
      PoseStack $$6 = new PoseStack();
      $$6.mulPose(pRenderInfo.rotation());
      $$6.mulPose(Axis.XP.rotationDegrees(150.0F * $$3 - 60.0F));
      $$6.scale(1.0F, -1.0F, -1.0F);
      $$6.translate(0.0F, -1.101F, 1.5F);
      MultiBufferSource.BufferSource $$7 = Minecraft.getInstance().renderBuffers().bufferSource();
      VertexConsumer $$8 = $$7.getBuffer(this.renderType);
      this.model.renderToBuffer($$6, $$8, 15728880, OverlayTexture.NO_OVERLAY, $$5);
      $$7.endBatch();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(
         SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
      ) {
         return new MobAppearanceParticle(pLevel, pX, pY, pZ);
      }
   }
}
