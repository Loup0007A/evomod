package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C, I extends ToFloatFunction<C>> extends ToFloatFunction<C> {
   @VisibleForDebug
   String parityString();

   CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> pVisitor);

   static <C, I extends ToFloatFunction<C>> Codec<CubicSpline<C, I>> codec(Codec<I> p_184263_) {
      MutableObject<Codec<CubicSpline<C, I>>> $$1 = new MutableObject();

      record Point<C, I extends ToFloatFunction<C>>(float location, CubicSpline<C, I> value, float derivative) {
      }

      Codec<Point<C, I>> $$2 = RecordCodecBuilder.create(
         p_326492_ -> p_326492_.group(
                  Codec.FLOAT.fieldOf("location").forGetter(Point::location),
                  Codec.lazyInitialized($$1::getValue).fieldOf("value").forGetter(Point::value),
                  Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
               )
               .apply(p_326492_, (p_184242_, p_184243_, p_184244_) -> new Point(p_184242_, p_184243_, p_184244_))
      );
      Codec<CubicSpline.Multipoint<C, I>> $$3 = RecordCodecBuilder.create(
         p_184267_ -> p_184267_.group(
                  p_184263_.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
                  ExtraCodecs.nonEmptyList($$2.listOf())
                     .fieldOf("points")
                     .forGetter(
                        p_184272_ -> IntStream.range(0, p_184272_.locations.length)
                              .mapToObj(
                                 p_184249_ -> new Point(
                                       p_184272_.locations()[p_184249_],
                                       (CubicSpline<C, I>)p_184272_.values().get(p_184249_),
                                       p_184272_.derivatives()[p_184249_]
                                    )
                              )
                              .toList()
                     )
               )
               .apply(p_184267_, (p_184258_, p_184259_) -> {
                  float[] $$2x = new float[p_184259_.size()];
                  com.google.common.collect.ImmutableList.Builder<CubicSpline<C, I>> $$3x = ImmutableList.builder();
                  float[] $$4 = new float[p_184259_.size()];

                  for (int $$5 = 0; $$5 < p_184259_.size(); $$5++) {
                     Point<C, I> $$6 = (Point<C, I>)p_184259_.get($$5);
                     $$2x[$$5] = $$6.location();
                     $$3x.add($$6.value());
                     $$4[$$5] = $$6.derivative();
                  }

                  return CubicSpline.Multipoint.create((I)p_184258_, $$2x, $$3x.build(), $$4);
               })
      );
      $$1.setValue(
         Codec.either(Codec.FLOAT, $$3)
            .xmap(
               p_184261_ -> (CubicSpline)p_184261_.map(CubicSpline.Constant::new, p_184246_ -> p_184246_),
               p_184251_ -> p_184251_ instanceof CubicSpline.Constant<C, I> $$1x ? Either.left($$1x.value()) : Either.right((CubicSpline.Multipoint)p_184251_)
            )
      );
      return (Codec<CubicSpline<C, I>>)$$1.getValue();
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> constant(float pValue) {
      return new CubicSpline.Constant<>(pValue);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I pCoordinate) {
      return new CubicSpline.Builder<>(pCoordinate);
   }

   static <C, I extends ToFloatFunction<C>> CubicSpline.Builder<C, I> builder(I pCoordinate, ToFloatFunction<Float> pValueTransformer) {
      return new CubicSpline.Builder<>(pCoordinate, pValueTransformer);
   }

   public static final class Builder<C, I extends ToFloatFunction<C>> {
      private final I coordinate;
      private final ToFloatFunction<Float> valueTransformer;
      private final FloatList locations = new FloatArrayList();
      private final List<CubicSpline<C, I>> values = Lists.newArrayList();
      private final FloatList derivatives = new FloatArrayList();

      protected Builder(I pCoordinate) {
         this(pCoordinate, ToFloatFunction.IDENTITY);
      }

      protected Builder(I pCoordinate, ToFloatFunction<Float> pValueTransformer) {
         this.coordinate = pCoordinate;
         this.valueTransformer = pValueTransformer;
      }

      public CubicSpline.Builder<C, I> addPoint(float pLocation, float pValue) {
         return this.addPoint(pLocation, new CubicSpline.Constant<>(this.valueTransformer.apply(pValue)), 0.0F);
      }

      public CubicSpline.Builder<C, I> addPoint(float pLocation, float pValue, float pDerivative) {
         return this.addPoint(pLocation, new CubicSpline.Constant<>(this.valueTransformer.apply(pValue)), pDerivative);
      }

      public CubicSpline.Builder<C, I> addPoint(float pLocation, CubicSpline<C, I> pValue) {
         return this.addPoint(pLocation, pValue, 0.0F);
      }

      private CubicSpline.Builder<C, I> addPoint(float pLocation, CubicSpline<C, I> pValue, float pDerivative) {
         if (!this.locations.isEmpty() && pLocation <= this.locations.getFloat(this.locations.size() - 1)) {
            throw new IllegalArgumentException("Please register points in ascending order");
         } else {
            this.locations.add(pLocation);
            this.values.add(pValue);
            this.derivatives.add(pDerivative);
            return this;
         }
      }

      public CubicSpline<C, I> build() {
         if (this.locations.isEmpty()) {
            throw new IllegalStateException("No elements added");
         } else {
            return CubicSpline.Multipoint.create(
               this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray()
            );
         }
      }
   }

   @VisibleForDebug
   public static record Constant<C, I extends ToFloatFunction<C>>(float value) implements CubicSpline<C, I> {
      @Override
      public float apply(C p_184313_) {
         return this.value;
      }

      @Override
      public String parityString() {
         return String.format(Locale.ROOT, "k=%.3f", this.value);
      }

      @Override
      public float minValue() {
         return this.value;
      }

      @Override
      public float maxValue() {
         return this.value;
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> p_211581_) {
         return this;
      }
   }

   public interface CoordinateVisitor<I> {
      I visit(I pCoordinate);
   }

   @VisibleForDebug
   public static record Multipoint<C, I extends ToFloatFunction<C>>(
      I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue
   ) implements CubicSpline<C, I> {
      public Multipoint(I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue) {
         validateSizes(locations, values, derivatives);
         this.coordinate = coordinate;
         this.locations = locations;
         this.values = values;
         this.derivatives = derivatives;
         this.minValue = minValue;
         this.maxValue = maxValue;
      }

      static <C, I extends ToFloatFunction<C>> CubicSpline.Multipoint<C, I> create(
         I pCoordinate, float[] pLocations, List<CubicSpline<C, I>> pValues, float[] pDerivatives
      ) {
         validateSizes(pLocations, pValues, pDerivatives);
         int $$4 = pLocations.length - 1;
         float $$5 = Float.POSITIVE_INFINITY;
         float $$6 = Float.NEGATIVE_INFINITY;
         float $$7 = pCoordinate.minValue();
         float $$8 = pCoordinate.maxValue();
         if ($$7 < pLocations[0]) {
            float $$9 = linearExtend($$7, pLocations, pValues.get(0).minValue(), pDerivatives, 0);
            float $$10 = linearExtend($$7, pLocations, pValues.get(0).maxValue(), pDerivatives, 0);
            $$5 = Math.min($$5, Math.min($$9, $$10));
            $$6 = Math.max($$6, Math.max($$9, $$10));
         }

         if ($$8 > pLocations[$$4]) {
            float $$11 = linearExtend($$8, pLocations, pValues.get($$4).minValue(), pDerivatives, $$4);
            float $$12 = linearExtend($$8, pLocations, pValues.get($$4).maxValue(), pDerivatives, $$4);
            $$5 = Math.min($$5, Math.min($$11, $$12));
            $$6 = Math.max($$6, Math.max($$11, $$12));
         }

         for (CubicSpline<C, I> $$13 : pValues) {
            $$5 = Math.min($$5, $$13.minValue());
            $$6 = Math.max($$6, $$13.maxValue());
         }

         for (int $$14 = 0; $$14 < $$4; $$14++) {
            float $$15 = pLocations[$$14];
            float $$16 = pLocations[$$14 + 1];
            float $$17 = $$16 - $$15;
            CubicSpline<C, I> $$18 = pValues.get($$14);
            CubicSpline<C, I> $$19 = pValues.get($$14 + 1);
            float $$20 = $$18.minValue();
            float $$21 = $$18.maxValue();
            float $$22 = $$19.minValue();
            float $$23 = $$19.maxValue();
            float $$24 = pDerivatives[$$14];
            float $$25 = pDerivatives[$$14 + 1];
            if ($$24 != 0.0F || $$25 != 0.0F) {
               float $$26 = $$24 * $$17;
               float $$27 = $$25 * $$17;
               float $$28 = Math.min($$20, $$22);
               float $$29 = Math.max($$21, $$23);
               float $$30 = $$26 - $$23 + $$20;
               float $$31 = $$26 - $$22 + $$21;
               float $$32 = -$$27 + $$22 - $$21;
               float $$33 = -$$27 + $$23 - $$20;
               float $$34 = Math.min($$30, $$32);
               float $$35 = Math.max($$31, $$33);
               $$5 = Math.min($$5, $$28 + 0.25F * $$34);
               $$6 = Math.max($$6, $$29 + 0.25F * $$35);
            }
         }

         return new CubicSpline.Multipoint<>(pCoordinate, pLocations, pValues, pDerivatives, $$5, $$6);
      }

      private static float linearExtend(float pCoordinate, float[] pLocations, float pValue, float[] pDerivatives, int pIndex) {
         float $$5 = pDerivatives[pIndex];
         return $$5 == 0.0F ? pValue : pValue + $$5 * (pCoordinate - pLocations[pIndex]);
      }

      private static <C, I extends ToFloatFunction<C>> void validateSizes(float[] pLocations, List<CubicSpline<C, I>> pValues, float[] pDerivatives) {
         if (pLocations.length != pValues.size() || pLocations.length != pDerivatives.length) {
            throw new IllegalArgumentException("All lengths must be equal, got: " + pLocations.length + " " + pValues.size() + " " + pDerivatives.length);
         } else if (pLocations.length == 0) {
            throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
         }
      }

      @Override
      public float apply(C pObject) {
         float $$1 = this.coordinate.apply(pObject);
         int $$2 = findIntervalStart(this.locations, $$1);
         int $$3 = this.locations.length - 1;
         if ($$2 < 0) {
            return linearExtend($$1, this.locations, this.values.get(0).apply(pObject), this.derivatives, 0);
         } else if ($$2 == $$3) {
            return linearExtend($$1, this.locations, this.values.get($$3).apply(pObject), this.derivatives, $$3);
         } else {
            float $$4 = this.locations[$$2];
            float $$5 = this.locations[$$2 + 1];
            float $$6 = ($$1 - $$4) / ($$5 - $$4);
            ToFloatFunction<C> $$7 = (ToFloatFunction<C>)this.values.get($$2);
            ToFloatFunction<C> $$8 = (ToFloatFunction<C>)this.values.get($$2 + 1);
            float $$9 = this.derivatives[$$2];
            float $$10 = this.derivatives[$$2 + 1];
            float $$11 = $$7.apply(pObject);
            float $$12 = $$8.apply(pObject);
            float $$13 = $$9 * ($$5 - $$4) - ($$12 - $$11);
            float $$14 = -$$10 * ($$5 - $$4) + ($$12 - $$11);
            return Mth.lerp($$6, $$11, $$12) + $$6 * (1.0F - $$6) * Mth.lerp($$6, $$13, $$14);
         }
      }

      private static int findIntervalStart(float[] pLocations, float pStart) {
         return Mth.binarySearch(0, pLocations.length, p_216142_ -> pStart < pLocations[p_216142_]) - 1;
      }

      @VisibleForTesting
      @Override
      public String parityString() {
         return "Spline{coordinate="
            + this.coordinate
            + ", locations="
            + this.toString(this.locations)
            + ", derivatives="
            + this.toString(this.derivatives)
            + ", values="
            + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
            + "}";
      }

      private String toString(float[] pLocations) {
         return "["
            + IntStream.range(0, pLocations.length)
               .mapToDouble(p_184338_ -> (double)pLocations[p_184338_])
               .mapToObj(p_184330_ -> String.format(Locale.ROOT, "%.3f", p_184330_))
               .collect(Collectors.joining(", "))
            + "]";
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> pVisitor) {
         return create(
            pVisitor.visit(this.coordinate), this.locations, this.values().stream().map(p_211588_ -> p_211588_.mapAll(pVisitor)).toList(), this.derivatives
         );
      }
   }
}
