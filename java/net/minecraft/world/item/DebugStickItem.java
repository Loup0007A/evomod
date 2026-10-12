package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item {
   public DebugStickItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      if (!pLevel.isClientSide) {
         this.handleInteraction(pPlayer, pState, pLevel, pPos, false, pPlayer.getItemInHand(InteractionHand.MAIN_HAND));
      }

      return false;
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Player $$1 = pContext.getPlayer();
      Level $$2 = pContext.getLevel();
      if (!$$2.isClientSide && $$1 != null) {
         BlockPos $$3 = pContext.getClickedPos();
         if (!this.handleInteraction($$1, $$2.getBlockState($$3), $$2, $$3, true, pContext.getItemInHand())) {
            return InteractionResult.FAIL;
         }
      }

      return InteractionResult.sidedSuccess($$2.isClientSide);
   }

   private boolean handleInteraction(
      Player pPlayer, BlockState pStateClicked, LevelAccessor pAccessor, BlockPos pPos, boolean pShouldCycleState, ItemStack pDebugStack
   ) {
      if (!pPlayer.canUseGameMasterBlocks()) {
         return false;
      } else {
         Holder<Block> $$6 = pStateClicked.getBlockHolder();
         StateDefinition<Block, BlockState> $$7 = $$6.value().getStateDefinition();
         Collection<Property<?>> $$8 = $$7.getProperties();
         if ($$8.isEmpty()) {
            message(pPlayer, Component.translatable(this.getDescriptionId() + ".empty", $$6.getRegisteredName()));
            return false;
         } else {
            DebugStickState $$9 = pDebugStack.get(DataComponents.DEBUG_STICK_STATE);
            if ($$9 == null) {
               return false;
            } else {
               Property<?> $$10 = $$9.properties().get($$6);
               if (pShouldCycleState) {
                  if ($$10 == null) {
                     $$10 = $$8.iterator().next();
                  }

                  BlockState $$11 = cycleState(pStateClicked, $$10, pPlayer.isSecondaryUseActive());
                  pAccessor.setBlock(pPos, $$11, 18);
                  message(pPlayer, Component.translatable(this.getDescriptionId() + ".update", $$10.getName(), getNameHelper($$11, $$10)));
               } else {
                  $$10 = getRelative($$8, $$10, pPlayer.isSecondaryUseActive());
                  pDebugStack.set(DataComponents.DEBUG_STICK_STATE, $$9.withProperty($$6, $$10));
                  message(pPlayer, Component.translatable(this.getDescriptionId() + ".select", $$10.getName(), getNameHelper(pStateClicked, $$10)));
               }

               return true;
            }
         }
      }
   }

   private static <T extends Comparable<T>> BlockState cycleState(BlockState pState, Property<T> pProperty, boolean pBackwards) {
      return pState.setValue(pProperty, getRelative(pProperty.getPossibleValues(), pState.getValue(pProperty), pBackwards));
   }

   private static <T> T getRelative(Iterable<T> pAllowedValues, @Nullable T pCurrentValue, boolean pBackwards) {
      return pBackwards ? Util.findPreviousInIterable(pAllowedValues, pCurrentValue) : Util.findNextInIterable(pAllowedValues, pCurrentValue);
   }

   private static void message(Player pPlayer, Component pMessageComponent) {
      ((ServerPlayer)pPlayer).sendSystemMessage(pMessageComponent, true);
   }

   private static <T extends Comparable<T>> String getNameHelper(BlockState pState, Property<T> pProperty) {
      return pProperty.getName(pState.getValue(pProperty));
   }
}
