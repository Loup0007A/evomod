package net.minecraft.util.parsing.packrat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public abstract class ParseState<S> {
   private final Map<ParseState.CacheKey<?>, ParseState.CacheEntry<?>> ruleCache = new HashMap<>();
   private final Dictionary<S> dictionary;
   private final ErrorCollector<S> errorCollector;

   protected ParseState(Dictionary<S> pDictionary, ErrorCollector<S> pErrorCollector) {
      this.dictionary = pDictionary;
      this.errorCollector = pErrorCollector;
   }

   public ErrorCollector<S> errorCollector() {
      return this.errorCollector;
   }

   public <T> Optional<T> parseTopRule(Atom<T> pAtom) {
      Optional<T> $$1 = this.parse(pAtom);
      if ($$1.isPresent()) {
         this.errorCollector.finish(this.mark());
      }

      return $$1;
   }

   public <T> Optional<T> parse(Atom<T> pAtom) {
      ParseState.CacheKey<T> $$1 = new ParseState.CacheKey<>(pAtom, this.mark());
      ParseState.CacheEntry<T> $$2 = this.lookupInCache($$1);
      if ($$2 != null) {
         this.restore($$2.mark());
         return $$2.value;
      } else {
         Rule<S, T> $$3 = this.dictionary.get(pAtom);
         if ($$3 == null) {
            throw new IllegalStateException("No symbol " + pAtom);
         } else {
            Optional<T> $$4 = $$3.parse(this);
            this.storeInCache($$1, $$4);
            return $$4;
         }
      }
   }

   @Nullable
   private <T> ParseState.CacheEntry<T> lookupInCache(ParseState.CacheKey<T> pKey) {
      return (ParseState.CacheEntry<T>)this.ruleCache.get(pKey);
   }

   private <T> void storeInCache(ParseState.CacheKey<T> pKey, Optional<T> pValue) {
      this.ruleCache.put(pKey, new ParseState.CacheEntry<>(pValue, this.mark()));
   }

   public abstract S input();

   public abstract int mark();

   public abstract void restore(int pCursor);

   static record CacheEntry<T>(Optional<T> value, int mark) {
   }

   static record CacheKey<T>(Atom<T> name, int mark) {
   }
}
