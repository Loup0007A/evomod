package com.evolutionmod.client;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Rend visibles les gènes de couleur : la créature est recouverte d'un voile
 * translucide de la teinte génétique (intensité = gène PIGMENT), et les
 * créatures bioluminescentes émettent une lueur et des particules.
 */
public class GeneticLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public GeneticLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        Genome genome = EvolutionCapability.genome(entity);
        if (genome == null) return;

        double pigment = genome.get(GeneType.PIGMENT);
        double glow = genome.get(GeneType.GLOW);
        if (pigment < 0.08 && glow < 0.3) return;

        ResourceLocation texture;
        try {
            texture = getTextureLocation(entity);
        } catch (Exception e) {
            return;
        }
        if (texture == null) return;

        int tint = genome.tintColor() & 0xFFFFFF;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);

        if (pigment >= 0.08) {
            int alpha = (int) Math.min(150, pigment * 150);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(texture));
            getParentModel().renderToBuffer(pose, vc, light, overlay, (alpha << 24) | tint);
        }

        if (glow >= 0.3) {
            if (pigment < 0.2) tint = 0x57E3C2; // lueur cyan par défaut
            float pulse = 0.6f + 0.4f * (float) Math.sin(ageInTicks * 0.15f);
            int alpha = (int) (Math.min(1.0, glow) * 90 * pulse);
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(texture));
            getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, (alpha << 24) | tint);

            if (entity.getRandom().nextFloat() < glow * 0.03f) {
                Vector3f c = new Vector3f(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f);
                entity.level().addParticle(new DustParticleOptions(c, 0.9f),
                    entity.getRandomX(0.6), entity.getRandomY(), entity.getRandomZ(0.6), 0, 0.02, 0);
            }
        }
    }
}
