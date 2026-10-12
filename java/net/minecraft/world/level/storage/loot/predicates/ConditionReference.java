package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record ConditionReference(ResourceKey<LootItemCondition> name) implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<ConditionReference> CODEC = RecordCodecBuilder.mapCodec(
      p_327641_ -> p_327641_.group(ResourceKey.codec(Registries.PREDICATE).fieldOf("name").forGetter(ConditionReference::name))
            .apply(p_327641_, ConditionReference::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   @Override
   public void validate(ValidationContext pContext) {
      if (!pContext.allowsReferences()) {
         pContext.reportProblem("Uses reference to " + this.name.location() + ", but references are not allowed");
      } else if (pContext.hasVisitedElement(this.name)) {
         pContext.reportProblem("Condition " + this.name.location() + " is recursively called");
      } else {
         LootItemCondition.super.validate(pContext);
         pContext.resolver()
            .get(Registries.PREDICATE, this.name)
            .ifPresentOrElse(
               p_327645_ -> p_327645_.value().validate(pContext.enterElement(".{" + this.name.location() + "}", this.name)),
               () -> pContext.reportProblem("Unknown condition table called " + this.name.location())
            );
      }
   }

   public boolean test(LootContext pContext) {
      LootItemCondition $$1 = pContext.getResolver().get(Registries.PREDICATE, this.name).map(Holder.Reference::value).orElse(null);
      if ($$1 == null) {
         LOGGER.warn("Tried using unknown condition table called {}", this.name.location());
         return false;
      } else {
         LootContext.VisitedEntry<?> $$2 = LootContext.createVisitedEntry($$1);
         if (pContext.pushVisitedElement($$2)) {
            boolean var4;
            try {
               var4 = $$1.test(pContext);
            } finally {
               pContext.popVisitedElement($$2);
            }

            return var4;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(ResourceKey<LootItemCondition> pName) {
      return () -> new ConditionReference(pName);
   }
}
