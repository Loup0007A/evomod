package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class TeamCommand {
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.team.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.team.empty.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_NAME = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.name.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_COLOR = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.color.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.nametagVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.deathMessageVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_COLLISION_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.collisionRule.unchanged")
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                    "team"
                                 )
                                 .requires(p_183713_ -> p_183713_.hasPermission(2)))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("list").executes(p_183711_ -> listTeams((CommandSourceStack)p_183711_.getSource())))
                                    .then(
                                       Commands.argument("team", TeamArgument.team())
                                          .executes(
                                             p_138876_ -> listMembers((CommandSourceStack)p_138876_.getSource(), TeamArgument.getTeam(p_138876_, "team"))
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("add")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("team", StringArgumentType.word())
                                          .executes(
                                             p_138995_ -> createTeam((CommandSourceStack)p_138995_.getSource(), StringArgumentType.getString(p_138995_, "team"))
                                          ))
                                       .then(
                                          Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                             .executes(
                                                p_138993_ -> createTeam(
                                                      (CommandSourceStack)p_138993_.getSource(),
                                                      StringArgumentType.getString(p_138993_, "team"),
                                                      ComponentArgument.getComponent(p_138993_, "displayName")
                                                   )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("remove")
                              .then(
                                 Commands.argument("team", TeamArgument.team())
                                    .executes(p_138991_ -> deleteTeam((CommandSourceStack)p_138991_.getSource(), TeamArgument.getTeam(p_138991_, "team")))
                              )
                        ))
                     .then(
                        Commands.literal("empty")
                           .then(
                              Commands.argument("team", TeamArgument.team())
                                 .executes(p_138989_ -> emptyTeam((CommandSourceStack)p_138989_.getSource(), TeamArgument.getTeam(p_138989_, "team")))
                           )
                     ))
                  .then(
                     Commands.literal("join")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("team", TeamArgument.team())
                                 .executes(
                                    p_308898_ -> joinTeam(
                                          (CommandSourceStack)p_308898_.getSource(),
                                          TeamArgument.getTeam(p_308898_, "team"),
                                          Collections.singleton(((CommandSourceStack)p_308898_.getSource()).getEntityOrException())
                                       )
                                 ))
                              .then(
                                 Commands.argument("members", ScoreHolderArgument.scoreHolders())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .executes(
                                       p_138985_ -> joinTeam(
                                             (CommandSourceStack)p_138985_.getSource(),
                                             TeamArgument.getTeam(p_138985_, "team"),
                                             ScoreHolderArgument.getNamesWithDefaultWildcard(p_138985_, "members")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("leave")
                     .then(
                        Commands.argument("members", ScoreHolderArgument.scoreHolders())
                           .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                           .executes(
                              p_138983_ -> leaveTeam(
                                    (CommandSourceStack)p_138983_.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(p_138983_, "members")
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("modify")
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                   "team", TeamArgument.team()
                                                )
                                                .then(
                                                   Commands.literal("displayName")
                                                      .then(
                                                         Commands.argument("displayName", ComponentArgument.textComponent(pContext))
                                                            .executes(
                                                               p_138981_ -> setDisplayName(
                                                                     (CommandSourceStack)p_138981_.getSource(),
                                                                     TeamArgument.getTeam(p_138981_, "team"),
                                                                     ComponentArgument.getComponent(p_138981_, "displayName")
                                                                  )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("color")
                                                   .then(
                                                      Commands.argument("value", ColorArgument.color())
                                                         .executes(
                                                            p_138979_ -> setColor(
                                                                  (CommandSourceStack)p_138979_.getSource(),
                                                                  TeamArgument.getTeam(p_138979_, "team"),
                                                                  ColorArgument.getColor(p_138979_, "value")
                                                               )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("friendlyFire")
                                                .then(
                                                   Commands.argument("allowed", BoolArgumentType.bool())
                                                      .executes(
                                                         p_138977_ -> setFriendlyFire(
                                                               (CommandSourceStack)p_138977_.getSource(),
                                                               TeamArgument.getTeam(p_138977_, "team"),
                                                               BoolArgumentType.getBool(p_138977_, "allowed")
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("seeFriendlyInvisibles")
                                             .then(
                                                Commands.argument("allowed", BoolArgumentType.bool())
                                                   .executes(
                                                      p_138975_ -> setFriendlySight(
                                                            (CommandSourceStack)p_138975_.getSource(),
                                                            TeamArgument.getTeam(p_138975_, "team"),
                                                            BoolArgumentType.getBool(p_138975_, "allowed")
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("nametagVisibility")
                                                   .then(
                                                      Commands.literal("never")
                                                         .executes(
                                                            p_138973_ -> setNametagVisibility(
                                                                  (CommandSourceStack)p_138973_.getSource(),
                                                                  TeamArgument.getTeam(p_138973_, "team"),
                                                                  Team.Visibility.NEVER
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("hideForOtherTeams")
                                                      .executes(
                                                         p_138971_ -> setNametagVisibility(
                                                               (CommandSourceStack)p_138971_.getSource(),
                                                               TeamArgument.getTeam(p_138971_, "team"),
                                                               Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("hideForOwnTeam")
                                                   .executes(
                                                      p_138969_ -> setNametagVisibility(
                                                            (CommandSourceStack)p_138969_.getSource(),
                                                            TeamArgument.getTeam(p_138969_, "team"),
                                                            Team.Visibility.HIDE_FOR_OWN_TEAM
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("always")
                                                .executes(
                                                   p_138967_ -> setNametagVisibility(
                                                         (CommandSourceStack)p_138967_.getSource(),
                                                         TeamArgument.getTeam(p_138967_, "team"),
                                                         Team.Visibility.ALWAYS
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("deathMessageVisibility")
                                                .then(
                                                   Commands.literal("never")
                                                      .executes(
                                                         p_138965_ -> setDeathMessageVisibility(
                                                               (CommandSourceStack)p_138965_.getSource(),
                                                               TeamArgument.getTeam(p_138965_, "team"),
                                                               Team.Visibility.NEVER
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("hideForOtherTeams")
                                                   .executes(
                                                      p_138963_ -> setDeathMessageVisibility(
                                                            (CommandSourceStack)p_138963_.getSource(),
                                                            TeamArgument.getTeam(p_138963_, "team"),
                                                            Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("hideForOwnTeam")
                                                .executes(
                                                   p_138961_ -> setDeathMessageVisibility(
                                                         (CommandSourceStack)p_138961_.getSource(),
                                                         TeamArgument.getTeam(p_138961_, "team"),
                                                         Team.Visibility.HIDE_FOR_OWN_TEAM
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("always")
                                             .executes(
                                                p_138959_ -> setDeathMessageVisibility(
                                                      (CommandSourceStack)p_138959_.getSource(),
                                                      TeamArgument.getTeam(p_138959_, "team"),
                                                      Team.Visibility.ALWAYS
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("collisionRule")
                                             .then(
                                                Commands.literal("never")
                                                   .executes(
                                                      p_138957_ -> setCollision(
                                                            (CommandSourceStack)p_138957_.getSource(),
                                                            TeamArgument.getTeam(p_138957_, "team"),
                                                            Team.CollisionRule.NEVER
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("pushOwnTeam")
                                                .executes(
                                                   p_138955_ -> setCollision(
                                                         (CommandSourceStack)p_138955_.getSource(),
                                                         TeamArgument.getTeam(p_138955_, "team"),
                                                         Team.CollisionRule.PUSH_OWN_TEAM
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("pushOtherTeams")
                                             .executes(
                                                p_138953_ -> setCollision(
                                                      (CommandSourceStack)p_138953_.getSource(),
                                                      TeamArgument.getTeam(p_138953_, "team"),
                                                      Team.CollisionRule.PUSH_OTHER_TEAMS
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("always")
                                          .executes(
                                             p_138951_ -> setCollision(
                                                   (CommandSourceStack)p_138951_.getSource(),
                                                   TeamArgument.getTeam(p_138951_, "team"),
                                                   Team.CollisionRule.ALWAYS
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("prefix")
                                 .then(
                                    Commands.argument("prefix", ComponentArgument.textComponent(pContext))
                                       .executes(
                                          p_138942_ -> setPrefix(
                                                (CommandSourceStack)p_138942_.getSource(),
                                                TeamArgument.getTeam(p_138942_, "team"),
                                                ComponentArgument.getComponent(p_138942_, "prefix")
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("suffix")
                              .then(
                                 Commands.argument("suffix", ComponentArgument.textComponent(pContext))
                                    .executes(
                                       p_138923_ -> setSuffix(
                                             (CommandSourceStack)p_138923_.getSource(),
                                             TeamArgument.getTeam(p_138923_, "team"),
                                             ComponentArgument.getComponent(p_138923_, "suffix")
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static Component getFirstMemberName(Collection<ScoreHolder> pScores) {
      return pScores.iterator().next().getFeedbackDisplayName();
   }

   private static int leaveTeam(CommandSourceStack pSource, Collection<ScoreHolder> pPlayers) {
      Scoreboard $$2 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$3 : pPlayers) {
         $$2.removePlayerFromTeam($$3.getScoreboardName());
      }

      if (pPlayers.size() == 1) {
         pSource.sendSuccess(() -> Component.translatable("commands.team.leave.success.single", getFirstMemberName(pPlayers)), true);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.team.leave.success.multiple", pPlayers.size()), true);
      }

      return pPlayers.size();
   }

   private static int joinTeam(CommandSourceStack pSource, PlayerTeam pTeam, Collection<ScoreHolder> pPlayers) {
      Scoreboard $$3 = pSource.getServer().getScoreboard();

      for (ScoreHolder $$4 : pPlayers) {
         $$3.addPlayerToTeam($$4.getScoreboardName(), pTeam);
      }

      if (pPlayers.size() == 1) {
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.join.success.single", getFirstMemberName(pPlayers), pTeam.getFormattedDisplayName()), true
         );
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.team.join.success.multiple", pPlayers.size(), pTeam.getFormattedDisplayName()), true);
      }

      return pPlayers.size();
   }

   private static int setNametagVisibility(CommandSourceStack pSource, PlayerTeam pTeam, Team.Visibility pVisibility) throws CommandSyntaxException {
      if (pTeam.getNameTagVisibility() == pVisibility) {
         throw ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED.create();
      } else {
         pTeam.setNameTagVisibility(pVisibility);
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.option.nametagVisibility.success", pTeam.getFormattedDisplayName(), pVisibility.getDisplayName()), true
         );
         return 0;
      }
   }

   private static int setDeathMessageVisibility(CommandSourceStack pSource, PlayerTeam pTeam, Team.Visibility pVisibility) throws CommandSyntaxException {
      if (pTeam.getDeathMessageVisibility() == pVisibility) {
         throw ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED.create();
      } else {
         pTeam.setDeathMessageVisibility(pVisibility);
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.option.deathMessageVisibility.success", pTeam.getFormattedDisplayName(), pVisibility.getDisplayName()),
            true
         );
         return 0;
      }
   }

   private static int setCollision(CommandSourceStack pSource, PlayerTeam pTeam, Team.CollisionRule pRule) throws CommandSyntaxException {
      if (pTeam.getCollisionRule() == pRule) {
         throw ERROR_TEAM_COLLISION_UNCHANGED.create();
      } else {
         pTeam.setCollisionRule(pRule);
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.option.collisionRule.success", pTeam.getFormattedDisplayName(), pRule.getDisplayName()), true
         );
         return 0;
      }
   }

   private static int setFriendlySight(CommandSourceStack pSource, PlayerTeam pTeam, boolean pValue) throws CommandSyntaxException {
      if (pTeam.canSeeFriendlyInvisibles() == pValue) {
         if (pValue) {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED.create();
         }
      } else {
         pTeam.setSeeFriendlyInvisibles(pValue);
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.option.seeFriendlyInvisibles." + (pValue ? "enabled" : "disabled"), pTeam.getFormattedDisplayName()),
            true
         );
         return 0;
      }
   }

   private static int setFriendlyFire(CommandSourceStack pSource, PlayerTeam pTeam, boolean pValue) throws CommandSyntaxException {
      if (pTeam.isAllowFriendlyFire() == pValue) {
         if (pValue) {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED.create();
         }
      } else {
         pTeam.setAllowFriendlyFire(pValue);
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.option.friendlyfire." + (pValue ? "enabled" : "disabled"), pTeam.getFormattedDisplayName()), true
         );
         return 0;
      }
   }

   private static int setDisplayName(CommandSourceStack pSource, PlayerTeam pTeam, Component pValue) throws CommandSyntaxException {
      if (pTeam.getDisplayName().equals(pValue)) {
         throw ERROR_TEAM_ALREADY_NAME.create();
      } else {
         pTeam.setDisplayName(pValue);
         pSource.sendSuccess(() -> Component.translatable("commands.team.option.name.success", pTeam.getFormattedDisplayName()), true);
         return 0;
      }
   }

   private static int setColor(CommandSourceStack pSource, PlayerTeam pTeam, ChatFormatting pValue) throws CommandSyntaxException {
      if (pTeam.getColor() == pValue) {
         throw ERROR_TEAM_ALREADY_COLOR.create();
      } else {
         pTeam.setColor(pValue);
         pSource.sendSuccess(() -> Component.translatable("commands.team.option.color.success", pTeam.getFormattedDisplayName(), pValue.getName()), true);
         return 0;
      }
   }

   private static int emptyTeam(CommandSourceStack pSource, PlayerTeam pTeam) throws CommandSyntaxException {
      Scoreboard $$2 = pSource.getServer().getScoreboard();
      Collection<String> $$3 = Lists.newArrayList(pTeam.getPlayers());
      if ($$3.isEmpty()) {
         throw ERROR_TEAM_ALREADY_EMPTY.create();
      } else {
         for (String $$4 : $$3) {
            $$2.removePlayerFromTeam($$4, pTeam);
         }

         pSource.sendSuccess(() -> Component.translatable("commands.team.empty.success", $$3.size(), pTeam.getFormattedDisplayName()), true);
         return $$3.size();
      }
   }

   private static int deleteTeam(CommandSourceStack pSource, PlayerTeam pTeam) {
      Scoreboard $$2 = pSource.getServer().getScoreboard();
      $$2.removePlayerTeam(pTeam);
      pSource.sendSuccess(() -> Component.translatable("commands.team.remove.success", pTeam.getFormattedDisplayName()), true);
      return $$2.getPlayerTeams().size();
   }

   private static int createTeam(CommandSourceStack pSource, String pName) throws CommandSyntaxException {
      return createTeam(pSource, pName, Component.literal(pName));
   }

   private static int createTeam(CommandSourceStack pSource, String pName, Component pDisplayName) throws CommandSyntaxException {
      Scoreboard $$3 = pSource.getServer().getScoreboard();
      if ($$3.getPlayerTeam(pName) != null) {
         throw ERROR_TEAM_ALREADY_EXISTS.create();
      } else {
         PlayerTeam $$4 = $$3.addPlayerTeam(pName);
         $$4.setDisplayName(pDisplayName);
         pSource.sendSuccess(() -> Component.translatable("commands.team.add.success", $$4.getFormattedDisplayName()), true);
         return $$3.getPlayerTeams().size();
      }
   }

   private static int listMembers(CommandSourceStack pSource, PlayerTeam pTeam) {
      Collection<String> $$2 = pTeam.getPlayers();
      if ($$2.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.team.list.members.empty", pTeam.getFormattedDisplayName()), false);
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.list.members.success", pTeam.getFormattedDisplayName(), $$2.size(), ComponentUtils.formatList($$2)),
            false
         );
      }

      return $$2.size();
   }

   private static int listTeams(CommandSourceStack pSource) {
      Collection<PlayerTeam> $$1 = pSource.getServer().getScoreboard().getPlayerTeams();
      if ($$1.isEmpty()) {
         pSource.sendSuccess(() -> Component.translatable("commands.team.list.teams.empty"), false);
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.team.list.teams.success", $$1.size(), ComponentUtils.formatList($$1, PlayerTeam::getFormattedDisplayName)),
            false
         );
      }

      return $$1.size();
   }

   private static int setPrefix(CommandSourceStack pSource, PlayerTeam pTeam, Component pPrefix) {
      pTeam.setPlayerPrefix(pPrefix);
      pSource.sendSuccess(() -> Component.translatable("commands.team.option.prefix.success", pPrefix), false);
      return 1;
   }

   private static int setSuffix(CommandSourceStack pSource, PlayerTeam pTeam, Component pSuffix) {
      pTeam.setPlayerSuffix(pSuffix);
      pSource.sendSuccess(() -> Component.translatable("commands.team.option.suffix.success", pSuffix), false);
      return 1;
   }
}
