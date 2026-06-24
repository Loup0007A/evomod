package com.evolutionmod.blocks;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre central des blocs custom du mod.
 *
 * Chaque bloc enregistré ici a aussi besoin d'un BlockItem correspondant
 * pour apparaître dans l'inventaire/créatif — Forge ne le fait pas
 * automatiquement contrairement à certains autres moddings loaders.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EvolutionMod.MOD_ID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EvolutionMod.MOD_ID);

    // ──────────────────────────────────────────────────────
    //  Blocs
    // ──────────────────────────────────────────────────────

    public static final RegistryObject<Block> BIOLUMINESCENT_LEAVES = BLOCKS.register(
            "bioluminescent_leaves",
            () -> new BioluminescentLeavesBlock(BioluminescentLeavesBlock.createProperties()));

    public static final RegistryObject<Block> MUTANT_LOG = BLOCKS.register(
            "mutant_log",
            () -> new MutantLogBlock(MutantLogBlock.createProperties()));

    // ──────────────────────────────────────────────────────
    //  BlockItems correspondants (nécessaires pour l'inventaire)
    // ──────────────────────────────────────────────────────

    public static final RegistryObject<Item> BIOLUMINESCENT_LEAVES_ITEM = BLOCK_ITEMS.register(
            "bioluminescent_leaves",
            () -> new BlockItem(BIOLUMINESCENT_LEAVES.get(), new Item.Properties()));

    public static final RegistryObject<Item> MUTANT_LOG_ITEM = BLOCK_ITEMS.register(
            "mutant_log",
            () -> new BlockItem(MUTANT_LOG.get(), new Item.Properties()));

    /**
     * À appeler depuis le constructeur du mod, juste après ModItems.register().
     */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ITEMS.register(modEventBus);
        modEventBus.addListener(ModBlocks::addToCreativeTab);
        EvolutionMod.LOGGER.info("ModBlocks enregistrés : {} blocs", 2);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(new ItemStack(BIOLUMINESCENT_LEAVES_ITEM.get()));
            event.accept(new ItemStack(MUTANT_LOG_ITEM.get()));
        }
    }
}
