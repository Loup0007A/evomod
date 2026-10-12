package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.StyleArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ScoreboardCommand {
   private static final SimpleCommandExceptionType ERROR_OBJECTIVE_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadyEmpty")
   );
   private static final SimpleCommandExceptionType ERROR_DISPLAY_SLOT_ALREADY_SET = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.objectives.display.alreadySet")
   );
   private static final SimpleCommandExceptionType ERROR_TRIGGER_ALREADY_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.failed")
   );
   private static final SimpleCommandExceptionType ERROR_NOT_TRIGGER = new SimpleCommandExceptionType(
      Component.translatable("commands.scoreboard.players.enable.invalid")
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_VALUE = new Dynamic2CommandExceptionType(
      (p_308842_, p_308843_) -> Component.translatableEscape("commands.scoreboard.players.get.null", p_308842_, p_308843_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("scoreboard")
                  .requires(p_138552_ -> p_138552_.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("objectives")
                                 .then(Commands.literal("list").executes(p_138585_ -> listObjectives((CommandSourceStack)p_138585_.getSource()))))
                              .then(
                                 Commands.literal("add")
                                    .then(
                                       Commands.argument("objective", StringArgumentType.word())
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("criteria", ObjectiveCriteriaArgument.criteria())
                                                   .executes(
                                                      p_138583_ -> addObjective(
                                                            (CommandSourceStack)p_138583_.getSource(),
                                                            StringArgumentType.getString(p_138583_, "objective"),
                                                            ObjectiveCriteriaArgument.getCriteria(p_138583_, "criteria"),
                                                            Component.literal(StringArgumentType.getString(p_138583_, "objective"))
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                                      .executes(
                                                         p_138581_ -> addObjective(
                                                               (CommandSourceStack)p_138581_.getSource(),
                                                               StringArgumentType.getString(p_138581_, "objective"),
                                                               ObjectiveCriteriaArgument.getCriteria(p_138581_, "criteria"),
                                                               ComponentArgument.getComponent(p_138581_, "displayName")
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("modify")
                                 .then(
                                    ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                   "objective", ObjectiveArgument.objective()
                                                )
                                                .then(
                                                   Commands.literal("displayname")
                                                      .then(
                                                         Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                                            .executes(
                                                               p_138579_ -> setDisplayName(
                                                                     (CommandSourceStack)p_138579_.getSource(),
                                                                     ObjectiveArgument.getObjective(p_138579_, "objective"),
                                                                     ComponentArgument.getComponent(p_138579_, "displayName")
                                                                  )
                                                            )
                                                      )
                                                ))
                                             .then(createRenderTypeModify()))
                                          .then(
                                             Commands.literal("displayautoupdate")
                                                .then(
                                                   Commands.argument("value", BoolArgumentType.bool())
                                                      .executes(
                                                         p_308844_ -> setDisplayAutoUpdate(
                                                               (CommandSourceStack)p_308844_.getSource(),
                                                               ObjectiveArgument.getObjective(p_308844_, "objective"),
                                                               BoolArgumentType.getBool(p_308844_, "value")
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          addNumberFormats(
                                             pContext,
                                             Commands.literal("numberformat"),
                                             (p_308837_, p_308838_) -> setObjectiveFormat(
                                                   (CommandSourceStack)p_308837_.getSource(), ObjectiveArgument.getObjective(p_308837_, "objective"), p_308838_
                                                )
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("remove")
                              .then(
                                 Commands.argument("objective", ObjectiveArgument.objective())
                                    .executes(
                                       p_138577_ -> removeObjective(
                                             (CommandSourceStack)p_138577_.getSource(), ObjectiveArgument.getObjective(p_138577_, "objective")
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("setdisplay")
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("slot", ScoreboardSlotArgument.displaySlot())
                                    .executes(
                                       p_296535_ -> clearDisplaySlot(
                                             (CommandSourceStack)p_296535_.getSource(), ScoreboardSlotArgument.getDisplaySlot(p_296535_, "slot")
                                          )
                                    ))
                                 .then(
                                    Commands.argument("objective", ObjectiveArgument.objective())
                                       .executes(
                                          p_296536_ -> setDisplaySlot(
                                                (CommandSourceStack)p_296536_.getSource(),
                                                ScoreboardSlotArgument.getDisplaySlot(p_296536_, "slot"),
                                                ObjectiveArgument.getObjective(p_296536_, "objective")
                                             )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                             "players"
                                          )
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("list")
                                                   .executes(p_138571_ -> listTrackedPlayers((CommandSourceStack)p_138571_.getSource())))
                                                .then(
                                                   Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                                      .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                      .executes(
                                                         p_308852_ -> listTrackedPlayerScores(
                                                               (CommandSourceStack)p_308852_.getSource(), ScoreHolderArgument.getName(p_308852_, "target")
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("set")
                                             .then(
                                                Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                                   .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                   .then(
                                                      Commands.argument("objective", ObjectiveArgument.objective())
                                                         .then(
                                                            Commands.argument("score", IntegerArgumentType.integer())
                                                               .executes(
                                                                  p_138567_ -> setScore(
                                                                        (CommandSourceStack)p_138567_.getSource(),
                                                                        ScoreHolderArgument.getNamesWithDefaultWildcard(p_138567_, "targets"),
                                                                        ObjectiveArgument.getWritableObjective(p_138567_, "objective"),
                                                                        IntegerArgumentType.getInteger(p_138567_, "score")
                                                                     )
                                                               )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                   Commands.argument("objective", ObjectiveArgument.objective())
                                                      .executes(
                                                         p_308815_ -> getScore(
                                                               (CommandSourceStack)p_308815_.getSource(),
                                                               ScoreHolderArgument.getName(p_308815_, "target"),
                                                               ObjectiveArgument.getObjective(p_308815_, "objective")
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("add")
                                       .then(
                                          Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                             .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                             .then(
                                                Commands.argument("objective", ObjectiveArgument.objective())
                                                   .then(
                                                      Commands.argument("score", IntegerArgumentType.integer(0))
                                                         .executes(
                                                            p_138563_ -> addScore(
                                                                  (CommandSourceStack)p_138563_.getSource(),
                                                                  ScoreHolderArgument.getNamesWithDefaultWildcard(p_138563_, "targets"),
                                                                  ObjectiveArgument.getWritableObjective(p_138563_, "objective"),
                                                                  IntegerArgumentType.getInteger(p_138563_, "score")
                                                               )
                                                         )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("remove")
                                    .then(
                                       Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .then(
                                             Commands.argument("objective", ObjectiveArgument.objective())
                                                .then(
                                                   Commands.argument("score", IntegerArgumentType.integer(0))
                                                      .executes(
                                                         p_138561_ -> removeScore(
                                                               (CommandSourceStack)p_138561_.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(p_138561_, "targets"),
                                                               ObjectiveArgument.getWritableObjective(p_138561_, "objective"),
                                                               IntegerArgumentType.getInteger(p_138561_, "score")
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("reset")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .executes(
                                             p_138559_ -> resetScores(
                                                   (CommandSourceStack)p_138559_.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(p_138559_, "targets")
                                                )
                                          ))
                                       .then(
                                          Commands.argument("objective", ObjectiveArgument.objective())
                                             .executes(
                                                p_138550_ -> resetScore(
                                                      (CommandSourceStack)p_138550_.getSource(),
                                                      ScoreHolderArgument.getNamesWithDefaultWildcard(p_138550_, "targets"),
                                                      ObjectiveArgument.getObjective(p_138550_, "objective")
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("enable")
                              .then(
                                 Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .then(
                                       Commands.argument("objective", ObjectiveArgument.objective())
                                          .suggests(
                                             (p_138473_, p_138474_) -> suggestTriggers(
                                                   (CommandSourceStack)p_138473_.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(p_138473_, "targets"),
                                                   p_138474_
                                                )
                                          )
                                          .executes(
                                             p_138537_ -> enableTrigger(
                                                   (CommandSourceStack)p_138537_.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(p_138537_, "targets"),
                                                   ObjectiveArgument.getObjective(p_138537_, "objective")
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("display")
                              .then(
                                 Commands.literal("name")
                                    .then(
                                       Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                          .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("objective", ObjectiveArgument.objective())
                                                   .then(
                                                      Commands.argument("name", ComponentArgument.textComponent(pContext))
                                                         .executes(
                                                            p_308819_ -> setScoreDisplay(
                                                                  (CommandSourceStack)p_308819_.getSource(),
                                                                  ScoreHolderArgument.getNamesWithDefaultWildcard(p_308819_, "targets"),
                                                                  ObjectiveArgument.getObjective(p_308819_, "objective"),
                                                                  ComponentArgument.getComponent(p_308819_, "name")
                                                               )
                                                         )
                                                   ))
                                                .executes(
                                                   p_308862_ -> setScoreDisplay(
                                                         (CommandSourceStack)p_308862_.getSource(),
                                                         ScoreHolderArgument.getNamesWithDefaultWildcard(p_308862_, "targets"),
                                                         ObjectiveArgument.getObjective(p_308862_, "objective"),
                                                         null
                                                      )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("numberformat")
                                 .then(
                                    Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                                       .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                       .then(
                                          addNumberFormats(
                                             pContext,
                                             Commands.argument("objective", ObjectiveArgument.objective()),
                                             (p_308863_, p_308864_) -> setScoreNumberFormat(
                                                   (CommandSourceStack)p_308863_.getSource(),
                                                   ScoreHolderArgument.getNamesWithDefaultWildcard(p_308863_, "targets"),
                                                   ObjectiveArgument.getObjective(p_308863_, "objective"),
                                                   p_308864_
                                                )
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("operation")
                        .then(
                           Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                              .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                              .then(
                                 Commands.argument("targetObjective", ObjectiveArgument.objective())
                                    .then(
                                       Commands.argument("operation", OperationArgument.operation())
                                          .then(
                                             Commands.argument("source", ScoreHolderArgument.scoreHolders())
                                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                .then(
                                                   Commands.argument("sourceObjective", ObjectiveArgument.objective())
                                                      .executes(
                                                         p_138471_ -> performOperation(
                                                               (CommandSourceStack)p_138471_.getSource(),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "targets"),
                                                               ObjectiveArgument.getWritableObjective(p_138471_, "targetObjective"),
                                                               OperationArgument.getOperation(p_138471_, "operation"),
                                                               ScoreHolderArgument.getNamesWithDefaultWildcard(p_138471_, "source"),
                                                               ObjectiveArgument.getObjective(p_138471_, "sourceObjective")
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addNumberFormats(
      CommandBuildContext pContext, ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, ScoreboardCommand.NumberFormatCommandExecutor pExecutor
   ) {
      return pArgumentBuilder.then(Commands.literal("blank").executes(p_308836_ -> pExecutor.run(p_308836_, BlankFormat.INSTANCE)))
         .then(Commands.literal("fixed").then(Commands.argument("contents", ComponentArgument.textComponent(pContext)).executes(p_308824_ -> {
            Component $$2 = ComponentArgument.getComponent(p_308824_, "contents");
            return pExecutor.run(p_308824_, new FixedFormat($$2));
         })))
         .then(Commands.literal("styled").then(Commands.argument("style", StyleArgument.style(pContext)).executes(p_308869_ -> {
            Style $$2 = StyleArgument.getStyle(p_308869_, "style");
            return pExecutor.run(p_308869_, new StyledFormat($$2));
         })))
         .executes(p_308875_ -> pExecutor.run(p_308875_, null));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRenderTypeModify() {
      LiteralArgumentBuilder<CommandSourceStack> $$0 = Commands.literal("rendertype");

      for (ObjectiveCriteria.RenderType $$1 : ObjectiveCriteria.RenderType.values()) {
         $$0.then(
            Commands.literal($$1.getId())
               .executes(p_138532_ -> setRenderType((CommandSourceStack)p_138532_.getSource(), ObjectiveArgument.getObjective(p_138532_, "objective"), $$1))
         );
      }

      return $$0;
   }

   private static CompletableFuture<Suggestions> suggestTriggers(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, SuggestionsBuilder pSuggestions) {
      List<String> $$3 = Lists.newArrayList();
      Scoreboard $$4 = pSource.getServer().getScoreboard();

      for (Objective $$5 : $$4.getObjectives()) {
         if ($$5.getCriteria() == ObjectiveCriteria.TRIGGER) {
            boolean $$6 = false;

            for (ScoreHolder $$7 : pTargets) {
               ReadOnlyScoreInfo $$8 = $$4.getPlayerScoreInfo($$7, $$5);
               if ($$8 == null || $$8.isLocked()) {
                  $$6 = true;
                  break;
               }
            }

            if ($$6) {
               $$3.add($$5.getName());
            }
         }
      }

      return SharedSuggestionProvider.suggest($$3, pSuggestions);
   }

   private static int getScore(CommandSourceStack pSource, ScoreHolder pScoreHolder, Objective pObjective) throws CommandSyntaxException {
      Scoreboard $$3 = pSource.getServer().getScoreboard();
      ReadOnlyScoreInfo $$4 = $$3.getPlayerScoreInfo(pScoreHolder, pObjective);
      if ($$4 == null) {
         throw ERROR_NO_VALUE.create(pObjective.getName(), pScoreHolder.getFeedbackDisplayName());
      } else {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.get.success", pScoreHolder.getFeedbackDisplayName(), $$4.value(), pObjective.getFormattedDisplayName()
               ),
            false
         );
         return $$4.value();
      }
   }

   private static Component getFirstTargetName(Collection<ScoreHolder> pScores) {
      return pScores.iterator().next().getFeedbackDisplayName();
   }

   private static int performOperation(
      CommandSourceStack pSource,
      Collection<ScoreHolder> pTargets,
      Objective pTargetObjectives,
      OperationArgument.Operation pOperation,
      Collection<ScoreHolder> pSourceEntities,
      Objective pSourceObjective
   ) throws CommandSyntaxException {
      Scoreboard $$6 = pSource.getServer().getScoreboard();
      int $$7 = 0;

      for (ScoreHolder $$8 : pTargets) {
         ScoreAccess $$9 = $$6.getOrCreatePlayerScore($$8, pTargetObjectives);

         for (ScoreHolder $$10 : pSourceEntities) {
            ScoreAccess $$11 = $$6.getOrCreatePlayerScore($$10, pSourceObjective);
            pOperation.apply($$9, $$11);
         }

         $$7 += $$9.get();
      }

      if (pTargets.size() == 1) {
         int $$12 = $$7;
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.operation.success.single", pTargetObjectives.getFormattedDisplayName(), getFirstTargetName(pTargets), $$12
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.operation.success.multiple", pTargetObjectives.getFormattedDisplayName(), pTargets.size()),
            true
         );
      }

      return $$7;
   }

   private static int enableTrigger(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective) throws CommandSyntaxException {
      if (pObjective.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_NOT_TRIGGER.create();
      } else {
         Scoreboard $$3 = pSource.getServer().getScoreboard();
         int $$4 = 0;

         for (ScoreHolder $$5 : pTargets) {
            ScoreAccess $$6 = $$3.getOrCreatePlayerScore($$5, pObjective);
            if ($$6.locked()) {
               $$6.unlock();
               $$4++;
            }
         }

         if ($$4 == 0) {
            throw ERROR_TRIGGER_ALREADY_ENABLED.create();
         } else {
            if (pTargets.size() == 1) {
               pSource.sendSuccess(
                  () -> Component.translatable(
                        "commands.scoreboard.players.enable.success.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets)
                     ),
                  true
               );
            } else {
               pSource.sendSuccess(
                  () -> Component.translatable("commands.scoreboard.players.enable.success.multiple", pObjective.getFormattedDisplayName(), pTargets.size()),
                  true
               );
            }

            return $$4;
         }
      }
   }

   private static int resetScores(CommandSourceStack pSource, Collection<ScoreHolder> pTargets) {
      Scoreboard $$2 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$3 : pTargets) {
         $$2.resetAllPlayerScores($$3);
      }

      if (pTargets.size() == 1) {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.single", getFirstTargetName(pTargets)), true);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.reset.all.multiple", pTargets.size()), true);
      }

      return pTargets.size();
   }

   private static int resetScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective) {
      Scoreboard $$3 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$4 : pTargets) {
         $$3.resetSinglePlayerScore($$4, pObjective);
      }

      if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.reset.specific.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets)
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.reset.specific.multiple", pObjective.getFormattedDisplayName(), pTargets.size()), true
         );
      }

      return pTargets.size();
   }

   private static int setScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pNewValue) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$5 : pTargets) {
         $$4.getOrCreatePlayerScore($$5, pObjective).set(pNewValue);
      }

      if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.set.success.single", pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), pNewValue
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.set.success.multiple", pObjective.getFormattedDisplayName(), pTargets.size(), pNewValue),
            true
         );
      }

      return pNewValue * pTargets.size();
   }

   private static int setScoreDisplay(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, @Nullable Component pDisplayName) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$5 : pTargets) {
         $$4.getOrCreatePlayerScore($$5, pObjective).display(pDisplayName);
      }

      if (pDisplayName == null) {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.players.display.name.clear.success.single", getFirstTargetName(pTargets), pObjective.getFormattedDisplayName()
                  ),
               true
            );
         } else {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.players.display.name.clear.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
                  ),
               true
            );
         }
      } else if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.display.name.set.success.single",
                  pDisplayName,
                  getFirstTargetName(pTargets),
                  pObjective.getFormattedDisplayName()
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.display.name.set.success.multiple", pDisplayName, pTargets.size(), pObjective.getFormattedDisplayName()
               ),
            true
         );
      }

      return pTargets.size();
   }

   private static int setScoreNumberFormat(
      CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, @Nullable NumberFormat pNumberFormat
   ) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$5 : pTargets) {
         $$4.getOrCreatePlayerScore($$5, pObjective).numberFormatOverride(pNumberFormat);
      }

      if (pNumberFormat == null) {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.players.display.numberFormat.clear.success.single",
                     getFirstTargetName(pTargets),
                     pObjective.getFormattedDisplayName()
                  ),
               true
            );
         } else {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.players.display.numberFormat.clear.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
                  ),
               true
            );
         }
      } else if (pTargets.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.display.numberFormat.set.success.single", getFirstTargetName(pTargets), pObjective.getFormattedDisplayName()
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.display.numberFormat.set.success.multiple", pTargets.size(), pObjective.getFormattedDisplayName()
               ),
            true
         );
      }

      return pTargets.size();
   }

   private static int addScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pAmount) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();
      int $$5 = 0;

      for (ScoreHolder $$6 : pTargets) {
         ScoreAccess $$7 = $$4.getOrCreatePlayerScore($$6, pObjective);
         $$7.set($$7.get() + pAmount);
         $$5 += $$7.get();
      }

      if (pTargets.size() == 1) {
         int $$8 = $$5;
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.add.success.single", pAmount, pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), $$8
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.add.success.multiple", pAmount, pObjective.getFormattedDisplayName(), pTargets.size()),
            true
         );
      }

      return $$5;
   }

   private static int removeScore(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, int pAmount) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();
      int $$5 = 0;

      for (ScoreHolder $$6 : pTargets) {
         ScoreAccess $$7 = $$4.getOrCreatePlayerScore($$6, pObjective);
         $$7.set($$7.get() - pAmount);
         $$5 += $$7.get();
      }

      if (pTargets.size() == 1) {
         int $$8 = $$5;
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.remove.success.single", pAmount, pObjective.getFormattedDisplayName(), getFirstTargetName(pTargets), $$8
               ),
            true
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.remove.success.multiple", pAmount, pObjective.getFormattedDisplayName(), pTargets.size()),
            true
         );
      }

      return $$5;
   }

   private static int listTrackedPlayers(CommandSourceStack pSource) {
      Collection<ScoreHolder> $$1 = pSource.getServer().getScoreboard().getTrackedPlayers();
      if ($$1.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.empty"), false);
      } else {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.players.list.success", $$1.size(), ComponentUtils.formatList($$1, ScoreHolder::getFeedbackDisplayName)
               ),
            false
         );
      }

      return $$1.size();
   }

   private static int listTrackedPlayerScores(CommandSourceStack pSource, ScoreHolder pScore) {
      Object2IntMap<Objective> $$2 = pSource.getServer().getScoreboard().listPlayerScores(pScore);
      if ($$2.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.players.list.entity.empty", pScore.getFeedbackDisplayName()), false);
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.players.list.entity.success", pScore.getFeedbackDisplayName(), $$2.size()), false
         );
         Object2IntMaps.fastForEach(
            $$2,
            p_308821_ -> pSource.sendSuccess(
                  () -> Component.translatable(
                        "commands.scoreboard.players.list.entity.entry", ((Objective)p_308821_.getKey()).getFormattedDisplayName(), p_308821_.getIntValue()
                     ),
                  false
               )
         );
      }

      return $$2.size();
   }

   private static int clearDisplaySlot(CommandSourceStack pSource, DisplaySlot pSlot) throws CommandSyntaxException {
      Scoreboard $$2 = pSource.getServer().getScoreboard();
      if ($$2.getDisplayObjective(pSlot) == null) {
         throw ERROR_DISPLAY_SLOT_ALREADY_EMPTY.create();
      } else {
         $$2.setDisplayObjective(pSlot, null);
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.display.cleared", pSlot.getSerializedName()), true);
         return 0;
      }
   }

   private static int setDisplaySlot(CommandSourceStack pSource, DisplaySlot pSlot, Objective pObjective) throws CommandSyntaxException {
      Scoreboard $$3 = pSource.getServer().getScoreboard();
      if ($$3.getDisplayObjective(pSlot) == pObjective) {
         throw ERROR_DISPLAY_SLOT_ALREADY_SET.create();
      } else {
         $$3.setDisplayObjective(pSlot, pObjective);
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.objectives.display.set", pSlot.getSerializedName(), pObjective.getDisplayName()), true
         );
         return 0;
      }
   }

   private static int setDisplayName(CommandSourceStack pSource, Objective pObjective, Component pDisplayName) {
      if (!pObjective.getDisplayName().equals(pDisplayName)) {
         pObjective.setDisplayName(pDisplayName);
         pSource.sendSuccess(
            () -> Component.translatable("commands.scoreboard.objectives.modify.displayname", pObjective.getName(), pObjective.getFormattedDisplayName()), true
         );
      }

      return 0;
   }

   private static int setDisplayAutoUpdate(CommandSourceStack pSource, Objective pObjective, boolean pDisplayAutoUpdate) {
      if (pObjective.displayAutoUpdate() != pDisplayAutoUpdate) {
         pObjective.setDisplayAutoUpdate(pDisplayAutoUpdate);
         if (pDisplayAutoUpdate) {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.objectives.modify.displayAutoUpdate.enable", pObjective.getName(), pObjective.getFormattedDisplayName()
                  ),
               true
            );
         } else {
            pSource.sendSuccess(
               () -> Component.translatable(
                     "commands.scoreboard.objectives.modify.displayAutoUpdate.disable", pObjective.getName(), pObjective.getFormattedDisplayName()
                  ),
               true
            );
         }
      }

      return 0;
   }

   private static int setObjectiveFormat(CommandSourceStack pSource, Objective pObjective, @Nullable NumberFormat pFormat) {
      pObjective.setNumberFormat(pFormat);
      if (pFormat != null) {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.set", pObjective.getName()), true);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.objectiveFormat.clear", pObjective.getName()), true);
      }

      return 0;
   }

   private static int setRenderType(CommandSourceStack pSource, Objective pObjective, ObjectiveCriteria.RenderType pRenderType) {
      if (pObjective.getRenderType() != pRenderType) {
         pObjective.setRenderType(pRenderType);
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.modify.rendertype", pObjective.getFormattedDisplayName()), true);
      }

      return 0;
   }

   private static int removeObjective(CommandSourceStack pSource, Objective pObjective) {
      Scoreboard $$2 = pSource.getServer().getScoreboard();
      $$2.removeObjective(pObjective);
      pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.remove.success", pObjective.getFormattedDisplayName()), true);
      return $$2.getObjectives().size();
   }

   private static int addObjective(CommandSourceStack pSource, String pName, ObjectiveCriteria pCriteria, Component pDisplayName) throws CommandSyntaxException {
      Scoreboard $$4 = pSource.getServer().getScoreboard();
      if ($$4.getObjective(pName) != null) {
         throw ERROR_OBJECTIVE_ALREADY_EXISTS.create();
      } else {
         $$4.addObjective(pName, pCriteria, pDisplayName, pCriteria.getDefaultRenderType(), false, null);
         Objective $$5 = $$4.getObjective(pName);
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.add.success", $$5.getFormattedDisplayName()), true);
         return $$4.getObjectives().size();
      }
   }

   private static int listObjectives(CommandSourceStack pSource) {
      Collection<Objective> $$1 = pSource.getServer().getScoreboard().getObjectives();
      if ($$1.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.scoreboard.objectives.list.empty"), false);
      } else {
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.scoreboard.objectives.list.success", $$1.size(), ComponentUtils.formatList($$1, Objective::getFormattedDisplayName)
               ),
            false
         );
      }

      return $$1.size();
   }

   @FunctionalInterface
   public interface NumberFormatCommandExecutor {
      int run(CommandContext<CommandSourceStack> pContext, @Nullable NumberFormat pFormat) throws CommandSyntaxException;
   }
}
