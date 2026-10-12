package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
   private static final int CHECK_TICK_DELAY = 5;
   private int openCount;
   private double maxInteractionRange;

   protected abstract void onOpen(Level pLevel, BlockPos pPos, BlockState pState);

   protected abstract void onClose(Level pLevel, BlockPos pPos, BlockState pState);

   protected abstract void openerCountChanged(Level pLevel, BlockPos pPos, BlockState pState, int pCount, int pOpenCount);

   protected abstract boolean isOwnContainer(Player p_155451_);

   public void incrementOpeners(Player pPlayer, Level pLevel, BlockPos pPos, BlockState pState) {
      int $$4 = this.openCount++;
      if ($$4 == 0) {
         this.onOpen(pLevel, pPos, pState);
         pLevel.gameEvent(pPlayer, GameEvent.CONTAINER_OPEN, pPos);
         scheduleRecheck(pLevel, pPos, pState);
      }

      this.openerCountChanged(pLevel, pPos, pState, $$4, this.openCount);
      this.maxInteractionRange = Math.max(pPlayer.blockInteractionRange(), this.maxInteractionRange);
   }

   public void decrementOpeners(Player pPlayer, Level pLevel, BlockPos pPos, BlockState pState) {
      int $$4 = this.openCount--;
      if (this.openCount == 0) {
         this.onClose(pLevel, pPos, pState);
         pLevel.gameEvent(pPlayer, GameEvent.CONTAINER_CLOSE, pPos);
         this.maxInteractionRange = 0.0;
      }

      this.openerCountChanged(pLevel, pPos, pState, $$4, this.openCount);
   }

   private List<Player> getPlayersWithContainerOpen(Level pLevel, BlockPos pPos) {
      double $$2 = this.maxInteractionRange + 4.0;
      AABB $$3 = new AABB(pPos).inflate($$2);
      return pLevel.getEntities(EntityTypeTest.forClass(Player.class), $$3, this::isOwnContainer);
   }

   public void recheckOpeners(Level pLevel, BlockPos pPos, BlockState pState) {
      List<Player> $$3 = this.getPlayersWithContainerOpen(pLevel, pPos);
      this.maxInteractionRange = 0.0;

      for (Player $$4 : $$3) {
         this.maxInteractionRange = Math.max($$4.blockInteractionRange(), this.maxInteractionRange);
      }

      int $$5 = $$3.size();
      int $$6 = this.openCount;
      if ($$6 != $$5) {
         boolean $$7 = $$5 != 0;
         boolean $$8 = $$6 != 0;
         if ($$7 && !$$8) {
            this.onOpen(pLevel, pPos, pState);
            pLevel.gameEvent(null, GameEvent.CONTAINER_OPEN, pPos);
         } else if (!$$7) {
            this.onClose(pLevel, pPos, pState);
            pLevel.gameEvent(null, GameEvent.CONTAINER_CLOSE, pPos);
         }

         this.openCount = $$5;
      }

      this.openerCountChanged(pLevel, pPos, pState, $$6, $$5);
      if ($$5 > 0) {
         scheduleRecheck(pLevel, pPos, pState);
      }
   }

   public int getOpenerCount() {
      return this.openCount;
   }

   private static void scheduleRecheck(Level pLevel, BlockPos pPos, BlockState pState) {
      pLevel.scheduleTick(pPos, pState.getBlock(), 5);
   }
}
