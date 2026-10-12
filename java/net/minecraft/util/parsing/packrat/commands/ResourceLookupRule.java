package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class ResourceLookupRule<C, V> implements Rule<StringReader, V>, ResourceSuggestion {
   private final Atom<ResourceLocation> idParser;
   protected final C context;

   protected ResourceLookupRule(Atom<ResourceLocation> pIdParser, C pContext) {
      this.idParser = pIdParser;
      this.context = pContext;
   }

   @Override
   public Optional<V> parse(ParseState<StringReader> pParseState) {
      pParseState.input().skipWhitespace();
      int $$1 = pParseState.mark();
      Optional<ResourceLocation> $$2 = pParseState.parse(this.idParser);
      if ($$2.isPresent()) {
         try {
            return Optional.of(this.validateElement((ImmutableStringReader)pParseState.input(), $$2.get()));
         } catch (Exception var5) {
            pParseState.errorCollector().store($$1, this, var5);
            return Optional.empty();
         }
      } else {
         pParseState.errorCollector().store($$1, this, ResourceLocation.ERROR_INVALID.createWithContext((ImmutableStringReader)pParseState.input()));
         return Optional.empty();
      }
   }

   protected abstract V validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception;
}
