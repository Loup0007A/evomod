package com.evolutionmod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.joml.Vector3f;

/** Soufre laissé par l'explosion d'un chaudron : fumant et irritant. */
public class SulfurBlock extends Block {

    public SulfurBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW)
            .strength(0.8f)
            .sound(SoundType.SAND)
            .lightLevel(s -> 3);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && !living.isSteppingCarefully()
            && level.random.nextInt(40) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.95f, 0.85f, 0.2f), 0.7f),
                pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
    }
}
