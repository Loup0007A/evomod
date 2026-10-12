package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WetSpongeBlock extends Block {
   public static final MapCodec<WetSpongeBlock> CODEC = simpleCodec(WetSpongeBlock::new);

   @Override
   public MapCodec<WetSpongeBlock> codec() {
      return CODEC;
   }

   public WetSpongeBlock(BlockBehaviour.Properties p_58222_) {
      super(p_58222_);
   }

   @Override
   protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
      if (pLevel.dimensionType().ultraWarm()) {
         pLevel.setBlock(pPos, Blocks.SPONGE.defaultBlockState(), 3);
         pLevel.levelEvent(2009, pPos, 0);
         pLevel.playSound(null, pPos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F, (1.0F + pLevel.getRandom().nextFloat() * 0.2F) * 0.7F);
      }
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      Direction $$4 = Direction.getRandom(pRandom);
      if ($$4 != Direction.UP) {
         BlockPos $$5 = pPos.relative($$4);
         BlockState $$6 = pLevel.getBlockState($$5);
         if (!pState.canOcclude() || !$$6.isFaceSturdy(pLevel, $$5, $$4.getOpposite())) {
            double $$7 = (double)pPos.getX();
            double $$8 = (double)pPos.getY();
            double $$9 = (double)pPos.getZ();
            if ($$4 == Direction.DOWN) {
               $$8 -= 0.05;
               $$7 += pRandom.nextDouble();
               $$9 += pRandom.nextDouble();
            } else {
               $$8 += pRandom.nextDouble() * 0.8;
               if ($$4.getAxis() == Direction.Axis.X) {
                  $$9 += pRandom.nextDouble();
                  if ($$4 == Direction.EAST) {
                     $$7++;
                  } else {
                     $$7 += 0.05;
                  }
               } else {
                  $$7 += pRandom.nextDouble();
                  if ($$4 == Direction.SOUTH) {
                     $$9++;
                  } else {
                     $$9 += 0.05;
                  }
               }
            }

            pLevel.addParticle(ParticleTypes.DRIPPING_WATER, $$7, $$8, $$9, 0.0, 0.0, 0.0);
         }
      }
   }
}
