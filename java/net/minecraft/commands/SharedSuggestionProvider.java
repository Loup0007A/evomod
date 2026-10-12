package net.minecraft.commands;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;

public interface SharedSuggestionProvider {
   Collection<String> getOnlinePlayerNames();

   default Collection<String> getCustomTabSugggestions() {
      return this.getOnlinePlayerNames();
   }

   default Collection<String> getSelectedEntities() {
      return Collections.emptyList();
   }

   Collection<String> getAllTeams();

   Stream<ResourceLocation> getAvailableSounds();

   Stream<ResourceLocation> getRecipeNames();

   CompletableFuture<Suggestions> customSuggestion(CommandContext<?> pContext);

   default Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   default Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   Set<ResourceKey<Level>> levels();

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default void suggestRegistryElements(Registry<?> pRegistry, SharedSuggestionProvider.ElementSuggestionType pType, SuggestionsBuilder pBuilder) {
      if (pType.shouldSuggestTags()) {
         suggestResource(pRegistry.getTagNames().map(TagKey::location), pBuilder, "#");
      }

      if (pType.shouldSuggestElements()) {
         suggestResource(pRegistry.keySet(), pBuilder);
      }
   }

   CompletableFuture<Suggestions> suggestRegistryElements(
      ResourceKey<? extends Registry<?>> pResourceKey,
      SharedSuggestionProvider.ElementSuggestionType pRegistryKey,
      SuggestionsBuilder pBuilder,
      CommandContext<?> pContext
   );

   boolean hasPermission(int pPermissionLevel);

   static <T> void filterResources(Iterable<T> pResources, String pInput, Function<T, ResourceLocation> pLocationFunction, Consumer<T> pResourceConsumer) {
      boolean $$4 = pInput.indexOf(58) > -1;

      for (T $$5 : pResources) {
         ResourceLocation $$6 = pLocationFunction.apply($$5);
         if ($$4) {
            String $$7 = $$6.toString();
            if (matchesSubStr(pInput, $$7)) {
               pResourceConsumer.accept($$5);
            }
         } else if (matchesSubStr(pInput, $$6.getNamespace()) || $$6.getNamespace().equals("minecraft") && matchesSubStr(pInput, $$6.getPath())) {
            pResourceConsumer.accept($$5);
         }
      }
   }

   static <T> void filterResources(
      Iterable<T> pResources, String pRemaining, String pPrefix, Function<T, ResourceLocation> pLocationFunction, Consumer<T> pResourceConsumer
   ) {
      if (pRemaining.isEmpty()) {
         pResources.forEach(pResourceConsumer);
      } else {
         String $$5 = Strings.commonPrefix(pRemaining, pPrefix);
         if (!$$5.isEmpty()) {
            String $$6 = pRemaining.substring($$5.length());
            filterResources(pResources, $$6, pLocationFunction, pResourceConsumer);
         }
      }
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<ResourceLocation> pResources, SuggestionsBuilder pBuilder, String pPrefix) {
      String $$3 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(pResources, $$3, pPrefix, p_82985_ -> p_82985_, p_325587_ -> pBuilder.suggest(pPrefix + p_325587_));
      return pBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<ResourceLocation> pResources, SuggestionsBuilder pBuilder, String pPrefix) {
      return suggestResource(pResources::iterator, pBuilder, pPrefix);
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<ResourceLocation> pResources, SuggestionsBuilder pBuilder) {
      String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(pResources, $$2, p_82966_ -> p_82966_, p_82925_ -> pBuilder.suggest(p_82925_.toString()));
      return pBuilder.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Iterable<T> pResources, SuggestionsBuilder pBuilder, Function<T, ResourceLocation> pLocationFunction, Function<T, Message> pSuggestionFunction
   ) {
      String $$4 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(
         pResources, $$4, pLocationFunction, p_82922_ -> pBuilder.suggest(pLocationFunction.apply(p_82922_).toString(), pSuggestionFunction.apply(p_82922_))
      );
      return pBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<ResourceLocation> pResourceLocations, SuggestionsBuilder pBuilder) {
      return suggestResource(pResourceLocations::iterator, pBuilder);
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Stream<T> pResources, SuggestionsBuilder pBuilder, Function<T, ResourceLocation> pLocationFunction, Function<T, Message> pSuggestionFunction
   ) {
      return suggestResource(pResources::iterator, pBuilder, pLocationFunction, pSuggestionFunction);
   }

