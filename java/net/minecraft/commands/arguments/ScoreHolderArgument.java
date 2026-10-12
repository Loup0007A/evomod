package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (p_108221_, p_108222_) -> {
      StringReader $$2 = new StringReader(p_108222_.getInput());
      $$2.setCursor(p_108222_.getStart());
      EntitySelectorParser $$3 = new EntitySelectorParser($$2, EntitySelectorParser.allowSelectors((CommandSourceStack)p_108221_.getSource()));

      try {
         $$3.parse();
      } catch (CommandSyntaxException var5) {
      }

      return $$3.fillSuggestions(
         p_108222_, p_171606_ -> SharedSuggestionProvider.suggest(((CommandSourceStack)p_108221_.getSource()).getOnlinePlayerNames(), p_171606_)
      );
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
   final boolean multiple;

   public ScoreHolderArgument(boolean pMultiple) {
      this.multiple = pMultiple;
   }

   public static ScoreHolder getName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName).iterator().next();
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName, Collections::emptyList);
   }

   public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName, ((CommandSourceStack)pContext.getSource()).getServer().getScoreboard()::getTrackedPlayers);
   }

   public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> pContext, String pName, Supplier<Collection<ScoreHolder>> pObjectives) throws CommandSyntaxException {
      Collection<ScoreHolder> $$3 = ((ScoreHolderArgument.Result)pContext.getArgument(pName, ScoreHolderArgument.Result.class))
         .getNames((CommandSourceStack)pContext.getSource(), pObjectives);
      if ($$3.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return $$3;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
      return this.parse(pReader, true);
   }

   public <S> ScoreHolderArgument.Result parse(StringReader p_345560_, S p_345549_) throws CommandSyntaxException {
      return this.parse(p_345560_, EntitySelectorParser.allowSelectors(p_345549_));
   }

   private ScoreHolderArgument.Result parse(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '@') {
         EntitySelectorParser $$2 = new EntitySelectorParser(pReader, pAllowSelectors);
         EntitySelector $$3 = $$2.parse();
         if (!this.multiple && $$3.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(pReader);
         } else {
            return new ScoreHolderArgument.SelectorResult($$3);
         }
      } else {
         int $$4 = pReader.getCursor();

         while (pReader.canRead() && pReader.peek() != ' ') {
            pReader.skip();
         }

         String $$5 = pReader.getString().substring($$4, pReader.getCursor());
         if ($$5.equals("*")) {
            return (p_108231_, p_108232_) -> {
               Collection<ScoreHolder> $$2 = p_108232_.get();
               if ($$2.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return $$2;
               }
            };
         } else {
            List<ScoreHolder> $$6 = List.of(ScoreHolder.forNameOnly($$5));
            if ($$5.startsWith("#")) {
               return (p_108237_, p_108238_) -> $$6;
            } else {
               try {
                  UUID $$7 = UUID.fromString($$5);
                  return (p_308383_, p_308384_) -> {
                     MinecraftServer $$4x = p_308383_.getServer();
                     ScoreHolder $$5x = null;
                     List<ScoreHolder> $$6x = null;

                     for (ServerLevel $$7x : $$4x.getAllLevels()) {
                        Entity $$8 = $$7x.getEntity($$7);
                        if ($$8 != null) {
                           if ($$5x == null) {
                              $$5x = $$8;
                           } else {
                              if ($$6x == null) {
                                 $$6x = new ArrayList<>();
                                 $$6x.add($$5x);
                              }

                              $$6x.add($$8);
                           }
                        }
                     }

                     if ($$6x != null) {
                        return $$6x;
                     } else {
                        return $$5x != null ? List.of($$5x) : $$6;
                     }
                  };
               } catch (IllegalArgumentException var7) {
                  return (p_308379_, p_308380_) -> {
                     MinecraftServer $$4x = p_308379_.getServer();
                     ServerPlayer $$5x = $$4x.getPlayerList().getPlayerByName($$5);
                     return $$5x != null ? List.of($$5x) : $$6;
                  };
               }
            }
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;

      public void serializeToNetwork(ScoreHolderArgument.Info.Template pTemplate, FriendlyByteBuf pBuffer) {
         int $$2 = 0;
         if (pTemplate.multiple) {
            $$2 |= 1;
         }

         pBuffer.writeByte($$2);
      }

      public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
         byte $$1 = pBuffer.readByte();
         boolean $$2 = ($$1 & 1) != 0;
         return new ScoreHolderArgument.Info.Template($$2);
      }

      public void serializeToJson(ScoreHolderArgument.Info.Template pTemplate, JsonObject pJson) {
         pJson.addProperty("amount", pTemplate.multiple ? "multiple" : "single");
      }

      public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument pArgument) {
         return new ScoreHolderArgument.Info.Template(pArgument.multiple);
      }

      public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
         final boolean multiple;

         Template(final boolean pMultiple) {
            this.multiple = pMultiple;
         }

         public ScoreHolderArgument instantiate(CommandBuildContext pContext) {
            return new ScoreHolderArgument(this.multiple);
         }

         @Override
         public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
            return Info.this;
         }
      }
   }

   @FunctionalInterface
   public interface Result {
      Collection<ScoreHolder> getNames(CommandSourceStack pSource, Supplier<Collection<ScoreHolder>> pObjectives) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector pSelector) {
         this.selector = pSelector;
      }

      @Override
      public Collection<ScoreHolder> getNames(CommandSourceStack pSource, Supplier<Collection<ScoreHolder>> pObjectives) throws CommandSyntaxException {
         List<? extends Entity> $$2 = this.selector.findEntities(pSource);
         if ($$2.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            return List.copyOf($$2);
         }
      }
   }
}
