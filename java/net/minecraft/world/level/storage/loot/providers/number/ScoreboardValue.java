package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public record ScoreboardValue(ScoreboardNameProvider target, String score, float scale) implements NumberProvider {
   public static final MapCodec<ScoreboardValue> CODEC = RecordCodecBuilder.mapCodec(
      p_297867_ -> p_297867_.group(
               ScoreboardNameProviders.CODEC.fieldOf("target").forGetter(ScoreboardValue::target),
               Codec.STRING.fieldOf("score").forGetter(ScoreboardValue::score),
               Codec.FLOAT.fieldOf("scale").orElse(1.0F).forGetter(ScoreboardValue::scale)
            )
            .apply(p_297867_, ScoreboardValue::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.SCORE;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.target.getReferencedContextParams();
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget pEntityTarget, String pScore) {
      return fromScoreboard(pEntityTarget, pScore, 1.0F);
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget pEntityTarget, String pScore, float pScale) {
      return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(pEntityTarget), pScore, pScale);
   }

   @Override
   public float getFloat(LootContext pLootContext) {
      ScoreHolder $$1 = this.target.getScoreHolder(pLootContext);
      if ($$1 == null) {
         return 0.0F;
      } else {
         Scoreboard $$2 = pLootContext.getLevel().getScoreboard();
         Objective $$3 = $$2.getObjective(this.score);
         if ($$3 == null) {
            return 0.0F;
         } else {
            ReadOnlyScoreInfo $$4 = $$2.getPlayerScoreInfo($$1, $$3);
            return $$4 == null ? 0.0F : (float)$$4.value() * this.scale;
         }
      }
   }
}
