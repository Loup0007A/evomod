package com.evolutionmod;

import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.menu.ModMenus;
import com.evolutionmod.network.EvoNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evolution Mod — point d'entrée.
 *
 * Les gestionnaires d'événements portent {@code @Mod.EventBusSubscriber} et
 * sont enregistrés automatiquement par Forge : ne pas les ré-enregistrer ici.
 */
@Mod(EvolutionMod.MOD_ID)
public class EvolutionMod {

    public static final String MOD_ID = "evolutionmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("EvolutionMod");

    public EvolutionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModMenus.register(modBus);
        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EvoConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(EvoNetwork::register);
        LOGGER.info("Evolution Mod prêt : la vie va évoluer.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
