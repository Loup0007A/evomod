package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SetWorldSpawnCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setworldspawn")
                  .requires(p_138665_ -> p_138665_.hasPermission(2)))
               .executes(
                  p_274830_ -> setSpawn(
                        (CommandSourceStack)p_274830_.getSource(), BlockPos.containing(((CommandSourceStack)p_274830_.getSource()).getPosition()), 0.0F
                     )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                     .executes(p_138671_ -> setSpawn((CommandSourceStack)p_138671_.getSource(), BlockPosArgument.getSpawnablePos(p_138671_, "pos"), 0.0F)))
                  .then(
                     Commands.argument("angle", AngleArgument.angle())
                        .executes(
                           p_138663_ -> setSpawn(
                                 (CommandSourceStack)p_138663_.getSource(),
                                 BlockPosArgument.getSpawnablePos(p_138663_, "pos"),
                                 AngleArgument.getAngle(p_138663_, "angle")
                              )
                        )
                  )
            )
      );
   }

   private static int setSpawn(CommandSourceStack pSource, BlockPos pPos, float pAngle) {
      ServerLevel $$3 = pSource.getLevel();
      if ($$3.dimension() != Level.OVERWORLD) {
         pSource.sendFailure(Component.translatable("commands.setworldspawn.failure.not_overworld"));
         return 0;
      } else {
         $$3.setDefaultSpawnPos(pPos, pAngle);
         pSource.sendSuccess(() -> Component.translatable("commands.setworldspawn.success", pPos.getX(), pPos.getY(), pPos.getZ(), pAngle), true);
         return 1;
      }
   }
}
