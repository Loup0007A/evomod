package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.timers.FunctionCallback;
import net.minecraft.world.level.timers.FunctionTagCallback;
import net.minecraft.world.level.timers.TimerQueue;

public class ScheduleCommand {
   private static final SimpleCommandExceptionType ERROR_SAME_TICK = new SimpleCommandExceptionType(Component.translatable("commands.schedule.same_tick"));
   private static final DynamicCommandExceptionType ERROR_CANT_REMOVE = new DynamicCommandExceptionType(
      p_308808_ -> Component.translatableEscape("commands.schedule.cleared.failure", p_308808_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCHEDULE = (p_138424_, p_138425_) -> SharedSuggestionProvider.suggest(
         ((CommandSourceStack)p_138424_.getSource()).getServer().getWorldData().overworldData().getScheduledEvents().getEventsIds(), p_138425_
      );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("schedule")
                  .requires(p_138427_ -> p_138427_.hasPermission(2)))
               .then(
                  Commands.literal("function")
                     .then(
                        Commands.argument("function", FunctionArgument.functions())
                           .suggests(FunctionCommand.SUGGEST_FUNCTION)
                           .then(
                              ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("time", TimeArgument.time())
                                       .executes(
                                          p_138459_ -> schedule(
                                                (CommandSourceStack)p_138459_.getSource(),
                                                FunctionArgument.getFunctionOrTag(p_138459_, "function"),
                                                IntegerArgumentType.getInteger(p_138459_, "time"),
                                                true
                                             )
                                       ))
                                    .then(
                                       Commands.literal("append")
                                          .executes(
                                             p_138457_ -> schedule(
                                                   (CommandSourceStack)p_138457_.getSource(),
                                                   FunctionArgument.getFunctionOrTag(p_138457_, "function"),
                                                   IntegerArgumentType.getInteger(p_138457_, "time"),
                                                   false
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("replace")
                                       .executes(
                                          p_138455_ -> schedule(
                                                (CommandSourceStack)p_138455_.getSource(),
                                                FunctionArgument.getFunctionOrTag(p_138455_, "function"),
                                                IntegerArgumentType.getInteger(p_138455_, "time"),
                                                true
                                             )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("clear")
                  .then(
                     Commands.argument("function", StringArgumentType.greedyString())
                        .suggests(SUGGEST_SCHEDULE)
                        .executes(p_138422_ -> remove((CommandSourceStack)p_138422_.getSource(), StringArgumentType.getString(p_138422_, "function")))
                  )
            )
      );
   }

   private static int schedule(
      CommandSourceStack pSource,
      Pair<ResourceLocation, Either<CommandFunction<CommandSourceStack>, Collection<CommandFunction<CommandSourceStack>>>> pFunction,
      int pTime,
      boolean pAppend
   ) throws CommandSyntaxException {
      if (pTime == 0) {
         throw ERROR_SAME_TICK.create();
      } else {
         long $$4 = pSource.getLevel().getGameTime() + (long)pTime;
         ResourceLocation $$5 = (ResourceLocation)pFunction.getFirst();
         TimerQueue<MinecraftServer> $$6 = pSource.getServer().getWorldData().overworldData().getScheduledEvents();
         ((Either)pFunction.getSecond()).ifLeft(p_312526_ -> {
            String $$7 = $$5.toString();
            if (pAppend) {
               $$6.remove($$7);
            }

            $$6.schedule($$7, $$4, new FunctionCallback($$5));
            pSource.sendSuccess(() -> Component.translatable("commands.schedule.created.function", Component.translationArg($$5), pTime, $$4), true);
         }).ifRight(p_326343_ -> {
            String $$7 = "#" + $$5;
            if (pAppend) {
               $$6.remove($$7);
            }

            $$6.schedule($$7, $$4, new FunctionTagCallback($$5));
            pSource.sendSuccess(() -> Component.translatable("commands.schedule.created.tag", Component.translationArg($$5), pTime, $$4), true);
         });
         return Math.floorMod($$4, Integer.MAX_VALUE);
      }
   }

   private static int remove(CommandSourceStack pSource, String pFunction) throws CommandSyntaxException {
      int $$2 = pSource.getServer().getWorldData().overworldData().getScheduledEvents().remove(pFunction);
      if ($$2 == 0) {
         throw ERROR_CANT_REMOVE.create(pFunction);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.schedule.cleared.success", $$2, pFunction), true);
         return $$2;
      }
   }
}
