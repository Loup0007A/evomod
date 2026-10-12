package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SetSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spawnpoint")
                  .requires(p_138648_ -> p_138648_.hasPermission(2)))
               .executes(
                  p_274828_ -> setSpawn(
                        (CommandSourceStack)p_274828_.getSource(),
                        Collections.singleton(((CommandSourceStack)p_274828_.getSource()).getPlayerOrException()),
                        BlockPos.containing(((CommandSourceStack)p_274828_.getSource()).getPosition()),
                        0.0F
                     )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        p_274829_ -> setSpawn(
                              (CommandSourceStack)p_274829_.getSource(),
                              EntityArgument.getPlayers(p_274829_, "targets"),
                              BlockPos.containing(((CommandSourceStack)p_274829_.getSource()).getPosition()),
                              0.0F
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(
                              p_138655_ -> setSpawn(
                                    (CommandSourceStack)p_138655_.getSource(),
                                    EntityArgument.getPlayers(p_138655_, "targets"),
                                    BlockPosArgument.getSpawnablePos(p_138655_, "pos"),
                                    0.0F
                                 )
                           ))
                        .then(
                           Commands.argument("angle", AngleArgument.angle())
                              .executes(
                                 p_138646_ -> setSpawn(
                                       (CommandSourceStack)p_138646_.getSource(),
                                       EntityArgument.getPlayers(p_138646_, "targets"),
                                       BlockPosArgument.getSpawnablePos(p_138646_, "pos"),
                                       AngleArgument.getAngle(p_138646_, "angle")
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, BlockPos pPos, float pAngle) {
      ResourceKey<Level> $$4 = pSource.getLevel().dimension();

      for (ServerPlayer $$5 : pTargets) {
         $$5.setRespawnPosition($$4, pPos, pAngle, true, false);
      }

      String $$6 = $$4.location().toString();
      if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.spawnpoint.success.single", pPos.getX(), pPos.getY(), pPos.getZ(), pAngle, $$6, pTargets.iterator().next().getDisplayName()
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.spawnpoint.success.multiple", pPos.getX(), pPos.getY(), pPos.getZ(), pAngle, $$6, pTargets.size()), true
         );
      }

      return pTargets.size();
   }
}
