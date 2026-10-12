package net.minecraftforge.common.util;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ClearableLazy<T> extends Lazy<T> {
   void invalidate();

   static <T> ClearableLazy<T> of(@NotNull Supplier<T> supplier) {
      return new ClearableLazy.Fast<>(supplier);
   }

   static <T> ClearableLazy<T> concurrentOf(@NotNull Supplier<T> supplier) {
      return new ClearableLazy.Concurrent<>(supplier);
   }

   public static final class Concurrent<T> implements ClearableLazy<T> {
      private final Object lock = new Object();
      private final Supplier<T> supplier;
      private volatile T instance;

      private Concurrent(Supplier<T> supplier) {
         this.supplier = supplier;
      }

      @Nullable
      @Override
      public final T get() {
         T ret = this.instance;
         if (ret == null) {
            synchronized (this.lock) {
               if (this.instance == null) {
                  return this.instance = this.supplier.get();
               }
            }
         }

         return ret;
      }

      @Override
      public void invalidate() {
         synchronized (this.lock) {
            this.instance = null;
         }
      }
   }

   public static final class Fast<T> implements ClearableLazy<T> {
      private final Supplier<T> supplier;
      private T instance;

      private Fast(Supplier<T> supplier) {
         this.supplier = supplier;
      }

      @Nullable
      @Override
      public final T get() {
         if (this.instance == null) {
            this.instance = this.supplier.get();
         }

         return this.instance;
      }

      @Override
      public void invalidate() {
         this.instance = null;
      }
   }
}
