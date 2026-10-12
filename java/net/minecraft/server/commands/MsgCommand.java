package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class MsgCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralCommandNode<CommandSourceStack> $$1 = pDispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("msg")
            .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes(p_248155_ -> {
               Collection<ServerPlayer> $$1x = EntityArgument.getPlayers(p_248155_, "targets");
               if (!$$1x.isEmpty()) {
                  MessageArgument.resolveChatMessage(p_248155_, "message", p_248154_ -> sendMessage((CommandSourceStack)p_248155_.getSource(), $$1x, p_248154_));
               }

               return $$1x.size();
            })))
      );
      pDispatcher.register((LiteralArgumentBuilder)Commands.literal("tell").redirect($$1));
      pDispatcher.register((LiteralArgumentBuilder)Commands.literal("w").redirect($$1));
   }

   private static void sendMessage(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, PlayerChatMessage pMessage) {
      ChatType.Bound $$3 = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, pSource);
      OutgoingChatMessage $$4 = OutgoingChatMessage.create(pMessage);
      boolean $$5 = false;

      for (ServerPlayer $$6 : pTargets) {
         ChatType.Bound $$7 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, pSource).withTargetName($$6.getDisplayName());
         pSource.sendChatMessage($$4, false, $$7);
         boolean $$8 = pSource.shouldFilterMessageTo($$6);
         $$6.sendChatMessage($$4, $$8, $$3);
         $$5 |= $$8 && pMessage.isFullyFiltered();
      }

      if ($$5) {
         pSource.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
      }
   }
}
