package net.minecraft.advancements.critereon;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class CriterionValidator {
   private final ProblemReporter reporter;
   private final HolderGetter.Provider lootData;

   public CriterionValidator(ProblemReporter pReporter, HolderGetter.Provider pLootData) {
      this.reporter = pReporter;
      this.lootData = pLootData;
   }

   public void validateEntity(Optional<ContextAwarePredicate> pEntity, String pName) {
      pEntity.ifPresent(p_312443_ -> this.validateEntity(p_312443_, pName));
   }

   public void validateEntities(List<ContextAwarePredicate> pEntities, String pName) {
      this.validate(pEntities, LootContextParamSets.ADVANCEMENT_ENTITY, pName);
   }

   public void validateEntity(ContextAwarePredicate pEntity, String pName) {
      this.validate(pEntity, LootContextParamSets.ADVANCEMENT_ENTITY, pName);
   }

   public void validate(ContextAwarePredicate pEntity, LootContextParamSet pContextParams, String pName) {
      pEntity.validate(new ValidationContext(this.reporter.forChild(pName), pContextParams, this.lootData));
   }

   public void validate(List<ContextAwarePredicate> pEntities, LootContextParamSet pContextParams, String pName) {
      for (int $$3 = 0; $$3 < pEntities.size(); $$3++) {
         ContextAwarePredicate $$4 = pEntities.get($$3);
         $$4.validate(new ValidationContext(this.reporter.forChild(pName + "[" + $$3 + "]"), pContextParams, this.lootData));
      }
   }
}
