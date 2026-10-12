package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FeatureFlagRemoveFix extends DataFix {
   private final String name;
   private final Set<String> flagsToRemove;

   public FeatureFlagRemoveFix(Schema pOutputSchema, String pName, Set<String> pFlagsToRemove) {
      super(pOutputSchema, false);
      this.name = pName;
      this.flagsToRemove = pFlagsToRemove;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.LEVEL), p_277407_ -> p_277407_.update(DSL.remainderFinder(), this::fixTag)
      );
   }

   private <T> Dynamic<T> fixTag(Dynamic<T> p_277583_) {
      List<Dynamic<T>> $$1 = p_277583_.get("removed_features").asStream().collect(Collectors.toCollection(ArrayList::new));
      Dynamic<T> $$2 = p_277583_.update(
         "enabled_features", p_326589_ -> (Dynamic)DataFixUtils.orElse(p_326589_.asStreamOpt().result().map(p_277400_ -> p_277400_.filter(p_326586_ -> {
                  Optional<String> $$3 = p_326586_.asString().result();
                  if ($$3.isEmpty()) {
                     return true;
                  } else {
                     boolean $$4 = this.flagsToRemove.contains($$3.get());
                     if ($$4) {
                        $$1.add(p_277583_.createString($$3.get()));
                     }

                     return !$$4;
                  }
               })).map(p_277583_::createList), p_326589_)
      );
      if (!$$1.isEmpty()) {
         $$2 = $$2.set("removed_features", p_277583_.createList($$1.stream()));
      }

      return $$2;
   }
}
