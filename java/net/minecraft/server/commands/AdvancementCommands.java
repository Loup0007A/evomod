package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
   private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(p_308608_ -> (Component)p_308608_);
   private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
      (p_341132_, p_341133_) -> Component.translatableEscape("commands.advancement.criterionNotFound", p_341132_, p_341133_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_ADVANCEMENTS = (p_136344_, p_136345_) -> {
      Collection<AdvancementHolder> $$2 = ((CommandSourceStack)p_136344_.getSource()).getServer().getAdvancements().getAllAdvancements();
      return SharedSuggestionProvider.suggestResource($$2.stream().map(AdvancementHolder::id), p_136345_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("advancement")
                  .requires(p_136318_ -> p_136318_.hasPermission(2)))
               .then(
                  Commands.literal("grant")
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                          "targets", EntityArgument.players()
                                       )
                                       .then(
                                          Commands.literal("only")
                                             .then(
                                                ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceLocationArgument.id())
                                                      .suggests(SUGGEST_ADVANCEMENTS)
                                                      .executes(
                                                         p_296473_ -> perform(
                                                               (CommandSourceStack)p_296473_.getSource(),
                                                               EntityArgument.getPlayers(p_296473_, "targets"),
                                                               AdvancementCommands.Action.GRANT,
                                                               getAdvancements(
                                                                  p_296473_,
                                                                  ResourceLocationArgument.getAdvancement(p_296473_, "advancement"),
                                                                  AdvancementCommands.Mode.ONLY
                                                               )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.argument("criterion", StringArgumentType.greedyString())
                                                         .suggests(
                                                            (p_296457_, p_296458_) -> SharedSuggestionProvider.suggest(
                                                                  ResourceLocationArgument.getAdvancement(p_296457_, "advancement").value().criteria().keySet(),
                                                                  p_296458_
                                                               )
                                                         )
                                                         .executes(
                                                            p_296456_ -> performCriterion(
                                                                  (CommandSourceStack)p_296456_.getSource(),
                                                                  EntityArgument.getPlayers(p_296456_, "targets"),
                                                                  AdvancementCommands.Action.GRANT,
                                                                  ResourceLocationArgument.getAdvancement(p_296456_, "advancement"),
                                                                  StringArgumentType.getString(p_296456_, "criterion")
                                                               )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("from")
                                          .then(
                                             Commands.argument("advancement", ResourceLocationArgument.id())
                                                .suggests(SUGGEST_ADVANCEMENTS)
                                                .executes(
                                                   p_296455_ -> perform(
                                                         (CommandSourceStack)p_296455_.getSource(),
                                                         EntityArgument.getPlayers(p_296455_, "targets"),
                                                         AdvancementCommands.Action.GRANT,
                                                         getAdvancements(
                                                            p_296455_,
                                                            ResourceLocationArgument.getAdvancement(p_296455_, "advancement"),
                                                            AdvancementCommands.Mode.FROM
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("until")
                                       .then(
                                          Commands.argument("advancement", ResourceLocationArgument.id())
                                             .suggests(SUGGEST_ADVANCEMENTS)
                                             .executes(
                                                p_296472_ -> perform(
                                                      (CommandSourceStack)p_296472_.getSource(),
                                                      EntityArgument.getPlayers(p_296472_, "targets"),
                                                      AdvancementCommands.Action.GRANT,
                                                      getAdvancements(
                                                         p_296472_,
                                                         ResourceLocationArgument.getAdvancement(p_296472_, "advancement"),
                                                         AdvancementCommands.Mode.UNTIL
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("through")
                                    .then(
                                       Commands.argument("advancement", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_ADVANCEMENTS)
                                          .executes(
                                             p_296459_ -> perform(
                                                   (CommandSourceStack)p_296459_.getSource(),
                                                   EntityArgument.getPlayers(p_296459_, "targets"),
                                                   AdvancementCommands.Action.GRANT,
                                                   getAdvancements(
                                                      p_296459_,
                                                      ResourceLocationArgument.getAdvancement(p_296459_, "advancement"),
                                                      AdvancementCommands.Mode.THROUGH
                                                   )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("everything")
                                 .executes(
                                    p_136353_ -> perform(
                                          (CommandSourceStack)p_136353_.getSource(),
                                          EntityArgument.getPlayers(p_136353_, "targets"),
                                          AdvancementCommands.Action.GRANT,
                                          ((CommandSourceStack)p_136353_.getSource()).getServer().getAdvancements().getAllAdvancements()
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("revoke")
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "targets", EntityArgument.players()
                                    )
                                    .then(
                                       Commands.literal("only")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("advancement", ResourceLocationArgument.id())
                                                   .suggests(SUGGEST_ADVANCEMENTS)
                                                   .executes(
                                                      p_296460_ -> perform(
                                                            (CommandSourceStack)p_296460_.getSource(),
                                                            EntityArgument.getPlayers(p_296460_, "targets"),
                                                            AdvancementCommands.Action.REVOKE,
                                                            getAdvancements(
                                                               p_296460_,
                                                               ResourceLocationArgument.getAdvancement(p_296460_, "advancement"),
                                                               AdvancementCommands.Mode.ONLY
                                                            )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("criterion", StringArgumentType.greedyString())
                                                      .suggests(
                                                         (p_296480_, p_296481_) -> SharedSuggestionProvider.suggest(
                                                               ResourceLocationArgument.getAdvancement(p_296480_, "advancement").value().criteria().keySet(),
                                                               p_296481_
                                                            )
                                                      )
                                                      .executes(
                                                         p_296468_ -> performCriterion(
                                                               (CommandSourceStack)p_296468_.getSource(),
                                                               EntityArgument.getPlayers(p_296468_, "targets"),
                                                               AdvancementCommands.Action.REVOKE,
                                                               ResourceLocationArgument.getAdvancement(p_296468_, "advancement"),
                                                               StringArgumentType.getString(p_296468_, "criterion")
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("from")
                                       .then(
                                          Commands.argument("advancement", ResourceLocationArgument.id())
                                             .suggests(SUGGEST_ADVANCEMENTS)
                                             .executes(
                                                p_296474_ -> perform(
                                                      (CommandSourceStack)p_296474_.getSource(),
                                                      EntityArgument.getPlayers(p_296474_, "targets"),
                                                      AdvancementCommands.Action.REVOKE,
                                                      getAdvancements(
                                                         p_296474_,
                                                         ResourceLocationArgument.getAdvancement(p_296474_, "advancement"),
                                                         AdvancementCommands.Mode.FROM
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("until")
                                    .then(
                                       Commands.argument("advancement", ResourceLocationArgument.id())
                                          .suggests(SUGGEST_ADVANCEMENTS)
                                          .executes(
                                             p_296475_ -> perform(
                                                   (CommandSourceStack)p_296475_.getSource(),
                                                   EntityArgument.getPlayers(p_296475_, "targets"),
                                                   AdvancementCommands.Action.REVOKE,
                                                   getAdvancements(
                                                      p_296475_,
                                                      ResourceLocationArgument.getAdvancement(p_296475_, "advancement"),
                                                      AdvancementCommands.Mode.UNTIL
                                                   )
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("through")
                                 .then(
                                    Commands.argument("advancement", ResourceLocationArgument.id())
                                       .suggests(SUGGEST_ADVANCEMENTS)
                                       .executes(
                                          p_296451_ -> perform(
                                                (CommandSourceStack)p_296451_.getSource(),
                                                EntityArgument.getPlayers(p_296451_, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                getAdvancements(
                                                   p_296451_,
                                                   ResourceLocationArgument.getAdvancement(p_296451_, "advancement"),
                                                   AdvancementCommands.Mode.THROUGH
                                                )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("everything")
                              .executes(
                                 p_136313_ -> perform(
                                       (CommandSourceStack)p_136313_.getSource(),
                                       EntityArgument.getPlayers(p_136313_, "targets"),
                                       AdvancementCommands.Action.REVOKE,
                                       ((CommandSourceStack)p_136313_.getSource()).getServer().getAdvancements().getAllAdvancements()
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int perform(
      CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, Collection<AdvancementHolder> pAdvancements
   ) throws CommandSyntaxException {
      int $$4 = 0;

      for (ServerPlayer $$5 : pTargets) {
         $$4 += pAction.perform($$5, pAdvancements);
      }

      if ($$4 == 0) {
         if (pAdvancements.size() == 1) {
            if (pTargets.size() == 1) {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(
                     pAction.getKey() + ".one.to.one.failure", Advancement.name(pAdvancements.iterator().next()), pTargets.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(pAction.getKey() + ".one.to.many.failure", Advancement.name(pAdvancements.iterator().next()), pTargets.size())
               );
            }
         } else if (pTargets.size() == 1) {
            throw ERROR_NO_ACTION_PERFORMED.create(
               Component.translatable(pAction.getKey() + ".many.to.one.failure", pAdvancements.size(), pTargets.iterator().next().getDisplayName())
            );
         } else {
            throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(pAction.getKey() + ".many.to.many.failure", pAdvancements.size(), pTargets.size()));
         }
      } else {
         if (pAdvancements.size() == 1) {
            if (pTargets.size() == 1) {
               pSource.sendSuccess(
                  () -> Component.translatable(
                        pAction.getKey() + ".one.to.one.success",
                        Advancement.name(pAdvancements.iterator().next()),
                        pTargets.iterator().next().getDisplayName()
                     ),
                  true
               );
            } else {
               pSource.sendSuccess(
                  () -> Component.translatable(pAction.getKey() + ".one.to.many.success", Advancement.name(pAdvancements.iterator().next()), pTargets.size()),
                  true
               );
            }
         } else if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable(pAction.getKey() + ".many.to.one.success", pAdvancements.size(), pTargets.iterator().next().getDisplayName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable(pAction.getKey() + ".many.to.many.success", pAdvancements.size(), pTargets.size()), true);
         }

         return $$4;
      }
   }

   private static int performCriterion(
      CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, AdvancementHolder pAdvancement, String pCriterionName
   ) throws CommandSyntaxException {
      int $$5 = 0;
      Advancement $$6 = pAdvancement.value();
      if (!$$6.criteria().containsKey(pCriterionName)) {
         throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(pAdvancement), pCriterionName);
      } else {
         for (ServerPlayer $$7 : pTargets) {
            if (pAction.performCriterion($$7, pAdvancement, pCriterionName)) {
               $$5++;
            }
         }

         if ($$5 == 0) {
            if (pTargets.size() == 1) {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(
                     pAction.getKey() + ".criterion.to.one.failure",
                     pCriterionName,
                     Advancement.name(pAdvancement),
                     pTargets.iterator().next().getDisplayName()
                  )
               );
            } else {
               throw ERROR_NO_ACTION_PERFORMED.create(
                  Component.translatable(pAction.getKey() + ".criterion.to.many.failure", pCriterionName, Advancement.name(pAdvancement), pTargets.size())
               );
            }
         } else {
            if (pTargets.size() == 1) {
               pSource.sendSuccess(
                  () -> Component.translatable(
                        pAction.getKey() + ".criterion.to.one.success",
                        pCriterionName,
                        Advancement.name(pAdvancement),
                        pTargets.iterator().next().getDisplayName()
                     ),
                  true
               );
            } else {
               pSource.sendSuccess(
                  () -> Component.translatable(pAction.getKey() + ".criterion.to.many.success", pCriterionName, Advancement.name(pAdvancement), pTargets.size()),
                  true
               );
            }

            return $$5;
         }
      }
   }

   private static List<AdvancementHolder> getAdvancements(
      CommandContext<CommandSourceStack> pContext, AdvancementHolder pAdvancement, AdvancementCommands.Mode pMode
   ) {
      AdvancementTree $$3 = ((CommandSourceStack)pContext.getSource()).getServer().getAdvancements().tree();
      AdvancementNode $$4 = $$3.get(pAdvancement);
      if ($$4 == null) {
         return List.of(pAdvancement);
      } else {
         List<AdvancementHolder> $$5 = new ArrayList<>();
         if (pMode.parents) {
            for (AdvancementNode $$6 = $$4.parent(); $$6 != null; $$6 = $$6.parent()) {
               $$5.add($$6.holder());
            }
         }

         $$5.add(pAdvancement);
         if (pMode.children) {
            addChildren($$4, $$5);
         }

         return $$5;
      }
   }

   private static void addChildren(AdvancementNode pNode, List<AdvancementHolder> pOutput) {
      for (AdvancementNode $$2 : pNode.children()) {
         pOutput.add($$2.holder());
         addChildren($$2, pOutput);
      }
   }

   static enum Action {
      GRANT("grant") {
         @Override
         protected boolean perform(ServerPlayer p_136395_, AdvancementHolder p_299481_) {
            AdvancementProgress $$2 = p_136395_.getAdvancements().getOrStartProgress(p_299481_);
            if ($$2.isDone()) {
               return false;
            } else {
               for (String $$3 : $$2.getRemainingCriteria()) {
                  p_136395_.getAdvancements().award(p_299481_, $$3);
               }

               return true;
            }
         }

         @Override
         protected boolean performCriterion(ServerPlayer p_136398_, AdvancementHolder p_300422_, String p_136400_) {
            return p_136398_.getAdvancements().award(p_300422_, p_136400_);
         }
      },
      REVOKE("revoke") {
         @Override
         protected boolean perform(ServerPlayer p_136406_, AdvancementHolder p_301329_) {
            AdvancementProgress $$2 = p_136406_.getAdvancements().getOrStartProgress(p_301329_);
            if (!$$2.hasProgress()) {
               return false;
            } else {
               for (String $$3 : $$2.getCompletedCriteria()) {
                  p_136406_.getAdvancements().revoke(p_301329_, $$3);
               }

               return true;
            }
         }

         @Override
         protected boolean performCriterion(ServerPlayer p_136409_, AdvancementHolder p_299512_, String p_136411_) {
            return p_136409_.getAdvancements().revoke(p_299512_, p_136411_);
         }
      };

      private final String key;

      Action(final String pKey) {
         this.key = "commands.advancement." + pKey;
      }

      public int perform(ServerPlayer pPlayer, Iterable<AdvancementHolder> pAdvancements) {
         int $$2 = 0;

         for (AdvancementHolder $$3 : pAdvancements) {
            if (this.perform(pPlayer, $$3)) {
               $$2++;
            }
         }

         return $$2;
      }

      protected abstract boolean perform(ServerPlayer pPlayer, AdvancementHolder pAdvancement);

      protected abstract boolean performCriterion(ServerPlayer pPlayer, AdvancementHolder pAdvancement, String pCriterionName);

      protected String getKey() {
         return this.key;
      }
   }

   static enum Mode {
      ONLY(false, false),
      THROUGH(true, true),
      FROM(false, true),
      UNTIL(true, false),
      EVERYTHING(true, true);

      final boolean parents;
      final boolean children;

      private Mode(final boolean pParents, final boolean pChildren) {
         this.parents = pParents;
         this.children = pChildren;
      }
   }
}
