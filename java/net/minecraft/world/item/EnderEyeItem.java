package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EnderEyeItem extends Item {
   public EnderEyeItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Level $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      BlockState $$3 = $$1.getBlockState($$2);
      if (!$$3.is(Blocks.END_PORTAL_FRAME) || $$3.getValue(EndPortalFrameBlock.HAS_EYE)) {
         return InteractionResult.PASS;
      } else if ($$1.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockState $$4 = $$3.setValue(EndPortalFrameBlock.HAS_EYE, Boolean.valueOf(true));
         Block.pushEntitiesUp($$3, $$4, $$1, $$2);
         $$1.setBlock($$2, $$4, 2);
         $$1.updateNeighbourForOutputSignal($$2, Blocks.END_PORTAL_FRAME);
         pContext.getItemInHand().shrink(1);
         $$1.levelEvent(1503, $$2, 0);
         BlockPattern.BlockPatternMatch $$5 = EndPortalFrameBlock.getOrCreatePortalShape().find($$1, $$2);
         if ($$5 != null) {
            BlockPos $$6 = $$5.getFrontTopLeft().offset(-3, 0, -3);

            for (int $$7 = 0; $$7 < 3; $$7++) {
               for (int $$8 = 0; $$8 < 3; $$8++) {
                  $$1.setBlock($$6.offset($$7, 0, $$8), Blocks.END_PORTAL.defaultBlockState(), 2);
               }
            }

            $$1.globalLevelEvent(1038, $$6.offset(1, 0, 1), 0);
         }

         return InteractionResult.CONSUME;
      }
   }

   @Override
   public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
      return 0;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      BlockHitResult $$4 = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.NONE);
      if ($$4.getType() == HitResult.Type.BLOCK && pLevel.getBlockState($$4.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
         return InteractionResultHolder.pass($$3);
      } else {
         pPlayer.startUsingItem(pHand);
         if (pLevel instanceof ServerLevel $$5) {
            BlockPos $$6 = $$5.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, pPlayer.blockPosition(), 100, false);
            if ($$6 != null) {
               EyeOfEnder $$7 = new EyeOfEnder(pLevel, pPlayer.getX(), pPlayer.getY(0.5), pPlayer.getZ());
               $$7.setItem($$3);
               $$7.signalTo($$6);
               pLevel.gameEvent(GameEvent.PROJECTILE_SHOOT, $$7.position(), GameEvent.Context.of(pPlayer));
               pLevel.addFreshEntity($$7);
               if (pPlayer instanceof ServerPlayer $$8) {
                  CriteriaTriggers.USED_ENDER_EYE.trigger($$8, $$6);
               }

               float $$9 = Mth.lerp(pLevel.random.nextFloat(), 0.33F, 0.5F);
               pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, $$9);
               $$3.consume(1, pPlayer);
               pPlayer.awardStat(Stats.ITEM_USED.get(this));
               pPlayer.swing(pHand, true);
               return InteractionResultHolder.success($$3);
            }
         }

         return InteractionResultHolder.consume($$3);
      }
   }
}
