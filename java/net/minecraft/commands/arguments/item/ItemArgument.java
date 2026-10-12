package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;

public class ItemArgument implements ArgumentType<ItemInput> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");
   private final ItemParser parser;

   public ItemArgument(CommandBuildContext pContext) {
      this.parser = new ItemParser(pContext);
   }

   public static ItemArgument item(CommandBuildContext pContext) {
      return new ItemArgument(pContext);
   }

   public ItemInput parse(StringReader pReader) throws CommandSyntaxException {
      ItemParser.ItemResult $$1 = this.parser.parse(pReader);
      return new ItemInput($$1.item(), $$1.components());
   }

   public static <S> ItemInput getItem(CommandContext<S> pContext, String pName) {
      return (ItemInput)pContext.getArgument(pName, ItemInput.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return this.parser.fillSuggestions(pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
