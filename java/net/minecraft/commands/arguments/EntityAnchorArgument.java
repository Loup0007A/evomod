package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityAnchorArgument implements ArgumentType<EntityAnchorArgument.Anchor> {
   private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
   private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(
      p_308348_ -> Component.translatableEscape("argument.anchor.invalid", p_308348_)
   );

   public static EntityAnchorArgument.Anchor getAnchor(CommandContext<CommandSourceStack> pContext, String pName) {
      return (EntityAnchorArgument.Anchor)pContext.getArgument(pName, EntityAnchorArgument.Anchor.class);
   }

   public static EntityAnchorArgument anchor() {
      return new EntityAnchorArgument();
   }

   public EntityAnchorArgument.Anchor parse(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();
      String $$2 = pReader.readUnquotedString();
      EntityAnchorArgument.Anchor $$3 = EntityAnchorArgument.Anchor.getByName($$2);
      if ($$3 == null) {
         pReader.setCursor($$1);
         throw ERROR_INVALID.createWithContext(pReader, $$2);
      } else {
         return $$3;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggest(EntityAnchorArgument.Anchor.BY_NAME.keySet(), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static enum Anchor {
      FEET("feet", (p_90389_, p_90390_) -> p_90389_),
      EYES("eyes", (p_90382_, p_90383_) -> new Vec3(p_90382_.x, p_90382_.y + (double)p_90383_.getEyeHeight(), p_90382_.z));

      static final Map<String, EntityAnchorArgument.Anchor> BY_NAME = Util.make(Maps.newHashMap(), p_90387_ -> {
         for (EntityAnchorArgument.Anchor $$1 : values()) {
            p_90387_.put($$1.name, $$1);
         }
      });
      private final String name;
      private final BiFunction<Vec3, Entity, Vec3> transform;

      private Anchor(final String pName, final BiFunction<Vec3, Entity, Vec3> pTransform) {
         this.name = pName;
         this.transform = pTransform;
      }

      @Nullable
      public static EntityAnchorArgument.Anchor getByName(String pName) {
         return BY_NAME.get(pName);
      }

      public Vec3 apply(Entity pEntity) {
         return this.transform.apply(pEntity.position(), pEntity);
      }

      public Vec3 apply(CommandSourceStack pSource) {
         Entity $$1 = pSource.getEntity();
         return $$1 == null ? pSource.getPosition() : this.transform.apply(pSource.getPosition(), $$1);
      }
   }
}
