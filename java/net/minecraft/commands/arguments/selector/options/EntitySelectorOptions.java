package net.minecraft.commands.arguments.selector.options;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class EntitySelectorOptions {
   private static final Map<String, EntitySelectorOptions.Option> OPTIONS = Maps.newHashMap();
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION = new DynamicCommandExceptionType(
      p_308416_ -> Component.translatableEscape("argument.entity.options.unknown", p_308416_)
   );
   public static final DynamicCommandExceptionType ERROR_INAPPLICABLE_OPTION = new DynamicCommandExceptionType(
      p_308412_ -> Component.translatableEscape("argument.entity.options.inapplicable", p_308412_)
   );
   public static final SimpleCommandExceptionType ERROR_RANGE_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.distance.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LEVEL_NEGATIVE = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.level.negative")
   );
   public static final SimpleCommandExceptionType ERROR_LIMIT_TOO_SMALL = new SimpleCommandExceptionType(
      Component.translatable("argument.entity.options.limit.toosmall")
   );
   public static final DynamicCommandExceptionType ERROR_SORT_UNKNOWN = new DynamicCommandExceptionType(
      p_308411_ -> Component.translatableEscape("argument.entity.options.sort.irreversible", p_308411_)
   );
   public static final DynamicCommandExceptionType ERROR_GAME_MODE_INVALID = new DynamicCommandExceptionType(
      p_308419_ -> Component.translatableEscape("argument.entity.options.mode.invalid", p_308419_)
   );
   public static final DynamicCommandExceptionType ERROR_ENTITY_TYPE_INVALID = new DynamicCommandExceptionType(
      p_308410_ -> Component.translatableEscape("argument.entity.options.type.invalid", p_308410_)
   );

   public static void register(String pId, EntitySelectorOptions.Modifier pHandler, Predicate<EntitySelectorParser> pPredicate, Component pTooltip) {
      OPTIONS.put(pId, new EntitySelectorOptions.Option(pHandler, pPredicate, pTooltip));
   }

   public static void bootStrap() {
      if (OPTIONS.isEmpty()) {
         register("name", p_299480_ -> {
            int $$1 = p_299480_.getReader().getCursor();
            boolean $$2 = p_299480_.shouldInvertValue();
            String $$3 = p_299480_.getReader().readString();
            if (p_299480_.hasNameNotEquals() && !$$2) {
               p_299480_.getReader().setCursor($$1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_299480_.getReader(), "name");
            } else {
               if ($$2) {
                  p_299480_.setHasNameNotEquals(true);
               } else {
                  p_299480_.setHasNameEquals(true);
               }

               p_299480_.addPredicate(p_175209_ -> p_175209_.getName().getString().equals($$3) != $$2);
            }
         }, p_121423_ -> !p_121423_.hasNameEquals(), Component.translatable("argument.entity.options.name.description"));
         register("distance", p_121421_ -> {
            int $$1 = p_121421_.getReader().getCursor();
            MinMaxBounds.Doubles $$2 = MinMaxBounds.Doubles.fromReader(p_121421_.getReader());
            if ((!$$2.min().isPresent() || !($$2.min().get() < 0.0)) && (!$$2.max().isPresent() || !($$2.max().get() < 0.0))) {
               p_121421_.setDistance($$2);
               p_121421_.setWorldLimited();
            } else {
               p_121421_.getReader().setCursor($$1);
               throw ERROR_RANGE_NEGATIVE.createWithContext(p_121421_.getReader());
            }
         }, p_121419_ -> p_121419_.getDistance().isAny(), Component.translatable("argument.entity.options.distance.description"));
         register("level", p_121417_ -> {
            int $$1 = p_121417_.getReader().getCursor();
            MinMaxBounds.Ints $$2 = MinMaxBounds.Ints.fromReader(p_121417_.getReader());
            if ((!$$2.min().isPresent() || $$2.min().get() >= 0) && (!$$2.max().isPresent() || $$2.max().get() >= 0)) {
               p_121417_.setLevel($$2);
               p_121417_.setIncludesEntities(false);
            } else {
               p_121417_.getReader().setCursor($$1);
               throw ERROR_LEVEL_NEGATIVE.createWithContext(p_121417_.getReader());
            }
         }, p_121415_ -> p_121415_.getLevel().isAny(), Component.translatable("argument.entity.options.level.description"));
         register("x", p_121413_ -> {
            p_121413_.setWorldLimited();
            p_121413_.setX(p_121413_.getReader().readDouble());
         }, p_121411_ -> p_121411_.getX() == null, Component.translatable("argument.entity.options.x.description"));
         register("y", p_121409_ -> {
            p_121409_.setWorldLimited();
            p_121409_.setY(p_121409_.getReader().readDouble());
         }, p_121407_ -> p_121407_.getY() == null, Component.translatable("argument.entity.options.y.description"));
         register("z", p_121405_ -> {
            p_121405_.setWorldLimited();
            p_121405_.setZ(p_121405_.getReader().readDouble());
         }, p_121403_ -> p_121403_.getZ() == null, Component.translatable("argument.entity.options.z.description"));
         register("dx", p_121401_ -> {
            p_121401_.setWorldLimited();
            p_121401_.setDeltaX(p_121401_.getReader().readDouble());
         }, p_121399_ -> p_121399_.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
         register("dy", p_121397_ -> {
            p_121397_.setWorldLimited();
            p_121397_.setDeltaY(p_121397_.getReader().readDouble());
         }, p_121395_ -> p_121395_.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
         register("dz", p_121562_ -> {
            p_121562_.setWorldLimited();
            p_121562_.setDeltaZ(p_121562_.getReader().readDouble());
         }, p_121560_ -> p_121560_.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
         register(
            "x_rotation",
            p_121558_ -> p_121558_.setRotX(WrappedMinMaxBounds.fromReader(p_121558_.getReader(), true, Mth::wrapDegrees)),
            p_121556_ -> p_121556_.getRotX() == WrappedMinMaxBounds.ANY,
            Component.translatable("argument.entity.options.x_rotation.description")
         );
         register(
            "y_rotation",
            p_121554_ -> p_121554_.setRotY(WrappedMinMaxBounds.fromReader(p_121554_.getReader(), true, Mth::wrapDegrees)),
            p_121552_ -> p_121552_.getRotY() == WrappedMinMaxBounds.ANY,
            Component.translatable("argument.entity.options.y_rotation.description")
         );
         register("limit", p_121550_ -> {
            int $$1 = p_121550_.getReader().getCursor();
            int $$2 = p_121550_.getReader().readInt();
            if ($$2 < 1) {
               p_121550_.getReader().setCursor($$1);
               throw ERROR_LIMIT_TOO_SMALL.createWithContext(p_121550_.getReader());
            } else {
               p_121550_.setMaxResults($$2);
               p_121550_.setLimited(true);
            }
         }, p_121548_ -> !p_121548_.isCurrentEntity() && !p_121548_.isLimited(), Component.translatable("argument.entity.options.limit.description"));
         register(
            "sort",
            p_247983_ -> {
               int $$1 = p_247983_.getReader().getCursor();
               String $$2 = p_247983_.getReader().readUnquotedString();
               p_247983_.setSuggestions(
                  (p_175153_, p_175154_) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), p_175153_)
               );

               p_247983_.setOrder(switch ($$2) {
                  case "nearest" -> EntitySelectorParser.ORDER_NEAREST;
                  case "furthest" -> EntitySelectorParser.ORDER_FURTHEST;
                  case "random" -> EntitySelectorParser.ORDER_RANDOM;
                  case "arbitrary" -> EntitySelector.ORDER_ARBITRARY;
                  default -> {
                     p_247983_.getReader().setCursor($$1);
                     throw ERROR_SORT_UNKNOWN.createWithContext(p_247983_.getReader(), $$2);
                  }
               });
               p_247983_.setSorted(true);
            },
            p_121544_ -> !p_121544_.isCurrentEntity() && !p_121544_.isSorted(),
            Component.translatable("argument.entity.options.sort.description")
         );
         register("gamemode", p_121542_ -> {
            p_121542_.setSuggestions((p_175193_, p_175194_) -> {
               String $$3x = p_175193_.getRemaining().toLowerCase(Locale.ROOT);
               boolean $$4x = !p_121542_.hasGamemodeNotEquals();
               boolean $$5 = true;
               if (!$$3x.isEmpty()) {
                  if ($$3x.charAt(0) == '!') {
                     $$4x = false;
                     $$3x = $$3x.substring(1);
                  } else {
                     $$5 = false;
                  }
               }

               for (GameType $$6 : GameType.values()) {
                  if ($$6.getName().toLowerCase(Locale.ROOT).startsWith($$3x)) {
                     if ($$5) {
                        p_175193_.suggest("!" + $$6.getName());
                     }

                     if ($$4x) {
                        p_175193_.suggest($$6.getName());
                     }
                  }
               }

               return p_175193_.buildFuture();
            });
            int $$1 = p_121542_.getReader().getCursor();
            boolean $$2 = p_121542_.shouldInvertValue();
            if (p_121542_.hasGamemodeNotEquals() && !$$2) {
               p_121542_.getReader().setCursor($$1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_121542_.getReader(), "gamemode");
            } else {
               String $$3 = p_121542_.getReader().readUnquotedString();
               GameType $$4 = GameType.byName($$3, null);
               if ($$4 == null) {
                  p_121542_.getReader().setCursor($$1);
                  throw ERROR_GAME_MODE_INVALID.createWithContext(p_121542_.getReader(), $$3);
               } else {
                  p_121542_.setIncludesEntities(false);
                  p_121542_.addPredicate(p_175190_ -> {
                     if (!(p_175190_ instanceof ServerPlayer)) {
                        return false;
                     } else {
                        GameType $$3x = ((ServerPlayer)p_175190_).gameMode.getGameModeForPlayer();
                        return $$2 ? $$3x != $$4 : $$3x == $$4;
                     }
                  });
                  if ($$2) {
                     p_121542_.setHasGamemodeNotEquals(true);
                  } else {
                     p_121542_.setHasGamemodeEquals(true);
                  }
               }
            }
         }, p_121540_ -> !p_121540_.hasGamemodeEquals(), Component.translatable("argument.entity.options.gamemode.description"));
         register("team", p_121538_ -> {
            boolean $$1 = p_121538_.shouldInvertValue();
            String $$2 = p_121538_.getReader().readUnquotedString();
            p_121538_.addPredicate(p_308415_ -> {
               if (!(p_308415_ instanceof LivingEntity)) {
                  return false;
               } else {
                  Team $$3 = p_308415_.getTeam();
                  String $$4 = $$3 == null ? "" : $$3.getName();
                  return $$4.equals($$2) != $$1;
               }
            });
            if ($$1) {
               p_121538_.setHasTeamNotEquals(true);
            } else {
               p_121538_.setHasTeamEquals(true);
            }
         }, p_121536_ -> !p_121536_.hasTeamEquals(), Component.translatable("argument.entity.options.team.description"));
         register("type", p_121534_ -> {
            p_121534_.setSuggestions((p_340977_, p_340978_) -> {
               SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_340977_, String.valueOf('!'));
               SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTagNames().map(TagKey::location), p_340977_, "!#");
               if (!p_121534_.isTypeLimitedInversely()) {
                  SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), p_340977_);
                  SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTagNames().map(TagKey::location), p_340977_, String.valueOf('#'));
               }

               return p_340977_.buildFuture();
            });
            int $$1 = p_121534_.getReader().getCursor();
            boolean $$2 = p_121534_.shouldInvertValue();
            if (p_121534_.isTypeLimitedInversely() && !$$2) {
               p_121534_.getReader().setCursor($$1);
               throw ERROR_INAPPLICABLE_OPTION.createWithContext(p_121534_.getReader(), "type");
            } else {
               if ($$2) {
                  p_121534_.setTypeLimitedInversely();
               }

               if (p_121534_.isTag()) {
                  TagKey<EntityType<?>> $$3 = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.read(p_121534_.getReader()));
                  p_121534_.addPredicate(p_205691_ -> p_205691_.getType().is($$3) != $$2);
               } else {
                  ResourceLocation $$4 = ResourceLocation.read(p_121534_.getReader());
                  EntityType<?> $$5 = BuiltInRegistries.ENTITY_TYPE.getOptional($$4).orElseThrow(() -> {
                     p_121534_.getReader().setCursor($$1);
                     return ERROR_ENTITY_TYPE_INVALID.createWithContext(p_121534_.getReader(), $$4.toString());
                  });
                  if (Objects.equals(EntityType.PLAYER, $$5) && !$$2) {
                     p_121534_.setIncludesEntities(false);
                  }

                  p_121534_.addPredicate(p_175151_ -> Objects.equals($$5, p_175151_.getType()) != $$2);
                  if (!$$2) {
                     p_121534_.limitToType($$5);
                  }
               }
            }
         }, p_121532_ -> !p_121532_.isTypeLimited(), Component.translatable("argument.entity.options.type.description"));
         register("tag", p_121530_ -> {
            boolean $$1 = p_121530_.shouldInvertValue();
            String $$2 = p_121530_.getReader().readUnquotedString();
            p_121530_.addPredicate(p_175166_ -> "".equals($$2) ? p_175166_.getTags().isEmpty() != $$1 : p_175166_.getTags().contains($$2) != $$1);
         }, p_121528_ -> true, Component.translatable("argument.entity.options.tag.description"));
         register("nbt", p_121526_ -> {
            boolean $$1 = p_121526_.shouldInvertValue();
            CompoundTag $$2 = new TagParser(p_121526_.getReader()).readStruct();
            p_121526_.addPredicate(p_175176_ -> {
               CompoundTag $$3 = p_175176_.saveWithoutId(new CompoundTag());
               if (p_175176_ instanceof ServerPlayer $$4) {
                  ItemStack $$5 = $$4.getInventory().getSelected();
                  if (!$$5.isEmpty()) {
                     $$3.put("SelectedItem", $$5.save($$4.registryAccess()));
                  }
               }

               return NbtUtils.compareNbt($$2, $$3, true) != $$1;
            });
         }, p_121524_ -> true, Component.translatable("argument.entity.options.nbt.description"));
         register("scores", p_121522_ -> {
            StringReader $$1 = p_121522_.getReader();
            Map<String, MinMaxBounds.Ints> $$2 = Maps.newHashMap();
            $$1.expect('{');
            $$1.skipWhitespace();

            while ($$1.canRead() && $$1.peek() != '}') {
               $$1.skipWhitespace();
               String $$3 = $$1.readUnquotedString();
               $$1.skipWhitespace();
               $$1.expect('=');
               $$1.skipWhitespace();
               MinMaxBounds.Ints $$4 = MinMaxBounds.Ints.fromReader($$1);
               $$2.put($$3, $$4);
               $$1.skipWhitespace();
               if ($$1.canRead() && $$1.peek() == ',') {
                  $$1.skip();
               }
            }

            $$1.expect('}');
            if (!$$2.isEmpty()) {
               p_121522_.addPredicate(p_308418_ -> {
                  Scoreboard $$2x = p_308418_.getServer().getScoreboard();

                  for (Entry<String, MinMaxBounds.Ints> $$3x : $$2.entrySet()) {
                     Objective $$4x = $$2x.getObjective($$3x.getKey());
                     if ($$4x == null) {
                        return false;
                     }

                     ReadOnlyScoreInfo $$5 = $$2x.getPlayerScoreInfo(p_308418_, $$4x);
                     if ($$5 == null) {
                        return false;
                     }

                     if (!$$3x.getValue().matches($$5.value())) {
                        return false;
                     }
                  }

                  return true;
               });
            }

            p_121522_.setHasScores(true);
         }, p_121518_ -> !p_121518_.hasScores(), Component.translatable("argument.entity.options.scores.description"));
         register("advancements", p_121514_ -> {
            StringReader $$1 = p_121514_.getReader();
            Map<ResourceLocation, Predicate<AdvancementProgress>> $$2 = Maps.newHashMap();
            $$1.expect('{');
            $$1.skipWhitespace();

            while ($$1.canRead() && $$1.peek() != '}') {
               $$1.skipWhitespace();
               ResourceLocation $$3 = ResourceLocation.read($$1);
               $$1.skipWhitespace();
               $$1.expect('=');
               $$1.skipWhitespace();
               if ($$1.canRead() && $$1.peek() == '{') {
                  Map<String, Predicate<CriterionProgress>> $$4 = Maps.newHashMap();
                  $$1.skipWhitespace();
                  $$1.expect('{');
                  $$1.skipWhitespace();

                  while ($$1.canRead() && $$1.peek() != '}') {
                     $$1.skipWhitespace();
                     String $$5 = $$1.readUnquotedString();
                     $$1.skipWhitespace();
                     $$1.expect('=');
                     $$1.skipWhitespace();
                     boolean $$6 = $$1.readBoolean();
                     $$4.put($$5, p_175186_ -> p_175186_.isDone() == $$6);
                     $$1.skipWhitespace();
                     if ($$1.canRead() && $$1.peek() == ',') {
                        $$1.skip();
                     }
                  }

                  $$1.skipWhitespace();
                  $$1.expect('}');
                  $$1.skipWhitespace();
                  $$2.put($$3, p_175169_ -> {
                     for (Entry<String, Predicate<CriterionProgress>> $$2x : $$4.entrySet()) {
                        CriterionProgress $$3x = p_175169_.getCriterion($$2x.getKey());
                        if ($$3x == null || !$$2x.getValue().test($$3x)) {
                           return false;
                        }
                     }

                     return true;
                  });
               } else {
                  boolean $$7 = $$1.readBoolean();
                  $$2.put($$3, p_175183_ -> p_175183_.isDone() == $$7);
               }

               $$1.skipWhitespace();
               if ($$1.canRead() && $$1.peek() == ',') {
                  $$1.skip();
               }
            }

            $$1.expect('}');
            if (!$$2.isEmpty()) {
               p_121514_.addPredicate(p_296327_ -> {
                  if (!(p_296327_ instanceof ServerPlayer $$2x)) {
                     return false;
                  } else {
                     PlayerAdvancements $$4 = $$2x.getAdvancements();
                     ServerAdvancementManager $$5x = $$2x.getServer().getAdvancements();

                     for (Entry<ResourceLocation, Predicate<AdvancementProgress>> $$6x : $$2.entrySet()) {
                        AdvancementHolder $$7x = $$5x.get($$6x.getKey());
                        if ($$7x == null || !$$6x.getValue().test($$4.getOrStartProgress($$7x))) {
                           return false;
                        }
                     }

                     return true;
                  }
               });
               p_121514_.setIncludesEntities(false);
            }

            p_121514_.setHasAdvancements(true);
         }, p_121506_ -> !p_121506_.hasAdvancements(), Component.translatable("argument.entity.options.advancements.description"));
         register(
            "predicate",
            p_325634_ -> {
               boolean $$1 = p_325634_.shouldInvertValue();
               ResourceKey<LootItemCondition> $$2 = ResourceKey.create(Registries.PREDICATE, ResourceLocation.read(p_325634_.getReader()));
               p_325634_.addPredicate(
                  p_325637_ -> {
                     if (!(p_325637_.level() instanceof ServerLevel)) {
                        return false;
                     } else {
                        ServerLevel $$3 = (ServerLevel)p_325637_.level();
                        Optional<LootItemCondition> $$4 = $$3.getServer().reloadableRegistries().lookup().get(Registries.PREDICATE, $$2).map(Holder::value);
                        if ($$4.isEmpty()) {
                           return false;
                        } else {
                           LootParams $$5 = new LootParams.Builder($$3)
                              .withParameter(LootContextParams.THIS_ENTITY, p_325637_)
                              .withParameter(LootContextParams.ORIGIN, p_325637_.position())
                              .create(LootContextParamSets.SELECTOR);
                           LootContext $$6 = new LootContext.Builder($$5).create(Optional.empty());
                           $$6.pushVisitedElement(LootContext.createVisitedEntry($$4.get()));
                           return $$1 ^ $$4.get().test($$6);
                        }
                     }
                  }
               );
            },
            p_121435_ -> true,
            Component.translatable("argument.entity.options.predicate.description")
         );
      }
   }

   public static EntitySelectorOptions.Modifier get(EntitySelectorParser pParser, String pId, int pCursor) throws CommandSyntaxException {
      EntitySelectorOptions.Option $$3 = OPTIONS.get(pId);
      if ($$3 != null) {
         if ($$3.canUse.test(pParser)) {
            return $$3.modifier;
         } else {
            throw ERROR_INAPPLICABLE_OPTION.createWithContext(pParser.getReader(), pId);
         }
      } else {
         pParser.getReader().setCursor(pCursor);
         throw ERROR_UNKNOWN_OPTION.createWithContext(pParser.getReader(), pId);
      }
   }

   public static void suggestNames(EntitySelectorParser pParser, SuggestionsBuilder pBuilder) {
      String $$2 = pBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (Entry<String, EntitySelectorOptions.Option> $$3 : OPTIONS.entrySet()) {
         if ($$3.getValue().canUse.test(pParser) && $$3.getKey().toLowerCase(Locale.ROOT).startsWith($$2)) {
            pBuilder.suggest($$3.getKey() + "=", $$3.getValue().description);
         }
      }
   }

   public interface Modifier {
      void handle(EntitySelectorParser pParser) throws CommandSyntaxException;
   }

   static record Option(EntitySelectorOptions.Modifier modifier, Predicate<EntitySelectorParser> canUse, Component description) {
   }
}
