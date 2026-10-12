package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

public class PathTypeCache {
   private static final int SIZE = 4096;
   private static final int MASK = 4095;
   private final long[] positions = new long[4096];
   private final PathType[] pathTypes = new PathType[4096];

   public PathType getOrCompute(BlockGetter pLevel, BlockPos pPos) {
      long $$2 = pPos.asLong();
      int $$3 = index($$2);
      PathType $$4 = this.get($$3, $$2);
      return $$4 != null ? $$4 : this.compute(pLevel, pPos, $$3, $$2);
   }

   @Nullable
   private PathType get(int pIndex, long pPos) {
      return this.positions[pIndex] == pPos ? this.pathTypes[pIndex] : null;
   }

   private PathType compute(BlockGetter pLevel, BlockPos pPos, int pIndex, long pPackedPos) {
      PathType $$4 = WalkNodeEvaluator.getPathTypeFromState(pLevel, pPos);
      this.positions[pIndex] = pPackedPos;
      this.pathTypes[pIndex] = $$4;
      return $$4;
   }

   public void invalidate(BlockPos pPos) {
      long $$1 = pPos.asLong();
      int $$2 = index($$1);
      if (this.positions[$$2] == $$1) {
         this.pathTypes[$$2] = null;
      }
   }

   private static int index(long pPos) {
      return (int)HashCommon.mix(pPos) & 4095;
   }
}
