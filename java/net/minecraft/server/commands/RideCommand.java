package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class RideCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_RIDING = new DynamicCommandExceptionType(
      p_308800_ -> Component.translatableEscape("commands.ride.not_riding", p_308800_)
   );
   private static final Dynamic2CommandExceptionType ERROR_ALREADY_RIDING = new Dynamic2CommandExceptionType(
      (p_308801_, p_308802_) -> Component.translatableEscape("commands.ride.already_riding", p_308801_, p_308802_)
   );
   private static final Dynamic2CommandExceptionType ERROR_MOUNT_FAILED = new Dynamic2CommandExceptionType(
      (p_308803_, p_308804_) -> Component.translatableEscape("commands.ride.mount.failure.generic", p_308803_, p_308804_)
   );
   private static final SimpleCommandExceptionType ERROR_MOUNTING_PLAYER = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.cant_ride_players")
   );
   private static final SimpleCommandExceptionType ERROR_MOUNTING_LOOP = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.loop")
   );
   private static final SimpleCommandExceptionType ERROR_WRONG_DIMENSION = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.wrong_dimension")
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ride").requires(p_265326_ -> p_265326_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .then(
                        Commands.literal("mount")
                           .then(
                              Commands.argument("vehicle", EntityArgument.entity())
                                 .executes(
                                    p_265139_ -> mount(
                                          (CommandSourceStack)p_265139_.getSource(),
                                          EntityArgument.getEntity(p_265139_, "target"),
                                          EntityArgument.getEntity(p_265139_, "vehicle")
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("dismount")
                        .executes(p_265418_ -> dismount((CommandSourceStack)p_265418_.getSource(), EntityArgument.getEntity(p_265418_, "target")))
                  )
            )
      );
   }

   private static int mount(CommandSourceStack pSource, Entity pTarget, Entity pVehicle) throws CommandSyntaxException {
      Entity $$3 = pTarget.getVehicle();
      if ($$3 != null) {
         throw ERROR_ALREADY_RIDING.create(pTarget.getDisplayName(), $$3.getDisplayName());
      } else if (pVehicle.getType() == EntityType.PLAYER) {
         throw ERROR_MOUNTING_PLAYER.create();
      } else if (pTarget.getSelfAndPassengers().anyMatch(p_265501_ -> p_265501_ == pVehicle)) {
         throw ERROR_MOUNTING_LOOP.create();
      } else if (pTarget.level() != pVehicle.level()) {
         throw ERROR_WRONG_DIMENSION.create();
      } else if (!pTarget.startRiding(pVehicle, true)) {
         throw ERROR_MOUNT_FAILED.create(pTarget.getDisplayName(), pVehicle.getDisplayName());
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.ride.mount.success", pTarget.getDisplayName(), pVehicle.getDisplayName()), true);
         return 1;
      }
   }

   private static int dismount(CommandSourceStack pSource, Entity pTarget) throws CommandSyntaxException {
      Entity $$2 = pTarget.getVehicle();
      if ($$2 == null) {
         throw ERROR_NOT_RIDING.create(pTarget.getDisplayName());
      } else {
         pTarget.stopRiding();
         pSource.sendSuccess(() -> Component.translatable("commands.ride.dismount.success", pTarget.getDisplayName(), $$2.getDisplayName()), true);
         return 1;
      }
   }
}
