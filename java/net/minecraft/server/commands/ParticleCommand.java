package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("particle").requires(p_138127_ -> p_138127_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("name", ParticleArgument.particle(pContext))
                     .executes(
                        p_138148_ -> sendParticles(
                              (CommandSourceStack)p_138148_.getSource(),
                              ParticleArgument.getParticle(p_138148_, "name"),
                              ((CommandSourceStack)p_138148_.getSource()).getPosition(),
                              Vec3.ZERO,
                              0.0F,
                              0,
                              false,
                              ((CommandSourceStack)p_138148_.getSource()).getServer().getPlayerList().getPlayers()
                           )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                           .executes(
                              p_138146_ -> sendParticles(
                                    (CommandSourceStack)p_138146_.getSource(),
                                    ParticleArgument.getParticle(p_138146_, "name"),
                                    Vec3Argument.getVec3(p_138146_, "pos"),
                                    Vec3.ZERO,
                                    0.0F,
                                    0,
                                    false,
                                    ((CommandSourceStack)p_138146_.getSource()).getServer().getPlayerList().getPlayers()
                                 )
                           ))
                        .then(
                           Commands.argument("delta", Vec3Argument.vec3(false))
                              .then(
                                 Commands.argument("speed", FloatArgumentType.floatArg(0.0F))
                                    .then(
                                       ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(
                                                   p_138144_ -> sendParticles(
                                                         (CommandSourceStack)p_138144_.getSource(),
                                                         ParticleArgument.getParticle(p_138144_, "name"),
                                                         Vec3Argument.getVec3(p_138144_, "pos"),
                                                         Vec3Argument.getVec3(p_138144_, "delta"),
                                                         FloatArgumentType.getFloat(p_138144_, "speed"),
                                                         IntegerArgumentType.getInteger(p_138144_, "count"),
                                                         false,
                                                         ((CommandSourceStack)p_138144_.getSource()).getServer().getPlayerList().getPlayers()
                                                      )
                                                ))
                                             .then(
                                                ((LiteralArgumentBuilder)Commands.literal("force")
                                                      .executes(
                                                         p_138142_ -> sendParticles(
                                                               (CommandSourceStack)p_138142_.getSource(),
                                                               ParticleArgument.getParticle(p_138142_, "name"),
                                                               Vec3Argument.getVec3(p_138142_, "pos"),
                                                               Vec3Argument.getVec3(p_138142_, "delta"),
                                                               FloatArgumentType.getFloat(p_138142_, "speed"),
                                                               IntegerArgumentType.getInteger(p_138142_, "count"),
                                                               true,
                                                               ((CommandSourceStack)p_138142_.getSource()).getServer().getPlayerList().getPlayers()
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.argument("viewers", EntityArgument.players())
                                                         .executes(
                                                            p_138140_ -> sendParticles(
                                                                  (CommandSourceStack)p_138140_.getSource(),
                                                                  ParticleArgument.getParticle(p_138140_, "name"),
                                                                  Vec3Argument.getVec3(p_138140_, "pos"),
                                                                  Vec3Argument.getVec3(p_138140_, "delta"),
                                                                  FloatArgumentType.getFloat(p_138140_, "speed"),
                                                                  IntegerArgumentType.getInteger(p_138140_, "count"),
                                                                  true,
                                                                  EntityArgument.getPlayers(p_138140_, "viewers")
                                                               )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("normal")
                                                   .executes(
                                                      p_138138_ -> sendParticles(
                                                            (CommandSourceStack)p_138138_.getSource(),
                                                            ParticleArgument.getParticle(p_138138_, "name"),
                                                            Vec3Argument.getVec3(p_138138_, "pos"),
                                                            Vec3Argument.getVec3(p_138138_, "delta"),
                                                            FloatArgumentType.getFloat(p_138138_, "speed"),
                                                            IntegerArgumentType.getInteger(p_138138_, "count"),
                                                            false,
                                                            ((CommandSourceStack)p_138138_.getSource()).getServer().getPlayerList().getPlayers()
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("viewers", EntityArgument.players())
                                                      .executes(
                                                         p_138125_ -> sendParticles(
                                                               (CommandSourceStack)p_138125_.getSource(),
                                                               ParticleArgument.getParticle(p_138125_, "name"),
                                                               Vec3Argument.getVec3(p_138125_, "pos"),
                                                               Vec3Argument.getVec3(p_138125_, "delta"),
                                                               FloatArgumentType.getFloat(p_138125_, "speed"),
                                                               IntegerArgumentType.getInteger(p_138125_, "count"),
                                                               false,
                                                               EntityArgument.getPlayers(p_138125_, "viewers")
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

   private static int sendParticles(
      CommandSourceStack pSource,
      ParticleOptions pParticleData,
      Vec3 pPos,
      Vec3 pDelta,
      float pSpeed,
      int pCount,
      boolean pForce,
      Collection<ServerPlayer> pViewers
   ) throws CommandSyntaxException {
      int $$8 = 0;

      for (ServerPlayer $$9 : pViewers) {
         if (pSource.getLevel().sendParticles($$9, pParticleData, pForce, pPos.x, pPos.y, pPos.z, pCount, pDelta.x, pDelta.y, pDelta.z, (double)pSpeed)) {
            $$8++;
         }
      }

      if ($$8 == 0) {
         throw ERROR_FAILED.create();
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.particle.success", BuiltInRegistries.PARTICLE_TYPE.getKey(pParticleData.getType()).toString()), true
         );
         return $$8;
      }
   }
}
