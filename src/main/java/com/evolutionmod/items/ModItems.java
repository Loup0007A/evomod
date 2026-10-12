package com.evolutionmod.items;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, EvolutionMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EvolutionMod.MOD_ID);

    public static final RegistryObject<Item> EVOLUTION_JOURNAL = ITEMS.register("evolution_journal",
        EvolutionJournalItem::new);
    public static final RegistryObject<Item> EVOLUTION_SCANNER = ITEMS.register("evolution_scanner",
        EvolutionScannerItem::new);
    public static final RegistryObject<Item> GENETIC_CRYSTAL = ITEMS.register("genetic_crystal",
        GeneticCrystalItem::new);
    public static final RegistryObject<Item> MUTAGEN = ITEMS.register("mutagen",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MUTANT_MEAT = ITEMS.register("mutant_meat",
        MutantMeatItem::new);
    public static final RegistryObject<Item> MUTANT_FRUIT = ITEMS.register("mutant_fruit",
        MutantFruitItem::new);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.evolutionmod"))
        .icon(() -> new ItemStack(GENETIC_CRYSTAL.get()))
        .displayItems((params, output) -> {
            output.accept(EVOLUTION_JOURNAL.get());
            output.accept(EVOLUTION_SCANNER.get());
            output.accept(GENETIC_CRYSTAL.get());
            output.accept(MUTAGEN.get());
            output.accept(ModBlocks.GENETIC_LAB_ITEM.get());
            output.accept(ModBlocks.MUTANT_LOG_ITEM.get());
            output.accept(ModBlocks.BIOLUMINESCENT_LEAVES_ITEM.get());
            output.accept(MUTANT_MEAT.get());
            output.accept(MUTANT_FRUIT.get());
            // Quelques cristaux d'exemple, pratiques pour découvrir le laboratoire
            for (String plant : new String[]{"oak", "wheat"}) {
                Genome g = Genome.fromProfile(SpeciesProfile.forPlant(plant));
                output.accept(GeneticCrystalItem.withGenome(new ItemStack(GENETIC_CRYSTAL.get()), g));
            }
        })
        .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
