package net.minecraft.world.level.block.state.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;

public class BlockPattern {
   private final Predicate<BlockInWorld>[][][] pattern;
   private final int depth;
   private final int height;
   private final int width;

   public BlockPattern(Predicate<BlockInWorld>[][][] pPattern) {
      this.pattern = pPattern;
      this.depth = pPattern.length;
      if (this.depth > 0) {
         this.height = pPattern[0].length;
         if (this.height > 0) {
            this.width = pPattern[0][0].length;
         } else {
            this.width = 0;
         }
      } else {
         this.height = 0;
         this.width = 0;
      }
   }

   public int getDepth() {
      return this.depth;
   }

   public int getHeight() {
      return this.height;
   }

   public int getWidth() {
      return this.width;
   }

   @VisibleForTesting
   public Predicate<BlockInWorld>[][][] getPattern() {
      return this.pattern;
   }

   @Nullable
   @VisibleForTesting
   public BlockPattern.BlockPatternMatch matches(LevelReader pLevel, BlockPos pPos, Direction pFinger, Direction pThumb) {
      LoadingCache<BlockPos, BlockInWorld> $$4 = createLevelCache(pLevel, false);
      return this.matches(pPos, pFinger, pThumb, $$4);
   }

   @Nullable
   private BlockPattern.BlockPatternMatch matches(BlockPos pPos, Direction pFinger, Direction pThumb, LoadingCache<BlockPos, BlockInWorld> pCache) {
      for (int $$4 = 0; $$4 < this.width; $$4++) {
         for (int $$5 = 0; $$5 < this.height; $$5++) {
            for (int $$6 = 0; $$6 < this.depth; $$6++) {
               if (!this.pattern[$$6][$$5][$$4].test((BlockInWorld)pCache.getUnchecked(translateAndRotate(pPos, pFinger, pThumb, $$4, $$5, $$6)))) {
                  return null;
               }
            }
         }
      }

      return new BlockPattern.BlockPatternMatch(pPos, pFinger, pThumb, pCache, this.width, this.height, this.depth);
   }

   @Nullable
   public BlockPattern.BlockPatternMatch find(LevelReader pLevel, BlockPos pPos) {
      LoadingCache<BlockPos, BlockInWorld> $$2 = createLevelCache(pLevel, false);
      int $$3 = Math.max(Math.max(this.width, this.height), this.depth);

      for (BlockPos $$4 : BlockPos.betweenClosed(pPos, pPos.offset($$3 - 1, $$3 - 1, $$3 - 1))) {
         for (Direction $$5 : Direction.values()) {
            for (Direction $$6 : Direction.values()) {
               if ($$6 != $$5 && $$6 != $$5.getOpposite()) {
                  BlockPattern.BlockPatternMatch $$7 = this.matches($$4, $$5, $$6, $$2);
                  if ($$7 != null) {
                     return $$7;
                  }
               }
            }
         }
      }

      return null;
   }

   public static LoadingCache<BlockPos, BlockInWorld> createLevelCache(LevelReader pLevel, boolean pForceLoad) {
      return CacheBuilder.newBuilder().build(new BlockPattern.BlockCacheLoader(pLevel, pForceLoad));
   }

   protected static BlockPos translateAndRotate(BlockPos pPos, Direction pFinger, Direction pThumb, int pPalmOffset, int pThumbOffset, int pFingerOffset) {
      if (pFinger != pThumb && pFinger != pThumb.getOpposite()) {
         Vec3i $$6 = new Vec3i(pFinger.getStepX(), pFinger.getStepY(), pFinger.getStepZ());
         Vec3i $$7 = new Vec3i(pThumb.getStepX(), pThumb.getStepY(), pThumb.getStepZ());
         Vec3i $$8 = $$6.cross($$7);
         return pPos.offset(
            $$7.getX() * -pThumbOffset + $$8.getX() * pPalmOffset + $$6.getX() * pFingerOffset,
            $$7.getY() * -pThumbOffset + $$8.getY() * pPalmOffset + $$6.getY() * pFingerOffset,
            $$7.getZ() * -pThumbOffset + $$8.getZ() * pPalmOffset + $$6.getZ() * pFingerOffset
         );
      } else {
         throw new IllegalArgumentException("Invalid forwards & up combination");
      }
   }

   static class BlockCacheLoader extends CacheLoader<BlockPos, BlockInWorld> {
      private final LevelReader level;
      private final boolean loadChunks;

      public BlockCacheLoader(LevelReader pLevel, boolean pLoadChunks) {
         this.level = pLevel;
         this.loadChunks = pLoadChunks;
      }

      public BlockInWorld load(BlockPos pPos) {
         return new BlockInWorld(this.level, pPos, this.loadChunks);
      }
   }

   public static class BlockPatternMatch {
      private final BlockPos frontTopLeft;
      private final Direction forwards;
      private final Direction up;
      private final LoadingCache<BlockPos, BlockInWorld> cache;
      private final int width;
      private final int height;
      private final int depth;

      public BlockPatternMatch(
         BlockPos pFrontTopLeft, Direction pForwards, Direction pUp, LoadingCache<BlockPos, BlockInWorld> pCache, int pWidth, int pHeight, int pDepth
      ) {
         this.frontTopLeft = pFrontTopLeft;
         this.forwards = pForwards;
         this.up = pUp;
         this.cache = pCache;
         this.width = pWidth;
         this.height = pHeight;
         this.depth = pDepth;
      }

      public BlockPos getFrontTopLeft() {
         return this.frontTopLeft;
      }

      public Direction getForwards() {
         return this.forwards;
      }

      public Direction getUp() {
         return this.up;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public int getDepth() {
         return this.depth;
      }

      public BlockInWorld getBlock(int pPalmOffset, int pThumbOffset, int pFingerOffset) {
         return (BlockInWorld)this.cache
            .getUnchecked(BlockPattern.translateAndRotate(this.frontTopLeft, this.getForwards(), this.getUp(), pPalmOffset, pThumbOffset, pFingerOffset));
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this).add("up", this.up).add("forwards", this.forwards).add("frontTopLeft", this.frontTopLeft).toString();
      }
   }
}
