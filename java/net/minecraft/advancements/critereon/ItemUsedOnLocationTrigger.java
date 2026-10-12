package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   @Override
   public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
      return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
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
      this.trigger(pPlayer, p_286596_ -> p_286596_.matches($$6));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_325222_ -> p_325222_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
                  ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
               )
               .apply(p_325222_, ItemUsedOnLocationTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block pBlock) {
         ContextAwarePredicate $$1 = ContextAwarePredicate.create(LootItemBlockStatePropertyCondition.hasBlockStateProperties(pBlock).build());
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of($$1)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... pConditions) {
         ContextAwarePredicate $$1 = ContextAwarePredicate.create(
            Arrays.stream(pConditions).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
         );
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of($$1)));
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         ContextAwarePredicate $$2 = ContextAwarePredicate.create(LocationCheck.checkLocation(pLocation).build(), MatchTool.toolMatches(pTool).build());
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of($$2));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pTool) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(pLocation, pTool));
      }

      public boolean matches(LootContext pContext) {
         return this.location.isEmpty() || this.location.get().matches(pContext);
      }

      @Override
      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         this.location.ifPresent(p_308139_ -> pValidator.validate(p_308139_, LootContextParamSets.ADVANCEMENT_LOCATION, ".location"));
      }
   }
}
