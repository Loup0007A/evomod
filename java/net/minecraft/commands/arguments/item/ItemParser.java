package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;

public class ItemParser {
   static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      p_308407_ -> Component.translatableEscape("argument.item.id.invalid", p_308407_)
   );
   static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
      p_308406_ -> Component.translatableEscape("arguments.item.component.unknown", p_308406_)
   );
   static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
      (p_325613_, p_325614_) -> Component.translatableEscape("arguments.item.component.malformed", p_325613_, p_325614_)
   );
   static final SimpleCommandExceptionType ERROR_EXPECTED_COMPONENT = new SimpleCommandExceptionType(
      Component.translatable("arguments.item.component.expected")
   );
   static final DynamicCommandExceptionType ERROR_REPEATED_COMPONENT = new DynamicCommandExceptionType(
      p_325615_ -> Component.translatableEscape("arguments.item.component.repeated", p_325615_)
   );
   private static final DynamicCommandExceptionType ERROR_MALFORMED_ITEM = new DynamicCommandExceptionType(
      p_325616_ -> Component.translatableEscape("arguments.item.malformed", p_325616_)
   );
   public static final char SYNTAX_START_COMPONENTS = '[';
   public static final char SYNTAX_END_COMPONENTS = ']';
   public static final char SYNTAX_COMPONENT_SEPARATOR = ',';
   public static final char SYNTAX_COMPONENT_ASSIGNMENT = '=';
   public static final char SYNTAX_REMOVED_COMPONENT = '!';
   static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   final HolderLookup.RegistryLookup<Item> items;
   final DynamicOps<Tag> registryOps;

   public ItemParser(HolderLookup.Provider pRegistries) {
      this.items = pRegistries.lookupOrThrow(Registries.ITEM);
      this.registryOps = pRegistries.createSerializationContext(NbtOps.INSTANCE);
   }

   public ItemParser.ItemResult parse(StringReader pReader) throws CommandSyntaxException {
      final MutableObject<Holder<Item>> $$1 = new MutableObject();
      final DataComponentPatch.Builder $$2 = DataComponentPatch.builder();
      this.parse(pReader, new ItemParser.Visitor() {
         @Override
         public void visitItem(Holder<Item> p_328041_) {
            $$1.setValue(p_328041_);
         }

         @Override
         public <T> void visitComponent(DataComponentType<T> p_331133_, T p_330958_) {
            $$2.set(p_331133_, p_330958_);
         }

         @Override
         public <T> void visitRemovedComponent(DataComponentType<T> p_342833_) {
            $$2.remove(p_342833_);
         }
      });
      Holder<Item> $$3 = Objects.requireNonNull((Holder<Item>)$$1.getValue(), "Parser gave no item");
      DataComponentPatch $$4 = $$2.build();
      validateComponents(pReader, $$3, $$4);
      return new ItemParser.ItemResult($$3, $$4);
   }

   private static void validateComponents(StringReader pReader, Holder<Item> pItem, DataComponentPatch pComponents) throws CommandSyntaxException {
      DataComponentMap $$3 = PatchedDataComponentMap.fromPatch(pItem.value().components(), pComponents);
      DataResult<Unit> $$4 = ItemStack.validateComponents($$3);
      $$4.getOrThrow(p_325612_ -> ERROR_MALFORMED_ITEM.createWithContext(pReader, p_325612_));
   }

   public void parse(StringReader pReader, ItemParser.Visitor pVisitor) throws CommandSyntaxException {
      int $$2 = pReader.getCursor();

      try {
         new ItemParser.State(pReader, pVisitor).parse();
      } catch (CommandSyntaxException var5) {
         pReader.setCursor($$2);
         throw var5;
      }
   }

   public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder pBuilder) {
      StringReader $$1 = new StringReader(pBuilder.getInput());
      $$1.setCursor(pBuilder.getStart());
      ItemParser.SuggestionsVisitor $$2 = new ItemParser.SuggestionsVisitor();
      ItemParser.State $$3 = new ItemParser.State($$1, $$2);

      try {
         $$3.parse();
      } catch (CommandSyntaxException var6) {
      }

      return $$2.resolveSuggestions(pBuilder, $$1);
   }

   public static record ItemResult(Holder<Item> item, DataComponentPatch components) {
   }

   class State {
      private final StringReader reader;
      private final ItemParser.Visitor visitor;

      State(final StringReader pReader, final ItemParser.Visitor pVisitor) {
         this.reader = pReader;
         this.visitor = pVisitor;
      }

      public void parse() throws CommandSyntaxException {
         this.visitor.visitSuggestions(this::suggestItem);
         this.readItem();
         this.visitor.visitSuggestions(this::suggestStartComponents);
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
            this.readComponents();
         }
      }

      private void readItem() throws CommandSyntaxException {
         int $$0 = this.reader.getCursor();
         ResourceLocation $$1 = ResourceLocation.read(this.reader);
         this.visitor.visitItem(ItemParser.this.items.get(ResourceKey.create(Registries.ITEM, $$1)).orElseThrow(() -> {
            this.reader.setCursor($$0);
            return ItemParser.ERROR_UNKNOWN_ITEM.createWithContext(this.reader, $$1);
         }));
      }

      private void readComponents() throws CommandSyntaxException {
         this.reader.expect('[');
         this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
         Set<DataComponentType<?>> $$0 = new ReferenceArraySet();

         while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == '!') {
               this.reader.skip();
               this.visitor.visitSuggestions(this::suggestComponent);
               DataComponentType<?> $$1 = readComponentType(this.reader);
               if (!$$0.add($$1)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create($$1);
               }

               this.visitor.visitRemovedComponent($$1);
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
            } else {
               DataComponentType<?> $$2 = readComponentType(this.reader);
               if (!$$0.add($$2)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create($$2);
               }

               this.visitor.visitSuggestions(this::suggestAssignment);
               this.reader.skipWhitespace();
               this.reader.expect('=');
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
               this.readComponent($$2);
               this.reader.skipWhitespace();
            }

            this.visitor.visitSuggestions(this::suggestNextOrEndComponents);
            if (!this.reader.canRead() || this.reader.peek() != ',') {
               break;
            }

            this.reader.skip();
            this.reader.skipWhitespace();
            this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
            if (!this.reader.canRead()) {
               throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(this.reader);
            }
         }

         this.reader.expect(']');
         this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
      }

      public static DataComponentType<?> readComponentType(StringReader pReader) throws CommandSyntaxException {
         if (!pReader.canRead()) {
            throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(pReader);
         } else {
            int $$1 = pReader.getCursor();
            ResourceLocation $$2 = ResourceLocation.read(pReader);
            DataComponentType<?> $$3 = BuiltInRegistries.DATA_COMPONENT_TYPE.get($$2);
            if ($$3 != null && !$$3.isTransient()) {
               return $$3;
            } else {
               pReader.setCursor($$1);
               throw ItemParser.ERROR_UNKNOWN_COMPONENT.createWithContext(pReader, $$2);
            }
         }
      }

      private <T> void readComponent(DataComponentType<T> pComponentType) throws CommandSyntaxException {
         int $$1 = this.reader.getCursor();
         Tag $$2 = new TagParser(this.reader).readValue();
         DataResult<T> $$3 = pComponentType.codecOrThrow().parse(ItemParser.this.registryOps, $$2);
         this.visitor.visitComponent(pComponentType, (T)$$3.getOrThrow(p_335662_ -> {
            this.reader.setCursor($$1);
            return ItemParser.ERROR_MALFORMED_COMPONENT.createWithContext(this.reader, pComponentType.toString(), p_335662_);
         }));
      }

      private CompletableFuture<Suggestions> suggestStartComponents(SuggestionsBuilder p_333169_) {
         if (p_333169_.getRemaining().isEmpty()) {
            p_333169_.suggest(String.valueOf('['));
         }

         return p_333169_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestNextOrEndComponents(SuggestionsBuilder p_335586_) {
         if (p_335586_.getRemaining().isEmpty()) {
            p_335586_.suggest(String.valueOf(','));
            p_335586_.suggest(String.valueOf(']'));
         }

         return p_335586_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestAssignment(SuggestionsBuilder p_335223_) {
         if (p_335223_.getRemaining().isEmpty()) {
            p_335223_.suggest(String.valueOf('='));
         }

         return p_335223_.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder p_329594_) {
         return SharedSuggestionProvider.suggestResource(ItemParser.this.items.listElementIds().map(ResourceKey::location), p_329594_);
      }

      private CompletableFuture<Suggestions> suggestComponentAssignmentOrRemoval(SuggestionsBuilder p_343738_) {
         p_343738_.suggest(String.valueOf('!'));
         return this.suggestComponent(p_343738_, String.valueOf('='));
      }

      private CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder p_342558_) {
         return this.suggestComponent(p_342558_, "");
      }

      private CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder pBuilder, String pSuffix) {
         String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
         SharedSuggestionProvider.filterResources(
            BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet(), $$2, p_328035_ -> p_328035_.getKey().location(), p_340973_ -> {
               DataComponentType<?> $$3 = p_340973_.getValue();
               if ($$3.codec() != null) {
                  ResourceLocation $$4 = p_340973_.getKey().location();
                  pBuilder.suggest($$4 + pSuffix);
               }
            }
         );
         return pBuilder.buildFuture();
      }
   }

   static class SuggestionsVisitor implements ItemParser.Visitor {
      private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = ItemParser.SUGGEST_NOTHING;

      @Override
      public void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> pSuggestions) {
         this.suggestions = pSuggestions;
      }

      public CompletableFuture<Suggestions> resolveSuggestions(SuggestionsBuilder pBuilder, StringReader pReader) {
         return this.suggestions.apply(pBuilder.createOffset(pReader.getCursor()));
      }
   }

   public interface Visitor {
      default void visitItem(Holder<Item> pItem) {
      }

      default <T> void visitComponent(DataComponentType<T> pComponentType, T pValue) {
      }

      default <T> void visitRemovedComponent(DataComponentType<T> pComponentType) {
      }

      default void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> pSuggestions) {
      }
   }
}
