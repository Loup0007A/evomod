package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;

public class WardenSpawnTrackerCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("warden_spawn_tracker")
                  .requires(p_214778_ -> p_214778_.hasPermission(2)))
               .then(
                  Commands.literal("clear")
                     .executes(
                        p_214787_ -> resetTracker(
                              (CommandSourceStack)p_214787_.getSource(), ImmutableList.of(((CommandSourceStack)p_214787_.getSource()).getPlayerOrException())
                           )
                     )
               ))
            .then(
               Commands.literal("set")
                  .then(
                     Commands.argument("warning_level", IntegerArgumentType.integer(0, 4))
                        .executes(
                           p_214776_ -> setWarningLevel(
                                 (CommandSourceStack)p_214776_.getSource(),
                                 ImmutableList.of(((CommandSourceStack)p_214776_.getSource()).getPlayerOrException()),
                                 IntegerArgumentType.getInteger(p_214776_, "warning_level")
                              )
                        )
                  )
            )
      );
   }

   private static int setWarningLevel(CommandSourceStack pSource, Collection<? extends Player> pTargets, int pWarningLevel) {
      for (Player $$3 : pTargets) {
         $$3.getWardenSpawnTracker().ifPresent(p_248188_ -> p_248188_.setWarningLevel(pWarningLevel));
      }

      if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable("commands.warden_spawn_tracker.set.success.single", pTargets.iterator().next().getDisplayName()), true
         );
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.set.success.multiple", pTargets.size()), true);
      }

      return pTargets.size();
   }

   private static int resetTracker(CommandSourceStack pSource, Collection<? extends Player> pTargets) {
      for (Player $$2 : pTargets) {
         $$2.getWardenSpawnTracker().ifPresent(WardenSpawnTracker::reset);
      }

      if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable("commands.warden_spawn_tracker.clear.success.single", pTargets.iterator().next().getDisplayName()), true
         );
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.warden_spawn_tracker.clear.success.multiple", pTargets.size()), true);
      }

      return pTargets.size();
   }
}
