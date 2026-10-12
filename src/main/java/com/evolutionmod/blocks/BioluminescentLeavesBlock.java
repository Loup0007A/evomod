package com.evolutionmod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.joml.Vector3f;

/**
 * Feuillage d'un arbre dont le gène GLOW a muté : il émet une lumière
 * proportionnelle au gène et prend la teinte génétique de l'espèce.
 */
public class BioluminescentLeavesBlock extends LeavesBlock {

    public static final IntegerProperty GENETIC_LIGHT = IntegerProperty.create("genetic_light", 0, 15);
    public static final IntegerProperty GENETIC_HUE = IntegerProperty.create("genetic_hue", 0, 7);

    public BioluminescentLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(GENETIC_LIGHT, 10)
            .setValue(GENETIC_HUE, 4)
            .setValue(DISTANCE, 7)
            .setValue(PERSISTENT, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GENETIC_LIGHT, GENETIC_HUE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        int light = state.getValue(GENETIC_LIGHT);
        if (light > 3 && random.nextInt(18 - light) == 0) {
            int rgb = segmentToRgb(state.getValue(GENETIC_HUE));
            Vector3f c = new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
            level.addParticle(new DustParticleOptions(c, 0.8f),
                pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                0, -0.02, 0);
        }
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(0.2f)
            .randomTicks()
            .sound(SoundType.AZALEA_LEAVES)
            .noOcclusion()
            .lightLevel(s -> s.getValue(GENETIC_LIGHT))
            .isValidSpawn((s, l, p, t) -> false)
            .isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor((s, l, p) -> false);
    }

    public static int hueToSegment(double hue) {
        double h = ((hue % 360) + 360) % 360;
        return ((int) Math.floor(h / 45.0)) & 7;
    }

    public static int segmentToRgb(int segment) {
        float hue = (segment * 45f + 22.5f) / 360f;
        return java.awt.Color.HSBtoRGB(hue, 0.7f, 0.95f) & 0xFFFFFF;
    }
}
