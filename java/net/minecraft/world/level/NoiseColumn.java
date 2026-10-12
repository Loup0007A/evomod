package net.minecraft.world.level;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;

public final class NoiseColumn implements BlockColumn {
   private final int minY;
   private final BlockState[] column;

   public NoiseColumn(int pMinY, BlockState[] pColumn) {
      this.minY = pMinY;
      this.column = pColumn;
   }

   @Override
   public BlockState getBlock(int pPos) {
      int $$1 = pPos - this.minY;
      return $$1 >= 0 && $$1 < this.column.length ? this.column[$$1] : Blocks.AIR.defaultBlockState();
   }

   @Override
   public void setBlock(int pPos, BlockState pState) {
      int $$2 = pPos - this.minY;
      if ($$2 >= 0 && $$2 < this.column.length) {
         this.column[$$2] = pState;
      } else {
         throw new IllegalArgumentException("Outside of column height: " + pPos);
      }
   }
}
