package net.minecraft.world.level.redstone;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface NeighborUpdater {
   Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

   void shapeUpdate(Direction pDirection, BlockState pState, BlockPos pPos, BlockPos pNeighborPos, int pFlags, int pRecursionLevel);

   void neighborChanged(BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos);

   void neighborChanged(BlockState pState, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston);

   default void updateNeighborsAtExceptFromFacing(BlockPos pPos, Block pBlock, @Nullable Direction pFacing) {
      for (Direction $$3 : UPDATE_ORDER) {
         if ($$3 != pFacing) {
            this.neighborChanged(pPos.relative($$3), pBlock, pPos);
         }
      }
   }

   static void executeShapeUpdate(
      LevelAccessor pLevel, Direction pDirection, BlockState pState, BlockPos pPos, BlockPos pNeighborPos, int pFlags, int pRecursionLevel
   ) {
      BlockState $$7 = pLevel.getBlockState(pPos);
      BlockState $$8 = $$7.updateShape(pDirection, pState, pLevel, pPos, pNeighborPos);
      Block.updateOrDestroy($$7, $$8, pLevel, pPos, pFlags, pRecursionLevel);
   }

   static void executeUpdate(Level pLevel, BlockState pState, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
      try {
         pState.handleNeighborChanged(pLevel, pPos, pNeighborBlock, pNeighborPos, pMovedByPiston);
      } catch (Throwable var9) {
         CrashReport $$7 = CrashReport.forThrowable(var9, "Exception while updating neighbours");
         CrashReportCategory $$8 = $$7.addCategory("Block being updated");
         $$8.setDetail(
            "Source block type",
            () -> {
               try {
                  return String.format(
                     Locale.ROOT,
                     "ID #%s (%s // %s)",
                     BuiltInRegistries.BLOCK.getKey(pNeighborBlock),
                     pNeighborBlock.getDescriptionId(),
                     pNeighborBlock.getClass().getCanonicalName()
                  );
               } catch (Throwable var2x) {
                  return "ID #" + BuiltInRegistries.BLOCK.getKey(pNeighborBlock);
               }
            }
         );
         CrashReportCategory.populateBlockDetails($$8, pLevel, pPos, pState);
         throw new ReportedException($$7);
      }
   }
}
