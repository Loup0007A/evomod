package net.minecraft;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Optionull {
   @Nullable
   public static <T, R> R map(@Nullable T pValue, Function<T, R> pMapper) {
      return pValue == null ? null : pMapper.apply(pValue);
   }

   public static <T, R> R mapOrDefault(@Nullable T pValue, Function<T, R> pMapper, R pDefaultValue) {
      return pValue == null ? pDefaultValue : pMapper.apply(pValue);
   }

   public static <T, R> R mapOrElse(@Nullable T pValue, Function<T, R> pMapper, Supplier<R> pSupplier) {
      return pValue == null ? pSupplier.get() : pMapper.apply(pValue);
   }

   @Nullable
   public static <T> T first(Collection<T> pCollection) {
      Iterator<T> $$1 = pCollection.iterator();
      return $$1.hasNext() ? $$1.next() : null;
   }

   public static <T> T firstOrDefault(Collection<T> pCollection, T pDefaultValue) {
      Iterator<T> $$2 = pCollection.iterator();
      return $$2.hasNext() ? $$2.next() : pDefaultValue;
   }

   public static <T> T firstOrElse(Collection<T> pCollection, Supplier<T> pSupplier) {
      Iterator<T> $$2 = pCollection.iterator();
      return $$2.hasNext() ? $$2.next() : pSupplier.get();
   }

   public static <T> boolean isNullOrEmpty(@Nullable T[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable boolean[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable byte[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable char[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable short[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable int[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable long[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable float[] pArray) {
      return pArray == null || pArray.length == 0;
   }

   public static boolean isNullOrEmpty(@Nullable double[] pArray) {
      return pArray == null || pArray.length == 0;
   }
}
