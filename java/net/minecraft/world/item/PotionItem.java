package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {
   private static final int DRINK_DURATION = 32;

   public PotionItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack $$0 = super.getDefaultInstance();
      $$0.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
      return $$0;
   }

   @Override
   public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
      Player $$3 = pEntityLiving instanceof Player ? (Player)pEntityLiving : null;
      if ($$3 instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)$$3, pStack);
      }

      if (!pLevel.isClientSide) {
         PotionContents $$4 = pStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
         $$4.forEachEffect(p_327729_ -> {
            if (p_327729_.getEffect().value().isInstantenous()) {
               p_327729_.getEffect().value().applyInstantenousEffect($$3, $$3, pEntityLiving, p_327729_.getAmplifier(), 1.0);
            } else {
               pEntityLiving.addEffect(p_327729_);
            }
         });
      }

      if ($$3 != null) {
         $$3.awardStat(Stats.ITEM_USED.get(this));
         pStack.consume(1, $$3);
      }

      if ($$3 == null || !$$3.hasInfiniteMaterials()) {
         if (pStack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
         }

         if ($$3 != null) {
            $$3.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
         }
      }

      pEntityLiving.gameEvent(GameEvent.DRINK);
      return pStack;
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Level $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      Player $$3 = pContext.getPlayer();
      ItemStack $$4 = pContext.getItemInHand();
      PotionContents $$5 = $$4.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      BlockState $$6 = $$1.getBlockState($$2);
      if (pContext.getClickedFace() != Direction.DOWN && $$6.is(BlockTags.CONVERTABLE_TO_MUD) && $$5.is(Potions.WATER)) {
         $$1.playSound(null, $$2, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
         $$3.setItemInHand(pContext.getHand(), ItemUtils.createFilledResult($$4, $$3, new ItemStack(Items.GLASS_BOTTLE)));
         $$3.awardStat(Stats.ITEM_USED.get($$4.getItem()));
         if (!$$1.isClientSide) {
            ServerLevel $$7 = (ServerLevel)$$1;

            for (int $$8 = 0; $$8 < 5; $$8++) {
               $$7.sendParticles(
                  ParticleTypes.SPLASH,
                  (double)$$2.getX() + $$1.random.nextDouble(),
                  (double)($$2.getY() + 1),
                  (double)$$2.getZ() + $$1.random.nextDouble(),
                  1,
                  0.0,
                  0.0,
                  0.0,
                  1.0
               );
            }
         }

         $$1.playSound(null, $$2, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         $$1.gameEvent(null, GameEvent.FLUID_PLACE, $$2);
         $$1.setBlockAndUpdate($$2, Blocks.MUD.defaultBlockState());
         return InteractionResult.sidedSuccess($$1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
      return 32;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.DRINK;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      return ItemUtils.startUsingInstantly(pLevel, pPlayer, pHand);
   }

   @Override
   public String getDescriptionId(ItemStack pStack) {
      return Potion.getName(pStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion(), this.getDescriptionId() + ".effect.");
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      PotionContents $$4 = pStack.get(DataComponents.POTION_CONTENTS);
      if ($$4 != null) {
         $$4.addPotionTooltip(pTooltipComponents::add, 1.0F, pContext.tickRate());
      }
   }
}
