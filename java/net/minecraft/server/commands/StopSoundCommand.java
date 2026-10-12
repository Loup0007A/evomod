package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class StopSoundCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, EntitySelector> $$1 = (RequiredArgumentBuilder<CommandSourceStack, EntitySelector>)((RequiredArgumentBuilder)Commands.argument(
               "targets", EntityArgument.players()
            )
            .executes(p_138809_ -> stopSound((CommandSourceStack)p_138809_.getSource(), EntityArgument.getPlayers(p_138809_, "targets"), null, null)))
         .then(
            Commands.literal("*")
               .then(
                  Commands.argument("sound", ResourceLocationArgument.id())
                     .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                     .executes(
                        p_138797_ -> stopSound(
                              (CommandSourceStack)p_138797_.getSource(),
                              EntityArgument.getPlayers(p_138797_, "targets"),
                              null,
                              ResourceLocationArgument.getId(p_138797_, "sound")
                           )
                     )
               )
         );

      for (SoundSource $$2 : SoundSource.values()) {
         $$1.then(
            ((LiteralArgumentBuilder)Commands.literal($$2.getName())
                  .executes(p_138807_ -> stopSound((CommandSourceStack)p_138807_.getSource(), EntityArgument.getPlayers(p_138807_, "targets"), $$2, null)))
               .then(
                  Commands.argument("sound", ResourceLocationArgument.id())
                     .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
                     .executes(
                        p_138793_ -> stopSound(
                              (CommandSourceStack)p_138793_.getSource(),
                              EntityArgument.getPlayers(p_138793_, "targets"),
                              $$2,
                              ResourceLocationArgument.getId(p_138793_, "sound")
                           )
                     )
               )
         );
      }

      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("stopsound").requires(p_138799_ -> p_138799_.hasPermission(2))).then($$1)
      );
   }

   private static int stopSound(
      CommandSourceStack pSource, Collection<ServerPlayer> pTargets, @Nullable SoundSource pCategory, @Nullable ResourceLocation pSound
   ) {
      ClientboundStopSoundPacket $$4 = new ClientboundStopSoundPacket(pSound, pCategory);

      for (ServerPlayer $$5 : pTargets) {
         $$5.connection.send($$4);
      }

      if (pCategory != null) {
         if (pSound != null) {
            pSource.sendSuccess(
               () -> Component.translatable("commands.stopsound.success.source.sound", Component.translationArg(pSound), pCategory.getName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.stopsound.success.source.any", pCategory.getName()), true);
         }
      } else if (pSound != null) {
         pSource.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.sound", Component.translationArg(pSound)), true);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.stopsound.success.sourceless.any"), true);
      }

      return pTargets.size();
   }
}
