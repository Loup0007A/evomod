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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;

public class ScoreboardSlotArgument implements ArgumentType<DisplaySlot> {
   private static final Collection<String> EXAMPLES = Arrays.asList("sidebar", "foo.bar");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308385_ -> Component.translatableEscape("argument.scoreboardDisplaySlot.invalid", p_308385_)
   );

   private ScoreboardSlotArgument() {
   }

   public static ScoreboardSlotArgument displaySlot() {
      return new ScoreboardSlotArgument();
   }

   public static DisplaySlot getDisplaySlot(CommandContext<CommandSourceStack> pContext, String pSlot) {
      return (DisplaySlot)pContext.getArgument(pSlot, DisplaySlot.class);
   }

   public DisplaySlot parse(StringReader pReader) throws CommandSyntaxException {
      String $$1 = pReader.readUnquotedString();
      DisplaySlot $$2 = DisplaySlot.CODEC.byName($$1);
      if ($$2 == null) {
         throw ERROR_INVALID_VALUE.createWithContext(pReader, $$1);
      } else {
         return $$2;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggest(Arrays.stream(DisplaySlot.values()).map(DisplaySlot::getSerializedName), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
