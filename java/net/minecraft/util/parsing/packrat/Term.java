package net.minecraft.util.parsing.packrat;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.mutable.MutableBoolean;

public interface Term<S> {
   boolean parse(ParseState<S> pParseState, Scope pScope, Control pControl);

   static <S> Term<S> named(Atom<?> pName) {
      return new Term.Reference<>(pName);
   }

   static <S, T> Term<S> marker(Atom<T> pName, T pValue) {
      return new Term.Marker<>(pName, pValue);
   }

   @SafeVarargs
   static <S> Term<S> sequence(Term<S>... pElements) {
      return new Term.Sequence<>(List.of(pElements));
   }

   @SafeVarargs
   static <S> Term<S> alternative(Term<S>... pElements) {
      return new Term.Alternative<>(List.of(pElements));
   }

   static <S> Term<S> optional(Term<S> pTerm) {
      return new Term.Maybe<>(pTerm);
   }

   static <S> Term<S> cut() {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_333527_, Scope p_336097_, Control p_335047_) {
            p_335047_.cut();
            return true;
         }

         @Override
         public String toString() {
            return "↑";
         }
      };
   }

   static <S> Term<S> empty() {
      return new Term<S>() {
         @Override
         public boolean parse(ParseState<S> p_328418_, Scope p_332040_, Control p_328784_) {
            return true;
         }

         @Override
         public String toString() {
            return "ε";
         }
      };
   }

   public static record Alternative<S>(List<Term<S>> elements) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_328094_, Scope p_331753_, Control p_334626_) {
         MutableBoolean $$3 = new MutableBoolean();
         Control $$4 = $$3::setTrue;
         int $$5 = p_328094_.mark();

         for (Term<S> $$6 : this.elements) {
            if ($$3.isTrue()) {
               break;
            }

            Scope $$7 = new Scope();
            if ($$6.parse(p_328094_, $$7, $$4)) {
               p_331753_.putAll($$7);
               return true;
            }

            p_328094_.restore($$5);
         }

         return false;
      }
   }

   public static record Marker<S, T>(Atom<T> name, T value) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_332878_, Scope p_331621_, Control p_334053_) {
         p_331621_.put(this.name, this.value);
         return true;
      }
   }

   public static record Maybe<S>(Term<S> term) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_332001_, Scope p_329861_, Control p_331352_) {
         int $$3 = p_332001_.mark();
         if (!this.term.parse(p_332001_, p_329861_, p_331352_)) {
            p_332001_.restore($$3);
         }

         return true;
      }
   }

   public static record Reference<S, T>(Atom<T> name) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_332365_, Scope p_333205_, Control p_334292_) {
         Optional<T> $$3 = p_332365_.parse(this.name);
         if ($$3.isEmpty()) {
            return false;
         } else {
            p_333205_.put(this.name, $$3.get());
            return true;
         }
      }
   }

   public static record Sequence<S>(List<Term<S>> elements) implements Term<S> {
      @Override
      public boolean parse(ParseState<S> p_330195_, Scope p_336361_, Control p_328798_) {
         int $$3 = p_330195_.mark();

         for (Term<S> $$4 : this.elements) {
            if (!$$4.parse(p_330195_, p_336361_, p_328798_)) {
               p_330195_.restore($$3);
               return false;
            }
         }

         return true;
      }
   }
}
