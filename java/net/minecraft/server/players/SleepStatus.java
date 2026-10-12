package net.minecraft.server.players;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class SleepStatus {
   private int activePlayers;
   private int sleepingPlayers;

   public boolean areEnoughSleeping(int pRequiredSleepPercentage) {
      return this.sleepingPlayers >= this.sleepersNeeded(pRequiredSleepPercentage);
   }

   public boolean areEnoughDeepSleeping(int pRequiredSleepPercentage, List<ServerPlayer> pSleepingPlayers) {
      int $$2 = (int)pSleepingPlayers.stream().filter(Player::isSleepingLongEnough).count();
      return $$2 >= this.sleepersNeeded(pRequiredSleepPercentage);
   }

   public int sleepersNeeded(int pRequiredSleepPercentage) {
      return Math.max(1, Mth.ceil((float)(this.activePlayers * pRequiredSleepPercentage) / 100.0F));
   }

   public void removeAllSleepers() {
      this.sleepingPlayers = 0;
   }

   public int amountSleeping() {
      return this.sleepingPlayers;
   }

   public boolean update(List<ServerPlayer> pPlayers) {
      int $$1 = this.activePlayers;
      int $$2 = this.sleepingPlayers;
      this.activePlayers = 0;
      this.sleepingPlayers = 0;

      for (ServerPlayer $$3 : pPlayers) {
         if (!$$3.isSpectator()) {
            this.activePlayers++;
            if ($$3.isSleeping()) {
               this.sleepingPlayers++;
            }
         }
      }

      return ($$2 > 0 || this.sleepingPlayers > 0) && ($$1 != this.activePlayers || $$2 != this.sleepingPlayers);
   }
}
