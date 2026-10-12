package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

public class CelebrateVillagersSurvivedRaid extends Behavior<Villager> {
   @Nullable
   private Raid currentRaid;

   public CelebrateVillagersSurvivedRaid(int pMinDuration, int pMaxDuration) {
      super(ImmutableMap.of(), pMinDuration, pMaxDuration);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      BlockPos $$2 = pOwner.blockPosition();
      this.currentRaid = pLevel.getRaidAt($$2);
      return this.currentRaid != null && this.currentRaid.isVictory() && MoveToSkySeeingSpot.hasNoBlocksAbove(pLevel, pOwner, $$2);
   }

   protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return this.currentRaid != null && !this.currentRaid.isStopped();
   }

   protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      this.currentRaid = null;
      pEntity.getBrain().updateActivityFromSchedule(pLevel.getDayTime(), pLevel.getGameTime());
   }

   protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      RandomSource $$3 = pOwner.getRandom();
      if ($$3.nextInt(100) == 0) {
         pOwner.playCelebrateSound();
      }

      if ($$3.nextInt(200) == 0 && MoveToSkySeeingSpot.hasNoBlocksAbove(pLevel, pOwner, pOwner.blockPosition())) {
         DyeColor $$4 = Util.getRandom(DyeColor.values(), $$3);
         int $$5 = $$3.nextInt(3);
         ItemStack $$6 = this.getFirework($$4, $$5);
         FireworkRocketEntity $$7 = new FireworkRocketEntity(pOwner.level(), pOwner, pOwner.getX(), pOwner.getEyeY(), pOwner.getZ(), $$6);
         pOwner.level().addFreshEntity($$7);
      }
   }

   private ItemStack getFirework(DyeColor pColor, int pFlightTime) {
      ItemStack $$2 = new ItemStack(Items.FIREWORK_ROCKET);
      $$2.set(
         DataComponents.FIREWORKS,
         new Fireworks(
            (byte)pFlightTime, List.of(new FireworkExplosion(FireworkExplosion.Shape.BURST, IntList.of(pColor.getFireworkColor()), IntList.of(), false, false))
         )
      );
      return $$2;
   }
}
