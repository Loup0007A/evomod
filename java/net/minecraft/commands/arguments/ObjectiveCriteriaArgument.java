package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ObjectiveCriteriaArgument implements ArgumentType<ObjectiveCriteria> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar.baz", "minecraft:foo");
   public static final DynamicCommandExceptionType ERROR_INVALID_VALUE = new DynamicCommandExceptionType(
      p_308355_ -> Component.translatableEscape("argument.criteria.invalid", p_308355_)
   );

   private ObjectiveCriteriaArgument() {
   }

   public static ObjectiveCriteriaArgument criteria() {
      return new ObjectiveCriteriaArgument();
   }

   public static ObjectiveCriteria getCriteria(CommandContext<CommandSourceStack> pContext, String pName) {
      return (ObjectiveCriteria)pContext.getArgument(pName, ObjectiveCriteria.class);
   }

   public ObjectiveCriteria parse(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();

      while (pReader.canRead() && pReader.peek() != ' ') {
         pReader.skip();
      }

      String $$2 = pReader.getString().substring($$1, pReader.getCursor());
      return ObjectiveCriteria.byName($$2).orElseThrow(() -> {
         pReader.setCursor($$1);
         return ERROR_INVALID_VALUE.createWithContext(pReader, $$2);
      });
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      List<String> $$2 = Lists.newArrayList(ObjectiveCriteria.getCustomCriteriaNames());

      for (StatType<?> $$3 : BuiltInRegistries.STAT_TYPE) {
         for (Object $$4 : $$3.getRegistry()) {
            String $$5 = this.getName($$3, $$4);
            $$2.add($$5);
         }
      }

      return SharedSuggestionProvider.suggest($$2, pBuilder);
   }

   public <T> String getName(StatType<T> pType, Object pValue) {
      return Stat.buildName(pType, (T)pValue);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
