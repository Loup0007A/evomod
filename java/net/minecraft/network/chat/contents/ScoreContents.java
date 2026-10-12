package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public class ScoreContents implements ComponentContents {
   public static final MapCodec<ScoreContents> INNER_CODEC = RecordCodecBuilder.mapCodec(
      p_310432_ -> p_310432_.group(
               Codec.STRING.fieldOf("name").forGetter(ScoreContents::getName), Codec.STRING.fieldOf("objective").forGetter(ScoreContents::getObjective)
            )
            .apply(p_310432_, ScoreContents::new)
   );
   public static final MapCodec<ScoreContents> CODEC = INNER_CODEC.fieldOf("score");
   public static final ComponentContents.Type<ScoreContents> TYPE = new ComponentContents.Type<>(CODEC, "score");
   private final String name;
   @Nullable
   private final EntitySelector selector;
   private final String objective;

   @Nullable
   private static EntitySelector parseSelector(String pSelector) {
      try {
         return new EntitySelectorParser(new StringReader(pSelector), true).parse();
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   public ScoreContents(String p_237438_, String p_237439_) {
      this.name = p_237438_;
      this.selector = parseSelector(p_237438_);
      this.objective = p_237439_;
   }

   @Override
   public ComponentContents.Type<?> type() {
      return TYPE;
   }

   public String getName() {
      return this.name;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   public String getObjective() {
      return this.objective;
   }

   private ScoreHolder findTargetName(CommandSourceStack pSource) throws CommandSyntaxException {
      if (this.selector != null) {
         List<? extends Entity> $$1 = this.selector.findEntities(pSource);
         if (!$$1.isEmpty()) {
            if ($$1.size() != 1) {
               throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
            }

            return $$1.get(0);
         }
      }

      return ScoreHolder.forNameOnly(this.name);
   }

   private MutableComponent getScore(ScoreHolder pScoreHolder, CommandSourceStack pSource) {
      MinecraftServer $$2 = pSource.getServer();
      if ($$2 != null) {
         Scoreboard $$3 = $$2.getScoreboard();
         Objective $$4 = $$3.getObjective(this.objective);
         if ($$4 != null) {
            ReadOnlyScoreInfo $$5 = $$3.getPlayerScoreInfo(pScoreHolder, $$4);
            if ($$5 != null) {
               return $$5.formatValue($$4.numberFormatOrDefault(StyledFormat.NO_STYLE));
            }
         }
      }

      return Component.empty();
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack pNbtPathPattern, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      if (pNbtPathPattern == null) {
         return Component.empty();
      } else {
         ScoreHolder $$3 = this.findTargetName(pNbtPathPattern);
         ScoreHolder $$4 = (ScoreHolder)(pEntity != null && $$3.equals(ScoreHolder.WILDCARD) ? pEntity : $$3);
         return this.getScore($$4, pNbtPathPattern);
      }
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof ScoreContents $$1 && this.name.equals($$1.name) && this.objective.equals($$1.objective)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.name.hashCode();
      return 31 * $$0 + this.objective.hashCode();
   }

   @Override
   public String toString() {
      return "score{name='" + this.name + "', objective='" + this.objective + "'}";
   }
}
