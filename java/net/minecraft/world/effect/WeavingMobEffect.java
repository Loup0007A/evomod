package net.minecraft.world.effect;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

class WeavingMobEffect extends MobEffect {
   private final ToIntFunction<RandomSource> maxCobwebs;

   protected WeavingMobEffect(MobEffectCategory pCategory, int pColor, ToIntFunction<RandomSource> pMaxCobwebs) {
      super(pCategory, pColor, ParticleTypes.ITEM_COBWEB);
      this.maxCobwebs = pMaxCobwebs;
   }

   @Override
   public void onMobRemoved(LivingEntity pLivingEntity, int pAmplifier, Entity.RemovalReason pReason) {
      if (pReason == Entity.RemovalReason.KILLED
         && (pLivingEntity instanceof Player || pLivingEntity.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))) {
         this.spawnCobwebsRandomlyAround(pLivingEntity.level(), pLivingEntity.getRandom(), pLivingEntity.getOnPos());
      }
   }

   private void spawnCobwebsRandomlyAround(Level pLevel, RandomSource pRandom, BlockPos pPos) {
      Set<BlockPos> $$3 = Sets.newHashSet();
      int $$4 = this.maxCobwebs.applyAsInt(pRandom);

      for (BlockPos $$5 : BlockPos.randomInCube(pRandom, 15, pPos, 1)) {
         BlockPos $$6 = $$5.below();
         if (!$$3.contains($$5) && pLevel.getBlockState($$5).canBeReplaced() && pLevel.getBlockState($$6).isFaceSturdy(pLevel, $$6, Direction.UP)) {
            $$3.add($$5.immutable());
            if ($$3.size() >= $$4) {
               break;
            }
         }
      }

      for (BlockPos $$7 : $$3) {
         pLevel.setBlock($$7, Blocks.COBWEB.defaultBlockState(), 3);
         pLevel.levelEvent(3018, $$7, 0);
      }
   }
}
