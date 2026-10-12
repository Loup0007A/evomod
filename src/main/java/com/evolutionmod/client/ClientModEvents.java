package com.evolutionmod.client;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.BioluminescentLeavesBlock;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.blocks.MutantLogBlock;
import com.evolutionmod.client.gui.GeneticLabScreen;
import com.evolutionmod.menu.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

/** Initialisation côté client (bus du mod). */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {}

    public static final KeyMapping OPEN_JOURNAL = new KeyMapping("key.evolutionmod.journal",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.evolutionmod");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.GENETIC_LAB.get(), GeneticLabScreen::new));
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_JOURNAL);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) ->
                BioluminescentLeavesBlock.segmentToRgb(state.getValue(BioluminescentLeavesBlock.GENETIC_HUE)),
            ModBlocks.BIOLUMINESCENT_LEAVES.get());
        event.register((state, level, pos, tint) -> tint == 0
                ? BioluminescentLeavesBlock.segmentToRgb(state.getValue(MutantLogBlock.GENETIC_HUE)) : 0xFFFFFF,
            ModBlocks.MUTANT_LOG.get());
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> BioluminescentLeavesBlock.segmentToRgb(4) | 0xFF000000,
            ModBlocks.BIOLUMINESCENT_LEAVES_ITEM.get());
        event.register((stack, tint) -> tint == 0 ? (BioluminescentLeavesBlock.segmentToRgb(6) | 0xFF000000) : -1,
            ModBlocks.MUTANT_LOG_ITEM.get());
    }

    /** Ajoute la couche de rendu génétique (teinte, lueur) à tous les mobs. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        int count = 0;
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            try {
                EntityRenderer<?> renderer = event.getEntityRenderer((EntityType) type);
                if (renderer instanceof LivingEntityRenderer living) {
                    living.addLayer(new GeneticLayer(living));
                    count++;
                }
            } catch (Exception ignored) {
                // type sans rendu vivant
            }
        }
        EvolutionMod.LOGGER.info("Couche génétique ajoutée à {} rendus de créatures", count);
    }
}
