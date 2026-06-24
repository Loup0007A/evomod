package com.evolutionmod.client;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

/**
 * Couche de rendu client qui applique les mutations génétiques visuellement.
 *
 * NOTE 1.21.1 : la signature de Model#renderToBuffer a changé plusieurs fois
 * entre 1.19 et 1.21.x (perte des 4 paramètres couleur r/g/b/a séparés dans
 * certaines branches), et n'a pas pu être confirmée de façon fiable et stable
 * pour cette version exacte sans risquer une signature incorrecte supplémentaire.
 *
 * Plutôt que de tenter un overlay de modèle coloré (fragile entre versions),
 * cette couche utilise uniquement des particules ambiantes pour signaler
 * visuellement les mutations fortes. Le glow (Glowing tag) a été délibérément
 * écarté : il rend l'entité visible À TRAVERS LES MURS en permanence, ce qui
 * casse la discrétion de base attendue en survie pour n'importe quelle
 * créature mutée — bien plus intrusif qu'un cosmétique, ça change le gameplay
 * de chasse/évitement de fond en comble. Les particules, elles, respectent
 * la ligne de vue normale et restent un signal discret plutôt qu'un beacon.
 */
public class GeneticEntityRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    public GeneticEntityRenderer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, T entity,
                       float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {

        EvolutionCapability.getEvolutionData(entity).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) return;

            // ── Particules ambiantes pour mutations marquées ──────────
            // Spawn occasionnel de particules colorées selon la teinte génétique,
            // visible uniquement client-side via le niveau de l'entité.
            double colorSat = genome.getGeneValue(Gene.GeneType.COLOR_SATURATION);
            if (colorSat > 0.6 && entity.tickCount % 40 == 0 && entity.level().isClientSide()) {
                spawnGeneticParticles(entity, genome);
            }
        });
    }

    /**
     * Spawn des particules colorées autour de l'entité selon sa teinte génétique.
     * Utilise uniquement des particules vanilla pour rester stable entre versions.
     */
    private void spawnGeneticParticles(T entity, Genome genome) {
        double hue = genome.getGeneValue(Gene.GeneType.COLOR_HUE) / 360.0;
        Color c = Color.getHSBColor((float) hue, 0.8f, 0.9f);

        net.minecraft.core.particles.ParticleOptions particle =
            new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f),
                1.0f
            );

        entity.level().addParticle(
            particle,
            entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
            entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight(),
            entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth(),
            0, 0.02, 0
        );
    }
}
