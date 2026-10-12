package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PlaySoundCommand {
   private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(Component.translatable("commands.playsound.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> $$1 = (RequiredArgumentBuilder<CommandSourceStack, ResourceLocation>)Commands.argument(
            "sound", ResourceLocationArgument.id()
         )
         .suggests(SuggestionProviders.AVAILABLE_SOUNDS)
         .executes(
            p_326320_ -> playSound(
                  (CommandSourceStack)p_326320_.getSource(),
                  getCallingPlayerAsCollection(((CommandSourceStack)p_326320_.getSource()).getPlayer()),
                  ResourceLocationArgument.getId(p_326320_, "sound"),
                  SoundSource.MASTER,
                  ((CommandSourceStack)p_326320_.getSource()).getPosition(),
                  1.0F,
                  1.0F,
                  0.0F
               )
         );

      for (SoundSource $$2 : SoundSource.values()) {
         $$1.then(source($$2));
      }

      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("playsound").requires(p_138159_ -> p_138159_.hasPermission(2))).then($$1)
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> source(SoundSource pCategory) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(pCategory.getName())
            .executes(
               p_326324_ -> playSound(
                     (CommandSourceStack)p_326324_.getSource(),
                     getCallingPlayerAsCollection(((CommandSourceStack)p_326324_.getSource()).getPlayer()),
                     ResourceLocationArgument.getId(p_326324_, "sound"),
                     pCategory,
                     ((CommandSourceStack)p_326324_.getSource()).getPosition(),
                     1.0F,
                     1.0F,
                     0.0F
                  )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                  .executes(
                     p_138180_ -> playSound(
                           (CommandSourceStack)p_138180_.getSource(),
                           EntityArgument.getPlayers(p_138180_, "targets"),
                           ResourceLocationArgument.getId(p_138180_, "sound"),
                           pCategory,
                           ((CommandSourceStack)p_138180_.getSource()).getPosition(),
                           1.0F,
                           1.0F,
                           0.0F
                        )
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           p_138177_ -> playSound(
                                 (CommandSourceStack)p_138177_.getSource(),
                                 EntityArgument.getPlayers(p_138177_, "targets"),
                                 ResourceLocationArgument.getId(p_138177_, "sound"),
                                 pCategory,
                                 Vec3Argument.getVec3(p_138177_, "pos"),
                                 1.0F,
                                 1.0F,
                                 0.0F
                              )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                              .executes(
                                 p_138174_ -> playSound(
                                       (CommandSourceStack)p_138174_.getSource(),
                                       EntityArgument.getPlayers(p_138174_, "targets"),
                                       ResourceLocationArgument.getId(p_138174_, "sound"),
                                       pCategory,
                                       Vec3Argument.getVec3(p_138174_, "pos"),
                                       (Float)p_138174_.getArgument("volume", Float.class),
                                       1.0F,
                                       0.0F
                                    )
                              ))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                    .executes(
                                       p_138171_ -> playSound(
                                             (CommandSourceStack)p_138171_.getSource(),
                                             EntityArgument.getPlayers(p_138171_, "targets"),
                                             ResourceLocationArgument.getId(p_138171_, "sound"),
                                             pCategory,
                                             Vec3Argument.getVec3(p_138171_, "pos"),
                                             (Float)p_138171_.getArgument("volume", Float.class),
                                             (Float)p_138171_.getArgument("pitch", Float.class),
                                             0.0F
                                          )
                                    ))
                                 .then(
                                    Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                       .executes(
                                          p_138155_ -> playSound(
                                                (CommandSourceStack)p_138155_.getSource(),
                                                EntityArgument.getPlayers(p_138155_, "targets"),
                                                ResourceLocationArgument.getId(p_138155_, "sound"),
                                                pCategory,
                                                Vec3Argument.getVec3(p_138155_, "pos"),
                                                (Float)p_138155_.getArgument("volume", Float.class),
                                                (Float)p_138155_.getArgument("pitch", Float.class),
                                                (Float)p_138155_.getArgument("minVolume", Float.class)
                                             )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static Collection<ServerPlayer> getCallingPlayerAsCollection(@Nullable ServerPlayer pPlayer) {
      return pPlayer != null ? List.of(pPlayer) : List.of();
   }

   private static int playSound(
      CommandSourceStack pSource,
      Collection<ServerPlayer> pTargets,
      ResourceLocation pSound,
      SoundSource pCategory,
      Vec3 pPos,
      float pVolume,
      float pPitch,
      float pMinVolume
   ) throws CommandSyntaxException {
      Holder<SoundEvent> $$8 = Holder.direct(SoundEvent.createVariableRangeEvent(pSound));
      double $$9 = (double)Mth.square($$8.value().getRange(pVolume));
      int $$10 = 0;
      long $$11 = pSource.getLevel().getRandom().nextLong();

      for (ServerPlayer $$12 : pTargets) {
         double $$13 = pPos.x - $$12.getX();
         double $$14 = pPos.y - $$12.getY();
         double $$15 = pPos.z - $$12.getZ();
         double $$16 = $$13 * $$13 + $$14 * $$14 + $$15 * $$15;
         Vec3 $$17 = pPos;
         float $$18 = pVolume;
         if ($$16 > $$9) {
            if (pMinVolume <= 0.0F) {
               continue;
            }

            double $$19 = Math.sqrt($$16);
            $$17 = new Vec3($$12.getX() + $$13 / $$19 * 2.0, $$12.getY() + $$14 / $$19 * 2.0, $$12.getZ() + $$15 / $$19 * 2.0);
            $$18 = pMinVolume;
         }

         $$12.connection.send(new ClientboundSoundPacket($$8, pCategory, $$17.x(), $$17.y(), $$17.z(), $$18, pPitch, $$11));
         $$10++;
      }

      if ($$10 == 0) {
         throw ERROR_TOO_FAR.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable("commands.playsound.success.single", Component.translationArg(pSound), pTargets.iterator().next().getDisplayName()),
               true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(pSound), pTargets.size()), true);
         }

         return $$10;
      }
   }
}
