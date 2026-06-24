package com.evolutionmod;

import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.network.EvolutionNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée du mod.
 *
 * Note d'architecture : tous les gestionnaires d'événements du mod
 * (DayChangeEventHandler, EntityEvolutionHandler, TreeEvolutionHandler,
 * NaturalSelectionHandler, EvolutionCommand, EvolutionCapability,
 * ClientSetup, ClientBlockColors...) utilisent l'annotation
 * {@code @Mod.EventBusSubscriber}. Forge les détecte et les enregistre
 * automatiquement sur le bus approprié dès le chargement de la classe —
 * il ne faut donc JAMAIS faire en plus
 * {@code MinecraftForge.EVENT_BUS.register(new XxxHandler())} ici,
 * sous peine de voir chaque événement traité deux fois (double mutation,
 * double dégâts de venin, etc.). Seuls les DeferredRegister (ModItems,
 * ModBlocks) et le setup réseau ont besoin d'un appel explicite, car eux
 * ne sont pas auto-découvrables par annotation.
 */
@Mod(EvolutionMod.MOD_ID)
public class EvolutionMod {

    public static final String MOD_ID = "evolutionmod";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public EvolutionMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registres différés : doivent être enregistrés explicitement sur le mod bus
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Tous les autres handlers (events.*, world.*, selection.*, core.EvolutionCommand,
        // client.* côté CLIENT uniquement) portent @Mod.EventBusSubscriber et n'ont
        // besoin d'aucune ligne ici — Forge les enregistre lui-même.

        LOGGER.info("Evolution Mod chargé - Le monde va évoluer !");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EvolutionCapability.register();
            EvolutionNetwork.register();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client Evolution Mod initialisé - rendu génétique actif");
    }
}

