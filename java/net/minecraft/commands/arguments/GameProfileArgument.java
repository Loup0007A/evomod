package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GameProfileArgument implements ArgumentType<GameProfileArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
   public static final SimpleCommandExceptionType ERROR_UNKNOWN_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.unknown"));

   public static Collection<GameProfile> getGameProfiles(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return ((GameProfileArgument.Result)pContext.getArgument(pName, GameProfileArgument.Result.class)).getNames((CommandSourceStack)pContext.getSource());
   }

   public static GameProfileArgument gameProfile() {
      return new GameProfileArgument();
   }

   public <S> GameProfileArgument.Result parse(StringReader p_345547_, S p_345566_) throws CommandSyntaxException {
      return parse(p_345547_, EntitySelectorParser.allowSelectors(p_345566_));
   }

   public GameProfileArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
      return parse(pReader, true);
   }

   private static GameProfileArgument.Result parse(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '@') {
         EntitySelectorParser $$2 = new EntitySelectorParser(pReader, pAllowSelectors);
         EntitySelector $$3 = $$2.parse();
         if ($$3.includesEntities()) {
            throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(pReader);
         } else {
            return new GameProfileArgument.SelectorResult($$3);
         }
      } else {
         int $$4 = pReader.getCursor();

         while (pReader.canRead() && pReader.peek() != ' ') {
            pReader.skip();
         }

         String $$5 = pReader.getString().substring($$4, pReader.getCursor());
         return p_94595_ -> {
            Optional<GameProfile> $$2 = p_94595_.getServer().getProfileCache().get($$5);
            return Collections.singleton($$2.orElseThrow(ERROR_UNKNOWN_PLAYER::create));
         };
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      if (pContext.getSource() instanceof SharedSuggestionProvider $$2) {
         StringReader $$3 = new StringReader(pBuilder.getInput());
         $$3.setCursor(pBuilder.getStart());
         EntitySelectorParser $$4 = new EntitySelectorParser($$3, EntitySelectorParser.allowSelectors($$2));

         try {
            $$4.parse();
         } catch (CommandSyntaxException var7) {
         }

         return $$4.fillSuggestions(pBuilder, p_94589_ -> SharedSuggestionProvider.suggest($$2.getOnlinePlayerNames(), p_94589_));
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @FunctionalInterface
   public interface Result {
      Collection<GameProfile> getNames(CommandSourceStack pSource) throws CommandSyntaxException;
   }

   public static class SelectorResult implements GameProfileArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector pSelector) {
         this.selector = pSelector;
      }

      @Override
      public Collection<GameProfile> getNames(CommandSourceStack pSource) throws CommandSyntaxException {
         List<ServerPlayer> $$1 = this.selector.findPlayers(pSource);
         if ($$1.isEmpty()) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
         } else {
            List<GameProfile> $$2 = Lists.newArrayList();

            for (ServerPlayer $$3 : $$1) {
               $$2.add($$3.getGameProfile());
            }

            return $$2;
         }
      }
   }
}
