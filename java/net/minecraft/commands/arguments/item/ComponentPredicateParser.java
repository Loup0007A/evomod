package net.minecraft.commands.arguments.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.TagParseRule;

public class ComponentPredicateParser {
   public static <T, C, P> Grammar<List<T>> createGrammar(ComponentPredicateParser.Context<T, C, P> pContext) {
      Atom<List<T>> $$1 = Atom.of("top");
      Atom<Optional<T>> $$2 = Atom.of("type");
      Atom<Unit> $$3 = Atom.of("any_type");
      Atom<T> $$4 = Atom.of("element_type");
      Atom<T> $$5 = Atom.of("tag_type");
      Atom<List<T>> $$6 = Atom.of("conditions");
      Atom<List<T>> $$7 = Atom.of("alternatives");
      Atom<T> $$8 = Atom.of("term");
      Atom<T> $$9 = Atom.of("negation");
      Atom<T> $$10 = Atom.of("test");
      Atom<C> $$11 = Atom.of("component_type");
      Atom<P> $$12 = Atom.of("predicate_type");
      Atom<ResourceLocation> $$13 = Atom.of("id");
      Atom<Tag> $$14 = Atom.of("tag");
      Dictionary<StringReader> $$15 = new Dictionary<>();
      $$15.put(
         $$1,
         Term.alternative(
            Term.sequence(Term.named($$2), StringReaderTerms.character('['), Term.cut(), Term.optional(Term.named($$6)), StringReaderTerms.character(']')),
            Term.named($$2)
         ),
         p_331933_ -> {
            Builder<T> $$3x = ImmutableList.builder();
            p_331933_.getOrThrow($$2).ifPresent($$3x::add);
            List<T> $$4x = p_331933_.get($$6);
            if ($$4x != null) {
               $$3x.addAll($$4x);
            }

            return $$3x.build();
         }
      );
      $$15.put(
         $$2,
         Term.alternative(Term.named($$4), Term.sequence(StringReaderTerms.character('#'), Term.cut(), Term.named($$5)), Term.named($$3)),
         p_333155_ -> Optional.ofNullable(p_333155_.getAny($$4, $$5))
      );
      $$15.put($$3, StringReaderTerms.character('*'), p_328666_ -> Unit.INSTANCE);
      $$15.put($$4, new ComponentPredicateParser.ElementLookupRule<>($$13, pContext));
      $$15.put($$5, new ComponentPredicateParser.TagLookupRule<>($$13, pContext));
      $$15.put($$6, Term.sequence(Term.named($$7), Term.optional(Term.sequence(StringReaderTerms.character(','), Term.named($$6)))), p_332096_ -> {
         T $$4x = pContext.anyOf(p_332096_.getOrThrow($$7));
         return Optional.ofNullable(p_332096_.get($$6)).map(p_331681_ -> Util.copyAndAdd($$4x, (List<T>)p_331681_)).orElse(List.of($$4x));
      });
      $$15.put($$7, Term.sequence(Term.named($$8), Term.optional(Term.sequence(StringReaderTerms.character('|'), Term.named($$7)))), p_334061_ -> {
         T $$3x = p_334061_.getOrThrow($$8);
         return Optional.ofNullable(p_334061_.get($$7)).map(p_334416_ -> Util.copyAndAdd($$3x, (List<T>)p_334416_)).orElse(List.of($$3x));
      });
      $$15.put(
         $$8,
         Term.alternative(Term.named($$10), Term.sequence(StringReaderTerms.character('!'), Term.named($$9))),
         p_335341_ -> p_335341_.getAnyOrThrow($$10, $$9)
      );
      $$15.put($$9, Term.named($$10), p_331974_ -> pContext.negate(p_331974_.getOrThrow($$10)));
      $$15.put(
         $$10,
         Term.alternative(
            Term.sequence(Term.named($$11), StringReaderTerms.character('='), Term.cut(), Term.named($$14)),
            Term.sequence(Term.named($$12), StringReaderTerms.character('~'), Term.cut(), Term.named($$14)),
            Term.named($$11)
         ),
         (p_329079_, p_335425_) -> {
            P $$6x = p_335425_.get($$12);

            try {
               if ($$6x != null) {
                  Tag $$7x = p_335425_.getOrThrow($$14);
                  return Optional.of(pContext.createPredicateTest((ImmutableStringReader)p_329079_.input(), $$6x, $$7x));
               } else {
                  C $$8x = p_335425_.getOrThrow($$11);
                  Tag $$9x = p_335425_.get($$14);
                  return Optional.of(
                     $$9x != null
                        ? pContext.createComponentTest((ImmutableStringReader)p_329079_.input(), $$8x, $$9x)
                        : pContext.createComponentTest((ImmutableStringReader)p_329079_.input(), $$8x)
                  );
               }
            } catch (CommandSyntaxException var9x) {
               p_329079_.errorCollector().store(p_329079_.mark(), var9x);
               return Optional.empty();
            }
         }
      );
      $$15.put($$11, new ComponentPredicateParser.ComponentLookupRule<>($$13, pContext));
      $$15.put($$12, new ComponentPredicateParser.PredicateLookupRule<>($$13, pContext));
      $$15.put($$14, TagParseRule.INSTANCE);
      $$15.put($$13, ResourceLocationParseRule.INSTANCE);
      return new Grammar<>($$15, $$1);
   }

