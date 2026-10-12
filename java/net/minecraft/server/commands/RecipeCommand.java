package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeCommand {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.give.failed"));
   private static final SimpleCommandExceptionType ERROR_TAKE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.recipe.take.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("recipe").requires(p_138205_ -> p_138205_.hasPermission(2)))
               .then(
                  Commands.literal("give")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                              .then(
                                 Commands.argument("recipe", ResourceLocationArgument.id())
                                    .suggests(SuggestionProviders.ALL_RECIPES)
                                    .executes(
                                       p_296531_ -> giveRecipes(
                                             (CommandSourceStack)p_296531_.getSource(),
                                             EntityArgument.getPlayers(p_296531_, "targets"),
                                             Collections.singleton(ResourceLocationArgument.getRecipe(p_296531_, "recipe"))
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("*")
                                 .executes(
                                    p_138217_ -> giveRecipes(
                                          (CommandSourceStack)p_138217_.getSource(),
                                          EntityArgument.getPlayers(p_138217_, "targets"),
                                          ((CommandSourceStack)p_138217_.getSource()).getServer().getRecipeManager().getRecipes()
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("take")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                           .then(
                              Commands.argument("recipe", ResourceLocationArgument.id())
                                 .suggests(SuggestionProviders.ALL_RECIPES)
                                 .executes(
                                    p_296532_ -> takeRecipes(
                                          (CommandSourceStack)p_296532_.getSource(),
                                          EntityArgument.getPlayers(p_296532_, "targets"),
                                          Collections.singleton(ResourceLocationArgument.getRecipe(p_296532_, "recipe"))
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("*")
                              .executes(
                                 p_138203_ -> takeRecipes(
                                       (CommandSourceStack)p_138203_.getSource(),
                                       EntityArgument.getPlayers(p_138203_, "targets"),
                                       ((CommandSourceStack)p_138203_.getSource()).getServer().getRecipeManager().getRecipes()
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int giveRecipes(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Collection<RecipeHolder<?>> pRecipes) throws CommandSyntaxException {
      int $$3 = 0;

      for (ServerPlayer $$4 : pTargets) {
         $$3 += $$4.awardRecipes(pRecipes);
      }

      if ($$3 == 0) {
         throw ERROR_GIVE_FAILED.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable("commands.recipe.give.success.single", pRecipes.size(), pTargets.iterator().next().getDisplayName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.recipe.give.success.multiple", pRecipes.size(), pTargets.size()), true);
         }

         return $$3;
      }
   }

   private static int takeRecipes(CommandSourceStack pSource, Collection<ServerPlayer> pTargets, Collection<RecipeHolder<?>> pRecipes) throws CommandSyntaxException {
      int $$3 = 0;

      for (ServerPlayer $$4 : pTargets) {
         $$3 += $$4.resetRecipes(pRecipes);
      }

      if ($$3 == 0) {
         throw ERROR_TAKE_FAILED.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable("commands.recipe.take.success.single", pRecipes.size(), pTargets.iterator().next().getDisplayName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.recipe.take.success.multiple", pRecipes.size(), pTargets.size()), true);
         }

         return $$3;
      }
   }
}
