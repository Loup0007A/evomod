package com.evolutionmod.items;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.data.MutantMeatItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre central de tous les items custom du mod.
 * Forge exige un DeferredRegister pour enregistrer proprement
 * les items au bon moment du cycle de chargement.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EvolutionMod.MOD_ID);

    // ──────────────────────────────────────────────────────
    //  Items du mod
    // ──────────────────────────────────────────────────────

    public static final RegistryObject<Item> GENETIC_CRYSTAL = ITEMS.register(
            "genetic_crystal", GeneticCrystalItem::new);

    public static final RegistryObject<Item> MUTANT_MEAT = ITEMS.register(
            "mutant_meat", MutantMeatItem::new);

    public static final RegistryObject<Item> MUTANT_FRUIT = ITEMS.register(
            "mutant_fruit", MutantFruitItem::new);

    public static final RegistryObject<Item> EVOLUTION_SCANNER = ITEMS.register(
            "evolution_scanner", EvolutionScannerItem::new);

    /**
     * À appeler dans le constructeur du mod, avant tout autre registre.
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTab);
        EvolutionMod.LOGGER.info("ModItems enregistrés : {} items", 4);
    }

    /**
     * Ajoute tous les items du mod à l'onglet "Outils & Utilitaires" créatif.
     */
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(new ItemStack(GENETIC_CRYSTAL.get()));
            event.accept(new ItemStack(EVOLUTION_SCANNER.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(new ItemStack(MUTANT_MEAT.get()));
            event.accept(new ItemStack(MUTANT_FRUIT.get()));
        }
    }
}
