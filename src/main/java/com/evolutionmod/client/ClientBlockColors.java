package com.evolutionmod.client;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.BioluminescentLeavesBlock;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.blocks.MutantLogBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Enregistre les BlockColor handlers qui lisent GENETIC_HUE_SEGMENT
 * pour teinter dynamiquement les blocs génétiques au rendu.
 *
 * Sans ce handler, les blocs auraient toujours leur texture par défaut
 * (vert pour les feuilles, brun pour le bois) quel que soit le segment
 * de teinte stocké dans le blockstate — le gène COLOR_HUE n'aurait
 * aucun effet visuel sans cette étape.
 *
 * Le rendu suit le même pipeline que les feuilles vanilla (qui sont déjà
 * teintées par biome), on remplace simplement la source de la teinte :
 * biome → segment génétique.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientBlockColors {

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {

        // Feuilles bioluminescentes : teinte selon GENETIC_HUE_SEGMENT
        event.register((state, level, pos, tintIndex) -> {
            int segment = state.getValue(BioluminescentLeavesBlock.GENETIC_HUE_SEGMENT);
            return BioluminescentLeavesBlock.segmentToRgb(segment);
        }, ModBlocks.BIOLUMINESCENT_LEAVES.get());

        // Bûches mutantes : même logique de teinte, segment indépendant
        event.register((state, level, pos, tintIndex) -> {
            int segment = state.getValue(MutantLogBlock.GENETIC_HUE_SEGMENT);
            return MutantLogBlock.hueToSegment(segment) >= 0
                    ? BioluminescentLeavesBlock.segmentToRgb(segment)
                    : 0xFFFFFF;
        }, ModBlocks.MUTANT_LOG.get());

        EvolutionMod.LOGGER.info("Handlers de couleur génétique enregistrés pour les blocs mutants.");
    }
}
