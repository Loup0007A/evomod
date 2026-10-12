package net.minecraft.world.item;

import java.util.List;
import java.util.Random;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class ShearsItem extends Item {
   public ShearsItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public static Tool createToolProperties() {
      return new Tool(
         List.of(
            Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0F),
            Tool.Rule.overrideSpeed(List.of(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F)
         ),
         1.0F,
         1
      );
   }

   @Override
   public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
      if (!pLevel.isClientSide && !pState.is(BlockTags.FIRE)) {
         pStack.hurtAndBreak(1, pEntityLiving, EquipmentSlot.MAINHAND);
      }

      return pState.is(BlockTags.LEAVES)
         || pState.is(Blocks.COBWEB)
         || pState.is(Blocks.SHORT_GRASS)
         || pState.is(Blocks.FERN)
         || pState.is(Blocks.DEAD_BUSH)
         || pState.is(Blocks.HANGING_ROOTS)
         || pState.is(Blocks.VINE)
         || pState.is(Blocks.TRIPWIRE)
         || pState.is(BlockTags.WOOL);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (blockstate.getBlock() instanceof GrowingPlantHeadBlock growingplantheadblock && !growingplantheadblock.isMaxAge(blockstate)) {
         Player player = pContext.getPlayer();
         ItemStack itemstack = pContext.getItemInHand();
         if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
         }

         level.playSound(player, blockpos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
         BlockState blockstate1 = growingplantheadblock.getMaxAgeState(blockstate);
         level.setBlockAndUpdate(blockpos, blockstate1);
         level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(pContext.getPlayer(), blockstate1));
         if (player != null) {
            itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(pContext.getHand()));
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }

      return super.useOn(pContext);
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity entity, InteractionHand hand) {
      if (entity instanceof IForgeShearable target) {
         if (entity.level().isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            BlockPos pos = BlockPos.containing(entity.position());
            if (target.isShearable(stack, entity.level(), pos)) {
               ResourceKey<Enchantment> key = Enchantments.FORTUNE;
               List<ItemStack> drops = target.onSheared(
                  playerIn,
                  stack,
                  entity.level(),
                  pos,
                  EnchantmentHelper.getItemEnchantmentLevel(entity.level().holderLookup(key.registryKey()).getOrThrow(key), stack)
               );
               Random rand = new Random();
               drops.forEach(
                  d -> {
                     ItemEntity ent = entity.spawnAtLocation(d, 1.0F);
                     ent.setDeltaMovement(
                        ent.getDeltaMovement()
                           .add(
                              (double)((rand.nextFloat() - rand.nextFloat()) * 0.1F),
                              (double)(rand.nextFloat() * 0.05F),
                              (double)((rand.nextFloat() - rand.nextFloat()) * 0.1F)
                           )
                     );
                  }
               );
               if (!drops.isEmpty()) {
                  stack.hurtAndBreak(1, playerIn, LivingEntity.getSlotForHand(hand));
               }
            }

            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
      return ToolActions.DEFAULT_SHEARS_ACTIONS.contains(toolAction);
   }
}
