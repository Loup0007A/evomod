package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
   private static final float MAX_TICKRATE = 10000.0F;
   private static final String DEFAULT_TICKRATE = String.valueOf(20);

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                 "tick"
                              )
                              .requires(p_313254_ -> p_313254_.hasPermission(3)))
                           .then(Commands.literal("query").executes(p_310022_ -> tickQuery((CommandSourceStack)p_310022_.getSource()))))
                        .then(
                           Commands.literal("rate")
                              .then(
                                 Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F))
                                    .suggests((p_311314_, p_311289_) -> SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, p_311289_))
                                    .executes(
                                       p_312329_ -> setTickingRate((CommandSourceStack)p_312329_.getSource(), FloatArgumentType.getFloat(p_312329_, "rate"))
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("step")
                                 .executes(p_311036_ -> step((CommandSourceStack)p_311036_.getSource(), 1)))
                              .then(Commands.literal("stop").executes(p_309944_ -> stopStepping((CommandSourceStack)p_309944_.getSource()))))
                           .then(
                              Commands.argument("time", TimeArgument.time(1))
                                 .suggests((p_313203_, p_312907_) -> SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, p_312907_))
                                 .executes(p_312113_ -> step((CommandSourceStack)p_312113_.getSource(), IntegerArgumentType.getInteger(p_312113_, "time")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("sprint")
                           .then(Commands.literal("stop").executes(p_311524_ -> stopSprinting((CommandSourceStack)p_311524_.getSource()))))
                        .then(
                           Commands.argument("time", TimeArgument.time(1))
                              .suggests((p_311140_, p_312761_) -> SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, p_312761_))
                              .executes(p_311082_ -> sprint((CommandSourceStack)p_311082_.getSource(), IntegerArgumentType.getInteger(p_311082_, "time")))
                        )
                  ))
               .then(Commands.literal("unfreeze").executes(p_309501_ -> setFreeze((CommandSourceStack)p_309501_.getSource(), false))))
            .then(Commands.literal("freeze").executes(p_312020_ -> setFreeze((CommandSourceStack)p_312020_.getSource(), true)))
      );
   }

   private static String nanosToMilisString(long pNanos) {
      return String.format("%.1f", (float)pNanos / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND);
   }

   private static int setTickingRate(CommandSourceStack pSource, float pTickRate) {
      ServerTickRateManager $$2 = pSource.getServer().tickRateManager();
      $$2.setTickRate(pTickRate);
      String $$3 = String.format("%.1f", pTickRate);
      pSource.sendSuccess(() -> Component.translatable("commands.tick.rate.success", $$3), true);
      return (int)pTickRate;
   }

   private static int tickQuery(CommandSourceStack pSource) {
      ServerTickRateManager $$1 = pSource.getServer().tickRateManager();
      String $$2 = nanosToMilisString(pSource.getServer().getAverageTickTimeNanos());
      float $$3 = $$1.tickrate();
      String $$4 = String.format("%.1f", $$3);
      if ($$1.isSprinting()) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
         pSource.sendSuccess(() -> Component.translatable("commands.tick.query.rate.sprinting", $$4, $$2), false);
      } else {
         if ($$1.isFrozen()) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
         } else if ($$1.nanosecondsPerTick() < pSource.getServer().getAverageTickTimeNanos()) {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
         }

         String $$5 = nanosToMilisString($$1.nanosecondsPerTick());
         pSource.sendSuccess(() -> Component.translatable("commands.tick.query.rate.running", $$4, $$2, $$5), false);
      }

      long[] $$6 = Arrays.copyOf(pSource.getServer().getTickTimesNanos(), pSource.getServer().getTickTimesNanos().length);
      Arrays.sort($$6);
      String $$7 = nanosToMilisString($$6[$$6.length / 2]);
      String $$8 = nanosToMilisString($$6[(int)((double)$$6.length * 0.95)]);
      String $$9 = nanosToMilisString($$6[(int)((double)$$6.length * 0.99)]);
      pSource.sendSuccess(() -> Component.translatable("commands.tick.query.percentiles", $$7, $$8, $$9, $$6.length), false);
      return (int)$$3;
   }

   private static int sprint(CommandSourceStack pSource, int pSprintTime) {
      boolean $$2 = pSource.getServer().tickRateManager().requestGameToSprint(pSprintTime);
      if ($$2) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
      }

      pSource.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
      return 1;
   }

   private static int setFreeze(CommandSourceStack pSource, boolean pFrozen) {
      ServerTickRateManager $$2 = pSource.getServer().tickRateManager();
      if (pFrozen) {
         if ($$2.isSprinting()) {
            $$2.stopSprinting();
         }

         if ($$2.isSteppingForward()) {
            $$2.stopStepping();
         }
      }

      $$2.setFrozen(pFrozen);
      if (pFrozen) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
      }

      return pFrozen ? 1 : 0;
   }

   private static int step(CommandSourceStack pSource, int pTicks) {
      ServerTickRateManager $$2 = pSource.getServer().tickRateManager();
      boolean $$3 = $$2.stepGameIfPaused(pTicks);
      if ($$3) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.step.success", pTicks), true);
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.step.fail"));
      }

      return 1;
   }

   private static int stopStepping(CommandSourceStack pSource) {
      ServerTickRateManager $$1 = pSource.getServer().tickRateManager();
      boolean $$2 = $$1.stopStepping();
      if ($$2) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
         return 1;
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
         return 0;
      }
   }

   private static int stopSprinting(CommandSourceStack pSource) {
      ServerTickRateManager $$1 = pSource.getServer().tickRateManager();
      boolean $$2 = $$1.stopSprinting();
      if ($$2) {
         pSource.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
         return 1;
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
         return 0;
      }
   }
}
