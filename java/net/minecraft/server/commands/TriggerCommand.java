package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TriggerCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(Component.translatable("commands.trigger.failed.unprimed"));
   private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(
      Component.translatable("commands.trigger.failed.invalid")
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("trigger")
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("objective", ObjectiveArgument.objective())
                        .suggests((p_139146_, p_139147_) -> suggestObjectives((CommandSourceStack)p_139146_.getSource(), p_139147_))
                        .executes(
                           p_308912_ -> simpleTrigger(
                                 (CommandSourceStack)p_308912_.getSource(),
                                 ((CommandSourceStack)p_308912_.getSource()).getPlayerOrException(),
                                 ObjectiveArgument.getObjective(p_308912_, "objective")
                              )
                        ))
                     .then(
                        Commands.literal("add")
                           .then(
                              Commands.argument("value", IntegerArgumentType.integer())
                                 .executes(
                                    p_308911_ -> addValue(
                                          (CommandSourceStack)p_308911_.getSource(),
                                          ((CommandSourceStack)p_308911_.getSource()).getPlayerOrException(),
                                          ObjectiveArgument.getObjective(p_308911_, "objective"),
                                          IntegerArgumentType.getInteger(p_308911_, "value")
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("set")
                        .then(
                           Commands.argument("value", IntegerArgumentType.integer())
                              .executes(
                                 p_308913_ -> setValue(
                                       (CommandSourceStack)p_308913_.getSource(),
                                       ((CommandSourceStack)p_308913_.getSource()).getPlayerOrException(),
                                       ObjectiveArgument.getObjective(p_308913_, "objective"),
                                       IntegerArgumentType.getInteger(p_308913_, "value")
                                    )
                              )
                        )
                  )
            )
      );
   }

   public static CompletableFuture<Suggestions> suggestObjectives(CommandSourceStack pSource, SuggestionsBuilder pBuilder) {
      ScoreHolder $$2 = pSource.getEntity();
      List<String> $$3 = Lists.newArrayList();
      if ($$2 != null) {
         Scoreboard $$4 = pSource.getServer().getScoreboard();

         for (Objective $$5 : $$4.getObjectives()) {
            if ($$5.getCriteria() == ObjectiveCriteria.TRIGGER) {
               ReadOnlyScoreInfo $$6 = $$4.getPlayerScoreInfo($$2, $$5);
               if ($$6 != null && !$$6.isLocked()) {
                  $$3.add($$5.getName());
               }
            }
         }
      }

      return SharedSuggestionProvider.suggest($$3, pBuilder);
   }

   private static int addValue(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective, int pValue) throws CommandSyntaxException {
      ScoreAccess $$4 = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      int $$5 = $$4.add(pValue);
      pSource.sendSuccess(() -> Component.translatable("commands.trigger.add.success", pObjective.getFormattedDisplayName(), pValue), true);
      return $$5;
   }

   private static int setValue(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective, int pValue) throws CommandSyntaxException {
      ScoreAccess $$4 = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      $$4.set(pValue);
      pSource.sendSuccess(() -> Component.translatable("commands.trigger.set.success", pObjective.getFormattedDisplayName(), pValue), true);
      return pValue;
   }

   private static int simpleTrigger(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective) throws CommandSyntaxException {
      ScoreAccess $$3 = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      int $$4 = $$3.add(1);
      pSource.sendSuccess(() -> Component.translatable("commands.trigger.simple.success", pObjective.getFormattedDisplayName()), true);
      return $$4;
   }

   private static ScoreAccess getScore(Scoreboard pScoreboard, ScoreHolder pScoreHolder, Objective pObjective) throws CommandSyntaxException {
      if (pObjective.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_INVALID_OBJECTIVE.create();
      } else {
         ReadOnlyScoreInfo $$3 = pScoreboard.getPlayerScoreInfo(pScoreHolder, pObjective);
         if ($$3 != null && !$$3.isLocked()) {
            ScoreAccess $$4 = pScoreboard.getOrCreatePlayerScore(pScoreHolder, pObjective);
            $$4.lock();
            return $$4;
         } else {
            throw ERROR_NOT_PRIMED.create();
         }
      }
   }
}
