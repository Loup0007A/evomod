package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WitherRoseBlock extends FlowerBlock {
   public static final MapCodec<WitherRoseBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_312834_ -> p_312834_.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(p_312834_, WitherRoseBlock::new)
   );

   @Override
   public MapCodec<WitherRoseBlock> codec() {
      return CODEC;
   }

   public WitherRoseBlock(Holder<MobEffect> pEffect, float pSeconds, BlockBehaviour.Properties pProperties) {
      this(makeEffectList(pEffect, pSeconds), pProperties);
   }

   public WitherRoseBlock(SuspiciousStewEffects p_333459_, BlockBehaviour.Properties p_310026_) {
      super(p_333459_, p_310026_);
   }

   @Override
   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return super.mayPlaceOn(pState, pLevel, pPos) || pState.is(Blocks.NETHERRACK) || pState.is(Blocks.SOUL_SAND) || pState.is(Blocks.SOUL_SOIL);
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      VoxelShape $$4 = this.getShape(pState, pLevel, pPos, CollisionContext.empty());
      Vec3 $$5 = $$4.bounds().getCenter();
      double $$6 = (double)pPos.getX() + $$5.x;
      double $$7 = (double)pPos.getZ() + $$5.z;

      for (int $$8 = 0; $$8 < 3; $$8++) {
         if (pRandom.nextBoolean()) {
            pLevel.addParticle(
               ParticleTypes.SMOKE,
               $$6 + pRandom.nextDouble() / 5.0,
               (double)pPos.getY() + (0.5 - pRandom.nextDouble()),
               $$7 + pRandom.nextDouble() / 5.0,
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (!pLevel.isClientSide && pLevel.getDifficulty() != Difficulty.PEACEFUL) {
         if (pEntity instanceof LivingEntity $$4 && !$$4.isInvulnerableTo(pLevel.damageSources().wither())) {
            $$4.addEffect(new MobEffectInstance(MobEffects.WITHER, 40));
         }
      }
   }
}
