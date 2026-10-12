package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

public class TestFinder<T> implements StructureBlockPosFinder, TestFunctionFinder {
   static final TestFunctionFinder NO_FUNCTIONS = Stream::empty;
   static final StructureBlockPosFinder NO_STRUCTURES = Stream::empty;
   private final TestFunctionFinder testFunctionFinder;
   private final StructureBlockPosFinder structureBlockPosFinder;
   private final CommandSourceStack source;
   private final Function<TestFinder<T>, T> contextProvider;

   @Override
   public Stream<BlockPos> findStructureBlockPos() {
      return this.structureBlockPosFinder.findStructureBlockPos();
   }

   TestFinder(
      CommandSourceStack pSource,
      Function<TestFinder<T>, T> pContextProvider,
      TestFunctionFinder pTestFunctionFinder,
      StructureBlockPosFinder pStructureBlockPosFinder
   ) {
      this.source = pSource;
      this.contextProvider = pContextProvider;
      this.testFunctionFinder = pTestFunctionFinder;
      this.structureBlockPosFinder = pStructureBlockPosFinder;
   }

   T get() {
      return this.contextProvider.apply(this);
   }

   public CommandSourceStack source() {
      return this.source;
   }

   @Override
   public Stream<TestFunction> findTestFunctions() {
      return this.testFunctionFinder.findTestFunctions();
   }

   public static class Builder<T> {
      private final Function<TestFinder<T>, T> contextProvider;
      private final UnaryOperator<Supplier<Stream<TestFunction>>> testFunctionFinderWrapper;
      private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

      public Builder(Function<TestFinder<T>, T> pContextProvider) {
         this.contextProvider = pContextProvider;
         this.testFunctionFinderWrapper = p_333647_ -> p_333647_;
         this.structureBlockPosFinderWrapper = p_327811_ -> p_327811_;
      }

      private Builder(
         Function<TestFinder<T>, T> pContextProvider,
         UnaryOperator<Supplier<Stream<TestFunction>>> pTestFunctionFinderWrapper,
         UnaryOperator<Supplier<Stream<BlockPos>>> pStructureBlockPosFinderWrapper
      ) {
         this.contextProvider = pContextProvider;
         this.testFunctionFinderWrapper = pTestFunctionFinderWrapper;
         this.structureBlockPosFinderWrapper = pStructureBlockPosFinderWrapper;
      }

      public TestFinder.Builder<T> createMultipleCopies(int pCount) {
         return new TestFinder.Builder<>(this.contextProvider, createCopies(pCount), createCopies(pCount));
      }

      private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int pCount) {
         return p_333976_ -> {
            List<Q> $$2 = new LinkedList<>();
            List<Q> $$3 = ((Stream)p_333976_.get()).toList();

            for (int $$4 = 0; $$4 < pCount; $$4++) {
               $$2.addAll($$3);
            }

            return $$2::stream;
         };
      }

      private T build(CommandSourceStack pSource, TestFunctionFinder pTestFunctionFinder, StructureBlockPosFinder pStructureBlockPosFinder) {
         return new TestFinder<>(
               pSource,
               this.contextProvider,
               this.testFunctionFinderWrapper.apply(pTestFunctionFinder::findTestFunctions)::get,
               this.structureBlockPosFinderWrapper.apply(pStructureBlockPosFinder::findStructureBlockPos)::get
            )
            .get();
      }

      public T radius(CommandContext<CommandSourceStack> pContext, int pRadius) {
         CommandSourceStack $$2 = (CommandSourceStack)pContext.getSource();
         BlockPos $$3 = BlockPos.containing($$2.getPosition());
         return this.build($$2, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks($$3, pRadius, $$2.getLevel()));
      }

      public T nearest(CommandContext<CommandSourceStack> pContext) {
         CommandSourceStack $$1 = (CommandSourceStack)pContext.getSource();
         BlockPos $$2 = BlockPos.containing($$1.getPosition());
         return this.build($$1, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestStructureBlock($$2, 15, $$1.getLevel()).stream());
      }

      public T allNearby(CommandContext<CommandSourceStack> pContext) {
         CommandSourceStack $$1 = (CommandSourceStack)pContext.getSource();
         BlockPos $$2 = BlockPos.containing($$1.getPosition());
         return this.build($$1, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureBlocks($$2, 200, $$1.getLevel()));
      }

      public T lookedAt(CommandContext<CommandSourceStack> pContext) {
         CommandSourceStack $$1 = (CommandSourceStack)pContext.getSource();
         return this.build(
            $$1,
            TestFinder.NO_FUNCTIONS,
            () -> StructureUtils.lookedAtStructureBlockPos(BlockPos.containing($$1.getPosition()), $$1.getPlayer().getCamera(), $$1.getLevel())
         );
      }

      public T allTests(CommandContext<CommandSourceStack> pContext) {
         return this.build(
            (CommandSourceStack)pContext.getSource(),
            () -> GameTestRegistry.getAllTestFunctions().stream().filter(p_334467_ -> !p_334467_.manualOnly()),
            TestFinder.NO_STRUCTURES
         );
      }

      public T allTestsInClass(CommandContext<CommandSourceStack> pContext, String pClassName) {
         return this.build(
            (CommandSourceStack)pContext.getSource(),
            () -> GameTestRegistry.getTestFunctionsForClassName(pClassName).filter(p_328668_ -> !p_328668_.manualOnly()),
            TestFinder.NO_STRUCTURES
         );
      }

      public T failedTests(CommandContext<CommandSourceStack> pContext, boolean pOnlyRequired) {
         return this.build(
            (CommandSourceStack)pContext.getSource(),
            () -> GameTestRegistry.getLastFailedTests().filter(p_328598_ -> !pOnlyRequired || p_328598_.required()),
            TestFinder.NO_STRUCTURES
         );
      }

      public T byArgument(CommandContext<CommandSourceStack> pContext, String pArgumentName) {
         return this.build(
            (CommandSourceStack)pContext.getSource(), () -> Stream.of(TestFunctionArgument.getTestFunction(pContext, pArgumentName)), TestFinder.NO_STRUCTURES
         );
      }

      public T locateByName(CommandContext<CommandSourceStack> pContext, String pName) {
         CommandSourceStack $$2 = (CommandSourceStack)pContext.getSource();
         BlockPos $$3 = BlockPos.containing($$2.getPosition());
         return this.build($$2, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findStructureByTestFunction($$3, 1024, $$2.getLevel(), pName));
      }

      public T failedTests(CommandContext<CommandSourceStack> pContext) {
         return this.failedTests(pContext, false);
      }
   }
}
