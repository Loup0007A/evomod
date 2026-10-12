package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;

public class GameTestBatchFactory {
   private static final int MAX_TESTS_PER_BATCH = 50;

   public static Collection<GameTestBatch> fromTestFunction(Collection<TestFunction> pTestFunctions, ServerLevel pLevel) {
      Map<String, List<TestFunction>> $$2 = pTestFunctions.stream().collect(Collectors.groupingBy(TestFunction::batchName));
      return $$2.entrySet()
         .stream()
         .flatMap(
            p_332128_ -> {
               String $$2x = p_332128_.getKey();
               List<TestFunction> $$3 = p_332128_.getValue();
               return Streams.mapWithIndex(
                  Lists.partition($$3, 50).stream(),
                  (p_341093_, p_341094_) -> toGameTestBatch(p_341093_.stream().map(p_334925_ -> toGameTestInfo(p_334925_, 0, pLevel)).toList(), $$2x, p_341094_)
               );
            }
         )
         .toList();
   }

   public static GameTestInfo toGameTestInfo(TestFunction pTestFunction, int pRotationSteps, ServerLevel pLevel) {
      return new GameTestInfo(pTestFunction, StructureUtils.getRotationForRotationSteps(pRotationSteps), pLevel, RetryOptions.noRetries());
   }

   public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
      return fromGameTestInfo(50);
   }

   public static GameTestRunner.GameTestBatcher fromGameTestInfo(int pMaxTests) {
      return p_341088_ -> {
         Map<String, List<GameTestInfo>> $$2 = p_341088_.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(p_330180_ -> p_330180_.getTestFunction().batchName()));
         return $$2.entrySet()
            .stream()
            .flatMap(
               p_341090_ -> {
                  String $$2x = p_341090_.getKey();
                  List<GameTestInfo> $$3 = p_341090_.getValue();
                  return Streams.mapWithIndex(
                     Lists.partition($$3, pMaxTests).stream(), (p_341085_, p_341086_) -> toGameTestBatch(List.copyOf(p_341085_), $$2x, p_341086_)
                  );
               }
            )
            .toList();
      };
   }

   public static GameTestBatch toGameTestBatch(Collection<GameTestInfo> pGameTestInfos, String pFunctionName, long pIndex) {
      Consumer<ServerLevel> $$3 = GameTestRegistry.getBeforeBatchFunction(pFunctionName);
      Consumer<ServerLevel> $$4 = GameTestRegistry.getAfterBatchFunction(pFunctionName);
      return new GameTestBatch(pFunctionName + ":" + pIndex, pGameTestInfos, $$3, $$4);
   }
}
