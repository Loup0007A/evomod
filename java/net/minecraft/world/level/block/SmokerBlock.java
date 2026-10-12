package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SmokerBlock extends AbstractFurnaceBlock {
   public static final MapCodec<SmokerBlock> CODEC = simpleCodec(SmokerBlock::new);

   @Override
   public MapCodec<SmokerBlock> codec() {
      return CODEC;
   }

   public SmokerBlock(BlockBehaviour.Properties p_56439_) {
      super(p_56439_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new SmokerBlockEntity(pPos, pState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createFurnaceTicker(pLevel, pBlockEntityType, BlockEntityType.SMOKER);
   }

   @Override
   protected void openContainer(Level pLevel, BlockPos pPos, Player pPlayer) {
      BlockEntity $$3 = pLevel.getBlockEntity(pPos);
      if ($$3 instanceof SmokerBlockEntity) {
         pPlayer.openMenu((MenuProvider)$$3);
         pPlayer.awardStat(Stats.INTERACT_WITH_SMOKER);
      }
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(LIT)) {
         double $$4 = (double)pPos.getX() + 0.5;
         double $$5 = (double)pPos.getY();
         double $$6 = (double)pPos.getZ() + 0.5;
         if (pRandom.nextDouble() < 0.1) {
            pLevel.playLocalSound($$4, $$5, $$6, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         pLevel.addParticle(ParticleTypes.SMOKE, $$4, $$5 + 1.1, $$6, 0.0, 0.0, 0.0);
      }
   }
}