   static class ComponentLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, C> {
      ComponentLookupRule(Atom<ResourceLocation> pIdParser, ComponentPredicateParser.Context<T, C, P> pContext) {
         super(pIdParser, pContext);
      }

      @Override
      protected C validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception {
         return this.context.lookupComponentType(pReader, pElementType);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listComponentTypes();
      }
   }

   public interface Context<T, C, P> {
      T forElementType(ImmutableStringReader pReader, ResourceLocation pElementType) throws CommandSyntaxException;

      Stream<ResourceLocation> listElementTypes();

      T forTagType(ImmutableStringReader pReader, ResourceLocation pTagType) throws CommandSyntaxException;

      Stream<ResourceLocation> listTagTypes();

      C lookupComponentType(ImmutableStringReader pReader, ResourceLocation pComponentType) throws CommandSyntaxException;

      Stream<ResourceLocation> listComponentTypes();

      T createComponentTest(ImmutableStringReader pReader, C pContext, Tag pValue) throws CommandSyntaxException;

      T createComponentTest(ImmutableStringReader pReader, C pContext);

      P lookupPredicateType(ImmutableStringReader pReader, ResourceLocation pPredicateType) throws CommandSyntaxException;

      Stream<ResourceLocation> listPredicateTypes();

      T createPredicateTest(ImmutableStringReader pReader, P pPredicate, Tag pValue) throws CommandSyntaxException;

      T negate(T pValue);

      T anyOf(List<T> pValues);
   }

   static class ElementLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      ElementLookupRule(Atom<ResourceLocation> pIdParser, ComponentPredicateParser.Context<T, C, P> pContext) {
         super(pIdParser, pContext);
      }

      @Override
      protected T validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception {
         return this.context.forElementType(pReader, pElementType);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listElementTypes();
      }
   }

   static class PredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, P> {
      PredicateLookupRule(Atom<ResourceLocation> pIdParser, ComponentPredicateParser.Context<T, C, P> pContext) {
         super(pIdParser, pContext);
      }

      @Override
      protected P validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception {
         return this.context.lookupPredicateType(pReader, pElementType);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listPredicateTypes();
      }
   }

   static class TagLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, T> {
      TagLookupRule(Atom<ResourceLocation> pIdParser, ComponentPredicateParser.Context<T, C, P> pContext) {
         super(pIdParser, pContext);
      }

      @Override
      protected T validateElement(ImmutableStringReader pReader, ResourceLocation pElementType) throws Exception {
         return this.context.forTagType(pReader, pElementType);
      }

      @Override
      public Stream<ResourceLocation> possibleResources() {
         return this.context.listTagTypes();
      }
   }
}
