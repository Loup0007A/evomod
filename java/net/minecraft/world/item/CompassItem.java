package net.minecraft.world.item;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CompassItem extends Item {
   public CompassItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Nullable
   public static GlobalPos getSpawnPosition(Level pLevel) {
      return pLevel.dimensionType().natural() ? GlobalPos.of(pLevel.dimension(), pLevel.getSharedSpawnPos()) : null;
   }

   @Override
   public boolean isFoil(ItemStack pStack) {
      return pStack.has(DataComponents.LODESTONE_TRACKER) || super.isFoil(pStack);
   }

   @Override
   public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
      if (pLevel instanceof ServerLevel $$5) {
         LodestoneTracker $$6 = pStack.get(DataComponents.LODESTONE_TRACKER);
         if ($$6 != null) {
            LodestoneTracker $$7 = $$6.tick($$5);
            if ($$7 != $$6) {
               pStack.set(DataComponents.LODESTONE_TRACKER, $$7);
            }
         }
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      BlockPos $$1 = pContext.getClickedPos();
      Level $$2 = pContext.getLevel();
      if (!$$2.getBlockState($$1).is(Blocks.LODESTONE)) {
         return super.useOn(pContext);
      } else {
         $$2.playSound(null, $$1, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
         Player $$3 = pContext.getPlayer();
         ItemStack $$4 = pContext.getItemInHand();
         boolean $$5 = !$$3.hasInfiniteMaterials() && $$4.getCount() == 1;
         LodestoneTracker $$6 = new LodestoneTracker(Optional.of(GlobalPos.of($$2.dimension(), $$1)), true);
         if ($$5) {
            $$4.set(DataComponents.LODESTONE_TRACKER, $$6);
         } else {
            ItemStack $$7 = $$4.transmuteCopy(Items.COMPASS, 1);
            $$4.consume(1, $$3);
            $$7.set(DataComponents.LODESTONE_TRACKER, $$6);
            if (!$$3.getInventory().add($$7)) {
               $$3.drop($$7, false);
            }
         }

         return InteractionResult.sidedSuccess($$2.isClientSide);
      }
   }

   @Override
   public String getDescriptionId(ItemStack pStack) {
      return pStack.has(DataComponents.LODESTONE_TRACKER) ? "item.minecraft.lodestone_compass" : super.getDescriptionId(pStack);
   }
}
