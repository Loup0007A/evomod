package net.minecraft.advancements.critereon;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public interface MinMaxBounds<T extends Number> {
   SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.range.empty"));
   SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(Component.translatable("argument.range.swapped"));

   Optional<T> min();

   Optional<T> max();

   default boolean isAny() {
      return this.min().isEmpty() && this.max().isEmpty();
   }

   default Optional<T> unwrapPoint() {
      Optional<T> $$0 = this.min();
      Optional<T> $$1 = this.max();
      return $$0.equals($$1) ? $$0 : Optional.empty();
   }

   static <T extends Number, R extends MinMaxBounds<T>> Codec<R> createCodec(Codec<T> pCodec, MinMaxBounds.BoundsFactory<T, R> pBoundsFactory) {
      Codec<R> $$2 = RecordCodecBuilder.create(
         p_325235_ -> p_325235_.group(pCodec.optionalFieldOf("min").forGetter(MinMaxBounds::min), pCodec.optionalFieldOf("max").forGetter(MinMaxBounds::max))
               .apply(p_325235_, pBoundsFactory::create)
      );
      return Codec.either($$2, pCodec)
         .xmap(
            p_299505_ -> (MinMaxBounds)p_299505_.map(
                  p_300863_ -> p_300863_, p_300712_ -> pBoundsFactory.create(Optional.of((T)p_300712_), Optional.of((T)p_300712_))
               ),
            p_300263_ -> {
               Optional<T> $$1 = p_300263_.unwrapPoint();
               return $$1.isPresent() ? Either.right($$1.get()) : Either.left(p_300263_);
            }
         );
   }

   static <T extends Number, R extends MinMaxBounds<T>> R fromReader(
      StringReader pReader,
      MinMaxBounds.BoundsFromReaderFactory<T, R> pBoundedFactory,
      Function<String, T> pValueFactory,
      Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier,
      Function<T, T> pFormatter
   ) throws CommandSyntaxException {
      if (!pReader.canRead()) {
         throw ERROR_EMPTY.createWithContext(pReader);
      } else {
         int $$5 = pReader.getCursor();

         try {
            Optional<T> $$6 = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
            Optional<T> $$7;
            if (pReader.canRead(2) && pReader.peek() == '.' && pReader.peek(1) == '.') {
               pReader.skip();
               pReader.skip();
               $$7 = readNumber(pReader, pValueFactory, pCommandExceptionSupplier).map(pFormatter);
               if ($$6.isEmpty() && $$7.isEmpty()) {
                  throw ERROR_EMPTY.createWithContext(pReader);
               }
            } else {
               $$7 = $$6;
            }

            if ($$6.isEmpty() && $$7.isEmpty()) {
               throw ERROR_EMPTY.createWithContext(pReader);
            } else {
               return pBoundedFactory.create(pReader, $$6, $$7);
            }
         } catch (CommandSyntaxException var8) {
            pReader.setCursor($$5);
            throw new CommandSyntaxException(var8.getType(), var8.getRawMessage(), var8.getInput(), $$5);
         }
      }
   }

   private static <T extends Number> Optional<T> readNumber(
      StringReader pReader, Function<String, T> pStringToValueFunction, Supplier<DynamicCommandExceptionType> pCommandExceptionSupplier
   ) throws CommandSyntaxException {
      int $$3 = pReader.getCursor();

      while (pReader.canRead() && isAllowedInputChat(pReader)) {
         pReader.skip();
      }

      String $$4 = pReader.getString().substring($$3, pReader.getCursor());
      if ($$4.isEmpty()) {
         return Optional.empty();
      } else {
         try {
            return Optional.of(pStringToValueFunction.apply($$4));
         } catch (NumberFormatException var6) {
            throw pCommandExceptionSupplier.get().createWithContext(pReader, $$4);
         }
      }
   }

   private static boolean isAllowedInputChat(StringReader pReader) {
      char $$1 = pReader.peek();
      if (($$1 < '0' || $$1 > '9') && $$1 != '-') {
         return $$1 != '.' ? false : !pReader.canRead(2) || pReader.peek(1) != '.';
      } else {
         return true;
      }
   }

   @FunctionalInterface
   public interface BoundsFactory<T extends Number, R extends MinMaxBounds<T>> {
      R create(Optional<T> pMin, Optional<T> pMax);
   }

   @FunctionalInterface
   public interface BoundsFromReaderFactory<T extends Number, R extends MinMaxBounds<T>> {
      R create(StringReader pReader, Optional<T> pMin, Optional<T> pMax) throws CommandSyntaxException;
   }

   public static record Doubles(Optional<Double> min, Optional<Double> max, Optional<Double> minSq, Optional<Double> maxSq) implements MinMaxBounds<Double> {
      public static final MinMaxBounds.Doubles ANY = new MinMaxBounds.Doubles(Optional.empty(), Optional.empty());
      public static final Codec<MinMaxBounds.Doubles> CODEC = MinMaxBounds.createCodec(Codec.DOUBLE, MinMaxBounds.Doubles::new);

      private Doubles(Optional<Double> p_299492_, Optional<Double> p_300933_) {
         this(p_299492_, p_300933_, squareOpt(p_299492_), squareOpt(p_300933_));
      }

      private static MinMaxBounds.Doubles create(StringReader p_154796_, Optional<Double> p_299495_, Optional<Double> p_301206_) throws CommandSyntaxException {
         if (p_299495_.isPresent() && p_301206_.isPresent() && p_299495_.get() > p_301206_.get()) {
            throw ERROR_SWAPPED.createWithContext(p_154796_);
         } else {
            return new MinMaxBounds.Doubles(p_299495_, p_301206_);
         }
      }

      private static Optional<Double> squareOpt(Optional<Double> pValue) {
         return pValue.map(p_296138_ -> p_296138_ * p_296138_);
      }

      public static MinMaxBounds.Doubles exactly(double pValue) {
         return new MinMaxBounds.Doubles(Optional.of(pValue), Optional.of(pValue));
      }

      public static MinMaxBounds.Doubles between(double pMin, double pMax) {
         return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.of(pMax));
      }

      public static MinMaxBounds.Doubles atLeast(double pMin) {
         return new MinMaxBounds.Doubles(Optional.of(pMin), Optional.empty());
      }

      public static MinMaxBounds.Doubles atMost(double pMax) {
         return new MinMaxBounds.Doubles(Optional.empty(), Optional.of(pMax));
      }

      public boolean matches(double pValue) {
         return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || !(this.max.get() < pValue);
      }

      public boolean matchesSqr(double pValue) {
         return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || !(this.maxSq.get() < pValue);
      }

      public static MinMaxBounds.Doubles fromReader(StringReader pReader) throws CommandSyntaxException {
         return fromReader(pReader, p_154807_ -> p_154807_);
      }

      public static MinMaxBounds.Doubles fromReader(StringReader pReader, Function<Double, Double> pFormatter) throws CommandSyntaxException {
         return MinMaxBounds.fromReader(
            pReader, MinMaxBounds.Doubles::create, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble, pFormatter
         );
      }
   }

   public static record Ints(Optional<Integer> min, Optional<Integer> max, Optional<Long> minSq, Optional<Long> maxSq) implements MinMaxBounds<Integer> {
      public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(Optional.empty(), Optional.empty());
      public static final Codec<MinMaxBounds.Ints> CODEC = MinMaxBounds.createCodec(Codec.INT, MinMaxBounds.Ints::new);

      private Ints(Optional<Integer> p_299979_, Optional<Integer> p_297344_) {
         this(p_299979_, p_297344_, p_299979_.map(p_296140_ -> p_296140_.longValue() * p_296140_.longValue()), squareOpt(p_297344_));
      }

      private static MinMaxBounds.Ints create(StringReader p_55378_, Optional<Integer> p_297316_, Optional<Integer> p_300359_) throws CommandSyntaxException {
         if (p_297316_.isPresent() && p_300359_.isPresent() && p_297316_.get() > p_300359_.get()) {
            throw ERROR_SWAPPED.createWithContext(p_55378_);
         } else {
            return new MinMaxBounds.Ints(p_297316_, p_300359_);
         }
      }

      private static Optional<Long> squareOpt(Optional<Integer> pValue) {
         return pValue.map(p_296139_ -> p_296139_.longValue() * p_296139_.longValue());
      }

      public static MinMaxBounds.Ints exactly(int pValue) {
         return new MinMaxBounds.Ints(Optional.of(pValue), Optional.of(pValue));
      }

      public static MinMaxBounds.Ints between(int pMin, int pMax) {
         return new MinMaxBounds.Ints(Optional.of(pMin), Optional.of(pMax));
      }

      public static MinMaxBounds.Ints atLeast(int pMin) {
         return new MinMaxBounds.Ints(Optional.of(pMin), Optional.empty());
      }

      public static MinMaxBounds.Ints atMost(int pMax) {
         return new MinMaxBounds.Ints(Optional.empty(), Optional.of(pMax));
      }

      public boolean matches(int pValue) {
         return this.min.isPresent() && this.min.get() > pValue ? false : this.max.isEmpty() || this.max.get() >= pValue;
      }

      public boolean matchesSqr(long pValue) {
         return this.minSq.isPresent() && this.minSq.get() > pValue ? false : this.maxSq.isEmpty() || this.maxSq.get() >= pValue;
      }

      public static MinMaxBounds.Ints fromReader(StringReader pReader) throws CommandSyntaxException {
         return fromReader(pReader, p_55389_ -> p_55389_);
      }

      public static MinMaxBounds.Ints fromReader(StringReader pReader, Function<Integer, Integer> pValueFunction) throws CommandSyntaxException {
         return MinMaxBounds.fromReader(
            pReader, MinMaxBounds.Ints::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, pValueFunction
         );
      }
   }
}
