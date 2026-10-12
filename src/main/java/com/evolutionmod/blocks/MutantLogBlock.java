package com.evolutionmod.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/** Bûche d'un arbre dont le gène DENSITY a muté : bois dense et teinté. */
public class MutantLogBlock extends RotatedPillarBlock {

    public static final IntegerProperty GENETIC_HUE = IntegerProperty.create("genetic_hue", 0, 7);

    public MutantLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(GENETIC_HUE, 4)
            .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GENETIC_HUE);
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .strength(4.0f, 8.0f)
            .sound(SoundType.WOOD)
            .ignitedByLava();
    }
}
