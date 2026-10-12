package com.evolutionmod.events;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.BioluminescentLeavesBlock;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.blocks.MutantLogBlock;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import com.evolutionmod.items.MutantFruitItem;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Expression des génomes végétaux : vitesse de pousse, rendement, fruits
 * mutants, feuillages bioluminescents et bois mutant.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public final class PlantEvents {

    private PlantEvents() {}

    private record PendingTree(ServerLevel level, BlockPos pos, String species, long runAt) {}

    private static final Deque<PendingTree> PENDING = new ArrayDeque<>();

    // ───────────────────────── Identification ─────────────────────────

    private static String path(Block block) {
        var key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? "" : key.getPath();
    }

    /** Espèce d'arbre d'un bloc (pousse, bûche, bois, feuilles) ou null. */
    static String treeSpecies(Block block) {
        return com.evolutionmod.core.TreeMutator.treeSpecies(block);
    }

    static String cropSpecies(Block block) {
        String p = path(block);
        return switch (p) {
            case "wheat" -> "wheat";
            case "carrots" -> "carrot";
            case "potatoes" -> "potato";
            case "beetroots" -> "beetroot";
            case "sugar_cane" -> "sugar_cane";
            case "melon", "melon_stem" -> "melon";
            case "pumpkin", "pumpkin_stem" -> "pumpkin";
            case "cactus" -> "cactus";
            case "nether_wart" -> "nether_wart";
            case "sweet_berry_bush" -> "sweet_berry";
            default -> null;
        };
    }

    private static Item cropProduce(String species) {
        return switch (species) {
            case "wheat" -> Items.WHEAT;
            case "carrot" -> Items.CARROT;
            case "potato" -> Items.POTATO;
            case "beetroot" -> Items.BEETROOT;
            case "sugar_cane" -> Items.SUGAR_CANE;
            case "melon" -> Items.MELON_SLICE;
            case "pumpkin" -> Items.PUMPKIN;
            case "cactus" -> Items.CACTUS;
            case "nether_wart" -> Items.NETHER_WART;
            case "sweet_berry" -> Items.SWEET_BERRIES;
            default -> Items.AIR;
        };
    }

    private static Genome plantGenome(ServerLevel level, String plant) {
        if (!EvoConfig.plantEvolution()) return null;
        SpeciesRecord r = EvolutionSavedData.get(level.getServer()).get(SpeciesProfile.plantId(plant));
        return r == null ? null : r.template();
    }

    // ───────────────────────── Croissance ─────────────────────────

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        String crop = cropSpecies(event.getState().getBlock());
        if (crop == null) return;
        Genome g = plantGenome(level, crop);
        if (g == null) return;
        double growth = g.get(GeneType.GROWTH);
        if (growth > 1.0) {
            if (level.random.nextDouble() < Math.min(0.6, (growth - 1.0) * 0.3)) {
                event.setResult(Event.Result.ALLOW);
            }
        } else if (growth < 1.0 && level.random.nextDouble() < (1.0 - growth) * 0.8) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ───────────────────────── Récolte ─────────────────────────

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer() == null || event.getPlayer().isCreative()) return;
        BlockState state = event.getState();
        Block block = state.getBlock();
        BlockPos pos = event.getPos();

        String crop = cropSpecies(block);
        if (crop != null) {
            if (EvoConfig.plantEvolution()) {
                com.evolutionmod.core.Adaptation.roll(level, pos, SpeciesProfile.plantId(crop),
                    com.evolutionmod.core.Adaptation.Cause.PLANT_BROKEN, null);
            }
            if (block instanceof CropBlock cb && !cb.isMaxAge(state)) return;
            if (path(block).endsWith("_stem")) return;
            Genome g = plantGenome(level, crop);
            if (g == null) return;
            double yield = g.get(GeneType.YIELD);
            if (yield > 1.0) {
                int extra = 0;
                double y = yield - 1.0;
                while (y > 0) {
                    if (level.random.nextDouble() < Math.min(1.0, y)) extra++;
                    y -= 1.0;
                }
                Item produce = cropProduce(crop);
                if (extra > 0 && produce != Items.AIR) drop(level, pos, new ItemStack(produce, extra));
            }
            double potency = g.get(GeneType.POTENCY);
            if (potency > 0.1 && level.random.nextDouble() < potency * 0.12) {
                drop(level, pos, MutantFruitItem.fromPlant(g, crop));
            }
            return;
        }

        String tree = treeSpecies(block);
        if (tree == null) return;
        Genome g = plantGenome(level, tree);
        if (g == null) return;
        com.evolutionmod.core.Adaptation.roll(level, pos, SpeciesProfile.plantId(tree),
            com.evolutionmod.core.Adaptation.Cause.PLANT_BROKEN, null);

        if (block instanceof LeavesBlock) {
            double potency = g.get(GeneType.POTENCY);
            if (potency > 0.1 && level.random.nextDouble() < potency * 0.05) {
                drop(level, pos, MutantFruitItem.fromPlant(g, tree));
            }
        } else if (block instanceof RotatedPillarBlock) {
            double density = g.get(GeneType.DENSITY);
            if (density > 0.4 && level.random.nextDouble() < (density - 0.4) * 0.4) {
                drop(level, pos, new ItemStack(ModBlocks.MUTANT_LOG_ITEM.get()));
            }
        }
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    // ───────────────────────── Arbres ─────────────────────────

    /**
     * La structure de l'arbre n'existe pas encore quand l'événement est émis :
     * on mémorise la position et on traite l'arbre quelques ticks plus tard.
     */
    @SubscribeEvent
    public static void onSaplingGrow(SaplingGrowTreeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        String tree = treeSpecies(level.getBlockState(event.getPos()).getBlock());
        if (tree == null) return;
        PENDING.add(new PendingTree(level, event.getPos().immutable(), tree, level.getGameTime() + 2));
    }

    static void processPending(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        int budget = 8;
        while (!PENDING.isEmpty() && budget-- > 0) {
            PendingTree t = PENDING.peek();
            if (t.level().getGameTime() < t.runAt()) break;
            PENDING.poll();
            Genome g = plantGenome(t.level(), t.species());
            if (g != null) {
                SpeciesRecord r = EvolutionSavedData.get(server).get(SpeciesProfile.plantId(t.species()));
                double div = r == null ? 0 : r.divergence();
                com.evolutionmod.core.TreeMutator.mutateNear(t.level(), t.pos().above(4), t.species(), g, 6,
                    Math.min(1.0, 0.25 + div * 4));
            }
        }
    }
}
