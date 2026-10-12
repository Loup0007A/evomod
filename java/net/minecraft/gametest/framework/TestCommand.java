package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class TestCommand {
   public static final int STRUCTURE_BLOCK_NEARBY_SEARCH_RADIUS = 15;
   public static final int STRUCTURE_BLOCK_FULL_SEARCH_RADIUS = 200;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_CLEAR_RADIUS = 200;
   private static final int MAX_CLEAR_RADIUS = 1024;
   private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
   private static final int SHOW_POS_DURATION_MS = 10000;
   private static final int DEFAULT_X_SIZE = 5;
   private static final int DEFAULT_Y_SIZE = 5;
   private static final int DEFAULT_Z_SIZE = 5;
   private static final String STRUCTURE_BLOCK_ENTITY_COULD_NOT_BE_FOUND = "Structure block entity could not be found";
   private static final TestFinder.Builder<TestCommand.Runner> testFinder = new TestFinder.Builder<>(TestCommand.Runner::new);

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder,
      Function<CommandContext<CommandSourceStack>, TestCommand.Runner> pRunnerGetter,
      Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> pModifier
   ) {
      return pArgumentBuilder.executes(p_325991_ -> pRunnerGetter.apply(p_325991_).run())
         .then(
            ((RequiredArgumentBuilder)Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                  .executes(
                     p_325975_ -> pRunnerGetter.apply(p_325975_).run(new RetryOptions(IntegerArgumentType.getInteger(p_325975_, "numberOfTimes"), false))
                  ))
               .then(
                  pModifier.apply(
                     Commands.argument("untilFailed", BoolArgumentType.bool())
                        .executes(
                           p_325980_ -> pRunnerGetter.apply(p_325980_)
                                 .run(
                                    new RetryOptions(
                                       IntegerArgumentType.getInteger(p_325980_, "numberOfTimes"), BoolArgumentType.getBool(p_325980_, "untilFailed")
                                    )
                                 )
                        )
                  )
               )
         );
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
      ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, Function<CommandContext<CommandSourceStack>, TestCommand.Runner> pRunnerGetter
   ) {
      return runWithRetryOptions(pArgumentBuilder, pRunnerGetter, p_325997_ -> p_325997_);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
      ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, Function<CommandContext<CommandSourceStack>, TestCommand.Runner> pRunnerGetter
   ) {
      return runWithRetryOptions(
         pArgumentBuilder,
         pRunnerGetter,
         p_325993_ -> p_325993_.then(
               ((RequiredArgumentBuilder)Commands.argument("rotationSteps", IntegerArgumentType.integer())
                     .executes(
                        p_326001_ -> pRunnerGetter.apply(p_326001_)
                              .run(
                                 new RetryOptions(
                                    IntegerArgumentType.getInteger(p_326001_, "numberOfTimes"), BoolArgumentType.getBool(p_326001_, "untilFailed")
                                 ),
                                 IntegerArgumentType.getInteger(p_326001_, "rotationSteps")
                              )
                     ))
                  .then(
                     Commands.argument("testsPerRow", IntegerArgumentType.integer())
                        .executes(
                           p_325977_ -> pRunnerGetter.apply(p_325977_)
                                 .run(
                                    new RetryOptions(
                                       IntegerArgumentType.getInteger(p_325977_, "numberOfTimes"), BoolArgumentType.getBool(p_325977_, "untilFailed")
                                    ),
                                    IntegerArgumentType.getInteger(p_325977_, "rotationSteps"),
                                    IntegerArgumentType.getInteger(p_325977_, "testsPerRow")
                                 )
                        )
                  )
            )
      );
   }

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      ArgumentBuilder<CommandSourceStack, ?> $$1 = runWithRetryOptionsAndBuildInfo(
         Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
         p_326015_ -> testFinder.failedTests(p_326015_, BoolArgumentType.getBool(p_326015_, "onlyRequiredTests"))
      );
      ArgumentBuilder<CommandSourceStack, ?> $$2 = runWithRetryOptionsAndBuildInfo(
         Commands.argument("testClassName", TestClassNameArgument.testClassName()),
         p_325999_ -> testFinder.allTestsInClass(p_325999_, TestClassNameArgument.getTestClassName(p_325999_, "testClassName"))
      );
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                                                    "test"
                                                                                 )
                                                                                 .then(
                                                                                    Commands.literal("run")
                                                                                       .then(
                                                                                          runWithRetryOptionsAndBuildInfo(
                                                                                             Commands.argument(
                                                                                                "testName", TestFunctionArgument.testFunctionArgument()
                                                                                             ),
                                                                                             p_325988_ -> testFinder.byArgument(p_325988_, "testName")
                                                                                          )
                                                                                       )
                                                                                 ))
                                                                              .then(
                                                                                 Commands.literal("runmultiple")
                                                                                    .then(
                                                                                       ((RequiredArgumentBuilder)Commands.argument(
                                                                                                "testName", TestFunctionArgument.testFunctionArgument()
                                                                                             )
                                                                                             .executes(
                                                                                                p_325973_ -> testFinder.byArgument(p_325973_, "testName").run()
                                                                                             ))
                                                                                          .then(
                                                                                             Commands.argument("amount", IntegerArgumentType.integer())
                                                                                                .executes(
                                                                                                   p_325995_ -> testFinder.createMultipleCopies(
                                                                                                            IntegerArgumentType.getInteger(p_325995_, "amount")
                                                                                                         )
                                                                                                         .byArgument(p_325995_, "testName")
                                                                                                         .run()
                                                                                                )
                                                                                          )
                                                                                    )
                                                                              ))
                                                                           .then(
                                                                              runWithRetryOptionsAndBuildInfo(
                                                                                 Commands.literal("runall").then($$2), testFinder::allTests
                                                                              )
                                                                           ))
                                                                        .then(runWithRetryOptions(Commands.literal("runthese"), testFinder::allNearby)))
                                                                     .then(runWithRetryOptions(Commands.literal("runclosest"), testFinder::nearest)))
                                                                  .then(runWithRetryOptions(Commands.literal("runthat"), testFinder::lookedAt)))
                                                               .then(
                                                                  runWithRetryOptionsAndBuildInfo(
                                                                     Commands.literal("runfailed").then($$1), testFinder::failedTests
                                                                  )
                                                               ))
                                                            .then(
                                                               Commands.literal("verify")
                                                                  .then(
                                                                     Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                                                                        .executes(p_341098_ -> testFinder.byArgument(p_341098_, "testName").verify())
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("verifyclass")
                                                               .then(
                                                                  Commands.argument("testClassName", TestClassNameArgument.testClassName())
                                                                     .executes(
                                                                        p_341097_ -> testFinder.allTestsInClass(
                                                                                 p_341097_, TestClassNameArgument.getTestClassName(p_341097_, "testClassName")
                                                                              )
                                                                              .verify()
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("locate")
                                                            .then(
                                                               Commands.argument("testName", TestFunctionArgument.testFunctionArgument())
                                                                  .executes(
                                                                     p_325985_ -> testFinder.locateByName(
                                                                              p_325985_,
                                                                              "minecraft:"
                                                                                 + TestFunctionArgument.getTestFunction(p_325985_, "testName").structureName()
                                                                           )
                                                                           .locate()
                                                                  )
                                                            )
                                                      ))
                                                   .then(Commands.literal("resetclosest").executes(p_325984_ -> testFinder.nearest(p_325984_).reset())))
                                                .then(Commands.literal("resetthese").executes(p_325994_ -> testFinder.allNearby(p_325994_).reset())))
                                             .then(Commands.literal("resetthat").executes(p_325983_ -> testFinder.lookedAt(p_325983_).reset())))
                                          .then(
                                             Commands.literal("export")
                                                .then(
                                                   Commands.argument("testName", StringArgumentType.word())
                                                      .executes(
                                                         p_325998_ -> exportTestStructure(
                                                               (CommandSourceStack)p_325998_.getSource(),
                                                               "minecraft:" + StringArgumentType.getString(p_325998_, "testName")
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(Commands.literal("exportclosest").executes(p_326009_ -> testFinder.nearest(p_326009_).export())))
                                    .then(Commands.literal("exportthese").executes(p_326010_ -> testFinder.allNearby(p_326010_).export())))
                                 .then(Commands.literal("exportthat").executes(p_326011_ -> testFinder.lookedAt(p_326011_).export())))
                              .then(Commands.literal("clearthat").executes(p_325987_ -> testFinder.lookedAt(p_325987_).clear())))
                           .then(Commands.literal("clearthese").executes(p_325978_ -> testFinder.allNearby(p_325978_).clear())))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("clearall").executes(p_325986_ -> testFinder.radius(p_325986_, 200).clear()))
                              .then(
                                 Commands.argument("radius", IntegerArgumentType.integer())
                                    .executes(
                                       p_325996_ -> testFinder.radius(p_325996_, Mth.clamp(IntegerArgumentType.getInteger(p_325996_, "radius"), 0, 1024))
                                             .clear()
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("import")
                           .then(
                              Commands.argument("testName", StringArgumentType.word())
                                 .executes(
                                    p_128025_ -> importTestStructure(
                                          (CommandSourceStack)p_128025_.getSource(), StringArgumentType.getString(p_128025_, "testName")
                                       )
                                 )
                           )
                     ))
                  .then(Commands.literal("stop").executes(p_326006_ -> stopTests())))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("pos").executes(p_128023_ -> showPos((CommandSourceStack)p_128023_.getSource(), "pos")))
                     .then(
                        Commands.argument("var", StringArgumentType.word())
                           .executes(p_128021_ -> showPos((CommandSourceStack)p_128021_.getSource(), StringArgumentType.getString(p_128021_, "var")))
                     )
               ))
            .then(
               Commands.literal("create")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("testName", StringArgumentType.word())
                           .suggests(TestFunctionArgument::suggestTestFunction)
                           .executes(
                              p_128019_ -> createNewStructure(
                                    (CommandSourceStack)p_128019_.getSource(), StringArgumentType.getString(p_128019_, "testName"), 5, 5, 5
                                 )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("width", IntegerArgumentType.integer())
                                 .executes(
                                    p_128014_ -> createNewStructure(
                                          (CommandSourceStack)p_128014_.getSource(),
                                          StringArgumentType.getString(p_128014_, "testName"),
                                          IntegerArgumentType.getInteger(p_128014_, "width"),
                                          IntegerArgumentType.getInteger(p_128014_, "width"),
                                          IntegerArgumentType.getInteger(p_128014_, "width")
                                       )
                                 ))
                              .then(
                                 Commands.argument("height", IntegerArgumentType.integer())
                                    .then(
                                       Commands.argument("depth", IntegerArgumentType.integer())
                                          .executes(
                                             p_128007_ -> createNewStructure(
                                                   (CommandSourceStack)p_128007_.getSource(),
                                                   StringArgumentType.getString(p_128007_, "testName"),
                                                   IntegerArgumentType.getInteger(p_128007_, "width"),
                                                   IntegerArgumentType.getInteger(p_128007_, "height"),
                                                   IntegerArgumentType.getInteger(p_128007_, "depth")
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int resetGameTestInfo(GameTestInfo pGameTestInfo) {
      pGameTestInfo.getLevel()
         .getEntities(null, pGameTestInfo.getStructureBounds())
         .stream()
         .forEach(p_325989_ -> p_325989_.remove(Entity.RemovalReason.DISCARDED));
      pGameTestInfo.getStructureBlockEntity().placeStructure(pGameTestInfo.getLevel());
      StructureUtils.removeBarriers(pGameTestInfo.getStructureBounds(), pGameTestInfo.getLevel());
      say(pGameTestInfo.getLevel(), "Reset succeded for: " + pGameTestInfo.getTestName(), ChatFormatting.GREEN);
      return 1;
   }

   static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack pSource, RetryOptions pRetryOptions, StructureBlockPosFinder pStructureBlockPosFinder) {
      return pStructureBlockPosFinder.findStructureBlockPos()
         .map(p_326014_ -> createGameTestInfo(p_326014_, pSource.getLevel(), pRetryOptions))
         .flatMap(Optional::stream);
   }

   static Stream<GameTestInfo> toGameTestInfo(
      CommandSourceStack pSource, RetryOptions pRetryOptions, TestFunctionFinder pTestFunctionFinder, int pRotationSteps
   ) {
      return pTestFunctionFinder.findTestFunctions()
         .filter(p_326008_ -> verifyStructureExists(pSource.getLevel(), p_326008_.structureName()))
         .map(p_326005_ -> new GameTestInfo(p_326005_, StructureUtils.getRotationForRotationSteps(pRotationSteps), pSource.getLevel(), pRetryOptions));
   }

   private static Optional<GameTestInfo> createGameTestInfo(BlockPos pPos, ServerLevel pLevel, RetryOptions pRetryOptions) {
      StructureBlockEntity $$3 = (StructureBlockEntity)pLevel.getBlockEntity(pPos);
      if ($$3 == null) {
         say(pLevel, "Structure block entity could not be found", ChatFormatting.RED);
         return Optional.empty();
      } else {
         String $$4 = $$3.getMetaData();
         Optional<TestFunction> $$5 = GameTestRegistry.findTestFunction($$4);
         if ($$5.isEmpty()) {
            say(pLevel, "Test function for test " + $$4 + " could not be found", ChatFormatting.RED);
            return Optional.empty();
         } else {
            TestFunction $$6 = $$5.get();
            GameTestInfo $$7 = new GameTestInfo($$6, $$3.getRotation(), pLevel, pRetryOptions);
            $$7.setStructureBlockPos(pPos);
            return !verifyStructureExists(pLevel, $$7.getStructureName()) ? Optional.empty() : Optional.of($$7);
         }
      }
   }

   private static int createNewStructure(CommandSourceStack pSource, String pStructureName, int pX, int pY, int pZ) {
      if (pX <= 48 && pY <= 48 && pZ <= 48) {
         ServerLevel $$5 = pSource.getLevel();
         BlockPos $$6 = createTestPositionAround(pSource).below();
         StructureUtils.createNewEmptyStructureBlock(pStructureName.toLowerCase(), $$6, new Vec3i(pX, pY, pZ), Rotation.NONE, $$5);
         BlockPos $$7 = $$6.above();
         BlockPos $$8 = $$7.offset(pX - 1, 0, pZ - 1);
         BlockPos.betweenClosedStream($$7, $$8).forEach(p_325982_ -> $$5.setBlockAndUpdate(p_325982_, Blocks.BEDROCK.defaultBlockState()));
         StructureUtils.addCommandBlockAndButtonToStartTest($$6, new BlockPos(1, 0, -1), Rotation.NONE, $$5);
         return 0;
      } else {
         throw new IllegalArgumentException("The structure must be less than 48 blocks big in each axis");
      }
   }

   private static int showPos(CommandSourceStack pSource, String pVariableName) throws CommandSyntaxException {
      BlockHitResult $$2 = (BlockHitResult)pSource.getPlayerOrException().pick(10.0, 1.0F, false);
      BlockPos $$3 = $$2.getBlockPos();
      ServerLevel $$4 = pSource.getLevel();
      Optional<BlockPos> $$5 = StructureUtils.findStructureBlockContainingPos($$3, 15, $$4);
      if ($$5.isEmpty()) {
         $$5 = StructureUtils.findStructureBlockContainingPos($$3, 200, $$4);
      }

      if ($$5.isEmpty()) {
         pSource.sendFailure(Component.literal("Can't find a structure block that contains the targeted pos " + $$3));
         return 0;
      } else {
         StructureBlockEntity $$6 = (StructureBlockEntity)$$4.getBlockEntity($$5.get());
         if ($$6 == null) {
            say($$4, "Structure block entity could not be found", ChatFormatting.RED);
            return 0;
         } else {
            BlockPos $$7 = $$3.subtract($$5.get());
            String $$8 = $$7.getX() + ", " + $$7.getY() + ", " + $$7.getZ();
            String $$9 = $$6.getMetaData();
            Component $$10 = Component.literal($$8)
               .setStyle(
                  Style.EMPTY
                     .withBold(true)
                     .withColor(ChatFormatting.GREEN)
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy to clipboard")))
                     .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "final BlockPos " + pVariableName + " = new BlockPos(" + $$8 + ");"))
               );
            pSource.sendSuccess(() -> Component.literal("Position relative to " + $$9 + ": ").append($$10), false);
            DebugPackets.sendGameTestAddMarker($$4, new BlockPos($$3), $$8, -2147418368, 10000);
            return 1;
         }
      }
   }

   static int stopTests() {
      GameTestTicker.SINGLETON.clear();
      return 1;
   }

   static int trackAndStartRunner(CommandSourceStack pSource, ServerLevel pLevel, GameTestRunner pRunner) {
      pRunner.addListener(new TestCommand.TestBatchSummaryDisplayer(pSource));
      MultipleTestTracker $$3 = new MultipleTestTracker(pRunner.getTestInfos());
      $$3.addListener(new TestCommand.TestSummaryDisplayer(pLevel, $$3));
      $$3.addFailureListener(p_127992_ -> GameTestRegistry.rememberFailedTest(p_127992_.getTestFunction()));
      pRunner.start();
      return 1;
   }

   static int saveAndExportTestStructure(CommandSourceStack pSource, StructureBlockEntity pStructureBlockEntity) {
      String $$2 = pStructureBlockEntity.getStructureName();
      if (!pStructureBlockEntity.saveStructure(true)) {
         say(pSource, "Failed to save structure " + $$2);
      }

      return exportTestStructure(pSource, $$2);
   }

   private static int exportTestStructure(CommandSourceStack pSource, String pStructurePath) {
      Path $$2 = Paths.get(StructureUtils.testStructuresDir);
      ResourceLocation $$3 = ResourceLocation.parse(pStructurePath);
      Path $$4 = pSource.getLevel().getStructureManager().createAndValidatePathToGeneratedStructure($$3, ".nbt");
      Path $$5 = NbtToSnbt.convertStructure(CachedOutput.NO_CACHE, $$4, $$3.getPath(), $$2);
      if ($$5 == null) {
         say(pSource, "Failed to export " + $$4);
         return 1;
      } else {
         try {
            FileUtil.createDirectoriesSafe($$5.getParent());
         } catch (IOException var7) {
            say(pSource, "Could not create folder " + $$5.getParent());
            LOGGER.error("Could not create export folder", var7);
            return 1;
         }

         say(pSource, "Exported " + pStructurePath + " to " + $$5.toAbsolutePath());
         return 0;
      }
   }

   private static boolean verifyStructureExists(ServerLevel pLevel, String pStructure) {
      if (pLevel.getStructureManager().get(ResourceLocation.parse(pStructure)).isEmpty()) {
         say(pLevel, "Test structure " + pStructure + " could not be found", ChatFormatting.RED);
         return false;
      } else {
         return true;
      }
   }

   static BlockPos createTestPositionAround(CommandSourceStack pSource) {
      BlockPos $$1 = BlockPos.containing(pSource.getPosition());
      int $$2 = pSource.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, $$1).getY();
      return new BlockPos($$1.getX(), $$2 + 1, $$1.getZ() + 3);
   }

   static void say(CommandSourceStack pSource, String pMessage) {
      pSource.sendSuccess(() -> Component.literal(pMessage), false);
   }

   private static int importTestStructure(CommandSourceStack pSource, String pStructurePath) {
      Path $$2 = Paths.get(StructureUtils.testStructuresDir, pStructurePath + ".snbt");
      ResourceLocation $$3 = ResourceLocation.withDefaultNamespace(pStructurePath);
      Path $$4 = pSource.getLevel().getStructureManager().createAndValidatePathToGeneratedStructure($$3, ".nbt");

      try {
         BufferedReader $$5 = Files.newBufferedReader($$2);
         String $$6 = IOUtils.toString($$5);
         Files.createDirectories($$4.getParent());

         try (OutputStream $$7 = Files.newOutputStream($$4)) {
            NbtIo.writeCompressed(NbtUtils.snbtToStructure($$6), $$7);
         }

         pSource.getLevel().getStructureManager().remove($$3);
         say(pSource, "Imported to " + $$4.toAbsolutePath());
         return 0;
      } catch (CommandSyntaxException | IOException var12) {
         LOGGER.error("Failed to load structure {}", pStructurePath, var12);
         return 1;
      }
   }

   static void say(ServerLevel pServerLevel, String pMessage, ChatFormatting pFormatting) {
      pServerLevel.getPlayers(p_127945_ -> true).forEach(p_308546_ -> p_308546_.sendSystemMessage(Component.literal(pMessage).withStyle(pFormatting)));
   }

   public static class Runner {
      private final TestFinder<TestCommand.Runner> finder;

      public Runner(TestFinder<TestCommand.Runner> pFinder) {
         this.finder = pFinder;
      }

      public int reset() {
         TestCommand.stopTests();
         return TestCommand.toGameTestInfos(this.finder.source(), RetryOptions.noRetries(), this.finder).map(TestCommand::resetGameTestInfo).toList().isEmpty()
            ? 0
            : 1;
      }

      private <T> void logAndRun(Stream<T> pStructureBlockPos, ToIntFunction<T> pTestCounter, Runnable pOnFail, Consumer<Integer> pOnSuccess) {
         int $$4 = pStructureBlockPos.mapToInt(pTestCounter).sum();
         if ($$4 == 0) {
            pOnFail.run();
         } else {
            pOnSuccess.accept($$4);
         }
      }

      public int clear() {
         TestCommand.stopTests();
         CommandSourceStack $$0 = this.finder.source();
         ServerLevel $$1 = $$0.getLevel();
         GameTestRunner.clearMarkers($$1);
         this.logAndRun(
            this.finder.findStructureBlockPos(),
            p_330831_ -> {
               StructureBlockEntity $$2 = (StructureBlockEntity)$$1.getBlockEntity(p_330831_);
               if ($$2 == null) {
                  return 0;
               } else {
                  BoundingBox $$3 = StructureUtils.getStructureBoundingBox($$2);
                  StructureUtils.clearSpaceForStructure($$3, $$1);
                  return 1;
               }
            },
            () -> TestCommand.say($$1, "Could not find any structures to clear", ChatFormatting.RED),
            p_330244_ -> TestCommand.say($$0, "Cleared " + p_330244_ + " structures")
         );
         return 1;
      }

      public int export() {
         MutableBoolean $$0 = new MutableBoolean(true);
         CommandSourceStack $$1 = this.finder.source();
         ServerLevel $$2 = $$1.getLevel();
         this.logAndRun(
            this.finder.findStructureBlockPos(),
            p_331429_ -> {
               StructureBlockEntity $$4 = (StructureBlockEntity)$$2.getBlockEntity(p_331429_);
               if ($$4 == null) {
                  TestCommand.say($$2, "Structure block entity could not be found", ChatFormatting.RED);
                  $$0.setFalse();
                  return 0;
               } else {
                  if (TestCommand.saveAndExportTestStructure($$1, $$4) != 0) {
                     $$0.setFalse();
                  }

                  return 1;
               }
            },
            () -> TestCommand.say($$2, "Could not find any structures to export", ChatFormatting.RED),
            p_333553_ -> TestCommand.say($$1, "Exported " + p_333553_ + " structures")
         );
         return $$0.getValue() ? 0 : 1;
      }

      int verify() {
         TestCommand.stopTests();
         CommandSourceStack $$0 = this.finder.source();
         ServerLevel $$1 = $$0.getLevel();
         BlockPos $$2 = TestCommand.createTestPositionAround($$0);
         Collection<GameTestInfo> $$3 = Stream.concat(
               TestCommand.toGameTestInfos($$0, RetryOptions.noRetries(), this.finder),
               TestCommand.toGameTestInfo($$0, RetryOptions.noRetries(), this.finder, 0)
            )
            .toList();
         int $$4 = 10;
         GameTestRunner.clearMarkers($$1);
         GameTestRegistry.forgetFailedTests();
         Collection<GameTestBatch> $$5 = new ArrayList<>();

         for (GameTestInfo $$6 : $$3) {
            for (Rotation $$7 : Rotation.values()) {
               Collection<GameTestInfo> $$8 = new ArrayList<>();

               for (int $$9 = 0; $$9 < 100; $$9++) {
                  GameTestInfo $$10 = new GameTestInfo($$6.getTestFunction(), $$7, $$1, new RetryOptions(1, true));
                  $$8.add($$10);
               }

               GameTestBatch $$11 = GameTestBatchFactory.toGameTestBatch($$8, $$6.getTestFunction().batchName(), (long)$$7.ordinal());
               $$5.add($$11);
            }
         }

         StructureGridSpawner $$12 = new StructureGridSpawner($$2, 10, true);
         GameTestRunner $$13 = GameTestRunner.Builder.fromBatches($$5, $$1)
            .batcher(GameTestBatchFactory.fromGameTestInfo(100))
            .newStructureSpawner($$12)
            .existingStructureSpawner($$12)
            .haltOnError(true)
            .build();
         return TestCommand.trackAndStartRunner($$0, $$1, $$13);
      }

      public int run(RetryOptions pRetryOptions, int pRotationSteps, int pTestsPerRow) {
         TestCommand.stopTests();
         CommandSourceStack $$3 = this.finder.source();
         ServerLevel $$4 = $$3.getLevel();
         BlockPos $$5 = TestCommand.createTestPositionAround($$3);
         Collection<GameTestInfo> $$6 = Stream.concat(
               TestCommand.toGameTestInfos($$3, pRetryOptions, this.finder), TestCommand.toGameTestInfo($$3, pRetryOptions, this.finder, pRotationSteps)
            )
            .toList();
         if ($$6.isEmpty()) {
            TestCommand.say($$3, "No tests found");
            return 0;
         } else {
            GameTestRunner.clearMarkers($$4);
            GameTestRegistry.forgetFailedTests();
            TestCommand.say($$3, "Running " + $$6.size() + " tests...");
            GameTestRunner $$7 = GameTestRunner.Builder.fromInfo($$6, $$4).newStructureSpawner(new StructureGridSpawner($$5, pTestsPerRow, false)).build();
            return TestCommand.trackAndStartRunner($$3, $$4, $$7);
         }
      }

      public int run(int pRotationSteps, int pTestsPerRow) {
         return this.run(RetryOptions.noRetries(), pRotationSteps, pTestsPerRow);
      }

      public int run(int pRotationSteps) {
         return this.run(RetryOptions.noRetries(), pRotationSteps, 8);
      }

      public int run(RetryOptions pRetryOptions, int pRotationSteps) {
         return this.run(pRetryOptions, pRotationSteps, 8);
      }

      public int run(RetryOptions pRetryOptions) {
         return this.run(pRetryOptions, 0, 8);
      }

      public int run() {
         return this.run(RetryOptions.noRetries());
      }

      public int locate() {
         TestCommand.say(this.finder.source(), "Started locating test structures, this might take a while..");
         MutableInt $$0 = new MutableInt(0);
         BlockPos $$1 = BlockPos.containing(this.finder.source().getPosition());
         this.finder
            .findStructureBlockPos()
            .forEach(
               p_327721_ -> {
                  StructureBlockEntity $$3 = (StructureBlockEntity)this.finder.source().getLevel().getBlockEntity(p_327721_);
                  if ($$3 != null) {
                     Direction $$4 = $$3.getRotation().rotate(Direction.NORTH);
                     BlockPos $$5 = $$3.getBlockPos().relative($$4, 2);
                     int $$6 = (int)$$4.getOpposite().toYRot();
                     String $$7 = String.format("/tp @s %d %d %d %d 0", $$5.getX(), $$5.getY(), $$5.getZ(), $$6);
                     int $$8 = $$1.getX() - p_327721_.getX();
                     int $$9 = $$1.getZ() - p_327721_.getZ();
                     int $$10 = Mth.floor(Mth.sqrt((float)($$8 * $$8 + $$9 * $$9)));
                     Component $$11 = ComponentUtils.wrapInSquareBrackets(
                           Component.translatable("chat.coordinates", p_327721_.getX(), p_327721_.getY(), p_327721_.getZ())
                        )
                        .withStyle(
                           p_332540_ -> p_332540_.withColor(ChatFormatting.GREEN)
                                 .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, $$7))
                                 .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
                        );
                     Component $$12 = Component.literal("Found structure at: ").append($$11).append(" (distance: " + $$10 + ")");
                     this.finder.source().sendSuccess(() -> $$12, false);
                     $$0.increment();
                  }
               }
            );
         int $$2 = $$0.intValue();
         if ($$2 == 0) {
            TestCommand.say(this.finder.source().getLevel(), "No such test structure found", ChatFormatting.RED);
            return 0;
         } else {
            TestCommand.say(this.finder.source().getLevel(), "Finished locating, found " + $$2 + " structure(s)", ChatFormatting.GREEN);
            return 1;
         }
      }
   }

   static record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
      @Override
      public void testBatchStarting(GameTestBatch p_327831_) {
         TestCommand.say(this.source, "Starting batch: " + p_327831_.name());
      }

      @Override
      public void testBatchFinished(GameTestBatch p_335734_) {
      }
   }

   public static record TestSummaryDisplayer(ServerLevel level, MultipleTestTracker tracker) implements GameTestListener {
      @Override
      public void testStructureLoaded(GameTestInfo pTestInfo) {
      }

      @Override
      public void testPassed(GameTestInfo pTest, GameTestRunner pRunner) {
         showTestSummaryIfAllDone(this.level, this.tracker);
      }

      @Override
      public void testFailed(GameTestInfo pTest, GameTestRunner pRunner) {
         showTestSummaryIfAllDone(this.level, this.tracker);
      }

      @Override
      public void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner) {
         this.tracker.addTestToTrack(pNewTest);
      }

      private static void showTestSummaryIfAllDone(ServerLevel pLevel, MultipleTestTracker pTracker) {
         if (pTracker.isDone()) {
            TestCommand.say(pLevel, "GameTest done! " + pTracker.getTotalCount() + " tests were run", ChatFormatting.WHITE);
            if (pTracker.hasFailedRequired()) {
               TestCommand.say(pLevel, pTracker.getFailedRequiredCount() + " required tests failed :(", ChatFormatting.RED);
            } else {
               TestCommand.say(pLevel, "All required tests passed :)", ChatFormatting.GREEN);
            }

            if (pTracker.hasFailedOptional()) {
               TestCommand.say(pLevel, pTracker.getFailedOptionalCount() + " optional tests failed", ChatFormatting.GRAY);
            }
         }
      }
   }
}
