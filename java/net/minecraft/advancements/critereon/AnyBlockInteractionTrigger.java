package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger<AnyBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<AnyBlockInteractionTrigger.TriggerInstance> codec() {
      return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
      ServerLevel $$3 = pPlayer.serverLevel();
      BlockState $$4 = $$3.getBlockState(pPos);
      LootParams $$5 = new LootParams.Builder($$3)
         .withParameter(LootContextParams.ORIGIN, pPos.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
         .withParameter(LootContextParams.BLOCK_STATE, $$4)
         .withParameter(LootContextParams.TOOL, pStack)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext $$6 = new LootContext.Builder($$5).create(Optional.empty());
      this.trigger(pPlayer, p_330225_ -> p_330225_.matches($$6));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_335009_ -> p_335009_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
                  ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
               )
               .apply(p_335009_, AnyBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext pContext) {
         return this.location.isEmpty() || this.location.get().matches(pContext);
      }

      @Override
      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         this.location.ifPresent(p_336130_ -> pValidator.validate(p_336130_, LootContextParamSets.ADVANCEMENT_LOCATION, ".location"));
      }
   }
}
