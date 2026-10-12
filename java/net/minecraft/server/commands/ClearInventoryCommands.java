package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
   private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(
      p_308637_ -> Component.translatableEscape("clear.failed.single", p_308637_)
   );
   private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(
      p_308634_ -> Component.translatableEscape("clear.failed.multiple", p_308634_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clear").requires(p_136704_ -> p_136704_.hasPermission(2)))
               .executes(
                  p_326228_ -> clearUnlimited(
                        (CommandSourceStack)p_326228_.getSource(),
                        Collections.singleton(((CommandSourceStack)p_326228_.getSource()).getPlayerOrException()),
                        p_180029_ -> true
                     )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                     .executes(
                        p_326232_ -> clearUnlimited(
                              (CommandSourceStack)p_326232_.getSource(), EntityArgument.getPlayers(p_326232_, "targets"), p_180027_ -> true
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("item", ItemPredicateArgument.itemPredicate(pContext))
                           .executes(
                              p_326233_ -> clearUnlimited(
                                    (CommandSourceStack)p_326233_.getSource(),
                                    EntityArgument.getPlayers(p_326233_, "targets"),
                                    ItemPredicateArgument.getItemPredicate(p_326233_, "item")
                                 )
                           ))
                        .then(
                           Commands.argument("maxCount", IntegerArgumentType.integer(0))
                              .executes(
                                 p_326231_ -> clearInventory(
                                       (CommandSourceStack)p_326231_.getSource(),
                                       EntityArgument.getPlayers(p_326231_, "targets"),
                                       ItemPredicateArgument.getItemPredicate(p_326231_, "item"),
                                       IntegerArgumentType.getInteger(p_326231_, "maxCount")
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int clearUnlimited(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Predicate<ItemStack> pFilter) throws CommandSyntaxException {
      return clearInventory(pSource, pTargets, pFilter, -1);
   }

   private static int clearInventory(CommandSourceStack pSource, Collection<ServerPlayer> pTargetPlayers, Predicate<ItemStack> pItemPredicate, int pMaxCount) throws CommandSyntaxException {
      int $$4 = 0;

      for (ServerPlayer $$5 : pTargetPlayers) {
         $$4 += $$5.getInventory().clearOrCountMatchingItems(pItemPredicate, pMaxCount, $$5.inventoryMenu.getCraftSlots());
         $$5.containerMenu.broadcastChanges();
         $$5.inventoryMenu.slotsChanged($$5.getInventory());
      }

      if ($$4 == 0) {
         if (pTargetPlayers.size() == 1) {
            throw ERROR_SINGLE.create(pTargetPlayers.iterator().next().getName());
         } else {
            throw ERROR_MULTIPLE.create(pTargetPlayers.size());
         }
      } else {
         int $$6 = $$4;
         if (pMaxCount == 0) {
            if (pTargetPlayers.size() == 1) {
               pSource.sendSuccess(() -> Component.translatable("commands.clear.test.single", $$6, pTargetPlayers.iterator().next().getDisplayName()), true);
            } else {
               pSource.sendSuccess(() -> Component.translatable("commands.clear.test.multiple", $$6, pTargetPlayers.size()), true);
            }
         } else if (pTargetPlayers.size() == 1) {
            pSource.sendSuccess(() -> Component.translatable("commands.clear.success.single", $$6, pTargetPlayers.iterator().next().getDisplayName()), true);
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.clear.success.multiple", $$6, pTargetPlayers.size()), true);
         }

         return $$4;
      }
   }
}
