package com.evolutionmod.core;

import com.evolutionmod.blocks.BioluminescentLeavesBlock;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.blocks.MutantLogBlock;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Rend l'évolution des arbres visible dans le monde : les feuillages prennent
 * la teinte (et la lumière) génétique de leur espèce, les troncs deviennent
 * du bois mutant quand le gène DENSITY est élevé.
 */
public final class TreeMutator {

    private TreeMutator() {}

    private static final RandomSource RANDOM = RandomSource.create();

    /** Espèce d'arbre d'un bloc (feuilles, bûche, bois, pousse) ou null. */
    public static String treeSpecies(Block block) {
        var key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null || !key.getNamespace().equals("minecraft")) return null;
        String p = key.getPath();
        if (p.startsWith("stripped_")) p = p.substring(9);
        for (String s : SpeciesProfile.TREE_SPECIES) {
            if (p.equals(s + "_sapling") || p.equals(s + "_log") || p.equals(s + "_wood")
                || p.equals(s + "_leaves") || (s.equals("mangrove") && p.equals("mangrove_propagule"))) {
                return s;
            }
        }
        return null;
    }

    /**
     * Fait muter les arbres de l'espèce autour d'une position.
     *
     * @param strength 0..1, part des blocs convertis (multipliée par la divergence)
     * @return nombre de blocs modifiés
     */
    public static int mutateNear(ServerLevel level, BlockPos center, String species, Genome g, int radius,
                                 double strength) {
        int hue = BioluminescentLeavesBlock.hueToSegment(g.get(GeneType.HUE));
        int light = (int) Math.round(Math.max(0, g.get(GeneType.GLOW)) * 15);
        double density = g.get(GeneType.DENSITY);
        double chance = Math.min(0.9, strength * (0.35 + g.get(GeneType.PIGMENT) + g.get(GeneType.GLOW)));
        int changed = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
            center.offset(radius, radius + 4, radius))) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            Block b = s.getBlock();
            if (b instanceof BioluminescentLeavesBlock || b instanceof MutantLogBlock) continue;
            if (!species.equals(treeSpecies(b))) continue;
            if (b instanceof LeavesBlock && RANDOM.nextDouble() < chance) {
                BlockState leaves = ModBlocks.BIOLUMINESCENT_LEAVES.get().defaultBlockState()
                    .setValue(BioluminescentLeavesBlock.GENETIC_LIGHT, light)
                    .setValue(BioluminescentLeavesBlock.GENETIC_HUE, hue)
                    .setValue(LeavesBlock.DISTANCE, s.getValue(LeavesBlock.DISTANCE))
                    .setValue(LeavesBlock.PERSISTENT, s.getValue(LeavesBlock.PERSISTENT));
                level.setBlock(p, leaves, Block.UPDATE_CLIENTS);
                changed++;
            } else if (b instanceof RotatedPillarBlock && density > 0.35
                && RANDOM.nextDouble() < (density - 0.25) * strength) {
                level.setBlock(p, ModBlocks.MUTANT_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS))
                    .setValue(MutantLogBlock.GENETIC_HUE, hue), Block.UPDATE_CLIENTS);
                changed++;
            }
        }
        return changed;
    }

    /**
     * Pendant le cycle quotidien : autour de chaque joueur, quelques arbres
     * existants expriment l'évolution de leur espèce.
     */
    public static void dailySpread(ServerLevel level) {
        EvolutionSavedData data = EvolutionSavedData.get(level.getServer());
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < 6; i++) {
                int x = player.getBlockX() + RANDOM.nextInt(81) - 40;
                int z = player.getBlockZ() + RANDOM.nextInt(81) - 40;
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
                if (!level.isLoaded(top)) continue;
                String species = treeSpecies(level.getBlockState(top).getBlock());
                if (species == null) continue;
                SpeciesRecord r = data.get(SpeciesProfile.plantId(species));
                if (r == null) continue;
                double div = r.divergence();
                if (div < 0.03) continue;
                mutateNear(level, top, species, r.template(), 4, Math.min(1.0, div * 4));
            }
        }
    }
}
