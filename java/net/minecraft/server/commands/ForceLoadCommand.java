package net.minecraft.server.commands;

import com.google.common.base.Joiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceLoadCommand {
   private static final int MAX_CHUNK_LIMIT = 256;
   private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType(
      (p_308708_, p_308709_) -> Component.translatableEscape("commands.forceload.toobig", p_308708_, p_308709_)
   );
   private static final Dynamic2CommandExceptionType ERROR_NOT_TICKING = new Dynamic2CommandExceptionType(
      (p_308720_, p_308721_) -> Component.translatableEscape("commands.forceload.query.failure", p_308720_, p_308721_)
   );
   private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(Component.translatable("commands.forceload.added.failure"));
   private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(
      Component.translatable("commands.forceload.removed.failure")
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("forceload")
                     .requires(p_137703_ -> p_137703_.hasPermission(2)))
                  .then(
                     Commands.literal("add")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos())
                                 .executes(
                                    p_137711_ -> changeForceLoad(
                                          (CommandSourceStack)p_137711_.getSource(),
                                          ColumnPosArgument.getColumnPos(p_137711_, "from"),
                                          ColumnPosArgument.getColumnPos(p_137711_, "from"),
                                          true
                                       )
                                 ))
                              .then(
                                 Commands.argument("to", ColumnPosArgument.columnPos())
                                    .executes(
                                       p_137709_ -> changeForceLoad(
                                             (CommandSourceStack)p_137709_.getSource(),
                                             ColumnPosArgument.getColumnPos(p_137709_, "from"),
                                             ColumnPosArgument.getColumnPos(p_137709_, "to"),
                                             true
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("remove")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("from", ColumnPosArgument.columnPos())
                                 .executes(
                                    p_137707_ -> changeForceLoad(
                                          (CommandSourceStack)p_137707_.getSource(),
                                          ColumnPosArgument.getColumnPos(p_137707_, "from"),
                                          ColumnPosArgument.getColumnPos(p_137707_, "from"),
                                          false
                                       )
                                 ))
                              .then(
                                 Commands.argument("to", ColumnPosArgument.columnPos())
                                    .executes(
                                       p_137705_ -> changeForceLoad(
                                             (CommandSourceStack)p_137705_.getSource(),
                                             ColumnPosArgument.getColumnPos(p_137705_, "from"),
                                             ColumnPosArgument.getColumnPos(p_137705_, "to"),
                                             false
                                          )
                                    )
                              )
                        ))
                     .then(Commands.literal("all").executes(p_137701_ -> removeAll((CommandSourceStack)p_137701_.getSource())))
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("query").executes(p_137694_ -> listForceLoad((CommandSourceStack)p_137694_.getSource())))
                  .then(
                     Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(p_137679_ -> queryForceLoad((CommandSourceStack)p_137679_.getSource(), ColumnPosArgument.getColumnPos(p_137679_, "pos")))
                  )
            )
      );
   }

   private static int queryForceLoad(CommandSourceStack pSource, ColumnPos pPos) throws CommandSyntaxException {
      ChunkPos $$2 = pPos.toChunkPos();
      ServerLevel $$3 = pSource.getLevel();
      ResourceKey<Level> $$4 = $$3.dimension();
      boolean $$5 = $$3.getForcedChunks().contains($$2.toLong());
      if ($$5) {
         pSource.sendSuccess(
            () -> Component.translatable("commands.forceload.query.success", Component.translationArg($$2), Component.translationArg($$4.location())), false
         );
         return 1;
      } else {
         throw ERROR_NOT_TICKING.create($$2, $$4.location());
      }
   }

   private static int listForceLoad(CommandSourceStack pSource) {
      ServerLevel $$1 = pSource.getLevel();
      ResourceKey<Level> $$2 = $$1.dimension();
      LongSet $$3 = $$1.getForcedChunks();
      int $$4 = $$3.size();
      if ($$4 > 0) {
         String $$5 = Joiner.on(", ").join($$3.stream().sorted().map(ChunkPos::new).map(ChunkPos::toString).iterator());
         if ($$4 == 1) {
            pSource.sendSuccess(() -> Component.translatable("commands.forceload.list.single", Component.translationArg($$2.location()), $$5), false);
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.forceload.list.multiple", $$4, Component.translationArg($$2.location()), $$5), false);
         }
      } else {
         pSource.sendFailure(Component.translatable("commands.forceload.added.none", Component.translationArg($$2.location())));
      }

      return $$4;
   }

   private static int removeAll(CommandSourceStack pSource) {
      ServerLevel $$1 = pSource.getLevel();
      ResourceKey<Level> $$2 = $$1.dimension();
      LongSet $$3 = $$1.getForcedChunks();
      $$3.forEach(p_137675_ -> $$1.setChunkForced(ChunkPos.getX(p_137675_), ChunkPos.getZ(p_137675_), false));
      pSource.sendSuccess(() -> Component.translatable("commands.forceload.removed.all", Component.translationArg($$2.location())), true);
      return 0;
   }

   private static int changeForceLoad(CommandSourceStack pSource, ColumnPos pFrom, ColumnPos pTo, boolean pAdd) throws CommandSyntaxException {
      int $$4 = Math.min(pFrom.x(), pTo.x());
      int $$5 = Math.min(pFrom.z(), pTo.z());
      int $$6 = Math.max(pFrom.x(), pTo.x());
      int $$7 = Math.max(pFrom.z(), pTo.z());
      if ($$4 >= -30000000 && $$5 >= -30000000 && $$6 < 30000000 && $$7 < 30000000) {
         int $$8 = SectionPos.blockToSectionCoord($$4);
         int $$9 = SectionPos.blockToSectionCoord($$5);
         int $$10 = SectionPos.blockToSectionCoord($$6);
         int $$11 = SectionPos.blockToSectionCoord($$7);
         long $$12 = ((long)($$10 - $$8) + 1L) * ((long)($$11 - $$9) + 1L);
         if ($$12 > 256L) {
            throw ERROR_TOO_MANY_CHUNKS.create(256, $$12);
         } else {
            ServerLevel $$13 = pSource.getLevel();
            ResourceKey<Level> $$14 = $$13.dimension();
            ChunkPos $$15 = null;
            int $$16 = 0;

            for (int $$17 = $$8; $$17 <= $$10; $$17++) {
               for (int $$18 = $$9; $$18 <= $$11; $$18++) {
                  boolean $$19 = $$13.setChunkForced($$17, $$18, pAdd);
                  if ($$19) {
                     $$16++;
                     if ($$15 == null) {
                        $$15 = new ChunkPos($$17, $$18);
                     }
                  }
               }
            }

            ChunkPos $$20 = $$15;
            int $$21 = $$16;
            if ($$21 == 0) {
               throw (pAdd ? ERROR_ALL_ADDED : ERROR_NONE_REMOVED).create();
            } else {
               if ($$21 == 1) {
                  pSource.sendSuccess(
                     () -> Component.translatable(
                           "commands.forceload." + (pAdd ? "added" : "removed") + ".single",
                           Component.translationArg($$20),
                           Component.translationArg($$14.location())
                        ),
                     true
                  );
               } else {
                  ChunkPos $$22 = new ChunkPos($$8, $$9);
                  ChunkPos $$23 = new ChunkPos($$10, $$11);
                  pSource.sendSuccess(
                     () -> Component.translatable(
                           "commands.forceload." + (pAdd ? "added" : "removed") + ".multiple",
                           $$21,
                           Component.translationArg($$14.location()),
                           Component.translationArg($$22),
                           Component.translationArg($$23)
                        ),
                     true
                  );
               }

               return $$21;
            }
         }
      } else {
         throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
      }
   }
}
