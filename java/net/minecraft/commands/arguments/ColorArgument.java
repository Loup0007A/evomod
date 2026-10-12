package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class ColorArgument implements ArgumentType<ChatFormatting> {
   private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308345_ -> Component.translatableEscape("argument.color.invalid", p_308345_)
   );

   private ColorArgument() {
   }

   public static ColorArgument color() {
      return new ColorArgument();
   }

   public static ChatFormatting getColor(CommandContext<CommandSourceStack> pContext, String pName) {
      return (ChatFormatting)pContext.getArgument(pName, ChatFormatting.class);
   }

   public ChatFormatting parse(StringReader pReader) throws CommandSyntaxException {
      String $$1 = pReader.readUnquotedString();
      ChatFormatting $$2 = ChatFormatting.getByName($$1);
      if ($$2 != null && !$$2.isFormat()) {
         return $$2;
      } else {
         throw ERROR_INVALID_VALUE.createWithContext(pReader, $$1);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggest(ChatFormatting.getNames(true, false), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
