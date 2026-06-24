package com.evolutionmod.client;

import com.evolutionmod.EvolutionMod;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Enregistre la couche de rendu génétique sur TOUS les mobs vivants.
 * Appelé côté client uniquement (Dist.CLIENT).
 *
 * NOTE 1.21.1 (vérifié dans le code source Forge, branche 1.21.1) :
 * EntityRenderersEvent.AddLayers n'expose PAS de méthode getEntityTypes() —
 * seulement getEntityRenderer(EntityType<?>) pour un type précis et une map
 * de renderers privée. Pour parcourir TOUS les types d'entités enregistrés,
 * il faut itérer sur le registre vanilla/Forge directement
 * (ForgeRegistries.ENTITY_TYPES), puis demander le renderer de chacun à
 * l'event via getEntityRenderer().
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            try {
                // getEntityRenderer(EntityType<? extends T>) avec T extends LivingEntity
                // ne peut pas être appelée directement avec un EntityType<?> capturé
                // depuis le registre global (qui couvre TOUS les Entity, pas seulement
                // les LivingEntity) — le compilateur rejette l'inférence des bornes.
                // On passe par un helper générique qui force le cast côté appelant ;
                // sûr ici car on filtre ensuite avec un instanceof LivingEntityRenderer
                // avant tout usage réel du résultat.
                EntityRenderer<?> renderer = getRendererUnchecked(event, entityType);
                if (renderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new GeneticEntityRenderer(livingRenderer));
                    EvolutionMod.LOGGER.debug("Couche génétique ajoutée à : {}", entityType.toShortString());
                }
            } catch (Exception e) {
                // Certains renderers ne supportent pas les layers — ignorer silencieusement
            }
        }

        EvolutionMod.LOGGER.info("Couches de rendu génétiques enregistrées sur tous les mobs.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityRenderer<?> getRendererUnchecked(EntityRenderersEvent.AddLayers event, EntityType<?> entityType) {
        return event.getEntityRenderer((EntityType) entityType);
    }
}
