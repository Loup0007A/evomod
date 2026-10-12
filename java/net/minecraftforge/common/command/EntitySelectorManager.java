package net.minecraftforge.common.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;

public class EntitySelectorManager {
   private static final HashMap<String, IEntitySelectorType> REGISTRY = new HashMap<>();
   private static final List<String> RESERVED_TOKENS = List.of("p", "a", "r", "s", "e");

   public static void register(String token, IEntitySelectorType type) {
      if (token.isEmpty()) {
         throw new IllegalArgumentException("Token must not be empty");
      } else if (RESERVED_TOKENS.contains(token)) {
         throw new IllegalArgumentException("Token clashes with vanilla @" + token);
      } else {
         for (char c : token.toCharArray()) {
            if (!StringReader.isAllowedInUnquotedString(c)) {
               throw new IllegalArgumentException("Token must only contain allowed characters");
            }
         }

         REGISTRY.put(token, type);
      }
   }

   public static EntitySelector parseSelector(EntitySelectorParser parser) throws CommandSyntaxException {
      if (parser.getReader().canRead()) {
         int i = parser.getReader().getCursor();
         String token = parser.getReader().readUnquotedString();
         IEntitySelectorType type = REGISTRY.get(token);
         if (type != null) {
            return type.build(parser);
         }

         parser.getReader().setCursor(i);
      }

      return null;
   }

   public static void fillSelectorSuggestions(SuggestionsBuilder suggestionBuilder) {
      for (Entry<String, IEntitySelectorType> entry : REGISTRY.entrySet()) {
         String token = entry.getKey();
         IEntitySelectorType type = entry.getValue();
         suggestionBuilder.suggest("@" + token, type.getSuggestionTooltip());
      }
   }
}
