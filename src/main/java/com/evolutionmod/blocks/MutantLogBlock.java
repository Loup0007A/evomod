package com.evolutionmod.blocks;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * Bûche Mutante — variante générée quand le gène WOOD_HARDNESS d'une espèce
 * d'arbre dépasse 7.0 (voir TreeEvolutionHandler).
 *
 * Le niveau de dureté génétique est stocké dans le blockstate ; il influence :
 *  - le temps de minage requis (via getDestroySpeed côté outil, géré par tags)
 *  - le type de loot généré au cassage (MutantWoodDrop)
 *  - la couleur de teinte du bloc (segments HSV comme pour les feuilles)
 *
 * Ce bloc reste un RotatedPillarBlock standard pour conserver
 * le comportement d'orientation des bûches vanilla (axe X/Y/Z).
 */
public class MutantLogBlock extends RotatedPillarBlock {

    /** Niveau de dureté génétique discrétisé (0-10), dérivé de WOOD_HARDNESS. */
    public static final IntegerProperty GENETIC_HARDNESS =
            IntegerProperty.create("genetic_hardness", 0, 10);

    /** Teinte génétique du bois, même encodage que les feuilles bioluminescentes. */
    public static final IntegerProperty GENETIC_HUE_SEGMENT =
            IntegerProperty.create("genetic_hue", 0, 7);

    public MutantLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(GENETIC_HARDNESS, 5)
                .setValue(GENETIC_HUE_SEGMENT, 0)
                .setValue(AXIS, net.minecraft.core.Direction.Axis.Y)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GENETIC_HARDNESS, GENETIC_HUE_SEGMENT);
    }

    /**
     * NOTE DE SIGNATURE (1.21.1, confirmé) : la signature réelle est à QUATRE
     * paramètres — (BlockState, Player, BlockGetter, BlockPos). Une signature
     * à trois paramètres sans BlockState ne correspond à aucune méthode de la
     * classe parente et ne serait jamais appelée par le moteur de jeu (le
     * compilateur la traite comme une méthode indépendante, pas un override).
     */
    @Override
    protected float getDestroyProgress(BlockState state,
                                        net.minecraft.world.entity.player.Player player,
                                        net.minecraft.world.level.BlockGetter level,
                                        net.minecraft.core.BlockPos pos) {
        int hardness = state.hasProperty(GENETIC_HARDNESS) ? state.getValue(GENETIC_HARDNESS) : 5;
        // Plus le gène de dureté est élevé, plus le minage est lent (jusqu'à ~3x)
        float speedMultiplier = 1.0f - (hardness / 10.0f) * 0.66f;
        return super.getDestroyProgress(state, player, level, pos) * Math.max(0.15f, speedMultiplier);
    }

    /**
     * Propriétés Vanilla/Forge pour un bloc de bûche fonctionnel.
     * La résistance/explosion-resistance de base augmente avec la dureté
     * moyenne attendue (8.0 → 6 sur l'échelle Minecraft).
     */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(3.0f, 6.0f)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    public static int hueToSegment(double hue) {
        return BioluminescentLeavesBlock.hueToSegment(hue);
    }

    public static int hardnessToLevel(double hardness) {
        return (int) Math.max(0, Math.min(10, Math.round(hardness)));
    }
}
