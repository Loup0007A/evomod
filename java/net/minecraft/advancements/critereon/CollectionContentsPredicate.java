package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
   List<P> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(Codec<P> pTestCodec) {
      return pTestCodec.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(P... pTests) {
      return of(List.of(pTests));
   }

   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(List<P> p_330160_) {
      return (CollectionContentsPredicate<T, P>)(switch (p_330160_.size()) {
         case 0 -> new CollectionContentsPredicate.Zero();
         case 1 -> new CollectionContentsPredicate.Single(p_330160_.getFirst());
         default -> new CollectionContentsPredicate.Multiple(p_330160_);
      });
   }

   public static record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> pContents) {
         List<Predicate<T>> $$1 = new ArrayList<>(this.tests);

         for (T $$2 : pContents) {
            $$1.removeIf(p_331259_ -> p_331259_.test($$2));
            if ($$1.isEmpty()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return this.tests;
      }
   }

   public static record Single<T, P extends Predicate<T>>(P test) implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> pContents) {
         for (T $$1 : pContents) {
            if (this.test.test($$1)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return List.of(this.test);
      }
   }

   public static class Zero<T, P extends Predicate<T>> implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> pContents) {
         return true;
      }

      @Override
      public List<P> unpack() {
         return List.of();
      }
   }
}
