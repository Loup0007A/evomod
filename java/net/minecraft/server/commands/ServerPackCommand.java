package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

public class ServerPackCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("serverpack")
                  .requires(p_312279_ -> p_312279_.hasPermission(2)))
               .then(
                  Commands.literal("push")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("url", StringArgumentType.string())
                              .then(
                                 ((RequiredArgumentBuilder)Commands.argument("uuid", UuidArgument.uuid())
                                       .then(
                                          Commands.argument("hash", StringArgumentType.word())
                                             .executes(
                                                p_310536_ -> pushPack(
                                                      (CommandSourceStack)p_310536_.getSource(),
                                                      StringArgumentType.getString(p_310536_, "url"),
                                                      Optional.of(UuidArgument.getUuid(p_310536_, "uuid")),
                                                      Optional.of(StringArgumentType.getString(p_310536_, "hash"))
                                                   )
                                             )
                                       ))
                                    .executes(
                                       p_311224_ -> pushPack(
                                             (CommandSourceStack)p_311224_.getSource(),
                                             StringArgumentType.getString(p_311224_, "url"),
                                             Optional.of(UuidArgument.getUuid(p_311224_, "uuid")),
                                             Optional.empty()
                                          )
                                    )
                              ))
                           .executes(
                              p_310851_ -> pushPack(
                                    (CommandSourceStack)p_310851_.getSource(),
                                    StringArgumentType.getString(p_310851_, "url"),
                                    Optional.empty(),
                                    Optional.empty()
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("pop")
                  .then(
                     Commands.argument("uuid", UuidArgument.uuid())
                        .executes(p_311174_ -> popPack((CommandSourceStack)p_311174_.getSource(), UuidArgument.getUuid(p_311174_, "uuid")))
                  )
            )
      );
   }

   private static void sendToAllConnections(CommandSourceStack pSource, Packet<?> pPacket) {
      pSource.getServer().getConnection().getConnections().forEach(p_310319_ -> p_310319_.send(pPacket));
   }

   private static int pushPack(CommandSourceStack pSource, String pUrl, Optional<UUID> pUuid, Optional<String> pHash) {
      UUID $$4 = pUuid.orElseGet(() -> UUID.nameUUIDFromBytes(pUrl.getBytes(StandardCharsets.UTF_8)));
      String $$5 = pHash.orElse("");
      ClientboundResourcePackPushPacket $$6 = new ClientboundResourcePackPushPacket($$4, pUrl, $$5, false, null);
      sendToAllConnections(pSource, $$6);
      return 0;
   }

   private static int popPack(CommandSourceStack pSource, UUID pUuid) {
      ClientboundResourcePackPopPacket $$2 = new ClientboundResourcePackPopPacket(Optional.of(pUuid));
      sendToAllConnections(pSource, $$2);
      return 0;
   }
}
