package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends SimpleCriterionTrigger.SimpleInstance> implements CriterionTrigger<T> {
   private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

   @Override
   public final void addPlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener) {
      this.players.computeIfAbsent(pPlayerAdvancements, p_66252_ -> Sets.newHashSet()).add(pListener);
   }

   @Override
   public final void removePlayerListener(PlayerAdvancements pPlayerAdvancements, CriterionTrigger.Listener<T> pListener) {
      Set<CriterionTrigger.Listener<T>> $$2 = this.players.get(pPlayerAdvancements);
      if ($$2 != null) {
         $$2.remove(pListener);
         if ($$2.isEmpty()) {
            this.players.remove(pPlayerAdvancements);
         }
      }
   }

   @Override
   public final void removePlayerListeners(PlayerAdvancements pPlayerAdvancements) {
      this.players.remove(pPlayerAdvancements);
   }

   protected void trigger(ServerPlayer pPlayer, Predicate<T> pTestTrigger) {
      PlayerAdvancements $$2 = pPlayer.getAdvancements();
      Set<CriterionTrigger.Listener<T>> $$3 = this.players.get($$2);
      if ($$3 != null && !$$3.isEmpty()) {
         LootContext $$4 = EntityPredicate.createContext(pPlayer, pPlayer);
         List<CriterionTrigger.Listener<T>> $$5 = null;

         for (CriterionTrigger.Listener<T> $$6 : $$3) {
            T $$7 = $$6.trigger();
            if (pTestTrigger.test($$7)) {
               Optional<ContextAwarePredicate> $$8 = $$7.player();
               if ($$8.isEmpty() || $$8.get().matches($$4)) {
                  if ($$5 == null) {
                     $$5 = Lists.newArrayList();
                  }

                  $$5.add($$6);
               }
            }
         }

         if ($$5 != null) {
            for (CriterionTrigger.Listener<T> $$9 : $$5) {
               $$9.run($$2);
            }
         }
      }
   }

   public interface SimpleInstance extends CriterionTriggerInstance {
      @Override
      default void validate(CriterionValidator p_311475_) {
         p_311475_.validateEntity(this.player(), ".player");
      }

      Optional<ContextAwarePredicate> player();
   }
}
