package net.minecraft.server.commands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.PlayerList;

public class BanListCommands {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("banlist")
                     .requires(p_136548_ -> p_136548_.hasPermission(3)))
                  .executes(
                     p_136555_ -> {
                        PlayerList $$1 = ((CommandSourceStack)p_136555_.getSource()).getServer().getPlayerList();
                        return showList(
                           (CommandSourceStack)p_136555_.getSource(),
                           Lists.newArrayList(Iterables.concat($$1.getBans().getEntries(), $$1.getIpBans().getEntries()))
                        );
                     }
                  ))
               .then(
                  Commands.literal("ips")
                     .executes(
                        p_136553_ -> showList(
                              (CommandSourceStack)p_136553_.getSource(),
                              ((CommandSourceStack)p_136553_.getSource()).getServer().getPlayerList().getIpBans().getEntries()
                           )
                     )
               ))
            .then(
               Commands.literal("players")
                  .executes(
                     p_136546_ -> showList(
                           (CommandSourceStack)p_136546_.getSource(),
                           ((CommandSourceStack)p_136546_.getSource()).getServer().getPlayerList().getBans().getEntries()
                        )
                  )
            )
      );
   }

   private static int showList(CommandSourceStack pSource, Collection<? extends BanListEntry<?>> pBannedPlayerList) {
      if (pBannedPlayerList.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.banlist.none"), false);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.banlist.list", pBannedPlayerList.size()), false);

         for (BanListEntry<?> $$2 : pBannedPlayerList) {
            pSource.sendSuccess(() -> Component.translatable("commands.banlist.entry", $$2.getDisplayName(), $$2.getSource(), $$2.getReason()), false);
         }
      }

      return pBannedPlayerList.size();
   }
}
