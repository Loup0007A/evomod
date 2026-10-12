package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

public class ListPlayersCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list")
               .executes(p_137830_ -> listPlayers((CommandSourceStack)p_137830_.getSource())))
            .then(Commands.literal("uuids").executes(p_137823_ -> listPlayersWithUuids((CommandSourceStack)p_137823_.getSource())))
      );
   }

   private static int listPlayers(CommandSourceStack pSource) {
      return format(pSource, Player::getDisplayName);
   }

   private static int listPlayersWithUuids(CommandSourceStack pSource) {
      return format(
         pSource,
         p_341180_ -> Component.translatable("commands.list.nameAndId", p_341180_.getName(), Component.translationArg(p_341180_.getGameProfile().getId()))
      );
   }

   private static int format(CommandSourceStack pSource, Function<ServerPlayer, Component> pNameExtractor) {
      PlayerList $$2 = pSource.getServer().getPlayerList();
      List<ServerPlayer> $$3 = $$2.getPlayers();
      Component $$4 = ComponentUtils.formatList($$3, pNameExtractor);
      pSource.sendSuccess(() -> Component.translatable("commands.list.players", $$3.size(), $$2.getMaxPlayers(), $$4), false);
      return $$3.size();
   }
}
