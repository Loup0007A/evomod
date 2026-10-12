package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
   private int attempts = 0;
   private int successes = 0;

   public ReportGameListener() {
   }

   @Override
   public void testStructureLoaded(GameTestInfo pTestInfo) {
      spawnBeacon(pTestInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
      this.attempts++;
   }

   private void handleRetry(GameTestInfo pTestInfo, GameTestRunner pRunner, boolean pPassed) {
      RetryOptions $$3 = pTestInfo.retryOptions();
      String $$4 = String.format("[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
      if (!$$3.unlimitedTries()) {
         $$4 = $$4 + String.format(", Left: %4d", $$3.numberOfTries() - this.attempts);
      }

      $$4 = $$4 + "]";
      String $$5 = pTestInfo.getTestName() + " " + (pPassed ? "passed" : "failed") + "! " + pTestInfo.getRunTime() + "ms";
      String $$6 = String.format("%-53s%s", $$4, $$5);
      if (pPassed) {
         reportPassed(pTestInfo, $$6);
      } else {
         say(pTestInfo.getLevel(), ChatFormatting.RED, $$6);
      }

      if ($$3.hasTriesLeft(this.attempts, this.successes)) {
         pRunner.rerunTest(pTestInfo);
      }
   }

   @Override
   public void testPassed(GameTestInfo pTest, GameTestRunner pRunner) {
      this.successes++;
      if (pTest.retryOptions().hasRetries()) {
         this.handleRetry(pTest, pRunner, true);
      } else if (!pTest.isFlaky()) {
         reportPassed(pTest, pTest.getTestName() + " passed! (" + pTest.getRunTime() + "ms)");
      } else {
         if (this.successes >= pTest.requiredSuccesses()) {
            reportPassed(pTest, pTest + " passed " + this.successes + " times of " + this.attempts + " attempts.");
         } else {
            say(pTest.getLevel(), ChatFormatting.GREEN, "Flaky test " + pTest + " succeeded, attempt: " + this.attempts + " successes: " + this.successes);
            pRunner.rerunTest(pTest);
         }
      }
   }

   @Override
   public void testFailed(GameTestInfo pTest, GameTestRunner pRunner) {
      if (!pTest.isFlaky()) {
         reportFailure(pTest, pTest.getError());
         if (pTest.retryOptions().hasRetries()) {
            this.handleRetry(pTest, pRunner, false);
         }
      } else {
         TestFunction $$2 = pTest.getTestFunction();
         String $$3 = "Flaky test " + pTest + " failed, attempt: " + this.attempts + "/" + $$2.maxAttempts();
         if ($$2.requiredSuccesses() > 1) {
            $$3 = $$3 + ", successes: " + this.successes + " (" + $$2.requiredSuccesses() + " required)";
         }

         say(pTest.getLevel(), ChatFormatting.YELLOW, $$3);
         if (pTest.maxAttempts() - this.attempts + this.successes >= pTest.requiredSuccesses()) {
            pRunner.rerunTest(pTest);
         } else {
            reportFailure(pTest, new ExhaustedAttemptsException(this.attempts, this.successes, pTest));
         }
      }
   }

   @Override
   public void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner) {
      pNewTest.addListener(this);
   }

   public static void reportPassed(GameTestInfo pTestInfo, String pMessage) {
      updateBeaconGlass(pTestInfo, Blocks.LIME_STAINED_GLASS);
      visualizePassedTest(pTestInfo, pMessage);
   }

   private static void visualizePassedTest(GameTestInfo pTestInfo, String pMessage) {
      say(pTestInfo.getLevel(), ChatFormatting.GREEN, pMessage);
      GlobalTestReporter.onTestSuccess(pTestInfo);
   }

   protected static void reportFailure(GameTestInfo pTestInfo, Throwable pError) {
      updateBeaconGlass(pTestInfo, pTestInfo.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
      spawnLectern(pTestInfo, Util.describeError(pError));
      visualizeFailedTest(pTestInfo, pError);
   }

   protected static void visualizeFailedTest(GameTestInfo pTestInfo, Throwable pError) {
      String $$2 = pError.getMessage() + (pError.getCause() == null ? "" : " cause: " + Util.describeError(pError.getCause()));
      String $$3 = (pTestInfo.isRequired() ? "" : "(optional) ") + pTestInfo.getTestName() + " failed! " + $$2;
      say(pTestInfo.getLevel(), pTestInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, $$3);
      Throwable $$4 = (Throwable)MoreObjects.firstNonNull(ExceptionUtils.getRootCause(pError), pError);
      if ($$4 instanceof GameTestAssertPosException $$5) {
         showRedBox(pTestInfo.getLevel(), $$5.getAbsolutePos(), $$5.getMessageToShowAtBlock());
      }

      GlobalTestReporter.onTestFailed(pTestInfo);
   }

   protected static void spawnBeacon(GameTestInfo pTestInfo, Block pBlock) {
      ServerLevel $$2 = pTestInfo.getLevel();
      BlockPos $$3 = getBeaconPos(pTestInfo);
      $$2.setBlockAndUpdate($$3, Blocks.BEACON.defaultBlockState().rotate(pTestInfo.getRotation()));
      updateBeaconGlass(pTestInfo, pBlock);

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            BlockPos $$6 = $$3.offset($$4, -1, $$5);
            $$2.setBlockAndUpdate($$6, Blocks.IRON_BLOCK.defaultBlockState());
         }
      }
   }

   private static BlockPos getBeaconPos(GameTestInfo pTestInfo) {
      BlockPos $$1 = pTestInfo.getStructureBlockPos();
      BlockPos $$2 = new BlockPos(-1, -2, -1);
      return StructureTemplate.transform($$1.offset($$2), Mirror.NONE, pTestInfo.getRotation(), $$1);
   }

   private static void updateBeaconGlass(GameTestInfo pTestInfo, Block pNewBlock) {
      ServerLevel $$2 = pTestInfo.getLevel();
      BlockPos $$3 = getBeaconPos(pTestInfo);
      if ($$2.getBlockState($$3).is(Blocks.BEACON)) {
         BlockPos $$4 = $$3.offset(0, 1, 0);
         $$2.setBlockAndUpdate($$4, pNewBlock.defaultBlockState());
      }
   }

   private static void spawnLectern(GameTestInfo pTestInfo, String pMessage) {
      ServerLevel $$2 = pTestInfo.getLevel();
      BlockPos $$3 = pTestInfo.getStructureBlockPos();
      BlockPos $$4 = new BlockPos(-1, 0, -1);
      BlockPos $$5 = StructureTemplate.transform($$3.offset($$4), Mirror.NONE, pTestInfo.getRotation(), $$3);
      $$2.setBlockAndUpdate($$5, Blocks.LECTERN.defaultBlockState().rotate(pTestInfo.getRotation()));
      BlockState $$6 = $$2.getBlockState($$5);
      ItemStack $$7 = createBook(pTestInfo.getTestName(), pTestInfo.isRequired(), pMessage);
      LecternBlock.tryPlaceBook(null, $$2, $$5, $$6, $$7);
   }

   private static ItemStack createBook(String pTestName, boolean pRequired, String pMessage) {
      StringBuffer $$3 = new StringBuffer();
      Arrays.stream(pTestName.split("\\.")).forEach(p_177716_ -> $$3.append(p_177716_).append('\n'));
      if (!pRequired) {
         $$3.append("(optional)\n");
      }

      $$3.append("-------------------\n");
      ItemStack $$4 = new ItemStack(Items.WRITABLE_BOOK);
      $$4.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough($$3 + pMessage))));
      return $$4;
   }

   protected static void say(ServerLevel pServerLevel, ChatFormatting pFormatting, String pMessage) {
      pServerLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(pMessage).withStyle(pFormatting)));
   }

   private static void showRedBox(ServerLevel pServerLevel, BlockPos pPos, String pDisplayMessage) {
      DebugPackets.sendGameTestAddMarker(pServerLevel, pPos, pDisplayMessage, -2130771968, Integer.MAX_VALUE);
   }
}
