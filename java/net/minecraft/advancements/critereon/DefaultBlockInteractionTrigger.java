package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class DefaultBlockInteractionTrigger extends SimpleCriterionTrigger<DefaultBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<DefaultBlockInteractionTrigger.TriggerInstance> codec() {
      return DefaultBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos) {
      ServerLevel $$2 = pPlayer.serverLevel();
      BlockState $$3 = $$2.getBlockState(pPos);
      LootParams $$4 = new LootParams.Builder($$2)
         .withParameter(LootContextParams.ORIGIN, pPos.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
         .withParameter(LootContextParams.BLOCK_STATE, $$3)
         .create(LootContextParamSets.BLOCK_USE);
      LootContext $$5 = new LootContext.Builder($$4).create(Optional.empty());
      this.trigger(pPlayer, p_335442_ -> p_335442_.matches($$5));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<DefaultBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_330508_ -> p_330508_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DefaultBlockInteractionTrigger.TriggerInstance::player),
                  ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(DefaultBlockInteractionTrigger.TriggerInstance::location)
               )
               .apply(p_330508_, DefaultBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext pContext) {
         return this.location.isEmpty() || this.location.get().matches(pContext);
      }

      @Override
      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         this.location.ifPresent(p_328396_ -> pValidator.validate(p_328396_, LootContextParamSets.BLOCK_USE, ".location"));
      }
   }
}
