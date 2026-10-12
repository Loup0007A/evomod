package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
   public LeadItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Level $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      BlockState $$3 = $$1.getBlockState($$2);
      if ($$3.is(BlockTags.FENCES)) {
         Player $$4 = pContext.getPlayer();
         if (!$$1.isClientSide && $$4 != null) {
            bindPlayerMobs($$4, $$1, $$2);
         }

         return InteractionResult.sidedSuccess($$1.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static InteractionResult bindPlayerMobs(Player pPlayer, Level pLevel, BlockPos pPos) {
      LeashFenceKnotEntity $$3 = null;
      List<Leashable> $$4 = leashableInArea(pLevel, pPos, p_341570_ -> p_341570_.getLeashHolder() == pPlayer);

      for (Leashable $$5 : $$4) {
         if ($$3 == null) {
            $$3 = LeashFenceKnotEntity.getOrCreateKnot(pLevel, pPos);
            $$3.playPlacementSound();
         }

         $$5.setLeashedTo($$3, true);
      }

      if (!$$4.isEmpty()) {
         pLevel.gameEvent(GameEvent.BLOCK_ATTACH, pPos, GameEvent.Context.of(pPlayer));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static List<Leashable> leashableInArea(Level pLevel, BlockPos pPos, Predicate<Leashable> pPredicate) {
      double $$3 = 7.0;
      int $$4 = pPos.getX();
      int $$5 = pPos.getY();
      int $$6 = pPos.getZ();
      AABB $$7 = new AABB((double)$$4 - 7.0, (double)$$5 - 7.0, (double)$$6 - 7.0, (double)$$4 + 7.0, (double)$$5 + 7.0, (double)$$6 + 7.0);
      return pLevel.getEntitiesOfClass(Entity.class, $$7, p_341568_ -> {
         if (p_341568_ instanceof Leashable $$2 && pPredicate.test($$2)) {
            return true;
         }

         return false;
      }).stream().map(Leashable.class::cast).toList();
   }
}
