package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_308669_, p_308670_) -> Component.translatableEscape("commands.execute.blocks.toobig", p_308669_, p_308670_)
   );
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(
      Component.translatable("commands.execute.conditional.fail")
   );
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(
      p_308680_ -> Component.translatableEscape("commands.execute.conditional.fail_count", p_308680_)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (p_308676_, p_308677_) -> Component.translatableEscape("commands.execute.function.instantiationFailure", p_308676_, p_308677_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (p_326244_, p_326245_) -> {
      ReloadableServerRegistries.Holder $$2 = ((CommandSourceStack)p_326244_.getSource()).getServer().reloadableRegistries();
      return SharedSuggestionProvider.suggestResource($$2.getKeys(Registries.PREDICATE), p_326245_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      LiteralCommandNode<CommandSourceStack> $$2 = pDispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("execute").requires(p_137197_ -> p_137197_.hasPermission(2))
      );
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                         "execute"
                                                      )
                                                      .requires(p_137103_ -> p_137103_.hasPermission(2)))
                                                   .then(Commands.literal("run").redirect(pDispatcher.getRoot())))
                                                .then(addConditionals($$2, Commands.literal("if"), true, pContext)))
                                             .then(addConditionals($$2, Commands.literal("unless"), false, pContext)))
                                          .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137299_ -> {
                                             List<CommandSourceStack> $$1 = Lists.newArrayList();

                                             for (Entity $$2x : EntityArgument.getOptionalEntities(p_137299_, "targets")) {
                                                $$1.add(((CommandSourceStack)p_137299_.getSource()).withEntity($$2x));
                                             }

                                             return $$1;
                                          }))))
                                       .then(
                                          Commands.literal("at")
                                             .then(
                                                Commands.argument("targets", EntityArgument.entities())
                                                   .fork(
                                                      $$2,
                                                      p_284653_ -> {
                                                         List<CommandSourceStack> $$1 = Lists.newArrayList();

                                                         for (Entity $$2x : EntityArgument.getOptionalEntities(p_284653_, "targets")) {
                                                            $$1.add(
                                                               ((CommandSourceStack)p_284653_.getSource())
                                                                  .withLevel((ServerLevel)$$2x.level())
                                                                  .withPosition($$2x.position())
                                                                  .withRotation($$2x.getRotationVector())
                                                            );
                                                         }

                                                         return $$1;
                                                      }
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("store").then(wrapStores($$2, Commands.literal("result"), true)))
                                          .then(wrapStores($$2, Commands.literal("success"), false))
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("positioned")
                                             .then(
                                                Commands.argument("pos", Vec3Argument.vec3())
                                                   .redirect(
                                                      $$2,
                                                      p_137295_ -> ((CommandSourceStack)p_137295_.getSource())
                                                            .withPosition(Vec3Argument.getVec3(p_137295_, "pos"))
                                                            .withAnchor(EntityAnchorArgument.Anchor.FEET)
                                                   )
                                             ))
                                          .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137293_ -> {
                                             List<CommandSourceStack> $$1 = Lists.newArrayList();

                                             for (Entity $$2x : EntityArgument.getOptionalEntities(p_137293_, "targets")) {
                                                $$1.add(((CommandSourceStack)p_137293_.getSource()).withPosition($$2x.position()));
                                             }

                                             return $$1;
                                          }))))
                                       .then(
                                          Commands.literal("over")
                                             .then(Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect($$2, p_274814_ -> {
                                                Vec3 $$1 = ((CommandSourceStack)p_274814_.getSource()).getPosition();
                                                ServerLevel $$2x = ((CommandSourceStack)p_274814_.getSource()).getLevel();
                                                double $$3 = $$1.x();
                                                double $$4 = $$1.z();
                                                if (!$$2x.hasChunk(SectionPos.blockToSectionCoord($$3), SectionPos.blockToSectionCoord($$4))) {
                                                   throw BlockPosArgument.ERROR_NOT_LOADED.create();
                                                } else {
                                                   int $$5 = $$2x.getHeight(
                                                      HeightmapTypeArgument.getHeightmap(p_274814_, "heightmap"), Mth.floor($$3), Mth.floor($$4)
                                                   );
                                                   return ((CommandSourceStack)p_274814_.getSource()).withPosition(new Vec3($$3, (double)$$5, $$4));
                                                }
                                             }))
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("rotated")
                                       .then(
                                          Commands.argument("rot", RotationArgument.rotation())
                                             .redirect(
                                                $$2,
                                                p_137291_ -> ((CommandSourceStack)p_137291_.getSource())
                                                      .withRotation(
                                                         RotationArgument.getRotation(p_137291_, "rot").getRotation((CommandSourceStack)p_137291_.getSource())
                                                      )
                                             )
                                       ))
                                    .then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork($$2, p_137289_ -> {
                                       List<CommandSourceStack> $$1 = Lists.newArrayList();

                                       for (Entity $$2x : EntityArgument.getOptionalEntities(p_137289_, "targets")) {
                                          $$1.add(((CommandSourceStack)p_137289_.getSource()).withRotation($$2x.getRotationVector()));
                                       }

                                       return $$1;
                                    })))
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("facing")
                                    .then(
                                       Commands.literal("entity")
                                          .then(
                                             Commands.argument("targets", EntityArgument.entities())
                                                .then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork($$2, p_137287_ -> {
                                                   List<CommandSourceStack> $$1 = Lists.newArrayList();
                                                   EntityAnchorArgument.Anchor $$2x = EntityAnchorArgument.getAnchor(p_137287_, "anchor");

                                                   for (Entity $$3 : EntityArgument.getOptionalEntities(p_137287_, "targets")) {
                                                      $$1.add(((CommandSourceStack)p_137287_.getSource()).facing($$3, $$2x));
                                                   }

                                                   return $$1;
                                                }))
                                          )
                                    ))
                                 .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                       .redirect($$2, p_137285_ -> ((CommandSourceStack)p_137285_.getSource()).facing(Vec3Argument.getVec3(p_137285_, "pos")))
                                 )
                           ))
                        .then(
                           Commands.literal("align")
                              .then(
                                 Commands.argument("axes", SwizzleArgument.swizzle())
                                    .redirect(
                                       $$2,
                                       p_137283_ -> ((CommandSourceStack)p_137283_.getSource())
                                             .withPosition(
                                                ((CommandSourceStack)p_137283_.getSource()).getPosition().align(SwizzleArgument.getSwizzle(p_137283_, "axes"))
                                             )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("anchored")
                           .then(
                              Commands.argument("anchor", EntityAnchorArgument.anchor())
                                 .redirect(
                                    $$2,
                                    p_137281_ -> ((CommandSourceStack)p_137281_.getSource()).withAnchor(EntityAnchorArgument.getAnchor(p_137281_, "anchor"))
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("in")
                        .then(
                           Commands.argument("dimension", DimensionArgument.dimension())
                              .redirect(
                                 $$2,
                                 p_137279_ -> ((CommandSourceStack)p_137279_.getSource()).withLevel(DimensionArgument.getDimension(p_137279_, "dimension"))
                              )
                        )
                  ))
               .then(
                  Commands.literal("summon")
                     .then(
                        Commands.argument("entity", ResourceArgument.resource(pContext, Registries.ENTITY_TYPE))
                           .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                           .redirect(
                              $$2,
                              p_269759_ -> spawnEntityAndRedirect(
                                    (CommandSourceStack)p_269759_.getSource(), ResourceArgument.getSummonableEntityType(p_269759_, "entity")
                                 )
                           )
                     )
               ))
            .then(createRelationOperations($$2, Commands.literal("on")))
      );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(
      LiteralCommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pStoringResult
   ) {
      pLiteral.then(
         Commands.literal("score")
            .then(
               Commands.argument("targets", ScoreHolderArgument.scoreHolders())
                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                  .then(
                     Commands.argument("objective", ObjectiveArgument.objective())
                        .redirect(
                           pParent,
                           p_137271_ -> storeValue(
                                 (CommandSourceStack)p_137271_.getSource(),
                                 ScoreHolderArgument.getNamesWithDefaultWildcard(p_137271_, "targets"),
                                 ObjectiveArgument.getObjective(p_137271_, "objective"),
                                 pStoringResult
                              )
                        )
                  )
            )
      );
      pLiteral.then(
         Commands.literal("bossbar")
            .then(
               ((RequiredArgumentBuilder)Commands.argument("id", ResourceLocationArgument.id())
                     .suggests(BossBarCommands.SUGGEST_BOSS_BAR)
                     .then(
                        Commands.literal("value")
                           .redirect(
                              pParent,
                              p_137259_ -> storeValue((CommandSourceStack)p_137259_.getSource(), BossBarCommands.getBossBar(p_137259_), true, pStoringResult)
                           )
                     ))
                  .then(
                     Commands.literal("max")
                        .redirect(
                           pParent,
                           p_137247_ -> storeValue((CommandSourceStack)p_137247_.getSource(), BossBarCommands.getBossBar(p_137247_), false, pStoringResult)
                        )
                  )
            )
      );

      for (DataCommands.DataProvider $$3 : DataCommands.TARGET_PROVIDERS) {
         $$3.wrap(
            pLiteral,
            p_137101_ -> p_137101_.then(
                  ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                       "path", NbtPathArgument.nbtPath()
                                    )
                                    .then(
                                       Commands.literal("int")
                                          .then(
                                             Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .redirect(
                                                   pParent,
                                                   p_180216_ -> storeData(
                                                         (CommandSourceStack)p_180216_.getSource(),
                                                         $$3.access(p_180216_),
                                                         NbtPathArgument.getPath(p_180216_, "path"),
                                                         p_180219_ -> IntTag.valueOf(
                                                               (int)((double)p_180219_ * DoubleArgumentType.getDouble(p_180216_, "scale"))
                                                            ),
                                                         pStoringResult
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("float")
                                       .then(
                                          Commands.argument("scale", DoubleArgumentType.doubleArg())
                                             .redirect(
                                                pParent,
                                                p_180209_ -> storeData(
                                                      (CommandSourceStack)p_180209_.getSource(),
                                                      $$3.access(p_180209_),
                                                      NbtPathArgument.getPath(p_180209_, "path"),
                                                      p_180212_ -> FloatTag.valueOf(
                                                            (float)((double)p_180212_ * DoubleArgumentType.getDouble(p_180209_, "scale"))
                                                         ),
                                                      pStoringResult
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("short")
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .redirect(
                                             pParent,
                                             p_180199_ -> storeData(
                                                   (CommandSourceStack)p_180199_.getSource(),
                                                   $$3.access(p_180199_),
                                                   NbtPathArgument.getPath(p_180199_, "path"),
                                                   p_180202_ -> ShortTag.valueOf(
                                                         (short)((int)((double)p_180202_ * DoubleArgumentType.getDouble(p_180199_, "scale")))
                                                      ),
                                                   pStoringResult
                                                )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("long")
                                 .then(
                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                       .redirect(
                                          pParent,
                                          p_180189_ -> storeData(
                                                (CommandSourceStack)p_180189_.getSource(),
                                                $$3.access(p_180189_),
                                                NbtPathArgument.getPath(p_180189_, "path"),
                                                p_180192_ -> LongTag.valueOf((long)((double)p_180192_ * DoubleArgumentType.getDouble(p_180189_, "scale"))),
                                                pStoringResult
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("double")
                              .then(
                                 Commands.argument("scale", DoubleArgumentType.doubleArg())
                                    .redirect(
                                       pParent,
                                       p_180179_ -> storeData(
                                             (CommandSourceStack)p_180179_.getSource(),
                                             $$3.access(p_180179_),
                                             NbtPathArgument.getPath(p_180179_, "path"),
                                             p_180182_ -> DoubleTag.valueOf((double)p_180182_ * DoubleArgumentType.getDouble(p_180179_, "scale")),
                                             pStoringResult
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("byte")
                           .then(
                              Commands.argument("scale", DoubleArgumentType.doubleArg())
                                 .redirect(
                                    pParent,
                                    p_180156_ -> storeData(
                                          (CommandSourceStack)p_180156_.getSource(),
                                          $$3.access(p_180156_),
                                          NbtPathArgument.getPath(p_180156_, "path"),
                                          p_180165_ -> ByteTag.valueOf((byte)((int)((double)p_180165_ * DoubleArgumentType.getDouble(p_180156_, "scale")))),
                                          pStoringResult
                                       )
                                 )
                           )
                     )
               )
         );
      }

      return pLiteral;
   }

   private static CommandSourceStack storeValue(CommandSourceStack pSource, Collection<ScoreHolder> pTargets, Objective pObjective, boolean pStoringResult) {
      Scoreboard $$4 = pSource.getServer().getScoreboard();
      return pSource.withCallback((p_137137_, p_137138_) -> {
         for (ScoreHolder $$6 : pTargets) {
            ScoreAccess $$7 = $$4.getOrCreatePlayerScore($$6, pObjective);
            int $$8 = pStoringResult ? p_137138_ : (p_137137_ ? 1 : 0);
            $$7.set($$8);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeValue(CommandSourceStack pSource, CustomBossEvent pBar, boolean pStoringValue, boolean pStoringResult) {
      return pSource.withCallback((p_137186_, p_137187_) -> {
         int $$5 = pStoringResult ? p_137187_ : (p_137186_ ? 1 : 0);
         if (pStoringValue) {
            pBar.setValue($$5);
         } else {
            pBar.setMax($$5);
         }
      }, CommandResultCallback::chain);
   }

   private static CommandSourceStack storeData(
      CommandSourceStack pSource, DataAccessor pAccessor, NbtPathArgument.NbtPath pPath, IntFunction<Tag> pTagConverter, boolean pStoringResult
   ) {
      return pSource.withCallback((p_137154_, p_137155_) -> {
         try {
            CompoundTag $$6 = pAccessor.getData();
            int $$7 = pStoringResult ? p_137155_ : (p_137154_ ? 1 : 0);
            pPath.set($$6, pTagConverter.apply($$7));
            pAccessor.setData($$6);
         } catch (CommandSyntaxException var8) {
         }
      }, CommandResultCallback::chain);
   }

   private static boolean isChunkLoaded(ServerLevel pLevel, BlockPos pPos) {
      ChunkPos $$2 = new ChunkPos(pPos);
      LevelChunk $$3 = pLevel.getChunkSource().getChunkNow($$2.x, $$2.z);
      return $$3 == null ? false : $$3.getFullStatus() == FullChunkStatus.ENTITY_TICKING && pLevel.areEntitiesLoaded($$2.toLong());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(
      CommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pIsIf, CommandBuildContext pContext
   ) {
      ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)pLiteral.then(
                                    Commands.literal("block")
                                       .then(
                                          Commands.argument("pos", BlockPosArgument.blockPos())
                                             .then(
                                                addConditional(
                                                   pParent,
                                                   Commands.argument("block", BlockPredicateArgument.blockPredicate(pContext)),
                                                   pIsIf,
                                                   p_137277_ -> BlockPredicateArgument.getBlockPredicate(p_137277_, "block")
                                                         .test(
                                                            new BlockInWorld(
                                                               ((CommandSourceStack)p_137277_.getSource()).getLevel(),
                                                               BlockPosArgument.getLoadedBlockPos(p_137277_, "pos"),
                                                               true
                                                            )
                                                         )
                                                )
                                             )
                                       )
                                 ))
                                 .then(
                                    Commands.literal("biome")
                                       .then(
                                          Commands.argument("pos", BlockPosArgument.blockPos())
                                             .then(
                                                addConditional(
                                                   pParent,
                                                   Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(pContext, Registries.BIOME)),
                                                   pIsIf,
                                                   p_308679_ -> ResourceOrTagArgument.getResourceOrTag(p_308679_, "biome", Registries.BIOME)
                                                         .test(
                                                            ((CommandSourceStack)p_308679_.getSource())
                                                               .getLevel()
                                                               .getBiome(BlockPosArgument.getLoadedBlockPos(p_308679_, "pos"))
                                                         )
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("loaded")
                                    .then(
                                       addConditional(
                                          pParent,
                                          Commands.argument("pos", BlockPosArgument.blockPos()),
                                          pIsIf,
                                          p_269757_ -> isChunkLoaded(
                                                ((CommandSourceStack)p_269757_.getSource()).getLevel(), BlockPosArgument.getBlockPos(p_269757_, "pos")
                                             )
                                       )
                                    )
                              ))
                           .then(
                              Commands.literal("dimension")
                                 .then(
                                    addConditional(
                                       pParent,
                                       Commands.argument("dimension", DimensionArgument.dimension()),
                                       pIsIf,
                                       p_264789_ -> DimensionArgument.getDimension(p_264789_, "dimension")
                                             == ((CommandSourceStack)p_264789_.getSource()).getLevel()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("score")
                              .then(
                                 Commands.argument("target", ScoreHolderArgument.scoreHolder())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .then(
                                       ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                            "targetObjective", ObjectiveArgument.objective()
                                                         )
                                                         .then(
                                                            Commands.literal("=")
                                                               .then(
                                                                  Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                     .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                     .then(
                                                                        addConditional(
                                                                           pParent,
                                                                           Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                           pIsIf,
                                                                           p_308678_ -> checkScore(p_308678_, (p_308655_, p_308656_) -> p_308655_ == p_308656_)
                                                                        )
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("<")
                                                            .then(
                                                               Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                                  .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                                  .then(
                                                                     addConditional(
                                                                        pParent,
                                                                        Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                        pIsIf,
                                                                        p_308668_ -> checkScore(p_308668_, (p_308681_, p_308682_) -> p_308681_ < p_308682_)
                                                                     )
                                                                  )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("<=")
                                                         .then(
                                                            Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                               .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                               .then(
                                                                  addConditional(
                                                                     pParent,
                                                                     Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                     pIsIf,
                                                                     p_308657_ -> checkScore(p_308657_, (p_308658_, p_308659_) -> p_308658_ <= p_308659_)
                                                                  )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal(">")
                                                      .then(
                                                         Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                            .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                            .then(
                                                               addConditional(
                                                                  pParent,
                                                                  Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                                  pIsIf,
                                                                  p_308665_ -> checkScore(p_308665_, (p_308660_, p_308661_) -> p_308660_ > p_308661_)
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal(">=")
                                                   .then(
                                                      Commands.argument("source", ScoreHolderArgument.scoreHolder())
                                                         .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                                         .then(
                                                            addConditional(
                                                               pParent,
                                                               Commands.argument("sourceObjective", ObjectiveArgument.objective()),
                                                               pIsIf,
                                                               p_308683_ -> checkScore(p_308683_, (p_308666_, p_308667_) -> p_308666_ >= p_308667_)
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("matches")
                                                .then(
                                                   addConditional(
                                                      pParent,
                                                      Commands.argument("range", RangeArgument.intRange()),
                                                      pIsIf,
                                                      p_137216_ -> checkScore(p_137216_, RangeArgument.Ints.getRange(p_137216_, "range"))
                                                   )
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("blocks")
                           .then(
                              Commands.argument("start", BlockPosArgument.blockPos())
                                 .then(
                                    Commands.argument("end", BlockPosArgument.blockPos())
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                                                .then(addIfBlocksConditional(pParent, Commands.literal("all"), pIsIf, false)))
                                             .then(addIfBlocksConditional(pParent, Commands.literal("masked"), pIsIf, true))
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("entity")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("entities", EntityArgument.entities())
                                 .fork(pParent, p_137232_ -> expect(p_137232_, pIsIf, !EntityArgument.getOptionalEntities(p_137232_, "entities").isEmpty())))
                              .executes(createNumericConditionalHandler(pIsIf, p_137189_ -> EntityArgument.getOptionalEntities(p_137189_, "entities").size()))
                        )
                  ))
               .then(
                  Commands.literal("predicate")
                     .then(
                        addConditional(
                           pParent,
                           Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(pContext)).suggests(SUGGEST_PREDICATE),
                           pIsIf,
                           p_326238_ -> checkCustomPredicate(
                                 (CommandSourceStack)p_326238_.getSource(), ResourceOrIdArgument.getLootPredicate(p_326238_, "predicate")
                              )
                        )
                     )
               ))
            .then(
               Commands.literal("function")
                  .then(
                     Commands.argument("name", FunctionArgument.functions())
                        .suggests(FunctionCommand.SUGGEST_FUNCTION)
                        .fork(pParent, new ExecuteCommand.ExecuteIfFunctionCustomModifier(pIsIf))
                  )
            ))
         .then(
            ((LiteralArgumentBuilder)Commands.literal("items")
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("entities", EntityArgument.entities())
                              .then(
                                 Commands.argument("slots", SlotsArgument.slots())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(pContext))
                                             .fork(
                                                pParent,
                                                p_326243_ -> expect(
                                                      p_326243_,
                                                      pIsIf,
                                                      countItems(
                                                            EntityArgument.getEntities(p_326243_, "entities"),
                                                            SlotsArgument.getSlots(p_326243_, "slots"),
                                                            ItemPredicateArgument.getItemPredicate(p_326243_, "item_predicate")
                                                         )
                                                         > 0
                                                   )
                                             ))
                                          .executes(
                                             createNumericConditionalHandler(
                                                pIsIf,
                                                p_326239_ -> countItems(
                                                      EntityArgument.getEntities(p_326239_, "entities"),
                                                      SlotsArgument.getSlots(p_326239_, "slots"),
                                                      ItemPredicateArgument.getItemPredicate(p_326239_, "item_predicate")
                                                   )
                                             )
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("block")
                     .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                           .then(
                              Commands.argument("slots", SlotsArgument.slots())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(pContext))
                                          .fork(
                                             pParent,
                                             p_326241_ -> expect(
                                                   p_326241_,
                                                   pIsIf,
                                                   countItems(
                                                         (CommandSourceStack)p_326241_.getSource(),
                                                         BlockPosArgument.getLoadedBlockPos(p_326241_, "pos"),
                                                         SlotsArgument.getSlots(p_326241_, "slots"),
                                                         ItemPredicateArgument.getItemPredicate(p_326241_, "item_predicate")
                                                      )
                                                      > 0
                                                )
                                          ))
                                       .executes(
                                          createNumericConditionalHandler(
                                             pIsIf,
                                             p_326246_ -> countItems(
                                                   (CommandSourceStack)p_326246_.getSource(),
                                                   BlockPosArgument.getLoadedBlockPos(p_326246_, "pos"),
                                                   SlotsArgument.getSlots(p_326246_, "slots"),
                                                   ItemPredicateArgument.getItemPredicate(p_326246_, "item_predicate")
                                                )
                                          )
                                       )
                                 )
                           )
                     )
               )
         );

      for (DataCommands.DataProvider $$4 : DataCommands.SOURCE_PROVIDERS) {
         pLiteral.then(
            $$4.wrap(
               Commands.literal("data"),
               p_137092_ -> p_137092_.then(
                     ((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath())
                           .fork(
                              pParent,
                              p_180175_ -> expect(p_180175_, pIsIf, checkMatchingData($$4.access(p_180175_), NbtPathArgument.getPath(p_180175_, "path")) > 0)
                           ))
                        .executes(
                           createNumericConditionalHandler(
                              pIsIf, p_180152_ -> checkMatchingData($$4.access(p_180152_), NbtPathArgument.getPath(p_180152_, "path"))
                           )
                        )
                  )
            )
         );
      }

      return pLiteral;
   }

   private static int countItems(Iterable<? extends Entity> pTargets, SlotRange pSlotRange, Predicate<ItemStack> pFilter) {
      int $$3 = 0;

      for (Entity $$4 : pTargets) {
         IntList $$5 = pSlotRange.slots();

         for (int $$6 = 0; $$6 < $$5.size(); $$6++) {
            int $$7 = $$5.getInt($$6);
            SlotAccess $$8 = $$4.getSlot($$7);
            ItemStack $$9 = $$8.get();
            if (pFilter.test($$9)) {
               $$3 += $$9.getCount();
            }
         }
      }

      return $$3;
   }

   private static int countItems(CommandSourceStack pSource, BlockPos pPos, SlotRange pSlotRange, Predicate<ItemStack> pFilter) throws CommandSyntaxException {
      int $$4 = 0;
      Container $$5 = ItemCommands.getContainer(pSource, pPos, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
      int $$6 = $$5.getContainerSize();
      IntList $$7 = pSlotRange.slots();

      for (int $$8 = 0; $$8 < $$7.size(); $$8++) {
         int $$9 = $$7.getInt($$8);
         if ($$9 >= 0 && $$9 < $$6) {
            ItemStack $$10 = $$5.getItem($$9);
            if (pFilter.test($$10)) {
               $$4 += $$10.getCount();
            }
         }
      }

      return $$4;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean pIsIf, ExecuteCommand.CommandNumericPredicate pPredicate) {
      return pIsIf ? p_288391_ -> {
         int $$2 = pPredicate.test(p_288391_);
         if ($$2 > 0) {
            ((CommandSourceStack)p_288391_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", $$2), false);
            return $$2;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : p_288393_ -> {
         int $$2 = pPredicate.test(p_288393_);
         if ($$2 == 0) {
            ((CommandSourceStack)p_288393_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create($$2);
         }
      };
   }

   private static int checkMatchingData(DataAccessor pAccessor, NbtPathArgument.NbtPath pPath) throws CommandSyntaxException {
      return pPath.countMatching(pAccessor.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> pSource, ExecuteCommand.IntBiPredicate pPredicate) throws CommandSyntaxException {
      ScoreHolder $$2 = ScoreHolderArgument.getName(pSource, "target");
      Objective $$3 = ObjectiveArgument.getObjective(pSource, "targetObjective");
      ScoreHolder $$4 = ScoreHolderArgument.getName(pSource, "source");
      Objective $$5 = ObjectiveArgument.getObjective(pSource, "sourceObjective");
      Scoreboard $$6 = ((CommandSourceStack)pSource.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo $$7 = $$6.getPlayerScoreInfo($$2, $$3);
      ReadOnlyScoreInfo $$8 = $$6.getPlayerScoreInfo($$4, $$5);
      return $$7 != null && $$8 != null ? pPredicate.test($$7.value(), $$8.value()) : false;
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> pContext, MinMaxBounds.Ints pBounds) throws CommandSyntaxException {
      ScoreHolder $$2 = ScoreHolderArgument.getName(pContext, "target");
      Objective $$3 = ObjectiveArgument.getObjective(pContext, "targetObjective");
      Scoreboard $$4 = ((CommandSourceStack)pContext.getSource()).getServer().getScoreboard();
      ReadOnlyScoreInfo $$5 = $$4.getPlayerScoreInfo($$2, $$3);
      return $$5 == null ? false : pBounds.matches($$5.value());
   }

   private static boolean checkCustomPredicate(CommandSourceStack pSource, Holder<LootItemCondition> pCondition) {
      ServerLevel $$2 = pSource.getLevel();
      LootParams $$3 = new LootParams.Builder($$2)
         .withParameter(LootContextParams.ORIGIN, pSource.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, pSource.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext $$4 = new LootContext.Builder($$3).create(Optional.empty());
      $$4.pushVisitedElement(LootContext.createVisitedEntry(pCondition.value()));
      return pCondition.value().test($$4);
   }

   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> pContext, boolean pActual, boolean pExpected) {
      return (Collection<CommandSourceStack>)(pExpected == pActual ? Collections.singleton((CommandSourceStack)pContext.getSource()) : Collections.emptyList());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(
      CommandNode<CommandSourceStack> pCommandNode, ArgumentBuilder<CommandSourceStack, ?> pBuilder, boolean pValue, ExecuteCommand.CommandPredicate pTest
   ) {
      return pBuilder.fork(pCommandNode, p_137214_ -> expect(p_137214_, pValue, pTest.test(p_137214_))).executes(p_288396_ -> {
         if (pValue == pTest.test(p_288396_)) {
            ((CommandSourceStack)p_288396_.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(
      CommandNode<CommandSourceStack> pCommandNode, ArgumentBuilder<CommandSourceStack, ?> pLiteral, boolean pIsIf, boolean pIsMasked
   ) {
      return pLiteral.fork(pCommandNode, p_137180_ -> expect(p_137180_, pIsIf, checkRegions(p_137180_, pIsMasked).isPresent()))
         .executes(pIsIf ? p_137210_ -> checkIfRegions(p_137210_, pIsMasked) : p_137165_ -> checkUnlessRegions(p_137165_, pIsMasked));
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      OptionalInt $$2 = checkRegions(pContext, pIsMasked);
      if ($$2.isPresent()) {
         ((CommandSourceStack)pContext.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", $$2.getAsInt()), false);
         return $$2.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      OptionalInt $$2 = checkRegions(pContext, pIsMasked);
      if ($$2.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create($$2.getAsInt());
      } else {
         ((CommandSourceStack)pContext.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
         return 1;
      }
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      return checkRegions(
         ((CommandSourceStack)pContext.getSource()).getLevel(),
         BlockPosArgument.getLoadedBlockPos(pContext, "start"),
         BlockPosArgument.getLoadedBlockPos(pContext, "end"),
         BlockPosArgument.getLoadedBlockPos(pContext, "destination"),
         pIsMasked
      );
   }

   private static OptionalInt checkRegions(ServerLevel pLevel, BlockPos pBegin, BlockPos pEnd, BlockPos pDestination, boolean pIsMasked) throws CommandSyntaxException {
      BoundingBox $$5 = BoundingBox.fromCorners(pBegin, pEnd);
      BoundingBox $$6 = BoundingBox.fromCorners(pDestination, pDestination.offset($$5.getLength()));
      BlockPos $$7 = new BlockPos($$6.minX() - $$5.minX(), $$6.minY() - $$5.minY(), $$6.minZ() - $$5.minZ());
      int $$8 = $$5.getXSpan() * $$5.getYSpan() * $$5.getZSpan();
      if ($$8 > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, $$8);
      } else {
         RegistryAccess $$9 = pLevel.registryAccess();
         int $$10 = 0;

         for (int $$11 = $$5.minZ(); $$11 <= $$5.maxZ(); $$11++) {
            for (int $$12 = $$5.minY(); $$12 <= $$5.maxY(); $$12++) {
               for (int $$13 = $$5.minX(); $$13 <= $$5.maxX(); $$13++) {
                  BlockPos $$14 = new BlockPos($$13, $$12, $$11);
                  BlockPos $$15 = $$14.offset($$7);
                  BlockState $$16 = pLevel.getBlockState($$14);
                  if (!pIsMasked || !$$16.is(Blocks.AIR)) {
                     if ($$16 != pLevel.getBlockState($$15)) {
                        return OptionalInt.empty();
                     }

                     BlockEntity $$17 = pLevel.getBlockEntity($$14);
                     BlockEntity $$18 = pLevel.getBlockEntity($$15);
                     if ($$17 != null) {
                        if ($$18 == null) {
                           return OptionalInt.empty();
                        }

                        if ($$18.getType() != $$17.getType()) {
                           return OptionalInt.empty();
                        }

                        if (!$$17.components().equals($$18.components())) {
                           return OptionalInt.empty();
                        }

                        CompoundTag $$19 = $$17.saveCustomOnly($$9);
                        CompoundTag $$20 = $$18.saveCustomOnly($$9);
                        if (!$$19.equals($$20)) {
                           return OptionalInt.empty();
                        }
                     }

                     $$10++;
                  }
               }
            }
         }

         return OptionalInt.of($$10);
      }
   }

   private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> pRelation) {
      return p_264786_ -> {
         CommandSourceStack $$2 = (CommandSourceStack)p_264786_.getSource();
         Entity $$3 = $$2.getEntity();
         return (Collection)($$3 == null
            ? List.of()
            : pRelation.apply($$3).filter(p_264783_ -> !p_264783_.isRemoved()).map(p_264775_ -> List.of($$2.withEntity(p_264775_))).orElse(List.of()));
      };
   }

   private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> pRelation) {
      return p_264780_ -> {
         CommandSourceStack $$2 = (CommandSourceStack)p_264780_.getSource();
         Entity $$3 = $$2.getEntity();
         return $$3 == null ? List.of() : pRelation.apply($$3).filter(p_264784_ -> !p_264784_.isRemoved()).map($$2::withEntity).toList();
      };
   }

   private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(
      CommandNode<CommandSourceStack> pNode, LiteralArgumentBuilder<CommandSourceStack> pArgumentBuilder
   ) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)pArgumentBuilder.then(
                              Commands.literal("owner")
                                 .fork(
                                    pNode,
                                    expandOneToOneEntityRelation(
                                       p_269758_ -> p_269758_ instanceof OwnableEntity $$1 ? Optional.ofNullable($$1.getOwner()) : Optional.empty()
                                    )
                                 )
                           ))
                           .then(
                              Commands.literal("leasher")
                                 .fork(
                                    pNode,
                                    expandOneToOneEntityRelation(
                                       p_341160_ -> p_341160_ instanceof Leashable $$1 ? Optional.ofNullable($$1.getLeashHolder()) : Optional.empty()
                                    )
                                 )
                           ))
                        .then(
                           Commands.literal("target")
                              .fork(
                                 pNode,
                                 expandOneToOneEntityRelation(
                                    p_272389_ -> p_272389_ instanceof Targeting $$1 ? Optional.ofNullable($$1.getTarget()) : Optional.empty()
                                 )
                              )
                        ))
                     .then(
                        Commands.literal("attacker")
                           .fork(
                              pNode,
                              expandOneToOneEntityRelation(
                                 p_272388_ -> p_272388_ instanceof Attackable $$1 ? Optional.ofNullable($$1.getLastAttacker()) : Optional.empty()
                              )
                           )
                     ))
                  .then(Commands.literal("vehicle").fork(pNode, expandOneToOneEntityRelation(p_264776_ -> Optional.ofNullable(p_264776_.getVehicle())))))
               .then(
                  Commands.literal("controller")
                     .fork(pNode, expandOneToOneEntityRelation(p_274815_ -> Optional.ofNullable(p_274815_.getControllingPassenger())))
               ))
            .then(
               Commands.literal("origin")
                  .fork(
                     pNode,
                     expandOneToOneEntityRelation(
                        p_266631_ -> p_266631_ instanceof TraceableEntity $$1 ? Optional.ofNullable($$1.getOwner()) : Optional.empty()
                     )
                  )
            ))
         .then(Commands.literal("passengers").fork(pNode, expandOneToManyEntityRelation(p_264777_ -> p_264777_.getPassengers().stream())));
   }

   private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack pSource, Holder.Reference<EntityType<?>> pEntityType) throws CommandSyntaxException {
      Entity $$2 = SummonCommand.createEntity(pSource, pEntityType, pSource.getPosition(), new CompoundTag(), true);
      return pSource.withEntity($$2);
   }

   public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(
      T pOriginalSource,
      List<T> pSources,
      Function<T, T> pSourceModifier,
      IntPredicate pSuccessCheck,
      ContextChain<T> pContextChain,
      @Nullable CompoundTag pArguments,
      ExecutionControl<T> pExecutionControl,
      ExecuteCommand.CommandGetter<T, Collection<CommandFunction<T>>> pFunctions,
      ChainModifiers pChainModifiers
   ) {
      List<T> $$9 = new ArrayList<>(pSources.size());

      Collection<CommandFunction<T>> $$10;
      try {
         $$10 = pFunctions.get(pContextChain.getTopContext().copyFor(pOriginalSource));
      } catch (CommandSyntaxException var18) {
         pOriginalSource.handleError(var18, pChainModifiers.isForked(), pExecutionControl.tracer());
         return;
      }

      int $$13 = $$10.size();
      if ($$13 != 0) {
         List<InstantiatedFunction<T>> $$14 = new ArrayList<>($$13);

         try {
            for (CommandFunction<T> $$15 : $$10) {
               try {
                  $$14.add($$15.instantiate(pArguments, pOriginalSource.dispatcher()));
               } catch (FunctionInstantiationException var17) {
                  throw ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create($$15.id(), var17.messageComponent());
               }
            }
         } catch (CommandSyntaxException var19) {
            pOriginalSource.handleError(var19, pChainModifiers.isForked(), pExecutionControl.tracer());
         }

         for (T $$18 : pSources) {
            T $$19 = (T)pSourceModifier.apply($$18.clearCallbacks());
            CommandResultCallback $$20 = (p_308674_, p_308675_) -> {
               if (pSuccessCheck.test(p_308675_)) {
                  $$9.add($$18);
               }
            };
            pExecutionControl.queueNext(new IsolatedCall<>(p_308664_ -> {
               for (InstantiatedFunction<T> $$3 : $$14) {
                  p_308664_.queueNext(new CallFunction<>($$3, p_308664_.currentFrame().returnValueConsumer(), true).bind($$19));
               }

               p_308664_.queueNext(FallthroughTask.instance());
            }, $$20));
         }

         ContextChain<T> $$21 = pContextChain.nextStage();
         String $$22 = pContextChain.getTopContext().getInput();
         pExecutionControl.queueNext(new BuildContexts.Continuation<>($$22, $$21, pChainModifiers, pOriginalSource, $$9));
      }
   }

   @FunctionalInterface
   public interface CommandGetter<T, R> {
      R get(CommandContext<T> pContext) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }

   static class ExecuteIfFunctionCustomModifier implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
      private final IntPredicate check;

      ExecuteIfFunctionCustomModifier(boolean pInvert) {
         this.check = pInvert ? p_311044_ -> p_311044_ != 0 : p_311222_ -> p_311222_ == 0;
      }

      public void apply(
         CommandSourceStack pOriginalSource,
         List<CommandSourceStack> pSoruces,
         ContextChain<CommandSourceStack> pContextChain,
         ChainModifiers pChainModifiers,
         ExecutionControl<CommandSourceStack> pExecutionControl
      ) {
         ExecuteCommand.scheduleFunctionConditionsAndTest(
            pOriginalSource,
            pSoruces,
            FunctionCommand::modifySenderForExecution,
            this.check,
            pContextChain,
            null,
            pExecutionControl,
            p_312564_ -> FunctionArgument.getFunctions(p_312564_, "name"),
            pChainModifiers
         );
      }
   }

   @FunctionalInterface
   interface IntBiPredicate {
      boolean test(int pValue1, int pValue2);
   }
}