   static CompletableFuture<Suggestions> suggestCoordinates(
      String pRemaining, Collection<SharedSuggestionProvider.TextCoordinates> pCoordinates, SuggestionsBuilder pBuilder, Predicate<String> pValidator
   ) {
      List<String> $$4 = Lists.newArrayList();
      if (Strings.isNullOrEmpty(pRemaining)) {
         for (SharedSuggestionProvider.TextCoordinates $$5 : pCoordinates) {
            String $$6 = $$5.x + " " + $$5.y + " " + $$5.z;
            if (pValidator.test($$6)) {
               $$4.add($$5.x);
               $$4.add($$5.x + " " + $$5.y);
               $$4.add($$6);
            }
         }
      } else {
         String[] $$7 = pRemaining.split(" ");
         if ($$7.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates $$8 : pCoordinates) {
               String $$9 = $$7[0] + " " + $$8.y + " " + $$8.z;
               if (pValidator.test($$9)) {
                  $$4.add($$7[0] + " " + $$8.y);
                  $$4.add($$9);
               }
            }
         } else if ($$7.length == 2) {
            for (SharedSuggestionProvider.TextCoordinates $$10 : pCoordinates) {
               String $$11 = $$7[0] + " " + $$7[1] + " " + $$10.z;
               if (pValidator.test($$11)) {
                  $$4.add($$11);
               }
            }
         }
      }

      return suggest($$4, pBuilder);
   }

   static CompletableFuture<Suggestions> suggest2DCoordinates(
      String pRemaining, Collection<SharedSuggestionProvider.TextCoordinates> pCoordinates, SuggestionsBuilder pBuilder, Predicate<String> pValidator
   ) {
      List<String> $$4 = Lists.newArrayList();
      if (Strings.isNullOrEmpty(pRemaining)) {
         for (SharedSuggestionProvider.TextCoordinates $$5 : pCoordinates) {
            String $$6 = $$5.x + " " + $$5.z;
            if (pValidator.test($$6)) {
               $$4.add($$5.x);
               $$4.add($$6);
            }
         }
      } else {
         String[] $$7 = pRemaining.split(" ");
         if ($$7.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates $$8 : pCoordinates) {
               String $$9 = $$7[0] + " " + $$8.z;
               if (pValidator.test($$9)) {
                  $$4.add($$9);
               }
            }
         }
      }

      return suggest($$4, pBuilder);
   }

   static CompletableFuture<Suggestions> suggest(Iterable<String> pStrings, SuggestionsBuilder pBuilder) {
      String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (String $$3 : pStrings) {
         if (matchesSubStr($$2, $$3.toLowerCase(Locale.ROOT))) {
            pBuilder.suggest($$3);
         }
      }

      return pBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(Stream<String> pStrings, SuggestionsBuilder pBuilder) {
      String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);
      pStrings.filter(p_82975_ -> matchesSubStr($$2, p_82975_.toLowerCase(Locale.ROOT))).forEach(pBuilder::suggest);
      return pBuilder.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(String[] pStrings, SuggestionsBuilder pBuilder) {
      String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (String $$3 : pStrings) {
         if (matchesSubStr($$2, $$3.toLowerCase(Locale.ROOT))) {
            pBuilder.suggest($$3);
         }
      }

      return pBuilder.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggest(
      Iterable<T> pResources, SuggestionsBuilder pBuilder, Function<T, String> pStringFunction, Function<T, Message> pSuggestionFunction
   ) {
      String $$4 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (T $$5 : pResources) {
         String $$6 = pStringFunction.apply($$5);
         if (matchesSubStr($$4, $$6.toLowerCase(Locale.ROOT))) {
            pBuilder.suggest($$6, pSuggestionFunction.apply($$5));
         }
      }

      return pBuilder.buildFuture();
   }

   static boolean matchesSubStr(String pInput, String pSubstring) {
      for (int $$2 = 0; !pSubstring.startsWith(pInput, $$2); $$2++) {
         int $$3 = pSubstring.indexOf(46, $$2);
         int $$4 = pSubstring.indexOf(95, $$2);
         if (Math.max($$3, $$4) < 0) {
            return false;
         }

         if ($$3 >= 0 && $$4 >= 0) {
            $$2 = Math.min($$4, $$3);
         } else {
            $$2 = $$3 >= 0 ? $$3 : $$4;
         }
      }

      return true;
   }

   public static enum ElementSuggestionType {
      TAGS,
      ELEMENTS,
      ALL;

      public boolean shouldSuggestTags() {
         return this == TAGS || this == ALL;
      }

      public boolean shouldSuggestElements() {
         return this == ELEMENTS || this == ALL;
      }
   }

   public static class TextCoordinates {
      public static final SharedSuggestionProvider.TextCoordinates DEFAULT_LOCAL = new SharedSuggestionProvider.TextCoordinates("^", "^", "^");
      public static final SharedSuggestionProvider.TextCoordinates DEFAULT_GLOBAL = new SharedSuggestionProvider.TextCoordinates("~", "~", "~");
      public final String x;
      public final String y;
      public final String z;

      public TextCoordinates(String pX, String pY, String pZ) {
         this.x = pX;
         this.y = pY;
         this.z = pZ;
      }
   }
}
