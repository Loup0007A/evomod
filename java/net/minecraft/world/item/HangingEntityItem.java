package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
   private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
   private final EntityType<? extends HangingEntity> type;

   public HangingEntityItem(EntityType<? extends HangingEntity> pType, Item.Properties pProperties) {
      super(pProperties);
      this.type = pType;
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      BlockPos $$1 = pContext.getClickedPos();
      Direction $$2 = pContext.getClickedFace();
      BlockPos $$3 = $$1.relative($$2);
      Player $$4 = pContext.getPlayer();
      ItemStack $$5 = pContext.getItemInHand();
      if ($$4 != null && !this.mayPlace($$4, $$2, $$5, $$3)) {
         return InteractionResult.FAIL;
      } else {
         Level $$6 = pContext.getLevel();
         HangingEntity $$8;
         if (this.type == EntityType.PAINTING) {
            Optional<Painting> $$7 = Painting.create($$6, $$3, $$2);
            if ($$7.isEmpty()) {
               return InteractionResult.CONSUME;
            }

            $$8 = $$7.get();
         } else if (this.type == EntityType.ITEM_FRAME) {
            $$8 = new ItemFrame($$6, $$3, $$2);
         } else {
            if (this.type != EntityType.GLOW_ITEM_FRAME) {
               return InteractionResult.sidedSuccess($$6.isClientSide);
            }

            $$8 = new GlowItemFrame($$6, $$3, $$2);
         }

         CustomData $$12 = $$5.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
         if (!$$12.isEmpty()) {
            EntityType.updateCustomEntityTag($$6, $$4, $$8, $$12);
         }

         if ($$8.survives()) {
            if (!$$6.isClientSide) {
               $$8.playPlacementSound();
               $$6.gameEvent($$4, GameEvent.ENTITY_PLACE, $$8.position());
               $$6.addFreshEntity($$8);
            }

            $$5.shrink(1);
            return InteractionResult.sidedSuccess($$6.isClientSide);
         } else {
            return InteractionResult.CONSUME;
         }
      }
   }

   protected boolean mayPlace(Player pPlayer, Direction pDirection, ItemStack pHangingEntityStack, BlockPos pPos) {
      return !pDirection.getAxis().isVertical() && pPlayer.mayUseItemAt(pPos, pDirection, pHangingEntityStack);
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
      HolderLookup.Provider $$4 = pContext.registries();
      if ($$4 != null && this.type == EntityType.PAINTING) {
         CustomData $$5 = pStack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
         if (!$$5.isEmpty()) {
            $$5.read($$4.createSerializationContext(NbtOps.INSTANCE), Painting.VARIANT_MAP_CODEC)
               .result()
               .ifPresentOrElse(
                  p_341548_ -> {
                     p_341548_.unwrapKey().ifPresent(p_270217_ -> {
                        pTooltipComponents.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW));
                        pTooltipComponents.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY));
                     });
                     pTooltipComponents.add(
                        Component.translatable(
                           "painting.dimensions", ((PaintingVariant)p_341548_.value()).width(), ((PaintingVariant)p_341548_.value()).height()
                        )
                     );
                  },
                  () -> pTooltipComponents.add(TOOLTIP_RANDOM_VARIANT)
               );
         } else if (pTooltipFlag.isCreative()) {
            pTooltipComponents.add(TOOLTIP_RANDOM_VARIANT);
         }
      }
   }
}
