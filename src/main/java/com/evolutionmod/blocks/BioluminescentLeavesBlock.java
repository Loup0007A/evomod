package com.evolutionmod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Feuilles Bioluminescentes — produites par les arbres dont le gène
 * BIOLUMINESCENCE dépasse le seuil de mutation (>8.0).
 *
 * Contrairement aux feuilles classiques, ce bloc émet de la lumière
 * (niveau dépendant du gène stocké) et change de couleur selon le
 * COLOR_HUE de l'arbre parent, encodé dans la propriété GENETIC_HUE_SEGMENT.
 *
 * Le niveau de lumière et la teinte sont fixés à la pousse de l'arbre
 * (voir TreeEvolutionHandler#makeBioluminescentLeaves) et restent stables
 * jusqu'à la prochaine génération.
 */
public class BioluminescentLeavesBlock extends LeavesBlock {

    /** Niveau de lumière génétique (0-15), dérivé du gène BIOLUMINESCENCE de l'arbre. */
    public static final IntegerProperty GENETIC_LIGHT =
            IntegerProperty.create("genetic_light", 0, 15);

    /** Teinte génétique discrétisée en 8 segments de 45° (pour limiter les blockstates). */
    public static final IntegerProperty GENETIC_HUE_SEGMENT =
            IntegerProperty.create("genetic_hue", 0, 7);

    public BioluminescentLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(GENETIC_LIGHT, 8)
                .setValue(GENETIC_HUE_SEGMENT, 0)
                .setValue(DISTANCE, 7)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GENETIC_LIGHT, GENETIC_HUE_SEGMENT);
    }

    /**
     * NOTE DE SIGNATURE : comme pour getDestroyProgress dans MutantLogBlock,
     * le 2e paramètre doit être exactement {@link net.minecraft.world.level.BlockGetter}.
     * Un type différent (même un sur-type compatible comme LevelReader) ferait
     * de cette méthode une SURCHARGE indépendante au lieu d'un override réel,
     * et la lumière génétique ne s'afficherait jamais malgré une compilation propre.
     */
    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(GENETIC_LIGHT);
    }

    /**
     * Propriétés Vanilla/Forge standards pour un bloc de feuilles fonctionnel
     * (decay, opacité, son, inflammabilité). Utilisé par ModBlocks lors de l'enregistrement.
     */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2f)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    /**
     * Encode une teinte HSV (0-360°) en segment discret (0-7) pour le blockstate.
     */
    public static int hueToSegment(double hue) {
        return (int) Math.floor((hue % 360) / 45.0) & 7;
    }

    /**
     * Convertit un segment en couleur RGB approximative pour le renderer client.
     * Utilisé par le BlockColor handler (voir ClientBlockColors).
     */
    public static int segmentToRgb(int segment) {
        float hue = (segment * 45f) / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.65f, 0.9f);
        return rgb & 0xFFFFFF;
    }
}
