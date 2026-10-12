package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
   @Override
   public Codec<KilledByCrossbowTrigger.TriggerInstance> codec() {
      return KilledByCrossbowTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, Collection<Entity> pEntities) {
      List<LootContext> $$2 = Lists.newArrayList();
      Set<EntityType<?>> $$3 = Sets.newHashSet();

      for (Entity $$4 : pEntities) {
         $$3.add($$4.getType());
         $$2.add(EntityPredicate.createContext(pPlayer, $$4));
      }

      this.trigger(pPlayer, p_46881_ -> p_46881_.matches($$2, $$3.size()));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<KilledByCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_325223_ -> p_325223_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(KilledByCrossbowTrigger.TriggerInstance::player),
                  EntityPredicate.ADVANCEMENT_CODEC.listOf().optionalFieldOf("victims", List.of()).forGetter(KilledByCrossbowTrigger.TriggerInstance::victims),
                  MinMaxBounds.Ints.CODEC
                     .optionalFieldOf("unique_entity_types", MinMaxBounds.Ints.ANY)
                     .forGetter(KilledByCrossbowTrigger.TriggerInstance::uniqueEntityTypes)
               )
               .apply(p_325223_, KilledByCrossbowTrigger.TriggerInstance::new)
      );

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(EntityPredicate.Builder... pVictims) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW
            .createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pVictims), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(MinMaxBounds.Ints pUniqueEntityTypes) {
         return CriteriaTriggers.KILLED_BY_CROSSBOW
            .createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), List.of(), pUniqueEntityTypes));
      }

      public boolean matches(Collection<LootContext> pContexts, int pBounds) {
         if (!this.victims.isEmpty()) {
            List<LootContext> $$2 = Lists.newArrayList(pContexts);

            for (ContextAwarePredicate $$3 : this.victims) {
               boolean $$4 = false;
               Iterator<LootContext> $$5 = $$2.iterator();

               while ($$5.hasNext()) {
                  LootContext $$6 = $$5.next();
                  if ($$3.matches($$6)) {
                     $$5.remove();
                     $$4 = true;
                     break;
                  }
               }

               if (!$$4) {
                  return false;
               }
            }
         }

         return this.uniqueEntityTypes.matches(pBounds);
      }

      @Override
      public void validate(CriterionValidator pValidator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
         pValidator.validateEntities(this.victims, ".victims");
      }
   }
}
