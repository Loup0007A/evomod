package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_308640_, p_308641_) -> Component.translatableEscape("commands.clone.toobig", p_308640_, p_308641_)
   );
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = p_284652_ -> !p_284652_.getState().isAir();

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires(p_136734_ -> p_136734_.hasPermission(2)))
               .then(beginEndDestinationAndModeSuffix(pContext, p_264757_ -> ((CommandSourceStack)p_264757_.getSource()).getLevel())))
            .then(
               Commands.literal("from")
                  .then(
                     Commands.argument("sourceDimension", DimensionArgument.dimension())
                        .then(beginEndDestinationAndModeSuffix(pContext, p_264743_ -> DimensionArgument.getDimension(p_264743_, "sourceDimension")))
                  )
            )
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
      CommandBuildContext pBuildContext, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pLevelGetter
   ) {
      return Commands.argument("begin", BlockPosArgument.blockPos())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos())
                  .then(destinationAndModeSuffix(pBuildContext, pLevelGetter, p_264751_ -> ((CommandSourceStack)p_264751_.getSource()).getLevel())))
               .then(
                  Commands.literal("to")
                     .then(
                        Commands.argument("targetDimension", DimensionArgument.dimension())
                           .then(
                              destinationAndModeSuffix(pBuildContext, pLevelGetter, p_264756_ -> DimensionArgument.getDimension(p_264756_, "targetDimension"))
                           )
                     )
               )
         );
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(
      CommandContext<CommandSourceStack> pContext, ServerLevel pLevel, String pName
   ) throws CommandSyntaxException {
      BlockPos $$3 = BlockPosArgument.getLoadedBlockPos(pContext, pLevel, pName);
      return new CloneCommands.DimensionAndPosition(pLevel, $$3);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndModeSuffix(
      CommandBuildContext pBuildContext,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pSourceLevelGetter,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pDestinationLevelGetter
   ) {
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$3 = p_264737_ -> getLoadedDimensionAndPosition(
            p_264737_, pSourceLevelGetter.apply(p_264737_), "begin"
         );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$4 = p_264735_ -> getLoadedDimensionAndPosition(
            p_264735_, pSourceLevelGetter.apply(p_264735_), "end"
         );
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> $$5 = p_264768_ -> getLoadedDimensionAndPosition(
            p_264768_, pDestinationLevelGetter.apply(p_264768_), "destination"
         );
      return ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                  .executes(
                     p_264761_ -> clone(
                           (CommandSourceStack)p_264761_.getSource(),
                           $$3.apply(p_264761_),
                           $$4.apply(p_264761_),
                           $$5.apply(p_264761_),
                           p_180033_ -> true,
                           CloneCommands.Mode.NORMAL
                        )
                  ))
               .then(
                  wrapWithCloneMode(
                     $$3,
                     $$4,
                     $$5,
                     p_264738_ -> p_180041_ -> true,
                     Commands.literal("replace")
                        .executes(
                           p_264755_ -> clone(
                                 (CommandSourceStack)p_264755_.getSource(),
                                 $$3.apply(p_264755_),
                                 $$4.apply(p_264755_),
                                 $$5.apply(p_264755_),
                                 p_180039_ -> true,
                                 CloneCommands.Mode.NORMAL
                              )
                        )
                  )
               ))
            .then(
               wrapWithCloneMode(
                  $$3,
                  $$4,
                  $$5,
                  p_264744_ -> FILTER_AIR,
                  Commands.literal("masked")
                     .executes(
                        p_264742_ -> clone(
                              (CommandSourceStack)p_264742_.getSource(),
                              $$3.apply(p_264742_),
                              $$4.apply(p_264742_),
                              $$5.apply(p_264742_),
                              FILTER_AIR,
                              CloneCommands.Mode.NORMAL
                           )
                     )
               )
            ))
         .then(
            Commands.literal("filtered")
               .then(
                  wrapWithCloneMode(
                     $$3,
                     $$4,
                     $$5,
                     p_264745_ -> BlockPredicateArgument.getBlockPredicate(p_264745_, "filter"),
                     Commands.argument("filter", BlockPredicateArgument.blockPredicate(pBuildContext))
                        .executes(
                           p_264733_ -> clone(
                                 (CommandSourceStack)p_264733_.getSource(),
                                 $$3.apply(p_264733_),
                                 $$4.apply(p_264733_),
                                 $$5.apply(p_264733_),
                                 BlockPredicateArgument.getBlockPredicate(p_264733_, "filter"),
                                 CloneCommands.Mode.NORMAL
                              )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pBeginGetter,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pEndGetter,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pTargetGetter,
      CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> pFilterGetter,
      ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder
   ) {
      return pArgumentBuilder.then(
            Commands.literal("force")
               .executes(
                  p_264773_ -> clone(
                        (CommandSourceStack)p_264773_.getSource(),
                        pBeginGetter.apply(p_264773_),
                        pEndGetter.apply(p_264773_),
                        pTargetGetter.apply(p_264773_),
                        pFilterGetter.apply(p_264773_),
                        CloneCommands.Mode.FORCE
                     )
               )
         )
         .then(
            Commands.literal("move")
               .executes(
                  p_264766_ -> clone(
                        (CommandSourceStack)p_264766_.getSource(),
                        pBeginGetter.apply(p_264766_),
                        pEndGetter.apply(p_264766_),
                        pTargetGetter.apply(p_264766_),
                        pFilterGetter.apply(p_264766_),
                        CloneCommands.Mode.MOVE
                     )
               )
         )
         .then(
            Commands.literal("normal")
               .executes(
                  p_264750_ -> clone(
                        (CommandSourceStack)p_264750_.getSource(),
                        pBeginGetter.apply(p_264750_),
                        pEndGetter.apply(p_264750_),
                        pTargetGetter.apply(p_264750_),
                        pFilterGetter.apply(p_264750_),
                        CloneCommands.Mode.NORMAL
                     )
               )
         );
   }

   private static int clone(
      CommandSourceStack pSource,
      CloneCommands.DimensionAndPosition pBegin,
      CloneCommands.DimensionAndPosition pEnd,
      CloneCommands.DimensionAndPosition pTarget,
      Predicate<BlockInWorld> pFilter,
      CloneCommands.Mode pMode
   ) throws CommandSyntaxException {
      BlockPos $$6 = pBegin.position();
      BlockPos $$7 = pEnd.position();
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      BlockPos $$9 = pTarget.position();
      BlockPos $$10 = $$9.offset($$8.getLength());
      BoundingBox $$11 = BoundingBox.fromCorners($$9, $$10);
      ServerLevel $$12 = pBegin.dimension();
      ServerLevel $$13 = pTarget.dimension();
      if (!pMode.canOverlap() && $$12 == $$13 && $$11.intersects($$8)) {
         throw ERROR_OVERLAP.create();
      } else {
         int $$14 = $$8.getXSpan() * $$8.getYSpan() * $$8.getZSpan();
         int $$15 = pSource.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
         if ($$14 > $$15) {
            throw ERROR_AREA_TOO_LARGE.create($$15, $$14);
         } else if ($$12.hasChunksAt($$6, $$7) && $$13.hasChunksAt($$9, $$10)) {
            List<CloneCommands.CloneBlockInfo> $$16 = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> $$17 = Lists.newArrayList();
            List<CloneCommands.CloneBlockInfo> $$18 = Lists.newArrayList();
            Deque<BlockPos> $$19 = Lists.newLinkedList();
            BlockPos $$20 = new BlockPos($$11.minX() - $$8.minX(), $$11.minY() - $$8.minY(), $$11.minZ() - $$8.minZ());

            for (int $$21 = $$8.minZ(); $$21 <= $$8.maxZ(); $$21++) {
               for (int $$22 = $$8.minY(); $$22 <= $$8.maxY(); $$22++) {
                  for (int $$23 = $$8.minX(); $$23 <= $$8.maxX(); $$23++) {
                     BlockPos $$24 = new BlockPos($$23, $$22, $$21);
                     BlockPos $$25 = $$24.offset($$20);
                     BlockInWorld $$26 = new BlockInWorld($$12, $$24, false);
                     BlockState $$27 = $$26.getState();
                     if (pFilter.test($$26)) {
                        BlockEntity $$28 = $$12.getBlockEntity($$24);
                        if ($$28 != null) {
                           CloneCommands.CloneBlockEntityInfo $$29 = new CloneCommands.CloneBlockEntityInfo(
                              $$28.saveCustomOnly(pSource.registryAccess()), $$28.components()
                           );
                           $$17.add(new CloneCommands.CloneBlockInfo($$25, $$27, $$29));
                           $$19.addLast($$24);
                        } else if (!$$27.isSolidRender($$12, $$24) && !$$27.isCollisionShapeFullBlock($$12, $$24)) {
                           $$18.add(new CloneCommands.CloneBlockInfo($$25, $$27, null));
                           $$19.addFirst($$24);
                        } else {
                           $$16.add(new CloneCommands.CloneBlockInfo($$25, $$27, null));
                           $$19.addLast($$24);
                        }
                     }
                  }
               }
            }

            if (pMode == CloneCommands.Mode.MOVE) {
               for (BlockPos $$30 : $$19) {
                  BlockEntity $$31 = $$12.getBlockEntity($$30);
                  Clearable.tryClear($$31);
                  $$12.setBlock($$30, Blocks.BARRIER.defaultBlockState(), 2);
               }

               for (BlockPos $$32 : $$19) {
                  $$12.setBlock($$32, Blocks.AIR.defaultBlockState(), 3);
               }
            }

            List<CloneCommands.CloneBlockInfo> $$33 = Lists.newArrayList();
            $$33.addAll($$16);
            $$33.addAll($$17);
            $$33.addAll($$18);
            List<CloneCommands.CloneBlockInfo> $$34 = Lists.reverse($$33);

            for (CloneCommands.CloneBlockInfo $$35 : $$34) {
               BlockEntity $$36 = $$13.getBlockEntity($$35.pos);
               Clearable.tryClear($$36);
               $$13.setBlock($$35.pos, Blocks.BARRIER.defaultBlockState(), 2);
            }

            int $$37 = 0;

            for (CloneCommands.CloneBlockInfo $$38 : $$33) {
               if ($$13.setBlock($$38.pos, $$38.state, 2)) {
                  $$37++;
               }
            }

            for (CloneCommands.CloneBlockInfo $$39 : $$17) {
               BlockEntity $$40 = $$13.getBlockEntity($$39.pos);
               if ($$39.blockEntityInfo != null && $$40 != null) {
                  $$40.loadCustomOnly($$39.blockEntityInfo.tag, $$13.registryAccess());
                  $$40.setComponents($$39.blockEntityInfo.components);
                  $$40.setChanged();
               }

               $$13.setBlock($$39.pos, $$39.state, 2);
            }

            for (CloneCommands.CloneBlockInfo $$41 : $$34) {
               $$13.blockUpdated($$41.pos, $$41.state.getBlock());
            }

            $$13.getBlockTicks().copyAreaFrom($$12.getBlockTicks(), $$8, $$20);
            if ($$37 == 0) {
               throw ERROR_FAILED.create();
            } else {
               int $$42 = $$37;
               pSource.sendSuccess(() -> Component.translatable("commands.clone.success", $$42), true);
               return $$37;
            }
         } else {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
         }
      }
   }

   static record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
   }

   static record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo) {
   }

   @FunctionalInterface
   interface CommandFunction<T, R> {
      R apply(T pInput) throws CommandSyntaxException;
   }

   static record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
   }

   static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      private Mode(final boolean pCanOverlap) {
         this.canOverlap = pCanOverlap;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }
   }
}
