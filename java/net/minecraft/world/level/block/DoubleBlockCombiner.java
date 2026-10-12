package net.minecraft.world.level.block;

import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class DoubleBlockCombiner {
   public static <S extends BlockEntity> DoubleBlockCombiner.NeighborCombineResult<S> combineWithNeigbour(
      BlockEntityType<S> pBlockEntityType,
      Function<BlockState, DoubleBlockCombiner.BlockType> pDoubleBlockTypeGetter,
      Function<BlockState, Direction> pDirectionGetter,
      DirectionProperty pDirectionProperty,
      BlockState pState,
      LevelAccessor pLevel,
      BlockPos pPos,
      BiPredicate<LevelAccessor, BlockPos> pBlockedChestTest
   ) {
      S $$8 = pBlockEntityType.getBlockEntity(pLevel, pPos);
      if ($$8 == null) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      } else if (pBlockedChestTest.test(pLevel, pPos)) {
         return DoubleBlockCombiner.Combiner::acceptNone;
      } else {
         DoubleBlockCombiner.BlockType $$9 = pDoubleBlockTypeGetter.apply(pState);
         boolean $$10 = $$9 == DoubleBlockCombiner.BlockType.SINGLE;
         boolean $$11 = $$9 == DoubleBlockCombiner.BlockType.FIRST;
         if ($$10) {
            return new DoubleBlockCombiner.NeighborCombineResult.Single<>($$8);
         } else {
            BlockPos $$12 = pPos.relative(pDirectionGetter.apply(pState));
            BlockState $$13 = pLevel.getBlockState($$12);
            if ($$13.is(pState.getBlock())) {
               DoubleBlockCombiner.BlockType $$14 = pDoubleBlockTypeGetter.apply($$13);
               if ($$14 != DoubleBlockCombiner.BlockType.SINGLE && $$9 != $$14 && $$13.getValue(pDirectionProperty) == pState.getValue(pDirectionProperty)) {
                  if (pBlockedChestTest.test(pLevel, $$12)) {
                     return DoubleBlockCombiner.Combiner::acceptNone;
                  }

                  S $$15 = pBlockEntityType.getBlockEntity(pLevel, $$12);
                  if ($$15 != null) {
                     S $$16 = $$11 ? $$8 : $$15;
                     S $$17 = $$11 ? $$15 : $$8;
                     return new DoubleBlockCombiner.NeighborCombineResult.Double<>($$16, $$17);
                  }
               }
            }

            return new DoubleBlockCombiner.NeighborCombineResult.Single<>($$8);
         }
      }
   }

   public static enum BlockType {
      SINGLE,
      FIRST,
      SECOND;
   }

   public interface Combiner<S, T> {
      T acceptDouble(S pFirst, S pSecond);

      T acceptSingle(S pSingle);

      T acceptNone();
   }

   public interface NeighborCombineResult<S> {
      <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> pCombiner);

      public static final class Double<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S first;
         private final S second;

         public Double(S pFirst, S pSecond) {
            this.first = pFirst;
            this.second = pSecond;
         }

         @Override
         public <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> pCombiner) {
            return pCombiner.acceptDouble(this.first, this.second);
         }
      }

      public static final class Single<S> implements DoubleBlockCombiner.NeighborCombineResult<S> {
         private final S single;

         public Single(S pSingle) {
            this.single = pSingle;
         }

         @Override
         public <T> T apply(DoubleBlockCombiner.Combiner<? super S, T> pCombiner) {
            return pCombiner.acceptSingle(this.single);
         }
      }
   }
}
