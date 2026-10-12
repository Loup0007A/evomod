package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BottleItem extends Item {
   public BottleItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      List<AreaEffectCloud> $$3 = pLevel.getEntitiesOfClass(
         AreaEffectCloud.class,
         pPlayer.getBoundingBox().inflate(2.0),
         p_341522_ -> p_341522_ != null && p_341522_.isAlive() && p_341522_.getOwner() instanceof EnderDragon
      );
      ItemStack $$4 = pPlayer.getItemInHand(pHand);
      if (!$$3.isEmpty()) {
         AreaEffectCloud $$5 = $$3.get(0);
         $$5.setRadius($$5.getRadius() - 0.5F);
         pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
         pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, pPlayer.position());
         if (pPlayer instanceof ServerPlayer $$6) {
            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger($$6, $$4, $$5);
         }

         return InteractionResultHolder.sidedSuccess(this.turnBottleIntoItem($$4, pPlayer, new ItemStack(Items.DRAGON_BREATH)), pLevel.isClientSide());
      } else {
         BlockHitResult $$7 = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
         if ($$7.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass($$4);
         } else {
            if ($$7.getType() == HitResult.Type.BLOCK) {
               BlockPos $$8 = $$7.getBlockPos();
               if (!pLevel.mayInteract(pPlayer, $$8)) {
                  return InteractionResultHolder.pass($$4);
               }

               if (pLevel.getFluidState($$8).is(FluidTags.WATER)) {
                  pLevel.playSound(pPlayer, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                  pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, $$8);
                  return InteractionResultHolder.sidedSuccess(
                     this.turnBottleIntoItem($$4, pPlayer, PotionContents.createItemStack(Items.POTION, Potions.WATER)), pLevel.isClientSide()
                  );
               }
            }

            return InteractionResultHolder.pass($$4);
         }
      }
   }

   protected ItemStack turnBottleIntoItem(ItemStack pBottleStack, Player pPlayer, ItemStack pFilledBottleStack) {
      pPlayer.awardStat(Stats.ITEM_USED.get(this));
      return ItemUtils.createFilledResult(pBottleStack, pPlayer, pFilledBottleStack);
   }
}
