package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class Climate {
   private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
   private static final float QUANTIZATION_FACTOR = 10000.0F;
   @VisibleForTesting
   protected static final int PARAMETER_COUNT = 7;

   public static Climate.TargetPoint target(float pTemperature, float pHumidity, float pContinentalness, float pErosion, float pDepth, float pWeirdness) {
      return new Climate.TargetPoint(
         quantizeCoord(pTemperature),
         quantizeCoord(pHumidity),
         quantizeCoord(pContinentalness),
         quantizeCoord(pErosion),
         quantizeCoord(pDepth),
         quantizeCoord(pWeirdness)
      );
   }

   public static Climate.ParameterPoint parameters(
      float pTemperature, float pHumidity, float pContinentalness, float pErosion, float pDepth, float pWeirdness, float pOffset
   ) {
      return new Climate.ParameterPoint(
         Climate.Parameter.point(pTemperature),
         Climate.Parameter.point(pHumidity),
         Climate.Parameter.point(pContinentalness),
         Climate.Parameter.point(pErosion),
         Climate.Parameter.point(pDepth),
         Climate.Parameter.point(pWeirdness),
         quantizeCoord(pOffset)
      );
   }

   public static Climate.ParameterPoint parameters(
      Climate.Parameter pTemperature,
      Climate.Parameter pHumidity,
      Climate.Parameter pContinentalness,
      Climate.Parameter pErosion,
      Climate.Parameter pDepth,
      Climate.Parameter pWeirdness,
      float pOffset
   ) {
      return new Climate.ParameterPoint(pTemperature, pHumidity, pContinentalness, pErosion, pDepth, pWeirdness, quantizeCoord(pOffset));
   }

   public static long quantizeCoord(float pCoord) {
      return (long)(pCoord * 10000.0F);
   }

   public static float unquantizeCoord(long pCoord) {
      return (float)pCoord / 10000.0F;
   }

   public static Climate.Sampler empty() {
      DensityFunction $$0 = DensityFunctions.zero();
      return new Climate.Sampler($$0, $$0, $$0, $$0, $$0, $$0, List.of());
   }

   public static BlockPos findSpawnPosition(List<Climate.ParameterPoint> pPoints, Climate.Sampler pSampler) {
      return (new Climate.SpawnFinder(pPoints, pSampler)).result.location();
   }

   interface DistanceMetric<T> {
      long distance(Climate.RTree.Node<T> pNode, long[] pSearchedValues);
   }

   public static record Parameter(long min, long max) {
      public static final Codec<Climate.Parameter> CODEC = ExtraCodecs.intervalCodec(
         Codec.floatRange(-2.0F, 2.0F),
         "min",
         "max",
         (p_275164_, p_275165_) -> p_275164_.compareTo(p_275165_) > 0
               ? DataResult.error(() -> "Cannon construct interval, min > max (" + p_275164_ + " > " + p_275165_ + ")")
               : DataResult.success(new Climate.Parameter(Climate.quantizeCoord(p_275164_), Climate.quantizeCoord(p_275165_))),
         p_186841_ -> Climate.unquantizeCoord(p_186841_.min()),
         p_186839_ -> Climate.unquantizeCoord(p_186839_.max())
      );

      public static Climate.Parameter point(float pValue) {
         return span(pValue, pValue);
      }

      public static Climate.Parameter span(float pMin, float pMax) {
         if (pMin > pMax) {
            throw new IllegalArgumentException("min > max: " + pMin + " " + pMax);
         } else {
            return new Climate.Parameter(Climate.quantizeCoord(pMin), Climate.quantizeCoord(pMax));
         }
      }

      public static Climate.Parameter span(Climate.Parameter pMin, Climate.Parameter pMax) {
         if (pMin.min() > pMax.max()) {
            throw new IllegalArgumentException("min > max: " + pMin + " " + pMax);
         } else {
            return new Climate.Parameter(pMin.min(), pMax.max());
         }
      }

      @Override
      public String toString() {
         return this.min == this.max ? String.format(Locale.ROOT, "%d", this.min) : String.format(Locale.ROOT, "[%d-%d]", this.min, this.max);
      }

      public long distance(long pPointValue) {
         long $$1 = pPointValue - this.max;
         long $$2 = this.min - pPointValue;
         return $$1 > 0L ? $$1 : Math.max($$2, 0L);
      }

      public long distance(Climate.Parameter pParameter) {
         long $$1 = pParameter.min() - this.max;
         long $$2 = this.min - pParameter.max();
         return $$1 > 0L ? $$1 : Math.max($$2, 0L);
      }

      public Climate.Parameter span(@Nullable Climate.Parameter pParam) {
         return pParam == null ? this : new Climate.Parameter(Math.min(this.min, pParam.min()), Math.max(this.max, pParam.max()));
      }
   }

   public static class ParameterList<T> {
      private final List<Pair<Climate.ParameterPoint, T>> values;
      private final Climate.RTree<T> index;

      public static <T> Codec<Climate.ParameterList<T>> codec(MapCodec<T> pCodec) {
         return ExtraCodecs.nonEmptyList(
               RecordCodecBuilder.create(
                     p_275233_ -> p_275233_.group(
                              Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), pCodec.forGetter(Pair::getSecond)
                           )
                           .apply(p_275233_, Pair::of)
                  )
                  .listOf()
            )
            .xmap(Climate.ParameterList::new, Climate.ParameterList::values);
      }

      public ParameterList(List<Pair<Climate.ParameterPoint, T>> p_186849_) {
         this.values = p_186849_;
         this.index = Climate.RTree.create(p_186849_);
      }

      public List<Pair<Climate.ParameterPoint, T>> values() {
         return this.values;
      }

      public T findValue(Climate.TargetPoint pTargetPoint) {
         return this.findValueIndex(pTargetPoint);
      }

      @VisibleForTesting
      public T findValueBruteForce(Climate.TargetPoint pTargetPoint) {
         Iterator<Pair<Climate.ParameterPoint, T>> $$1 = this.values().iterator();
         Pair<Climate.ParameterPoint, T> $$2 = $$1.next();
         long $$3 = ((Climate.ParameterPoint)$$2.getFirst()).fitness(pTargetPoint);
         T $$4 = (T)$$2.getSecond();

         while ($$1.hasNext()) {
            Pair<Climate.ParameterPoint, T> $$5 = $$1.next();
            long $$6 = ((Climate.ParameterPoint)$$5.getFirst()).fitness(pTargetPoint);
            if ($$6 < $$3) {
               $$3 = $$6;
               $$4 = (T)$$5.getSecond();
            }
         }

         return $$4;
      }

      public T findValueIndex(Climate.TargetPoint pTargetPoint) {
         return this.findValueIndex(pTargetPoint, Climate.RTree.Node::distance);
      }

      protected T findValueIndex(Climate.TargetPoint pTargetPoint, Climate.DistanceMetric<T> pDistanceMetric) {
         return this.index.search(pTargetPoint, pDistanceMetric);
      }
   }

   public static record ParameterPoint(
      Climate.Parameter temperature,
      Climate.Parameter humidity,
      Climate.Parameter continentalness,
      Climate.Parameter erosion,
      Climate.Parameter depth,
      Climate.Parameter weirdness,
      long offset
   ) {
      public static final Codec<Climate.ParameterPoint> CODEC = RecordCodecBuilder.create(
         p_186885_ -> p_186885_.group(
                  Climate.Parameter.CODEC.fieldOf("temperature").forGetter(p_186905_ -> p_186905_.temperature),
                  Climate.Parameter.CODEC.fieldOf("humidity").forGetter(p_186902_ -> p_186902_.humidity),
                  Climate.Parameter.CODEC.fieldOf("continentalness").forGetter(p_186897_ -> p_186897_.continentalness),
                  Climate.Parameter.CODEC.fieldOf("erosion").forGetter(p_186894_ -> p_186894_.erosion),
                  Climate.Parameter.CODEC.fieldOf("depth").forGetter(p_186891_ -> p_186891_.depth),
                  Climate.Parameter.CODEC.fieldOf("weirdness").forGetter(p_186888_ -> p_186888_.weirdness),
                  Codec.floatRange(0.0F, 1.0F)
                     .fieldOf("offset")
                     .xmap(Climate::quantizeCoord, Climate::unquantizeCoord)
                     .forGetter(p_186881_ -> p_186881_.offset)
               )
               .apply(p_186885_, Climate.ParameterPoint::new)
      );

      long fitness(Climate.TargetPoint pPoint) {
         return Mth.square(this.temperature.distance(pPoint.temperature))
            + Mth.square(this.humidity.distance(pPoint.humidity))
            + Mth.square(this.continentalness.distance(pPoint.continentalness))
            + Mth.square(this.erosion.distance(pPoint.erosion))
            + Mth.square(this.depth.distance(pPoint.depth))
            + Mth.square(this.weirdness.distance(pPoint.weirdness))
            + Mth.square(this.offset);
      }

      protected List<Climate.Parameter> parameterSpace() {
         return ImmutableList.of(
            this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, new Climate.Parameter(this.offset, this.offset)
         );
      }
   }

   protected static final class RTree<T> {
      private static final int CHILDREN_PER_NODE = 6;
      private final Climate.RTree.Node<T> root;
      private final ThreadLocal<Climate.RTree.Leaf<T>> lastResult = new ThreadLocal<>();

      private RTree(Climate.RTree.Node<T> pRoot) {
         this.root = pRoot;
      }

      public static <T> Climate.RTree<T> create(List<Pair<Climate.ParameterPoint, T>> pNodes) {
         if (pNodes.isEmpty()) {
            throw new IllegalArgumentException("Need at least one value to build the search tree.");
         } else {
            int $$1 = ((Climate.ParameterPoint)pNodes.get(0).getFirst()).parameterSpace().size();
            if ($$1 != 7) {
               throw new IllegalStateException("Expecting parameter space to be 7, got " + $$1);
            } else {
               List<Climate.RTree.Leaf<T>> $$2 = pNodes.stream()
                  .map(p_186934_ -> new Climate.RTree.Leaf<>((Climate.ParameterPoint)p_186934_.getFirst(), p_186934_.getSecond()))
                  .collect(Collectors.toCollection(ArrayList::new));
               return new Climate.RTree<>(build($$1, $$2));
            }
         }
      }

      private static <T> Climate.RTree.Node<T> build(int pParamSpaceSize, List<? extends Climate.RTree.Node<T>> pChildren) {
         if (pChildren.isEmpty()) {
            throw new IllegalStateException("Need at least one child to build a node");
         } else if (pChildren.size() == 1) {
            return (Climate.RTree.Node<T>)pChildren.get(0);
         } else if (pChildren.size() <= 6) {
            pChildren.sort(Comparator.comparingLong(p_186916_ -> {
               long $$2x = 0L;

               for (int $$3x = 0; $$3x < pParamSpaceSize; $$3x++) {
                  Climate.Parameter $$4x = p_186916_.parameterSpace[$$3x];
                  $$2x += Math.abs(($$4x.min() + $$4x.max()) / 2L);
               }

               return $$2x;
            }));
            return new Climate.RTree.SubTree<>(pChildren);
         } else {
            long $$2 = Long.MAX_VALUE;
            int $$3 = -1;
            List<Climate.RTree.SubTree<T>> $$4 = null;

            for (int $$5 = 0; $$5 < pParamSpaceSize; $$5++) {
               sort(pChildren, pParamSpaceSize, $$5, false);
               List<Climate.RTree.SubTree<T>> $$6 = bucketize(pChildren);
               long $$7 = 0L;

               for (Climate.RTree.SubTree<T> $$8 : $$6) {
                  $$7 += cost($$8.parameterSpace);
               }

               if ($$2 > $$7) {
                  $$2 = $$7;
                  $$3 = $$5;
                  $$4 = $$6;
               }
            }

            sort($$4, pParamSpaceSize, $$3, true);
            return new Climate.RTree.SubTree<>(
               $$4.stream().map(p_186919_ -> build(pParamSpaceSize, Arrays.asList(p_186919_.children))).collect(Collectors.toList())
            );
         }
      }

      private static <T> void sort(List<? extends Climate.RTree.Node<T>> pChildren, int pParamSpaceSize, int pSize, boolean pAbsolute) {
         Comparator<Climate.RTree.Node<T>> $$4 = comparator(pSize, pAbsolute);

         for (int $$5 = 1; $$5 < pParamSpaceSize; $$5++) {
            $$4 = $$4.thenComparing(comparator((pSize + $$5) % pParamSpaceSize, pAbsolute));
         }

         pChildren.sort($$4);
      }

      private static <T> Comparator<Climate.RTree.Node<T>> comparator(int pSize, boolean pAbsolute) {
         return Comparator.comparingLong(p_186929_ -> {
            Climate.Parameter $$3 = p_186929_.parameterSpace[pSize];
            long $$4 = ($$3.min() + $$3.max()) / 2L;
            return pAbsolute ? Math.abs($$4) : $$4;
         });
      }

      private static <T> List<Climate.RTree.SubTree<T>> bucketize(List<? extends Climate.RTree.Node<T>> pNodes) {
         List<Climate.RTree.SubTree<T>> $$1 = Lists.newArrayList();
         List<Climate.RTree.Node<T>> $$2 = Lists.newArrayList();
         int $$3 = (int)Math.pow(6.0, Math.floor(Math.log((double)pNodes.size() - 0.01) / Math.log(6.0)));

         for (Climate.RTree.Node<T> $$4 : pNodes) {
            $$2.add($$4);
            if ($$2.size() >= $$3) {
               $$1.add(new Climate.RTree.SubTree<>($$2));
               $$2 = Lists.newArrayList();
            }
         }

         if (!$$2.isEmpty()) {
            $$1.add(new Climate.RTree.SubTree<>($$2));
         }

         return $$1;
      }

      private static long cost(Climate.Parameter[] pParameters) {
         long $$1 = 0L;

         for (Climate.Parameter $$2 : pParameters) {
            $$1 += Math.abs($$2.max() - $$2.min());
         }

         return $$1;
      }

      static <T> List<Climate.Parameter> buildParameterSpace(List<? extends Climate.RTree.Node<T>> pChildren) {
         if (pChildren.isEmpty()) {
            throw new IllegalArgumentException("SubTree needs at least one child");
         } else {
            int $$1 = 7;
            List<Climate.Parameter> $$2 = Lists.newArrayList();

            for (int $$3 = 0; $$3 < 7; $$3++) {
               $$2.add(null);
            }

            for (Climate.RTree.Node<T> $$4 : pChildren) {
               for (int $$5 = 0; $$5 < 7; $$5++) {
                  $$2.set($$5, $$4.parameterSpace[$$5].span($$2.get($$5)));
               }
            }

            return $$2;
         }
      }

      public T search(Climate.TargetPoint pTargetPoint, Climate.DistanceMetric<T> pDistanceMetric) {
         long[] $$2 = pTargetPoint.toParameterArray();
         Climate.RTree.Leaf<T> $$3 = this.root.search($$2, this.lastResult.get(), pDistanceMetric);
         this.lastResult.set($$3);
         return $$3.value;
      }

      static final class Leaf<T> extends Climate.RTree.Node<T> {
         final T value;

         Leaf(Climate.ParameterPoint pPoint, T pValue) {
            super(pPoint.parameterSpace());
            this.value = pValue;
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] pSearchedValues, @Nullable Climate.RTree.Leaf<T> pLeaf, Climate.DistanceMetric<T> pMetric) {
            return this;
         }
      }

      abstract static class Node<T> {
         protected final Climate.Parameter[] parameterSpace;

         protected Node(List<Climate.Parameter> pParameters) {
            this.parameterSpace = pParameters.toArray(new Climate.Parameter[0]);
         }

         protected abstract Climate.RTree.Leaf<T> search(long[] pSearchedValues, @Nullable Climate.RTree.Leaf<T> pLeaf, Climate.DistanceMetric<T> pMetric);

         protected long distance(long[] pValues) {
            long $$1 = 0L;

            for (int $$2 = 0; $$2 < 7; $$2++) {
               $$1 += Mth.square(this.parameterSpace[$$2].distance(pValues[$$2]));
            }

            return $$1;
         }

         @Override
         public String toString() {
            return Arrays.toString((Object[])this.parameterSpace);
         }
      }

      static final class SubTree<T> extends Climate.RTree.Node<T> {
         final Climate.RTree.Node<T>[] children;

         protected SubTree(List<? extends Climate.RTree.Node<T>> pParameters) {
            this(Climate.RTree.buildParameterSpace(pParameters), pParameters);
         }

         protected SubTree(List<Climate.Parameter> pParameters, List<? extends Climate.RTree.Node<T>> pChildren) {
            super(pParameters);
            this.children = pChildren.toArray(new Climate.RTree.Node[0]);
         }

         @Override
         protected Climate.RTree.Leaf<T> search(long[] pSearchedValues, @Nullable Climate.RTree.Leaf<T> pLeaf, Climate.DistanceMetric<T> pMetric) {
            long $$3 = pLeaf == null ? Long.MAX_VALUE : pMetric.distance(pLeaf, pSearchedValues);
            Climate.RTree.Leaf<T> $$4 = pLeaf;

            for (Climate.RTree.Node<T> $$5 : this.children) {
               long $$6 = pMetric.distance($$5, pSearchedValues);
               if ($$3 > $$6) {
                  Climate.RTree.Leaf<T> $$7 = $$5.search(pSearchedValues, $$4, pMetric);
                  long $$8 = $$5 == $$7 ? $$6 : pMetric.distance($$7, pSearchedValues);
                  if ($$3 > $$8) {
                     $$3 = $$8;
                     $$4 = $$7;
                  }
               }
            }

            return $$4;
         }
      }
   }

   public static record Sampler(
      DensityFunction temperature,
      DensityFunction humidity,
      DensityFunction continentalness,
      DensityFunction erosion,
      DensityFunction depth,
      DensityFunction weirdness,
      List<Climate.ParameterPoint> spawnTarget
   ) {
      public Climate.TargetPoint sample(int pX, int pY, int pZ) {
         int $$3 = QuartPos.toBlock(pX);
         int $$4 = QuartPos.toBlock(pY);
         int $$5 = QuartPos.toBlock(pZ);
         DensityFunction.SinglePointContext $$6 = new DensityFunction.SinglePointContext($$3, $$4, $$5);
         return Climate.target(
            (float)this.temperature.compute($$6),
            (float)this.humidity.compute($$6),
            (float)this.continentalness.compute($$6),
            (float)this.erosion.compute($$6),
            (float)this.depth.compute($$6),
            (float)this.weirdness.compute($$6)
         );
      }

      public BlockPos findSpawnPosition() {
         return this.spawnTarget.isEmpty() ? BlockPos.ZERO : Climate.findSpawnPosition(this.spawnTarget, this);
      }
   }

   static class SpawnFinder {
      Climate.SpawnFinder.Result result;

      SpawnFinder(List<Climate.ParameterPoint> pPoints, Climate.Sampler pSampler) {
         this.result = getSpawnPositionAndFitness(pPoints, pSampler, 0, 0);
         this.radialSearch(pPoints, pSampler, 2048.0F, 512.0F);
         this.radialSearch(pPoints, pSampler, 512.0F, 32.0F);
      }

      private void radialSearch(List<Climate.ParameterPoint> pPoint, Climate.Sampler pSampler, float pMax, float pMin) {
         float $$4 = 0.0F;
         float $$5 = pMin;
         BlockPos $$6 = this.result.location();

         while ($$5 <= pMax) {
            int $$7 = $$6.getX() + (int)(Math.sin((double)$$4) * (double)$$5);
            int $$8 = $$6.getZ() + (int)(Math.cos((double)$$4) * (double)$$5);
            Climate.SpawnFinder.Result $$9 = getSpawnPositionAndFitness(pPoint, pSampler, $$7, $$8);
            if ($$9.fitness() < this.result.fitness()) {
               this.result = $$9;
            }

            $$4 += pMin / $$5;
            if ((double)$$4 > Math.PI * 2) {
               $$4 = 0.0F;
               $$5 += pMin;
            }
         }
      }

      private static Climate.SpawnFinder.Result getSpawnPositionAndFitness(List<Climate.ParameterPoint> pPoints, Climate.Sampler pSampler, int pX, int pZ) {
         double $$4 = Mth.square(2500.0);
         int $$5 = 2;
         long $$6 = (long)((double)Mth.square(10000.0F) * Math.pow((double)(Mth.square((long)pX) + Mth.square((long)pZ)) / $$4, 2.0));
         Climate.TargetPoint $$7 = pSampler.sample(QuartPos.fromBlock(pX), 0, QuartPos.fromBlock(pZ));
         Climate.TargetPoint $$8 = new Climate.TargetPoint($$7.temperature(), $$7.humidity(), $$7.continentalness(), $$7.erosion(), 0L, $$7.weirdness());
         long $$9 = Long.MAX_VALUE;

         for (Climate.ParameterPoint $$10 : pPoints) {
            $$9 = Math.min($$9, $$10.fitness($$8));
         }

         return new Climate.SpawnFinder.Result(new BlockPos(pX, 0, pZ), $$6 + $$9);
      }

      static record Result(BlockPos location, long fitness) {
      }
   }

   public static record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {
      @VisibleForTesting
      protected long[] toParameterArray() {
         return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
      }
   }
}
