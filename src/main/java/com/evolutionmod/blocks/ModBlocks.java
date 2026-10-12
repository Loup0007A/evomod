package com.evolutionmod.blocks;

import com.evolutionmod.EvolutionMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registre des blocs, de leurs items et des block entities. */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, EvolutionMod.MOD_ID);
    public static final DeferredRegister<Item> BLOCK_ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, EvolutionMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EvolutionMod.MOD_ID);

    public static final RegistryObject<Block> BIOLUMINESCENT_LEAVES = BLOCKS.register("bioluminescent_leaves",
        () -> new BioluminescentLeavesBlock(BioluminescentLeavesBlock.createProperties()));
    public static final RegistryObject<Block> MUTANT_LOG = BLOCKS.register("mutant_log",
        () -> new MutantLogBlock(MutantLogBlock.createProperties()));
    public static final RegistryObject<Block> GENETIC_LAB = BLOCKS.register("genetic_lab",
        () -> new GeneticLabBlock(GeneticLabBlock.createProperties()));

    public static final RegistryObject<Item> BIOLUMINESCENT_LEAVES_ITEM = BLOCK_ITEMS.register("bioluminescent_leaves",
        () -> new BlockItem(BIOLUMINESCENT_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> MUTANT_LOG_ITEM = BLOCK_ITEMS.register("mutant_log",
        () -> new BlockItem(MUTANT_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> GENETIC_LAB_ITEM = BLOCK_ITEMS.register("genetic_lab",
        () -> new BlockItem(GENETIC_LAB.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<GeneticLabBlockEntity>> GENETIC_LAB_BE =
        BLOCK_ENTITIES.register("genetic_lab",
            () -> BlockEntityType.Builder.of(GeneticLabBlockEntity::new, GENETIC_LAB.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
