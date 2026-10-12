package net.minecraft.util.parsing.packrat;

import java.util.Optional;

public interface Rule<S, T> {
   Optional<T> parse(ParseState<S> pParseState);

   static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.RuleAction<S, T> pAction) {
      return new Rule.WrappedTerm<>(pAction, pChild);
   }

   static <S, T> Rule<S, T> fromTerm(Term<S> pChild, Rule.SimpleRuleAction<T> pAction) {
      return new Rule.WrappedTerm<>((p_331302_, p_331658_) -> Optional.of(pAction.run(p_331658_)), pChild);
   }

   @FunctionalInterface
   public interface RuleAction<S, T> {
      Optional<T> run(ParseState<S> pParseState, Scope pScope);
   }

   @FunctionalInterface
   public interface SimpleRuleAction<T> {
      T run(Scope pScope);
   }

   public static record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
      @Override
      public Optional<T> parse(ParseState<S> p_328860_) {
         Scope $$1 = new Scope();
         return this.child.parse(p_328860_, $$1, Control.UNBOUND) ? this.action.run(p_328860_, $$1) : Optional.empty();
      }
   }
}
